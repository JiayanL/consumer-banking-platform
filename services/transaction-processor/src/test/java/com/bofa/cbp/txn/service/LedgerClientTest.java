package com.bofa.cbp.txn.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class LedgerClientTest {

    private LedgerClient ledger;

    @BeforeEach
    void setUp() {
        ledger = new LedgerClient();
    }

    @Test
    void getBalanceForSeededAccount() {
        assertEquals(0, new BigDecimal("10000.00").compareTo(ledger.getBalance("ACC-0001")));
    }

    @Test
    void getBalanceForUnknownAccount() {
        assertEquals(0, BigDecimal.ZERO.compareTo(ledger.getBalance("ACC-NONEXISTENT")));
    }

    @Test
    void debitReducesBalance() {
        ledger.debit("ACC-0001", new BigDecimal("100.00"));
        assertEquals(0, new BigDecimal("9900.00").compareTo(ledger.getBalance("ACC-0001")));
    }

    @Test
    void creditIncreasesBalance() {
        ledger.credit("ACC-0001", new BigDecimal("500.00"));
        assertEquals(0, new BigDecimal("10500.00").compareTo(ledger.getBalance("ACC-0001")));
    }
}
