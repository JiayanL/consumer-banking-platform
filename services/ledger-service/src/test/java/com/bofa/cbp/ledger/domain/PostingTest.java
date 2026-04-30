package com.bofa.cbp.ledger.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PostingTest {

    @Test
    void noArgConstructor_createsInstanceWithNullFields() {
        Posting posting = new Posting();

        assertThat(posting.getId()).isNull();
        assertThat(posting.getAccountId()).isNull();
        assertThat(posting.getDebit()).isNull();
        assertThat(posting.getCredit()).isNull();
        assertThat(posting.getTxnId()).isNull();
        assertThat(posting.getTimestamp()).isNull();
    }

    @Test
    void fullConstructor_setsAllFields() {
        Instant now = Instant.now();
        Posting posting = new Posting("acct-A", new BigDecimal("100.00"), new BigDecimal("50.00"), "txn-1", now);

        assertThat(posting.getAccountId()).isEqualTo("acct-A");
        assertThat(posting.getDebit()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(posting.getCredit()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(posting.getTxnId()).isEqualTo("txn-1");
        assertThat(posting.getTimestamp()).isEqualTo(now);
    }

    @Test
    void constructor_nullDebitDefaultsToZero() {
        Posting posting = new Posting("acct-A", null, new BigDecimal("10.00"), "txn-1", Instant.now());

        assertThat(posting.getDebit()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void constructor_nullCreditDefaultsToZero() {
        Posting posting = new Posting("acct-A", new BigDecimal("10.00"), null, "txn-1", Instant.now());

        assertThat(posting.getCredit()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void setters_updateFields() {
        Posting posting = new Posting();
        Instant now = Instant.now();

        posting.setId(42L);
        posting.setAccountId("acct-B");
        posting.setDebit(new BigDecimal("200.00"));
        posting.setCredit(new BigDecimal("300.00"));
        posting.setTxnId("txn-99");
        posting.setTimestamp(now);

        assertThat(posting.getId()).isEqualTo(42L);
        assertThat(posting.getAccountId()).isEqualTo("acct-B");
        assertThat(posting.getDebit()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(posting.getCredit()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(posting.getTxnId()).isEqualTo("txn-99");
        assertThat(posting.getTimestamp()).isEqualTo(now);
    }
}
