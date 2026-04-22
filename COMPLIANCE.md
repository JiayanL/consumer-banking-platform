# Compliance status — CBP monorepo

> Internal working document. Maintained by the platform compliance liaison
> in coordination with the service-owning engineering teams listed in
> `CODEOWNERS`. Not for external distribution.

## 1. Scope

This document tracks the current state of the Consumer Banking Platform
(CBP) monorepo against the control-point categories identified during the
examination prep meeting on 2025-Q4. It is intended to feed the regulator
pre-brief and the internal audit follow-up. It is not a substitute for the
formal findings register.

Examination scope, as agreed with the examiners' team, covers the
following control categories:

1. **TRANSACTION_INTEGRITY** — debit/credit posting, double-entry ledger,
   wire initiation, reversal handling.
2. **AUTHENTICATION** — token issuance, token validation, role-based
   access checks.
3. **PII_HANDLING** — tokenization of SSN / DOB / account numbers, masking
   and hashing in downstream reporting, KYC vendor lookups.
4. **AUDIT_TRAIL** — write-path for audit events, retention, forwarding
   to the downstream archive.

Code paths in scope are annotated in-source with `@ComplianceCritical`
(Java), the `@compliance-critical` JSDoc tag (TypeScript), or the
`@compliance_critical` decorator (Python). The annotation list is the
source of truth; this document rolls them up.

## 2. Coverage status (most recent local run)

Coverage figures below are instruction-level for Java (JaCoCo), statement-
level for TypeScript (Istanbul/nyc), and line-level for Python (coverage.py).
They reflect the merge of the `devin/*` feature branches into `main`.

| Service                    | Language  | Coverage | Target | Notes |
|---                         |---        |---:      |---:    |--- |
| account-service            | Java      | 34.0%    | ~35%   | |
| ledger-service             | Java      | 33.5%    | ~33%   | Testcontainers suite aspirational (disabled). |
| wire-transfer-service      | Java      | 31.7%    | ~29%   | Still on Spring Boot 2.7. Migration PLAT-1440. |
| auth-service               | Java      | 39.6%    | ~40%   | |
| transaction-processor      | Java      | 18.1%    | ~18%   | Priority uplift service — see §4. |
| audit-logger               | Java      | 46.2%    | —      | |
| notification-service       | TS        | 41.7%    | ~40%   | |
| customer-profile-api       | TS        | 27.4%    | ~28%   | One test file currently `describe.skip`. |
| api-gateway                | TS        | 22.7%    | —      | |
| session-manager            | TS        | 30.0%    | ~30%   | Flake observed in idle-reaper test — see §4. |
| kyc-enrichment             | Python    | 31.0%    | ~32%   | |
| reporting-aggregator       | Python    | 22.0%    | ~25%   | Lower end of tolerance. |
| pii-tokenization-service   | Python    | **0%**   | —      | **No test infrastructure present.** See §4. |

Per-category roll-up (weighted by instrumented units within each service's
tagged methods only — see `tools/compliance_map.json` for the service
membership and the `@ComplianceCritical` annotations for the method-level
membership):

| Category               | Coverage (approx.) |
|---                     |---:                |
| TRANSACTION_INTEGRITY  | ~27%               |
| AUTHENTICATION         | ~36%               |
| PII_HANDLING           | ~18%               |
| AUDIT_TRAIL            | ~46%               |

## 3. Annotation inventory

The `@ComplianceCritical` annotation is defined in:

- `libs/common-auth/src/main/java/com/bofa/cbp/auth/compliance/ComplianceCritical.java`
- `libs/logging-sdk/src/compliance.ts` (JSDoc convention)
- `libs/pii-utils/src/pii_utils/compliance.py` (runtime no-op decorator)

Annotations are applied to methods/functions on the control-point path —
not to supporting, transport, or diagnostic code. The dashboard (pending,
see `tools/README.md`) will enumerate them once built; until then, the
authoritative way to find them is `git grep`:

```bash
git grep -n '@ComplianceCritical'     # Java
git grep -n '@compliance-critical'    # TypeScript
git grep -n '@compliance_critical'    # Python
```

## 4. Open findings and gaps

The following items are known gaps. They are tracked here because they
will likely be visible to the examiners; each has an internal ticket.

1. **PLAT-1871 — `pii-tokenization-service` has no test infrastructure.**
   The service was stood up as a greenfield project, shipped with a
   functional tokenize/detokenize API backed by the stub HSM, and never
   had pytest, tests/, or a CI workflow added. Coverage is 0% and
   `@compliance_critical` methods on `tokenize()` and `detokenize()` are
   entirely uncovered. This is the single largest gap in the PII_HANDLING
   category. Owner: @bofa/data-security.

2. **PLAT-1730 — `transaction-processor` coverage is 18.1%.**
   The `process()`, fraud evaluation, limits, velocity, and reversal paths
   are all annotated but thinly tested. The one unit test that does exist
   only exercises the debit happy-path. Regulator-facing controls on the
   TRANSACTION_INTEGRITY category depend on uplifting this. Owner:
   @bofa/payments-team.

3. **PLAT-1730-a — concurrency behaviour of `transaction-processor`
   is unverified.** The service relies on an in-process `LedgerClient`
   stub whose balance updates are not guarded by a per-account lock; we
   have no load or concurrency tests that would surface this. Owner:
   @bofa/payments-team.

4. **PLAT-1808 — `audit-logger` swallows downstream forwarding failures.**
   `AuditWriter.write()` catches all exceptions from the forwarding sink
   and returns successfully. The original intent was resilience (the
   downstream archive is flaky), but the current implementation means a
   persistent forwarder outage would go undetected by any existing test
   or alert. Owner: @bofa/observability.

5. **PLAT-1912 — JWT `exp` semantics disagree across languages.**
   `libs/common-auth` (Java) treats `exp == now` as **expired**;
   `session-manager` (TypeScript) treats `exp == now` as **valid**. No
   cross-boundary test has been written. Intermittent auth failures at
   exactly the boundary are possible in production. Owner:
   @bofa/identity-platform + @bofa/platform-security.

6. **PLAT-1871-b — `session-manager` idle-reaper test is timing
   dependent.** The `reapIdle()` test in
   `services/session-manager/src/__tests__/idleReap.test.ts` uses real
   wall-clock timing with no margin; we have observed intermittent
   failures. A mocked clock attempt was abandoned. Owner:
   @bofa/identity-platform.

7. **PLAT-1440 — `wire-transfer-service` still on Spring Boot 2.7.**
   Tracked migration; not a compliance issue in itself but noted so the
   examiners don't flag the version skew themselves.

## 5. Planned work

Not a commitment register — those live in Jira. Directional only:

- Raise `transaction-processor` TRANSACTION_INTEGRITY coverage through
  concurrency and edge-case tests.
- Stand up a test harness for `pii-tokenization-service`, including
  input-validation tests.
- Reconcile JWT `exp` semantics between `common-auth` and
  `session-manager` with a shared contract test.
- Replace the bare `catch (Exception e)` in `AuditWriter.write()` with
  explicit, observable failure handling.
- Wire the coverage dashboard so per-category rollups stop being
  hand-computed.

## 6. Document ownership

This document is maintained by the platform compliance liaison in
coordination with the service owners listed in `CODEOWNERS`. Numbers are
refreshed on each push to `main` by the `coverage-aggregation` workflow;
until the dashboard lands, the rollup table above is updated by hand
from that artifact.
