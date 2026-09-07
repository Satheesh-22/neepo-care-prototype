package com.neepo.backend.service;

import com.neepo.backend.domain.DriverStatus;
import com.neepo.backend.domain.UserRole;
import com.neepo.backend.dto.AcceptInvitationRequest;
import com.neepo.backend.dto.CreateUserInvitationRequest;
import com.neepo.backend.dto.UserInvitationResponse;
import com.neepo.backend.dto.UserResponse;
import com.neepo.backend.entity.Driver;
import com.neepo.backend.entity.User;
import com.neepo.backend.entity.UserInvitation;
import com.neepo.backend.exception.EmailAlreadyExistsException;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.ResourceNotFoundException;
import com.neepo.backend.repository.DriverRepository;
import com.neepo.backend.repository.UserInvitationRepository;
import com.neepo.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserInvitationService {

    private final UserInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final Clock clock;

    public UserInvitationService(UserInvitationRepository invitationRepository,
                                 UserRepository userRepository,
                                 DriverRepository driverRepository,
                                 PasswordEncoder passwordEncoder,
                                 AuditService auditService,
                                 Clock clock) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public UserInvitationResponse inviteUser(UUID companyId, UUID actorUserId, UserRole actorRole,
                                            UUID traceId, CreateUserInvitationRequest request) {
        if (actorRole == UserRole.DISPATCHER && request.role() == UserRole.ADMIN) {
            throw new ResourceConflictException("DISPATCHER_CANNOT_CREATE_ADMIN",
                    "Dispatchers cannot invite or create administrators");
        }

        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        Optional<User> existingUser = userRepository.findByEmailIgnoreCase(normalizedEmail);

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (!companyId.equals(user.getCompanyId())) {
                throw new EmailAlreadyExistsException("A user with this email belongs to another company");
            }
            if (!"INVITED".equals(user.getStatus())) {
                throw new EmailAlreadyExistsException("A user with this email is already registered and active");
            }
        } else {
            user = User.createInvited(companyId, request.name().trim(), normalizedEmail, request.phone().trim(), request.role());
            user = userRepository.save(user);
        }

        // Generate raw one-way token and SHA-256 hash
        String rawToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        String tokenHash = hashToken(rawToken);

        Instant expiresAt = clock.instant().plus(Duration.ofDays(7));
        UserInvitation invitation = UserInvitation.create(
                companyId,
                user.getId(),
                tokenHash,
                normalizedEmail,
                request.role(),
                expiresAt,
                actorUserId
        );

        UserInvitation saved = invitationRepository.save(invitation);

        auditService.record(traceId, actorUserId, "USER_INVITATION_CREATED", "USER", user.getId(),
                Map.of("email", normalizedEmail, "role", request.role().name(), "expiresAt", expiresAt.toString()));

        return new UserInvitationResponse(
                saved.getId(),
                user.getId(),
                saved.getEmail(),
                saved.getRole(),
                rawToken,
                saved.getExpiresAt(),
                saved.getAcceptedAt(),
                saved.getCreatedAt()
        );
    }

    @Transactional
    public UserResponse acceptInvitation(AcceptInvitationRequest request, UUID traceId) {
        String tokenHash = hashToken(request.token().trim());
        UserInvitation invitation = invitationRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("INVITATION_NOT_FOUND",
                        "Invitation token is invalid or has expired"));

        Instant now = clock.instant();
        if (invitation.isAccepted()) {
            throw new ResourceConflictException("INVITATION_ALREADY_USED", "This invitation has already been accepted");
        }
        if (invitation.isExpired(now)) {
            throw new ResourceConflictException("INVITATION_EXPIRED", "This invitation has expired");
        }

        User user = userRepository.findById(invitation.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User account not found"));

        user.activateWithPassword(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        invitation.markAccepted(now);
        invitationRepository.save(invitation);

        // If driver exists for this user, activate driver verification
        driverRepository.findByUserId(user.getId()).ifPresent(driver -> {
            driver.setVerificationState("VERIFIED");
            if (driver.getStatus() == null) {
                driver.markAvailable();
            }
            driverRepository.save(driver);
        });

        auditService.record(traceId, user.getId(), "USER_INVITATION_ACCEPTED", "USER", user.getId(),
                Map.of("email", user.getEmail(), "role", user.getRole().name()));

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.getLastActiveAt(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                "ACCEPTED"
        );
    }

    @Transactional
    public void revokeInvitation(UUID companyId, UUID invitationId, UUID actorUserId, UUID traceId) {
        UserInvitation invitation = invitationRepository.findByIdAndCompanyId(invitationId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("INVITATION_NOT_FOUND", "Invitation not found"));

        UUID userId = invitation.getUserId();
        invitationRepository.delete(invitation);

        userRepository.findById(userId).ifPresent(user -> {
            if ("INVITED".equals(user.getStatus())) {
                userRepository.delete(user);
            }
        });

        auditService.record(traceId, actorUserId, "USER_INVITATION_REVOKED", "USER", userId,
                Map.of("invitationId", invitationId.toString()));
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
