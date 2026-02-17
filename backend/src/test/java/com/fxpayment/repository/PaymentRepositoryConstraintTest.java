package com.fxpayment.repository;

import com.fxpayment.annotation.RepositoryTest;
import com.fxpayment.model.Payment;
import com.fxpayment.model.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static com.fxpayment.utils.TestDataFactory.*;
import static org.junit.jupiter.api.Assertions.*;

@RepositoryTest
@DisplayName("Payment repository constraint tests (2a)")
class PaymentRepositoryConstraintTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Nested
    @DisplayName("Null column constraint violations")
    class NullConstraints {

        @Test
        @DisplayName("null amount should fail to persist")
        void nullAmountShouldFail() {
            Payment payment = aPayment().amount(null).build();

            assertThrows(Exception.class,
                    () -> paymentRepository.saveAndFlush(payment));
        }

        @Test
        @DisplayName("null currency should fail to persist")
        void nullCurrencyShouldFail() {
            Payment payment = aPayment().currency(null).build();

            assertThrows(Exception.class,
                    () -> paymentRepository.saveAndFlush(payment));
        }

        @Test
        @DisplayName("null recipient should fail to persist")
        void nullRecipientShouldFail() {
            Payment payment = aPayment().recipient(null).build();

            assertThrows(Exception.class,
                    () -> paymentRepository.saveAndFlush(payment));
        }

        @Test
        @DisplayName("null recipientAccount should fail to persist")
        void nullRecipientAccountShouldFail() {
            Payment payment = aPayment().recipientAccount(null).build();

            assertThrows(Exception.class,
                    () -> paymentRepository.saveAndFlush(payment));
        }

        @Test
        @DisplayName("null processingFee should fail to persist")
        void nullProcessingFeeShouldFail() {
            Payment payment = aPayment().processingFee(null).build();

            assertThrows(Exception.class,
                    () -> paymentRepository.saveAndFlush(payment));
        }

        @Test
        @DisplayName("null status should fail to persist")
        void nullStatusShouldFail() {
            Payment payment = aPayment().status(null).build();

            assertThrows(Exception.class,
                    () -> paymentRepository.saveAndFlush(payment));
        }

        @Test
        @DisplayName("null idempotencyKey should fail to persist")
        void nullIdempotencyKeyShouldFail() {
            Payment payment = aPayment().idempotencyKey(null).build();

            assertThrows(Exception.class,
                    () -> paymentRepository.saveAndFlush(payment));
        }
    }

    @Nested
    @DisplayName("VARCHAR length overflow")
    class VarcharOverflow {

        @Test
        @DisplayName("currency longer than 3 characters should fail")
        void currencyOverflowShouldFail() {
            Payment payment = aPayment().currency("ABCD").build();

            assertThrows(Exception.class,
                    () -> paymentRepository.saveAndFlush(payment));
        }
    }
}
