package com.bofa.cbp.txn.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Currency conversion via stubbed FX rates. In production this calls
 * the treasury FX service. For the self-contained demo we hard-code
 * a small rate table with rates frozen at 2024-01-01.
 */
@Component
public class CurrencyConverter {

    private static final Map<String, BigDecimal> RATES_TO_USD = Map.of(
            "USD", new BigDecimal("1.0000"),
            "EUR", new BigDecimal("1.1023"),
            "GBP", new BigDecimal("1.2712"),
            "JPY", new BigDecimal("0.0067"),
            "CAD", new BigDecimal("0.7489"),
            "MXN", new BigDecimal("0.0586")
    );

    public BigDecimal toUsd(BigDecimal amount, String currency) {
        BigDecimal rate = RATES_TO_USD.get(currency);
        if (rate == null) {
            throw new IllegalArgumentException("unsupported currency: " + currency);
        }
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal fromUsd(BigDecimal amountUsd, String currency) {
        BigDecimal rate = RATES_TO_USD.get(currency);
        if (rate == null) {
            throw new IllegalArgumentException("unsupported currency: " + currency);
        }
        return amountUsd.divide(rate, 4, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isSupported(String currency) {
        return RATES_TO_USD.containsKey(currency);
    }
}
