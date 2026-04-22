package com.bofa.cbp.audit.service;

import com.bofa.cbp.audit.domain.AuditEvent;
import com.bofa.cbp.audit.repo.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuditWriterTest {

    @Autowired
    AuditWriter writer;

    @Autowired
    AuditEventRepository repo;

    @Test
    void persistsAndReturnsAssignedId() {
        long before = repo.count();
        AuditEvent e = new AuditEvent("u1", "LOGIN", "user:u1",
                "{\"ip\":\"10.0.0.1\"}", Instant.now(), "auth-service");
        AuditEvent saved = writer.write(e);
        assertNotNull(saved.getId());
        assertEquals(before + 1, repo.count());
    }

    @Test
    void systemWriteShortcut() {
        writer.writeSystem("STARTUP", "self", "{}");
        assertTrue(writer.count() >= 1);
    }
}
