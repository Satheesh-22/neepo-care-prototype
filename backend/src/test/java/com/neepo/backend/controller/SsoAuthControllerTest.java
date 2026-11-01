package com.neepo.backend.controller;

import com.neepo.backend.domain.IdpType;
import com.neepo.backend.domain.UserRole;
import com.neepo.backend.entity.Company;
import com.neepo.backend.entity.CompanySsoConfig;
import com.neepo.backend.entity.User;
import com.neepo.backend.repository.CompanyRepository;
import com.neepo.backend.repository.CompanySsoConfigRepository;
import com.neepo.backend.repository.SsoAuditLogRepository;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class SsoAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanySsoConfigRepository ssoConfigRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SsoAuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Company testCompany;
    private CompanySsoConfig testConfig;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        ssoConfigRepository.deleteAll();
        userRepository.deleteAll();
        companyRepository.deleteAll();

        testCompany = Company.create(
                "Acme Pharma Logistics", "ACME-LOG", "Acme Pharma Logistics Inc.",
                "Acme Logistics", "REG-ACME-001", "Pharmaceutical Transport",
                "100 Innovation Way", "Boston", "MA", "US", "02110",
                "Compliance Officer", "admin@acme-pharma.com", "+15550112233",
                "America/New_York", "METRIC", "STRICT_COLD_CHAIN", null, 90
        );
        testCompany = companyRepository.save(testCompany);

        testConfig = new CompanySsoConfig(
                testCompany.getId(),
                IdpType.OIDC,
                "https://auth.acme-pharma.com/oauth2",
                "acme-client-id-xyz",
                "secret_hash_12345",
                "acme-pharma.com"
        );
        testConfig.setEnabled(true);
        testConfig.setEnforceSso(false);
        testConfig.setJitProvisioningEnabled(true);
        testConfig.setDefaultRole(UserRole.DISPATCHER);
        testConfig = ssoConfigRepository.save(testConfig);
    }

    @Test
    void discoverSsoWithRegisteredDomainReturnsMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/auth/sso/discover").param("domain", "acme-pharma.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ssoAvailable").value(true))
                .andExpect(jsonPath("$.data.domain").value("acme-pharma.com"))
                .andExpect(jsonPath("$.data.idpType").value("OIDC"))
                .andExpect(jsonPath("$.data.companyName").value("Acme Pharma Logistics"));
    }

    @Test
    void discoverSsoWithEmailReturnsMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/auth/sso/discover").param("email", "sarah.connor@acme-pharma.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ssoAvailable").value(true))
                .andExpect(jsonPath("$.data.domain").value("acme-pharma.com"));
    }

    @Test
    void discoverSsoWithUnregisteredDomainReturnsUnavailable() throws Exception {
        mockMvc.perform(get("/api/v1/auth/sso/discover").param("domain", "random-domain-not-registered.org"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ssoAvailable").value(false));
    }

    @Test
    void ssoCallbackWithValidTokenAndExistingUserReturnsSessionToken() throws Exception {
        User existing = User.createForCompany(
                testCompany.getId(),
                "Existing Dispatcher",
                "dispatcher@acme-pharma.com",
                "+15550001111",
                UserRole.DISPATCHER,
                passwordEncoder.encode("Password123!")
        );
        userRepository.save(existing);

        mockMvc.perform(post("/api/v1/auth/sso/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "dispatcher@acme-pharma.com",
                                  "idpToken": "mock_valid_idp_token_xyz987",
                                  "domain": "acme-pharma.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.isNewUser").value(false))
                .andExpect(jsonPath("$.data.user.email").value("dispatcher@acme-pharma.com"))
                .andExpect(jsonPath("$.data.idpType").value("OIDC"));
    }

    @Test
    void ssoCallbackWithJitProvisioningCreatesNewUser() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sso/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "dr.elena.rostova@acme-pharma.com",
                                  "idpToken": "mock_valid_idp_token_jwt_signature",
                                  "domain": "acme-pharma.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.isNewUser").value(true))
                .andExpect(jsonPath("$.data.user.email").value("dr.elena.rostova@acme-pharma.com"))
                .andExpect(jsonPath("$.data.user.role").value("DISPATCHER"));
    }

    @Test
    void ssoCallbackWithInvalidTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sso/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "someone@acme-pharma.com",
                                  "idpToken": "invalid",
                                  "domain": "acme-pharma.com"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void ssoCallbackWithJitDisabledRejectsNewUser() throws Exception {
        testConfig.setJitProvisioningEnabled(false);
        ssoConfigRepository.save(testConfig);

        mockMvc.perform(post("/api/v1/auth/sso/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "unknown.user@acme-pharma.com",
                                  "idpToken": "valid_token_sample",
                                  "domain": "acme-pharma.com"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }
}

