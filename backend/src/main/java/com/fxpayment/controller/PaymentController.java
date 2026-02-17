package com.fxpayment.controller;

import com.fxpayment.dto.CreatePaymentResult;
import com.fxpayment.dto.PaymentRequest;
import com.fxpayment.dto.PaymentResponse;
import com.fxpayment.service.PaymentService;
import com.fxpayment.util.PaymentConstants;
import com.fxpayment.validation.ValidUuid;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(path = "/api/v1/payments", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Validated
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader("Idempotency-Key")
            @ValidUuid(message = "Idempotency-Key must be a valid UUID")
            String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {
        log.info("Received payment request: currency={}, idempotencyKey={}",
                request.currency(), idempotencyKey);

        CreatePaymentResult result = paymentService.createPayment(idempotencyKey, request);

        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        log.info("Payment {}: id={}",
                result.created() ? "created" : "replayed", result.response().id());
        return ResponseEntity.status(status).body(result.response());
    }

    @GetMapping
    public ResponseEntity<Page<PaymentResponse>> getAllPayments(
            @RequestParam(defaultValue = PaymentConstants.DEFAULT_PAGE) @Min(0) int page,
            @RequestParam(defaultValue = PaymentConstants.DEFAULT_PAGE_SIZE) @Min(1) @Max(PaymentConstants.MAX_PAGE_SIZE) int size) {
        log.debug("Fetching payments: page={}, size={}", page, size);
        Page<PaymentResponse> payments = paymentService.getAllPayments(page, size);
        log.debug("Returning {} payments (page {} of {})", payments.getNumberOfElements(), page, payments.getTotalPages());
        return ResponseEntity.ok(payments);
    }
}
