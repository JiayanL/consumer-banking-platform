package com.bofa.cbp.txn.repository;

import com.bofa.cbp.txn.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Optional<Transaction> findFirstByIdempotencyKey(String idempotencyKey);
}
