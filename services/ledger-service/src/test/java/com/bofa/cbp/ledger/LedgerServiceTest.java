package com.bofa.cbp.ledger;

import com.bofa.cbp.ledger.domain.Journal;
import com.bofa.cbp.ledger.domain.Posting;
import com.bofa.cbp.ledger.service.LedgerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class LedgerServiceTest {

    @Autowired
    private LedgerService ledgerService;

    @Test
    void submittingBalancedJournalPersistsPostingsAndDerivesBalance() {
        Posting debit = new Posting("acct-A", new BigDecimal("100.00"), BigDecimal.ZERO, "txn-1", null);
        Posting credit = new Posting("acct-B", BigDecimal.ZERO, new BigDecimal("100.00"), "txn-1", null);

        Journal journal = new Journal("txn-1", List.of(debit, credit));
        assertEquals(0, journal.totalDebits().compareTo(journal.totalCredits()));

        List<Posting> saved = ledgerService.submitJournal(journal);
        assertEquals(2, saved.size());

        BigDecimal balanceB = ledgerService.deriveBalance("acct-B");
        assertEquals(0, new BigDecimal("100.00").compareTo(balanceB));
    }
}
