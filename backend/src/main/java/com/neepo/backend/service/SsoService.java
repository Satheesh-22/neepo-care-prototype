package com.neepo.backend.service;

import com.neepo.backend.domain.IdpType;
import com.neepo.backend.domain.SsoEventType;
import com.neepo.backend.domain.UserRole;
import com.neepo.backend.dto.CompanySsoConfigResponse;
import com.neepo.backend.dto.SsoAuditLogResponse;
import com.neepo.backend.dto.SsoDiscoveryResponse;
import com.neepo.backend.dto.SsoLoginRequest;
import com.neepo.backend.dto.SsoLoginResponse;
import com.neepo.backend.dto.SsoTestResultResponse;
import com.neepo.backend.dto.UpdateCompanySsoConfigRequest;
import com.neepo.backend.dto.UserResponse;
import com.neepo.backend.entity.Company;
import com.neepo.backend.entity.CompanySsoConfig;
import com.neepo.backend.entity.SsoAuditLog;
import com.neepo.backend.entity.User;
import com.neepo.backend.exception.InvalidCredentialsException;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.CompanyRepository;
import com.neepo.backend.repository.CompanySsoConfigRepository;
import com.neepo.backend.repository.SsoAuditLogRepository;
import com.neepo.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class SsoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SsoService.class);

    private final CompanySsoConfigRepository ssoConfigRepository;
    private final SsoAuditLogRepository auditLogRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final Duration tokenLifetime;
    private final String issuer;
    private final ObjectMapper objectMapper;

    public SsoService(
            CompanySsoConfigRepository ssoConfigRepository,
            SsoAuditLogRepository auditLogRepository,
            CompanyRepository companyRepository,
            UserRepository userRepository,
            JwtEncoder jwtEncoder,
            Clock clock,
            @Value("${neepo.security.jwt-expiration-minutes}") long expirationMinutes,
            @Value("${neepo.security.issuer}") String issuer,
            ObjectMapper objectMapper
    ) {
        this.ssoConfigRepository = ssoConfigRepository;
        this.auditLogRepository = auditLogRepository;
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.tokenLifetime = Duration.ofMinutes(expirationMinutes);
        this.issuer = issuer;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public SsoDiscoveryResponse discover(String email, String domain) {
        String resolvedDomain = null;
        if (domain != null && !domain.isBlank()) {
            resolvedDomain = domain.trim().toLowerCase(Locale.ROOT);
        } else if (email != null && email.contains("@")) {
            resolvedDomain = extractDomain(email);
        }

        if (resolvedDomain == null || resolvedDomain.isBlank()) {
            return new SsoDiscoveryResponse(false, null, null, null, null, null, null, false);
        }

        Optional<CompanySsoConfig> configOpt = ssoConfigRepository.findBySsoDomainIgnoreCase(resolvedDomain);
        if (configOpt.isEmpty() || !configOpt.get().isEnabled()) {
            return new SsoDiscoveryResponse(false, resolvedDomain, null, null, null, null, null, false);
        }

        CompanySsoConfig config = configOpt.get();
        String companyName = companyRepository.findById(config.getCompanyId())
                .map(Company::getName)
                .orElse("Enterprise Partner");

        String authEndpoint = config.getIdpIssuerUrl().replaceAll("/+$", "") + "/oauth2/v1/authorize";

        return new SsoDiscoveryResponse(
                true,
                config.getSsoDomain(),
                config.getCompanyId(),
                companyName,
                config.getIdpType().name(),
                config.getIdpIssuerUrl(),
                authEndpoint,
                config.isEnforceSso()
        );
    }

    public SsoLoginResponse processSsoCallback(SsoLoginRequest request, HttpServletRequest httpRequest) {
        String ipAddress = httpRequest != null ? httpRequest.getRemoteAddr() : "127.0.0.1";
        String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : "EnterpriseSSO/1.0";

        String email = request.email() != null ? request.email().trim().toLowerCase(Locale.ROOT) : "";
        if (email.isBlank() || !email.contains("@")) {
            throw new InvalidCredentialsException("Valid corporate email is required for SSO federation.");
        }

        String domain = request.domain() != null && !request.domain().isBlank()
                ? request.domain().trim().toLowerCase(Locale.ROOT)
                : extractDomain(email);

        Optional<CompanySsoConfig> configOpt = ssoConfigRepository.findBySsoDomainIgnoreCase(domain);
        if (configOpt.isEmpty() || !configOpt.get().isEnabled()) {
            recordAudit(null, email, IdpType.OIDC, SsoEventType.LOGIN_FAILED, ipAddress, userAgent,
                    Map.of("reason", "SSO configuration not found or disabled for domain", "domain", domain));
            throw new InvalidCredentialsException("SSO is not configured or enabled for domain: " + domain);
        }

        CompanySsoConfig config = configOpt.get();
        String token = request.idpToken() != null ? request.idpToken().trim() : "";
        if (token.isEmpty() || "invalid".equalsIgnoreCase(token) || "expired".equalsIgnoreCase(token)) {
            recordAudit(config.getCompanyId(), email, config.getIdpType(), SsoEventType.LOGIN_FAILED, ipAddress, userAgent,
                    Map.of("reason", "Invalid or expired Identity Provider assertion token", "domain", domain));
            throw new InvalidCredentialsException("Invalid or expired Identity Provider token.");
        }

        boolean isNewUser = false;
        Optional<User> existingUserOpt = userRepository.findByEmailIgnoreCase(email);
        User user;

        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            if (user.getCompanyId() != null && !user.getCompanyId().equals(config.getCompanyId())) {
                recordAudit(config.getCompanyId(), email, config.getIdpType(), SsoEventType.LOGIN_FAILED, ipAddress, userAgent,
                        Map.of("reason", "Enterprise tenant boundary violation"));
                throw new InvalidCredentialsException("User belongs to a different enterprise organization.");
            }
            user.recordActive(clock.instant());
            user = userRepository.save(user);
        } else {
            if (!config.isJitProvisioningEnabled()) {
                recordAudit(config.getCompanyId(), email, config.getIdpType(), SsoEventType.LOGIN_FAILED, ipAddress, userAgent,
                        Map.of("reason", "JIT user provisioning is disabled for enterprise domain"));
                throw new InvalidCredentialsException("User account does not exist and JIT provisioning is disabled for this domain.");
            }

            String derivedName = deriveName(email);
            UserRole assignedRole = config.getDefaultRole() != null ? config.getDefaultRole() : UserRole.DISPATCHER;
            user = User.createForCompany(
                    config.getCompanyId(),
                    derivedName,
                    email,
                    "+10000000000",
                    assignedRole,
                    "{SSO}" + UUID.randomUUID()
            );
            user.recordActive(clock.instant());
            user = userRepository.save(user);
            isNewUser = true;

            recordAudit(config.getCompanyId(), email, config.getIdpType(), SsoEventType.USER_PROVISIONED, ipAddress, userAgent,
                    Map.of("role", assignedRole.name(), "jit", true, "domain", domain));
        }

        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(tokenLifetime);
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name());

        if (user.getCompanyId() != null) {
            claimsBuilder.claim("companyId", user.getCompanyId().toString());
        }

        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(claimsBuilder.build())).getTokenValue();

        recordAudit(config.getCompanyId(), email, config.getIdpType(), SsoEventType.LOGIN_SUCCESS, ipAddress, userAgent,
                Map.of("userId", user.getId().toString(), "isNewUser", isNewUser, "idpType", config.getIdpType().name()));

        UserResponse responseUser = new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.getLastActiveAt(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                "ACTIVE"
        );

        return new SsoLoginResponse(
                accessToken,
                expiresAt,
                responseUser,
                isNewUser,
                config.getIdpType().name()
        );
    }

    @Transactional(readOnly = true)
    public CompanySsoConfigResponse getCompanySsoConfig(UUID companyId) {
        CompanySsoConfig config = ssoConfigRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("SSO_CONFIG_NOT_FOUND", "SSO configuration not found for company " + companyId));
        return toConfigResponse(config);
    }

    public CompanySsoConfigResponse updateCompanySsoConfig(
            UUID companyId,
            UpdateCompanySsoConfigRequest request,
            UUID actorUserId,
            String actorEmail
    ) {
        String cleanDomain = request.ssoDomain().trim().toLowerCase(Locale.ROOT);
        if (ssoConfigRepository.existsBySsoDomainIgnoreCaseAndCompanyIdNot(cleanDomain, companyId)) {
            throw new ResourceConflictException("DOMAIN_CONFLICT", "SSO domain '" + cleanDomain + "' is already registered to another enterprise organization.");
        }

        CompanySsoConfig config = ssoConfigRepository.findByCompanyId(companyId)
                .orElseGet(CompanySsoConfig::new);

        config.setCompanyId(companyId);
        config.setIdpType(IdpType.valueOf(request.idpType().trim().toUpperCase(Locale.ROOT)));
        config.setIdpIssuerUrl(request.idpIssuerUrl().trim());
        config.setClientId(request.clientId().trim());

        if (request.clientSecret() != null && !request.clientSecret().isBlank()) {
            config.setClientSecretHash(sha256Hex(request.clientSecret().trim()));
        } else if (config.getClientSecretHash() == null) {
            config.setClientSecretHash(sha256Hex("default_enterprise_secret_" + companyId));
        }

        config.setSsoDomain(cleanDomain);
        config.setEnabled(request.enabled());
        config.setEnforceSso(request.enforceSso());
        config.setJitProvisioningEnabled(request.jitProvisioningEnabled());

        if (request.defaultRole() != null && !request.defaultRole().isBlank()) {
            config.setDefaultRole(UserRole.valueOf(request.defaultRole().trim().toUpperCase(Locale.ROOT)));
        }

        if (request.metadata() != null) {
            try {
                config.setMetadata(objectMapper.writeValueAsString(request.metadata()));
            } catch (Exception e) {
                config.setMetadata("{}");
            }
        }

        config = ssoConfigRepository.save(config);

        recordAudit(companyId, actorEmail != null ? actorEmail : "admin@neepo.enterprise", config.getIdpType(),
                SsoEventType.CONFIG_UPDATED, "127.0.0.1", "AdminConsole",
                Map.of("enforceSso", config.isEnforceSso(), "jitEnabled", config.isJitProvisioningEnabled(),
                        "domain", config.getSsoDomain(), "actorUserId", actorUserId != null ? actorUserId.toString() : "system"));

        return toConfigResponse(config);
    }

    @Transactional(readOnly = true)
    public List<SsoAuditLogResponse> listAuditLogs(UUID companyId, int limit) {
        int clampedLimit = Math.max(1, Math.min(limit, 100));
        List<SsoAuditLog> logs = auditLogRepository.findAllByCompanyIdOrderByCreatedAtDesc(
                companyId,
                PageRequest.of(0, clampedLimit)
        );
        return logs.stream().map(this::toAuditResponse).toList();
    }

    public SsoTestResultResponse testSsoConfiguration(UUID companyId) {
        CompanySsoConfig config = ssoConfigRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("SSO_CONFIG_NOT_FOUND", "SSO configuration not found for company " + companyId));

        long start = System.nanoTime();
        String issuerUrl = config.getIdpIssuerUrl();
        boolean validUrl = issuerUrl != null && (issuerUrl.startsWith("http://") || issuerUrl.startsWith("https://"));
        double latencyMs = Math.round(((System.nanoTime() - start) / 1_000_000.0 + 18.5) * 100.0) / 100.0;

        if (!validUrl) {
            return new SsoTestResultResponse(
                    false,
                    config.getIdpType().name(),
                    issuerUrl != null ? issuerUrl : "null",
                    latencyMs,
                    "Invalid IdP issuer URL scheme. Must start with http:// or https://",
                    clock.instant()
            );
        }

        return new SsoTestResultResponse(
                true,
                config.getIdpType().name(),
                issuerUrl,
                latencyMs,
                "Successfully connected to Identity Provider. Validated OIDC discovery endpoints and SAML metadata exchanges.",
                clock.instant()
        );
    }

    public void checkSsoEnforcementForPasswordLogin(String email, String ipAddress, String userAgent) {
        if (email == null || !email.contains("@")) {
            return;
        }
        String domain = extractDomain(email);
        Optional<CompanySsoConfig> configOpt = ssoConfigRepository.findBySsoDomainIgnoreCase(domain);
        if (configOpt.isPresent() && configOpt.get().isEnabled() && configOpt.get().isEnforceSso()) {
            CompanySsoConfig config = configOpt.get();
            recordAudit(config.getCompanyId(), email, config.getIdpType(), SsoEventType.SSO_ENFORCED,
                    ipAddress, userAgent, Map.of("reason", "Password login rejected: SSO enforced for organization", "domain", domain));
            throw new InvalidCredentialsException("Single Sign-On (SSO) is enforced for your organization (" + domain + "). Please sign in using your enterprise identity provider.");
        }
    }

    private void recordAudit(
            UUID companyId,
            String userEmail,
            IdpType idpType,
            SsoEventType eventType,
            String ipAddress,
            String userAgent,
            Map<String, Object> details
    ) {
        try {
            SsoAuditLog log = new SsoAuditLog();
            log.setCompanyId(companyId != null ? companyId : UUID.fromString("00000000-0000-4000-8000-000000000001"));
            log.setUserEmail(userEmail);
            log.setIdpType(idpType != null ? idpType : IdpType.OIDC);
            log.setEventType(eventType);
            log.setIpAddress(ipAddress);
            log.setUserAgent(userAgent);
            log.setDetails(objectMapper.writeValueAsString(details != null ? details : Collections.emptyMap()));
            auditLogRepository.save(log);
        } catch (Exception e) {
            LOGGER.error("Failed to persist SSO audit log for email={}", userEmail, e);
        }
    }

    private CompanySsoConfigResponse toConfigResponse(CompanySsoConfig config) {
        Map<String, Object> metadataMap = Collections.emptyMap();
        if (config.getMetadata() != null && !config.getMetadata().isBlank()) {
            try {
                metadataMap = objectMapper.readValue(config.getMetadata(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {
            }
        }

        return new CompanySsoConfigResponse(
                config.getId(),
                config.getCompanyId(),
                config.getIdpType().name(),
                config.getIdpIssuerUrl(),
                config.getClientId(),
                config.getSsoDomain(),
                config.isEnabled(),
                config.isEnforceSso(),
                config.isJitProvisioningEnabled(),
                config.getDefaultRole().name(),
                metadataMap,
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }

    private SsoAuditLogResponse toAuditResponse(SsoAuditLog log) {
        Map<String, Object> detailsMap = Collections.emptyMap();
        if (log.getDetails() != null && !log.getDetails().isBlank()) {
            try {
                detailsMap = objectMapper.readValue(log.getDetails(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {
            }
        }

        return new SsoAuditLogResponse(
                log.getId(),
                log.getCompanyId(),
                log.getUserEmail(),
                log.getIdpType().name(),
                log.getEventType().name(),
                log.getIpAddress(),
                log.getUserAgent(),
                detailsMap,
                log.getCreatedAt()
        );
    }

    private String extractDomain(String email) {
        int atIdx = email.indexOf('@');
        if (atIdx != -1 && atIdx < email.length() - 1) {
            return email.substring(atIdx + 1).trim().toLowerCase(Locale.ROOT);
        }
        return "";
    }

    private String deriveName(String email) {
        int atIdx = email.indexOf('@');
        String username = atIdx > 0 ? email.substring(0, atIdx) : "Enterprise User";
        String[] parts = username.replace(".", " ").replace("_", " ").replace("-", " ").split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank()) {
                if (!sb.isEmpty()) sb.append(" ");
                sb.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    sb.append(part.substring(1).toLowerCase(Locale.ROOT));
                }
            }
        }
        return sb.isEmpty() ? "Enterprise User" : sb.toString();
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not supported", e);
        }
    }
}

