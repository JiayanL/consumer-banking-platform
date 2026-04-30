package com.bofa.cbp.wire.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SanctionsScreenerTest {

    private final SanctionsScreener screener = new SanctionsScreener();

    @Test
    void clearCountryPasses() {
        assertFalse(screener.isCountryBlocked("US"));
        assertFalse(screener.isCountryBlocked("GB"));
    }

    @Test
    void blockedCountriesDetected() {
        assertTrue(screener.isCountryBlocked("IR"));
        assertTrue(screener.isCountryBlocked("KP"));
        assertTrue(screener.isCountryBlocked("SY"));
        assertTrue(screener.isCountryBlocked("CU"));
    }

    @Test
    void countryCheckCaseInsensitive() {
        assertTrue(screener.isCountryBlocked("ir"));
        assertTrue(screener.isCountryBlocked("Kp"));
    }

    @Test
    void nullCountryNotBlocked() {
        assertFalse(screener.isCountryBlocked(null));
    }

    @Test
    void clearNamePasses() {
        assertFalse(screener.isNameBlocked("Jane Smith"));
    }

    @Test
    void blockedNameDetected() {
        assertTrue(screener.isNameBlocked("Specially Designated Nationals LLC"));
        assertTrue(screener.isNameBlocked("BLOCKED PERSONS Entity"));
    }

    @Test
    void nullNameNotBlocked() {
        assertFalse(screener.isNameBlocked(null));
    }

    @Test
    void screenClearedResult() {
        SanctionsScreener.ScreeningResult result = screener.screen("Jane Smith", "US");
        assertTrue(result.isCleared());
        assertNull(result.getReason());
    }

    @Test
    void screenBlockedCountry() {
        SanctionsScreener.ScreeningResult result = screener.screen("Jane Smith", "IR");
        assertFalse(result.isCleared());
        assertNotNull(result.getReason());
    }

    @Test
    void screenBlockedName() {
        SanctionsScreener.ScreeningResult result = screener.screen("specially designated nationals", "US");
        assertFalse(result.isCleared());
        assertNotNull(result.getReason());
    }

    @Test
    void emptyNamePasses() {
        assertFalse(screener.isNameBlocked(""));
    }

    @Test
    void specialCharactersInName() {
        assertFalse(screener.isNameBlocked("O'Brien & Associates"));
    }
}
