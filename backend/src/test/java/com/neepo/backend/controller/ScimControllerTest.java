package com.neepo.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neepo.backend.dto.ScimPatchOpRequest;
import com.neepo.backend.dto.ScimUserDto;
import com.neepo.backend.entity.Company;
import com.neepo.backend.entity.CompanySsoConfig;
import com.neepo.backend.repository.CompanyRepository;
import com.neepo.backend.repository.CompanySsoConfigRepository;
import com.neepo.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class ScimControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanySsoConfigRepository ssoConfigRepository;

    @Autowired
    private UserRepository userRepository;

    private Company testCompany;
    private CompanySsoConfig testConfig;

    @BeforeEach
    void setUp() {
        ssoConfigRepository.deleteAll();
        companyRepository.deleteAll();
        String rand = UUID.randomUUID().toString().substring(0, 8);
        testCompany = Company.create(
                "SCIM Enterprise " + rand, "SCIM-CORP-" + rand, "SCIM Enterprise Corp",
                "SCIM", "REG-SCIM-" + rand, "Enterprise Logistics",
                "200 Cloud Way", "Redmond", "WA", "US", "98052",
                "Security Admin", "security" + rand + "@scim-enterprise.com", "+14255551234",
                "America/Los_Angeles", "METRIC", "STANDARD", null, 90
        );
        testCompany = companyRepository.save(testCompany);

        testConfig = new CompanySsoConfig();
        testConfig.setCompanyId(testCompany.getId());
        testConfig.setSsoDomain("scim-test-" + rand + ".com");
        testConfig.setClientId("scim-client-" + rand);
        testConfig.setClientSecretHash("secret-hash");
        testConfig.setIdpIssuerUrl("https://login.scim-test.com");
        testConfig = ssoConfigRepository.save(testConfig);
    }

    private RequestPostProcessor adminJwt() {
        return jwt().jwt(builder -> builder
                .subject(UUID.randomUUID().toString())
                .claim("companyId", testCompany.getId().toString())
                .claim("role", "ADMIN")
        ).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Test
    void getServiceProviderConfigReturnsRfcSpec() throws Exception {
        mockMvc.perform(get("/api/v1/scim/v2/ServiceProviderConfig"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemas[0]").value("urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig"))
                .andExpect(jsonPath("$.patch.supported").value(true));
    }

    @Test
    void getSchemasReturnsCoreSchemas() throws Exception {
        mockMvc.perform(get("/api/v1/scim/v2/Schemas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemas[0]").value("urn:ietf:params:scim:api:messages:2.0:ListResponse"))
                .andExpect(jsonPath("$.totalResults").value(1));
    }

    @Test
    void adminCanGenerateAndRevokeScimToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/scim/token").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.enabled").value(true))
                .andReturn();

        String rawToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();

        // Check SCIM status
        mockMvc.perform(get("/api/v1/admin/scim/status").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        // Use token to access SCIM Users endpoint
        mockMvc.perform(get("/api/v1/scim/v2/Users")
                        .header("Authorization", "Bearer " + rawToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schemas[0]").value("urn:ietf:params:scim:api:messages:2.0:ListResponse"));

        // Revoke token
        mockMvc.perform(delete("/api/v1/admin/scim/token").with(adminJwt()))
                .andExpect(status().isNoContent());

        // Now access with revoked token should fail with 401
        mockMvc.perform(get("/api/v1/scim/v2/Users")
                        .header("Authorization", "Bearer " + rawToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void scimUserLifecycleProvisioning() throws Exception {
        // Generate active token
        MvcResult result = mockMvc.perform(post("/api/v1/admin/scim/token").with(adminJwt()))
                .andExpect(status().isOk())
                .andReturn();
        String rawToken = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();

        // 1. Create User via SCIM
        String uniqueEmail = "scim.user." + UUID.randomUUID().toString().substring(0, 8) + "@neepo.local";
        ScimUserDto newDto = new ScimUserDto(
                List.of("urn:ietf:params:scim:schemas:core:2.0:User"),
                null,
                "ext-okta-" + UUID.randomUUID().toString().substring(0, 6),
                uniqueEmail,
                new ScimUserDto.Name("Sarah Connor", "Connor", "Sarah"),
                List.of(new ScimUserDto.Email(uniqueEmail, true, "work")),
                List.of(new ScimUserDto.PhoneNumber("+12065559876", "work")),
                true,
                "DISPATCHER",
                null
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/scim/v2/Users")
                        .header("Authorization", "Bearer " + rawToken)
                        .contentType("application/scim+json")
                        .content(objectMapper.writeValueAsString(newDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userName").value(uniqueEmail))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        JsonNode createdNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String userId = createdNode.get("id").asText();

        // 2. Fetch created user
        mockMvc.perform(get("/api/v1/scim/v2/Users/" + userId)
                        .header("Authorization", "Bearer " + rawToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.userName").value(uniqueEmail));

        // 3. Patch user (deprovision: active = false)
        ScimPatchOpRequest patchRequest = new ScimPatchOpRequest(
                List.of("urn:ietf:params:scim:api:messages:2.0:PatchOp"),
                List.of(new ScimPatchOpRequest.Operation("replace", "active", false))
        );

        mockMvc.perform(patch("/api/v1/scim/v2/Users/" + userId)
                        .header("Authorization", "Bearer " + rawToken)
                        .contentType("application/scim+json")
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // 4. Delete user (soft deactivation)
        mockMvc.perform(delete("/api/v1/scim/v2/Users/" + userId)
                        .header("Authorization", "Bearer " + rawToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void unauthenticatedScimRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/scim/v2/Users")
                        .header("Authorization", "Bearer invalid-dummy-token"))
                .andExpect(status().isUnauthorized());
    }
}
