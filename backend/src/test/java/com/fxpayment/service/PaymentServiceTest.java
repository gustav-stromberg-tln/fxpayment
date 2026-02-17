package com.fxpayment.service;

import com.fxpayment.exception.InvalidRequestException;
import com.fxpayment.exception.PaymentProcessingException;
import com.fxpayment.dto.CreatePaymentResult;
import com.fxpayment.dto.PaymentRequest;
import com.fxpayment.dto.PaymentResponse;
import com.fxpayment.model.CurrencyEntity;
import com.fxpayment.model.Payment;
import com.fxpayment.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import com.fxpayment.repository.PaymentRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.fxpayment.utils.TestDataFactory.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final UUID ID_1 = UUID.fromString(PAYMENT_UUID_1);
    private static final UUID ID_2 = UUID.fromString(PAYMENT_UUID_2);
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

    @Test
    void createPaymentShouldCalculateFeeAndSave() {
        stubNoExistingPayment();
        stubValidation("USD");
        when(feeCalculationService.calculateFee(any(BigDecimal.class), any(CurrencyEntity.class))).thenReturn(USD_FEE);
        Payment savedPayment = aPayment().id(ID_1).build();
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenReturn(savedPayment);

        CreatePaymentResult result = paymentService.createPayment(newIdempotencyKey(), request);

        assertTrue(result.created());
        PaymentResponse response = result.response();
        assertNotNull(response);
        assertEquals(ID_1, response.id());
        assertEquals(new BigDecimal("100.00"), response.amount());
        assertEquals("USD", response.currency());
        assertEquals("John Doe", response.recipient());
        assertEquals(new BigDecimal("5.00"), response.processingFee());
        verify(feeCalculationService).calculateFee(any(BigDecimal.class), any(CurrencyEntity.class));
    }

    @Test
    void createPaymentShouldPassCorrectDataToRepository() {
        stubNoExistingPayment();
        stubValidation("USD");
        when(feeCalculationService.calculateFee(any(BigDecimal.class), any(CurrencyEntity.class))).thenReturn(USD_FEE);
        Payment savedPayment = aPayment().id(ID_1).build();
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenReturn(savedPayment);

        String idempotencyKey = newIdempotencyKey();
        paymentService.createPayment(idempotencyKey, request);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).saveAndFlush(captor.capture());
        Payment captured = captor.getValue();
        assertEquals(new BigDecimal("100.00"), captured.getAmount());
        assertEquals("USD", captured.getCurrency());
        assertEquals("John Doe", captured.getRecipient());
        assertEquals(ESTONIAN_IBAN, captured.getRecipientAccount());
        assertEquals(USD_FEE, captured.getProcessingFee());
        assertEquals(PaymentStatus.COMPLETED, captured.getStatus());
        assertEquals(idempotencyKey, captured.getIdempotencyKey());
    }

    @Test
    void getAllPaymentsShouldReturnPagedResponses() {
        stubDecimals("USD");
        Payment payment1 = aPayment().id(ID_1).build();
        Payment payment2 = aPayment().id(ID_2).processingFee(new BigDecimal("10.0000")).build();
        Page<Payment> page = new PageImpl<>(List.of(payment1, payment2));
        when(paymentRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<PaymentResponse> responses = paymentService.getAllPayments(0, 20);

        assertEquals(2, responses.getContent().size());
        assertEquals(ID_1, responses.getContent().get(0).id());
        assertEquals(ID_2, responses.getContent().get(1).id());
    }

    @Test
    void getAllPaymentsShouldReturnEmptyPageWhenNoPayments() {
        Page<Payment> emptyPage = Page.empty();
        when(paymentRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        Page<PaymentResponse> responses = paymentService.getAllPayments(0, 20);

        assertTrue(responses.getContent().isEmpty());
    }

    @Test
    void createPaymentShouldDelegateValidation() {
        stubNoExistingPayment();
        when(paymentValidationService.resolveAndValidateCurrency(any(PaymentRequest.class)))
                .thenThrow(new InvalidRequestException("Unsupported currency code: " + UNSUPPORTED_CURRENCY));
        PaymentRequest badRequest = aPaymentRequest().currency(UNSUPPORTED_CURRENCY).build();

        assertThrows(InvalidRequestException.class,
                () -> paymentService.createPayment(newIdempotencyKey(), badRequest));
        verify(paymentValidationService).resolveAndValidateCurrency(badRequest);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void createPaymentShouldAcceptWholeAmountForZeroDecimalCurrency() {
        stubNoExistingPayment();
        stubValidation("JPY");
        when(feeCalculationService.calculateFee(any(BigDecimal.class), any(CurrencyEntity.class)))
                .thenReturn(new BigDecimal("500.0000"));
        PaymentRequest jpyRequest = aPaymentRequest()
                .amount(new BigDecimal("10000")).currency("JPY").build();
        Payment savedPayment = aPayment().id(ID_1)
                .amount(new BigDecimal("10000.0000")).currency("JPY")
                .processingFee(new BigDecimal("500.0000")).build();
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenReturn(savedPayment);

        CreatePaymentResult result = paymentService.createPayment(newIdempotencyKey(), jpyRequest);

        assertTrue(result.created());
        PaymentResponse response = result.response();
        assertNotNull(response);
        assertEquals(ID_1, response.id());
        assertEquals("JPY", response.currency());
        assertEquals(new BigDecimal("10000"), response.amount());
        assertEquals(new BigDecimal("500"), response.processingFee());
    }

    @Test
    void createPaymentShouldAcceptThreeDecimalPlacesForBHD() {
        stubNoExistingPayment();
        stubValidation("BHD");
        when(feeCalculationService.calculateFee(any(BigDecimal.class), any(CurrencyEntity.class)))
                .thenReturn(new BigDecimal("2.0000"));
        PaymentRequest bhdRequest = aPaymentRequest()
                .amount(new BigDecimal("100.500")).currency("BHD").build();
        Payment savedPayment = aPayment().id(ID_1)
                .amount(new BigDecimal("100.5000")).currency("BHD")
                .processingFee(new BigDecimal("2.0000")).build();
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenReturn(savedPayment);

        CreatePaymentResult result = paymentService.createPayment(newIdempotencyKey(), bhdRequest);

        assertTrue(result.created());
        PaymentResponse response = result.response();
        assertNotNull(response);
        assertEquals(ID_1, response.id());
        assertEquals("BHD", response.currency());
        assertEquals(new BigDecimal("100.500"), response.amount());
        assertEquals(new BigDecimal("2.000"), response.processingFee());
    }

    @Test
    void createPaymentWithSwedishIbanShouldSucceed() {
        stubNoExistingPayment();
        stubValidation("USD");
        when(feeCalculationService.calculateFee(any(BigDecimal.class), any(CurrencyEntity.class))).thenReturn(USD_FEE);
        PaymentRequest sweRequest = aPaymentRequest()
                .amount(new BigDecimal("250.00")).recipientAccount(SWEDISH_IBAN).build();
        Payment savedPayment = aPayment().id(ID_1)
                .amount(new BigDecimal("250.00")).recipientAccount(SWEDISH_IBAN).build();
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenReturn(savedPayment);

        CreatePaymentResult result = paymentService.createPayment(newIdempotencyKey(), sweRequest);

        assertNotNull(result.response());
    }

    @Test
    void createPaymentWithGermanIbanShouldSucceed() {
        stubNoExistingPayment();
        stubValidation("USD");
        when(feeCalculationService.calculateFee(any(BigDecimal.class), any(CurrencyEntity.class))).thenReturn(USD_FEE);
        PaymentRequest deRequest = aPaymentRequest()
                .amount(new BigDecimal("750.00")).recipientAccount(GERMAN_IBAN).build();
        Payment savedPayment = aPayment().id(ID_1)
                .amount(new BigDecimal("750.00")).recipientAccount(GERMAN_IBAN).build();
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenReturn(savedPayment);

        CreatePaymentResult result = paymentService.createPayment(newIdempotencyKey(), deRequest);

        assertNotNull(result.response());
    }

    @Test
    void createPaymentShouldPassCorrectIbanToRepository() {
        stubNoExistingPayment();
        stubValidation("USD");
        when(feeCalculationService.calculateFee(any(BigDecimal.class), any(CurrencyEntity.class))).thenReturn(USD_FEE);
        PaymentRequest sweRequest = aPaymentRequest().recipientAccount(SWEDISH_IBAN).build();
        Payment savedPayment = aPayment().id(ID_1).recipientAccount(SWEDISH_IBAN).build();
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenReturn(savedPayment);

        paymentService.createPayment(newIdempotencyKey(), sweRequest);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).saveAndFlush(captor.capture());
        assertEquals(SWEDISH_IBAN, captor.getValue().getRecipientAccount());
    }

    @Test
    void duplicateIdempotencyKeyShouldReturnExistingPayment() {
        stubDecimals("USD");
        String idempotencyKey = newIdempotencyKey();
        Payment existingPayment = aPayment().id(ID_1).build();
        when(idempotencyCacheService.findExistingPayment(idempotencyKey)).thenReturn(Optional.of(existingPayment));

        CreatePaymentResult result = paymentService.createPayment(idempotencyKey, request);

        assertFalse(result.created());
        assertEquals(ID_1, result.response().id());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void duplicateIdempotencyKeyShouldNotCalculateFee() {
        stubDecimals("USD");
        String idempotencyKey = newIdempotencyKey();
        Payment existingPayment = aPayment().id(ID_1).build();
        when(idempotencyCacheService.findExistingPayment(idempotencyKey)).thenReturn(Optional.of(existingPayment));

        paymentService.createPayment(idempotencyKey, request);

        verifyNoInteractions(feeCalculationService);
    }

    @Test
    void existingPaymentShouldSkipValidationAndFeeCalculation() {
        stubDecimals("USD");
        String idempotencyKey = newIdempotencyKey();
        Payment existingPayment = aPayment().id(ID_1).build();
        when(idempotencyCacheService.findExistingPayment(idempotencyKey)).thenReturn(Optional.of(existingPayment));

        CreatePaymentResult result = paymentService.createPayment(idempotencyKey, request);

        assertFalse(result.created());
        assertEquals(ID_1, result.response().id());
        verifyNoInteractions(paymentValidationService);
        verifyNoInteractions(feeCalculationService);
    }

    @Test
    void newPaymentShouldPopulateCache() {
        stubNoExistingPayment();
        stubValidation("USD");
        when(feeCalculationService.calculateFee(any(BigDecimal.class), any(CurrencyEntity.class))).thenReturn(USD_FEE);
        Payment savedPayment = aPayment().id(ID_1).build();
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenReturn(savedPayment);
        String idempotencyKey = newIdempotencyKey();

        paymentService.createPayment(idempotencyKey, request);

        verify(idempotencyCacheService).cachePayment(idempotencyKey, savedPayment);
    }

    @Test
    void createPaymentShouldThrowPaymentProcessingExceptionOnDatabaseFailure() {
        stubNoExistingPayment();
        stubValidation("USD");
        when(feeCalculationService.calculateFee(any(BigDecimal.class), any(CurrencyEntity.class))).thenReturn(USD_FEE);
        when(paymentRepository.saveAndFlush(any(Payment.class)))
                .thenThrow(new DataAccessResourceFailureException("Connection refused"));

        assertThrows(PaymentProcessingException.class,
                () -> paymentService.createPayment(newIdempotencyKey(), request));
    }

    @SuppressWarnings("unchecked")
    @Test
    void createPaymentShouldRetryOnDataIntegrityViolation() {
        String idempotencyKey = newIdempotencyKey();
        Payment existingPayment = aPayment().id(ID_1).build();
        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"))
                .thenAnswer(invocation -> {
                    TransactionCallback<CreatePaymentResult> callback = invocation.getArgument(0);
                    return callback.doInTransaction(mock(TransactionStatus.class));
                });
        when(idempotencyCacheService.findExistingPayment(idempotencyKey))
                .thenReturn(Optional.of(existingPayment));
        stubDecimals("USD");

        CreatePaymentResult result = paymentService.createPayment(idempotencyKey, request);

        assertFalse(result.created());
        assertEquals(ID_1, result.response().id());
        verify(transactionTemplate, times(2)).execute(any(TransactionCallback.class));
    }

    @Test
    void createHighValueEurPaymentWithZeroFee() {
        stubNoExistingPayment();
        stubValidation("EUR");
        when(feeCalculationService.calculateFee(any(BigDecimal.class), any(CurrencyEntity.class)))
                .thenReturn(new BigDecimal("0.0000"));
        PaymentRequest eurRequest = aPaymentRequest()
                .amount(new BigDecimal("999999.99"))
                .currency("EUR")
                .recipient(RECIPIENT_WITH_DIACRITICS)
                .recipientAccount(GERMAN_IBAN)
                .build();
        Payment savedPayment = aPayment().id(ID_1)
                .amount(new BigDecimal("999999.9900")).currency("EUR")
                .recipient(RECIPIENT_WITH_DIACRITICS).recipientAccount(GERMAN_IBAN)
                .processingFee(new BigDecimal("0.0000")).build();
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenReturn(savedPayment);

        CreatePaymentResult result = paymentService.createPayment(newIdempotencyKey(), eurRequest);

        assertTrue(result.created());
        PaymentResponse response = result.response();
        assertEquals(new BigDecimal("999999.99"), response.amount());
        assertEquals("EUR", response.currency());
        assertEquals(RECIPIENT_WITH_DIACRITICS, response.recipient());
        assertEquals(new BigDecimal("0.00"), response.processingFee());
    }

    @Test
    void createMinimumAmountGbpPaymentWithMinimumFee() {
        stubNoExistingPayment();
        stubValidation("GBP");
        when(feeCalculationService.calculateFee(any(BigDecimal.class), any(CurrencyEntity.class)))
                .thenReturn(new BigDecimal("5.0000"));
        PaymentRequest gbpRequest = aPaymentRequest()
                .amount(new BigDecimal("0.01"))
                .currency("GBP")
                .recipient("Li")
                .recipientAccount(FINNISH_IBAN)
                .build();
        Payment savedPayment = aPayment().id(ID_1)
                .amount(new BigDecimal("0.0100")).currency("GBP")
                .recipient("Li").recipientAccount(FINNISH_IBAN)
                .processingFee(new BigDecimal("5.0000")).build();
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenReturn(savedPayment);

        CreatePaymentResult result = paymentService.createPayment(newIdempotencyKey(), gbpRequest);

        assertTrue(result.created());
        PaymentResponse response = result.response();
        assertEquals(new BigDecimal("0.01"), response.amount());
        assertEquals("GBP", response.currency());
        assertEquals("Li", response.recipient());
        assertEquals(new BigDecimal("5.00"), response.processingFee());
    }

    @Test
    void createPaymentShouldNormaliseRecipientWhitespace() {
        stubNoExistingPayment();
        stubValidation("USD");
        when(feeCalculationService.calculateFee(any(BigDecimal.class), any(CurrencyEntity.class))).thenReturn(USD_FEE);
        PaymentRequest spaceRequest = aPaymentRequest()
                .recipient("  Eriksson   Ljungberg  ").build();
        Payment savedPayment = aPayment().id(ID_1)
                .recipient("Eriksson Ljungberg").build();
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenReturn(savedPayment);

        paymentService.createPayment(newIdempotencyKey(), spaceRequest);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).saveAndFlush(captor.capture());
        assertEquals("Eriksson Ljungberg", captor.getValue().getRecipient());
    }

    @Test
    void getAllPaymentsShouldMapFieldsCorrectlyForPagedResults() {
        stubDecimals("USD");
        Payment payment = aPayment().id(ID_1)
                .amount(new BigDecimal("550.0000"))
                .recipient("Eriksson Ljungberg")
                .recipientAccount(SWEDISH_IBAN)
                .processingFee(new BigDecimal("5.5000"))
                .build();
        Page<Payment> page = new PageImpl<>(List.of(payment));
        when(paymentRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<PaymentResponse> responses = paymentService.getAllPayments(0, 20);

        assertEquals(1, responses.getContent().size());
        PaymentResponse response = responses.getContent().get(0);
        assertEquals(ID_1, response.id());
        assertEquals(new BigDecimal("550.00"), response.amount());
        assertEquals("USD", response.currency());
        assertEquals("Eriksson Ljungberg", response.recipient());
        assertEquals(new BigDecimal("5.50"), response.processingFee());
    }
}
