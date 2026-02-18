package com.fxpayment.utils;

import com.fxpayment.model.CurrencyEntity;
import com.fxpayment.repository.CurrencyRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class TestDataFactory {

    public static final String ESTONIAN_IBAN = "EE382200221020145685";
    public static final String SWEDISH_IBAN = "SE4550000000058398257466";
    public static final String FINNISH_IBAN = "FI2112345600000785";
    public static final String GERMAN_IBAN = "DE89370400440532013000";

    public static final String UNSUPPORTED_CURRENCY = "ZZZ";
    public static final String RECIPIENT_WITH_DIACRITICS = "Müller Ödegård";

    public static final String PAYMENT_UUID_1 = "00000000-0000-0000-0000-000000000001";
    public static final String PAYMENT_UUID_2 = "00000000-0000-0000-0000-000000000002";
    public static final String PAYMENTS_API_PATH = "/api/v1/payments";
    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    public static final Map<String, CurrencyEntity> CURRENCIES = Map.of(
            "EUR", eurCurrency(),
            "USD", usdCurrency(),
            "GBP", gbpCurrency(),
            "JPY", jpyCurrency(),
            "BHD", bhdCurrency()
    );

    public static PaymentRequestBuilder aPaymentRequest() {
        return new PaymentRequestBuilder();
    }

    public static TestPaymentBuilder aPayment() {
        return new TestPaymentBuilder();
    }

    public static CurrencyEntityBuilder aCurrency() {
        return new CurrencyEntityBuilder();
    }

    public static void seedCurrencies(CurrencyRepository currencyRepository) {
        currencyRepository.saveAllAndFlush(List.of(usdCurrency(), eurCurrency(), gbpCurrency()));
    }

    public static CurrencyEntity usdCurrency() {
        return aCurrency().build();
    }

    public static CurrencyEntity eurCurrency() {
        return aCurrency()
                .code("EUR").name("Euro")
                .feeRate(new BigDecimal("0.0000"))
                .minimumFee(new BigDecimal("0.0000"))
                .decimals((short) 2)
                .build();
    }

    public static CurrencyEntity gbpCurrency() {
        return aCurrency()
                .code("GBP").name("British Pound")
                .feeRate(new BigDecimal("0.0100"))
                .minimumFee(new BigDecimal("5.0000"))
                .decimals((short) 2)
                .build();
    }

    public static CurrencyEntity jpyCurrency() {
        return aCurrency()
                .code("JPY").name("Japanese Yen")
                .feeRate(new BigDecimal("0.0100"))
                .minimumFee(new BigDecimal("500"))
                .decimals((short) 0)
                .build();
    }

    public static CurrencyEntity bhdCurrency() {
        return aCurrency()
                .code("BHD").name("Bahraini Dinar")
                .feeRate(new BigDecimal("0.0100"))
                .minimumFee(new BigDecimal("2.000"))
                .decimals((short) 3)
                .build();
    }
}
