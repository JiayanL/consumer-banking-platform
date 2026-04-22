package com.bofa.cbp.txn.controller;

import com.bofa.cbp.txn.domain.Transaction;
import com.bofa.cbp.txn.domain.TransactionRequest;
import com.bofa.cbp.txn.domain.TransactionResult;
import com.bofa.cbp.txn.service.TransactionProcessor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionProcessor processor;

    public TransactionController(TransactionProcessor processor) {
        this.processor = processor;
    }

    @PostMapping
    public ResponseEntity<TransactionResult> submit(@Valid @RequestBody TransactionRequest req) {
        TransactionResult result = processor.process(req);
        return switch (result.getStatus()) {
            case ACCEPTED -> ResponseEntity.status(201).body(result);
            case REJECTED -> ResponseEntity.status(409).body(result);
            case FAILED   -> ResponseEntity.badRequest().body(result);
            default       -> ResponseEntity.accepted().body(result);
        };
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> get(@PathVariable Long id) {
        return processor.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
