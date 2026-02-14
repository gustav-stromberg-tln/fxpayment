package com.fxpayment.repository;

import com.fxpayment.annotation.RepositoryTest;
import com.fxpayment.model.Curr;
import com.fxpayment.utils.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@RepositoryTest
class CurrencyRepositoryTest {

    @Autowired
    private CurrencyRepository currencyRepository;

    @BeforeEach
    void setUp() {
        TestDataFactory.seedCurrencies(currencyRepository);
    }

    @Test
    void findByIdShouldReturnCurrencyWhenExists() {
        Curr usd = currencyRepository.findById("USD").orElseThrow();

        assertNotNull(usd.getName());
        assertNotNull(usd.getFeePercentage());
        assertNotNull(usd.getMinimumFee());
    }

    @Test
    void findByIdShouldReturnEmptyForNonexistentCurrency() {
        Optional<Curr> result = currencyRepository.findById("ZZZ");

        assertTrue(result.isEmpty());
    }

    @Test
    void findAllShouldReturnAllCurrencies() {
        List<Curr> currencies = currencyRepository.findAll();

        assertEquals(3, currencies.size());
    }

    @Test
    void saveShouldPersistNewCurrency() {
        Curr currency = Curr.builder()
                .code("CHF")
                .name("Swiss Franc")
                .feePercentage(new BigDecimal("0.0150"))
                .minimumFee(new BigDecimal("3.0000"))
                .decimals((short) 2)
                .build();

        Curr saved = currencyRepository.save(currency);

        assertEquals(currency.getCode(), saved.getCode());
        assertEquals(currency.getName(), saved.getName());
        assertEquals(0, currency.getFeePercentage().compareTo(saved.getFeePercentage()));
        assertEquals(0, currency.getMinimumFee().compareTo(saved.getMinimumFee()));
        assertEquals(currency.getDecimals(), saved.getDecimals());
    }

    @Test
    void saveShouldPersistCurrencyWithExplicitDecimals() {
        Curr currency = TestDataFactory.jpyCurrency();

        currencyRepository.saveAndFlush(currency);
        Curr found = currencyRepository.findById(currency.getCode()).orElseThrow();

        assertEquals(currency.getDecimals(), found.getDecimals());
    }

    @Test
    void savingWithSameCodeShouldUpdateExistingCurrency() {
        Curr original = TestDataFactory.jpyCurrency();
        currencyRepository.saveAndFlush(original);

        String updatedName = "Updated Yen";
        currencyRepository.saveAndFlush(Curr.builder()
                .code(original.getCode()).name(updatedName)
                .feePercentage(new BigDecimal("0.0200"))
                .minimumFee(new BigDecimal("10.0000"))
                .decimals(original.getDecimals()).build());

        Curr found = currencyRepository.findById(original.getCode()).orElseThrow();
        assertEquals(updatedName, found.getName());
    }
}
