package com.fxpayment.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
@RequiredArgsConstructor
public class CacheConfig {

    private final CacheProperties cacheProperties;

    @Bean
    @ConditionalOnProperty(name = "app.cache.enabled", matchIfMissing = true)
    public CacheManager cacheManager() {
        CacheProperties.CurrencyProperties currency = cacheProperties.currency();
        CacheProperties.IdempotencyProperties idempotency = cacheProperties.idempotency();

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                buildCurrencyCache("currencies", currency),
                new CaffeineCache("idempotencyKeys", Caffeine.newBuilder()
                        .maximumSize(idempotency.maxSize())
                        .expireAfterWrite(idempotency.ttl())
                        .build())
        ));

        return manager;
    }

    private CaffeineCache buildCurrencyCache(String name, CacheProperties.CurrencyProperties props) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .maximumSize(props.maxSize())
                .expireAfterWrite(props.ttl())
                .build());
    }
}
