package com.fxpayment.service;

import com.fxpayment.model.CurrencyEntity;
import com.fxpayment.util.MoneyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class FeeCalculationService {

    public BigDecimal calculateFee(BigDecimal amount, CurrencyEntity currency) {
        if (amount == null || currency == null || currency.getFeeRate() == null) {
            return MoneyUtil.zeroWithInternalScale();
        }

        if (amount.signum() < 0) {
            log.error("Negative amount rejected: currency={}, amount={}", currency.getCode(), amount);
            throw new IllegalArgumentException("Fee calculation requires a positive amount, got: " + amount);
        }

        if (currency.getFeeRate().signum() == 0) {
            return MoneyUtil.zeroWithInternalScale();
        }

        BigDecimal percentageFee = amount.multiply(currency.getFeeRate());
        BigDecimal minimumFee = currency.getMinimumFee() != null ?
                currency.getMinimumFee() : BigDecimal.ZERO;

        BigDecimal result = percentageFee.max(minimumFee);
        BigDecimal rounded = MoneyUtil.roundToInternalScale(result);
        log.debug("Fee calculated: currency={}, amount={}, fee={}", currency.getCode(), amount, rounded);
        return rounded;
    }
}
