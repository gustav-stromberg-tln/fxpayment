package com.fxpayment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fxpayment.dto.PaymentRequest;
import com.fxpayment.repository.PaymentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.fxpayment.utils.TestDataFactory.*;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Payment API edge case integration tests")
class PaymentControllerEdgeCaseIntegrationTest extends AbstractIntegrationTest {

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
        return postPayment(request, UUID.randomUUID().toString());
    }

    private ResultActions postPayment(PaymentRequest request, String idempotencyKey) throws Exception {
        return mockMvc.perform(post(PAYMENTS_API_PATH)
                .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    @Nested
    @DisplayName("Concurrency edge cases")
    class ConcurrencyEdgeCases {

        private static final String[] NAMES = {
                "Alice", "Bob", "Charlie", "Diana", "Elena",
                "Franz", "Greta", "Helga", "Ivan", "Julia"
        };

        @Test
        @DisplayName("concurrent requests with different keys should all succeed (1a)")
        void concurrentDifferentKeysShouldAllSucceed() throws Exception {
            int concurrency = 10;
            CountDownLatch startGate = new CountDownLatch(1);

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Future<Integer>> futures = new ArrayList<>();
                for (int i = 0; i < concurrency; i++) {
                    String key = UUID.randomUUID().toString();
                    String body = objectMapper.writeValueAsString(
                            aPaymentRequest().recipient(NAMES[i]).build());
                    futures.add(executor.submit(() -> {
                        startGate.await();
                        return mockMvc.perform(post(PAYMENTS_API_PATH)
                                        .header(IDEMPOTENCY_KEY_HEADER, key)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                                .andReturn().getResponse().getStatus();
                    }));
                }
                startGate.countDown();

                List<Integer> statuses = new ArrayList<>();
                for (var future : futures) {
                    statuses.add(future.get(10, TimeUnit.SECONDS));
                }

                long created = statuses.stream().filter(s -> s == 201).count();
                assertEquals(concurrency, created,
                        "All requests with unique keys should get 201 Created");
            }

            assertEquals(concurrency, paymentRepository.count(),
                    "Each unique key should produce exactly one payment row");
        }

        @Test
        @DisplayName("concurrent reads during writes should not fail (1c)")
        void concurrentReadsDuringWritesShouldNotFail() throws Exception {
            // Seed some payments first
            for (int i = 0; i < 5; i++) {
                postPayment(aPaymentRequest().recipient(NAMES[i]).build())
                        .andExpect(status().isCreated());
            }

            int concurrency = 10;
            CountDownLatch startGate = new CountDownLatch(1);

            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Future<Integer>> readFutures = new ArrayList<>();
                List<Future<Integer>> writeFutures = new ArrayList<>();

                // 5 concurrent readers
                for (int i = 0; i < concurrency / 2; i++) {
                    readFutures.add(executor.submit(() -> {
                        startGate.await();
                        return mockMvc.perform(get(PAYMENTS_API_PATH)
                                        .param("page", "0")
                                        .param("size", "10"))
                                .andReturn().getResponse().getStatus();
                    }));
                }

                // 5 concurrent writers
                for (int i = 0; i < concurrency / 2; i++) {
                    String key = UUID.randomUUID().toString();
                    String body = objectMapper.writeValueAsString(
                            aPaymentRequest().recipient(NAMES[i + 5]).build());
                    writeFutures.add(executor.submit(() -> {
                        startGate.await();
                        return mockMvc.perform(post(PAYMENTS_API_PATH)
                                        .header(IDEMPOTENCY_KEY_HEADER, key)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                                .andReturn().getResponse().getStatus();
                    }));
                }

                startGate.countDown();

                Set<Integer> readStatuses = new HashSet<>();
                for (var future : readFutures) {
                    readStatuses.add(future.get(10, TimeUnit.SECONDS));
                }
                Set<Integer> writeStatuses = new HashSet<>();
                for (var future : writeFutures) {
                    writeStatuses.add(future.get(10, TimeUnit.SECONDS));
                }

                assertTrue(readStatuses.stream().allMatch(s -> s == 200),
                        "All reads should return 200 OK");
                assertTrue(writeStatuses.stream().allMatch(s -> s == 201),
                        "All writes with unique keys should return 201 Created");
            }
        }
    }

    @Nested
    @DisplayName("Boundary value edge cases")
    class BoundaryValueEdgeCases {

        @Test
        @DisplayName("negative page number returns 400 (4a)")
        void negativePageNumberShouldReturn400() throws Exception {
            mockMvc.perform(get(PAYMENTS_API_PATH)
                            .param("page", "-1")
                            .param("size", "10"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("non-numeric page parameter returns 400 (4b)")
        void nonNumericPageShouldReturn400() throws Exception {
            mockMvc.perform(get(PAYMENTS_API_PATH)
                            .param("page", "abc")
                            .param("size", "10"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.errors[0]").isString());
        }

        @Test
        @DisplayName("non-numeric size parameter returns 400 (4b)")
        void nonNumericSizeShouldReturn400() throws Exception {
            mockMvc.perform(get(PAYMENTS_API_PATH)
                            .param("page", "0")
                            .param("size", "xyz"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("non-UUID Idempotency-Key returns 400 (4c)")
        void nonUuidIdempotencyKeyShouldReturn400() throws Exception {
            postPayment(aPaymentRequest().build(), "not-a-valid-uuid")
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("empty Idempotency-Key returns 400 (4c)")
        void emptyIdempotencyKeyShouldReturn400() throws Exception {
            postPayment(aPaymentRequest().build(), "")
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("empty request body returns 400 (4d)")
        void emptyRequestBodyShouldReturn400() throws Exception {
            mockMvc.perform(post(PAYMENTS_API_PATH)
                            .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("null JSON body returns 400 (4d)")
        void nullJsonBodyShouldReturn400() throws Exception {
            mockMvc.perform(post(PAYMENTS_API_PATH)
                            .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("malformed JSON body returns 400 (4d)")
        void malformedJsonBodyShouldReturn400() throws Exception {
            mockMvc.perform(post(PAYMENTS_API_PATH)
                            .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{invalid json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0]").value("Malformed request body"));
        }

        @Test
        @DisplayName("lowercase currency code returns 400 (4e)")
        void lowercaseCurrencyCodeShouldReturn400() throws Exception {
            postPayment(aPaymentRequest().currency("usd").build())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("extra unknown JSON fields are silently ignored (4f)")
        void extraJsonFieldsShouldBeIgnored() throws Exception {
            String jsonWithExtra = """
                    {
                      "amount": 100.00,
                      "currency": "USD",
                      "recipient": "John Doe",
                      "recipientAccount": "%s",
                      "unknownField": "should be ignored"
                    }
                    """.formatted(ESTONIAN_IBAN);

            mockMvc.perform(post(PAYMENTS_API_PATH)
                            .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonWithExtra))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.amount").value(100.00));
        }

        @Test
        @DisplayName("DELETE on payments endpoint returns 405 (4g)")
        void deleteMethodShouldReturn405() throws Exception {
            mockMvc.perform(delete(PAYMENTS_API_PATH))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.status").value(405));
        }

        @Test
        @DisplayName("PUT on payments endpoint returns 405 (4g)")
        void putMethodShouldReturn405() throws Exception {
            mockMvc.perform(put(PAYMENTS_API_PATH)
                            .header(IDEMPOTENCY_KEY_HEADER, UUID.randomUUID().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(aPaymentRequest().build())))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.status").value(405))
                    .andExpect(jsonPath("$.errors[0]").isString());
        }

        @Test
        @DisplayName("unknown route returns 404 (4h)")
        void unknownRouteShouldReturn404() throws Exception {
            mockMvc.perform(get("/api/v1/nonexistent"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.errors[0]").value("Resource not found"));
        }

        @Test
        @DisplayName("page beyond available data returns empty page (4i)")
        void pageBeyondDataShouldReturnEmptyPage() throws Exception {
            postPayment(aPaymentRequest().build())
                    .andExpect(status().isCreated());

            mockMvc.perform(get(PAYMENTS_API_PATH)
                            .param("page", "9999")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.page.totalElements").value(1));
        }

        @Test
        @DisplayName("whitespace-only currency returns 400")
        void whitespaceCurrencyShouldReturn400() throws Exception {
            postPayment(aPaymentRequest().currency("   ").build())
                    .andExpect(status().isBadRequest());
        }
    }
}
