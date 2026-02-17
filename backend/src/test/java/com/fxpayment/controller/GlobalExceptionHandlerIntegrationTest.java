package com.fxpayment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fxpayment.repository.PaymentRepository;
import com.fxpayment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.fxpayment.utils.TestDataFactory.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("GlobalExceptionHandler integration tests (2c, 5e)")
class GlobalExceptionHandlerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    @DisplayName("unexpected RuntimeException returns 500 with standard error body (5e)")
    void unexpectedExceptionShouldReturn500WithErrorBody() throws Exception {
        when(paymentService.createPayment(anyString(), any()))
                .thenThrow(new RuntimeException("Unexpected infrastructure failure"));

        mockMvc.perform(post(PAYMENTS_API_PATH)
                        .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aPaymentRequest().build())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.errors[0]").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    @DisplayName("DataIntegrityViolationException at controller level returns 409 (2c)")
    void dataIntegrityViolationShouldReturn409() throws Exception {
        when(paymentService.createPayment(anyString(), any()))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate key"));

        mockMvc.perform(post(PAYMENTS_API_PATH)
                        .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(aPaymentRequest().build())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.errors[0]").value("A conflict occurred while processing your request"))
                .andExpect(jsonPath("$.timestamp").isString());
    }
}
