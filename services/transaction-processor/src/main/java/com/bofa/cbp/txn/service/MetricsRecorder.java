package com.bofa.cbp.txn.service;

import com.bofa.cbp.txn.domain.TransactionStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lightweight in-process counters. Production runs export via
 * Micrometer to the observability stack, but for local tests we keep
 * a pluggable in-memory version so assertions don't need the full
 * Micrometer test harness.
 *
 * TODO: replace with MeterRegistry once payments-team finalizes the
 * metric naming conventions (PLAT-1490).
 */
@Component
public class MetricsRecorder {

    private final Map<TransactionStatus, AtomicLong> counters = new EnumMap<>(TransactionStatus.class);
    private final AtomicLong totalProcessed = new AtomicLong();
    private final AtomicLong totalRejected = new AtomicLong();

    public MetricsRecorder() {
        for (TransactionStatus s : TransactionStatus.values()) {
            counters.put(s, new AtomicLong());
        }
    }

    public void record(TransactionStatus status) {
        counters.get(status).incrementAndGet();
        totalProcessed.incrementAndGet();
        if (status == TransactionStatus.REJECTED || status == TransactionStatus.FAILED) {
            totalRejected.incrementAndGet();
        }
    }

    public long count(TransactionStatus status) {
        return counters.get(status).get();
    }

    public long processed() { return totalProcessed.get(); }
    public long rejected() { return totalRejected.get(); }

    public double rejectionRate() {
        long total = totalProcessed.get();
        if (total == 0) return 0.0;
        return (double) totalRejected.get() / total;
    }

    public void reset() {
        counters.values().forEach(a -> a.set(0));
        totalProcessed.set(0);
        totalRejected.set(0);
    }
}
