package com.neepo.backend.service;

import com.neepo.backend.api.PageResponse;
import com.neepo.backend.dto.CreateUserRequest;
import com.neepo.backend.dto.UserResponse;
import com.neepo.backend.entity.User;
import com.neepo.backend.exception.EmailAlreadyExistsException;
import com.neepo.backend.exception.UserNotFoundException;
import com.neepo.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException("Email is already registered");
        }

        User user = User.create(
                request.name().trim(),
                normalizedEmail,
                request.phone().trim(),
                request.role()
        );

        return toResponse(userRepository.save(user));
    }

    public PageResponse<UserResponse> getUsers(int page, int size) {
        Page<UserResponse> users = userRepository.findAll(
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name", "id")))
                .map(this::toResponse);
        return PageResponse.from(users);
    }

    public UserResponse getUserById(UUID id) {
        return userRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public UserResponse getUserByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .map(this::toResponse)
                .orElseThrow(UserNotFoundException::forEmail);
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        userRepository.delete(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
