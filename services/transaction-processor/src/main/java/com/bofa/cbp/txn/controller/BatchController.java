package com.bofa.cbp.txn.controller;

import com.bofa.cbp.txn.domain.TransactionRequest;
import com.bofa.cbp.txn.service.BatchSubmissionService;
import com.bofa.cbp.txn.service.BatchSubmissionService.BatchResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions/batch")
public class BatchController {

    private final BatchSubmissionService batch;

    public BatchController(BatchSubmissionService batch) {
        this.batch = batch;
    }

    @PostMapping
    public ResponseEntity<BatchResult> submit(@RequestBody List<TransactionRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (requests.size() > 1000) {
            return ResponseEntity.status(413).build();
        }
        return ResponseEntity.ok(batch.submitAll(requests));
    }
}
