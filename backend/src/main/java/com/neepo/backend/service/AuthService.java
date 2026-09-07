package com.neepo.backend.service;

import com.neepo.backend.dto.LoginRequest;
import com.neepo.backend.dto.LoginResponse;
import com.neepo.backend.dto.UserResponse;
import com.neepo.backend.entity.User;
import com.neepo.backend.exception.InvalidCredentialsException;
import com.neepo.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
@Transactional
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final Duration tokenLifetime;
    private final String issuer;

    public AuthService(UserRepository users, PasswordEncoder passwords, JwtEncoder jwtEncoder,
                       Clock clock,
                       @Value("${neepo.security.jwt-expiration-minutes}") long expirationMinutes,
                       @Value("${neepo.security.issuer}") String issuer) {
        this.users = users;
        this.passwords = passwords;
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.tokenLifetime = Duration.ofMinutes(expirationMinutes);
        this.issuer = issuer;
    }

    public LoginResponse login(LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim().toLowerCase(Locale.ROOT))
                .filter(User::isEnabled)
                .filter(candidate -> passwords.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        Instant issuedAt = clock.instant();
        user.recordActive(issuedAt);
        users.save(user);

        Instant expiresAt = issuedAt.plus(tokenLifetime);
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer(issuer).issuedAt(issuedAt).expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name());
        if (user.getCompanyId() != null) {
            claimsBuilder.claim("companyId", user.getCompanyId().toString());
        }
        JwtClaimsSet claims = claimsBuilder.build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
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
        return new LoginResponse(token, expiresAt, responseUser);
    }
}
