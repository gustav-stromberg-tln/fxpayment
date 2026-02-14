package com.fxpayment.dto;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        String timestamp,
        int status,
        List<String> errors
) {
    public static ErrorResponse of(int status, String error) {
        return new ErrorResponse(Instant.now().toString(), status, List.of(error));
    }

    public static ErrorResponse of(int status, List<String> errors) {
        return new ErrorResponse(Instant.now().toString(), status, errors);
    }
}
