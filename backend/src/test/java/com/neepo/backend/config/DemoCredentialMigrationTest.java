package com.neepo.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoCredentialMigrationTest {
    @Test
    void migrationContainsVerifiedDemoPasswordHash() throws IOException {
        String migration;
        try (var input = getClass().getResourceAsStream(
                "/db/migration/V6__correct_demo_user_credentials.sql")) {
            if (input == null) {
                throw new IOException("V6 demo credential migration is missing");
            }
            migration = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        int hashStart = migration.indexOf("$2a$");
        String hash = migration.substring(hashStart, hashStart + 60);
        assertTrue(new BCryptPasswordEncoder().matches("ChangeMe123!", hash));
        assertTrue(migration.contains("dispatcher@neepo.local"));
        assertTrue(migration.contains("driver@neepo.local"));
    }
}
