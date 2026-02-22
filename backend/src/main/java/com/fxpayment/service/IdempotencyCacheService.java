package com.fxpayment.service;

import com.fxpayment.model.Payment;
import com.fxpayment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyCacheService {

    private final PaymentRepository paymentRepository;

    @Cacheable(value = "idempotencyKeys", unless = "#result == null")
    public Optional<Payment> findExistingPayment(String idempotencyKey) {
        log.debug("Idempotency cache miss, querying database: idempotencyKey={}", idempotencyKey);
        return paymentRepository.findByIdempotencyKey(idempotencyKey);
    }

}
