package com.fxpayment.service;

import com.fxpayment.dto.CurrencyResponse;
import com.fxpayment.exception.InvalidRequestException;
import com.fxpayment.model.CurrencyEntity;
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
class CurrencyServiceTest {

    @Mock
    private CurrencyLookupService currencyLookupService;

    @InjectMocks
    private CurrencyService currencyService;

    @Test
    void getAllCurrenciesShouldReturnAllFromLookupService() {
        List<CurrencyEntity> entities = List.of(eurCurrency(), usdCurrency(), gbpCurrency());
        when(currencyLookupService.findAll()).thenReturn(entities);

        List<CurrencyResponse> result = currencyService.getAllCurrencies();

        assertEquals(3, result.size());
        assertEquals("EUR", result.get(0).code());
        assertEquals("USD", result.get(1).code());
        assertEquals("GBP", result.get(2).code());
    }

    @Test
    void getAllCurrenciesShouldReturnEmptyListWhenNoCurrencies() {
        when(currencyLookupService.findAll()).thenReturn(Collections.emptyList());

        List<CurrencyResponse> result = currencyService.getAllCurrencies();

        assertTrue(result.isEmpty());
    }

    @Test
    void findByCodeShouldReturnCurrencyWhenExists() {
        CurrencyEntity usd = usdCurrency();
        when(currencyLookupService.findByCode("USD")).thenReturn(Optional.of(usd));

        Optional<CurrencyEntity> result = currencyService.findByCode("USD");

        assertTrue(result.isPresent());
        assertEquals("USD", result.get().getCode());
    }

    @Test
    void findByCodeShouldReturnEmptyForNonexistentCurrency() {
        when(currencyLookupService.findByCode(UNSUPPORTED_CURRENCY)).thenReturn(Optional.empty());

        Optional<CurrencyEntity> result = currencyService.findByCode(UNSUPPORTED_CURRENCY);

        assertTrue(result.isEmpty());
    }

    @Test
    void getDecimalsShouldReturnDecimalsForExistingCurrency() {
        when(currencyLookupService.findByCode("JPY")).thenReturn(Optional.of(jpyCurrency()));

        assertEquals(0, currencyService.getDecimals("JPY"));
    }

    @Test
    void getDecimalsShouldThrowForNonexistentCurrency() {
        when(currencyLookupService.findByCode(UNSUPPORTED_CURRENCY)).thenReturn(Optional.empty());

        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> currencyService.getDecimals(UNSUPPORTED_CURRENCY)
        );
        assertTrue(exception.getMessage().contains(UNSUPPORTED_CURRENCY));
    }
}
