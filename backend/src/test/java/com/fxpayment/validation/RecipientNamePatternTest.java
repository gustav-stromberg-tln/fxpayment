package com.fxpayment.validation;

import com.fxpayment.util.PaymentConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Tests the RECIPIENT_NAME_PATTERN regex in isolation.
// This regex only validates the character set (Latin letters, combining marks, spaces).
// Semantic rules like "not blank" and length limits are enforced by @NotBlank and @Size
// on PaymentRequest.recipient, so they are intentionally outside the scope of this class.
@DisplayName("Recipient name regex pattern")
class RecipientNamePatternTest {

    private static final Pattern PATTERN =
            Pattern.compile(PaymentConstants.RECIPIENT_NAME_PATTERN);

    private boolean matches(String input) {
        return PATTERN.matcher(input).matches();
    }

    @Nested
    @DisplayName("accepts valid Latin character combinations")
    class ValidInputs {

        @ParameterizedTest
        @ValueSource(strings = {
                "xqzplm",
                "BDFHJK",
                "xQzPlM",
                "xq zp lm",
                "ñøæßðþ",
                "àèìòùü",
                "ÿĊŦħŋ",
                "ĀāĂăĄą",
                " xq ",
        })
        @DisplayName("basic Latin, extended Latin, and spaces")
        void shouldMatchLatinLettersAndSpaces(String input) {
            assertTrue(matches(input), () -> "Expected match for: «" + input + "»");
        }

        @Test
        @DisplayName("combining grave accent (U+0300)")
        void shouldMatchCombiningGrave() {
            assertTrue(matches("a\u0300b\u0300"));
        }

        @Test
        @DisplayName("combining acute accent (U+0301)")
        void shouldMatchCombiningAcute() {
            assertTrue(matches("e\u0301z\u0301"));
        }

        @Test
        @DisplayName("combining diaeresis (U+0308)")
        void shouldMatchCombiningDiaeresis() {
            assertTrue(matches("a\u0308o\u0308u\u0308"));
        }

        @Test
        @DisplayName("combining cedilla (U+0327)")
        void shouldMatchCombiningCedilla() {
            assertTrue(matches("c\u0327x\u0327"));
        }

        @Test
        @DisplayName("combining tilde (U+0303)")
        void shouldMatchCombiningTilde() {
            assertTrue(matches("n\u0303q\u0303"));
        }

        @Test
        @DisplayName("multiple combining marks stacked on one base letter")
        void shouldMatchStackedCombiningMarks() {
            // a + combining acute + combining diaeresis
            assertTrue(matches("a\u0301\u0308"));
        }

        @Test
        @DisplayName("single Latin letter")
        void shouldMatchSingleLetter() {
            assertTrue(matches("x"));
        }

        @Test
        @DisplayName("single space (regex allows; length validated separately)")
        void shouldMatchSingleSpace() {
            assertTrue(matches(" "));
        }

        @Test
        @DisplayName("long repetition of extended Latin char")
        void shouldMatchLongRepetition() {
            assertTrue(matches("ø".repeat(200)));
        }

        @Test
        @DisplayName("spaces between extended Latin segments")
        void shouldMatchExtendedLatinWithSpaces() {
            assertTrue(matches("ñøæ ßðþ àèì"));
        }
    }

    @Nested
    @DisplayName("rejects invalid character combinations")
    class InvalidInputs {

        @ParameterizedTest
        @ValueSource(strings = {
                "abc7xyz",
                "42",
                "xq!zm",
                "ab@cd",
                "xq#zm",
                "ab.cd",
                "xq-zm",
                "ab'cd",
                "xq&zm",
                "<xq>",
                "$€£",
                "∑∏",
        })
        @DisplayName("digits, punctuation, and symbols")
        void shouldRejectDigitsAndSymbols(String input) {
            assertFalse(matches(input), () -> "Expected no match for: «" + input + "»");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "абвгд",
                "αβγδ",
                "漢字仮名",
                "عربي",
                "हिन्दी",
                "ქართ",
        })
        @DisplayName("non-Latin scripts (Cyrillic, Greek, CJK, Arabic, Devanagari, Georgian)")
        void shouldRejectNonLatinScripts(String input) {
            assertFalse(matches(input), () -> "Expected no match for: «" + input + "»");
        }

        @Test
        @DisplayName("empty string")
        void shouldRejectEmptyString() {
            assertFalse(matches(""));
        }

        @Test
        @DisplayName("tab character between Latin letters")
        void shouldRejectTab() {
            assertFalse(matches("xq\tzm"));
        }

        @Test
        @DisplayName("newline between Latin letters")
        void shouldRejectNewline() {
            assertFalse(matches("xq\nzm"));
        }

        @Test
        @DisplayName("carriage return between Latin letters")
        void shouldRejectCarriageReturn() {
            assertFalse(matches("xq\rzm"));
        }

        @Test
        @DisplayName("null character embedded in Latin letters")
        void shouldRejectNullChar() {
            assertFalse(matches("xq\0zm"));
        }

        @Test
        @DisplayName("emoji between Latin letters")
        void shouldRejectEmoji() {
            assertFalse(matches("xq\uD83D\uDE00zm"));
        }

        @Test
        @DisplayName("Latin + Cyrillic mixed")
        void shouldRejectMixedLatinCyrillic() {
            assertFalse(matches("xqабcd"));
        }

        @Test
        @DisplayName("Latin + digits mixed")
        void shouldRejectMixedLatinDigits() {
            assertFalse(matches("ab cd7ef"));
        }

        @Test
        @DisplayName("Latin + underscore")
        void shouldRejectUnderscore() {
            assertFalse(matches("xq_zm"));
        }

    }

    // These inputs pass the regex but are rejected at the DTO layer:
    // @NotBlank catches whitespace-only strings, @Size(min=2) catches too-short inputs.
    // assertTrue here is meant to confirm the regex itself does not reject them.
    @Nested
    @DisplayName("edge cases (documented regex behaviour)")
    class EdgeCases {

        @Test
        @DisplayName("standalone combining mark (U+0301) is accepted because \\p{M} is in the allowed set")
        void standaloneCombiningMarkIsAccepted() {
            assertTrue(matches("\u0301"));
        }

        @Test
        @DisplayName("only spaces are accepted by regex (length validation is separate)")
        void onlySpacesAcceptedByRegex() {
            assertTrue(matches("   "));
        }
    }
}
