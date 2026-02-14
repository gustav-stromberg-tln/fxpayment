package com.fxpayment.service;

import com.fxpayment.model.Curr;
import com.fxpayment.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final CurrencyRepository currencyRepository;

    public List<Curr> getAllCurrencies() {
        log.debug("Loading all currencies from database");
        return currencyRepository.findAll();
    }

    public Optional<Curr> findByCode(String code) {
        log.debug("Loading currency from database: code={}", code);
        return currencyRepository.findById(code);
    }
}
