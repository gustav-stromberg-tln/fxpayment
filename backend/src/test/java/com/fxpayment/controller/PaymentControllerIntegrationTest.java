package com.fxpayment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fxpayment.dto.PaymentRequest;
import com.fxpayment.repository.PaymentRepository;
import com.fxpayment.utils.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAll();
    }

    private ResultActions postPayment(PaymentRequest request) throws Exception {
        return mockMvc.perform(post("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    @Test
    void createPaymentShouldReturn201WithValidRequest() throws Exception {
        postPayment(TestDataFactory.paymentRequest())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.recipient").value("John Doe"))
                .andExpect(jsonPath("$.recipientAccount").value(TestDataFactory.ESTONIAN_IBAN))
                .andExpect(jsonPath("$.processingFee").isNumber())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.id").isString());
    }

    @Test
    void createPaymentWithInvalidIbanShouldReturn400() throws Exception {
        PaymentRequest request = new PaymentRequest(
                new BigDecimal("100.00"),
                "USD",
                "John Doe",
                "INVALID_IBAN"
        );

        postPayment(request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllPaymentsShouldReturnPaginatedResults() throws Exception {
        postPayment(TestDataFactory.paymentRequest())
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/payments")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.page.totalElements").isNumber())
                .andExpect(jsonPath("$.page.totalPages").isNumber());
    }

    @Test
    void getAllPaymentsShouldRejectSizeExceeding100() throws Exception {
        mockMvc.perform(get("/api/v1/payments")
                        .param("page", "0")
                        .param("size", "200"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPaymentWithZeroAmountShouldReturn400() throws Exception {
        postPayment(TestDataFactory.paymentRequest(BigDecimal.ZERO, "USD"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPaymentWithEurShouldHaveZeroFee() throws Exception {
        postPayment(TestDataFactory.paymentRequest(new BigDecimal("500.00"), "EUR"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.processingFee").value(0.0));
    }
}
