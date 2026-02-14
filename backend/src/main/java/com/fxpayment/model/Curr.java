package com.fxpayment.model;

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
// Named Curr to avoid clash with java.util.Currency
public class Curr {

    @Id
    @Column(length = 3)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "fee_percentage", nullable = false, precision = 9, scale = 6)
    private BigDecimal feePercentage;

    @Column(name = "minimum_fee", nullable = false, precision = 19, scale = 4)
    private BigDecimal minimumFee;

    @Min(0)
    @Max(18)
    @Column(nullable = false)
    private short decimals;
}
