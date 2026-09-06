package com.neepo.backend.controller;

import com.neepo.backend.dto.WebSocketTicketResponse;
import com.neepo.backend.service.WebSocketTicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class WebSocketTicketControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean WebSocketTicketService tickets;

    @Test
    void authenticatedUserCanIssueShortLivedTicket() throws Exception {
        when(tickets.issue(any())).thenReturn(new WebSocketTicketResponse(
                "06c86988-4f09-4a5f-8898-8fe033490738",
                Instant.parse("2026-08-24T13:00:30Z")
        ));

        mockMvc.perform(post("/api/v1/events/ticket").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticket").value("06c86988-4f09-4a5f-8898-8fe033490738"));
    }

    @Test
    void anonymousUserCannotIssueTicket() throws Exception {
        mockMvc.perform(post("/api/v1/events/ticket"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
