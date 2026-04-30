package com.bofa.cbp.txn.service;

import com.bofa.cbp.txn.domain.*;
import com.bofa.cbp.txn.events.EventEmitter;
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

class BatchSubmissionServiceTest {

    private BatchSubmissionService batch;

    @BeforeEach
    void setUp() {
        TransactionRepository repo = mock(TransactionRepository.class);
        LedgerClient ledger = new LedgerClient();
        EventEmitter events = new EventEmitter();
        TransactionProcessor processor = new TransactionProcessor(repo, ledger, events);
        batch = new BatchSubmissionService(processor);

        when(repo.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(System.nanoTime());
            if (t.getCreatedAt() == null) t.setCreatedAt(Instant.now());
            return t;
        });
        when(repo.findFirstByIdempotencyKey(any())).thenReturn(Optional.empty());
    }

    @Test
    void batchOfValidTransactions() {
        List<TransactionRequest> reqs = List.of(
                buildReq("ACC-0001", new BigDecimal("50.00")),
                buildReq("ACC-0001", new BigDecimal("75.00"))
        );
        BatchSubmissionService.BatchResult result = batch.submitAll(reqs);

        assertEquals(2, result.total());
        assertEquals(2, result.accepted());
        assertEquals(0, result.rejected());
        assertEquals(0, result.failed());
    }

    @Test
    void batchWithMixedValidAndInvalid() {
        List<TransactionRequest> reqs = List.of(
                buildReq("ACC-0001", new BigDecimal("50.00")),
                buildReq("ACC-0003", new BigDecimal("999.00"))
        );
        BatchSubmissionService.BatchResult result = batch.submitAll(reqs);

        assertEquals(2, result.total());
        assertEquals(1, result.accepted());
        assertEquals(1, result.rejected());
    }

    @Test
    void summarizeStaticMethod() {
        List<TransactionResult> rs = List.of(
                TransactionResult.accepted(1L),
                TransactionResult.rejected(2L, "reason"),
                TransactionResult.failed("error")
        );
        BatchSubmissionService.BatchResult summary = BatchSubmissionService.summarize(rs);
        assertEquals(3, summary.total());
        assertEquals(1, summary.accepted());
        assertEquals(1, summary.rejected());
        assertEquals(1, summary.failed());
    }

    private TransactionRequest buildReq(String account, BigDecimal amount) {
        TransactionRequest req = new TransactionRequest();
        req.setAccountId(account);
        req.setCounterpartyAccountId("ACC-9999");
        req.setType(TransactionType.DEBIT);
        req.setAmount(amount);
        req.setCurrency("USD");
        return req;
    }
}
