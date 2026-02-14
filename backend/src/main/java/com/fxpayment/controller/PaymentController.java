package com.fxpayment.controller;

import com.fxpayment.dto.PaymentRequest;
import com.fxpayment.dto.PaymentResponse;
import com.fxpayment.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request) {
        log.info("Received payment request: currency={}, recipient={}",
                request.currency(), request.recipient());
        PaymentResponse response = paymentService.createPayment(request);
        log.info("Payment created: id={}, status={}", response.id(), response.status());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<PaymentResponse>> getAllPayments(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        log.debug("Fetching payments: page={}, size={}", page, size);
        Page<PaymentResponse> payments = paymentService.getAllPayments(page, size);
        log.debug("Returning {} payments (page {} of {})", payments.getNumberOfElements(), page, payments.getTotalPages());
        return ResponseEntity.ok(payments);
    }
}
