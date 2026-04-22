package com.bofa.cbp.ledger.service;

import java.math.BigDecimal;

public class UnbalancedJournalException extends RuntimeException {
    public UnbalancedJournalException(String txnId, BigDecimal debits, BigDecimal credits) {
        super("journal " + txnId + " unbalanced: debits=" + debits + " credits=" + credits);
    }
}
