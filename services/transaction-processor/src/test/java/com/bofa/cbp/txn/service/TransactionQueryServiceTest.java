package com.bofa.cbp.txn.service;

import com.bofa.cbp.txn.domain.Transaction;
import com.bofa.cbp.txn.domain.TransactionStatus;
import com.bofa.cbp.txn.domain.TransactionType;
import com.bofa.cbp.txn.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionQueryServiceTest {

    private TransactionRepository repo;
    private TransactionQueryService queryService;

    private final Instant now = Instant.now();

    private List<Transaction> seedData() {
        return List.of(
                Transaction.builder().id(1L).accountId("ACC-0001").counterpartyAccountId("ACC-9999")
                        .type(TransactionType.DEBIT).amount(new BigDecimal("100.00")).currency("USD")
                        .status(TransactionStatus.ACCEPTED).createdAt(now.minus(3, ChronoUnit.HOURS)).build(),
                Transaction.builder().id(2L).accountId("ACC-0001").counterpartyAccountId("ACC-9999")
                        .type(TransactionType.CREDIT).amount(new BigDecimal("200.00")).currency("USD")
                        .status(TransactionStatus.ACCEPTED).createdAt(now.minus(1, ChronoUnit.HOURS)).build(),
                Transaction.builder().id(3L).accountId("ACC-0002").counterpartyAccountId("ACC-0001")
                        .type(TransactionType.DEBIT).amount(new BigDecimal("50.00")).currency("USD")
                        .status(TransactionStatus.REJECTED).createdAt(now.minus(2, ChronoUnit.HOURS)).build(),
                Transaction.builder().id(4L).accountId("ACC-0001").counterpartyAccountId("ACC-9999")
                        .type(TransactionType.TRANSFER).amount(new BigDecimal("500.00")).currency("USD")
                        .status(TransactionStatus.ACCEPTED).createdAt(now.minus(5, ChronoUnit.HOURS)).build()
        );
    }

    @BeforeEach
    void setUp() {
        repo = mock(TransactionRepository.class);
        queryService = new TransactionQueryService(repo);
        when(repo.findAll()).thenReturn(seedData());
    }

    @Test
    void findByAccountFiltersCorrectly() {
        List<Transaction> result = queryService.findByAccount("ACC-0001");

        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(t -> "ACC-0001".equals(t.getAccountId())));
    }

    @Test
    void findByAccountSortedByCreatedAtDesc() {
        List<Transaction> result = queryService.findByAccount("ACC-0001");

        for (int i = 0; i < result.size() - 1; i++) {
            assertTrue(result.get(i).getCreatedAt().isAfter(result.get(i + 1).getCreatedAt())
                    || result.get(i).getCreatedAt().equals(result.get(i + 1).getCreatedAt()));
        }
    }

    @Test
    void findByStatus() {
        List<Transaction> rejected = queryService.findByStatus(TransactionStatus.REJECTED);

        assertEquals(1, rejected.size());
        assertEquals(3L, rejected.get(0).getId());
    }

    @Test
    void findByType() {
        List<Transaction> debits = queryService.findByType(TransactionType.DEBIT);

        assertEquals(2, debits.size());
        assertTrue(debits.stream().allMatch(t -> t.getType() == TransactionType.DEBIT));
    }

    @Test
    void findSince() {
        Instant since = now.minus(2, ChronoUnit.HOURS).minusSeconds(1);
        List<Transaction> result = queryService.findSince(since);

        assertEquals(2, result.size());
    }

    @Test
    void sumAccepted() {
        BigDecimal sum = queryService.sumAccepted("ACC-0001");

        assertEquals(0, new BigDecimal("800.00").compareTo(sum));
    }
}
