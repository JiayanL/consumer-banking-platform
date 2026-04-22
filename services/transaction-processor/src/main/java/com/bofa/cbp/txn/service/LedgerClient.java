package com.bofa.cbp.txn.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stub client for ledger-service. In production this talks over HTTP.
 * For local testing and the self-contained demo checkout, balances live
 * in an in-process map seeded with a few accounts.
 */
@Component
public class LedgerClient {

    private final Map<String, BigDecimal> balances = new ConcurrentHashMap<>(Map.of(
            "ACC-0001", new BigDecimal("10000.00"),
            "ACC-0002", new BigDecimal("2500.00"),
            "ACC-0003", new BigDecimal("50.00"),
            "ACC-9999", new BigDecimal("100000.00")
    ));

    public BigDecimal getBalance(String accountId) {
        return balances.getOrDefault(accountId, BigDecimal.ZERO);
    }

    public void debit(String accountId, BigDecimal amount) {
        BigDecimal current = balances.getOrDefault(accountId, BigDecimal.ZERO);
        balances.put(accountId, current.subtract(amount));
    }

    public void credit(String accountId, BigDecimal amount) {
        BigDecimal current = balances.getOrDefault(accountId, BigDecimal.ZERO);
        balances.put(accountId, current.add(amount));
    }
}
