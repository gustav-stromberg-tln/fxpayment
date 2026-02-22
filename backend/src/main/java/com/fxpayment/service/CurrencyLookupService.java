package com.fxpayment.service;

import com.fxpayment.model.CurrencyEntity;
import com.fxpayment.repository.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurrencyLookupService {

    private final CurrencyRepository currencyRepository;

    @Cacheable("allCurrencies")
    public List<CurrencyEntity> findAll() {
        log.debug("Loading all currencies from database");
        return currencyRepository.findAll();
    }

    @Cacheable(value = "currencyByCode", key = "#code")
    public Optional<CurrencyEntity> findByCode(String code) {
        log.debug("Loading currency from database: code={}", code);
        return currencyRepository.findById(code);
    }
}
