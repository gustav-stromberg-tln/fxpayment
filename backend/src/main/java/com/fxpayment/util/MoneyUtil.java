package com.fxpayment.util;

import java.math.BigDecimal;

import static com.fxpayment.util.PaymentConstants.INTERNAL_SCALE;
import static com.fxpayment.util.PaymentConstants.ROUNDING_MODE;

public final class MoneyUtil {

    private MoneyUtil() {}

    public static BigDecimal roundToInternalScale(BigDecimal value) {
        return value.setScale(INTERNAL_SCALE, ROUNDING_MODE);
    }

    public static BigDecimal roundToScale(BigDecimal value, int scale) {
        return value.setScale(scale, ROUNDING_MODE);
    }

    public static BigDecimal zeroWithInternalScale() {
        return BigDecimal.ZERO.setScale(INTERNAL_SCALE, ROUNDING_MODE);
    }
}
