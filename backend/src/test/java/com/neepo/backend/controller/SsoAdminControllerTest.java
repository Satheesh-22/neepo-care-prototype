package com.neepo.backend.controller;

import com.neepo.backend.domain.IdpType;
import com.neepo.backend.domain.UserRole;
import com.neepo.backend.entity.Company;
import com.neepo.backend.entity.CompanySsoConfig;
import com.neepo.backend.repository.CompanyRepository;
import com.neepo.backend.repository.CompanySsoConfigRepository;
import com.neepo.backend.repository.SsoAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class SsoAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanySsoConfigRepository ssoConfigRepository;

    @Autowired
    private SsoAuditLogRepository auditLogRepository;

    private Company testCompany;
    private CompanySsoConfig testConfig;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        ssoConfigRepository.deleteAll();
        companyRepository.deleteAll();

        testCompany = Company.create(
                "Nordic Cold Pharma", "NORD-PHARMA", "Nordic Cold Pharma AB",
                "Nordic Pharma", "REG-NORDIC-777", "Biomedical Cold Chain",
                "45 Vasagatan", "Stockholm", "Stockholm", "SE", "11120",
                "IT Director", "admin@nordic-pharma.se", "+4681234567",
                "Europe/Stockholm", "METRIC", "STRICT_COLD_CHAIN", null, 90
        );
        testCompany = companyRepository.save(testCompany);

        testConfig = new CompanySsoConfig(
                testCompany.getId(),
                IdpType.OIDC,
                "https://sso.nordic-pharma.se",
                "nordic-client-999",
                "hash_secret_nordic",
                "nordic-pharma.se"
        );
        testConfig.setEnabled(true);
        testConfig.setEnforceSso(false);
        testConfig.setJitProvisioningEnabled(true);
        testConfig.setDefaultRole(UserRole.DISPATCHER);
        testConfig = ssoConfigRepository.save(testConfig);
    }

    @Test
    void getCompanySsoConfigReturnsConfigForAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sso").with(jwtForRole(testCompany.getId(), "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ssoDomain").value("nordic-pharma.se"))
                .andExpect(jsonPath("$.data.idpType").value("OIDC"))
                .andExpect(jsonPath("$.data.jitProvisioningEnabled").value(true));
    }

    @Test
    void getCompanySsoConfigAsDispatcherReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sso").with(jwtForRole(testCompany.getId(), "ROLE_DISPATCHER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateCompanySsoConfigUpdatesFieldsSuccessfully() throws Exception {
        mockMvc.perform(put("/api/v1/admin/sso").with(jwtForRole(testCompany.getId(), "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idpType": "SAML2",
                                  "idpIssuerUrl": "https://saml.nordic-pharma.se/idp",
                                  "clientId": "updated-client-id",
                                  "clientSecret": "new_super_secret_key",
                                  "ssoDomain": "nordic-pharma.se",
                                  "enabled": true,
                                  "enforceSso": true,
                                  "jitProvisioningEnabled": true,
                                  "defaultRole": "ADMIN",
                                  "metadata": {
                                    "entityId": "https://care.neepo.com/saml/metadata"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.idpType").value("SAML2"))
                .andExpect(jsonPath("$.data.enforceSso").value(true))
                .andExpect(jsonPath("$.data.defaultRole").value("ADMIN"))
                .andExpect(jsonPath("$.data.metadata.entityId").value("https://care.neepo.com/saml/metadata"));
    }

    @Test
    void updateCompanySsoConfigWithConflictingDomainReturns409() throws Exception {
        Company otherCompany = Company.create(
                "Other Co", "OTHER-CO", "Other Co Ltd",
                "Other", "REG-OTHER-123", "Transport",
                "1 Street", "City", "State", "US", "10001",
                "Admin", "admin@other.com", "+10000000000",
                "UTC", "METRIC", "STRICT_COLD_CHAIN", null, 90
        );
        otherCompany = companyRepository.save(otherCompany);

        CompanySsoConfig otherConfig = new CompanySsoConfig(
                otherCompany.getId(),
                IdpType.OIDC,
                "https://auth.other.com",
                "other-client",
                "hash",
                "claimed-domain.com"
        );
        ssoConfigRepository.save(otherConfig);

        mockMvc.perform(put("/api/v1/admin/sso").with(jwtForRole(testCompany.getId(), "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idpType": "OIDC",
                                  "idpIssuerUrl": "https://auth.nordic.com",
                                  "clientId": "client-id",
                                  "ssoDomain": "claimed-domain.com",
                                  "enabled": true,
                                  "enforceSso": false,
                                  "jitProvisioningEnabled": true,
                                  "defaultRole": "DISPATCHER"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DOMAIN_CONFLICT"));
    }

    @Test
    void testSsoConfigurationReturnsSuccessResult() throws Exception {
        mockMvc.perform(post("/api/v1/admin/sso/test").with(jwtForRole(testCompany.getId(), "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.idpType").value("OIDC"))
                .andExpect(jsonPath("$.data.latencyMs").isNumber());
    }

    @Test
    void listSsoAuditLogsReturnsRecordedEvents() throws Exception {
        mockMvc.perform(get("/api/v1/admin/sso/audit-logs").with(jwtForRole(testCompany.getId(), "ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    private RequestPostProcessor jwtForRole(UUID companyId, String role) {
        return jwt().jwt(token -> {
            token.claim("companyId", companyId.toString());
            token.claim("sub", UUID.randomUUID().toString());
            token.claim("role", role.replace("ROLE_", ""));
            token.claim("email", "admin@company.test");
        }).authorities(new SimpleGrantedAuthority(role));
    }
}

