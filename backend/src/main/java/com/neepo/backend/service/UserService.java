package com.neepo.backend.service;

import com.neepo.backend.api.PageResponse;
import com.neepo.backend.domain.UserRole;
import com.neepo.backend.dto.CreateUserRequest;
import com.neepo.backend.dto.UpdateUserRequest;
import com.neepo.backend.dto.UserResponse;
import com.neepo.backend.entity.User;
import com.neepo.backend.entity.UserInvitation;
import com.neepo.backend.exception.EmailAlreadyExistsException;
import com.neepo.backend.exception.ResourceConflictException;
import com.neepo.backend.exception.UserNotFoundException;
import com.neepo.backend.repository.UserInvitationRepository;
import com.neepo.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserInvitationRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @org.springframework.beans.factory.annotation.Autowired
    public UserService(UserRepository userRepository,
                       UserInvitationRepository invitationRepository,
                       PasswordEncoder passwordEncoder,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.invitationRepository = invitationRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this(userRepository, null, passwordEncoder, null);
    }

    @Transactional
    public UserResponse createUser(UUID companyId, CreateUserRequest request) {
        return createUser(companyId, null, UserRole.ADMIN, null, request);
    }

    @Transactional
    public UserResponse createUser(UUID companyId, UUID actorUserId, UserRole actorRole,
                                  UUID traceId, CreateUserRequest request) {
        if (actorRole == UserRole.DISPATCHER && request.role() == UserRole.ADMIN) {
            throw new ResourceConflictException("DISPATCHER_CANNOT_CREATE_ADMIN",
                    "Dispatchers cannot create administrators");
        }

        String normalizedEmail = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException("Email is already registered");
        }

        User user = User.createForCompany(
                companyId,
                request.name().trim(),
                normalizedEmail,
                request.phone().trim(),
                request.role(),
                passwordEncoder.encode(request.password())
        );

        User saved = userRepository.save(user);

        if (auditService != null) {
            auditService.record(traceId, actorUserId, "USER_CREATED", "USER", saved.getId(),
                    Map.of("email", saved.getEmail(), "role", saved.getRole().name()));
        }

        return toResponse(saved);
    }

    public PageResponse<UserResponse> getUsers(UUID companyId, int page, int size) {
        Page<UserResponse> users = userRepository.findAllByCompanyId(
                        companyId,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name", "id")))
                .map(this::toResponse);
        return PageResponse.from(users);
    }

    public UserResponse getUserById(UUID companyId, UUID id) {
        return userRepository.findByIdAndCompanyId(id, companyId)
                .map(this::toResponse)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public UserResponse getUserByEmail(UUID companyId, String email) {
        return userRepository.findByEmailIgnoreCaseAndCompanyId(normalizeEmail(email), companyId)
                .map(this::toResponse)
                .orElseThrow(UserNotFoundException::forEmail);
    }

    @Transactional
    public UserResponse updateUser(UUID companyId, UUID userId, UUID actorUserId, UUID traceId, UpdateUserRequest request) {
        User user = userRepository.findByIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (request.role() != null && request.role() != user.getRole()) {
            if (user.getRole() == UserRole.ADMIN && request.role() != UserRole.ADMIN) {
                checkFinalAdminProtection(companyId, userId, "demote");
            }
        }

        user.updateDetails(request.name(), request.phone(), request.role());
        User saved = userRepository.save(user);

        if (auditService != null) {
            auditService.record(traceId, actorUserId, "USER_UPDATED", "USER", saved.getId(),
                    Map.of("name", saved.getName(), "role", saved.getRole().name()));
        }

        return toResponse(saved);
    }

    @Transactional
    public UserResponse suspendUser(UUID companyId, UUID userId, UUID actorUserId, UUID traceId) {
        User user = userRepository.findByIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (user.getRole() == UserRole.ADMIN) {
            checkFinalAdminProtection(companyId, userId, "suspend");
        }

        user.suspend();
        User saved = userRepository.save(user);

        if (auditService != null) {
            auditService.record(traceId, actorUserId, "USER_SUSPENDED", "USER", saved.getId(),
                    Map.of("email", saved.getEmail(), "role", saved.getRole().name()));
        }

        return toResponse(saved);
    }

    @Transactional
    public UserResponse reactivateUser(UUID companyId, UUID userId, UUID actorUserId, UUID traceId) {
        User user = userRepository.findByIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.reactivate();
        User saved = userRepository.save(user);

        if (auditService != null) {
            auditService.record(traceId, actorUserId, "USER_REACTIVATED", "USER", saved.getId(),
                    Map.of("email", saved.getEmail(), "role", saved.getRole().name()));
        }

        return toResponse(saved);
    }

    @Transactional
    public void deleteUser(UUID companyId, UUID id) {
        deleteUser(companyId, id, null, null);
    }

    @Transactional
    public void deleteUser(UUID companyId, UUID id, UUID actorUserId, UUID traceId) {
        User user = userRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (user.getRole() == UserRole.ADMIN) {
            checkFinalAdminProtection(companyId, id, "delete");
        }

        userRepository.delete(user);

        if (auditService != null) {
            auditService.record(traceId, actorUserId, "USER_DELETED", "USER", id,
                    Map.of("email", user.getEmail()));
        }
    }

    private void checkFinalAdminProtection(UUID companyId, UUID userId, String action) {
        long activeAdminCount = userRepository.countActiveByCompanyIdAndRole(companyId, UserRole.ADMIN);
        if (activeAdminCount <= 1) {
            throw new ResourceConflictException("LAST_ADMIN_PROTECTED",
                    "Cannot " + action + " the last active administrator of the company");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse toResponse(User user) {
        String invitationStatus = "INVITED".equals(user.getStatus()) ? "PENDING" : user.getStatus();
        if (invitationRepository != null) {
            Optional<UserInvitation> inv = invitationRepository.findFirstByUserIdAndAcceptedAtIsNullOrderByCreatedAtDesc(user.getId());
            if (inv.isPresent()) {
                invitationStatus = "INVITED";
            }
        }
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
                invitationStatus
        );
    }
}
