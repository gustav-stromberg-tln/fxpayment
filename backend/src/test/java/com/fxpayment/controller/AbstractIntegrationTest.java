package com.fxpayment.controller;

import com.fxpayment.repository.CurrencyRepository;
import com.fxpayment.utils.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected CurrencyRepository currencyRepository;

    @BeforeEach
    void setUpCurrencies() {
        TestDataFactory.seedCurrencies(currencyRepository);
    }

    @AfterEach
    void tearDownCurrencies() {
        currencyRepository.deleteAll();
    }
}
