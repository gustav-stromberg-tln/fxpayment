package com.fxpayment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "cache")
public record CacheProperties(
        CurrencyProperties currency,
        IdempotencyProperties idempotency
) {
    public record CurrencyProperties(Duration ttl, long maxSize) {}

    public record IdempotencyProperties(Duration ttl, long maxSize) {}
}
