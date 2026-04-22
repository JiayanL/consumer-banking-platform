package com.bofa.cbp.ledger.service;

import com.bofa.cbp.auth.compliance.ComplianceCategory;
import com.bofa.cbp.auth.compliance.ComplianceCritical;
import com.bofa.cbp.ledger.domain.Journal;
import com.bofa.cbp.ledger.domain.Posting;
import com.bofa.cbp.ledger.domain.PostingRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Core ledger logic. Uses constructor injection (contrast with
 * {@link com.bofa.cbp.ledger.web.LedgerController} which uses field
 * injection).
 */
@Service
public class LedgerService {

    private final PostingRepository repository;

    public LedgerService(PostingRepository repository) {
        this.repository = repository;
    }

    @ComplianceCritical(
        category = ComplianceCategory.TRANSACTION_INTEGRITY,
        note = "Enforces debits == credits for every journal. Double-entry invariant."
    )
    public List<Posting> submitJournal(Journal journal) {
        BigDecimal debits = journal.totalDebits();
        BigDecimal credits = journal.totalCredits();
        if (debits.compareTo(credits) != 0) {
            throw new UnbalancedJournalException(journal.getTxnId(), debits, credits);
        }
        if (journal.getPostings().isEmpty()) {
            throw new IllegalArgumentException("journal must contain at least one posting");
        }
        Instant now = Instant.now();
        for (Posting p : journal.getPostings()) {
            if (p.getTimestamp() == null) {
                p.setTimestamp(now);
            }
            if (p.getTxnId() == null) {
                p.setTxnId(journal.getTxnId());
            }
        }
        return repository.saveAll(journal.getPostings());
    }

    public List<Posting> postingsForAccount(String accountId) {
        return repository.findAllByAccountId(accountId);
    }

    @ComplianceCritical(
        category = ComplianceCategory.TRANSACTION_INTEGRITY,
        note = "Balance derived strictly from persisted postings; no side state."
    )
    public BigDecimal deriveBalance(String accountId) {
        List<Posting> postings = repository.findAllByAccountId(accountId);
        BigDecimal credit = postings.stream()
                .map(Posting::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal debit = postings.stream()
                .map(Posting::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return credit.subtract(debit);
    }
}
