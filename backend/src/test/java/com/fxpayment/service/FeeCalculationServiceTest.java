package com.fxpayment.service;

import com.fxpayment.model.Curr;
import com.fxpayment.utils.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeCalculationServiceTest {

    @Mock
    private CurrencyService currencyService;

    @InjectMocks
    private FeeCalculationService feeCalculationService;

    private final Map<String, Curr> currencies = Map.of(
            "EUR", TestDataFactory.eurCurrency(),
            "USD", TestDataFactory.usdCurrency(),
            "GBP", TestDataFactory.gbpCurrency(),
            "JPY", TestDataFactory.jpyCurrency(),
            "BHD", TestDataFactory.bhdCurrency()
    );

    @ParameterizedTest(name = "{0} {1} -> fee {2}")
    @CsvSource({
            // 2-decimal currencies
            "1000.00, EUR, 0.00",
            "100.00,  USD, 5.00",
            "500.00,  USD, 5.00",
            "501.00,  USD, 5.01",
            "1000.00, USD, 10.00",
            "1.00,    USD, 5.00",
            "200.00,  GBP, 5.00",
            "5000.00, GBP, 50.00",
            // 0-decimal currency (JPY) — no fractional yen
            "100000,  JPY, 1000",
            "10000,   JPY, 500",
            "1000,    JPY, 500",
            // 3-decimal currency (BHD) — three minor unit digits
            "1000.000, BHD, 10.000",
            "100.000,  BHD, 2.000",
            "50.000,   BHD, 2.000",
    })
    void shouldCalculateCorrectFee(String amount, String currencyCode, String expectedFee) {
        when(currencyService.findByCode(currencyCode))
                .thenReturn(Optional.of(currencies.get(currencyCode)));

        BigDecimal fee = feeCalculationService.calculateFee(new BigDecimal(amount), currencyCode);

        assertEquals(new BigDecimal(expectedFee), fee);
    }

    @Test
    void feeScaleShouldMatchCurrencyDecimals() {
        when(currencyService.findByCode("JPY")).thenReturn(Optional.of(currencies.get("JPY")));
        when(currencyService.findByCode("BHD")).thenReturn(Optional.of(currencies.get("BHD")));
        when(currencyService.findByCode("USD")).thenReturn(Optional.of(currencies.get("USD")));

        assertEquals(0, feeCalculationService.calculateFee(new BigDecimal("100000"), "JPY").scale());
        assertEquals(3, feeCalculationService.calculateFee(new BigDecimal("1000.000"), "BHD").scale());
        assertEquals(2, feeCalculationService.calculateFee(new BigDecimal("1000.00"), "USD").scale());
    }

    @Test
    void unknownCurrencyShouldThrowException() {
        when(currencyService.findByCode("XYZ")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> feeCalculationService.calculateFee(new BigDecimal("100.00"), "XYZ"));
    }
}
