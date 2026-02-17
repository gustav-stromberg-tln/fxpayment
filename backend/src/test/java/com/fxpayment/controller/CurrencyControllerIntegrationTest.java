package com.fxpayment.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.fxpayment.utils.TestDataFactory.bhdCurrency;
import static com.fxpayment.utils.TestDataFactory.jpyCurrency;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CurrencyControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void getCurrenciesShouldReturnAllCurrencies() throws Exception {
        mockMvc.perform(get("/api/v1/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$[*].code", hasItems("USD", "EUR", "GBP")))
                .andExpect(jsonPath("$[*].name").exists())
                .andExpect(jsonPath("$[*].decimals").exists())
                .andExpect(jsonPath("$[0].feeRate").doesNotExist())
                .andExpect(jsonPath("$[0].minimumFee").doesNotExist());
    }

    @Test
    void getCurrenciesShouldReturnCorrectStructure() throws Exception {
        mockMvc.perform(get("/api/v1/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").isString())
                .andExpect(jsonPath("$[0].name").isString())
                .andExpect(jsonPath("$[0].decimals").isNumber());
    }

    @Test
    @DisplayName("should return correct field values for each seeded currency")
    void getCurrenciesShouldReturnCorrectFieldValues() throws Exception {
        mockMvc.perform(get("/api/v1/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.code == 'USD')].name", hasItem("US Dollar")))
                .andExpect(jsonPath("$[?(@.code == 'USD')].decimals", hasItem(2)))
                .andExpect(jsonPath("$[?(@.code == 'EUR')].name", hasItem("Euro")))
                .andExpect(jsonPath("$[?(@.code == 'EUR')].decimals", hasItem(2)))
                .andExpect(jsonPath("$[?(@.code == 'GBP')].name", hasItem("British Pound")))
                .andExpect(jsonPath("$[?(@.code == 'GBP')].decimals", hasItem(2)));
    }

    @Test
    @DisplayName("should include zero-decimal and three-decimal currencies when seeded")
    void getCurrenciesShouldReturnDiverseDecimalConfigs() throws Exception {
        currencyRepository.save(jpyCurrency());
        currencyRepository.save(bhdCurrency());

        mockMvc.perform(get("/api/v1/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(5))))
                .andExpect(jsonPath("$[?(@.code == 'JPY')].name", hasItem("Japanese Yen")))
                .andExpect(jsonPath("$[?(@.code == 'JPY')].decimals", hasItem(0)))
                .andExpect(jsonPath("$[?(@.code == 'BHD')].name", hasItem("Bahraini Dinar")))
                .andExpect(jsonPath("$[?(@.code == 'BHD')].decimals", hasItem(3)));
    }
}
