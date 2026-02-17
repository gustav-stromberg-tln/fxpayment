package com.fxpayment.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class IbanValidatorTest {

    private final IbanValidator validator = new IbanValidator();

    @ParameterizedTest(name = "valid IBAN: {0}")
    @ValueSource(strings = {
            "EE382200221020145685",   // Estonian
            "SE4550000000058398257466", // Swedish
            "FI2112345600000785",     // Finnish
            "DE89370400440532013000", // German
    })
    void shouldAcceptValidIbans(String iban) {
        assertTrue(validator.isValid(iban, null));
    }

    @Test
    void shouldAcceptNullValue() {
        assertTrue(validator.isValid(null, null));
    }

    @Test
    void shouldRejectEmptyString() {
        assertFalse(validator.isValid("", null));
    }

    @Test
    void shouldRejectInvalidIban() {
        assertFalse(validator.isValid("INVALID_IBAN", null));
    }

    @Test
    void shouldRejectIbanWithWrongCheckDigits() {
        assertFalse(validator.isValid("DE00370400440532013000", null));
    }

    @Test
    void shouldAcceptIbanWithWhitespace() {
        assertTrue(validator.isValid("DE89 3704 0044 0532 0130 00", null));
    }

    @Test
    void shouldAcceptIbanWithTabsAndMultipleSpaces() {
        assertTrue(validator.isValid("SE45  5000\t0000 0583 9825 7466", null));
    }

    @Test
    void shouldAcceptLowercaseIban() {
        assertTrue(validator.isValid("de89370400440532013000", null));
    }

    @Test
    void shouldAcceptMixedCaseIban() {
        assertTrue(validator.isValid("Se4550000000058398257466", null));
    }

    @Test
    void shouldRejectIbanWithInvalidCountryCode() {
        assertFalse(validator.isValid("XX89370400440532013000", null));
    }

    @Test
    void shouldRejectTooShortIban() {
        assertFalse(validator.isValid("DE89", null));
    }

    @ParameterizedTest(name = "reject invalid: {0}")
    @ValueSource(strings = {
            "1234567890",
            "ABCDEFGHIJ",
            "DE893704004405320130001234567890", // too long
    })
    void shouldRejectMalformedValues(String value) {
        assertFalse(validator.isValid(value, null));
    }
}
