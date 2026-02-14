package com.fxpayment.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.validator.routines.IBANValidator;

import java.util.regex.Pattern;

public class IbanValidator implements ConstraintValidator<ValidIban, String> {

    private static final IBANValidator IBAN_VALIDATOR = IBANValidator.getInstance();
    private static final Pattern WHITESPACE= Pattern.compile("\\s+");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        String iban = WHITESPACE.matcher(value).replaceAll("").toUpperCase();
        return IBAN_VALIDATOR.isValid(iban);
    }
}