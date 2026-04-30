package com.bofa.cbp.txn.service;

import com.bofa.cbp.txn.domain.*;
import com.bofa.cbp.txn.events.EventEmitter;
import com.bofa.cbp.txn.events.TransactionEvent;
import com.bofa.cbp.txn.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionProcessorTest {

    private TransactionRepository repo;
    private LedgerClient ledger;
    private EventEmitter events;
    private TransactionProcessor processor;

    @BeforeEach
    void setUp() {
        repo = mock(TransactionRepository.class);
        ledger = new LedgerClient();
        events = new EventEmitter();
        processor = new TransactionProcessor(repo, ledger, events);

        when(repo.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(42L);
            if (t.getCreatedAt() == null) t.setCreatedAt(Instant.now());
            return t;
        });
        when(repo.findFirstByIdempotencyKey(any())).thenReturn(Optional.empty());
    }

    @Test
    void debitHappyPath() {
        TransactionRequest req = new TransactionRequest();
        req.setAccountId("ACC-0001");
        req.setCounterpartyAccountId("ACC-9999");
        req.setType(TransactionType.DEBIT);
        req.setAmount(new BigDecimal("100.00"));
        req.setCurrency("USD");

        TransactionResult r = processor.process(req);

        assertEquals(TransactionStatus.ACCEPTED, r.getStatus());
        assertEquals(42L, r.getTransactionId());
        assertEquals(1, events.drain().size());
    }

    @Test
    void creditHappyPath() {
        TransactionRequest req = new TransactionRequest();
        req.setAccountId("ACC-0001");
        req.setCounterpartyAccountId("ACC-9999");
        req.setType(TransactionType.CREDIT);
        req.setAmount(new BigDecimal("500.00"));
        req.setCurrency("USD");

        BigDecimal balanceBefore = ledger.getBalance("ACC-0001");
        TransactionResult r = processor.process(req);

        assertEquals(TransactionStatus.ACCEPTED, r.getStatus());
        assertEquals(42L, r.getTransactionId());

        BigDecimal balanceAfter = ledger.getBalance("ACC-0001");
        assertEquals(0, balanceBefore.add(new BigDecimal("500.00")).compareTo(balanceAfter));

        verify(repo).save(argThat(t -> t.getType() == TransactionType.CREDIT));
    }

    @Test
    void transferHappyPath() {
        TransactionRequest req = new TransactionRequest();
        req.setAccountId("ACC-0001");
        req.setCounterpartyAccountId("ACC-0002");
        req.setType(TransactionType.TRANSFER);
        req.setAmount(new BigDecimal("200.00"));
        req.setCurrency("USD");

        BigDecimal sourceBalanceBefore = ledger.getBalance("ACC-0001");
        BigDecimal counterpartyBalanceBefore = ledger.getBalance("ACC-0002");

        TransactionResult r = processor.process(req);

        assertEquals(TransactionStatus.ACCEPTED, r.getStatus());

        BigDecimal sourceBalanceAfter = ledger.getBalance("ACC-0001");
        BigDecimal counterpartyBalanceAfter = ledger.getBalance("ACC-0002");

        assertEquals(0, sourceBalanceBefore.subtract(new BigDecimal("200.00")).compareTo(sourceBalanceAfter));
        assertEquals(0, counterpartyBalanceBefore.add(new BigDecimal("200.00")).compareTo(counterpartyBalanceAfter));
    }

    @Test
    void idempotencyReplay() {
        Transaction existing = Transaction.builder()
                .id(99L)
                .accountId("ACC-0001")
                .counterpartyAccountId("ACC-9999")
                .type(TransactionType.DEBIT)
                .amount(new BigDecimal("50.00"))
                .currency("USD")
                .status(TransactionStatus.ACCEPTED)
                .idempotencyKey("idem-key-1")
                .createdAt(Instant.now())
                .build();
        when(repo.findFirstByIdempotencyKey("idem-key-1")).thenReturn(Optional.of(existing));

        TransactionRequest req = new TransactionRequest();
        req.setAccountId("ACC-0001");
        req.setCounterpartyAccountId("ACC-9999");
        req.setType(TransactionType.DEBIT);
        req.setAmount(new BigDecimal("50.00"));
        req.setCurrency("USD");
        req.setIdempotencyKey("idem-key-1");

        TransactionResult r = processor.process(req);

        assertEquals(TransactionStatus.ACCEPTED, r.getStatus());
        assertEquals("idempotent-replay", r.getReason());
        assertEquals(99L, r.getTransactionId());
        verify(repo, never()).save(any());
    }

    @Test
    void rejectsAmountWithMoreThanTwoDecimalPlaces() {
        TransactionRequest req = new TransactionRequest();
        req.setAccountId("ACC-0001");
        req.setCounterpartyAccountId("ACC-9999");
        req.setType(TransactionType.DEBIT);
        req.setAmount(new BigDecimal("100.123"));
        req.setCurrency("USD");

        TransactionResult r = processor.process(req);

        assertEquals(TransactionStatus.FAILED, r.getStatus());
        assertEquals("amount exceeds 2 decimal places", r.getReason());
    }

    @Test
    void rejectsNonUsdCurrency() {
        TransactionRequest req = new TransactionRequest();
        req.setAccountId("ACC-0001");
        req.setCounterpartyAccountId("ACC-9999");
        req.setType(TransactionType.DEBIT);
        req.setAmount(new BigDecimal("100.00"));
        req.setCurrency("EUR");

        TransactionResult r = processor.process(req);

        assertEquals(TransactionStatus.FAILED, r.getStatus());
        assertEquals("only USD is supported in this build", r.getReason());
    }

    @Test
    void insufficientFundsDebit() {
        TransactionRequest req = new TransactionRequest();
        req.setAccountId("ACC-0003");
        req.setCounterpartyAccountId("ACC-9999");
        req.setType(TransactionType.DEBIT);
        req.setAmount(new BigDecimal("100.00"));
        req.setCurrency("USD");

        TransactionResult r = processor.process(req);

        assertEquals(TransactionStatus.REJECTED, r.getStatus());
        assertEquals("INSUFFICIENT_FUNDS", r.getReason());

        List<TransactionEvent> emittedEvents = events.drain();
        assertEquals(1, emittedEvents.size());
        assertEquals(TransactionStatus.REJECTED, emittedEvents.get(0).getStatus());
    }

    @Test
    void insufficientFundsTransfer() {
        TransactionRequest req = new TransactionRequest();
        req.setAccountId("ACC-0003");
        req.setCounterpartyAccountId("ACC-0002");
        req.setType(TransactionType.TRANSFER);
        req.setAmount(new BigDecimal("100.00"));
        req.setCurrency("USD");

        TransactionResult r = processor.process(req);

        assertEquals(TransactionStatus.REJECTED, r.getStatus());
        assertEquals("INSUFFICIENT_FUNDS", r.getReason());

        List<TransactionEvent> emittedEvents = events.drain();
        assertEquals(1, emittedEvents.size());
        assertEquals(TransactionStatus.REJECTED, emittedEvents.get(0).getStatus());
    }
}
