package com.fxpayment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fxpayment.dto.PaymentRequest;
import com.fxpayment.repository.CurrencyRepository;
import com.fxpayment.repository.PaymentRepository;
import com.fxpayment.utils.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

import static com.fxpayment.utils.TestDataFactory.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.cache.enabled=false")
@DisplayName("Payment flow with cache disabled (3c)")
class CacheDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        TestDataFactory.seedCurrencies(currencyRepository);
    }

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAll();
        currencyRepository.deleteAll();
    }

    @Test
    @DisplayName("payment creation works without caching")
    void paymentCreationShouldWorkWithoutCache() throws Exception {
        PaymentRequest request = aPaymentRequest().build();

        mockMvc.perform(post(PAYMENTS_API_PATH)
                        .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    @DisplayName("idempotency replay works via database-only lookup when cache is disabled")
    void idempotencyReplayShouldWorkViaDbWhenCacheDisabled() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();
        PaymentRequest request = aPaymentRequest().build();
        String body = objectMapper.writeValueAsString(request);

        // First request — creates
        String firstResponse = mockMvc.perform(post(PAYMENTS_API_PATH)
                        .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Second request — replays from DB (no cache)
        String secondResponse = mockMvc.perform(post(PAYMENTS_API_PATH)
                        .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Compare by fields (timestamp serialisation may differ in nanosecond trailing zeros)
        JsonNode first = objectMapper.readTree(firstResponse);
        JsonNode second = objectMapper.readTree(secondResponse);
        assertEquals(first.get("id").asText(), second.get("id").asText());
        assertEquals(first.get("amount").decimalValue(), second.get("amount").decimalValue());
        assertEquals(first.get("currency").asText(), second.get("currency").asText());
        assertEquals(first.get("recipient").asText(), second.get("recipient").asText());
        assertEquals(first.get("processingFee").decimalValue(), second.get("processingFee").decimalValue());
        assertEquals(1, paymentRepository.count());
    }
}
