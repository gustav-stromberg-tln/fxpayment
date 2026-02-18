package com.fxpayment.utils;

import com.fxpayment.model.CurrencyEntity;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.Instant;

// Defaults to USD with 1% fee rate, 5 minimum fee, and 2 decimal places.
@Setter
@Accessors(fluent = true, chain = true)
public final class CurrencyEntityBuilder {

    private String code = "USD";
    private String name = "US Dollar";
    private BigDecimal feeRate = new BigDecimal("0.0100");
    private BigDecimal minimumFee = new BigDecimal("5.0000");
    private short decimals = 2;
    private Instant createdAt;
    private Instant updatedAt;

    public CurrencyEntity build() {
        return CurrencyEntity.builder()
                .code(code)
                .name(name)
                .feeRate(feeRate)
                .minimumFee(minimumFee)
                .decimals(decimals)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }
}
