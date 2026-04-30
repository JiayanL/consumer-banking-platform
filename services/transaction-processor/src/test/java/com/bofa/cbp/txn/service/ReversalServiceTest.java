package com.bofa.cbp.txn.service;

import com.bofa.cbp.txn.domain.*;
import com.bofa.cbp.txn.events.EventEmitter;
import com.bofa.cbp.txn.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReversalServiceTest {

    private TransactionRepository repo;
    private LedgerClient ledger;
    private EventEmitter events;
    private ReversalService reversalService;

    @BeforeEach
    void setUp() {
        repo = mock(TransactionRepository.class);
        ledger = new LedgerClient();
        events = new EventEmitter();
        reversalService = new ReversalService(repo, ledger, events);
        when(repo.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void reverseExistingDebitTransaction() {
        Transaction t = Transaction.builder()
                .id(1L)
                .accountId("ACC-0001")
                .counterpartyAccountId("ACC-9999")
                .type(TransactionType.DEBIT)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(TransactionStatus.ACCEPTED)
                .createdAt(Instant.now())
                .build();
        when(repo.findById(1L)).thenReturn(Optional.of(t));

        ReversalService.ReversalResult r = reversalService.reverse(1L, "customer-request");
        assertEquals("OK", r.status());
        assertEquals(1, events.drain().size());
    }

    @Test
    void reverseNonExistentTransaction() {
        when(repo.findById(999L)).thenReturn(Optional.empty());
        ReversalService.ReversalResult r = reversalService.reverse(999L, "reason");
        assertEquals("NOT_FOUND", r.status());
    }

    @Test
    void reverseRejectedTransactionIsIneligible() {
        Transaction t = Transaction.builder()
                .id(2L)
                .accountId("ACC-0001")
                .counterpartyAccountId("ACC-9999")
                .type(TransactionType.DEBIT)
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .status(TransactionStatus.REJECTED)
                .createdAt(Instant.now())
                .build();
        when(repo.findById(2L)).thenReturn(Optional.of(t));

        ReversalService.ReversalResult r = reversalService.reverse(2L, "reason");
        assertEquals("INELIGIBLE", r.status());
        assertEquals("not-accepted", r.reason());
    }

    @Test
    void reverseCreditTransaction() {
        Transaction t = Transaction.builder()
                .id(3L)
                .accountId("ACC-0001")
                .counterpartyAccountId("ACC-9999")
                .type(TransactionType.CREDIT)
                .amount(new BigDecimal("200.00"))
                .currency("USD")
                .status(TransactionStatus.ACCEPTED)
                .createdAt(Instant.now())
                .build();
        when(repo.findById(3L)).thenReturn(Optional.of(t));

        ReversalService.ReversalResult r = reversalService.reverse(3L, "error-correction");
        assertEquals("OK", r.status());
    }

    @Test
    void reverseTransferCreditsBackSenderDebitsCounterparty() {
        Transaction t = Transaction.builder()
                .id(4L)
                .accountId("ACC-0001")
                .counterpartyAccountId("ACC-0002")
                .type(TransactionType.TRANSFER)
                .amount(new BigDecimal("100.00"))
                .currency("USD")
                .status(TransactionStatus.ACCEPTED)
                .createdAt(Instant.now())
                .build();
        when(repo.findById(4L)).thenReturn(Optional.of(t));

        BigDecimal senderBefore = ledger.getBalance("ACC-0001");
        BigDecimal counterBefore = ledger.getBalance("ACC-0002");

        reversalService.reverse(4L, "undo");

        assertEquals(senderBefore.add(new BigDecimal("100.00")), ledger.getBalance("ACC-0001"));
        assertEquals(counterBefore.subtract(new BigDecimal("100.00")), ledger.getBalance("ACC-0002"));
    }
}
