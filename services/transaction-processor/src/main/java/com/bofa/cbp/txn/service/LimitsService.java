package com.bofa.cbp.txn.service;

import com.bofa.cbp.txn.domain.TransactionRequest;
import com.bofa.cbp.txn.domain.TransactionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces per-account daily and per-transaction limits.
 * Daily windows reset at midnight UTC.
 */
@Service
public class LimitsService {

    private static final BigDecimal DEFAULT_DAILY = new BigDecimal("50000.00");
    private static final BigDecimal DEFAULT_PER_TXN = new BigDecimal("25000.00");

    private final Map<String, AccountWindow> windows = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> dailyOverrides = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> perTxnOverrides = new ConcurrentHashMap<>();

    public LimitDecision check(TransactionRequest req) {
        if (req.getType() == TransactionType.CREDIT) {
            return LimitDecision.ok();
        }
        BigDecimal perTxn = perTxnOverrides.getOrDefault(req.getAccountId(), DEFAULT_PER_TXN);
        if (req.getAmount().compareTo(perTxn) > 0) {
            return LimitDecision.reject("per-transaction-limit");
        }

        BigDecimal daily = dailyOverrides.getOrDefault(req.getAccountId(), DEFAULT_DAILY);
        AccountWindow w = windows.computeIfAbsent(req.getAccountId(), k -> new AccountWindow());
        if (w.totalToday().add(req.getAmount()).compareTo(daily) > 0) {
            return LimitDecision.reject("daily-limit");
        }
        return LimitDecision.ok();
    }

    public void record(TransactionRequest req) {
        if (req.getType() == TransactionType.CREDIT) return;
        windows.computeIfAbsent(req.getAccountId(), k -> new AccountWindow()).add(req.getAmount());
    }

    public void setDailyLimit(String accountId, BigDecimal amount) {
        dailyOverrides.put(accountId, amount);
    }

    public void setPerTxnLimit(String accountId, BigDecimal amount) {
        perTxnOverrides.put(accountId, amount);
    }

    public record LimitDecision(boolean allowed, String reason) {
        public static LimitDecision ok() { return new LimitDecision(true, null); }
        public static LimitDecision reject(String r) { return new LimitDecision(false, r); }
    }

    private static final class AccountWindow {
        private Instant windowStart = Instant.now().truncatedTo(ChronoUnit.DAYS);
        private BigDecimal total = BigDecimal.ZERO;

        synchronized BigDecimal totalToday() {
            rollIfNeeded();
            return total;
        }

        synchronized void add(BigDecimal amount) {
            rollIfNeeded();
            total = total.add(amount);
        }

        private void rollIfNeeded() {
            Instant startOfToday = Instant.now().truncatedTo(ChronoUnit.DAYS);
            if (startOfToday.isAfter(windowStart)) {
                windowStart = startOfToday;
                total = BigDecimal.ZERO;
            }
        }
    }
}
