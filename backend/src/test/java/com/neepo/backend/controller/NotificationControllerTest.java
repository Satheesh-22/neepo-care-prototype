package com.neepo.backend.controller;

import com.neepo.backend.domain.NotificationChannelType;
import com.neepo.backend.entity.NotificationChannel;
import com.neepo.backend.repository.NotificationChannelRepository;
import com.neepo.backend.repository.NotificationDeliveryLogRepository;
import com.neepo.contracts.DisruptionSeverity;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerTest {

    private static final UUID COMPANY_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID OTHER_COMPANY_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationChannelRepository channelRepository;

    @Autowired
    private NotificationDeliveryLogRepository deliveryLogRepository;

    @BeforeEach
    void setUp() {
        deliveryLogRepository.deleteAll();
        channelRepository.deleteAll();
    }

    @Test
    void managesNotificationChannelsWithTenantIsolation() throws Exception {
        // 1. Create channel as DISPATCHER
        String createResponse = mockMvc.perform(post("/api/v1/notifications/channels")
                        .with(dispatcher(COMPANY_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Fleet Dispatch Webhook",
                                  "channelType": "WEBHOOK",
                                  "targetEndpoint": "https://alerts.myfleet.com/neepo",
                                  "secretKey": "my_super_secret_webhook_token_2026",
                                  "minimumSeverity": "HIGH",
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Fleet Dispatch Webhook"))
                .andExpect(jsonPath("$.data.channelType").value("WEBHOOK"))
                .andExpect(jsonPath("$.data.minimumSeverity").value("HIGH"))
                .andExpect(jsonPath("$.data.secretKey").value("my_s...2026")) // masked
                .andReturn().getResponse().getContentAsString();

        String channelId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(createResponse).at("/data/id").asText();

        // 2. List channels for company -> 1 found
        mockMvc.perform(get("/api/v1/notifications/channels")
                        .with(dispatcher(COMPANY_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(channelId))
                .andExpect(jsonPath("$.data[0].secretKey").value("my_s...2026"));

        // 3. Other company cannot see this channel
        mockMvc.perform(get("/api/v1/notifications/channels")
                        .with(dispatcher(OTHER_COMPANY_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        // 4. Test notification ping
        mockMvc.perform(post("/api/v1/notifications/channels/{channelId}/test", channelId)
                        .with(dispatcher(COMPANY_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.channelId").value(channelId))
                .andExpect(jsonPath("$.data.signature").isString())
                .andExpect(jsonPath("$.data.payload").isString());

        // 5. Query delivery logs
        mockMvc.perform(get("/api/v1/notifications/logs")
                        .with(dispatcher(COMPANY_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].channelId").value(channelId))
                .andExpect(jsonPath("$.data[0].disruptionType").value("TEST_PING"));

        // 6. Delete channel
        mockMvc.perform(delete("/api/v1/notifications/channels/{channelId}", channelId)
                        .with(dispatcher(COMPANY_ID)))
                .andExpect(status().isNoContent());

        // 7. Verify deletion
        mockMvc.perform(get("/api/v1/notifications/channels")
                        .with(dispatcher(COMPANY_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void rejectsDriverRoleFromCreatingChannels() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/channels")
                        .with(driver(COMPANY_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Driver Trying to Webhook",
                                  "channelType": "WEBHOOK",
                                  "targetEndpoint": "https://bad.actor.com",
                                  "minimumSeverity": "LOW"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    private RequestPostProcessor dispatcher(UUID companyId) {
        return jwt().jwt(token -> token.claim("companyId", companyId.toString()).claim("role", "DISPATCHER"))
                .authorities(new SimpleGrantedAuthority("ROLE_DISPATCHER"));
    }

    private RequestPostProcessor driver(UUID companyId) {
        return jwt().jwt(token -> token.claim("companyId", companyId.toString()).claim("role", "DRIVER"))
                .authorities(new SimpleGrantedAuthority("ROLE_DRIVER"));
    }
}
