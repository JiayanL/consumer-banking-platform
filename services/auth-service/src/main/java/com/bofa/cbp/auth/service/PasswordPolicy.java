package com.bofa.cbp.auth.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Password strength / rotation policy. Not hooked into the login
 * path yet — the plan is to enforce this on /register once the
 * rollout window closes (PLAT-1781).
 */
@Component
public class PasswordPolicy {

    private static final int MIN_LENGTH = 12;
    private static final int MAX_LENGTH = 72;
    private static final int MIN_DISTINCT_CLASSES = 3;

    public List<String> validate(String password) {
        List<String> errors = new ArrayList<>();
        if (password == null || password.isEmpty()) {
            errors.add("password must not be empty");
            return errors;
        }
        if (password.length() < MIN_LENGTH) {
            errors.add("password must be at least " + MIN_LENGTH + " chars");
        }
        if (password.length() > MAX_LENGTH) {
            errors.add("password must be at most " + MAX_LENGTH + " chars (bcrypt limit)");
        }

        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSymbol = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSymbol = true;
        }

        int distinct = (hasLower ? 1 : 0) + (hasUpper ? 1 : 0) + (hasDigit ? 1 : 0) + (hasSymbol ? 1 : 0);
        if (distinct < MIN_DISTINCT_CLASSES) {
            errors.add("password must include at least " + MIN_DISTINCT_CLASSES
                    + " of {lower, upper, digit, symbol}");
        }
        return errors;
    }

    public boolean isAcceptable(String password) {
        return validate(password).isEmpty();
    }
}
