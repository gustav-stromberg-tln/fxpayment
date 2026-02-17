package com.fxpayment.dto;

import com.fxpayment.model.CurrencyEntity;

public record CurrencyResponse(
        String code,
        String name,
        short decimals
) {
    public static CurrencyResponse from(CurrencyEntity currency) {
        return new CurrencyResponse(
                currency.getCode(),
                currency.getName(),
                currency.getDecimals()
        );
    }
}
