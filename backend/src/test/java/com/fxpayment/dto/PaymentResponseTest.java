package com.fxpayment.dto;

import com.fxpayment.model.Payment;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static com.fxpayment.utils.TestDataFactory.*;
import static org.junit.jupiter.api.Assertions.*;

class PaymentResponseTest {

    private static final UUID PAYMENT_ID = UUID.fromString(PAYMENT_UUID_1);
    private static final Instant FIXED_CREATED_AT = Instant.parse("2025-01-15T10:30:00Z");

    @Test
    void fromShouldMapAllFields() {
        Payment payment = aPayment()
                .id(PAYMENT_ID)
                .amount(new BigDecimal("250.50"))
                .currency("GBP")
                .recipient("Jane Smith")
                .recipientAccount(GERMAN_IBAN)
                .processingFee(new BigDecimal("5.00"))
                .createdAt(FIXED_CREATED_AT)
                .build();

        PaymentResponse response = PaymentResponse.from(payment, 2);

        assertEquals(PAYMENT_ID, response.id());
        assertEquals(new BigDecimal("250.50"), response.amount());
        assertEquals("GBP", response.currency());
        assertEquals("Jane Smith", response.recipient());
        assertEquals(new BigDecimal("5.00"), response.processingFee());
        assertEquals(FIXED_CREATED_AT, response.createdAt());
    }

    @Test
    void fromShouldRoundInternalPrecisionToDisplayDecimals() {
        Payment payment = aPayment()
                .processingFee(new BigDecimal("5.0050"))
                .createdAt(Instant.now())
                .build();

        PaymentResponse response = PaymentResponse.from(payment, 2);

        assertEquals(new BigDecimal("100.00"), response.amount());
        assertEquals(new BigDecimal("5.01"), response.processingFee());
    }

    @Test
    void fromShouldRoundToZeroDecimalsForZeroDecimalCurrency() {
        Payment payment = aPayment()
                .amount(new BigDecimal("1000.50"))
                .processingFee(new BigDecimal("500.99"))
                .createdAt(Instant.now())
                .build();

        PaymentResponse response = PaymentResponse.from(payment, 0);

        assertEquals(new BigDecimal("1001"), response.amount());
        assertEquals(new BigDecimal("501"), response.processingFee());
    }

    @Test
    void fromShouldRoundToThreeDecimalsForThreeDecimalCurrency() {
        Payment payment = aPayment()
                .amount(new BigDecimal("100.12345"))
                .processingFee(new BigDecimal("2.5555"))
                .createdAt(Instant.now())
                .build();

        PaymentResponse response = PaymentResponse.from(payment, 3);

        assertEquals(new BigDecimal("100.123"), response.amount());
        assertEquals(new BigDecimal("2.556"), response.processingFee());
    }

    @Test
    void fromShouldThrowOnNullPayment() {
        assertThrows(NullPointerException.class, () -> PaymentResponse.from(null, 2));
    }

    @Test
    void responsesWithSameDataShouldBeEqual() {
        Payment payment = aPayment()
                .id(PAYMENT_ID)
                .amount(new BigDecimal("100.0000"))
                .processingFee(new BigDecimal("5.0000"))
                .createdAt(FIXED_CREATED_AT)
                .build();

        PaymentResponse r1 = PaymentResponse.from(payment, 2);
        PaymentResponse r2 = PaymentResponse.from(payment, 2);

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void responsesWithDifferentAmountsShouldNotBeEqual() {
        Payment payment1 = aPayment().id(PAYMENT_ID).amount(new BigDecimal("100.0000")).createdAt(FIXED_CREATED_AT).build();
        Payment payment2 = aPayment().id(PAYMENT_ID).amount(new BigDecimal("200.0000")).createdAt(FIXED_CREATED_AT).build();

        PaymentResponse r1 = PaymentResponse.from(payment1, 2);
        PaymentResponse r2 = PaymentResponse.from(payment2, 2);

        assertNotEquals(r1, r2);
    }
}
