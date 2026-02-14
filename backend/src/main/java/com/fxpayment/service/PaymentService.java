package com.fxpayment.service;

import com.fxpayment.dto.PaymentRequest;
import com.fxpayment.dto.PaymentResponse;
import com.fxpayment.model.Curr;
import com.fxpayment.model.Payment;
import com.fxpayment.model.PaymentStatus;
import com.fxpayment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final FeeCalculationService feeCalculationService;
    private final CurrencyService currencyService;

    @Transactional
    public PaymentResponse createPayment(PaymentRequest request) {
        Curr currency = currencyService.findByCode(request.currency())
                .orElseThrow(() -> {
                    log.error("Unsupported currency code: {}", request.currency());
                    return new IllegalArgumentException("Unsupported currency code: " + request.currency());
                });

        if (request.amount().scale() > currency.getDecimals()) {
            throw new IllegalArgumentException(
                    "Amount has too many decimal places for currency " + currency.getCode()
                            + ": maximum " + currency.getDecimals() + " allowed");
        }

        BigDecimal fee = feeCalculationService.calculateFee(request.amount(), request.currency());
        log.debug("Calculated fee={} for amount={} currency={}", fee, request.amount(), request.currency());

        // TODO: No payment processing logic yet - mark every payment as COMPLETED immediately
        Payment payment = Payment.builder()
                .amount(request.amount())
                .currency(request.currency())
                .recipient(request.recipient())
                .recipientAccount(request.recipientAccount())
                .processingFee(fee)
                .status(PaymentStatus.COMPLETED)
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Payment persisted: id={}, amount={}, currency={}",
                saved.getId(), saved.getAmount(), saved.getCurrency());

        return PaymentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(int page, int size) {
        log.debug("Querying payments: page={}, size={}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return paymentRepository.findAll(pageable)
                .map(PaymentResponse::from);
    }
}
