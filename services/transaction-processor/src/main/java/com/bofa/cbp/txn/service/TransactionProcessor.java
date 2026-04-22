package com.bofa.cbp.txn.service;

import com.bofa.cbp.auth.compliance.ComplianceCategory;
import com.bofa.cbp.auth.compliance.ComplianceCritical;
import com.bofa.cbp.txn.domain.*;
import com.bofa.cbp.txn.events.EventEmitter;
import com.bofa.cbp.txn.events.TransactionEvent;
import com.bofa.cbp.txn.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * Core transaction processing. This service sits on the primary money
 * movement path and is the most heavily scrutinized service by
 * compliance during exam prep.
 *
 * Flow: validate -> idempotency check -> balance check -> debit ->
 *       persist -> emit event.
 */
@Service
public class TransactionProcessor {

    private static final Logger log = LoggerFactory.getLogger(TransactionProcessor.class);

    private final TransactionRepository transactions;
    private final LedgerClient ledger;
    private final EventEmitter events;

    public TransactionProcessor(TransactionRepository transactions,
                                LedgerClient ledger,
                                EventEmitter events) {
        this.transactions = transactions;
        this.ledger = ledger;
        this.events = events;
    }

    @ComplianceCritical(
        category = ComplianceCategory.TRANSACTION_INTEGRITY,
        note = "Primary transaction accept path. Any change here must go through payments-team review."
    )
    public TransactionResult process(TransactionRequest req) {
        if (req.getIdempotencyKey() != null) {
            Optional<Transaction> prior = transactions.findFirstByIdempotencyKey(req.getIdempotencyKey());
            if (prior.isPresent()) {
                Transaction p = prior.get();
                return new TransactionResult(p.getId(), p.getStatus(), "idempotent-replay");
            }
        }

        // Validate basics the bean-validation framework doesn't cover.
        if (req.getAmount().scale() > 2) {
            return TransactionResult.failed("amount exceeds 2 decimal places");
        }
        if (!"USD".equals(req.getCurrency())) {
            return TransactionResult.failed("only USD is supported in this build");
        }

        // Balance check + debit happen as separate logical steps against
        // the in-process ledger client. See TransactionProcessor notes
        // in docs/architecture.md for the sequencing contract.
        BigDecimal balance = ledger.getBalance(req.getAccountId());
        if (req.getType() == TransactionType.DEBIT || req.getType() == TransactionType.TRANSFER) {
            if (balance.compareTo(req.getAmount()) < 0) {
                Transaction saved = persist(req, TransactionStatus.REJECTED, "INSUFFICIENT_FUNDS");
                events.emit(toEvent(saved));
                return TransactionResult.rejected(saved.getId(), "INSUFFICIENT_FUNDS");
            }
            ledger.debit(req.getAccountId(), req.getAmount());
            if (req.getType() == TransactionType.TRANSFER) {
                ledger.credit(req.getCounterpartyAccountId(), req.getAmount());
            }
        } else {
            ledger.credit(req.getAccountId(), req.getAmount());
        }

        Transaction saved = persist(req, TransactionStatus.ACCEPTED, null);
        events.emit(toEvent(saved));
        log.info("transaction accepted id={} account={} amount={}",
                saved.getId(), req.getAccountId(), req.getAmount());
        return TransactionResult.accepted(saved.getId());
    }

    private Transaction persist(TransactionRequest req, TransactionStatus status, String reason) {
        Transaction t = Transaction.builder()
                .accountId(req.getAccountId())
                .counterpartyAccountId(req.getCounterpartyAccountId())
                .type(req.getType())
                .amount(req.getAmount())
                .currency(req.getCurrency())
                .status(status)
                .idempotencyKey(req.getIdempotencyKey())
                .createdAt(Instant.now())
                .rejectionReason(reason)
                .build();
        return transactions.save(t);
    }

    private TransactionEvent toEvent(Transaction t) {
        return new TransactionEvent(
                t.getId(),
                t.getAccountId(),
                t.getType(),
                t.getAmount(),
                t.getStatus(),
                t.getCreatedAt()
        );
    }

    public Optional<Transaction> findById(Long id) {
        return transactions.findById(id);
    }
}
