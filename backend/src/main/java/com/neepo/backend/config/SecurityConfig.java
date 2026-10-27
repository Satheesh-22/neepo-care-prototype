package com.neepo.backend.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/users/invitations/accept",
                                "/ws",
                                "/actuator/health/**",
                                "/actuator/info"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/company", "/api/v1/company/me").hasAnyRole("ADMIN", "DISPATCHER", "DRIVER")
                        .requestMatchers("/api/v1/company/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/users/**").hasAnyRole("ADMIN", "DISPATCHER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/drivers/me").hasAnyRole("ADMIN", "DISPATCHER", "DRIVER")
                        .requestMatchers("/api/v1/drivers/**").hasAnyRole("ADMIN", "DISPATCHER")
                        .requestMatchers("/api/v1/vehicles/**").hasAnyRole("ADMIN", "DISPATCHER")
                        .requestMatchers("/api/v1/assignments/**").hasAnyRole("ADMIN", "DISPATCHER")
                        .requestMatchers("/api/v1/cargo/**").hasAnyRole("ADMIN", "DISPATCHER")
                        .requestMatchers("/api/v1/providers/**").hasAnyRole("ADMIN", "DISPATCHER")
                        .requestMatchers("/api/v1/notifications/**").hasAnyRole("ADMIN", "DISPATCHER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/trips/**")
                        .hasAnyRole("ADMIN", "DISPATCHER", "DRIVER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/trips/*/telemetry-policy")
                        .hasAnyRole("ADMIN", "DISPATCHER", "DRIVER")
                        .requestMatchers("/api/v1/trips/**").hasAnyRole("ADMIN", "DISPATCHER")
                        .requestMatchers("/api/v1/recovery-plans/**").hasAnyRole("ADMIN", "DISPATCHER")
                        .requestMatchers("/api/v1/telemetry").hasAnyRole("ADMIN", "DISPATCHER", "DRIVER")
                        .requestMatchers("/api/v1/disruptions").hasAnyRole("ADMIN", "DISPATCHER", "DRIVER")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(response, 401, "UNAUTHORIZED",
                                        "Authentication is required.", TraceIdFilter.traceId(request))))
                .exceptionHandling(errors -> errors.accessDeniedHandler((request, response, exception) ->
                        writeError(response, 403, "FORBIDDEN",
                                "You do not have permission to perform this operation.",
                                TraceIdFilter.traceId(request))));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecretKey jwtSecretKey(@Value("${neepo.security.jwt-secret}") String secret) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        }
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey secretKey) {
        return NimbusJwtEncoder.withSecretKey(secretKey).build();
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey secretKey) {
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("role");
        roles.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    private void writeError(HttpServletResponse response, int status,
                            String code, String message, String traceId) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"success\":false,\"error\":{\"code\":\"" + code
                + "\",\"message\":\"" + message + "\",\"fieldErrors\":[]},\"timestamp\":\""
                + Instant.now() + "\",\"traceId\":\"" + traceId + "\"}");
    }
}
