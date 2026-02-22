package com.fxpayment.service;

import com.fxpayment.dto.CreatePaymentResult;
import com.fxpayment.dto.PaymentRequest;
import com.fxpayment.dto.PaymentResponse;
import com.fxpayment.exception.PaymentProcessingException;
import com.fxpayment.model.CurrencyEntity;
import com.fxpayment.model.Payment;
import com.fxpayment.model.PaymentStatus;
import com.fxpayment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final FeeCalculationService feeCalculationService;
    private final CurrencyService currencyService;
    private final PaymentValidationService paymentValidationService;
    private final IdempotencyCacheService idempotencyCacheService;
    private final TransactionTemplate transactionTemplate;

    public CreatePaymentResult createPayment(String idempotencyKey, PaymentRequest request) {
        try {
            return requireNonNullResult(
                    transactionTemplate.execute(_ -> doCreatePayment(idempotencyKey, request)));
        } catch (DataIntegrityViolationException ex) {
            // Concurrent insert with the same idempotency key is resolved by the
            // DB unique constraint. Retrying starts a new transaction whose initial
            // findByIdempotencyKey lookup will find the winner's record and return
            // a replay response.
            try {
                return requireNonNullResult(
                        transactionTemplate.execute(status -> doCreatePayment(idempotencyKey, request)));
            } catch (DataIntegrityViolationException retryEx) {
                log.error("Idempotency retry also failed: idempotencyKey={}", idempotencyKey, retryEx);
                throw new PaymentProcessingException("Payment could not be processed due to a conflict", retryEx);
            }
        }
    }

    private CreatePaymentResult requireNonNullResult(CreatePaymentResult result) {
        if (result == null) {
            throw new PaymentProcessingException("Transaction produced no result", null);
        }
        return result;
    }

    // No per-user scoping - scope to authenticated user when auth is added.
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return paymentRepository.findAll(pageable)
                .map(payment -> PaymentResponse.from(payment, currencyService.getDecimals(payment.getCurrency())));
    }

    private CreatePaymentResult doCreatePayment(String idempotencyKey, PaymentRequest request) {
        Optional<Payment> existing = idempotencyCacheService.findExistingPayment(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotency replay: idempotencyKey={}, paymentId={}", idempotencyKey, existing.get().getId());
            return replayResponse(existing.get());
        }

        PaymentRequest normalised = request.normalised();
        CurrencyEntity currency = paymentValidationService.resolveAndValidateCurrency(normalised);
        BigDecimal fee = feeCalculationService.calculateFee(normalised.amount(), currency);
        Payment saved = persistPayment(idempotencyKey, normalised, fee);

        return new CreatePaymentResult(PaymentResponse.from(saved, currency.getDecimals()), true);
    }

    private Payment persistPayment(String idempotencyKey, PaymentRequest request, BigDecimal fee) {
        Payment payment = Payment.builder()
                .amount(request.amount())
                .currency(request.currency())
                .recipient(request.recipient())
                .recipientAccount(request.recipientAccount())
                .processingFee(fee)
                .status(PaymentStatus.COMPLETED)
                .idempotencyKey(idempotencyKey)
                .build();

        try {
            Payment saved = paymentRepository.saveAndFlush(payment);
            log.info("Payment persisted: id={}, amount={}, currency={}, idempotencyKey={}",
                    saved.getId(), saved.getAmount(), saved.getCurrency(), idempotencyKey);
            return saved;
        } catch (DataIntegrityViolationException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            log.error("Failed to persist payment: idempotencyKey={}", idempotencyKey, ex);
            throw new PaymentProcessingException("Payment could not be processed", ex);
        }
    }

    private CreatePaymentResult replayResponse(Payment payment) {
        int decimals = currencyService.getDecimals(payment.getCurrency());
        return new CreatePaymentResult(PaymentResponse.from(payment, decimals), false);
    }
}
