package com.fxpayment.model;

import com.fxpayment.util.PaymentConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.SoftDelete;
import org.hibernate.annotations.SoftDeleteType;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@SoftDelete(strategy = SoftDeleteType.DELETED)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, precision = PaymentConstants.MONEY_PRECISION, scale = PaymentConstants.INTERNAL_SCALE)
    private BigDecimal amount;

    @Column(nullable = false, length = PaymentConstants.CURRENCY_CODE_LENGTH)
    private String currency;

    @Column(nullable = false, length = PaymentConstants.MAX_RECIPIENT_LENGTH)
    private String recipient;

    @Column(name = "recipient_account", nullable = false)
    private String recipientAccount;

    @Column(name = "processing_fee", nullable = false, precision = PaymentConstants.MONEY_PRECISION, scale = PaymentConstants.INTERNAL_SCALE)
    private BigDecimal processingFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = PaymentConstants.STATUS_MAX_LENGTH)
    private PaymentStatus status;

    @Column(name = "idempotency_key", nullable = false, length = PaymentConstants.UUID_STRING_LENGTH, unique = true)
    private String idempotencyKey;

    @CurrentTimestamp(event = EventType.INSERT)
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @CurrentTimestamp(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "updated_at")
    private Instant updatedAt;
}
