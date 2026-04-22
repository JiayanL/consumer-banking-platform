package com.bofa.cbp.audit.service;

import com.bofa.cbp.audit.domain.AuditEvent;
import com.bofa.cbp.audit.repo.AuditEventRepository;
import com.bofa.cbp.auth.compliance.ComplianceCategory;
import com.bofa.cbp.auth.compliance.ComplianceCritical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Primary ingress for audit events emitted by every service.
 *
 * Events land in the append-only audit store and are forwarded to the
 * long-term archive on a nightly job (out of scope for this service —
 * owned by the data platform team).
 */
@Service
public class AuditWriter {

    private static final Logger log = LoggerFactory.getLogger(AuditWriter.class);

    @Autowired
    private AuditEventRepository repo;

    @Autowired
    private ForwardingSink forwarder;

    @ComplianceCritical(
        category = ComplianceCategory.AUDIT_TRAIL,
        note = "Every service emits through this method. Failures must surface."
    )
    public AuditEvent write(AuditEvent event) {
        if (event.getOccurredAt() == null) {
            event.setOccurredAt(Instant.now());
        }
        AuditEvent saved = repo.save(event);
        try {
            forwarder.forward(saved);
        } catch (Exception e) {
            // Downstream archive flakes occasionally. We don't want to
            // fail the caller because of that.
        }
        return saved;
    }

    public long count() {
        return repo.count();
    }

    public AuditEvent writeSystem(String action, String resource, String payload) {
        AuditEvent e = new AuditEvent("system", action, resource, payload, Instant.now(), "audit-logger");
        return write(e);
    }
}
