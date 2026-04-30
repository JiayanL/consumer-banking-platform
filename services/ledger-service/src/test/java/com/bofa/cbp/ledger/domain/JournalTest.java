package com.bofa.cbp.ledger.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JournalTest {

    @Test
    void totalDebits_sumsDebitAmountsAcrossPostings() {
        Posting p1 = new Posting("acct-A", new BigDecimal("100.00"), BigDecimal.ZERO, "txn-1", Instant.now());
        Posting p2 = new Posting("acct-B", new BigDecimal("200.00"), BigDecimal.ZERO, "txn-1", Instant.now());
        Journal journal = new Journal("txn-1", List.of(p1, p2));

        assertThat(journal.totalDebits()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    void totalCredits_sumsCreditAmountsAcrossPostings() {
        Posting p1 = new Posting("acct-A", BigDecimal.ZERO, new BigDecimal("150.00"), "txn-1", Instant.now());
        Posting p2 = new Posting("acct-B", BigDecimal.ZERO, new BigDecimal("350.00"), "txn-1", Instant.now());
        Journal journal = new Journal("txn-1", List.of(p1, p2));

        assertThat(journal.totalCredits()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void totalDebits_handlesNullDebitValues() {
        Posting withNull = new Posting("acct-A", null, BigDecimal.ZERO, "txn-1", Instant.now());
        Posting withValue = new Posting("acct-B", new BigDecimal("50.00"), BigDecimal.ZERO, "txn-1", Instant.now());
        Journal journal = new Journal("txn-1", List.of(withNull, withValue));

        assertThat(journal.totalDebits()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void totalCredits_handlesNullCreditValues() {
        Posting withNull = new Posting("acct-A", BigDecimal.ZERO, null, "txn-1", Instant.now());
        Posting withValue = new Posting("acct-B", BigDecimal.ZERO, new BigDecimal("75.00"), "txn-1", Instant.now());
        Journal journal = new Journal("txn-1", List.of(withNull, withValue));

        assertThat(journal.totalCredits()).isEqualByComparingTo(new BigDecimal("75.00"));
    }

    @Test
    void constructorWithNullPostings_createsEmptyList() {
        Journal journal = new Journal("txn-null", null);

        assertThat(journal.getPostings()).isEmpty();
        assertThat(journal.totalDebits()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(journal.totalCredits()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getPostings_returnsUnmodifiableList() {
        Posting p = new Posting("acct-A", BigDecimal.ZERO, BigDecimal.ZERO, "txn-1", Instant.now());
        Journal journal = new Journal("txn-1", List.of(p));

        assertThatThrownBy(() -> journal.getPostings().add(
                new Posting("acct-B", BigDecimal.ZERO, BigDecimal.ZERO, "txn-1", Instant.now())
        )).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getTxnId_returnsTransactionId() {
        Journal journal = new Journal("txn-abc", List.of());

        assertThat(journal.getTxnId()).isEqualTo("txn-abc");
    }

    @Test
    void balancedJournal_debitsEqualCredits() {
        Posting debit = new Posting("acct-A", new BigDecimal("500.00"), BigDecimal.ZERO, "txn-1", Instant.now());
        Posting credit = new Posting("acct-B", BigDecimal.ZERO, new BigDecimal("500.00"), "txn-1", Instant.now());
        Journal journal = new Journal("txn-1", List.of(debit, credit));

        assertThat(journal.totalDebits()).isEqualByComparingTo(journal.totalCredits());
    }
}
