package com.fxpayment.config;

import com.fxpayment.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.cache.enabled", matchIfMissing = true)
public class CacheWarmingListener {

    private final CurrencyService currencyService;

    @EventListener(ApplicationReadyEvent.class)
    public void warmCurrencyCache() {
        log.info("Warming currency cache on startup");
        var currencies = currencyService.getAllCurrencies();
        log.info("Currency cache warmed with {} entries", currencies.size());
    }
}
