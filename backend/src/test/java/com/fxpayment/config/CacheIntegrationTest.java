package com.fxpayment.config;

import com.fxpayment.model.CurrencyEntity;
import com.fxpayment.model.Payment;
import com.fxpayment.repository.CurrencyRepository;
import com.fxpayment.repository.PaymentRepository;
import com.fxpayment.service.CurrencyLookupService;
import com.fxpayment.service.IdempotencyCacheService;
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
import java.util.UUID;

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

    @MockitoSpyBean
    private PaymentRepository paymentRepository;

    @Autowired
    private CurrencyLookupService currencyLookupService;

    @Autowired
    private IdempotencyCacheService idempotencyCacheService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        evictAllCaches();
        seedCurrencies(currencyRepository);
        clearInvocations(currencyRepository, paymentRepository);
    }

    @AfterEach
    void tearDown() {
        paymentRepository.deleteAll();
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

    @Test
    @DisplayName("@CachePut should populate cache so @Cacheable returns without DB call")
    void cachePutShouldPopulateCacheForSubsequentLookup() {
        String idempotencyKey = UUID.randomUUID().toString();
        Payment payment = aPayment().idempotencyKey(idempotencyKey).build();
        Payment saved = paymentRepository.save(payment);
        clearInvocations(paymentRepository);

        // @CachePut stores the payment in the idempotencyKeys cache
        idempotencyCacheService.cachePayment(idempotencyKey, saved);

        // @Cacheable should find it in cache without querying the database
        Optional<Payment> found = idempotencyCacheService.findExistingPayment(idempotencyKey);

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals(saved.getCurrency(), found.get().getCurrency());
        assertEquals(saved.getAmount(), found.get().getAmount());
        verify(paymentRepository, never()).findByIdempotencyKey(idempotencyKey);
    }

    @Test
    @DisplayName("findExistingPayment should cache result after first DB call")
    void findExistingPaymentShouldCacheAfterFirstLookup() {
        String idempotencyKey = UUID.randomUUID().toString();
        Payment payment = aPayment().idempotencyKey(idempotencyKey).build();
        paymentRepository.save(payment);
        clearInvocations(paymentRepository);

        // First call — cache miss, hits DB
        Optional<Payment> first = idempotencyCacheService.findExistingPayment(idempotencyKey);
        // Second call — cache hit, no DB
        Optional<Payment> second = idempotencyCacheService.findExistingPayment(idempotencyKey);

        assertTrue(first.isPresent());
        assertTrue(second.isPresent());
        assertEquals(first.get().getId(), second.get().getId());
        verify(paymentRepository, times(1)).findByIdempotencyKey(idempotencyKey);
    }

    @Test
    @DisplayName("idempotency cache should store different keys independently")
    void idempotencyCacheShouldStoreDifferentKeysIndependently() {
        String key1 = UUID.randomUUID().toString();
        String key2 = UUID.randomUUID().toString();
        Payment payment1 = aPayment().idempotencyKey(key1).recipient("Alice").build();
        Payment payment2 = aPayment().idempotencyKey(key2).recipient("Bob").build();
        paymentRepository.save(payment1);
        paymentRepository.save(payment2);
        clearInvocations(paymentRepository);

        // First lookups — both hit DB
        Optional<Payment> found1 = idempotencyCacheService.findExistingPayment(key1);
        Optional<Payment> found2 = idempotencyCacheService.findExistingPayment(key2);

        // Second lookups — both from cache
        idempotencyCacheService.findExistingPayment(key1);
        idempotencyCacheService.findExistingPayment(key2);

        assertTrue(found1.isPresent());
        assertTrue(found2.isPresent());
        assertEquals("Alice", found1.get().getRecipient());
        assertEquals("Bob", found2.get().getRecipient());
        verify(paymentRepository, times(1)).findByIdempotencyKey(key1);
        verify(paymentRepository, times(1)).findByIdempotencyKey(key2);
    }
}
