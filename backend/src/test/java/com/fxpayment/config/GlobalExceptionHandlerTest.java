package com.fxpayment.config;

import com.fxpayment.dto.ErrorResponse;
import com.fxpayment.exception.InvalidRequestException;
import com.fxpayment.exception.PaymentProcessingException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidationErrorsShouldReturn400WithFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("paymentRequest", "amount", "Amount is required"),
                new FieldError("paymentRequest", "currency", "Currency is required")
        ));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals(2, response.getBody().errors().size());
        assertTrue(response.getBody().errors().stream().anyMatch(e -> e.contains("Amount is required")));
        assertTrue(response.getBody().errors().stream().anyMatch(e -> e.contains("Currency is required")));
    }

    @Test
    void handleValidationErrorsShouldReturnDefaultMessage() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("paymentRequest", "recipientAccount", "Invalid IBAN")
        ));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationErrors(ex);

        assertEquals("Invalid IBAN", response.getBody().errors().get(0));
    }

    @Test
    void handleInvalidRequestShouldReturn400WithMessage() {
        InvalidRequestException ex = new InvalidRequestException("Unsupported currency code: ZZZ");

        ResponseEntity<ErrorResponse> response = handler.handleInvalidRequest(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals(1, response.getBody().errors().size());
        assertEquals("Unsupported currency code: ZZZ", response.getBody().errors().get(0));
    }

    @Test
    void handleIllegalArgumentShouldReturn400WithGenericMessage() {
        IllegalArgumentException ex = new IllegalArgumentException("some internal library detail");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals("Invalid request", response.getBody().errors().get(0));
    }

    @Test
    void handleIllegalArgumentShouldNotLeakExceptionMessage() {
        IllegalArgumentException ex = new IllegalArgumentException(
                "No enum constant com.fxpayment.model.PaymentStatus.INVALID");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex);

        assertFalse(response.getBody().errors().get(0).contains("enum"));
        assertFalse(response.getBody().errors().get(0).contains("com.fxpayment"));
    }

    @Test
    void handleDataIntegrityViolationShouldReturn409() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Duplicate key");

        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().status());
        assertEquals("A conflict occurred while processing your request", response.getBody().errors().get(0));
    }

    @Test
    void handleConstraintViolationShouldReturn400WithViolationDetails() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        Path.Node node = mock(Path.Node.class);
        when(node.getName()).thenReturn("size");
        when(path.iterator()).thenReturn(List.of(node).iterator());
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must be less than or equal to 100");
        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals(1, response.getBody().errors().size());
        assertEquals("size: must be less than or equal to 100", response.getBody().errors().get(0));
    }

    @Test
    void handleMissingHeaderShouldReturn400WithHeaderName() throws Exception {
        MissingRequestHeaderException ex = new MissingRequestHeaderException("X-Request-Id",
                new org.springframework.core.MethodParameter(
                        GlobalExceptionHandlerTest.class.getDeclaredMethod("handleMissingHeaderShouldReturn400WithHeaderName"), -1));

        ResponseEntity<ErrorResponse> response = handler.handleMissingHeader(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertTrue(response.getBody().errors().get(0).contains("X-Request-Id"));
    }

    @Test
    void handleUnreadableMessageShouldReturn400WithGenericMessage() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMessage()).thenReturn("JSON parse error: Unexpected character");

        ResponseEntity<ErrorResponse> response = handler.handleUnreadableMessage(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals("Malformed request body", response.getBody().errors().get(0));
    }

    @Test
    void handleUnreadableMessageShouldNotLeakParseDetails() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMessage()).thenReturn(
                "Cannot deserialize value of type `java.math.BigDecimal` from String \"abc\"");

        ResponseEntity<ErrorResponse> response = handler.handleUnreadableMessage(ex);

        assertFalse(response.getBody().errors().get(0).contains("BigDecimal"));
        assertFalse(response.getBody().errors().get(0).contains("deserialize"));
    }

    @Test
    void handleMethodNotSupportedShouldReturn405() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");

        ResponseEntity<ErrorResponse> response = handler.handleMethodNotSupported(ex);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(405, response.getBody().status());
        assertTrue(response.getBody().errors().get(0).contains("DELETE"));
    }

    @Test
    void handleNoResourceFoundShouldReturn404() throws Exception {
        NoResourceFoundException ex = new NoResourceFoundException(
                org.springframework.http.HttpMethod.GET, "/api/v1/nonexistent");

        ResponseEntity<ErrorResponse> response = handler.handleNoResourceFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("Resource not found", response.getBody().errors().get(0));
    }

    @Test
    void handleNoResourceFoundShouldNotLeakPath() throws Exception {
        NoResourceFoundException ex = new NoResourceFoundException(
                org.springframework.http.HttpMethod.GET, "/api/v1/admin/internal");

        ResponseEntity<ErrorResponse> response = handler.handleNoResourceFound(ex);

        assertFalse(response.getBody().errors().get(0).contains("admin"));
        assertFalse(response.getBody().errors().get(0).contains("internal"));
    }

    @Test
    void handleTypeMismatchShouldReturn400WithParameterName() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("page");
        when(ex.getMessage()).thenReturn("Failed to convert value of type 'String' to required type 'int'");

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertTrue(response.getBody().errors().get(0).contains("page"));
    }

    @Test
    void handleTypeMismatchShouldNotLeakTypeDetails() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("page");
        when(ex.getMessage()).thenReturn("Failed to convert value of type 'java.lang.String' to required type 'int'");

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex);

        assertFalse(response.getBody().errors().get(0).contains("java.lang.String"));
        assertFalse(response.getBody().errors().get(0).contains("convert"));
    }

    @Test
    void handlePaymentProcessingShouldReturn500WithMessage() {
        PaymentProcessingException ex = new PaymentProcessingException(
                "Payment could not be processed", new RuntimeException("DB down"));

        ResponseEntity<ErrorResponse> response = handler.handlePaymentProcessing(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().status());
        assertEquals("Payment could not be processed", response.getBody().errors().get(0));
    }

    @Test
    void handlePaymentProcessingShouldNotLeakCauseDetails() {
        PaymentProcessingException ex = new PaymentProcessingException(
                "Payment could not be processed",
                new RuntimeException("FATAL: connection to server at \"10.0.0.5\" refused"));

        ResponseEntity<ErrorResponse> response = handler.handlePaymentProcessing(ex);

        assertFalse(response.getBody().errors().get(0).contains("10.0.0.5"));
        assertFalse(response.getBody().errors().get(0).contains("FATAL"));
    }

    @Test
    void handleGenericExceptionShouldReturn500WithGenericMessage() {
        Exception ex = new RuntimeException("Something went wrong");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().status());
        assertEquals("An unexpected error occurred", response.getBody().errors().get(0));
    }

    @Test
    void handleGenericExceptionShouldNotLeakInternalDetails() {
        Exception ex = new RuntimeException("SQL syntax error at line 42: SELECT * FROM passwords");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);

        assertFalse(response.getBody().errors().get(0).contains("SQL"));
        assertFalse(response.getBody().errors().get(0).contains("passwords"));
    }

    @Test
    void allErrorResponsesShouldContainTimestamp() {
        InvalidRequestException ex = new InvalidRequestException("test");

        ResponseEntity<ErrorResponse> response = handler.handleInvalidRequest(ex);

        assertNotNull(response.getBody().timestamp());
    }
}
