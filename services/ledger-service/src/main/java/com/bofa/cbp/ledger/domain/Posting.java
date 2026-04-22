package com.bofa.cbp.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "postings")
public class Posting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal debit;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal credit;

    @Column(nullable = false)
    private String txnId;

    @Column(nullable = false)
    private Instant timestamp;

    public Posting() {}

    public Posting(String accountId, BigDecimal debit, BigDecimal credit, String txnId, Instant timestamp) {
        this.accountId = accountId;
        this.debit = debit == null ? BigDecimal.ZERO : debit;
        this.credit = credit == null ? BigDecimal.ZERO : credit;
        this.txnId = txnId;
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public String getAccountId() { return accountId; }
    public BigDecimal getDebit() { return debit; }
    public BigDecimal getCredit() { return credit; }
    public String getTxnId() { return txnId; }
    public Instant getTimestamp() { return timestamp; }

    public void setId(Long id) { this.id = id; }
    public void setAccountId(String accountId) { this.accountId = accountId; }
    public void setDebit(BigDecimal debit) { this.debit = debit; }
    public void setCredit(BigDecimal credit) { this.credit = credit; }
    public void setTxnId(String txnId) { this.txnId = txnId; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
