package com.bofa.cbp.txn.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyConverterTest {

    private final CurrencyConverter converter = new CurrencyConverter();

    @Test
    void usdToUsdIsIdentity() {
        BigDecimal result = converter.toUsd(new BigDecimal("100.00"), "USD");
        assertEquals(0, new BigDecimal("100.00").compareTo(result));
    }

    @Test
    void eurToUsd() {
        BigDecimal result = converter.toUsd(new BigDecimal("100.00"), "EUR");
        assertEquals(0, new BigDecimal("110.23").compareTo(result));
    }

    @Test
    void jpyToUsd() {
        BigDecimal result = converter.toUsd(new BigDecimal("10000"), "JPY");
        assertEquals(0, new BigDecimal("67.00").compareTo(result));
    }

    @Test
    void fromUsdToEur() {
        BigDecimal result = converter.fromUsd(new BigDecimal("110.23"), "EUR");
        assertEquals(0, new BigDecimal("100.00").compareTo(result));
    }

    @Test
    void unsupportedCurrencyToUsd() {
        assertThrows(IllegalArgumentException.class,
                () -> converter.toUsd(new BigDecimal("100.00"), "XYZ"));
    }

    @Test
    void unsupportedCurrencyFromUsd() {
        assertThrows(IllegalArgumentException.class,
                () -> converter.fromUsd(new BigDecimal("100.00"), "XYZ"));
    }

    @Test
    void isSupportedTrue() {
        assertTrue(converter.isSupported("USD"));
    }

    @Test
    void isSupportedFalse() {
        assertFalse(converter.isSupported("XYZ"));
    }
}
