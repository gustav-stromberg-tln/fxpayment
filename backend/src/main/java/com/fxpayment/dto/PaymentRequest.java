package com.fxpayment.dto;

import com.fxpayment.util.PaymentConstants;
import com.fxpayment.validation.ValidIban;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = PaymentConstants.MIN_AMOUNT, message = "Amount must be at least " + PaymentConstants.MIN_AMOUNT)
        @DecimalMax(value = PaymentConstants.MAX_AMOUNT, message = "Amount exceeds maximum transaction limit")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Size(min = PaymentConstants.CURRENCY_CODE_LENGTH, max = PaymentConstants.CURRENCY_CODE_LENGTH,
                message = "Currency code must be exactly " + PaymentConstants.CURRENCY_CODE_LENGTH + " characters")
        String currency,

        @NotBlank(message = "Recipient is required")
        @Size(min = PaymentConstants.MIN_RECIPIENT_LENGTH,
                max = PaymentConstants.MAX_RECIPIENT_LENGTH,
                message = "Recipient name must be between " + PaymentConstants.MIN_RECIPIENT_LENGTH
                        + " and " + PaymentConstants.MAX_RECIPIENT_LENGTH + " characters")
        @Pattern(regexp = PaymentConstants.RECIPIENT_NAME_PATTERN,
                message = "Recipient name must contain only Latin letters. Numbers and non-Latin characters are not allowed")
        String recipient,

        @NotBlank(message = "Recipient account is required")
        @ValidIban(message = "Invalid IBAN: must be a valid IBAN with correct check digits")
        String recipientAccount
) {
    public PaymentRequest normalised() {
        return new PaymentRequest(
                amount,
                currency,
                StringUtils.normalizeSpace(recipient),
                StringUtils.deleteWhitespace(recipientAccount).toUpperCase()
        );
    }
}
