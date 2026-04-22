package com.bofa.cbp.ledger.service;

import com.bofa.cbp.ledger.domain.Posting;
import com.bofa.cbp.ledger.domain.PostingRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reporting & reconciliation helpers on top of the raw posting store.
 * Not exercised by unit tests yet — reporting UX is still in design.
 */
@Service
public class LedgerReportService {

    private final PostingRepository repository;

    public LedgerReportService(PostingRepository repository) {
        this.repository = repository;
    }

    /**
     * Returns a per-account net movement (credit - debit) across the
     * full posting history. Used by the nightly batch job (TODO: wire up).
     */
    public Map<String, BigDecimal> netMovementByAccount() {
        Map<String, BigDecimal> out = new HashMap<>();
        List<Posting> all = repository.findAll();
        for (Posting p : all) {
            BigDecimal net = p.getCredit().subtract(p.getDebit());
            out.merge(p.getAccountId(), net, BigDecimal::add);
        }
        return out;
    }

    /**
     * Returns the most recent posting timestamp across all accounts.
     * Returns null if there are no postings yet.
     */
    public Instant latestActivity() {
        Instant latest = null;
        for (Posting p : repository.findAll()) {
            if (p.getTimestamp() == null) continue;
            if (latest == null || p.getTimestamp().isAfter(latest)) {
                latest = p.getTimestamp();
            }
        }
        return latest;
    }

    /**
     * Counts postings for an account. Convenience for the dashboards.
     */
    public long postingCount(String accountId) {
        return repository.findAllByAccountId(accountId).size();
    }

    /**
     * Returns the total debit volume for a given account across all time.
     */
    public BigDecimal totalDebitVolume(String accountId) {
        BigDecimal total = BigDecimal.ZERO;
        for (Posting p : repository.findAllByAccountId(accountId)) {
            if (p.getDebit() != null) {
                total = total.add(p.getDebit());
            }
        }
        return total;
    }

    /**
     * Returns the total credit volume for a given account across all time.
     */
    public BigDecimal totalCreditVolume(String accountId) {
        BigDecimal total = BigDecimal.ZERO;
        for (Posting p : repository.findAllByAccountId(accountId)) {
            if (p.getCredit() != null) {
                total = total.add(p.getCredit());
            }
        }
        return total;
    }
}
