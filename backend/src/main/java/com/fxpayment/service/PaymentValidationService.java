package com.fxpayment.service;

import com.fxpayment.dto.PaymentRequest;
import com.fxpayment.exception.InvalidRequestException;
import com.fxpayment.model.CurrencyEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentValidationService {

    private final CurrencyService currencyService;

    public CurrencyEntity resolveAndValidateCurrency(PaymentRequest request) {
        CurrencyEntity currency = currencyService.findByCode(request.currency())
                .orElseThrow(() -> {
                    log.warn("Unsupported currency code: {}", request.currency());
                    return new InvalidRequestException("Unsupported currency code: " + request.currency());
                });

        if (request.amount().scale() > currency.getDecimals()) {
            log.warn("Amount decimal places exceeded: currency={}, allowed={}, actual={}",
                    currency.getCode(), currency.getDecimals(), request.amount().scale());
            throw new InvalidRequestException(
                    "Amount has too many decimal places for currency " + currency.getCode()
                            + ": maximum " + currency.getDecimals() + " allowed");
        }
        return currency;
    }
}
