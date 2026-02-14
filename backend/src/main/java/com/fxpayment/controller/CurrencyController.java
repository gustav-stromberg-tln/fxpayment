package com.fxpayment.controller;

import com.fxpayment.dto.CurrencyResponse;
import com.fxpayment.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/currencies")
@RequiredArgsConstructor
public class CurrencyController {

    private final CurrencyService currencyService;

    @GetMapping
    public ResponseEntity<List<CurrencyResponse>> getAllCurrencies() {
        log.debug("Fetching all currencies");
        List<CurrencyResponse> currencies = currencyService.getAllCurrencies().stream()
                .map(CurrencyResponse::from)
                .toList();
        log.debug("Returning {} currencies", currencies.size());
        return ResponseEntity.ok(currencies);
    }
}