package com.fxpayment.config;

import com.fxpayment.model.CurrencyEntity;
import com.fxpayment.repository.CurrencyRepository;
import com.fxpayment.service.CurrencyLookupService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.Optional;

import static com.fxpayment.utils.TestDataFactory.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.cache.enabled=true")
@DisplayName("Caffeine cache behaviour")
class CacheIntegrationTest {

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

    @Test
    @DisplayName("findByCode should hit database only on first call")
    void findByCodeShouldHitDatabaseOnlyOnce() {
        Optional<CurrencyEntity> first = currencyLookupService.findByCode("USD");
        Optional<CurrencyEntity> second = currencyLookupService.findByCode("USD");

        assertTrue(first.isPresent());
        assertTrue(second.isPresent());
        assertEquals("USD", first.get().getCode());
        assertEquals("US Dollar", first.get().getName());
        assertEquals("USD", second.get().getCode());
        verify(currencyRepository, times(1)).findById("USD");
    }

    @Test
    @DisplayName("findAll should hit database only on first call")
    void findAllShouldHitDatabaseOnlyOnce() {
        List<CurrencyEntity> first = currencyLookupService.findAll();
        List<CurrencyEntity> second = currencyLookupService.findAll();

        assertEquals(3, first.size());
        assertEquals(3, second.size());
        verify(currencyRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("findByCode should cache different currency codes independently")
    void findByCodeShouldCacheDifferentKeysIndependently() {
        currencyLookupService.findByCode("USD");
        currencyLookupService.findByCode("EUR");
        currencyLookupService.findByCode("GBP");

        // Repeated lookups for the same codes should not hit DB again
        currencyLookupService.findByCode("USD");
        currencyLookupService.findByCode("EUR");
        currencyLookupService.findByCode("GBP");

        verify(currencyRepository, times(1)).findById("USD");
        verify(currencyRepository, times(1)).findById("EUR");
        verify(currencyRepository, times(1)).findById("GBP");
    }

    @Test
    @DisplayName("cache eviction should force next findByCode to hit database")
    void cacheEvictionShouldForceDbCall() {
        currencyLookupService.findByCode("USD");
        verify(currencyRepository, times(1)).findById("USD");

        evictAllCaches();

        currencyLookupService.findByCode("USD");
        verify(currencyRepository, times(2)).findById("USD");
    }

    @Test
    @DisplayName("cache eviction should force next findAll to hit database")
    void findAllEvictionShouldForceDbCall() {
        currencyLookupService.findAll();
        verify(currencyRepository, times(1)).findAll();

        evictAllCaches();

        currencyLookupService.findAll();
        verify(currencyRepository, times(2)).findAll();
    }

}
