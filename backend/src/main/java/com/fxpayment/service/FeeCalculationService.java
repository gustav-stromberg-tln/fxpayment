package com.fxpayment.service;

import com.fxpayment.model.Curr;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeCalculationService {

    private final CurrencyService currencyService;

    public BigDecimal calculateFee(BigDecimal amount, String currencyCode) {
        Curr currency = currencyService.findByCode(currencyCode)
                .orElseThrow(() -> {
                    log.error("Unsupported currency code requested: {}", currencyCode);
                    return new IllegalArgumentException("Unsupported currency code");
                });

        int scale = currency.getDecimals();

        if (currency.getFeePercentage().compareTo(BigDecimal.ZERO) == 0) {
            log.debug("Zero-fee currency: {}", currencyCode);
            return BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        }

        BigDecimal percentageFee = amount.multiply(currency.getFeePercentage())
                .setScale(scale, RoundingMode.HALF_UP);
        BigDecimal minimumFee = currency.getMinimumFee().setScale(scale, RoundingMode.HALF_UP);
        BigDecimal appliedFee = percentageFee.compareTo(minimumFee) < 0
                ? minimumFee
                : percentageFee;

        log.debug("Fee calculated for {} {}: percentageFee={}, minimumFee={}, appliedFee={}",
                amount, currencyCode, percentageFee, minimumFee, appliedFee);
        return appliedFee;
    }
}
