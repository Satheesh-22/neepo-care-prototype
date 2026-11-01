package com.neepo.backend.service;

import com.neepo.backend.domain.UserRole;
import com.neepo.backend.dto.ScimListResponse;
import com.neepo.backend.dto.ScimPatchOpRequest;
import com.neepo.backend.dto.ScimStatusResponse;
import com.neepo.backend.dto.ScimTokenResponse;
import com.neepo.backend.dto.ScimUserDto;
import com.neepo.backend.entity.CompanySsoConfig;
import com.neepo.backend.entity.User;
import com.neepo.backend.exception.InvalidCredentialsException;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.CompanySsoConfigRepository;
import com.neepo.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ScimService {

    private final CompanySsoConfigRepository companySsoConfigRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public ScimService(
            CompanySsoConfigRepository companySsoConfigRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.companySsoConfigRepository = companySsoConfigRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public ScimTokenResponse generateScimToken(UUID companyId, String baseUrl) {
        CompanySsoConfig config = companySsoConfigRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("SSO_CONFIG_NOT_FOUND", "SSO configuration not found for company."));

        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        String tokenBody = HexFormat.of().formatHex(randomBytes);
        String rawToken = "scim_live_" + tokenBody;
        String tokenHash = hashToken(rawToken);
        String prefix = rawToken.substring(0, Math.min(16, rawToken.length())) + "...";

        config.setScimEnabled(true);
        config.setScimTokenHash(tokenHash);
        config.setScimTokenPrefix(prefix);
        companySsoConfigRepository.save(config);

        String scimBaseUrl = baseUrl + "/api/v1/scim/v2";
        return new ScimTokenResponse(rawToken, prefix, scimBaseUrl, true, Instant.now());
    }

    public void revokeScimToken(UUID companyId) {
        companySsoConfigRepository.findByCompanyId(companyId).ifPresent(config -> {
            config.setScimEnabled(false);
            config.setScimTokenHash(null);
            config.setScimTokenPrefix(null);
            companySsoConfigRepository.save(config);
        });
    }

    @Transactional(readOnly = true)
    public ScimStatusResponse getScimStatus(UUID companyId, String baseUrl) {
        CompanySsoConfig config = companySsoConfigRepository.findByCompanyId(companyId).orElse(null);
        boolean enabled = config != null && config.isScimEnabled();
        String prefix = config != null ? config.getScimTokenPrefix() : null;
        String scimBaseUrl = baseUrl + "/api/v1/scim/v2";

        long provisionedCount = userRepository.findAllByCompanyId(companyId, PageRequest.of(0, 1000))
                .stream()
                .filter(u -> u.getScimExternalId() != null && !u.getScimExternalId().isBlank())
                .count();

        return new ScimStatusResponse(enabled, prefix, scimBaseUrl, provisionedCount);
    }

    @Transactional(readOnly = true)
    public UUID authenticateScimBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new InvalidCredentialsException("Missing or invalid Authorization header for SCIM request.");
        }
        String token = authHeader.substring(7).trim();
        String tokenHash = hashToken(token);

        CompanySsoConfig config = companySsoConfigRepository.findByScimTokenHashAndScimEnabledTrue(tokenHash)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or revoked SCIM token."));

        return config.getCompanyId();
    }

    @Transactional(readOnly = true)
    public ScimListResponse<ScimUserDto> listScimUsers(UUID companyId, int startIndex, int count, String baseUrl) {
        int pageIndex = Math.max(0, (startIndex - 1) / Math.max(1, count));
        int pageSize = Math.min(100, Math.max(1, count));
        Page<User> page = userRepository.findAllByCompanyId(companyId, PageRequest.of(pageIndex, pageSize));

        List<ScimUserDto> resources = page.getContent()
                .stream()
                .map(u -> toScimUser(u, baseUrl))
                .collect(Collectors.toList());

        return new ScimListResponse<>(
                List.of("urn:ietf:params:scim:api:messages:2.0:ListResponse"),
                page.getTotalElements(),
                startIndex,
                resources.size(),
                resources
        );
    }

    @Transactional(readOnly = true)
    public ScimUserDto getScimUser(UUID companyId, UUID userId, String baseUrl) {
        User user = userRepository.findByIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "SCIM user not found: " + userId));
        return toScimUser(user, baseUrl);
    }

    public ScimUserDto createScimUser(UUID companyId, ScimUserDto request, String baseUrl) {
        String email = extractEmail(request);
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("SCIM User requires at least one work email.");
        }
        email = email.toLowerCase().trim();

        Optional<User> existing = userRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            User user = existing.get();
            if (!companyId.equals(user.getCompanyId())) {
                throw new ResourceConflictException("EMAIL_IN_USE", "User email is already registered in another organization.");
            }
            // Link existing user to SCIM external ID
            if (request.externalId() != null) {
                user.setScimExternalId(request.externalId());
            }
            if (request.active() != null) {
                if (Boolean.TRUE.equals(request.active())) {
                    user.reactivate();
                } else {
                    user.suspend();
                }
            }
            userRepository.save(user);
            return toScimUser(user, baseUrl);
        }

        String passwordHash = passwordEncoder.encode(UUID.randomUUID().toString());
        User user = User.createForCompany(
                companyId,
                extractName(request, email),
                email,
                extractPhone(request),
                extractRole(request),
                passwordHash
        );
        user.setScimExternalId(request.externalId() != null ? request.externalId() : UUID.randomUUID().toString());
        if (Boolean.FALSE.equals(request.active())) {
            user.suspend();
        }

        User saved = userRepository.save(user);
        return toScimUser(saved, baseUrl);
    }

    public ScimUserDto updateScimUser(UUID companyId, UUID userId, ScimUserDto request, String baseUrl) {
        User user = userRepository.findByIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "SCIM user not found: " + userId));

        String email = extractEmail(request);
        if (email != null && !email.isBlank()) {
            email = email.toLowerCase().trim();
            if (!email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailIgnoreCase(email)) {
                throw new ResourceConflictException("EMAIL_IN_USE", "Email already in use by another user.");
            }
            user.setEmail(email);
        }

        String name = extractName(request, user.getName());
        String phone = extractPhone(request);
        UserRole role = user.getRole();
        if (request.userType() != null) {
            try {
                role = UserRole.valueOf(request.userType().toUpperCase());
            } catch (Exception ignored) {}
        }
        user.updateDetails(name, phone, role);

        if (request.externalId() != null) {
            user.setScimExternalId(request.externalId());
        }

        if (request.active() != null) {
            if (Boolean.TRUE.equals(request.active())) {
                user.reactivate();
            } else {
                user.suspend();
            }
        }

        User saved = userRepository.save(user);
        return toScimUser(saved, baseUrl);
    }

    public ScimUserDto patchScimUser(UUID companyId, UUID userId, ScimPatchOpRequest request, String baseUrl) {
        User user = userRepository.findByIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "SCIM user not found: " + userId));

        if (request.operations() != null) {
            for (ScimPatchOpRequest.Operation op : request.operations()) {
                if ("replace".equalsIgnoreCase(op.op()) || "add".equalsIgnoreCase(op.op())) {
                    if ("active".equalsIgnoreCase(op.path()) || (op.path() == null && op.value() instanceof Boolean)) {
                        boolean active = Boolean.parseBoolean(String.valueOf(op.value()));
                        if (active) {
                            user.reactivate();
                        } else {
                            user.suspend();
                        }
                    }
                }
            }
        }

        User saved = userRepository.save(user);
        return toScimUser(saved, baseUrl);
    }

    public void deleteScimUser(UUID companyId, UUID userId) {
        User user = userRepository.findByIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "SCIM user not found: " + userId));

        user.suspend();
        userRepository.save(user);
    }

    private ScimUserDto toScimUser(User user, String baseUrl) {
        String location = baseUrl + "/api/v1/scim/v2/Users/" + user.getId();
        String[] nameParts = user.getName().split(" ", 2);
        String givenName = nameParts[0];
        String familyName = nameParts.length > 1 ? nameParts[1] : "";

        return new ScimUserDto(
                List.of("urn:ietf:params:scim:schemas:core:2.0:User"),
                user.getId().toString(),
                user.getScimExternalId(),
                user.getEmail(),
                new ScimUserDto.Name(user.getName(), familyName, givenName),
                List.of(new ScimUserDto.Email(user.getEmail(), true, "work")),
                user.getPhone() != null ? List.of(new ScimUserDto.PhoneNumber(user.getPhone(), "work")) : List.of(),
                user.isEnabled(),
                user.getRole().name(),
                new ScimUserDto.Meta(
                        "User",
                        user.getCreatedAt().toString(),
                        user.getUpdatedAt().toString(),
                        location
                )
        );
    }

    private String extractEmail(ScimUserDto request) {
        if (request.emails() != null && !request.emails().isEmpty()) {
            return request.emails().get(0).value();
        }
        if (request.userName() != null && request.userName().contains("@")) {
            return request.userName();
        }
        return null;
    }

    private String extractName(ScimUserDto request, String fallback) {
        if (request.name() != null) {
            if (request.name().formatted() != null && !request.name().formatted().isBlank()) {
                return request.name().formatted().trim();
            }
            if (request.name().givenName() != null || request.name().familyName() != null) {
                String given = request.name().givenName() != null ? request.name().givenName() : "";
                String family = request.name().familyName() != null ? request.name().familyName() : "";
                return (given + " " + family).trim();
            }
        }
        return fallback != null ? fallback : "SCIM User";
    }

    private String extractPhone(ScimUserDto request) {
        if (request.phoneNumbers() != null && !request.phoneNumbers().isEmpty()) {
            String p = request.phoneNumbers().get(0).value();
            if (p != null) {
                // Ensure starts with + or numbers
                String clean = p.replaceAll("[^0-9+]", "");
                if (clean.length() >= 10) {
                    return clean.startsWith("+") ? clean : "+" + clean;
                }
            }
        }
        return "+10000000000";
    }

    private UserRole extractRole(ScimUserDto request) {
        if (request.userType() != null) {
            try {
                return UserRole.valueOf(request.userType().toUpperCase());
            } catch (Exception ignored) {}
        }
        return UserRole.DISPATCHER;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
