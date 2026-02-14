package com.fxpayment.dto;

import com.fxpayment.validation.ValidIban;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        @DecimalMax(value = "1000000", message = "Amount exceeds maximum transaction limit")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
        String currency,

        @NotBlank(message = "Recipient is required")
        @Size(max = 255, message = "Recipient name must not exceed 255 characters")
        String recipient,

        @NotBlank(message = "Recipient account is required")
        @ValidIban(message = "Invalid IBAN: must be a valid IBAN with correct check digits")
        String recipientAccount
) {
}
