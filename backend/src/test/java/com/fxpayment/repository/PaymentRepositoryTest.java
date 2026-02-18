package com.fxpayment.repository;

import com.fxpayment.annotation.RepositoryTest;
import com.fxpayment.model.Payment;
import com.fxpayment.model.PaymentStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static com.fxpayment.utils.TestDataFactory.*;
import static org.junit.jupiter.api.Assertions.*;

@RepositoryTest
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void saveShouldPersistPaymentWithAllFields() {
        Payment payment = aPayment().build();

        Payment saved = paymentRepository.saveAndFlush(payment);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals(new BigDecimal("100.0000"), saved.getAmount());
        assertEquals("USD", saved.getCurrency());
        assertEquals("John Doe", saved.getRecipient());
        assertEquals(ESTONIAN_IBAN, saved.getRecipientAccount());
        assertEquals(new BigDecimal("5.0000"), saved.getProcessingFee());
        assertEquals(PaymentStatus.COMPLETED, saved.getStatus());
    }

    @Test
    @DisplayName("H2/Hibernate generates v4 UUIDs (production Postgres uses time-ordered v7)")
    void savedPaymentIdShouldBeValidV4Uuid() {
        // In production, PostgreSQL's DEFAULT uuidv7() generates time-ordered v7 UUIDs
        // with better index locality for idx_payments_active_by_time.
        // Hibernate GenerationType.UUID generates random v4 UUIDs in H2.
        // Consider Testcontainers with PostgreSQL 18 to test v7 ordering.
        Payment payment = aPayment().build();

        Payment saved = paymentRepository.save(payment);

        assertNotNull(saved.getId());
        assertEquals(4, saved.getId().version(),
                "H2/Hibernate generates v4 UUIDs; production uses v7 via uuidv7()");
    }

    @Test
    void findAllWithPageableShouldReturnPaginatedResults() {
        for (int i = 0; i < 5; i++) {
            paymentRepository.save(aPayment().recipient("Recipient " + i).build());
        }

        Page<Payment> page = paymentRepository.findAll(
                PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertEquals(3, page.getContent().size());
        assertEquals(5, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
    }

    @Test
    void findByIdempotencyKeyShouldReturnPaymentWithAllFields() {
        String idempotencyKey = UUID.randomUUID().toString();
        Payment payment = aPayment()
                .idempotencyKey(idempotencyKey)
                .amount(new BigDecimal("750.0000"))
                .currency("GBP")
                .recipient("Virtanen Kallio")
                .recipientAccount(FINNISH_IBAN)
                .processingFee(new BigDecimal("7.5000"))
                .build();
        paymentRepository.save(payment);

        Optional<Payment> found = paymentRepository.findByIdempotencyKey(idempotencyKey);

        assertTrue(found.isPresent());
        Payment p = found.get();
        assertEquals(idempotencyKey, p.getIdempotencyKey());
        assertEquals(0, new BigDecimal("750.0000").compareTo(p.getAmount()));
        assertEquals("GBP", p.getCurrency());
        assertEquals("Virtanen Kallio", p.getRecipient());
        assertEquals(FINNISH_IBAN, p.getRecipientAccount());
        assertEquals(0, new BigDecimal("7.5000").compareTo(p.getProcessingFee()));
        assertEquals(PaymentStatus.COMPLETED, p.getStatus());
    }

    @Test
    void findByIdempotencyKeyShouldReturnEmptyForNonexistentKey() {
        Optional<Payment> found = paymentRepository.findByIdempotencyKey("nonexistent-key");

        assertTrue(found.isEmpty());
    }

    @Test
    void onUpdateShouldSetUpdatedAtTimestamp() {
        // @CurrentTimestamp makes Hibernate set createdAt on INSERT and updatedAt on INSERT+UPDATE.
        Payment payment = aPayment().build();

        Payment saved = paymentRepository.saveAndFlush(payment);
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertTrue(java.time.Duration.between(saved.getCreatedAt(), saved.getUpdatedAt()).abs().toMillis() < 1,
                "createdAt and updatedAt should be set within 1ms of each other on insert");

        Payment modified = Payment.builder()
                .id(saved.getId())
                .amount(saved.getAmount())
                .currency(saved.getCurrency())
                .recipient("Updated Recipient")
                .recipientAccount(saved.getRecipientAccount())
                .processingFee(saved.getProcessingFee())
                .status(PaymentStatus.PROCESSING)
                .idempotencyKey(saved.getIdempotencyKey())
                .createdAt(saved.getCreatedAt())
                .build();
        entityManager.merge(modified);
        entityManager.flush();
        entityManager.clear();

        Payment updated = paymentRepository.findById(saved.getId()).orElseThrow();
        assertNotNull(updated.getUpdatedAt());
        assertFalse(updated.getUpdatedAt().isBefore(saved.getCreatedAt()),
                "updatedAt must not be before createdAt");
        assertEquals("Updated Recipient", updated.getRecipient());
        assertEquals(PaymentStatus.PROCESSING, updated.getStatus());
    }

    @Test
    void duplicateIdempotencyKeyShouldThrowDataIntegrityViolation() {
        String idempotencyKey = UUID.randomUUID().toString();
        paymentRepository.saveAndFlush(aPayment().idempotencyKey(idempotencyKey).build());

        Payment duplicate = aPayment().idempotencyKey(idempotencyKey).build();

        assertThrows(DataIntegrityViolationException.class,
                () -> paymentRepository.saveAndFlush(duplicate));
    }

    @Test
    void softDeletedPaymentsShouldNotAppearInQueries() {
        Payment payment = aPayment().recipient("Deleted User").build();
        UUID paymentId = paymentRepository.save(payment).getId();

        paymentRepository.deleteById(paymentId);

        Optional<Payment> found = paymentRepository.findById(paymentId);
        assertTrue(found.isEmpty());

        Page<Payment> page = paymentRepository.findAll(PageRequest.of(0, 100));
        assertTrue(page.getContent().stream()
                .noneMatch(p -> paymentId.equals(p.getId())));
    }

    @Test
    void softDeletedPaymentShouldNotBeFoundByIdempotencyKey() {
        String idempotencyKey = UUID.randomUUID().toString();
        Payment payment = aPayment().idempotencyKey(idempotencyKey).build();
        UUID paymentId = paymentRepository.save(payment).getId();

        paymentRepository.deleteById(paymentId);

        Optional<Payment> found = paymentRepository.findByIdempotencyKey(idempotencyKey);
        assertTrue(found.isEmpty());
    }

    @Nested
    @DisplayName("Data variety: diverse payment profiles")
    class DataVariety {

        @Test
        @DisplayName("high-value EUR payment with German IBAN")
        void shouldPersistHighValueEurPayment() {
            Payment payment = aPayment()
                    .amount(new BigDecimal("999999.0000"))
                    .currency("EUR")
                    .recipient(RECIPIENT_WITH_DIACRITICS)
                    .recipientAccount(GERMAN_IBAN)
                    .processingFee(new BigDecimal("0.0000"))
                    .build();

            Payment saved = paymentRepository.saveAndFlush(payment);

            assertNotNull(saved.getId());
            assertEquals(0, new BigDecimal("999999.0000").compareTo(saved.getAmount()));
            assertEquals("EUR", saved.getCurrency());
            assertEquals(RECIPIENT_WITH_DIACRITICS, saved.getRecipient());
            assertEquals(GERMAN_IBAN, saved.getRecipientAccount());
            assertEquals(0, BigDecimal.ZERO.compareTo(saved.getProcessingFee()));
        }

        @Test
        @DisplayName("minimum-amount GBP payment with Finnish IBAN")
        void shouldPersistMinimumAmountGbpPayment() {
            Payment payment = aPayment()
                    .amount(new BigDecimal("0.0100"))
                    .currency("GBP")
                    .recipient("Li")
                    .recipientAccount(FINNISH_IBAN)
                    .processingFee(new BigDecimal("5.0000"))
                    .build();

            Payment saved = paymentRepository.saveAndFlush(payment);

            assertNotNull(saved.getId());
            assertEquals(0, new BigDecimal("0.0100").compareTo(saved.getAmount()));
            assertEquals("GBP", saved.getCurrency());
            assertEquals("Li", saved.getRecipient());
            assertEquals(FINNISH_IBAN, saved.getRecipientAccount());
        }

        @Test
        @DisplayName("payment with Swedish IBAN and varied fee")
        void shouldPersistPaymentWithSwedishIban() {
            Payment payment = aPayment()
                    .amount(new BigDecimal("550.0000"))
                    .currency("USD")
                    .recipient("Eriksson Ljungberg")
                    .recipientAccount(SWEDISH_IBAN)
                    .processingFee(new BigDecimal("5.5000"))
                    .build();

            Payment saved = paymentRepository.saveAndFlush(payment);
            entityManager.clear();

            Payment found = paymentRepository.findById(saved.getId()).orElseThrow();
            assertEquals(0, new BigDecimal("550.0000").compareTo(found.getAmount()));
            assertEquals("Eriksson Ljungberg", found.getRecipient());
            assertEquals(SWEDISH_IBAN, found.getRecipientAccount());
            assertEquals(0, new BigDecimal("5.5000").compareTo(found.getProcessingFee()));
        }

        @Test
        @DisplayName("multiple payments with different currencies in pagination")
        void shouldPaginateMixedCurrencyPayments() {
            paymentRepository.save(aPayment().currency("USD").recipient("Alice").build());
            paymentRepository.save(aPayment().currency("EUR").recipient("Bob").build());
            paymentRepository.save(aPayment().currency("GBP").recipient("Charlie").build());
            paymentRepository.save(aPayment().currency("USD").recipient("Diana").build());

            Page<Payment> firstPage = paymentRepository.findAll(PageRequest.of(0, 2));
            Page<Payment> secondPage = paymentRepository.findAll(PageRequest.of(1, 2));

            assertEquals(2, firstPage.getContent().size());
            assertEquals(2, secondPage.getContent().size());
            assertEquals(4, firstPage.getTotalElements());
            assertEquals(2, firstPage.getTotalPages());
        }
    }
}
