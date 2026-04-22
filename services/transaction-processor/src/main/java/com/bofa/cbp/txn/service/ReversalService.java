package com.bofa.cbp.txn.service;

import com.bofa.cbp.auth.compliance.ComplianceCategory;
import com.bofa.cbp.auth.compliance.ComplianceCritical;
import com.bofa.cbp.txn.domain.Transaction;
import com.bofa.cbp.txn.domain.TransactionStatus;
import com.bofa.cbp.txn.domain.TransactionType;
import com.bofa.cbp.txn.events.EventEmitter;
import com.bofa.cbp.txn.events.TransactionEvent;
import com.bofa.cbp.txn.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Handles reversals and refunds of accepted transactions. Reversals
 * require original transaction to be in ACCEPTED state and must happen
 * within the reversal window (24h for debits, 90d for credits).
 */
@Service
public class ReversalService {

    private static final long DEBIT_REVERSAL_WINDOW_SECS = 24 * 60 * 60L;
    private static final long CREDIT_REVERSAL_WINDOW_SECS = 90 * 24 * 60 * 60L;

    private final TransactionRepository transactions;
    private final LedgerClient ledger;
    private final EventEmitter events;

    public ReversalService(TransactionRepository transactions, LedgerClient ledger, EventEmitter events) {
        this.transactions = transactions;
        this.ledger = ledger;
        this.events = events;
    }

    @ComplianceCritical(category = ComplianceCategory.TRANSACTION_INTEGRITY)
    public ReversalResult reverse(Long transactionId, String reason) {
        Transaction original = transactions.findById(transactionId)
                .orElse(null);
        if (original == null) return ReversalResult.notFound();
        if (original.getStatus() != TransactionStatus.ACCEPTED) {
            return ReversalResult.ineligible("not-accepted");
        }
        long age = Instant.now().getEpochSecond() - original.getCreatedAt().getEpochSecond();
        long window = original.getType() == TransactionType.CREDIT
                ? CREDIT_REVERSAL_WINDOW_SECS
                : DEBIT_REVERSAL_WINDOW_SECS;
        if (age > window) {
            return ReversalResult.ineligible("outside-window");
        }

        if (original.getType() == TransactionType.DEBIT || original.getType() == TransactionType.TRANSFER) {
            ledger.credit(original.getAccountId(), original.getAmount());
            if (original.getType() == TransactionType.TRANSFER) {
                ledger.debit(original.getCounterpartyAccountId(), original.getAmount());
            }
        } else {
            ledger.debit(original.getAccountId(), original.getAmount());
        }

        original.setStatus(TransactionStatus.REJECTED);
        original.setRejectionReason("REVERSED: " + reason);
        transactions.save(original);

        events.emit(new TransactionEvent(
                original.getId(),
                original.getAccountId(),
                original.getType(),
                original.getAmount().negate(),
                TransactionStatus.REJECTED,
                Instant.now()
        ));
        return ReversalResult.ok();
    }

    public record ReversalResult(String status, String reason) {
        public static ReversalResult ok() { return new ReversalResult("OK", null); }
        public static ReversalResult notFound() { return new ReversalResult("NOT_FOUND", null); }
        public static ReversalResult ineligible(String reason) { return new ReversalResult("INELIGIBLE", reason); }
    }
}
