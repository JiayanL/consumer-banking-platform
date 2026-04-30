package com.bofa.cbp.wire;

import com.bofa.cbp.wire.domain.WireStatus;
import com.bofa.cbp.wire.domain.WireTransfer;
import com.bofa.cbp.wire.service.WireNotFoundException;
import com.bofa.cbp.wire.service.WireTransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void fullLifecycleInitiatedToPendingToSettled() {
        WireTransfer w = service.initiate("ACC-1", "Beneficiary", "IBAN-1",
                new BigDecimal("500.00"), "USD");
        service.markPending(w.getId());
        WireTransfer settled = service.settle(w.getId());
        assertEquals(WireStatus.SETTLED, settled.getStatus());
    }

    @Test
    void cancellationFlow() {
        WireTransfer w = service.initiate("ACC-1", "Beneficiary", "IBAN-1",
                new BigDecimal("500.00"), "USD");
        WireTransfer cancelled = service.cancel(w.getId());
        assertEquals(WireStatus.REJECTED, cancelled.getStatus());
    }

    @Test
    void cannotCancelSettledWire() {
        WireTransfer w = service.initiate("ACC-1", "Beneficiary", "IBAN-1",
                new BigDecimal("500.00"), "USD");
        service.markPending(w.getId());
        service.settle(w.getId());
        assertThrows(IllegalStateException.class, () -> service.cancel(w.getId()));
    }

    @Test
    void nullBeneficiaryThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                service.initiate("ACC-1", null, "IBAN-1", new BigDecimal("100.00"), "USD"));
    }

    @Test
    void negativeAmountThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                service.initiate("ACC-1", "Ben", "IBAN-1", new BigDecimal("-50.00"), "USD"));
    }

    @Test
    void zeroAmountThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                service.initiate("ACC-1", "Ben", "IBAN-1", BigDecimal.ZERO, "USD"));
    }

    @Test
    void invalidCurrencyThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                service.initiate("ACC-1", "Ben", "IBAN-1", new BigDecimal("100.00"), "US"));
    }

    @Test
    void blankSenderAccountThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                service.initiate("", "Ben", "IBAN-1", new BigDecimal("100.00"), "USD"));
    }

    @Test
    void nonExistentWireThrowsNotFoundException() {
        assertThrows(WireNotFoundException.class, () -> service.getById(999999L));
    }

    @Test
    void markPendingOnNonInitiatedThrows() {
        WireTransfer w = service.initiate("ACC-1", "Ben", "IBAN-1",
                new BigDecimal("100.00"), "USD");
        service.markPending(w.getId());
        assertThrows(IllegalStateException.class, () -> service.markPending(w.getId()));
    }

    @Test
    void settleOnNonPendingThrows() {
        WireTransfer w = service.initiate("ACC-1", "Ben", "IBAN-1",
                new BigDecimal("100.00"), "USD");
        assertThrows(IllegalStateException.class, () -> service.settle(w.getId()));
    }
}
