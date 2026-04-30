package com.bofa.cbp.wire.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class FeeCalculatorTest {

    private final FeeCalculator calc = new FeeCalculator();

    @Test
    void domesticTransferFlatFee() {
        BigDecimal fee = calc.estimateFee("USD", new BigDecimal("1000.00"));
        assertEquals(new BigDecimal("15.00"), fee);
    }

    @Test
    void internationalTransferFlatPlusVariable() {
        BigDecimal fee = calc.estimateFee("EUR", new BigDecimal("10000.00"));
        // 45.00 + 10000 * 0.001 = 45.00 + 10.00 = 55.00
        assertEquals(new BigDecimal("55.00"), fee);
    }

    @Test
    void zeroAmountReturnsZero() {
        assertEquals(BigDecimal.ZERO, calc.estimateFee("USD", BigDecimal.ZERO));
    }

    @Test
    void negativeAmountReturnsZero() {
        assertEquals(BigDecimal.ZERO, calc.estimateFee("USD", new BigDecimal("-100.00")));
    }

    @Test
    void nullAmountReturnsZero() {
        assertEquals(BigDecimal.ZERO, calc.estimateFee("USD", null));
    }

    @Test
    void veryLargeInternationalAmount() {
        BigDecimal fee = calc.estimateFee("GBP", new BigDecimal("1000000.00"));
        // 45.00 + 1000000 * 0.001 = 45.00 + 1000.00 = 1045.00
        assertEquals(new BigDecimal("1045.00"), fee);
    }

    @Test
    void isDomesticUsd() {
        assertTrue(calc.isDomestic("USD"));
        assertTrue(calc.isDomestic("usd"));
    }

    @Test
    void isDomesticNonUsd() {
        assertFalse(calc.isDomestic("EUR"));
        assertFalse(calc.isDomestic(null));
    }

    @Test
    void netAmountDomestic() {
        BigDecimal net = calc.netAmount("USD", new BigDecimal("1000.00"));
        assertEquals(new BigDecimal("985.00"), net);
    }

    @Test
    void netAmountNullGross() {
        assertEquals(BigDecimal.ZERO, calc.netAmount("USD", null));
    }
}
