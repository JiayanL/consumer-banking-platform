# transaction-processor

Accepts transaction requests, validates (idempotency, limits, fraud
hooks), debits/credits via ledger-service, persists, and emits events.

Sits on the primary money movement path. Treat changes here as
payments-team-reviewed by default.

## Run

```bash
(cd ../../libs/common-auth && mvn -q -DskipTests install)
mvn spring-boot:run
```

Service listens on `:8082`. H2 is in-memory and reset on restart.

## Test + coverage

```bash
mvn test
# JaCoCo report: target/site/jacoco/index.html
```

Current measured INSTRUCTION coverage: **~18%**. This number is low by
design for the current exam-prep cycle; see `COMPLIANCE.md`.

## Compliance annotations

`@ComplianceCritical(TRANSACTION_INTEGRITY)` on the primary processing
and reversal paths. Be strict about adding new ones here — the coverage
delta bot will force any new annotated path to have accompanying tests
or fail the PR.
