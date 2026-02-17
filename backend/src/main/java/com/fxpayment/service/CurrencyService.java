package com.fxpayment.service;

import com.fxpayment.dto.CurrencyResponse;
import com.fxpayment.exception.InvalidRequestException;
import com.fxpayment.model.CurrencyEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final CurrencyLookupService currencyLookupService;

    public List<CurrencyResponse> getAllCurrencies() {
        return currencyLookupService.findAll().stream()
                .map(CurrencyResponse::from)
                .toList();
    }

    public Optional<CurrencyEntity> findByCode(String code) {
        return currencyLookupService.findByCode(code);
    }

    public int getDecimals(String code) {
        return currencyLookupService.findByCode(code)
                .orElseThrow(() -> {
                    log.error("Currency not found during decimals lookup: code={}", code);
                    return new InvalidRequestException("Currency not found: " + code);
                })
                .getDecimals();
    }
}
