package com.fxpayment.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupConfigLogger {

    private static final Pattern USERINFO_PATTERN = Pattern.compile("(://)[^@/]*@");

    private final Environment environment;
    private final CorsProperties corsProperties;
    private final CacheProperties cacheProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void logConfiguration() {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug("=== Application Configuration ===");
        log.debug("Datasource URL: {}", maskCredentials(environment.getProperty("spring.datasource.url")));
        log.debug("Server port: {}", environment.getProperty("server.port", "8080"));
        log.debug("Flyway enabled: {}", environment.getProperty("spring.flyway.enabled", "true"));
        log.debug("CORS allowed origins: {}", corsProperties.allowedOrigins());
        log.debug("CORS allowed methods: {}", corsProperties.allowedMethods());
        logCacheConfig();
        log.debug("=================================");
    }

    private void logCacheConfig() {
        if (cacheProperties.currency() != null) {
            log.debug("Cache currency TTL: {}, max-size: {}",
                    cacheProperties.currency().ttl(), cacheProperties.currency().maxSize());
        }
        if (cacheProperties.idempotency() != null) {
            log.debug("Cache idempotency TTL: {}, max-size: {}",
                    cacheProperties.idempotency().ttl(), cacheProperties.idempotency().maxSize());
        }
    }

    static String maskCredentials(String url) {
        if (url == null) {
            return "not configured";
        }
        return USERINFO_PATTERN.matcher(url).replaceFirst("$1***@");
    }
}
