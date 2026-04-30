package com.bofa.cbp.txn.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CurrencyConverterTest {

    private final CurrencyConverter converter = new CurrencyConverter();

    @Test
    void usdToUsdNoConversion() {
        BigDecimal result = converter.toUsd(new BigDecimal("100.00"), "USD");
        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    void eurToUsd() {
        BigDecimal result = converter.toUsd(new BigDecimal("100.00"), "EUR");
        assertEquals(new BigDecimal("110.23"), result);
    }

    @Test
    void gbpToUsd() {
        BigDecimal result = converter.toUsd(new BigDecimal("100.00"), "GBP");
        assertEquals(new BigDecimal("127.12"), result);
    }

    @Test
    void fromUsdToEur() {
        BigDecimal result = converter.fromUsd(new BigDecimal("110.23"), "EUR");
        assertNotNull(result);
        assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void unsupportedCurrencyThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> converter.toUsd(new BigDecimal("100.00"), "XYZ"));
    }

    @Test
    void unsupportedCurrencyFromUsdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> converter.fromUsd(new BigDecimal("100.00"), "XYZ"));
    }

    @Test
    void isSupportedTrue() {
        assertTrue(converter.isSupported("USD"));
        assertTrue(converter.isSupported("EUR"));
    }

    @Test
    void isSupportedFalse() {
        assertFalse(converter.isSupported("XYZ"));
    }
}
