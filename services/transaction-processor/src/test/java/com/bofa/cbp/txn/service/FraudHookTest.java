package com.bofa.cbp.txn.service;

import com.bofa.cbp.txn.domain.TransactionRequest;
import com.bofa.cbp.txn.domain.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class FraudHookTest {

    private final FraudHook hook = new FraudHook();

    @Test
    void normalTransactionPassesFraud() {
        TransactionRequest req = buildReq("ACC-0001", "ACC-0002", new BigDecimal("100.00"));
        FraudHook.FraudDecision d = hook.evaluate(req);
        assertEquals("OK", d.verdict());
        assertFalse(d.isBlocking());
        assertFalse(d.requiresReview());
    }

    @Test
    void blockedAccountIsBlocked() {
        TransactionRequest req = buildReq("ACC-BLOCKED-1", "ACC-0002", new BigDecimal("100.00"));
        FraudHook.FraudDecision d = hook.evaluate(req);
        assertTrue(d.isBlocking());
        assertEquals("account-blocklisted", d.reason());
    }

    @Test
    void blockedCounterpartyIsBlocked() {
        TransactionRequest req = buildReq("ACC-0001", "ACC-BLOCKED-1", new BigDecimal("100.00"));
        FraudHook.FraudDecision d = hook.evaluate(req);
        assertTrue(d.isBlocking());
        assertEquals("counterparty-blocklisted", d.reason());
    }

    @Test
    void largeAmountFlaggedForReview() {
        TransactionRequest req = buildReq("ACC-0001", "ACC-0002", new BigDecimal("300000.00"));
        FraudHook.FraudDecision d = hook.evaluate(req);
        assertTrue(d.requiresReview());
        assertEquals("amount-above-review-threshold", d.reason());
    }

    @Test
    void amountAtExactLimitPasses() {
        TransactionRequest req = buildReq("ACC-0001", "ACC-0002", new BigDecimal("250000.00"));
        FraudHook.FraudDecision d = hook.evaluate(req);
        assertEquals("OK", d.verdict());
    }

    private TransactionRequest buildReq(String account, String counterparty, BigDecimal amount) {
        TransactionRequest req = new TransactionRequest();
        req.setAccountId(account);
        req.setCounterpartyAccountId(counterparty);
        req.setType(TransactionType.DEBIT);
        req.setAmount(amount);
        req.setCurrency("USD");
        return req;
    }
}
