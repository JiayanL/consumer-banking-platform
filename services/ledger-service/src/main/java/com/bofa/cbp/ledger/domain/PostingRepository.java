package com.bofa.cbp.ledger.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostingRepository extends JpaRepository<Posting, Long> {
    List<Posting> findAllByAccountId(String accountId);
}
