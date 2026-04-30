package com.bofa.cbp.txn.service;

import com.bofa.cbp.txn.domain.TransactionRequest;
import com.bofa.cbp.txn.domain.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class VelocityTrackerTest {

    private VelocityTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new VelocityTracker();
    }

    private TransactionRequest buildRequest(String accountId, BigDecimal amount) {
        TransactionRequest req = new TransactionRequest();
        req.setAccountId(accountId);
        req.setCounterpartyAccountId("ACC-9999");
        req.setType(TransactionType.DEBIT);
        req.setAmount(amount);
        req.setCurrency("USD");
        return req;
    }

    @Test
    void countZeroForUnknownAccount() {
        assertEquals(0, tracker.countWithinWindow("ACC-UNKNOWN"));
    }

    @Test
    void amountZeroForUnknownAccount() {
        assertEquals(0, BigDecimal.ZERO.compareTo(tracker.amountWithinWindow("ACC-UNKNOWN")));
    }

    @Test
    void recordAndCount() {
        String account = "ACC-COUNT";
        tracker.record(buildRequest(account, new BigDecimal("100.00")));
        tracker.record(buildRequest(account, new BigDecimal("200.00")));
        tracker.record(buildRequest(account, new BigDecimal("300.00")));

        assertEquals(3, tracker.countWithinWindow(account));
    }

    @Test
    void recordAndAmount() {
        String account = "ACC-AMOUNT";
        tracker.record(buildRequest(account, new BigDecimal("100.00")));
        tracker.record(buildRequest(account, new BigDecimal("250.50")));
        tracker.record(buildRequest(account, new BigDecimal("49.50")));

        assertEquals(0, new BigDecimal("400.00").compareTo(tracker.amountWithinWindow(account)));
    }

    @Test
    void isHotAccountBelowThreshold() {
        String account = "ACC-NOT-HOT";
        for (int i = 0; i < 19; i++) {
            tracker.record(buildRequest(account, new BigDecimal("10.00")));
        }
        assertFalse(tracker.isHotAccount(account));
    }

    @Test
    void isHotAccountAtThreshold() {
        String account = "ACC-HOT";
        for (int i = 0; i < 20; i++) {
            tracker.record(buildRequest(account, new BigDecimal("10.00")));
        }
        assertTrue(tracker.isHotAccount(account));
    }
}
