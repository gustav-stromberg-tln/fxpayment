package com.fxpayment.dto;

import com.fxpayment.model.CurrencyEntity;
import org.junit.jupiter.api.Test;

import static com.fxpayment.utils.TestDataFactory.*;
import static org.junit.jupiter.api.Assertions.*;

class CurrencyResponseTest {

    @Test
    void fromShouldMapOnlyRequiredFields() {
        CurrencyEntity currency = usdCurrency();

        CurrencyResponse response = CurrencyResponse.from(currency);

        assertEquals("USD", response.code());
        assertEquals("US Dollar", response.name());
        assertEquals(2, response.decimals());
    }

    @Test
    void fromShouldHandleZeroDecimalCurrency() {
        CurrencyEntity currency = jpyCurrency();

        CurrencyResponse response = CurrencyResponse.from(currency);

        assertEquals("JPY", response.code());
        assertEquals(0, response.decimals());
    }

    @Test
    void fromShouldHandleThreeDecimalCurrency() {
        CurrencyEntity currency = bhdCurrency();

        CurrencyResponse response = CurrencyResponse.from(currency);

        assertEquals("BHD", response.code());
        assertEquals("Bahraini Dinar", response.name());
        assertEquals(3, response.decimals());
    }

    @Test
    void fromShouldThrowOnNullEntity() {
        assertThrows(NullPointerException.class, () -> CurrencyResponse.from(null));
    }

    @Test
    void fromShouldNotExposeInternalFeeFields() {
        CurrencyEntity currency = usdCurrency();
        CurrencyResponse response = CurrencyResponse.from(currency);

        assertEquals(3, CurrencyResponse.class.getRecordComponents().length,
                "CurrencyResponse should only have code, name, and decimals");
    }

    @Test
    void responsesWithSameDataShouldBeEqual() {
        CurrencyResponse r1 = CurrencyResponse.from(usdCurrency());
        CurrencyResponse r2 = CurrencyResponse.from(usdCurrency());

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void responsesWithDifferentDataShouldNotBeEqual() {
        CurrencyResponse r1 = CurrencyResponse.from(usdCurrency());
        CurrencyResponse r2 = CurrencyResponse.from(jpyCurrency());

        assertNotEquals(r1, r2);
    }
}
