package com.fxpayment.dto;

import com.fxpayment.model.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        BigDecimal amount,
        String currency,
        String recipient,
        String recipientAccount,
        BigDecimal processingFee,
        String status,
        Instant createdAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getRecipient(),
                payment.getRecipientAccount(),
                payment.getProcessingFee(),
                payment.getStatus().name(),
                payment.getCreatedAt()
        );
    }
}
