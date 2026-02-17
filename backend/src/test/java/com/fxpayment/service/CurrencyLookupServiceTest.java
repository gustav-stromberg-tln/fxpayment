package com.fxpayment.service;

import com.fxpayment.model.CurrencyEntity;
import com.fxpayment.repository.CurrencyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.fxpayment.utils.TestDataFactory.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyLookupServiceTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @InjectMocks
    private CurrencyLookupService currencyLookupService;

    @Test
    void findAllShouldReturnAllFromRepository() {
        List<CurrencyEntity> entities = List.of(eurCurrency(), usdCurrency(), gbpCurrency());
        when(currencyRepository.findAll()).thenReturn(entities);

        List<CurrencyEntity> result = currencyLookupService.findAll();

        assertEquals(3, result.size());
        assertEquals("EUR", result.get(0).getCode());
        assertEquals("USD", result.get(1).getCode());
        assertEquals("GBP", result.get(2).getCode());
    }

    @Test
    void findAllShouldReturnEmptyListWhenNoCurrencies() {
        when(currencyRepository.findAll()).thenReturn(Collections.emptyList());

        List<CurrencyEntity> result = currencyLookupService.findAll();

        assertTrue(result.isEmpty());
    }

    @Test
    void findByCodeShouldReturnCurrencyWhenExists() {
        CurrencyEntity usd = usdCurrency();
        when(currencyRepository.findById("USD")).thenReturn(Optional.of(usd));

        Optional<CurrencyEntity> result = currencyLookupService.findByCode("USD");

        assertTrue(result.isPresent());
        assertEquals("USD", result.get().getCode());
    }

    @Test
    void findByCodeShouldReturnEmptyForNonexistentCurrency() {
        when(currencyRepository.findById(UNSUPPORTED_CURRENCY)).thenReturn(Optional.empty());

        Optional<CurrencyEntity> result = currencyLookupService.findByCode(UNSUPPORTED_CURRENCY);

        assertTrue(result.isEmpty());
    }
}
