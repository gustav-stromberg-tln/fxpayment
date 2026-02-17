package com.fxpayment.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fxpayment.dto.PaymentRequest;
import com.fxpayment.model.Payment;
import com.fxpayment.util.PaymentConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import com.fxpayment.repository.PaymentRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.fxpayment.utils.TestDataFactory.*;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Payment API integration tests")
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
        return postPayment(request, UUID.randomUUID().toString());
    }

    private ResultActions postPayment(PaymentRequest request, String idempotencyKey) throws Exception {
        return mockMvc.perform(post(PAYMENTS_API_PATH)
                .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    @Nested
    @DisplayName("Payment creation")
    class Creation {

        @Test
        @DisplayName("valid request returns 201 with all expected fields")
        void shouldReturn201WithValidRequest() throws Exception {
            postPayment(aPaymentRequest().build())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.amount").value(100.00))
                    .andExpect(jsonPath("$.currency").value("USD"))
                    .andExpect(jsonPath("$.recipient").value("John Doe"))
                    .andExpect(jsonPath("$.processingFee").isNumber())
                    .andExpect(jsonPath("$.id").isString())
                    .andExpect(jsonPath("$.createdAt").isString());
        }

        @Test
        @DisplayName("EUR payment has zero processing fee")
        void eurPaymentShouldHaveZeroFee() throws Exception {
            postPayment(aPaymentRequest().amount(new BigDecimal("500.00")).currency("EUR").build())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.processingFee").value(0.0));
        }

        @Test
        @DisplayName("minimum allowed amount (0.01) is accepted")
        void minimumAmountShouldBeAccepted() throws Exception {
            postPayment(aPaymentRequest().amount(new BigDecimal("0.01")).build())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.amount").value(0.01));
        }

        @Test
        @DisplayName("maximum allowed amount (1000000) is accepted")
        void maximumAmountShouldBeAccepted() throws Exception {
            postPayment(aPaymentRequest().amount(new BigDecimal("1000000")).currency("EUR").build())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.amount").value(1000000));
        }
    }

    @Nested
    @DisplayName("Recipient validation")
    class RecipientValidation {

        @Test
        @DisplayName("missing (blank) recipient returns 400")
        void blankRecipientShouldReturn400() throws Exception {
            postPayment(aPaymentRequest().recipient("").build())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("recipient below minimum length returns 400")
        void belowMinLengthShouldReturn400() throws Exception {
            postPayment(aPaymentRequest().recipient("A").build())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("recipient at minimum length (2 chars) returns 201")
        void atMinLengthShouldReturn201() throws Exception {
            postPayment(aPaymentRequest().recipient("Li").build())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.recipient").value("Li"));
        }

        @Test
        @DisplayName("recipient at maximum length (140 chars) returns 201")
        void atMaxLengthShouldReturn201() throws Exception {
            String maxName = "A".repeat(PaymentConstants.MAX_RECIPIENT_LENGTH);

            postPayment(aPaymentRequest().recipient(maxName).build())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.recipient").value(maxName));
        }

        @Test
        @DisplayName("recipient exceeding maximum length returns 400")
        void aboveMaxLengthShouldReturn400() throws Exception {
            String tooLong = "A".repeat(PaymentConstants.MAX_RECIPIENT_LENGTH + 1);

            postPayment(aPaymentRequest().recipient(tooLong).build())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("invalid characters in recipient returns 400 (pattern wiring check)")
        void invalidCharactersShouldReturn400() throws Exception {
            postPayment(aPaymentRequest().recipient("John <script>").build())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("diacritics in recipient are accepted (pattern wiring check)")
        void diacriticsShouldReturn201() throws Exception {
            postPayment(aPaymentRequest().recipient(RECIPIENT_WITH_DIACRITICS).build())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.recipient").value(RECIPIENT_WITH_DIACRITICS));
        }

        @Test
        @DisplayName("leading and trailing spaces are trimmed")
        void leadingTrailingSpacesShouldBeTrimmed() throws Exception {
            postPayment(aPaymentRequest().recipient("  John Doe  ").build())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.recipient").value("John Doe"));
        }

        @Test
        @DisplayName("multiple consecutive spaces are collapsed to single space")
        void multipleSpacesShouldBeNormalised() throws Exception {
            postPayment(aPaymentRequest().recipient("John    Doe").build())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.recipient").value("John Doe"));
        }
    }

    @Nested
    @DisplayName("Amount validation")
    class AmountValidation {

        @Test
        @DisplayName("zero amount returns 400")
        void zeroAmountShouldReturn400() throws Exception {
            postPayment(aPaymentRequest().amount(BigDecimal.ZERO).build())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("negative amount returns 400")
        void negativeAmountShouldReturn400() throws Exception {
            postPayment(aPaymentRequest().amount(new BigDecimal("-50.00")).build())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("null amount returns 400")
        void nullAmountShouldReturn400() throws Exception {
            postPayment(aPaymentRequest().amount(null).build())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("amount exceeding maximum returns 400")
        void exceedingMaxAmountShouldReturn400() throws Exception {
            postPayment(aPaymentRequest().amount(new BigDecimal("1000001")).build())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Currency and decimal precision")
    class CurrencyValidation {

        @Test
        @DisplayName("unsupported currency returns 400")
        void unsupportedCurrencyShouldReturn400() throws Exception {
            postPayment(aPaymentRequest().currency(UNSUPPORTED_CURRENCY).build())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("missing (blank) currency returns 400")
        void missingCurrencyShouldReturn400() throws Exception {
            postPayment(aPaymentRequest().currency("").build())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("too many decimal places for USD returns 400")
        void tooManyDecimalsForUsdShouldReturn400() throws Exception {
            postPayment(aPaymentRequest().amount(new BigDecimal("100.123")).build())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("JPY whole amount returns 201")
        void jpyWholeAmountShouldReturn201() throws Exception {
            currencyRepository.save(jpyCurrency());

            postPayment(aPaymentRequest().amount(new BigDecimal("10000")).currency("JPY").build())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.currency").value("JPY"))
                    .andExpect(jsonPath("$.processingFee").isNumber());
        }

        @Test
        @DisplayName("JPY fractional amount returns 400")
        void jpyFractionalAmountShouldReturn400() throws Exception {
            currencyRepository.save(jpyCurrency());

            postPayment(aPaymentRequest().amount(new BigDecimal("10000.5")).currency("JPY").build())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("BHD three decimal places returns 201")
        void bhdThreeDecimalsShouldReturn201() throws Exception {
            currencyRepository.save(bhdCurrency());

            postPayment(aPaymentRequest().amount(new BigDecimal("500.125")).currency("BHD").build())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.currency").value("BHD"))
                    .andExpect(jsonPath("$.processingFee").isNumber());
        }

        @Test
        @DisplayName("BHD four decimal places returns 400")
        void bhdFourDecimalsShouldReturn400() throws Exception {
            currencyRepository.save(bhdCurrency());

            postPayment(aPaymentRequest().amount(new BigDecimal("500.1234")).currency("BHD").build())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("IBAN validation")
    class IbanValidation {

        @Test
        @DisplayName("invalid IBAN returns 400")
        void invalidIbanShouldReturn400() throws Exception {
            postPayment(aPaymentRequest().recipientAccount("INVALID_IBAN").build())
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Swedish IBAN is accepted and persisted")
        void swedishIbanShouldReturn201() throws Exception {
            String body = postPayment(aPaymentRequest().recipientAccount(SWEDISH_IBAN).build())
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            UUID id = UUID.fromString(objectMapper.readTree(body).get("id").asText());
            Payment persisted = paymentRepository.findById(id).orElseThrow();
            assertEquals(SWEDISH_IBAN, persisted.getRecipientAccount());
        }

        @Test
        @DisplayName("German IBAN is accepted and persisted")
        void germanIbanShouldReturn201() throws Exception {
            String body = postPayment(aPaymentRequest().currency("GBP").recipientAccount(GERMAN_IBAN).build())
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            UUID id = UUID.fromString(objectMapper.readTree(body).get("id").asText());
            Payment persisted = paymentRepository.findById(id).orElseThrow();
            assertEquals(GERMAN_IBAN, persisted.getRecipientAccount());
        }

        @Test
        @DisplayName("Finnish IBAN is accepted and persisted")
        void finnishIbanShouldReturn201() throws Exception {
            String body = postPayment(aPaymentRequest().currency("EUR").recipientAccount(FINNISH_IBAN).build())
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            UUID id = UUID.fromString(objectMapper.readTree(body).get("id").asText());
            Payment persisted = paymentRepository.findById(id).orElseThrow();
            assertEquals(FINNISH_IBAN, persisted.getRecipientAccount());
        }
    }

    @Nested
    @DisplayName("Pagination")
    class PaginationTests {

        @Test
        @DisplayName("returns paginated results with expected structure")
        void shouldReturnPaginatedResults() throws Exception {
            postPayment(aPaymentRequest().build())
                    .andExpect(status().isCreated());

            mockMvc.perform(get(PAYMENTS_API_PATH)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$.page.totalElements").isNumber())
                    .andExpect(jsonPath("$.page.totalPages").isNumber());
        }

        @Test
        @DisplayName("returns empty page when no payments exist")
        void shouldReturnEmptyPage() throws Exception {
            mockMvc.perform(get(PAYMENTS_API_PATH)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.page.totalElements").value(0));
        }

        @Test
        @DisplayName("uses default pagination parameters when none provided")
        void shouldUseDefaultParams() throws Exception {
            postPayment(aPaymentRequest().build())
                    .andExpect(status().isCreated());

            mockMvc.perform(get(PAYMENTS_API_PATH))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.page.size").value(20));
        }

        @Test
        @DisplayName("rejects page size exceeding 100")
        void shouldRejectSizeExceeding100() throws Exception {
            mockMvc.perform(get(PAYMENTS_API_PATH)
                            .param("page", "0")
                            .param("size", "200"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("rejects page size below minimum")
        void shouldRejectSizeBelowMinimum() throws Exception {
            mockMvc.perform(get(PAYMENTS_API_PATH)
                            .param("size", "0"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Idempotency")
    class IdempotencyTests {

        @Test
        @DisplayName("duplicate idempotency key returns 200 with original payment")
        void duplicateKeyShouldReturn200WithOriginal() throws Exception {
            String idempotencyKey = UUID.randomUUID().toString();
            PaymentRequest request = aPaymentRequest().build();

            String firstResponseBody = postPayment(request, idempotencyKey)
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            String secondResponseBody = postPayment(request, idempotencyKey)
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            // Compare by fields — H2 may truncate nanosecond precision in timestamps
            JsonNode first = objectMapper.readTree(firstResponseBody);
            JsonNode second = objectMapper.readTree(secondResponseBody);
            assertEquals(first.get("id").asText(), second.get("id").asText());
            assertEquals(first.get("amount").decimalValue(), second.get("amount").decimalValue());
            assertEquals(first.get("currency").asText(), second.get("currency").asText());
            assertEquals(first.get("recipient").asText(), second.get("recipient").asText());
            assertEquals(first.get("processingFee").decimalValue(), second.get("processingFee").decimalValue());
        }

        @Test
        @DisplayName("duplicate idempotency key does not create a second payment")
        void duplicateKeyShouldNotCreateSecondPayment() throws Exception {
            String idempotencyKey = UUID.randomUUID().toString();

            postPayment(aPaymentRequest().build(), idempotencyKey)
                    .andExpect(status().isCreated());
            postPayment(aPaymentRequest().build(), idempotencyKey)
                    .andExpect(status().isOk());

            assertEquals(1, paymentRepository.count());
        }

        @Test
        @DisplayName("different idempotency keys create separate payments")
        void differentKeysShouldCreateSeparatePayments() throws Exception {
            postPayment(aPaymentRequest().build(), UUID.randomUUID().toString())
                    .andExpect(status().isCreated());
            postPayment(aPaymentRequest().build(), UUID.randomUUID().toString())
                    .andExpect(status().isCreated());

            assertEquals(2, paymentRepository.count());
        }

        @Test
        @DisplayName("concurrent requests with same idempotency key create only one payment")
        void concurrentDuplicateKeyShouldCreateOnlyOnePayment() throws Exception {
            String idempotencyKey = UUID.randomUUID().toString();
            PaymentRequest request = aPaymentRequest().build();
            String requestBody = objectMapper.writeValueAsString(request);
            int concurrency = 5;

            CountDownLatch startGate = new CountDownLatch(1);
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                List<Future<Integer>> futures = new ArrayList<>();
                for (int i = 0; i < concurrency; i++) {
                    futures.add(executor.submit(() -> {
                        startGate.await();
                        return mockMvc.perform(post(PAYMENTS_API_PATH)
                                        .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestBody))
                                .andReturn().getResponse().getStatus();
                    }));
                }
                startGate.countDown();

                List<Integer> statuses = new ArrayList<>();
                for (var future : futures) {
                    statuses.add(future.get(10, TimeUnit.SECONDS));
                }

                long created = statuses.stream().filter(s -> s == 201).count();
                long replayed = statuses.stream().filter(s -> s == 200).count();

                assertEquals(1, created, "Exactly one request should get 201 Created");
                assertEquals(concurrency - 1, replayed,
                        "All other requests should get 200 OK (replay)");
            }

            assertEquals(1, paymentRepository.count(),
                    "Only one payment row should be persisted");
        }

        @Test
        @DisplayName("missing Idempotency-Key header returns 400")
        void missingHeaderShouldReturn400() throws Exception {
            mockMvc.perform(post(PAYMENTS_API_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(aPaymentRequest().build())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0]").value("Missing required header: Idempotency-Key"));
        }
    }

    @Nested
    @DisplayName("Error responses")
    class ErrorResponses {

        @Test
        @DisplayName("validation error includes status, errors array, and timestamp")
        void errorShouldContainExpectedStructure() throws Exception {
            postPayment(aPaymentRequest().currency(UNSUPPORTED_CURRENCY).build())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.timestamp").isString());
        }
    }

    @Nested
    @DisplayName("Data variety: diverse payment profiles")
    class DiversePaymentProfiles {

        @Test
        @DisplayName("EUR high-value payment with German IBAN and diacritics")
        void shouldCreateHighValueEurPaymentWithDiacritics() throws Exception {
            postPayment(aPaymentRequest()
                    .amount(new BigDecimal("999999.99"))
                    .currency("EUR")
                    .recipient("Ödegård Müller")
                    .recipientAccount(GERMAN_IBAN)
                    .build())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.amount").value(999999.99))
                    .andExpect(jsonPath("$.currency").value("EUR"))
                    .andExpect(jsonPath("$.recipient").value("Ödegård Müller"))
                    .andExpect(jsonPath("$.processingFee").value(0.0));
        }

        @Test
        @DisplayName("GBP payment with Finnish IBAN")
        void shouldCreateGbpPaymentWithFinnishIban() throws Exception {
            postPayment(aPaymentRequest()
                    .amount(new BigDecimal("500.00"))
                    .currency("GBP")
                    .recipient("Virtanen Kallio")
                    .recipientAccount(FINNISH_IBAN)
                    .build())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.amount").value(500.00))
                    .andExpect(jsonPath("$.currency").value("GBP"))
                    .andExpect(jsonPath("$.recipient").value("Virtanen Kallio"))
                    .andExpect(jsonPath("$.processingFee").value(5.0));
        }

        @Test
        @DisplayName("USD payment with Swedish IBAN and fee above minimum")
        void shouldCreateUsdPaymentWithSwedishIbanAndHigherFee() throws Exception {
            postPayment(aPaymentRequest()
                    .amount(new BigDecimal("1000.00"))
                    .currency("USD")
                    .recipient("Eriksson Ljungberg")
                    .recipientAccount(SWEDISH_IBAN)
                    .build())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.amount").value(1000.00))
                    .andExpect(jsonPath("$.currency").value("USD"))
                    .andExpect(jsonPath("$.recipient").value("Eriksson Ljungberg"))
                    .andExpect(jsonPath("$.processingFee").value(10.0));
        }

        @Test
        @DisplayName("minimum-length recipient with minimum amount")
        void shouldCreatePaymentWithMinimumRecipientAndAmount() throws Exception {
            postPayment(aPaymentRequest()
                    .amount(new BigDecimal("0.01"))
                    .currency("EUR")
                    .recipient("Li")
                    .recipientAccount(FINNISH_IBAN)
                    .build())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.amount").value(0.01))
                    .andExpect(jsonPath("$.recipient").value("Li"))
                    .andExpect(jsonPath("$.processingFee").value(0.0));
        }

        @Test
        @DisplayName("multiple diverse payments visible in paginated list")
        void shouldListDiversePayments() throws Exception {
            postPayment(aPaymentRequest()
                    .amount(new BigDecimal("100.00")).currency("USD").recipient("Alice").build());
            postPayment(aPaymentRequest()
                    .amount(new BigDecimal("200.00")).currency("EUR").recipient("Bob")
                    .recipientAccount(GERMAN_IBAN).build());
            postPayment(aPaymentRequest()
                    .amount(new BigDecimal("300.00")).currency("GBP").recipient("Charlie")
                    .recipientAccount(FINNISH_IBAN).build());

            mockMvc.perform(get(PAYMENTS_API_PATH)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(3)))
                    .andExpect(jsonPath("$.page.totalElements").value(3));
        }
    }
}
