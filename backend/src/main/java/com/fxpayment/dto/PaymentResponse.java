package com.fxpayment.dto;

import com.fxpayment.model.Payment;
import com.fxpayment.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        BigDecimal amount,
        String currency,
        String recipient,
        BigDecimal processingFee,
        Instant createdAt
) {
    public static PaymentResponse from(Payment payment, int currencyDecimals) {
        return new PaymentResponse(
                payment.getId(),
                MoneyUtil.roundToScale(payment.getAmount(), currencyDecimals),
                payment.getCurrency(),
                payment.getRecipient(),
                MoneyUtil.roundToScale(payment.getProcessingFee(), currencyDecimals),
                payment.getCreatedAt()
        );
    }
}
