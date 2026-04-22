# Architecture

High-level topology of the consumer banking platform. This document is
maintained on a best-effort basis; service READMEs are the authoritative
source for individual service behavior.

## Request shape

External traffic enters through `api-gateway`, which terminates TLS
(handled upstream in production) and performs coarse auth checks
against `auth-service` via the `common-auth` library. Downstream
services are reached directly inside the cluster.

```
                +---------------------+
   client  -->  |     api-gateway     |  --> auth-service
                +---------------------+
                          |
            +-------------+-------------+
            |             |             |
   account-service  transaction-     wire-transfer-
                    processor        service
            |             |             |
            +-------------+-------------+
                          |
                    ledger-service
                          |
                    audit-logger
```

`notification-service`, `customer-profile-api`, `session-manager`,
`kyc-enrichment`, and `reporting-aggregator` are reached from
`api-gateway` directly. `pii-tokenization-service` is an internal-only
service — only `kyc-enrichment`, `reporting-aggregator`, and
`customer-profile-api` are allowed to call it.

## Language choices

- **Java (Spring Boot)** for the transactional core: account,
  transaction-processor, ledger, wire-transfer, auth, audit-logger.
  History: the original platform was a single Spring monolith; services
  were carved out over the 2019–2022 decomposition program and most
  still use Spring Boot 3.x. `wire-transfer-service` is still on 2.7
  pending a scheduled upgrade (tracked in PLAT-1841).
- **TypeScript (Node)** for API and orchestration surfaces:
  api-gateway, notification-service, session-manager, customer-profile
  API.
- **Python** for data-adjacent services: kyc-enrichment,
  reporting-aggregator, and the newer pii-tokenization-service.

## Shared libraries

- `libs/common-auth` (Java): JWT validation, role enforcement, and the
  `@ComplianceCritical` annotation.
- `libs/logging-sdk` (TypeScript): structured logging with a JSDoc
  `@compliance-critical` convention the coverage tooling parses.
- `libs/pii-utils` (Python): masking, hashing, format validation, and
  the `@compliance_critical` decorator.

## Data stores

For this checkout, every service uses an in-memory or embedded store
(H2, SQLite, in-process dicts/maps). The production deployment uses
PostgreSQL clusters managed by the Data Platform team; see the infra
repo.

## What is explicitly out of scope here

- Production secret material. Services ship with dev-only placeholders.
- Real HSM integration for `pii-tokenization-service` (stubbed).
- Kafka / outbox wiring (transaction-processor emits events to an
  in-process sink).
