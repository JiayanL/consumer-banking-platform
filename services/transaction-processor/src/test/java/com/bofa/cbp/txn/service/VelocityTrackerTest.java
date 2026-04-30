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

    @Test
    void emptyTrackerReturnsZero() {
        assertEquals(0, tracker.countWithinWindow("ACC-1"));
        assertEquals(BigDecimal.ZERO, tracker.amountWithinWindow("ACC-1"));
    }

    @Test
    void recordIncreasesCount() {
        tracker.record(buildReq("ACC-1", new BigDecimal("100.00")));
        assertEquals(1, tracker.countWithinWindow("ACC-1"));
    }

    @Test
    void amountTrackedCorrectly() {
        tracker.record(buildReq("ACC-1", new BigDecimal("100.00")));
        tracker.record(buildReq("ACC-1", new BigDecimal("200.00")));
        assertEquals(new BigDecimal("300.00"), tracker.amountWithinWindow("ACC-1"));
    }

    @Test
    void isHotAccountFalseForLowVolume() {
        tracker.record(buildReq("ACC-1", new BigDecimal("10.00")));
        assertFalse(tracker.isHotAccount("ACC-1"));
    }

    @Test
    void isHotAccountTrueWhenLimitReached() {
        for (int i = 0; i < 20; i++) {
            tracker.record(buildReq("ACC-HOT", new BigDecimal("1.00")));
        }
        assertTrue(tracker.isHotAccount("ACC-HOT"));
    }

    @Test
    void differentAccountsTrackedSeparately() {
        tracker.record(buildReq("ACC-1", new BigDecimal("100.00")));
        tracker.record(buildReq("ACC-2", new BigDecimal("200.00")));
        assertEquals(1, tracker.countWithinWindow("ACC-1"));
        assertEquals(1, tracker.countWithinWindow("ACC-2"));
    }

    private TransactionRequest buildReq(String account, BigDecimal amount) {
        TransactionRequest req = new TransactionRequest();
        req.setAccountId(account);
        req.setCounterpartyAccountId("ACC-9999");
        req.setType(TransactionType.DEBIT);
        req.setAmount(amount);
        req.setCurrency("USD");
        return req;
    }
}
