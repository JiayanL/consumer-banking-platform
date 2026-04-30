package com.bofa.cbp.txn.service;

import com.bofa.cbp.txn.domain.TransactionRequest;
import com.bofa.cbp.txn.domain.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class FraudHookTest {

    private final FraudHook hook = new FraudHook();

    private TransactionRequest buildRequest(String accountId, String counterpartyId, BigDecimal amount) {
        TransactionRequest req = new TransactionRequest();
        req.setAccountId(accountId);
        req.setCounterpartyAccountId(counterpartyId);
        req.setType(TransactionType.DEBIT);
        req.setAmount(amount);
        req.setCurrency("USD");
        return req;
    }

    @Test
    void okForNormalTransaction() {
        TransactionRequest req = buildRequest("ACC-0001", "ACC-0002", new BigDecimal("500.00"));

        FraudHook.FraudDecision decision = hook.evaluate(req);

        assertEquals("OK", decision.verdict());
        assertNull(decision.reason());
    }

    @Test
    void blocksBlocklistedAccount() {
        TransactionRequest req = buildRequest("ACC-BLOCKED-1", "ACC-0002", new BigDecimal("100.00"));

        FraudHook.FraudDecision decision = hook.evaluate(req);

        assertEquals("BLOCK", decision.verdict());
        assertEquals("account-blocklisted", decision.reason());
    }

    @Test
    void blocksBlocklistedCounterparty() {
        TransactionRequest req = buildRequest("ACC-0001", "ACC-BLOCKED-1", new BigDecimal("100.00"));

        FraudHook.FraudDecision decision = hook.evaluate(req);

        assertEquals("BLOCK", decision.verdict());
        assertEquals("counterparty-blocklisted", decision.reason());
    }

    @Test
    void reviewForLargeAmount() {
        TransactionRequest req = buildRequest("ACC-0001", "ACC-0002", new BigDecimal("250001.00"));

        FraudHook.FraudDecision decision = hook.evaluate(req);

        assertEquals("REVIEW", decision.verdict());
        assertEquals("amount-above-review-threshold", decision.reason());
    }

    @Test
    void okForAmountAtThreshold() {
        TransactionRequest req = buildRequest("ACC-0001", "ACC-0002", new BigDecimal("250000.00"));

        FraudHook.FraudDecision decision = hook.evaluate(req);

        assertEquals("OK", decision.verdict());
    }

    @Test
    void isBlockingAndRequiresReviewHelpers() {
        FraudHook.FraudDecision ok = FraudHook.FraudDecision.ok();
        assertFalse(ok.isBlocking());
        assertFalse(ok.requiresReview());

        FraudHook.FraudDecision block = FraudHook.FraudDecision.block("reason");
        assertTrue(block.isBlocking());
        assertFalse(block.requiresReview());

        FraudHook.FraudDecision review = FraudHook.FraudDecision.review("reason");
        assertFalse(review.isBlocking());
        assertTrue(review.requiresReview());
    }
}
