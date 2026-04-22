package com.bofa.cbp.wire.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "wire_transfers")
public class WireTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String senderAccount;

    @Column(nullable = false)
    private String beneficiaryName;

    @Column(nullable = false)
    private String beneficiaryAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WireStatus status;

    @Column(nullable = false, unique = true)
    private String referenceNumber;

    @Column(nullable = false)
    private Instant initiatedAt;

    public WireTransfer() {}

    public Long getId() { return id; }
    public String getSenderAccount() { return senderAccount; }
    public String getBeneficiaryName() { return beneficiaryName; }
    public String getBeneficiaryAccount() { return beneficiaryAccount; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public WireStatus getStatus() { return status; }
    public String getReferenceNumber() { return referenceNumber; }
    public Instant getInitiatedAt() { return initiatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setSenderAccount(String v) { this.senderAccount = v; }
    public void setBeneficiaryName(String v) { this.beneficiaryName = v; }
    public void setBeneficiaryAccount(String v) { this.beneficiaryAccount = v; }
    public void setAmount(BigDecimal v) { this.amount = v; }
    public void setCurrency(String v) { this.currency = v; }
    public void setStatus(WireStatus v) { this.status = v; }
    public void setReferenceNumber(String v) { this.referenceNumber = v; }
    public void setInitiatedAt(Instant v) { this.initiatedAt = v; }
}
