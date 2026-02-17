package com.fxpayment.service;

import com.fxpayment.model.CurrencyEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static com.fxpayment.util.PaymentConstants.INTERNAL_SCALE;
import static com.fxpayment.utils.TestDataFactory.*;
import static org.junit.jupiter.api.Assertions.*;

class FeeCalculationServiceTest {

    private final FeeCalculationService feeCalculationService = new FeeCalculationService();

    @ParameterizedTest(name = "{0} {1} -> fee {2}")
    @CsvSource({
            // All fees at internal precision (scale 4), regardless of currency display decimals
            "1000.00,  EUR, 0.0000",
            "100.00,   USD, 5.0000",
            "500.00,   USD, 5.0000",
            "501.00,   USD, 5.0100",
            "1000.00,  USD, 10.0000",
            "1.00,     USD, 5.0000",
            "200.00,   GBP, 5.0000",
            "5000.00,  GBP, 50.0000",
            // 0-decimal currency (JPY) — still stored at internal precision
            "100000,   JPY, 1000.0000",
            "10000,    JPY, 500.0000",
            "1000,     JPY, 500.0000",
            // 3-decimal currency (BHD) — stored at internal precision
            "1000.000, BHD, 10.0000",
            "100.000,  BHD, 2.0000",
            "50.000,   BHD, 2.0000",
    })
    void shouldCalculateCorrectFee(String amount, String currencyCode, String expectedFee) {
        CurrencyEntity currency = CURRENCIES.get(currencyCode);

        BigDecimal fee = feeCalculationService.calculateFee(new BigDecimal(amount), currency);

        assertEquals(new BigDecimal(expectedFee), fee);
    }

    @Test
    void feeScaleShouldAlwaysBeInternalPrecision() {
        assertEquals(INTERNAL_SCALE, feeCalculationService.calculateFee(new BigDecimal("100000"), CURRENCIES.get("JPY")).scale());
        assertEquals(INTERNAL_SCALE, feeCalculationService.calculateFee(new BigDecimal("1000.000"), CURRENCIES.get("BHD")).scale());
        assertEquals(INTERNAL_SCALE, feeCalculationService.calculateFee(new BigDecimal("1000.00"), CURRENCIES.get("USD")).scale());
    }

    @ParameterizedTest(name = "{0} fee scale should be {1}")
    @CsvSource({
            "EUR, 4",
            "USD, 4",
            "GBP, 4",
            "JPY, 4",
            "BHD, 4",
    })
    void feeScaleShouldBeInternalPrecisionForAllCurrencies(String currencyCode, int expectedScale) {
        CurrencyEntity currency = CURRENCIES.get(currencyCode);

        BigDecimal amount = currency.getDecimals() == 0
                ? new BigDecimal("100000")
                : new BigDecimal("1000.00");

        assertEquals(expectedScale, feeCalculationService.calculateFee(amount, currency).scale());
    }

    @Test
    void zeroFeeEurShouldReturnZeroAtInternalScale() {
        BigDecimal fee = feeCalculationService.calculateFee(new BigDecimal("999999.99"), CURRENCIES.get("EUR"));

        assertEquals(BigDecimal.ZERO.setScale(INTERNAL_SCALE), fee);
        assertEquals(INTERNAL_SCALE, fee.scale());
    }

    @ParameterizedTest(name = "rounding {0} {1} -> fee {2}")
    @CsvSource({
            // USD: 1% of 0.01 = 0.0001 at scale 4, minimum 5.0000 applies
            "0.01,      USD, 5.0000",
            // BHD: 1% of 0.001 = 0.0000 at scale 4, minimum 2.0000 applies
            "0.001,     BHD, 2.0000",
            // JPY: 1% of 1 = 0.0100 at scale 4, minimum 500.0000 applies
            "1,         JPY, 500.0000",
            // USD: 1% of 999999.99 = 9999.9999 at scale 4 (no longer rounds to 10000)
            "999999.99, USD, 9999.9999",
            // BHD: 1% of 999.999 = 9.99999, rounds to 10.0000 at scale 4
            "999.999,   BHD, 10.0000",
            // USD: 1% of 550.00 = 5.5000, above minimum 5.0000
            "550.00,    USD, 5.5000",
            // GBP: 1% of 499.99 = 4.9999 at scale 4, below minimum 5.0000
            "499.99,    GBP, 5.0000",
            // USD: 1% of 500.50 = 5.0050 at scale 4 (preserves precision, no longer rounds to 5.01)
            "500.50,    USD, 5.0050",
    })
    void shouldHandleRoundingAndMinimumFeeEdgeCases(String amount, String currencyCode, String expectedFee) {
        CurrencyEntity currency = CURRENCIES.get(currencyCode);

        BigDecimal fee = feeCalculationService.calculateFee(new BigDecimal(amount), currency);

        assertEquals(new BigDecimal(expectedFee), fee);
    }

    @Nested
    @DisplayName("Null-guard edge cases")
    class NullGuards {

        private static final BigDecimal ZERO_INTERNAL = BigDecimal.ZERO.setScale(INTERNAL_SCALE);

        @Test
        void nullAmountShouldReturnZeroAtInternalScale() {
            BigDecimal fee = feeCalculationService.calculateFee(null, CURRENCIES.get("USD"));

            assertEquals(ZERO_INTERNAL, fee);
            assertEquals(INTERNAL_SCALE, fee.scale());
        }

        @Test
        void nullCurrencyShouldReturnZeroAtInternalScale() {
            BigDecimal fee = feeCalculationService.calculateFee(new BigDecimal("100.00"), null);

            assertEquals(ZERO_INTERNAL, fee);
            assertEquals(INTERNAL_SCALE, fee.scale());
        }

        @Test
        void nullFeeRateShouldReturnZeroAtInternalScale() {
            CurrencyEntity noRate = aCurrency()
                    .code("TST").name("Test").feeRate(null)
                    .minimumFee(new BigDecimal("1.0000")).decimals((short) 2)
                    .build();

            BigDecimal fee = feeCalculationService.calculateFee(new BigDecimal("100.00"), noRate);

            assertEquals(ZERO_INTERNAL, fee);
            assertEquals(INTERNAL_SCALE, fee.scale());
        }

        @Test
        void negativeAmountShouldThrowIllegalArgument() {
            assertThrows(IllegalArgumentException.class,
                    () -> feeCalculationService.calculateFee(new BigDecimal("-100.00"), CURRENCIES.get("USD")));
        }

        @Test
        void nullMinimumFeeShouldFallBackToZeroMinimum() {
            CurrencyEntity noMinimum = aCurrency()
                    .code("TST").name("Test").feeRate(new BigDecimal("0.0100"))
                    .minimumFee(null).decimals((short) 2)
                    .build();

            BigDecimal fee = feeCalculationService.calculateFee(new BigDecimal("1000.00"), noMinimum);

            // 1% of 1000 = 10.0000, no minimum to compare against
            assertEquals(new BigDecimal("10.0000"), fee);
        }
    }
}
