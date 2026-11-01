package com.neepo.backend.controller;

import com.neepo.backend.domain.IdpType;
import com.neepo.backend.domain.UserRole;
import com.neepo.backend.entity.Company;
import com.neepo.backend.entity.CompanySsoConfig;
import com.neepo.backend.entity.User;
import com.neepo.backend.repository.CompanyRepository;
import com.neepo.backend.repository.CompanySsoConfigRepository;
import com.neepo.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired CompanyRepository companyRepository;
    @Autowired CompanySsoConfigRepository ssoConfigRepository;

    @BeforeEach
    void setUp() {
        ssoConfigRepository.deleteAll();
        users.deleteAll();
        companyRepository.deleteAll();

        users.save(User.create("Admin", "admin@neepo.local", "+919999999990", UserRole.ADMIN,
                passwords.encode("StrongPassword123!")));
    }

    @Test
    void validCredentialsReturnSignedBearerToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"ADMIN@neepo.local","password":"StrongPassword123!"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"));
    }

    @Test
    void invalidCredentialsReturnStandard401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"admin@neepo.local","password":"wrong"}
                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void passwordLoginBlockedWhenSsoEnforced() throws Exception {
        Company company = Company.create(
                "Enforced Co", "ENF-CO", "Enforced Co Inc",
                "Enforced", "REG-ENF-1", "Pharma",
                "1 Main", "City", "State", "US", "10001",
                "Lead", "lead@enforced.com", "+10000000000",
                "UTC", "METRIC", "STRICT_COLD_CHAIN", null, 90
        );
        company = companyRepository.save(company);

        CompanySsoConfig ssoConfig = new CompanySsoConfig(
                company.getId(),
                IdpType.OIDC,
                "https://auth.enforced.com",
                "client-enf",
                "secret",
                "enforced.com"
        );
        ssoConfig.setEnabled(true);
        ssoConfig.setEnforceSso(true);
        ssoConfigRepository.save(ssoConfig);

        users.save(User.createForCompany(company.getId(), "Enforced User", "user@enforced.com", "+10000000001", UserRole.DISPATCHER,
                passwords.encode("StrongPassword123!")));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"user@enforced.com","password":"StrongPassword123!"}
                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.error.message").value(org.hamcrest.Matchers.containsString("Single Sign-On (SSO) is enforced")));
    }

    @Test
    void protectedEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
