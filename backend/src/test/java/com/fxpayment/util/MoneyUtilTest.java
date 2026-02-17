package com.fxpayment.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.fxpayment.util.PaymentConstants.INTERNAL_SCALE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyUtilTest {

    @Test
    void roundToInternalScaleShouldUseScale4HalfUp() {
        assertEquals(new BigDecimal("5.0050"), MoneyUtil.roundToInternalScale(new BigDecimal("5.005")));
        assertEquals(new BigDecimal("5.0100"), MoneyUtil.roundToInternalScale(new BigDecimal("5.01")));
        assertEquals(new BigDecimal("9.9999"), MoneyUtil.roundToInternalScale(new BigDecimal("9.99994")));
        assertEquals(new BigDecimal("10.0000"), MoneyUtil.roundToInternalScale(new BigDecimal("9.99995")));
    }

    @Test
    void roundToScaleShouldRoundToCurrencyDecimals() {
        assertEquals(new BigDecimal("5.01"), MoneyUtil.roundToScale(new BigDecimal("5.0050"), 2));
        assertEquals(new BigDecimal("100"), MoneyUtil.roundToScale(new BigDecimal("100.4999"), 0));
        assertEquals(new BigDecimal("10.000"), MoneyUtil.roundToScale(new BigDecimal("9.99999"), 3));
    }

    @Test
    void zeroWithInternalScaleShouldReturnZeroAtScale4() {
        BigDecimal zero = MoneyUtil.zeroWithInternalScale();

        assertEquals(BigDecimal.ZERO.setScale(INTERNAL_SCALE), zero);
        assertEquals(INTERNAL_SCALE, zero.scale());
    }
}
