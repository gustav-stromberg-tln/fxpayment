package com.fxpayment.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    private static final String FIXED_TIMESTAMP = "2025-01-15T10:30:00Z";

    @Test
    void ofWithSingleErrorShouldWrapInList() {
        ErrorResponse response = ErrorResponse.of(400, "Field is required");

        assertEquals(400, response.status());
        assertEquals(1, response.errors().size());
        assertEquals("Field is required", response.errors().get(0));
    }

    @Test
    void ofWithErrorListShouldPreserveAll() {
        List<String> errors = List.of("amount: required", "currency: invalid");

        ErrorResponse response = ErrorResponse.of(400, errors);

        assertEquals(400, response.status());
        assertEquals(2, response.errors().size());
        assertEquals(errors, response.errors());
    }

    @Test
    void ofShouldIncludeTimestamp() {
        Instant before = Instant.now();
        ErrorResponse response = ErrorResponse.of(500, "Server error");
        Instant after = Instant.now();

        assertNotNull(response.timestamp());
        assertFalse(response.timestamp().isEmpty());

        Instant parsed = Instant.parse(response.timestamp());
        assertFalse(parsed.isBefore(before), "Timestamp should not be before test start");
        assertFalse(parsed.isAfter(after), "Timestamp should not be after test end");
    }

    @Test
    void ofShouldPreserveStatusCode() {
        assertEquals(400, ErrorResponse.of(400, "bad request").status());
        assertEquals(409, ErrorResponse.of(409, "conflict").status());
        assertEquals(500, ErrorResponse.of(500, "server error").status());
    }

    @Test
    void errorListShouldBeUnmodifiable() {
        ErrorResponse response = ErrorResponse.of(400, "error");

        assertThrows(UnsupportedOperationException.class, () -> response.errors().add("another"));
    }

    @Test
    void mutatingInputListShouldNotAffectResponse() {
        List<String> mutableErrors = new ArrayList<>(List.of("error1", "error2"));
        ErrorResponse response = ErrorResponse.of(400, mutableErrors);

        mutableErrors.add("error3");

        assertEquals(2, response.errors().size());
        assertEquals(List.of("error1", "error2"), response.errors());
    }

    @Test
    void ofWithEmptyListShouldWork() {
        ErrorResponse response = ErrorResponse.of(400, List.of());

        assertTrue(response.errors().isEmpty());
    }

    @Test
    void ofWithNullMessageShouldThrow() {
        assertThrows(NullPointerException.class, () -> ErrorResponse.of(400, (String) null));
    }

    @Test
    void ofWithNullListShouldThrow() {
        assertThrows(NullPointerException.class, () -> ErrorResponse.of(400, (List<String>) null));
    }

    @Test
    void responsesWithSameDataShouldBeEqual() {
        String timestamp = FIXED_TIMESTAMP;
        ErrorResponse r1 = new ErrorResponse(timestamp, 400, List.of("error"));
        ErrorResponse r2 = new ErrorResponse(timestamp, 400, List.of("error"));

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void responsesWithDifferentDataShouldNotBeEqual() {
        String timestamp = FIXED_TIMESTAMP;
        ErrorResponse r1 = new ErrorResponse(timestamp, 400, List.of("error1"));
        ErrorResponse r2 = new ErrorResponse(timestamp, 400, List.of("error2"));

        assertNotEquals(r1, r2);
    }
}
