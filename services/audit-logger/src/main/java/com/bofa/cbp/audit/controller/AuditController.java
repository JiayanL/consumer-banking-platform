package com.bofa.cbp.audit.controller;

import com.bofa.cbp.audit.domain.AuditEvent;
import com.bofa.cbp.audit.repo.AuditEventRepository;
import com.bofa.cbp.audit.service.AuditWriter;
import com.bofa.cbp.auth.compliance.ComplianceCategory;
import com.bofa.cbp.auth.compliance.ComplianceCritical;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/audit")
public class AuditController {

    private final AuditWriter writer;
    private final AuditEventRepository repo;

    public AuditController(AuditWriter writer, AuditEventRepository repo) {
        this.writer = writer;
        this.repo = repo;
    }

    @ComplianceCritical(category = ComplianceCategory.AUDIT_TRAIL)
    @PostMapping("/events")
    public ResponseEntity<AuditEvent> submit(@RequestBody AuditEvent event) {
        if (event.getActor() == null || event.getAction() == null || event.getResource() == null) {
            return ResponseEntity.badRequest().build();
        }
        AuditEvent saved = writer.write(event);
        return ResponseEntity.status(201).body(saved);
    }

    @GetMapping("/events")
    public List<AuditEvent> list(@RequestParam(required = false) String actor,
                                  @RequestParam(required = false) String sourceService) {
        if (actor != null) return repo.findByActor(actor);
        if (sourceService != null) return repo.findBySourceService(sourceService);
        return repo.findAll();
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<AuditEvent> get(@PathVariable Long id) {
        return repo.findById(id).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/count")
    public long count() {
        return writer.count();
    }
}
