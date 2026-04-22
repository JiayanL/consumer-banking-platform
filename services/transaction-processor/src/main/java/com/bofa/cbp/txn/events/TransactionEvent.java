package com.bofa.cbp.txn.events;

import com.bofa.cbp.txn.domain.TransactionStatus;
import com.bofa.cbp.txn.domain.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
public class TransactionEvent {
    private final Long transactionId;
    private final String accountId;
    private final TransactionType type;
    private final BigDecimal amount;
    private final TransactionStatus status;
    private final Instant occurredAt;
}
