package com.bofa.cbp.ledger.service;

import com.bofa.cbp.ledger.domain.Journal;
import com.bofa.cbp.ledger.domain.Posting;
import com.bofa.cbp.ledger.domain.PostingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerServiceUnitTest {

    @Mock
    private PostingRepository repository;

    @InjectMocks
    private LedgerService ledgerService;

    // --- submitJournal: compliance-critical happy path ---

    @Test
    void submitJournal_balancedJournal_savesAndReturnsPostings() {
        Posting debit = new Posting("acct-A", new BigDecimal("250.00"), BigDecimal.ZERO, "txn-1", Instant.now());
        Posting credit = new Posting("acct-B", BigDecimal.ZERO, new BigDecimal("250.00"), "txn-1", Instant.now());
        Journal journal = new Journal("txn-1", List.of(debit, credit));

        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Posting> result = ledgerService.submitJournal(journal);

        assertThat(result).hasSize(2);
        verify(repository).saveAll(anyList());
    }

    @Test
    void submitJournal_balancedJournal_fillsNullTimestamps() {
        Posting debit = new Posting("acct-A", new BigDecimal("50.00"), BigDecimal.ZERO, "txn-2", null);
        Posting credit = new Posting("acct-B", BigDecimal.ZERO, new BigDecimal("50.00"), "txn-2", null);
        Journal journal = new Journal("txn-2", List.of(debit, credit));

        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Posting> result = ledgerService.submitJournal(journal);

        assertThat(result).allSatisfy(p -> assertThat(p.getTimestamp()).isNotNull());
    }

    @Test
    void submitJournal_balancedJournal_fillsNullTxnIds() {
        Posting debit = new Posting("acct-A", new BigDecimal("75.00"), BigDecimal.ZERO, null, Instant.now());
        Posting credit = new Posting("acct-B", BigDecimal.ZERO, new BigDecimal("75.00"), null, Instant.now());
        Journal journal = new Journal("txn-3", List.of(debit, credit));

        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Posting> result = ledgerService.submitJournal(journal);

        assertThat(result).allSatisfy(p -> assertThat(p.getTxnId()).isEqualTo("txn-3"));
    }

    @Test
    void submitJournal_preservesExistingTimestampsAndTxnIds() {
        Instant fixedTime = Instant.parse("2026-01-15T10:00:00Z");
        Posting debit = new Posting("acct-A", new BigDecimal("100.00"), BigDecimal.ZERO, "existing-txn", fixedTime);
        Posting credit = new Posting("acct-B", BigDecimal.ZERO, new BigDecimal("100.00"), "existing-txn", fixedTime);
        Journal journal = new Journal("txn-4", List.of(debit, credit));

        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Posting> result = ledgerService.submitJournal(journal);

        assertThat(result).allSatisfy(p -> {
            assertThat(p.getTimestamp()).isEqualTo(fixedTime);
            assertThat(p.getTxnId()).isEqualTo("existing-txn");
        });
    }

    // --- submitJournal: compliance-critical failure paths ---

    @Test
    void submitJournal_unbalancedJournal_throwsUnbalancedJournalException() {
        Posting debit = new Posting("acct-A", new BigDecimal("100.00"), BigDecimal.ZERO, "txn-bad", Instant.now());
        Posting credit = new Posting("acct-B", BigDecimal.ZERO, new BigDecimal("50.00"), "txn-bad", Instant.now());
        Journal journal = new Journal("txn-bad", List.of(debit, credit));

        assertThatThrownBy(() -> ledgerService.submitJournal(journal))
                .isInstanceOf(UnbalancedJournalException.class)
                .hasMessageContaining("txn-bad")
                .hasMessageContaining("unbalanced");
    }

    @Test
    void submitJournal_emptyPostings_throwsIllegalArgumentException() {
        Journal journal = new Journal("txn-empty", Collections.emptyList());

        assertThatThrownBy(() -> ledgerService.submitJournal(journal))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one posting");
    }

    @Test
    void submitJournal_singlePostingWithZeroDebitAndCredit_throwsForEmptyCheck() {
        Posting zero = new Posting("acct-A", BigDecimal.ZERO, BigDecimal.ZERO, "txn-zero", Instant.now());
        Journal journal = new Journal("txn-zero", List.of(zero));

        when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<Posting> result = ledgerService.submitJournal(journal);
        assertThat(result).hasSize(1);
    }

    // --- postingsForAccount ---

    @Test
    void postingsForAccount_delegatesToRepository() {
        Posting p1 = new Posting("acct-X", new BigDecimal("10.00"), BigDecimal.ZERO, "txn-1", Instant.now());
        when(repository.findAllByAccountId("acct-X")).thenReturn(List.of(p1));

        List<Posting> result = ledgerService.postingsForAccount("acct-X");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccountId()).isEqualTo("acct-X");
        verify(repository).findAllByAccountId("acct-X");
    }

    // --- deriveBalance: compliance-critical ---

    @Test
    void deriveBalance_computesCreditMinusDebit() {
        Posting p1 = new Posting("acct-B", BigDecimal.ZERO, new BigDecimal("500.00"), "txn-1", Instant.now());
        Posting p2 = new Posting("acct-B", new BigDecimal("200.00"), BigDecimal.ZERO, "txn-2", Instant.now());
        when(repository.findAllByAccountId("acct-B")).thenReturn(List.of(p1, p2));

        BigDecimal balance = ledgerService.deriveBalance("acct-B");

        assertThat(balance).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    @Test
    void deriveBalance_emptyAccount_returnsZero() {
        when(repository.findAllByAccountId("acct-empty")).thenReturn(Collections.emptyList());

        BigDecimal balance = ledgerService.deriveBalance("acct-empty");

        assertThat(balance).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void deriveBalance_multiplePostings_correctAggregation() {
        Posting credit1 = new Posting("acct-C", BigDecimal.ZERO, new BigDecimal("1000.00"), "txn-1", Instant.now());
        Posting debit1 = new Posting("acct-C", new BigDecimal("300.00"), BigDecimal.ZERO, "txn-2", Instant.now());
        Posting credit2 = new Posting("acct-C", BigDecimal.ZERO, new BigDecimal("200.00"), "txn-3", Instant.now());
        Posting debit2 = new Posting("acct-C", new BigDecimal("150.00"), BigDecimal.ZERO, "txn-4", Instant.now());
        when(repository.findAllByAccountId("acct-C")).thenReturn(List.of(credit1, debit1, credit2, debit2));

        BigDecimal balance = ledgerService.deriveBalance("acct-C");

        assertThat(balance).isEqualByComparingTo(new BigDecimal("750.00"));
    }
}
