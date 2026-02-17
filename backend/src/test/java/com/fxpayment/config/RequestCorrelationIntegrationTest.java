package com.fxpayment.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.fxpayment.utils.TestDataFactory.PAYMENTS_API_PATH;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Request correlation integration tests")
class RequestCorrelationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("response contains X-Request-Id header when none sent")
    void shouldReturnRequestIdHeaderWhenNoneSent() throws Exception {
        mockMvc.perform(get(PAYMENTS_API_PATH)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(header().exists(RequestCorrelationFilter.REQUEST_ID_HEADER));
    }

    @Test
    @DisplayName("response echoes client-provided X-Request-Id")
    void shouldEchoClientProvidedRequestId() throws Exception {
        String clientId = UUID.randomUUID().toString();

        mockMvc.perform(get(PAYMENTS_API_PATH)
                        .header(RequestCorrelationFilter.REQUEST_ID_HEADER, clientId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestCorrelationFilter.REQUEST_ID_HEADER, clientId));
    }
}
