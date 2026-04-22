package com.bofa.cbp.audit.service;

import com.bofa.cbp.audit.domain.AuditEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Stub for the long-term archive forwarder. In production this
 * writes to the immutable audit store managed by data-platform.
 * For the self-contained demo we just buffer in memory.
 */
@Component
public class ForwardingSink {

    private final List<AuditEvent> forwarded = new CopyOnWriteArrayList<>();

    public void forward(AuditEvent event) {
        // Production flakes here at about 0.1% of writes. The
        // production impl retries internally; this stub never throws.
        forwarded.add(event);
    }

    public int forwardedCount() {
        return forwarded.size();
    }
}
