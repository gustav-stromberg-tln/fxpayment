package com.fxpayment.util;

import java.math.RoundingMode;

public final class PaymentConstants {

    private PaymentConstants() {}

    public static final int MONEY_PRECISION = 19;
    public static final int INTERNAL_SCALE = 4;
    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    public static final int MAX_CURRENCY_DECIMALS = 4;

    public static final int FEE_RATE_PRECISION = 9;
    public static final int FEE_RATE_SCALE = 6;

    public static final String MIN_AMOUNT = "0.01";
    public static final String MAX_AMOUNT = "1000000";

    public static final int CURRENCY_CODE_LENGTH = 3;

    public static final int MIN_RECIPIENT_LENGTH = 2;
    public static final int MAX_RECIPIENT_LENGTH = 140;
    // Allows Latin letters, diacritical marks, and spaces only.
    public static final String RECIPIENT_NAME_PATTERN = "^[\\p{IsLatin}\\p{M} ]+$";

    public static final String DEFAULT_PAGE = "0";
    public static final String DEFAULT_PAGE_SIZE = "20";
    public static final long MAX_PAGE_SIZE = 100;

    public static final int STATUS_MAX_LENGTH = 20;

    public static final int UUID_STRING_LENGTH = 36;
}
