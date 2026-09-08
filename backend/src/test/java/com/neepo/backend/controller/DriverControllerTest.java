package com.neepo.backend.controller;

import com.neepo.backend.domain.UserRole;
import com.neepo.backend.entity.Company;
import com.neepo.backend.entity.Driver;
import com.neepo.backend.entity.User;
import com.neepo.backend.repository.CompanyRepository;
import com.neepo.backend.repository.DriverRepository;
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

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class DriverControllerTest {
    private static final UUID COMPANY_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID ADMIN_USER_ID = UUID.fromString("00000000-0000-4000-8100-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @BeforeEach
    void setUp() {
        driverRepository.deleteAll();
        userRepository.deleteAll();
        companyRepository.deleteAll();

        Company company = Company.create(
                "NEEPO Drivers Corp", "NEEPO-DRV", "NEEPO Drivers Logistics Inc.",
                "NEEPO Drivers", "REG-NEEPO-DRV", "Logistics",
                "100 Market St", "San Francisco", "CA", "US", "94105",
                "Fleet Director", "admin@neepodrv.local", "+15550100000",
                "America/Los_Angeles", "METRIC", "STRICT_COLD_CHAIN", null, 90
        );
        companyRepository.save(company);
    }

    @Test
    void onboardDriverCreatesUserAndDriverProfile() throws Exception {
        Company company = companyRepository.findAll().get(0);
        mockMvc.perform(post("/api/v1/drivers").with(adminJwt(company.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "John",
                                  "lastName": "Doe",
                                  "email": "johndoe@test.com",
                                  "phone": "+15550112233",
                                  "employeeId": "EMP-001",
                                  "licenseNumber": "DL-998877",
                                  "licenseCategory": "HEAVY_RIGID",
                                  "licenseExpiryDate": "2028-12-31",
                                  "employmentStatus": "FULL_TIME",
                                  "baseLocation": "San Francisco Hub",
                                  "maximumDailyDrivingMinutes": 600
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.licenseNumber").value("DL-998877"))
                .andExpect(jsonPath("$.data.licenseExpired").value(false));
    }

    @Test
    void listDriversReturnsDriversForCompany() throws Exception {
        Company company = companyRepository.findAll().get(0);
        User user = userRepository.save(User.createForCompany(
                company.getId(), "Jane Driver", "janedriver@test.com", "+15550112244", UserRole.DRIVER, "unusable"
        ));

        Driver driver = Driver.createDetailed(
                company.getId(), user.getId(), "EMP-002", "Jane", "Driver", "janedriver@test.com", "+15550112244",
                "DL-112233", "HEAVY_COMBINATION", LocalDate.now().plusYears(2),
                "Emergency Contact", "+15550119999", "San Francisco Hub", "FULL_TIME", null, "VERIFIED", 600
        );
        driverRepository.save(driver);

        mockMvc.perform(get("/api/v1/drivers").with(adminJwt(company.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].licenseNumber").value("DL-112233"))
                .andExpect(jsonPath("$.data[0].email").value("janedriver@test.com"));
    }

    @Test
    void suspendAndReactivateDriver() throws Exception {
        Company company = companyRepository.findAll().get(0);
        User user = userRepository.save(User.createForCompany(
                company.getId(), "Sam Driver", "samdriver@test.com", "+15550112255", UserRole.DRIVER, "unusable"
        ));

        Driver driver = Driver.createDetailed(
                company.getId(), user.getId(), "EMP-003", "Sam", "Driver", "samdriver@test.com", "+15550112255",
                "DL-334455", "HEAVY_RIGID", LocalDate.now().plusYears(1),
                "Emergency Contact", "+15550119999", "San Francisco Hub", "FULL_TIME", null, "VERIFIED", 600
        );
        driver = driverRepository.save(driver);

        mockMvc.perform(post("/api/v1/drivers/{id}/suspend", driver.getId()).with(adminJwt(company.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verificationState").value("SUSPENDED"));

        mockMvc.perform(post("/api/v1/drivers/{id}/reactivate", driver.getId()).with(adminJwt(company.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verificationState").value("VERIFIED"));
    }

    @Test
    void rejectDuplicateLicenseNumber() throws Exception {
        Company company = companyRepository.findAll().get(0);
        User user = userRepository.save(User.createForCompany(
                company.getId(), "Existing Driver", "existing@test.com", "+15550112288", UserRole.DRIVER, "unusable"
        ));

        Driver driver = Driver.createDetailed(
                company.getId(), user.getId(), "EMP-DUP-1", "Existing", "Driver", "existing@test.com", "+15550112288",
                "DL-DUP-999", "HEAVY_RIGID", LocalDate.now().plusYears(1),
                "Emergency Contact", "+15550119999", "San Francisco Hub", "FULL_TIME", null, "VERIFIED", 600
        );
        driverRepository.save(driver);

        mockMvc.perform(post("/api/v1/drivers").with(adminJwt(company.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "New",
                                  "lastName": "Driver",
                                  "email": "newdriver@test.com",
                                  "phone": "+15550112277",
                                  "employeeId": "EMP-DUP-2",
                                  "licenseNumber": "DL-DUP-999",
                                  "licenseCategory": "HEAVY_RIGID",
                                  "licenseExpiryDate": "2028-12-31"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DRIVER_LICENSE_EXISTS"));
    }

    @Test
    void driverRoleCannotOnboardDriver() throws Exception {
        Company company = companyRepository.findAll().get(0);
        mockMvc.perform(post("/api/v1/drivers").with(driverJwt(company.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Unauth",
                                  "lastName": "Driver",
                                  "email": "unauth@test.com",
                                  "phone": "+15550112266",
                                  "employeeId": "EMP-UNAUTH",
                                  "licenseNumber": "DL-UNAUTH",
                                  "licenseCategory": "HEAVY_RIGID",
                                  "licenseExpiryDate": "2028-12-31"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt(UUID companyId) {
        return jwt().jwt(token -> {
            token.claim("companyId", companyId.toString());
            token.subject(ADMIN_USER_ID.toString());
            token.claim("role", "ADMIN");
        }).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor driverJwt(UUID companyId) {
        return jwt().jwt(token -> {
            token.claim("companyId", companyId.toString());
            token.subject(UUID.randomUUID().toString());
            token.claim("role", "DRIVER");
        }).authorities(new SimpleGrantedAuthority("ROLE_DRIVER"));
    }
}
