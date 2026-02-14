package com.fxpayment.service;

import com.fxpayment.dto.PaymentRequest;
import com.fxpayment.dto.PaymentResponse;
import com.fxpayment.model.Curr;
import com.fxpayment.model.Payment;
import com.fxpayment.model.PaymentStatus;
import com.fxpayment.repository.PaymentRepository;
import com.fxpayment.utils.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final UUID ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final BigDecimal USD_FEE = new BigDecimal("5.0000");

    private static final Map<String, Curr> CURRENCIES = Map.of(
            "USD", TestDataFactory.usdCurrency(),
            "JPY", TestDataFactory.jpyCurrency(),
            "BHD", TestDataFactory.bhdCurrency()
    );

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private FeeCalculationService feeCalculationService;

    @Mock
    private CurrencyService currencyService;

    @InjectMocks
    private PaymentService paymentService;

    private final PaymentRequest request = TestDataFactory.paymentRequest();

    private void stubCurrency(String code) {
        when(currencyService.findByCode(code))
                .thenReturn(Optional.ofNullable(CURRENCIES.get(code)));
    }

    @Test
    void createPaymentShouldCalculateFeeAndSave() {
        stubCurrency("USD");
        when(feeCalculationService.calculateFee(request.amount(), request.currency())).thenReturn(USD_FEE);

        Payment savedPayment = TestDataFactory.paymentEntity(ID_1, request, USD_FEE);
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        PaymentResponse response = paymentService.createPayment(request);

        assertNotNull(response);
        assertEquals(ID_1, response.id());
        assertEquals(new BigDecimal("100.00"), response.amount());
        assertEquals("USD", response.currency());
        assertEquals("John Doe", response.recipient());
        assertEquals(TestDataFactory.ESTONIAN_IBAN, response.recipientAccount());
        assertEquals(USD_FEE, response.processingFee());
        assertEquals("COMPLETED", response.status());
        verify(feeCalculationService).calculateFee(request.amount(), request.currency());
    }

    @Test
    void createPaymentShouldPassCorrectDataToRepository() {
        stubCurrency("USD");
        when(feeCalculationService.calculateFee(any(), any())).thenReturn(USD_FEE);

        Payment savedPayment = TestDataFactory.paymentEntity(ID_1, request, USD_FEE);
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        paymentService.createPayment(request);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());

        Payment captured = captor.getValue();
        assertEquals(new BigDecimal("100.00"), captured.getAmount());
        assertEquals("USD", captured.getCurrency());
        assertEquals("John Doe", captured.getRecipient());
        assertEquals(TestDataFactory.ESTONIAN_IBAN, captured.getRecipientAccount());
        assertEquals(USD_FEE, captured.getProcessingFee());
        assertEquals(PaymentStatus.COMPLETED, captured.getStatus());
    }

    @Test
    void getAllPaymentsShouldReturnPagedResponses() {
        Payment payment1 = TestDataFactory.paymentEntity(ID_1, request, USD_FEE);
        Payment payment2 = TestDataFactory.paymentEntity(ID_2, request, new BigDecimal("10.0000"));

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
    void createPaymentShouldRejectAmountWithTooManyDecimalPlaces() {
        stubCurrency("USD");

        PaymentRequest badRequest = TestDataFactory.paymentRequest(new BigDecimal("100.123"), "USD");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.createPayment(badRequest)
        );

        assertTrue(exception.getMessage().contains("too many decimal places"));
    }

    @Test
    void createPaymentShouldRejectUnsupportedCurrency() {
        when(currencyService.findByCode("ZZZ")).thenReturn(Optional.empty());

        PaymentRequest badRequest = TestDataFactory.paymentRequest(new BigDecimal("100.00"), "ZZZ");

        assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.createPayment(badRequest)
        );
    }

    @Test
    void createPaymentShouldAcceptWholeAmountForZeroDecimalCurrency() {
        stubCurrency("JPY");
        when(feeCalculationService.calculateFee(any(), any())).thenReturn(new BigDecimal("500"));

        PaymentRequest jpyRequest = TestDataFactory.paymentRequest(new BigDecimal("10000"), "JPY");
        Payment savedPayment = TestDataFactory.paymentEntity(ID_1, jpyRequest, new BigDecimal("500"));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        PaymentResponse response = paymentService.createPayment(jpyRequest);

        assertNotNull(response);
        assertEquals("JPY", response.currency());
    }

    @Test
    void createPaymentShouldRejectFractionalAmountForZeroDecimalCurrency() {
        stubCurrency("JPY");

        PaymentRequest badRequest = TestDataFactory.paymentRequest(new BigDecimal("10000.5"), "JPY");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> paymentService.createPayment(badRequest)
        );

        assertTrue(exception.getMessage().contains("too many decimal places"));
        assertTrue(exception.getMessage().contains("JPY"));
    }

    @Test
    void createPaymentShouldAcceptThreeDecimalPlacesForBHD() {
        stubCurrency("BHD");
        when(feeCalculationService.calculateFee(any(), any())).thenReturn(new BigDecimal("2.000"));

        PaymentRequest bhdRequest = TestDataFactory.paymentRequest(new BigDecimal("100.500"), "BHD");
        Payment savedPayment = TestDataFactory.paymentEntity(ID_1, bhdRequest, new BigDecimal("2.000"));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        PaymentResponse response = paymentService.createPayment(bhdRequest);

        assertNotNull(response);
        assertEquals("BHD", response.currency());
    }
}
