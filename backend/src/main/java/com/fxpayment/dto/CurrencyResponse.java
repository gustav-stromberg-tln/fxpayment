package com.fxpayment.dto;

import com.fxpayment.model.Curr;

import java.math.BigDecimal;

public record CurrencyResponse(
        String code,
        String name,
        BigDecimal feePercentage,
        BigDecimal minimumFee,
        short decimals
) {
    public static CurrencyResponse from(Curr currency) {
        return new CurrencyResponse(
                currency.getCode(),
                currency.getName(),
                currency.getFeePercentage(),
                currency.getMinimumFee(),
                currency.getDecimals()
        );
    }
}
