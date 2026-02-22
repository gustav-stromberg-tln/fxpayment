package com.fxpayment.config;

import com.fxpayment.model.CurrencyEntity;
import com.fxpayment.repository.CurrencyRepository;
import com.fxpayment.service.CurrencyLookupService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.fxpayment.utils.TestDataFactory.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.cache.enabled=true")
@DisplayName("Cache edge case tests")
class CacheEdgeCaseIntegrationTest {

    @MockitoSpyBean
    private CurrencyRepository currencyRepository;

    @Autowired
    private CurrencyLookupService currencyLookupService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        evictAllCaches();
        seedCurrencies(currencyRepository);
        clearInvocations(currencyRepository);
    }

    @AfterEach
    void tearDown() {
        currencyRepository.deleteAll();
    }

    private void evictAllCaches() {
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    @Nested
    @DisplayName("Stale cache behaviour (3b)")
    class StaleCacheBehaviour {

        @Test
        @DisplayName("currency cache returns stale value after DB update until evicted")
        void currencyCacheShouldReturnStaleValueAfterDbUpdate() {
            // Cache the USD currency
            Optional<CurrencyEntity> cached = currencyLookupService.findByCode("USD");
            assertTrue(cached.isPresent());
            assertEquals(0, new BigDecimal("0.01").compareTo(cached.get().getFeeRate()));

            // Update the fee rate directly in the database
            CurrencyEntity updated = CurrencyEntity.builder()
                    .code("USD")
                    .name("US Dollar")
                    .feeRate(new BigDecimal("0.0200"))
                    .minimumFee(new BigDecimal("5.0000"))
                    .decimals((short) 2)
                    .build();
            currencyRepository.save(updated);
            clearInvocations(currencyRepository);

            // Cache should still return the old value
            Optional<CurrencyEntity> stale = currencyLookupService.findByCode("USD");
            assertTrue(stale.isPresent());
            assertEquals(0, new BigDecimal("0.01").compareTo(stale.get().getFeeRate()));
            verify(currencyRepository, never()).findById("USD");

            // After eviction, fresh value from DB
            evictAllCaches();
            Optional<CurrencyEntity> fresh = currencyLookupService.findByCode("USD");
            assertTrue(fresh.isPresent());
            assertEquals(0, new BigDecimal("0.02").compareTo(fresh.get().getFeeRate()));
            verify(currencyRepository, times(1)).findById("USD");
        }

        @Test
        @DisplayName("findAll cache returns stale list after DB addition until evicted")
        void findAllCacheShouldReturnStaleListAfterDbAddition() {
            // Cache all currencies
            List<CurrencyEntity> initial = currencyLookupService.findAll();
            assertEquals(3, initial.size());

            // Add a new currency directly in DB
            currencyRepository.save(jpyCurrency());
            clearInvocations(currencyRepository);

            // Cache still returns 3
            List<CurrencyEntity> stale = currencyLookupService.findAll();
            assertEquals(3, stale.size());
            verify(currencyRepository, never()).findAll();

            // After eviction, we see 4
            evictAllCaches();
            List<CurrencyEntity> fresh = currencyLookupService.findAll();
            assertEquals(4, fresh.size());
        }
    }
}
