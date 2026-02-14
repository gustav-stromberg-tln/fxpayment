package com.fxpayment.controller;

import org.junit.jupiter.api.Test;

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
                .andExpect(jsonPath("$[*].feePercentage").exists())
                .andExpect(jsonPath("$[*].minimumFee").exists())
                .andExpect(jsonPath("$[*].decimals").exists());
    }

    @Test
    void getCurrenciesShouldReturnCorrectStructure() throws Exception {
        mockMvc.perform(get("/api/v1/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").isString())
                .andExpect(jsonPath("$[0].name").isString())
                .andExpect(jsonPath("$[0].feePercentage").isNumber())
                .andExpect(jsonPath("$[0].minimumFee").isNumber())
                .andExpect(jsonPath("$[0].decimals").isNumber());
    }
}
