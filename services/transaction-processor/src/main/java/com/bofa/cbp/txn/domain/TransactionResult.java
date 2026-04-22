package com.bofa.cbp.txn.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TransactionResult {
    private final Long transactionId;
    private final TransactionStatus status;
    private final String reason;

    public static TransactionResult accepted(Long id) {
        return new TransactionResult(id, TransactionStatus.ACCEPTED, null);
    }

    public static TransactionResult rejected(Long id, String reason) {
        return new TransactionResult(id, TransactionStatus.REJECTED, reason);
    }

    public static TransactionResult failed(String reason) {
        return new TransactionResult(null, TransactionStatus.FAILED, reason);
    }
}
