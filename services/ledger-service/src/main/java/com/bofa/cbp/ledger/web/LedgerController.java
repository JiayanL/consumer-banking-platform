package com.bofa.cbp.ledger.web;

import com.bofa.cbp.ledger.domain.Journal;
import com.bofa.cbp.ledger.domain.Posting;
import com.bofa.cbp.ledger.service.LedgerService;
import com.bofa.cbp.ledger.service.UnbalancedJournalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/")
public class LedgerController {

    // Field injection here is deliberate — see LedgerService for the
    // constructor-injection contrast.
    @Autowired
    private LedgerService ledgerService;

    @PostMapping("/journal")
    public ResponseEntity<List<Posting>> submitJournal(@RequestBody JournalRequest req) {
        List<Posting> postings = new ArrayList<>();
        for (PostingRequest p : req.postings()) {
            postings.add(new Posting(
                    p.accountId(),
                    p.debit() == null ? BigDecimal.ZERO : p.debit(),
                    p.credit() == null ? BigDecimal.ZERO : p.credit(),
                    req.txnId(),
                    null
            ));
        }
        Journal journal = new Journal(req.txnId(), postings);
        List<Posting> saved = ledgerService.submitJournal(journal);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/postings")
    public List<Posting> byAccount(@RequestParam String accountId) {
        return ledgerService.postingsForAccount(accountId);
    }

    @GetMapping("/accounts/{id}/balance")
    public Map<String, Object> balance(@PathVariable("id") String accountId) {
        BigDecimal b = ledgerService.deriveBalance(accountId);
        return Map.of("accountId", accountId, "balance", b);
    }

    @ExceptionHandler(UnbalancedJournalException.class)
    public ResponseEntity<Map<String, String>> handleUnbalanced(UnbalancedJournalException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", ex.getMessage()));
    }

    public record JournalRequest(String txnId, List<PostingRequest> postings) {}
    public record PostingRequest(String accountId, BigDecimal debit, BigDecimal credit) {}
}
