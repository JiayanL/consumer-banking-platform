package com.bofa.cbp.txn.service;

import com.bofa.cbp.txn.domain.Transaction;
import com.bofa.cbp.txn.domain.TransactionStatus;
import com.bofa.cbp.txn.domain.TransactionType;
import com.bofa.cbp.txn.events.EventEmitter;
import com.bofa.cbp.txn.events.TransactionEvent;
import com.bofa.cbp.txn.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReversalServiceTest {

    private TransactionRepository repo;
    private LedgerClient ledger;
    private EventEmitter events;
    private ReversalService service;

    @BeforeEach
    void setUp() {
        repo = mock(TransactionRepository.class);
        ledger = new LedgerClient();
        events = new EventEmitter();
        service = new ReversalService(repo, ledger, events);

        when(repo.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Transaction buildTransaction(Long id, TransactionType type, TransactionStatus status,
                                         String accountId, String counterpartyId,
                                         BigDecimal amount, Instant createdAt) {
        return Transaction.builder()
                .id(id)
                .accountId(accountId)
                .counterpartyAccountId(counterpartyId)
                .type(type)
                .amount(amount)
                .currency("USD")
                .status(status)
                .createdAt(createdAt)
                .build();
    }

    @Test
    void reverseDebitHappyPath() {
        Transaction debit = buildTransaction(1L, TransactionType.DEBIT, TransactionStatus.ACCEPTED,
                "ACC-0001", "ACC-9999", new BigDecimal("200.00"), Instant.now().minus(1, ChronoUnit.HOURS));
        when(repo.findById(1L)).thenReturn(Optional.of(debit));

        BigDecimal balanceBefore = ledger.getBalance("ACC-0001");
        ReversalService.ReversalResult result = service.reverse(1L, "customer-request");

        assertEquals("OK", result.status());
        assertNull(result.reason());

        BigDecimal balanceAfter = ledger.getBalance("ACC-0001");
        assertEquals(0, balanceBefore.add(new BigDecimal("200.00")).compareTo(balanceAfter));

        List<TransactionEvent> emitted = events.drain();
        assertEquals(1, emitted.size());
        assertEquals(0, emitted.get(0).getAmount().compareTo(new BigDecimal("-200.00")));
    }

    @Test
    void reverseTransferHappyPath() {
        Transaction transfer = buildTransaction(2L, TransactionType.TRANSFER, TransactionStatus.ACCEPTED,
                "ACC-0001", "ACC-0002", new BigDecimal("300.00"), Instant.now().minus(2, ChronoUnit.HOURS));
        when(repo.findById(2L)).thenReturn(Optional.of(transfer));

        BigDecimal sourceBefore = ledger.getBalance("ACC-0001");
        BigDecimal counterpartyBefore = ledger.getBalance("ACC-0002");

        ReversalService.ReversalResult result = service.reverse(2L, "error-correction");

        assertEquals("OK", result.status());

        assertEquals(0, sourceBefore.add(new BigDecimal("300.00")).compareTo(ledger.getBalance("ACC-0001")));
        assertEquals(0, counterpartyBefore.subtract(new BigDecimal("300.00")).compareTo(ledger.getBalance("ACC-0002")));
    }

    @Test
    void reverseCreditHappyPath() {
        Transaction credit = buildTransaction(3L, TransactionType.CREDIT, TransactionStatus.ACCEPTED,
                "ACC-0001", "ACC-9999", new BigDecimal("150.00"), Instant.now().minus(1, ChronoUnit.DAYS));
        when(repo.findById(3L)).thenReturn(Optional.of(credit));

        BigDecimal balanceBefore = ledger.getBalance("ACC-0001");
        ReversalService.ReversalResult result = service.reverse(3L, "chargeback");

        assertEquals("OK", result.status());
        assertEquals(0, balanceBefore.subtract(new BigDecimal("150.00")).compareTo(ledger.getBalance("ACC-0001")));
    }

    @Test
    void reverseNotFound() {
        when(repo.findById(999L)).thenReturn(Optional.empty());

        ReversalService.ReversalResult result = service.reverse(999L, "n/a");

        assertEquals("NOT_FOUND", result.status());
    }

    @Test
    void reverseNotAccepted() {
        Transaction rejected = buildTransaction(4L, TransactionType.DEBIT, TransactionStatus.REJECTED,
                "ACC-0001", "ACC-9999", new BigDecimal("100.00"), Instant.now().minus(1, ChronoUnit.HOURS));
        when(repo.findById(4L)).thenReturn(Optional.of(rejected));

        ReversalService.ReversalResult result = service.reverse(4L, "n/a");

        assertEquals("INELIGIBLE", result.status());
        assertEquals("not-accepted", result.reason());
    }

    @Test
    void reverseOutsideDebitWindow() {
        Transaction oldDebit = buildTransaction(5L, TransactionType.DEBIT, TransactionStatus.ACCEPTED,
                "ACC-0001", "ACC-9999", new BigDecimal("100.00"), Instant.now().minus(25, ChronoUnit.HOURS));
        when(repo.findById(5L)).thenReturn(Optional.of(oldDebit));

        ReversalService.ReversalResult result = service.reverse(5L, "late-request");

        assertEquals("INELIGIBLE", result.status());
        assertEquals("outside-window", result.reason());
    }

    @Test
    void reverseCreditWithin90Days() {
        Transaction credit89 = buildTransaction(6L, TransactionType.CREDIT, TransactionStatus.ACCEPTED,
                "ACC-0001", "ACC-9999", new BigDecimal("100.00"), Instant.now().minus(89, ChronoUnit.DAYS));
        when(repo.findById(6L)).thenReturn(Optional.of(credit89));

        ReversalService.ReversalResult result = service.reverse(6L, "dispute");

        assertEquals("OK", result.status());
    }

    @Test
    void reverseCreditOutside90Days() {
        Transaction credit91 = buildTransaction(7L, TransactionType.CREDIT, TransactionStatus.ACCEPTED,
                "ACC-0001", "ACC-9999", new BigDecimal("100.00"), Instant.now().minus(91, ChronoUnit.DAYS));
        when(repo.findById(7L)).thenReturn(Optional.of(credit91));

        ReversalService.ReversalResult result = service.reverse(7L, "dispute");

        assertEquals("INELIGIBLE", result.status());
        assertEquals("outside-window", result.reason());
    }
}
