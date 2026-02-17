package com.fxpayment.service;

import com.fxpayment.model.Payment;
import com.fxpayment.model.PaymentStatus;
import com.fxpayment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static com.fxpayment.utils.TestDataFactory.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdempotencyCacheServiceTest {

    private static final UUID PAYMENT_ID = UUID.fromString(PAYMENT_UUID_1);

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private IdempotencyCacheService idempotencyCacheService;

    private String newIdempotencyKey() {
        return UUID.randomUUID().toString();
    }

    @Test
    void findExistingPaymentShouldReturnPaymentWithAllFields() {
        String idempotencyKey = newIdempotencyKey();
        Payment payment = aPayment().id(PAYMENT_ID).build();
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(payment));

        Optional<Payment> result = idempotencyCacheService.findExistingPayment(idempotencyKey);

        assertTrue(result.isPresent());
        Payment found = result.get();
        assertEquals(PAYMENT_ID, found.getId());
        assertEquals(new BigDecimal("100.0000"), found.getAmount());
        assertEquals("USD", found.getCurrency());
        assertEquals("John Doe", found.getRecipient());
        assertEquals(ESTONIAN_IBAN, found.getRecipientAccount());
        assertEquals(new BigDecimal("5.0000"), found.getProcessingFee());
        assertEquals(PaymentStatus.COMPLETED, found.getStatus());
    }

    @Test
    void findExistingPaymentShouldReturnEmptyWhenNotFound() {
        String idempotencyKey = newIdempotencyKey();
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());

        Optional<Payment> result = idempotencyCacheService.findExistingPayment(idempotencyKey);

        assertTrue(result.isEmpty());
    }

    @Test
    void cachePaymentShouldReturnPaymentWithAllFields() {
        String idempotencyKey = newIdempotencyKey();
        Payment payment = aPayment().id(PAYMENT_ID).build();

        Optional<Payment> result = idempotencyCacheService.cachePayment(idempotencyKey, payment);

        assertTrue(result.isPresent());
        Payment cached = result.get();
        assertEquals(PAYMENT_ID, cached.getId());
        assertEquals(new BigDecimal("100.0000"), cached.getAmount());
        assertEquals("USD", cached.getCurrency());
        assertEquals("John Doe", cached.getRecipient());
        assertEquals(ESTONIAN_IBAN, cached.getRecipientAccount());
        assertEquals(new BigDecimal("5.0000"), cached.getProcessingFee());
        assertEquals(PaymentStatus.COMPLETED, cached.getStatus());
    }

    @Test
    void findExistingPaymentShouldReturnDiversePaymentData() {
        String idempotencyKey = newIdempotencyKey();
        Payment payment = aPayment()
                .id(PAYMENT_ID)
                .amount(new BigDecimal("999999.0000"))
                .currency("EUR")
                .recipient(RECIPIENT_WITH_DIACRITICS)
                .recipientAccount(GERMAN_IBAN)
                .processingFee(new BigDecimal("0.0000"))
                .build();
        when(paymentRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(payment));

        Optional<Payment> result = idempotencyCacheService.findExistingPayment(idempotencyKey);

        assertTrue(result.isPresent());
        assertEquals(new BigDecimal("999999.0000"), result.get().getAmount());
        assertEquals("EUR", result.get().getCurrency());
        assertEquals(RECIPIENT_WITH_DIACRITICS, result.get().getRecipient());
        assertEquals(GERMAN_IBAN, result.get().getRecipientAccount());
        assertEquals(new BigDecimal("0.0000"), result.get().getProcessingFee());
    }
}
