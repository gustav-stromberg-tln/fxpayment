package com.fxpayment.service;

import com.fxpayment.dto.CreatePaymentResult;
import com.fxpayment.dto.PaymentRequest;
import com.fxpayment.dto.PaymentResponse;
import com.fxpayment.exception.InvalidRequestException;
import com.fxpayment.exception.PaymentProcessingException;
import com.fxpayment.model.CurrencyEntity;
import com.fxpayment.model.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import com.fxpayment.repository.PaymentRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static com.fxpayment.utils.TestDataFactory.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService edge case tests")
class PaymentServiceEdgeCaseTest {

    private static final UUID ID_1 = UUID.fromString(PAYMENT_UUID_1);
    private static final BigDecimal USD_FEE = new BigDecimal("5.0000");

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private FeeCalculationService feeCalculationService;

    @Mock
    private CurrencyService currencyService;

    @Mock
    private PaymentValidationService paymentValidationService;

    @Mock
    private IdempotencyCacheService idempotencyCacheService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private PaymentService paymentService;

    private final PaymentRequest request = aPaymentRequest().build();

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        lenient().when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> {
                    TransactionCallback<CreatePaymentResult> callback = invocation.getArgument(0);
                    return callback.doInTransaction(mock(TransactionStatus.class));
                });
    }

    private String newIdempotencyKey() {
        return UUID.randomUUID().toString();
    }

    private void stubNoExistingPayment() {
        when(idempotencyCacheService.findExistingPayment(anyString())).thenReturn(Optional.empty());
    }

    private void stubValidation(String code) {
        when(paymentValidationService.resolveAndValidateCurrency(any(PaymentRequest.class)))
                .thenReturn(CURRENCIES.get(code));
    }

    private void stubDecimals(String code) {
        when(currencyService.getDecimals(code))
                .thenReturn((int) CURRENCIES.get(code).getDecimals());
    }

    @Nested
    @DisplayName("Retry exhaustion (1b)")
    class RetryExhaustion {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("double DataIntegrityViolationException wraps as PaymentProcessingException")
        void doubleDataIntegrityViolationShouldThrowPaymentProcessingException() {
            when(transactionTemplate.execute(any(TransactionCallback.class)))
                    .thenThrow(new DataIntegrityViolationException("First conflict"))
                    .thenThrow(new DataIntegrityViolationException("Second conflict"));

            PaymentProcessingException ex = assertThrows(PaymentProcessingException.class,
                    () -> paymentService.createPayment(newIdempotencyKey(), request));

            assertTrue(ex.getMessage().contains("conflict"));
            verify(transactionTemplate, times(2)).execute(any(TransactionCallback.class));
        }
    }

    @Nested
    @DisplayName("TransactionTemplate null result (5a)")
    class TransactionTemplateNullResult {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("null result from TransactionTemplate throws PaymentProcessingException")
        void nullTransactionResultShouldThrowPaymentProcessingException() {
            when(transactionTemplate.execute(any(TransactionCallback.class)))
                    .thenReturn(null);

            assertThrows(PaymentProcessingException.class,
                    () -> paymentService.createPayment(newIdempotencyKey(), request));
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("null result on retry also throws PaymentProcessingException")
        void nullResultOnRetryShouldThrowPaymentProcessingException() {
            when(transactionTemplate.execute(any(TransactionCallback.class)))
                    .thenThrow(new DataIntegrityViolationException("Conflict"))
                    .thenReturn(null);

            assertThrows(PaymentProcessingException.class,
                    () -> paymentService.createPayment(newIdempotencyKey(), request));
        }
    }

    @Nested
    @DisplayName("Resilience: getAllPayments failures (5b)")
    class GetAllPaymentsResilience {

        @Test
        @DisplayName("DataAccessException during getAllPayments propagates")
        void databaseFailureDuringGetAllPaymentsShouldPropagate() {
            when(paymentRepository.findAll(any(Pageable.class)))
                    .thenThrow(new DataAccessResourceFailureException("Connection refused"));

            assertThrows(DataAccessResourceFailureException.class,
                    () -> paymentService.getAllPayments(0, 20));
        }

        @Test
        @DisplayName("QueryTimeoutException during getAllPayments propagates")
        void queryTimeoutDuringGetAllPaymentsShouldPropagate() {
            when(paymentRepository.findAll(any(Pageable.class)))
                    .thenThrow(new QueryTimeoutException("Query timed out"));

            assertThrows(QueryTimeoutException.class,
                    () -> paymentService.getAllPayments(0, 20));
        }
    }

    @Nested
    @DisplayName("Resilience: cache write failures (5c)")
    class CacheWriteFailure {

        @Test
        @DisplayName("cache failure after DB save propagates as uncaught exception")
        void cacheFailureAfterSaveShouldPropagate() {
            stubNoExistingPayment();
            stubValidation("USD");
            when(feeCalculationService.calculateFee(any(BigDecimal.class), any(CurrencyEntity.class)))
                    .thenReturn(USD_FEE);
            Payment savedPayment = aPayment().id(ID_1).build();
            when(paymentRepository.saveAndFlush(any(Payment.class))).thenReturn(savedPayment);
            doThrow(new RuntimeException("Cache write failed"))
                    .when(idempotencyCacheService).cachePayment(anyString(), any(Payment.class));

            assertThrows(RuntimeException.class,
                    () -> paymentService.createPayment(newIdempotencyKey(), request));
        }
    }

    @Nested
    @DisplayName("Resilience: orphaned currency in getAllPayments (5d)")
    class OrphanedCurrency {

        @Test
        @DisplayName("deleted currency during response mapping throws InvalidRequestException")
        void deletedCurrencyDuringMappingShouldThrow() {
            Payment payment = aPayment().id(ID_1).currency("ZZZ").build();
            Page<Payment> page = new org.springframework.data.domain.PageImpl<>(
                    java.util.List.of(payment));
            when(paymentRepository.findAll(any(Pageable.class))).thenReturn(page);
            when(currencyService.getDecimals("ZZZ"))
                    .thenThrow(new InvalidRequestException("Currency not found: ZZZ"));

            assertThrows(InvalidRequestException.class,
                    () -> paymentService.getAllPayments(0, 20));
        }
    }
}
