package com.fxpayment.repository;

import com.fxpayment.annotation.RepositoryTest;
import com.fxpayment.model.Payment;
import com.fxpayment.utils.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@RepositoryTest
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void saveShouldPersistPaymentWithAllFields() {
        Payment payment = TestDataFactory.defaultPaymentBuilder()
                .createdBy("test-admin")
                .build();

        Payment saved = paymentRepository.save(payment);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals(payment.getAmount(), saved.getAmount());
        assertEquals(payment.getCurrency(), saved.getCurrency());
        assertEquals(payment.getRecipient(), saved.getRecipient());
        assertEquals(payment.getRecipientAccount(), saved.getRecipientAccount());
        assertEquals(payment.getProcessingFee(), saved.getProcessingFee());
        assertEquals(payment.getStatus(), saved.getStatus());
        assertEquals(payment.getCreatedBy(), saved.getCreatedBy());
    }

    @Test
    void findAllWithPageableShouldReturnPaginatedResults() {
        for (int i = 0; i < 5; i++) {
            paymentRepository.save(TestDataFactory.defaultPaymentBuilder()
                    .recipient("Recipient " + i)
                    .build());
        }

        Page<Payment> page = paymentRepository.findAll(
                PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertEquals(3, page.getContent().size());
        assertEquals(5, page.getTotalElements());
        assertEquals(2, page.getTotalPages());
    }

    @Test
    void softDeletedPaymentsShouldNotAppearInQueries() {
        Payment payment = TestDataFactory.defaultPaymentBuilder()
                .recipient("Deleted User")
                .build();

        UUID paymentId = paymentRepository.save(payment).getId();
        paymentRepository.deleteById(paymentId);

        Optional<Payment> found = paymentRepository.findById(paymentId);
        assertTrue(found.isEmpty());

        Page<Payment> page = paymentRepository.findAll(PageRequest.of(0, 100));
        assertTrue(page.getContent().stream()
                .noneMatch(p -> paymentId.equals(p.getId())));
    }
}
