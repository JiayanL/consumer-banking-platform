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
}
