package com.bofa.cbp.ledger.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Groups one or more {@link Posting} rows into a single balanced
 * double-entry transaction. A journal is valid iff the sum of debits
 * equals the sum of credits across its postings.
 */
public class Journal {

    private final String txnId;
    private final List<Posting> postings;

    public Journal(String txnId, List<Posting> postings) {
        this.txnId = txnId;
        this.postings = postings == null ? new ArrayList<>() : new ArrayList<>(postings);
    }

    public String getTxnId() {
        return txnId;
    }

    public List<Posting> getPostings() {
        return Collections.unmodifiableList(postings);
    }

    public BigDecimal totalDebits() {
        return postings.stream()
                .map(p -> p.getDebit() == null ? BigDecimal.ZERO : p.getDebit())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalCredits() {
        return postings.stream()
                .map(p -> p.getCredit() == null ? BigDecimal.ZERO : p.getCredit())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
