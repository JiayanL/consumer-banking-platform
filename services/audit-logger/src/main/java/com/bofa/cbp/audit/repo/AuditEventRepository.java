package com.bofa.cbp.audit.repo;

import com.bofa.cbp.audit.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByActor(String actor);
    List<AuditEvent> findBySourceService(String sourceService);
}
