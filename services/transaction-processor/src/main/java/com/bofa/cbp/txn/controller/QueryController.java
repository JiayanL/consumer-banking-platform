package com.bofa.cbp.txn.controller;

import com.bofa.cbp.txn.domain.Transaction;
import com.bofa.cbp.txn.domain.TransactionStatus;
import com.bofa.cbp.txn.domain.TransactionType;
import com.bofa.cbp.txn.service.TransactionQueryService;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/transactions/query")
public class QueryController {

    private final TransactionQueryService query;

    public QueryController(TransactionQueryService query) {
        this.query = query;
    }

    @GetMapping("/by-account/{accountId}")
    public List<Transaction> byAccount(@PathVariable String accountId) {
        return query.findByAccount(accountId);
    }

    @GetMapping("/by-status")
    public List<Transaction> byStatus(@RequestParam TransactionStatus status) {
        return query.findByStatus(status);
    }

    @GetMapping("/by-type")
    public List<Transaction> byType(@RequestParam TransactionType type) {
        return query.findByType(type);
    }

    @GetMapping("/since")
    public List<Transaction> since(@RequestParam Instant at) {
        return query.findSince(at);
    }
}
