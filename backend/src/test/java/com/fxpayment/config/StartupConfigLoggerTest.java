package com.fxpayment.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("StartupConfigLogger unit tests")
class StartupConfigLoggerTest {

    @ParameterizedTest
    @DisplayName("masks credentials in datasource URLs")
    @CsvSource({
            "jdbc:postgresql://db:5432/fxpayment,             jdbc:postgresql://db:5432/fxpayment",
            "jdbc:postgresql://user:secret@db:5432/fxpayment, jdbc:postgresql://***@db:5432/fxpayment",
            "jdbc:mysql://root:pass@localhost/mydb,            jdbc:mysql://***@localhost/mydb",
    })
    void shouldMaskCredentials(String input, String expected) {
        assertEquals(expected, StartupConfigLogger.maskCredentials(input));
    }

    @Test
    @DisplayName("returns placeholder when URL is null")
    void shouldReturnPlaceholderWhenUrlIsNull() {
        assertEquals("not configured", StartupConfigLogger.maskCredentials(null));
    }
}
