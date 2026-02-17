package com.fxpayment.model;

import com.fxpayment.util.PaymentConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "currencies")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrencyEntity {

    @Id
    @Column(length = PaymentConstants.CURRENCY_CODE_LENGTH)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "fee_rate", nullable = false, precision = PaymentConstants.FEE_RATE_PRECISION, scale = PaymentConstants.FEE_RATE_SCALE)
    private BigDecimal feeRate;

    @Column(name = "minimum_fee", nullable = false, precision = PaymentConstants.MONEY_PRECISION, scale = PaymentConstants.INTERNAL_SCALE)
    private BigDecimal minimumFee;

    @Min(0)
    @Max(PaymentConstants.MAX_CURRENCY_DECIMALS)
    @Column(nullable = false)
    private short decimals;
}
