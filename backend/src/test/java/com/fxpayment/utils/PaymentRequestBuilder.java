package com.fxpayment.utils;

import com.fxpayment.dto.PaymentRequest;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

// Defaults to a 100 USD payment to "John Doe" at the Estonian IBAN.
@Setter
@Accessors(fluent = true, chain = true)
public final class PaymentRequestBuilder {

    private BigDecimal amount = new BigDecimal("100.00");
    private String currency = "USD";
    private String recipient = "John Doe";
    private String recipientAccount = TestDataFactory.ESTONIAN_IBAN;

    public PaymentRequest build() {
        return new PaymentRequest(amount, currency, recipient, recipientAccount);
    }
}
