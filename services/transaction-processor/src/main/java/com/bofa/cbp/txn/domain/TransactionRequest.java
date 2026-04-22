package com.bofa.cbp.txn.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequest {
    @NotBlank
    private String accountId;
    @NotBlank
    private String counterpartyAccountId;
    @NotNull
    private TransactionType type;
    @NotNull
    @Positive
    private BigDecimal amount;
    @NotBlank
    private String currency;
    private String idempotencyKey;
}
