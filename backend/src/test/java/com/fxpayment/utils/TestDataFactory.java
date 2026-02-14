package com.fxpayment.utils;

import com.fxpayment.dto.PaymentRequest;
import com.fxpayment.model.Curr;
import com.fxpayment.model.Payment;
import com.fxpayment.model.PaymentStatus;
import com.fxpayment.repository.CurrencyRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TestDataFactory {

    public static final String ESTONIAN_IBAN = "EE382200221020145685";
    public static final String FINNISH_IBAN = "FI2112345600000785";

    private TestDataFactory() {}

    public static void seedCurrencies(CurrencyRepository currencyRepository) {
        currencyRepository.saveAll(List.of(usdCurrency(), eurCurrency(), gbpCurrency()));
    }

    public static Curr eurCurrency() {
        return Curr.builder()
                .code("EUR").name("Euro")
                .feePercentage(new BigDecimal("0.0000"))
                .minimumFee(new BigDecimal("0.0000"))
                .decimals((short) 2)
                .build();
    }

    public static Curr usdCurrency() {
        return Curr.builder()
                .code("USD").name("US Dollar")
                .feePercentage(new BigDecimal("0.0100"))
                .minimumFee(new BigDecimal("5.0000"))
                .decimals((short) 2)
                .build();
    }

    public static Curr gbpCurrency() {
        return Curr.builder()
                .code("GBP").name("British Pound")
                .feePercentage(new BigDecimal("0.0100"))
                .minimumFee(new BigDecimal("5.0000"))
                .decimals((short) 2)
                .build();
    }

    public static Curr jpyCurrency() {
        return Curr.builder()
                .code("JPY").name("Japanese Yen")
                .feePercentage(new BigDecimal("0.0100"))
                .minimumFee(new BigDecimal("500"))
                .decimals((short) 0)
                .build();
    }

    public static Curr bhdCurrency() {
        return Curr.builder()
                .code("BHD").name("Bahraini Dinar")
                .feePercentage(new BigDecimal("0.0100"))
                .minimumFee(new BigDecimal("2.000"))
                .decimals((short) 3)
                .build();
    }

    public static Payment.PaymentBuilder defaultPaymentBuilder() {
        return Payment.builder()
                .amount(new BigDecimal("100.0000"))
                .currency("USD")
                .recipient("Test Recipient")
                .recipientAccount(FINNISH_IBAN)
                .processingFee(new BigDecimal("5.0000"))
                .status(PaymentStatus.COMPLETED);
    }

    public static PaymentRequest paymentRequest() {
        return paymentRequest(new BigDecimal("100.00"), "USD");
    }

    public static PaymentRequest paymentRequest(BigDecimal amount, String currency) {
        return new PaymentRequest(amount, currency, "John Doe", ESTONIAN_IBAN);
    }

    public static Payment paymentEntity(UUID id, PaymentRequest request, BigDecimal fee) {
        return Payment.builder()
                .id(id)
                .amount(request.amount())
                .currency(request.currency())
                .recipient(request.recipient())
                .recipientAccount(request.recipientAccount())
                .processingFee(fee)
                .status(PaymentStatus.COMPLETED)
                .createdAt(Instant.parse("2025-01-15T10:30:00Z"))
                .build();
    }
}
