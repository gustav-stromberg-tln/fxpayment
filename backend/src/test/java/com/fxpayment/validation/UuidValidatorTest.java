package com.fxpayment.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class UuidValidatorTest {

    private final UuidValidator validator = new UuidValidator();

    @Test
    void shouldAcceptValidUuid() {
        assertTrue(validator.isValid("550e8400-e29b-41d4-a716-446655440000", null));
    }

    @Test
    void shouldAcceptNullValue() {
        assertTrue(validator.isValid(null, null));
    }

    @Test
    void shouldRejectEmptyString() {
        assertFalse(validator.isValid("", null));
    }

    @ParameterizedTest(name = "reject invalid: {0}")
    @ValueSource(strings = {
            "not-a-uuid",
            "12345",
            "550e8400-e29b-41d4-a716",
            "550e8400-e29b-41d4-a716-446655440000-extra",
            "zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz",
    })
    void shouldRejectInvalidUuids(String value) {
        assertFalse(validator.isValid(value, null));
    }

    @ParameterizedTest(name = "accept valid: {0}")
    @ValueSource(strings = {
            "00000000-0000-0000-0000-000000000000",
            "ffffffff-ffff-ffff-ffff-ffffffffffff",
            "123e4567-e89b-12d3-a456-426614174000",
    })
    void shouldAcceptVariousValidUuids(String value) {
        assertTrue(validator.isValid(value, null));
    }
}
