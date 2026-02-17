package com.fxpayment.service;

import com.fxpayment.dto.PaymentRequest;
import com.fxpayment.exception.InvalidRequestException;
import com.fxpayment.model.CurrencyEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static com.fxpayment.utils.TestDataFactory.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentValidationServiceTest {

    private static final String TOO_MANY_DECIMALS = "too many decimal places";

    @Mock
    private CurrencyService currencyService;

    @InjectMocks
    private PaymentValidationService paymentValidationService;

    private void stubCurrency(String code) {
        when(currencyService.findByCode(code))
                .thenReturn(Optional.ofNullable(CURRENCIES.get(code)));
    }

    @Test
    void shouldResolveValidCurrencyAndAmount() {
        stubCurrency("USD");
        PaymentRequest request = aPaymentRequest().build();

        CurrencyEntity result = paymentValidationService.resolveAndValidateCurrency(request);

        assertEquals("USD", result.getCode());
    }

    @Test
    void shouldRejectUnsupportedCurrency() {
        when(currencyService.findByCode(UNSUPPORTED_CURRENCY)).thenReturn(Optional.empty());
        PaymentRequest request = aPaymentRequest().currency(UNSUPPORTED_CURRENCY).build();

        assertThrows(InvalidRequestException.class,
                () -> paymentValidationService.resolveAndValidateCurrency(request));
    }

    @Test
    void shouldRejectAmountWithTooManyDecimalPlaces() {
        stubCurrency("USD");
        PaymentRequest request = aPaymentRequest().amount(new BigDecimal("100.123")).build();

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> paymentValidationService.resolveAndValidateCurrency(request));
        assertTrue(ex.getMessage().contains(TOO_MANY_DECIMALS));
    }

    @Test
    void shouldAcceptWholeAmountForZeroDecimalCurrency() {
        stubCurrency("JPY");
        PaymentRequest request = aPaymentRequest()
                .amount(new BigDecimal("10000")).currency("JPY").build();

        CurrencyEntity result = paymentValidationService.resolveAndValidateCurrency(request);

        assertEquals("JPY", result.getCode());
    }

    @Test
    void shouldRejectFractionalAmountForZeroDecimalCurrency() {
        stubCurrency("JPY");
        PaymentRequest request = aPaymentRequest()
                .amount(new BigDecimal("10000.5")).currency("JPY").build();

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> paymentValidationService.resolveAndValidateCurrency(request));
        assertTrue(ex.getMessage().contains(TOO_MANY_DECIMALS));
        assertTrue(ex.getMessage().contains("JPY"));
    }

    @Test
    void shouldAcceptThreeDecimalPlacesForBHD() {
        stubCurrency("BHD");
        PaymentRequest request = aPaymentRequest()
                .amount(new BigDecimal("100.500")).currency("BHD").build();

        CurrencyEntity result = paymentValidationService.resolveAndValidateCurrency(request);

        assertEquals("BHD", result.getCode());
    }

    @Test
    void shouldRejectFourDecimalPlacesForBHD() {
        stubCurrency("BHD");
        PaymentRequest request = aPaymentRequest()
                .amount(new BigDecimal("100.5001")).currency("BHD").build();

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> paymentValidationService.resolveAndValidateCurrency(request));
        assertTrue(ex.getMessage().contains(TOO_MANY_DECIMALS));
        assertTrue(ex.getMessage().contains("BHD"));
    }

    @Test
    void shouldRejectThreeDecimalPlacesForEUR() {
        stubCurrency("EUR");
        PaymentRequest request = aPaymentRequest()
                .amount(new BigDecimal("100.123")).currency("EUR").build();

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> paymentValidationService.resolveAndValidateCurrency(request));
        assertTrue(ex.getMessage().contains(TOO_MANY_DECIMALS));
        assertTrue(ex.getMessage().contains("EUR"));
    }

    @Test
    void shouldAcceptExactDecimalLimitForEUR() {
        stubCurrency("EUR");
        PaymentRequest request = aPaymentRequest()
                .amount(new BigDecimal("100.99")).currency("EUR").build();

        CurrencyEntity result = paymentValidationService.resolveAndValidateCurrency(request);

        assertEquals("EUR", result.getCode());
    }

    @Test
    void shouldAcceptExactDecimalLimitForBHD() {
        stubCurrency("BHD");
        PaymentRequest request = aPaymentRequest()
                .amount(new BigDecimal("200.123")).currency("BHD").build();

        CurrencyEntity result = paymentValidationService.resolveAndValidateCurrency(request);

        assertEquals("BHD", result.getCode());
    }

    @Test
    void shouldRejectOneDecimalPlaceForJPY() {
        stubCurrency("JPY");
        PaymentRequest request = aPaymentRequest()
                .amount(new BigDecimal("100.1")).currency("JPY").build();

        InvalidRequestException ex = assertThrows(InvalidRequestException.class,
                () -> paymentValidationService.resolveAndValidateCurrency(request));
        assertTrue(ex.getMessage().contains(TOO_MANY_DECIMALS));
        assertTrue(ex.getMessage().contains("JPY"));
        assertTrue(ex.getMessage().contains("maximum 0 allowed"));
    }
}
