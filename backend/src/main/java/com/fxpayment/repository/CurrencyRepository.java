package com.fxpayment.repository;

import com.fxpayment.model.Curr;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrencyRepository extends JpaRepository<Curr, String> {
}
