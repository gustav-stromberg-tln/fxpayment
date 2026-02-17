package com.fxpayment.utils;

import com.fxpayment.model.Payment;
import com.fxpayment.model.PaymentStatus;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Defaults to a completed 100 USD payment to "John Doe", an Estonian IBAN,
// with a 5 USD processing fee, a random idempotency key and a fixed creation timestamp.
@Setter
@Accessors(fluent = true, chain = true)
public final class TestPaymentBuilder {

    private UUID id;
    private BigDecimal amount = new BigDecimal("100.0000");
    private String currency = "USD";
    private String recipient = "John Doe";
    private String recipientAccount = TestDataFactory.ESTONIAN_IBAN;
    private BigDecimal processingFee = new BigDecimal("5.0000");
    private String idempotencyKey = UUID.randomUUID().toString();
    private PaymentStatus status = PaymentStatus.COMPLETED;
    private Instant createdAt = Instant.parse("2025-01-15T10:30:00Z");
    private Instant updatedAt;

    public Payment build() {
        return Payment.builder()
                .id(id)
                .amount(amount)
                .currency(currency)
                .recipient(recipient)
                .recipientAccount(recipientAccount)
                .processingFee(processingFee)
                .idempotencyKey(idempotencyKey)
                .status(status)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
