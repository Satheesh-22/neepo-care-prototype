package com.neepo.backend.controller;

import com.neepo.backend.domain.UserRole;
import com.neepo.backend.domain.VehicleStatus;
import com.neepo.backend.entity.Driver;
import com.neepo.backend.entity.User;
import com.neepo.backend.entity.Vehicle;
import com.neepo.backend.repository.DriverRepository;
import com.neepo.backend.repository.DriverVehicleAssignmentRepository;
import com.neepo.backend.repository.UserRepository;
import com.neepo.backend.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
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
class AssignmentControllerTest {
    private static final UUID COMPANY_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID ADMIN_USER_ID = UUID.fromString("00000000-0000-4000-8100-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DriverVehicleAssignmentRepository assignmentRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.neepo.backend.repository.CompanyRepository companyRepository;

    private UUID activeCompanyId;

    @BeforeEach
    void setUp() {
        assignmentRepository.deleteAll();
        driverRepository.deleteAll();
        vehicleRepository.deleteAll();
        userRepository.deleteAll();
        companyRepository.deleteAll();

        com.neepo.backend.entity.Company company = com.neepo.backend.entity.Company.create(
                "NEEPO Fleet Corp", "NEEPO-FLT", "NEEPO Fleet Logistics Inc.",
                "NEEPO Fleet", "REG-NEEPO-FLT", "Logistics",
                "100 Market St", "San Francisco", "CA", "US", "94105",
                "Fleet Director", "admin@neepoflt.local", "+15550100000",
                "America/Los_Angeles", "METRIC", "STRICT_COLD_CHAIN", null, 90
        );
        company = companyRepository.save(company);
        activeCompanyId = company.getId();
    }

    @Test
    void createAndReleaseAssignmentFlow() throws Exception {
        User user = userRepository.save(User.createForCompany(
                activeCompanyId, "Assignee Driver", "assignee@test.com", "+15550119999", UserRole.DRIVER, "unusable"
        ));

        Driver driver = Driver.createDetailed(
                activeCompanyId, user.getId(), "EMP-004", "Assignee", "Driver", "assignee@test.com", "+15550119999",
                "DL-990011", "HEAVY_RIGID", LocalDate.now().plusYears(1),
                "Emergency Contact", "+15550119999", "San Francisco Hub", "FULL_TIME", null, "VERIFIED", 600
        );
        driver = driverRepository.save(driver);

        Vehicle vehicle = Vehicle.createDetailed(
                activeCompanyId, "IL-8877-TRK", "1HGCR2F8XEA554433", "Volvo", "VNL", 2025,
                "HEAVY_TRUCK", BigDecimal.valueOf(3.8), BigDecimal.valueOf(2.5),
                BigDecimal.valueOf(20000.0), BigDecimal.valueOf(10000.0),
                BigDecimal.valueOf(105.0), true, null, null, "DEV-004", VehicleStatus.AVAILABLE
        );
        vehicle = vehicleRepository.save(vehicle);

        // 1. Create Assignment
        mockMvc.perform(post("/api/v1/assignments").with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "driverId": "%s",
                                  "vehicleId": "%s",
                                  "notes": "Testing assignment release cycle"
                                }
                                """, driver.getId(), vehicle.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.driverName").value("Assignee Driver"))
                .andExpect(jsonPath("$.data.vehicleRegistrationNumber").value("IL-8877-TRK"));

        // 2. Query Active Assignments
        mockMvc.perform(get("/api/v1/assignments").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));

        // Extract ID
        UUID assignmentId = assignmentRepository.findAll().get(0).getId();

        // 3. Release Assignment
        mockMvc.perform(put("/api/v1/assignments/{id}/release", assignmentId).with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "releaseNotes": "Shift completed smoothly."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RELEASED"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
        return jwt().jwt(token -> {
            token.claim("companyId", activeCompanyId.toString());
            token.subject(ADMIN_USER_ID.toString());
            token.claim("role", "ADMIN");
        }).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
