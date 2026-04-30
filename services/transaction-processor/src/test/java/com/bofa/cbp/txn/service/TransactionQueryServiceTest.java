package com.bofa.cbp.txn.service;

import com.bofa.cbp.txn.domain.Transaction;
import com.bofa.cbp.txn.domain.TransactionStatus;
import com.bofa.cbp.txn.domain.TransactionType;
import com.bofa.cbp.txn.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionQueryServiceTest {

    private TransactionRepository repo;
    private TransactionQueryService queryService;

    @BeforeEach
    void setUp() {
        repo = mock(TransactionRepository.class);
        queryService = new TransactionQueryService(repo);

        when(repo.findAll()).thenReturn(List.of(
                buildTxn(1L, "ACC-1", TransactionType.DEBIT, TransactionStatus.ACCEPTED,
                        new BigDecimal("100.00"), Instant.parse("2024-03-01T10:00:00Z")),
                buildTxn(2L, "ACC-1", TransactionType.CREDIT, TransactionStatus.ACCEPTED,
                        new BigDecimal("200.00"), Instant.parse("2024-03-02T10:00:00Z")),
                buildTxn(3L, "ACC-2", TransactionType.DEBIT, TransactionStatus.REJECTED,
                        new BigDecimal("50.00"), Instant.parse("2024-03-01T11:00:00Z"))
        ));
    }

    @Test
    void findByAccountReturnsMatchingTransactions() {
        List<Transaction> result = queryService.findByAccount("ACC-1");
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(t -> "ACC-1".equals(t.getAccountId())));
    }

    @Test
    void findByAccountReturnsEmptyForUnknown() {
        List<Transaction> result = queryService.findByAccount("ACC-UNKNOWN");
        assertTrue(result.isEmpty());
    }

    @Test
    void findByStatus() {
        List<Transaction> result = queryService.findByStatus(TransactionStatus.REJECTED);
        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).getId());
    }

    @Test
    void findByType() {
        List<Transaction> result = queryService.findByType(TransactionType.DEBIT);
        assertEquals(2, result.size());
    }

    @Test
    void findSince() {
        List<Transaction> result = queryService.findSince(Instant.parse("2024-03-01T12:00:00Z"));
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getId());
    }

    @Test
    void sumAccepted() {
        BigDecimal total = queryService.sumAccepted("ACC-1");
        assertEquals(new BigDecimal("300.00"), total);
    }

    @Test
    void sumAcceptedForUnknownAccount() {
        assertEquals(BigDecimal.ZERO, queryService.sumAccepted("ACC-UNKNOWN"));
    }

    private Transaction buildTxn(Long id, String account, TransactionType type,
                                 TransactionStatus status, BigDecimal amount, Instant created) {
        return Transaction.builder()
                .id(id)
                .accountId(account)
                .counterpartyAccountId("ACC-9999")
                .type(type)
                .amount(amount)
                .currency("USD")
                .status(status)
                .createdAt(created)
                .build();
    }
}
