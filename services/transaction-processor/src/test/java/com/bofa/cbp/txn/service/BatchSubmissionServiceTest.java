package com.bofa.cbp.txn.service;

import com.bofa.cbp.txn.domain.TransactionRequest;
import com.bofa.cbp.txn.domain.TransactionResult;
import com.bofa.cbp.txn.domain.TransactionStatus;
import com.bofa.cbp.txn.domain.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BatchSubmissionServiceTest {

    private TransactionProcessor processor;
    private BatchSubmissionService batchService;

    @BeforeEach
    void setUp() {
        processor = mock(TransactionProcessor.class);
        batchService = new BatchSubmissionService(processor);
    }

    private TransactionRequest buildRequest() {
        TransactionRequest req = new TransactionRequest();
        req.setAccountId("ACC-0001");
        req.setCounterpartyAccountId("ACC-9999");
        req.setType(TransactionType.DEBIT);
        req.setAmount(new BigDecimal("100.00"));
        req.setCurrency("USD");
        return req;
    }

    @Test
    void allAccepted() {
        when(processor.process(any())).thenReturn(TransactionResult.accepted(1L));

        List<TransactionRequest> requests = List.of(buildRequest(), buildRequest(), buildRequest());
        BatchSubmissionService.BatchResult result = batchService.submitAll(requests);

        assertEquals(3, result.total());
        assertEquals(3, result.accepted());
        assertEquals(0, result.rejected());
        assertEquals(0, result.failed());
    }

    @Test
    void mixedResults() {
        when(processor.process(any()))
                .thenReturn(TransactionResult.accepted(1L))
                .thenReturn(TransactionResult.rejected(2L, "INSUFFICIENT_FUNDS"))
                .thenReturn(TransactionResult.failed("validation error"));

        List<TransactionRequest> requests = List.of(buildRequest(), buildRequest(), buildRequest());
        BatchSubmissionService.BatchResult result = batchService.submitAll(requests);

        assertEquals(3, result.total());
        assertEquals(1, result.accepted());
        assertEquals(1, result.rejected());
        assertEquals(1, result.failed());
    }

    @Test
    void runtimeExceptionCaught() {
        when(processor.process(any()))
                .thenReturn(TransactionResult.accepted(1L))
                .thenThrow(new RuntimeException("boom"));

        List<TransactionRequest> requests = List.of(buildRequest(), buildRequest());
        BatchSubmissionService.BatchResult result = batchService.submitAll(requests);

        assertEquals(2, result.total());
        assertEquals(1, result.accepted());
        assertEquals(0, result.rejected());
        assertEquals(1, result.failed());
    }

    @Test
    void summarizeStaticMethod() {
        List<TransactionResult> results = List.of(
                TransactionResult.accepted(1L),
                TransactionResult.accepted(2L),
                TransactionResult.rejected(3L, "limit"),
                TransactionResult.failed("error")
        );

        BatchSubmissionService.BatchResult summary = BatchSubmissionService.summarize(results);

        assertEquals(4, summary.total());
        assertEquals(2, summary.accepted());
        assertEquals(1, summary.rejected());
        assertEquals(1, summary.failed());
    }
}
