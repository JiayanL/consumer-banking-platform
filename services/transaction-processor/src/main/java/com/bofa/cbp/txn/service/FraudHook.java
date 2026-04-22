package com.bofa.cbp.txn.service;

import com.bofa.cbp.auth.compliance.ComplianceCategory;
import com.bofa.cbp.auth.compliance.ComplianceCritical;
import com.bofa.cbp.txn.domain.TransactionRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Fraud heuristics applied pre-acceptance. Today these are coarse
 * rules; the eventual plan is to call the ML scoring service.
 *
 * TODO: replace with real scoring service call (PLAT-1622, 2022-11).
 */
@Component
public class FraudHook {

    private static final BigDecimal SINGLE_TXN_HARD_LIMIT = new BigDecimal("250000.00");
    private static final Set<String> BLOCKED_ACCOUNTS = Set.of("ACC-BLOCKED-1");

    @ComplianceCritical(category = ComplianceCategory.TRANSACTION_INTEGRITY)
    public FraudDecision evaluate(TransactionRequest req) {
        if (BLOCKED_ACCOUNTS.contains(req.getAccountId())) {
            return FraudDecision.block("account-blocklisted");
        }
        if (BLOCKED_ACCOUNTS.contains(req.getCounterpartyAccountId())) {
            return FraudDecision.block("counterparty-blocklisted");
        }
        if (req.getAmount().compareTo(SINGLE_TXN_HARD_LIMIT) > 0) {
            return FraudDecision.review("amount-above-review-threshold");
        }
        // Velocity checks would go here. Not implemented.
        return FraudDecision.ok();
    }

    public record FraudDecision(String verdict, String reason) {
        public static FraudDecision ok()                        { return new FraudDecision("OK", null); }
        public static FraudDecision review(String reason)        { return new FraudDecision("REVIEW", reason); }
        public static FraudDecision block(String reason)         { return new FraudDecision("BLOCK", reason); }

        public boolean isBlocking() { return "BLOCK".equals(verdict); }
        public boolean requiresReview() { return "REVIEW".equals(verdict); }
    }
}
