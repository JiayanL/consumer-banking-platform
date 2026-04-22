package com.bofa.cbp.txn.service;

import com.bofa.cbp.txn.domain.Transaction;
import com.bofa.cbp.txn.domain.TransactionStatus;
import com.bofa.cbp.txn.domain.TransactionType;
import com.bofa.cbp.txn.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * Read-side queries over the transaction store. Kept separate from the
 * processor so that heavy reporting paths don't sit behind the same
 * service that accepts traffic.
 */
@Service
public class TransactionQueryService {

    private final TransactionRepository transactions;

    public TransactionQueryService(TransactionRepository transactions) {
        this.transactions = transactions;
    }

    public List<Transaction> findByAccount(String accountId) {
        return transactions.findAll().stream()
                .filter(t -> accountId.equals(t.getAccountId()))
                .sorted(Comparator.comparing(Transaction::getCreatedAt).reversed())
                .toList();
    }

    public List<Transaction> findByStatus(TransactionStatus status) {
        return transactions.findAll().stream()
                .filter(t -> t.getStatus() == status)
                .toList();
    }

    public List<Transaction> findByType(TransactionType type) {
        return transactions.findAll().stream()
                .filter(t -> t.getType() == type)
                .toList();
    }

    public List<Transaction> findSince(Instant since) {
        return transactions.findAll().stream()
                .filter(t -> t.getCreatedAt().isAfter(since))
                .toList();
    }

    public BigDecimal sumAccepted(String accountId) {
        return transactions.findAll().stream()
                .filter(t -> accountId.equals(t.getAccountId()))
                .filter(t -> t.getStatus() == TransactionStatus.ACCEPTED)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
