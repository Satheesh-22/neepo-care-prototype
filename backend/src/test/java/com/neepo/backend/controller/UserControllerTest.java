package com.neepo.backend.controller;

import com.neepo.backend.domain.UserRole;
import com.neepo.backend.entity.User;
import com.neepo.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void createUserReturnsStandard201ResponseAndNormalizesInput() throws Exception {
        String traceId = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/v1/users")
                        .header("X-Trace-Id", traceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "  Demo Dispatcher  ",
                                  "email": "DISPATCHER@EXAMPLE.COM",
                                  "phone": "+919999999999",
                                  "role": "DISPATCHER"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Trace-Id", traceId))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.name").value("Demo Dispatcher"))
                .andExpect(jsonPath("$.data.email").value("dispatcher@example.com"))
                .andExpect(jsonPath("$.data.role").value("DISPATCHER"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.traceId").value(traceId));
    }

    @Test
    void duplicateEmailIsCaseInsensitiveAndReturns409() throws Exception {
        userRepository.save(User.create(
                "Existing User",
                "duplicate@example.com",
                "9999999999",
                UserRole.DRIVER
        ));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Another User",
                                  "email": "DUPLICATE@example.com",
                                  "phone": "8888888888",
                                  "role": "DRIVER"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.error.fieldErrors", hasSize(0)));
    }

    @Test
    void invalidBodyReturnsDeterministicFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "email": "not-an-email",
                                  "phone": "123",
                                  "role": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fieldErrors[*].field",
                        hasItems("name", "email", "phone", "role")));
    }

    @Test
    void unknownRoleReturns400InsteadOf500() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Unknown Role",
                                  "email": "role@example.com",
                                  "phone": "9999999999",
                                  "role": "USER"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void listUsersReturnsPageEnvelope() throws Exception {
        userRepository.save(User.create("Alice", "alice@example.com", "9999999999", UserRole.DISPATCHER));
        userRepository.save(User.create("Bob", "bob@example.com", "8888888888", UserRole.DRIVER));

        mockMvc.perform(get("/api/v1/users?page=0&size=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name").value("Alice"))
                .andExpect(jsonPath("$.data.metadata.totalElements").value(2))
                .andExpect(jsonPath("$.data.metadata.totalPages").value(2));
    }

    @Test
    void invalidPaginationReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/users?page=-1&size=101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getUnknownUserReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
    }

    @Test
    void deleteExistingUserReturns204() throws Exception {
        User saved = userRepository.save(User.create(
                "Delete Me",
                "delete@example.com",
                "9999999999",
                UserRole.DRIVER
        ));

        mockMvc.perform(delete("/api/v1/users/{id}", saved.getId()))
                .andExpect(status().isNoContent());
    }
}
