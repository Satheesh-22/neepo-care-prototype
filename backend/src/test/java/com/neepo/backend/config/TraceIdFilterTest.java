package com.neepo.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class TraceIdFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void invalidTraceIdHeaderIsReplacedWithGeneratedUuidAndEchoedInBody() throws Exception {
        ResultActions resultActions = mockMvc.perform(get("/api/v1/users")
                        .header("X-Trace-Id", "not-a-valid-uuid"))
                .andExpect(status().isOk());

        MvcResult result = resultActions.andReturn();
        String headerTraceId = result.getResponse().getHeader("X-Trace-Id");

        assertNotNull(headerTraceId, "Response must always echo an X-Trace-Id header");
        assertNotEquals(
                "not-a-valid-uuid",
                headerTraceId,
                "An invalid inbound trace ID must never be echoed back unchanged"
        );
        assertDoesNotThrow(
                () -> UUID.fromString(headerTraceId),
                "Replacement trace ID must be a valid UUID"
        );

        resultActions.andExpect(jsonPath("$.traceId").value(headerTraceId));
    }
}
