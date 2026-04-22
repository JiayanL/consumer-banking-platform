package com.bofa.cbp.wire.service;

import com.bofa.cbp.wire.domain.WireStatus;
import com.bofa.cbp.wire.domain.WireTransfer;
import com.bofa.cbp.wire.domain.WireTransferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Placeholder nightly-batch hook. The real implementation will be
 * driven by the settlement scheduler; this class exists so the batch
 * contract can be wired up early without blocking on the scheduler
 * work. Not covered by tests.
 */
@Service
public class BatchSettlementService {

    @Autowired
    private WireTransferRepository repository;

    @Autowired
    private WireEventPublisher publisher;

    public List<WireTransfer> promoteInitiatedToPending() {
        List<WireTransfer> promoted = new ArrayList<>();
        for (WireTransfer w : repository.findAllByStatus(WireStatus.INITIATED)) {
            w.setStatus(WireStatus.PENDING);
            WireTransfer saved = repository.save(w);
            publisher.publishStatusChange(saved, "INITIATED");
            promoted.add(saved);
        }
        return promoted;
    }

    public List<WireTransfer> settlePending() {
        List<WireTransfer> settled = new ArrayList<>();
        for (WireTransfer w : repository.findAllByStatus(WireStatus.PENDING)) {
            w.setStatus(WireStatus.SETTLED);
            WireTransfer saved = repository.save(w);
            publisher.publishStatusChange(saved, "PENDING");
            settled.add(saved);
        }
        return settled;
    }

    public int rejectOlderThan(long maxAgeMillis) {
        long now = System.currentTimeMillis();
        int rejected = 0;
        for (WireTransfer w : repository.findAllByStatus(WireStatus.INITIATED)) {
            long age = now - (w.getInitiatedAt() == null ? now : w.getInitiatedAt().toEpochMilli());
            if (age > maxAgeMillis) {
                w.setStatus(WireStatus.REJECTED);
                WireTransfer saved = repository.save(w);
                publisher.publishCancelled(saved, "aged-out");
                rejected++;
            }
        }
        return rejected;
    }
}
