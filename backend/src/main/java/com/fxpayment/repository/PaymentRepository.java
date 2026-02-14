package com.fxpayment.repository;

import com.fxpayment.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

// Soft-delete filtering is handled by @SoftDelete on the Payment entity.

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
}
