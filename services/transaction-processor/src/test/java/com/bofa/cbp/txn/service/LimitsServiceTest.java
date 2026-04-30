package com.bofa.cbp.txn.service;

import com.bofa.cbp.txn.domain.TransactionRequest;
import com.bofa.cbp.txn.domain.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LimitsServiceTest {

    private LimitsService limits;

    @BeforeEach
    void setUp() {
        limits = new LimitsService();
    }

    private TransactionRequest buildRequest(String accountId, TransactionType type, BigDecimal amount) {
        TransactionRequest req = new TransactionRequest();
        req.setAccountId(accountId);
        req.setCounterpartyAccountId("ACC-9999");
        req.setType(type);
        req.setAmount(amount);
        req.setCurrency("USD");
        return req;
    }

    @Test
    void creditAlwaysAllowed() {
        TransactionRequest req = buildRequest("ACC-0001", TransactionType.CREDIT, new BigDecimal("999999.99"));

        LimitsService.LimitDecision decision = limits.check(req);

        assertTrue(decision.allowed());
        assertNull(decision.reason());
    }

    @Test
    void debitWithinPerTxnLimit() {
        TransactionRequest req = buildRequest("ACC-0001", TransactionType.DEBIT, new BigDecimal("24999.00"));

        LimitsService.LimitDecision decision = limits.check(req);

        assertTrue(decision.allowed());
    }

    @Test
    void debitExceedsPerTxnLimit() {
        TransactionRequest req = buildRequest("ACC-0001", TransactionType.DEBIT, new BigDecimal("25001.00"));

        LimitsService.LimitDecision decision = limits.check(req);

        assertFalse(decision.allowed());
        assertEquals("per-transaction-limit", decision.reason());
    }

    @Test
    void debitExceedsDailyLimit() {
        String account = "ACC-DAILY-TEST";
        for (int i = 0; i < 4; i++) {
            TransactionRequest req = buildRequest(account, TransactionType.DEBIT, new BigDecimal("12000.00"));
            limits.record(req);
        }

        TransactionRequest pushOver = buildRequest(account, TransactionType.DEBIT, new BigDecimal("3000.00"));
        LimitsService.LimitDecision decision = limits.check(pushOver);

        assertFalse(decision.allowed());
        assertEquals("daily-limit", decision.reason());
    }

    @Test
    void customPerTxnOverride() {
        String account = "ACC-CUSTOM-PTX";
        limits.setPerTxnLimit(account, new BigDecimal("5000.00"));

        TransactionRequest under = buildRequest(account, TransactionType.DEBIT, new BigDecimal("4999.00"));
        assertTrue(limits.check(under).allowed());

        TransactionRequest over = buildRequest(account, TransactionType.DEBIT, new BigDecimal("5001.00"));
        LimitsService.LimitDecision decision = limits.check(over);
        assertFalse(decision.allowed());
        assertEquals("per-transaction-limit", decision.reason());
    }

    @Test
    void customDailyOverride() {
        String account = "ACC-CUSTOM-DAILY";
        limits.setDailyLimit(account, new BigDecimal("10000.00"));

        TransactionRequest first = buildRequest(account, TransactionType.DEBIT, new BigDecimal("8000.00"));
        limits.record(first);

        TransactionRequest pushOver = buildRequest(account, TransactionType.DEBIT, new BigDecimal("3000.00"));
        LimitsService.LimitDecision decision = limits.check(pushOver);

        assertFalse(decision.allowed());
        assertEquals("daily-limit", decision.reason());
    }

    @Test
    void recordIgnoresCredits() {
        String account = "ACC-CREDIT-IGNORE";
        TransactionRequest credit = buildRequest(account, TransactionType.CREDIT, new BigDecimal("49000.00"));
        limits.record(credit);

        TransactionRequest debit = buildRequest(account, TransactionType.DEBIT, new BigDecimal("24000.00"));
        LimitsService.LimitDecision decision = limits.check(debit);

        assertTrue(decision.allowed());
    }
}
