package com.bofa.cbp.wire.service;

import com.bofa.cbp.auth.compliance.ComplianceCategory;
import com.bofa.cbp.auth.compliance.ComplianceCritical;
import com.bofa.cbp.wire.domain.WireStatus;
import com.bofa.cbp.wire.domain.WireTransfer;
import com.bofa.cbp.wire.domain.WireTransferRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class WireTransferService {

    @Autowired
    private WireTransferRepository repository;

    // TODO: add OFAC screening before wire execution — tracked in PLAT-1840
    @ComplianceCritical(
        category = ComplianceCategory.TRANSACTION_INTEGRITY,
        note = "Validates wire inputs (amount > 0, currency, non-empty beneficiary) before persistence."
    )
    public WireTransfer initiate(String senderAccount,
                                 String beneficiaryName,
                                 String beneficiaryAccount,
                                 BigDecimal amount,
                                 String currency) {
        if (senderAccount == null || senderAccount.isBlank()) {
            throw new IllegalArgumentException("senderAccount required");
        }
        if (beneficiaryName == null || beneficiaryName.isBlank()) {
            throw new IllegalArgumentException("beneficiaryName required");
        }
        if (beneficiaryAccount == null || beneficiaryAccount.isBlank()) {
            throw new IllegalArgumentException("beneficiaryAccount required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
        if (currency == null || currency.length() != 3) {
            throw new IllegalArgumentException("currency must be ISO-4217 3-letter code");
        }

        WireTransfer wire = new WireTransfer();
        wire.setSenderAccount(senderAccount);
        wire.setBeneficiaryName(beneficiaryName);
        wire.setBeneficiaryAccount(beneficiaryAccount);
        wire.setAmount(amount);
        wire.setCurrency(currency.toUpperCase());
        wire.setStatus(WireStatus.INITIATED);
        wire.setReferenceNumber("WIRE-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        wire.setInitiatedAt(Instant.now());
        return repository.save(wire);
    }

    public WireTransfer getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new WireNotFoundException(id));
    }

    public List<WireTransfer> findByStatus(WireStatus status) {
        return repository.findAllByStatus(status);
    }

    public WireTransfer markPending(Long id) {
        WireTransfer wire = getById(id);
        if (wire.getStatus() != WireStatus.INITIATED) {
            throw new IllegalStateException("only INITIATED wires can be moved to PENDING");
        }
        wire.setStatus(WireStatus.PENDING);
        return repository.save(wire);
    }

    public WireTransfer settle(Long id) {
        WireTransfer wire = getById(id);
        if (wire.getStatus() != WireStatus.PENDING) {
            throw new IllegalStateException("only PENDING wires can settle");
        }
        wire.setStatus(WireStatus.SETTLED);
        return repository.save(wire);
    }

    public WireTransfer cancel(Long id) {
        WireTransfer wire = getById(id);
        if (wire.getStatus() == WireStatus.SETTLED) {
            throw new IllegalStateException("cannot cancel a settled wire");
        }
        wire.setStatus(WireStatus.REJECTED);
        return repository.save(wire);
    }
}
