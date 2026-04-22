package com.bofa.cbp.wire.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Domestic vs. international wire fee schedule. Intentionally lightweight;
 * the real schedule is owned by Payments Ops and ingested separately.
 * Not covered by unit tests yet.
 */
@Component
public class FeeCalculator {

    private static final BigDecimal DOMESTIC_FLAT = new BigDecimal("15.00");
    private static final BigDecimal INTERNATIONAL_FLAT = new BigDecimal("45.00");
    private static final BigDecimal INTERNATIONAL_BPS = new BigDecimal("0.0010");

    public BigDecimal estimateFee(String currency, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        if (isDomestic(currency)) {
            return DOMESTIC_FLAT;
        }
        BigDecimal variable = amount.multiply(INTERNATIONAL_BPS).setScale(2, RoundingMode.HALF_UP);
        return INTERNATIONAL_FLAT.add(variable);
    }

    public boolean isDomestic(String currency) {
        return currency != null && currency.equalsIgnoreCase("USD");
    }

    public BigDecimal netAmount(String currency, BigDecimal gross) {
        BigDecimal fee = estimateFee(currency, gross);
        if (gross == null) return BigDecimal.ZERO;
        return gross.subtract(fee);
    }
}
