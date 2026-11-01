package com.neepo.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neepo.backend.domain.DepotStatus;
import com.neepo.backend.dto.CreateDepotRequest;
import com.neepo.backend.dto.UpdateDepotRequest;
import com.neepo.backend.entity.Company;
import com.neepo.backend.entity.Depot;
import com.neepo.backend.repository.CompanyRepository;
import com.neepo.backend.repository.DepotRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class DepotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private DepotRepository depotRepository;

    private Company testCompany;
    private Depot testDepot;

    @BeforeEach
    void setUp() {
        depotRepository.deleteAll();
        companyRepository.deleteAll();
        String rand = UUID.randomUUID().toString().substring(0, 8);
        testCompany = Company.create(
                "Depot Logistics " + rand, "DEPOT-CORP-" + rand, "Depot Logistics Ltd",
                "Depot", "REG-DEPOT-" + rand, "Fleet Management",
                "100 Pier Way", "Seattle", "WA", "US", "98101",
                "Operations Mgr", "ops" + rand + "@depot.com", "+12065551234",
                "America/Los_Angeles", "METRIC", "STANDARD", null, 90
        );
        testCompany = companyRepository.save(testCompany);

        testDepot = new Depot();
        testDepot.setCompanyId(testCompany.getId());
        testDepot.setCode("DEPOT-TST-01");
        testDepot.setName("Test Seattle Hub");
        testDepot.setAddress("123 Warehouse Way");
        testDepot.setCity("Seattle");
        testDepot.setState("WA");
        testDepot.setPostalCode("98101");
        testDepot.setLatitude(47.6062);
        testDepot.setLongitude(-122.3321);
        testDepot.setCapacity(40);
        testDepot.setStatus(DepotStatus.ACTIVE);
        testDepot.setContactPhone("+12065551122");
        testDepot.setContactEmail("test-depot@neepo.com");
        testDepot = depotRepository.save(testDepot);
    }

    private RequestPostProcessor adminJwt() {
        return jwt().jwt(builder -> builder
                .subject(UUID.randomUUID().toString())
                .claim("companyId", testCompany.getId().toString())
                .claim("role", "ADMIN")
        ).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private RequestPostProcessor driverJwt() {
        return jwt().jwt(builder -> builder
                .subject(UUID.randomUUID().toString())
                .claim("companyId", testCompany.getId().toString())
                .claim("role", "DRIVER")
        ).authorities(new SimpleGrantedAuthority("ROLE_DRIVER"));
    }

    @Test
    void listDepotsReturnsListOfDepots() throws Exception {
        mockMvc.perform(get("/api/v1/depots").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].code").value("DEPOT-TST-01"))
                .andExpect(jsonPath("$[0].city").value("Seattle"));
    }

    @Test
    void getDepotByIdReturnsDepot() throws Exception {
        mockMvc.perform(get("/api/v1/depots/" + testDepot.getId()).with(driverJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testDepot.getId().toString()))
                .andExpect(jsonPath("$.name").value("Test Seattle Hub"))
                .andExpect(jsonPath("$.capacity").value(40));
    }

    @Test
    void adminCanCreateDepot() throws Exception {
        CreateDepotRequest request = new CreateDepotRequest(
                "DEPOT-NEW-02",
                "Spokane Cold Logistics",
                "500 Industry Blvd",
                "Spokane",
                "WA",
                "99201",
                47.6588,
                -117.4260,
                25,
                DepotStatus.ACTIVE,
                "+15095551234",
                "spokane@neepo.com"
        );

        mockMvc.perform(post("/api/v1/admin/depots")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("DEPOT-NEW-02"))
                .andExpect(jsonPath("$.name").value("Spokane Cold Logistics"))
                .andExpect(jsonPath("$.city").value("Spokane"));
    }

    @Test
    void duplicateDepotCodeReturnsConflict() throws Exception {
        CreateDepotRequest request = new CreateDepotRequest(
                "DEPOT-TST-01",
                "Duplicate Hub",
                "999 Main St",
                "Seattle",
                "WA",
                "98101",
                47.60,
                -122.33,
                50,
                DepotStatus.ACTIVE,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/admin/depots")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void adminCanUpdateDepot() throws Exception {
        UpdateDepotRequest request = new UpdateDepotRequest(
                null,
                "Updated Seattle Facility",
                null,
                null,
                null,
                null,
                null,
                null,
                65,
                DepotStatus.MAINTENANCE,
                null,
                null
        );

        mockMvc.perform(put("/api/v1/admin/depots/" + testDepot.getId())
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Seattle Facility"))
                .andExpect(jsonPath("$.capacity").value(65))
                .andExpect(jsonPath("$.status").value("MAINTENANCE"));
    }

    @Test
    void driverCannotCreateOrDeleteDepot() throws Exception {
        CreateDepotRequest request = new CreateDepotRequest(
                "DEPOT-FAIL", "Fail Hub", "123 Way", "City", "WA", "00000",
                47.0, -122.0, 10, DepotStatus.ACTIVE, null, null
        );

        mockMvc.perform(post("/api/v1/admin/depots")
                        .with(driverJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/admin/depots/" + testDepot.getId())
                        .with(driverJwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanDeleteDepot() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/depots/" + testDepot.getId())
                        .with(adminJwt()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/depots/" + testDepot.getId()).with(adminJwt()))
                .andExpect(status().isNotFound());
    }
}
