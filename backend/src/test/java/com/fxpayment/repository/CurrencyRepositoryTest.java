package com.fxpayment.repository;

import com.fxpayment.annotation.RepositoryTest;
import com.fxpayment.model.CurrencyEntity;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static com.fxpayment.utils.TestDataFactory.*;
import static org.junit.jupiter.api.Assertions.*;

@RepositoryTest
class CurrencyRepositoryTest {

    private static final BigDecimal STANDARD_FEE_RATE = new BigDecimal("0.0100");
    private static final BigDecimal STANDARD_MINIMUM_FEE = new BigDecimal("5.0000");

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        seedCurrencies(currencyRepository);
    }

    @Test
    void findByIdShouldReturnCurrencyWhenExists() {
        CurrencyEntity usd = currencyRepository.findById("USD").orElseThrow();

        assertEquals("US Dollar", usd.getName());
        assertEquals(0, STANDARD_FEE_RATE.compareTo(usd.getFeeRate()));
        assertEquals(0, STANDARD_MINIMUM_FEE.compareTo(usd.getMinimumFee()));
        assertEquals(2, usd.getDecimals());
        assertNotNull(usd.getCreatedAt());
        assertNotNull(usd.getUpdatedAt());
    }

    @Test
    void findByIdShouldReturnCorrectFieldsForEur() {
        CurrencyEntity eur = currencyRepository.findById("EUR").orElseThrow();

        assertEquals("Euro", eur.getName());
        assertEquals(0, BigDecimal.ZERO.compareTo(eur.getFeeRate()));
        assertEquals(0, BigDecimal.ZERO.compareTo(eur.getMinimumFee()));
        assertEquals(2, eur.getDecimals());
        assertNotNull(eur.getCreatedAt());
        assertNotNull(eur.getUpdatedAt());
    }

    @Test
    void findByIdShouldReturnCorrectFieldsForGbp() {
        CurrencyEntity gbp = currencyRepository.findById("GBP").orElseThrow();

        assertEquals("British Pound", gbp.getName());
        assertEquals(0, STANDARD_FEE_RATE.compareTo(gbp.getFeeRate()));
        assertEquals(0, STANDARD_MINIMUM_FEE.compareTo(gbp.getMinimumFee()));
        assertEquals(2, gbp.getDecimals());
        assertNotNull(gbp.getCreatedAt());
        assertNotNull(gbp.getUpdatedAt());
    }

    @Test
    void saveShouldPersistZeroDecimalCurrencyWithAllFields() {
        CurrencyEntity jpy = jpyCurrency();

        currencyRepository.saveAndFlush(jpy);
        CurrencyEntity found = currencyRepository.findById("JPY").orElseThrow();

        assertEquals("Japanese Yen", found.getName());
        assertEquals(0, STANDARD_FEE_RATE.compareTo(found.getFeeRate()));
        assertEquals(0, found.getDecimals());
        assertNotNull(found.getCreatedAt());
        assertNotNull(found.getUpdatedAt());
    }

    @Test
    void saveShouldPersistThreeDecimalCurrencyWithAllFields() {
        CurrencyEntity bhd = bhdCurrency();

        currencyRepository.saveAndFlush(bhd);
        CurrencyEntity found = currencyRepository.findById("BHD").orElseThrow();

        assertEquals("Bahraini Dinar", found.getName());
        assertEquals(0, STANDARD_FEE_RATE.compareTo(found.getFeeRate()));
        assertEquals(3, found.getDecimals());
        assertNotNull(found.getCreatedAt());
        assertNotNull(found.getUpdatedAt());
    }

    @Test
    void findByIdShouldReturnEmptyForNonexistentCurrency() {
        Optional<CurrencyEntity> result = currencyRepository.findById(UNSUPPORTED_CURRENCY);

        assertTrue(result.isEmpty());
    }

    @Test
    void findAllShouldReturnAllCurrencies() {
        List<CurrencyEntity> currencies = currencyRepository.findAll();

        assertEquals(3, currencies.size());
    }

    @Test
    void saveShouldPersistNewCurrency() {
        CurrencyEntity currency = aCurrency()
                .code("CHF").name("Swiss Franc")
                .feeRate(new BigDecimal("0.0150"))
                .minimumFee(new BigDecimal("3.0000"))
                .build();

        CurrencyEntity saved = currencyRepository.saveAndFlush(currency);

        assertEquals(currency.getCode(), saved.getCode());
        assertEquals(currency.getName(), saved.getName());
        assertEquals(0, currency.getFeeRate().compareTo(saved.getFeeRate()));
        assertEquals(0, currency.getMinimumFee().compareTo(saved.getMinimumFee()));
        assertEquals(currency.getDecimals(), saved.getDecimals());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void saveShouldPersistCurrencyWithExplicitDecimals() {
        CurrencyEntity currency = jpyCurrency();

        currencyRepository.saveAndFlush(currency);
        CurrencyEntity found = currencyRepository.findById(currency.getCode()).orElseThrow();

        assertEquals(currency.getDecimals(), found.getDecimals());
    }

    @Test
    void saveShouldSetCreatedAtAndUpdatedAtOnInsert() {
        CurrencyEntity currency = jpyCurrency();

        CurrencyEntity saved = currencyRepository.saveAndFlush(currency);

        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertTrue(Duration.between(saved.getCreatedAt(), saved.getUpdatedAt()).abs().toMillis() < 1,
                "createdAt and updatedAt should be set within 1ms of each other on insert");
    }

    @Test
    void onUpdateShouldSetUpdatedAtTimestamp() {
        CurrencyEntity original = jpyCurrency();
        CurrencyEntity saved = currencyRepository.saveAndFlush(original);
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        CurrencyEntity modified = aCurrency()
                .code(saved.getCode()).name("Updated Yen")
                .feeRate(new BigDecimal("0.0200"))
                .minimumFee(new BigDecimal("10.0000"))
                .decimals(saved.getDecimals())
                .createdAt(saved.getCreatedAt())
                .build();
        entityManager.merge(modified);
        entityManager.flush();
        entityManager.clear();

        CurrencyEntity updated = currencyRepository.findById(saved.getCode()).orElseThrow();
        assertNotNull(updated.getUpdatedAt());
        assertFalse(updated.getUpdatedAt().isBefore(saved.getCreatedAt()),
                "updatedAt must not be before createdAt");
        assertEquals("Updated Yen", updated.getName());
    }

    @Test
    void savingWithSameCodeShouldUpdateExistingCurrency() {
        CurrencyEntity original = jpyCurrency();
        currencyRepository.saveAndFlush(original);
        String updatedName = "Updated Yen";

        currencyRepository.saveAndFlush(aCurrency()
                .code(original.getCode()).name(updatedName)
                .feeRate(new BigDecimal("0.0200"))
                .minimumFee(new BigDecimal("10.0000"))
                .decimals(original.getDecimals()).build());

        CurrencyEntity found = currencyRepository.findById(original.getCode()).orElseThrow();
        assertEquals(updatedName, found.getName());
    }

    // Note: The DB-level CHECK constraints (check_decimals, check_fee_rate_range)
    // are defined in V1__initial_schema.sql which is not applied in tests (Flyway
    // disabled, ddl-auto: create-drop). These tests exercise the JPA @Min/@Max
    // bean validation annotations on CurrencyEntity instead. For full constraint
    // testing, consider enabling Flyway in tests or using Testcontainers with
    // PostgreSQL.

    @Nested
    @DisplayName("H2 vs PostgreSQL constraint gaps")
    class ConstraintGaps {

        @Test
        @DisplayName("fee_rate above 1.0 is NOT rejected in H2 (Postgres CHECK constraint gap)")
        void feeRateAboveOneShouldBeAcceptedByH2() {
            // In production, CHECK (fee_rate >= 0 AND fee_rate <= 1.0) would reject this.
            // In H2 with Flyway disabled, the CHECK constraint does not exist.
            // Consider adding @DecimalMax("1.0") to CurrencyEntity.feeRate
            // or using Testcontainers with PostgreSQL for full constraint coverage.
            CurrencyEntity currency = aCurrency()
                    .code("TST").name("Test")
                    .feeRate(new BigDecimal("5.0000"))
                    .minimumFee(new BigDecimal("1.0000"))
                    .build();

            CurrencyEntity saved = currencyRepository.saveAndFlush(currency);

            assertEquals(0, new BigDecimal("5.0000").compareTo(saved.getFeeRate()));
        }

        @Test
        @DisplayName("negative fee_rate is NOT rejected in H2 (Postgres CHECK constraint gap)")
        void negativeFeeRateShouldBeAcceptedByH2() {
            // In production, CHECK (fee_rate >= 0) would reject this.
            CurrencyEntity currency = aCurrency()
                    .code("NEG").name("Negative Fee")
                    .feeRate(new BigDecimal("-0.0100"))
                    .minimumFee(new BigDecimal("1.0000"))
                    .build();

            CurrencyEntity saved = currencyRepository.saveAndFlush(currency);

            assertEquals(0, new BigDecimal("-0.0100").compareTo(saved.getFeeRate()));
        }
    }

    @Nested
    @DisplayName("Decimals validation (JPA @Min/@Max)")
    class DecimalsValidation {

        @ParameterizedTest(name = "decimals={0} is valid")
        @ValueSource(shorts = {0, 1, 2, 3, 4})
        void shouldAcceptDecimalsInValidRange(short decimals) {
            CurrencyEntity currency = aCurrency()
                    .code("T" + decimals + "T").name("Test")
                    .decimals(decimals).build();

            CurrencyEntity saved = currencyRepository.saveAndFlush(currency);

            assertEquals(decimals, saved.getDecimals());
        }

        @Test
        @DisplayName("decimals above maximum (5) should be rejected by bean validation")
        void shouldRejectDecimalsAboveMaximum() {
            CurrencyEntity currency = aCurrency()
                    .code("TST").name("Test")
                    .decimals((short) 5).build();

            assertThrows(ConstraintViolationException.class,
                    () -> currencyRepository.saveAndFlush(currency));
        }

        @Test
        @DisplayName("negative decimals should be rejected by bean validation")
        void shouldRejectNegativeDecimals() {
            CurrencyEntity currency = aCurrency()
                    .code("TST").name("Test")
                    .decimals((short) -1).build();

            assertThrows(ConstraintViolationException.class,
                    () -> currencyRepository.saveAndFlush(currency));
        }
    }
}
