package com.fxpayment.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.IBANValidator;

public class IbanValidator implements ConstraintValidator<ValidIban, String> {

    private static final IBANValidator IBAN_VALIDATOR = IBANValidator.getInstance();

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        String iban = StringUtils.deleteWhitespace(value).toUpperCase();
        return IBAN_VALIDATOR.isValid(iban);
    }

}