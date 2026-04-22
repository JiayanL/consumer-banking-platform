package com.bofa.cbp.wire;

import com.bofa.cbp.wire.domain.WireStatus;
import com.bofa.cbp.wire.domain.WireTransfer;
import com.bofa.cbp.wire.service.WireTransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class WireTransferServiceTest {

    @Autowired
    private WireTransferService service;

    @Test
    void happyPathInitiateAndTransitionStatus() {
        WireTransfer w = service.initiate(
                "ACC-1",
                "Jane Beneficiary",
                "DE89370400440532013000",
                new BigDecimal("1234.56"),
                "USD"
        );
        assertNotNull(w.getId());
        assertEquals(WireStatus.INITIATED, w.getStatus());
        assertNotNull(w.getReferenceNumber());
        assertNotNull(w.getInitiatedAt());

        WireTransfer pending = service.markPending(w.getId());
        assertEquals(WireStatus.PENDING, pending.getStatus());
    }
}
