package com.bofa.cbp.txn.service;

import com.bofa.cbp.txn.domain.TransactionRequest;
import com.bofa.cbp.txn.domain.TransactionResult;
import com.bofa.cbp.txn.domain.TransactionStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Accepts bulk transaction submissions. Each row is processed
 * independently — a failed row does not abort the batch.
 */
@Service
public class BatchSubmissionService {

    private final TransactionProcessor processor;

    public BatchSubmissionService(TransactionProcessor processor) {
        this.processor = processor;
    }

    public BatchResult submitAll(List<TransactionRequest> requests) {
        List<TransactionResult> results = new ArrayList<>(requests.size());
        int accepted = 0, rejected = 0, failed = 0;
        for (TransactionRequest req : requests) {
            try {
                TransactionResult r = processor.process(req);
                results.add(r);
                switch (r.getStatus()) {
                    case ACCEPTED -> accepted++;
                    case REJECTED -> rejected++;
                    case FAILED -> failed++;
                    default -> {}
                }
            } catch (RuntimeException e) {
                results.add(TransactionResult.failed("unhandled: " + e.getClass().getSimpleName()));
                failed++;
            }
        }
        return new BatchResult(requests.size(), accepted, rejected, failed, results);
    }

    public static BatchResult summarize(List<TransactionResult> rs) {
        int a = 0, r = 0, f = 0;
        for (TransactionResult x : rs) {
            if (x.getStatus() == TransactionStatus.ACCEPTED) a++;
            else if (x.getStatus() == TransactionStatus.REJECTED) r++;
            else if (x.getStatus() == TransactionStatus.FAILED) f++;
        }
        return new BatchResult(rs.size(), a, r, f, rs);
    }

    public record BatchResult(int total, int accepted, int rejected, int failed, List<TransactionResult> results) {}
}
