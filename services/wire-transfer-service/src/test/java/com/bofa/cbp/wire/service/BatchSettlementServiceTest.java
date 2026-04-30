package com.bofa.cbp.wire.service;

import com.bofa.cbp.wire.domain.WireStatus;
import com.bofa.cbp.wire.domain.WireTransfer;
import com.bofa.cbp.wire.domain.WireTransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BatchSettlementServiceTest {

    private WireTransferRepository repo;
    private WireEventPublisher publisher;
    private BatchSettlementService batch;

    @BeforeEach
    void setUp() {
        repo = mock(WireTransferRepository.class);
        publisher = new WireEventPublisher();
        batch = new BatchSettlementService();
        ReflectionTestUtils.setField(batch, "repository", repo);
        ReflectionTestUtils.setField(batch, "publisher", publisher);
        when(repo.save(any(WireTransfer.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void promoteInitiatedToPending() {
        WireTransfer w = buildWire(1L, WireStatus.INITIATED);
        when(repo.findAllByStatus(WireStatus.INITIATED)).thenReturn(List.of(w));

        List<WireTransfer> result = batch.promoteInitiatedToPending();

        assertEquals(1, result.size());
        assertEquals(WireStatus.PENDING, result.get(0).getStatus());
        assertFalse(publisher.drain().isEmpty());
    }

    @Test
    void settlePendingWires() {
        WireTransfer w = buildWire(2L, WireStatus.PENDING);
        when(repo.findAllByStatus(WireStatus.PENDING)).thenReturn(List.of(w));

        List<WireTransfer> result = batch.settlePending();

        assertEquals(1, result.size());
        assertEquals(WireStatus.SETTLED, result.get(0).getStatus());
    }

    @Test
    void emptyListProducesNoPromotions() {
        when(repo.findAllByStatus(WireStatus.INITIATED)).thenReturn(List.of());
        List<WireTransfer> result = batch.promoteInitiatedToPending();
        assertTrue(result.isEmpty());
    }

    private WireTransfer buildWire(Long id, WireStatus status) {
        WireTransfer w = new WireTransfer();
        w.setId(id);
        w.setSenderAccount("ACC-1");
        w.setBeneficiaryName("Jane");
        w.setBeneficiaryAccount("DE89370400440532013000");
        w.setAmount(new BigDecimal("500.00"));
        w.setCurrency("USD");
        w.setStatus(status);
        w.setReferenceNumber("WIRE-TEST");
        w.setInitiatedAt(Instant.now());
        return w;
    }
}
