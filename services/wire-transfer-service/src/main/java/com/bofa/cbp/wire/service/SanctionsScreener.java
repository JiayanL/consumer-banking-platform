package com.bofa.cbp.wire.service;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Placeholder sanctions (OFAC-style) screener. The production version
 * will be backed by the central sanctions-data feed; for now this is
 * a stubbed in-memory allowlist keyed by ISO country code. Not covered
 * by unit tests yet (see PLAT-1840).
 */
@Component
public class SanctionsScreener {

    private static final Set<String> BLOCKED_COUNTRIES = new HashSet<>(Arrays.asList(
            "IR", "KP", "SY", "CU"
    ));

    private static final Set<String> BLOCKED_NAMES = new HashSet<>(Arrays.asList(
            "specially designated nationals",
            "blocked persons"
    ));

    public boolean isCountryBlocked(String isoCountryCode) {
        if (isoCountryCode == null) return false;
        return BLOCKED_COUNTRIES.contains(isoCountryCode.toUpperCase(Locale.ROOT));
    }

    public boolean isNameBlocked(String beneficiaryName) {
        if (beneficiaryName == null) return false;
        String normalized = beneficiaryName.toLowerCase(Locale.ROOT).trim();
        for (String blocked : BLOCKED_NAMES) {
            if (normalized.contains(blocked)) {
                return true;
            }
        }
        return false;
    }

    public ScreeningResult screen(String beneficiaryName, String isoCountryCode) {
        if (isCountryBlocked(isoCountryCode)) {
            return new ScreeningResult(false, "country " + isoCountryCode + " is on the block list");
        }
        if (isNameBlocked(beneficiaryName)) {
            return new ScreeningResult(false, "beneficiary name is on the block list");
        }
        return new ScreeningResult(true, null);
    }

    public static final class ScreeningResult {
        private final boolean cleared;
        private final String reason;

        ScreeningResult(boolean cleared, String reason) {
            this.cleared = cleared;
            this.reason = reason;
        }

        public boolean isCleared() { return cleared; }
        public String getReason() { return reason; }
    }
}
