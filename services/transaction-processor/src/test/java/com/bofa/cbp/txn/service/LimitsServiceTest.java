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

    @Test
    void withinLimitsAllowed() {
        TransactionRequest req = debitReq("ACC-1", new BigDecimal("1000.00"));
        LimitsService.LimitDecision d = limits.check(req);
        assertTrue(d.allowed());
    }

    @Test
    void perTransactionLimitExceeded() {
        TransactionRequest req = debitReq("ACC-1", new BigDecimal("30000.00"));
        LimitsService.LimitDecision d = limits.check(req);
        assertFalse(d.allowed());
        assertEquals("per-transaction-limit", d.reason());
    }

    @Test
    void dailyLimitExceeded() {
        limits.setDailyLimit("ACC-1", new BigDecimal("500.00"));
        limits.record(debitReq("ACC-1", new BigDecimal("400.00")));

        LimitsService.LimitDecision d = limits.check(debitReq("ACC-1", new BigDecimal("200.00")));
        assertFalse(d.allowed());
        assertEquals("daily-limit", d.reason());
    }

    @Test
    void creditsBypassLimits() {
        TransactionRequest req = new TransactionRequest();
        req.setAccountId("ACC-1");
        req.setCounterpartyAccountId("ACC-2");
        req.setType(TransactionType.CREDIT);
        req.setAmount(new BigDecimal("999999.00"));
        req.setCurrency("USD");

        assertTrue(limits.check(req).allowed());
    }

    @Test
    void customPerTxnLimitOverridesDefault() {
        limits.setPerTxnLimit("ACC-1", new BigDecimal("100.00"));
        TransactionRequest req = debitReq("ACC-1", new BigDecimal("150.00"));
        assertFalse(limits.check(req).allowed());
    }

    private TransactionRequest debitReq(String account, BigDecimal amount) {
        TransactionRequest req = new TransactionRequest();
        req.setAccountId(account);
        req.setCounterpartyAccountId("ACC-9999");
        req.setType(TransactionType.DEBIT);
        req.setAmount(amount);
        req.setCurrency("USD");
        return req;
    }
}
