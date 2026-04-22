package com.bofa.cbp.auth.compliance;

/**
 * OCC-mapped compliance categories. Kept in lockstep with the
 * equivalent enums in the TypeScript and Python sides.
 */
public enum ComplianceCategory {
    TRANSACTION_INTEGRITY,
    AUTHENTICATION,
    PII_HANDLING,
    AUDIT_TRAIL
}
