package com.bofa.cbp.wire.service;

import com.bofa.cbp.wire.domain.WireStatus;
import com.bofa.cbp.wire.domain.WireTransfer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WireEventPublisherTest {

    private WireEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new WireEventPublisher();
    }

    @Test
    void publishInitiatedAddsEvent() {
        publisher.publishInitiated(buildWire());
        List<String> events = publisher.peek();
        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("INITIATED"));
    }

    @Test
    void publishStatusChangeAddsEvent() {
        publisher.publishStatusChange(buildWire(), "INITIATED");
        List<String> events = publisher.peek();
        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("STATUS"));
        assertTrue(events.get(0).contains("INITIATED"));
    }

    @Test
    void publishCancelledAddsEvent() {
        publisher.publishCancelled(buildWire(), "user-request");
        List<String> events = publisher.peek();
        assertEquals(1, events.size());
        assertTrue(events.get(0).contains("CANCELLED"));
        assertTrue(events.get(0).contains("user-request"));
    }

    @Test
    void drainClearsBuffer() {
        publisher.publishInitiated(buildWire());
        List<String> drained = publisher.drain();
        assertEquals(1, drained.size());
        assertTrue(publisher.peek().isEmpty());
    }

    @Test
    void peekDoesNotClearBuffer() {
        publisher.publishInitiated(buildWire());
        publisher.peek();
        assertEquals(1, publisher.peek().size());
    }

    private WireTransfer buildWire() {
        WireTransfer w = new WireTransfer();
        w.setId(1L);
        w.setSenderAccount("ACC-1");
        w.setBeneficiaryName("Jane");
        w.setBeneficiaryAccount("DE89370400440532013000");
        w.setAmount(new BigDecimal("100.00"));
        w.setCurrency("USD");
        w.setStatus(WireStatus.INITIATED);
        w.setReferenceNumber("WIRE-TEST-001");
        w.setInitiatedAt(Instant.now());
        return w;
    }
}
