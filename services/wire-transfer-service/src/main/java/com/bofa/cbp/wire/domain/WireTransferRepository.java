package com.bofa.cbp.wire.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WireTransferRepository extends JpaRepository<WireTransfer, Long> {
    List<WireTransfer> findAllByStatus(WireStatus status);
}
