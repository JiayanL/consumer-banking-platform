/**
 * OCC-mapped compliance categories. Kept in lockstep with the
 * equivalent enums on the Java and Python sides.
 *
 * The coverage dashboard parses JSDoc `@compliance-critical <CATEGORY>`
 * annotations from source files under services/ and libs/ and expects
 * the CATEGORY token to match one of these values.
 */
export const ComplianceCategory = {
  TRANSACTION_INTEGRITY: 'TRANSACTION_INTEGRITY',
  AUTHENTICATION: 'AUTHENTICATION',
  PII_HANDLING: 'PII_HANDLING',
  AUDIT_TRAIL: 'AUDIT_TRAIL',
} as const;

export type ComplianceCategory =
  (typeof ComplianceCategory)[keyof typeof ComplianceCategory];
