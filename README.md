# consumer-banking-platform

Internal monorepo for the consumer banking platform. Contains the
transactional services, supporting APIs, and shared libraries that back
the retail banking product line.

This repo is owned by the Platform Engineering org. Service-level
ownership is declared in [`CODEOWNERS`](./CODEOWNERS).

## Layout

```
services/        # deployable services (Java / TypeScript / Python)
libs/            # shared libraries, per language
test-utils/      # cross-language test scaffolding (WIP)
tools/           # internal tooling
docs/            # architecture + standards
.github/         # CI workflows
```

See [`docs/architecture.md`](./docs/architecture.md) for the high-level
topology and [`docs/testing-standards.md`](./docs/testing-standards.md)
for the (evolving) testing expectations.

Compliance posture and the current OCC examination prep status live in
[`COMPLIANCE.md`](./COMPLIANCE.md).

## Building things locally

Each service owns its own build. There is deliberately no top-level
orchestrator — run builds from inside the service directory.

| Language   | Typical commands                                   |
|------------|----------------------------------------------------|
| Java       | `mvn test`, `mvn verify` (JaCoCo report in target) |
| TypeScript | `npm install && npm test` (coverage via nyc/jest)  |
| Python     | `pip install -e .[dev] && pytest --cov`            |

CI mirrors these commands. See `.github/workflows/` for the per-service
pipelines.

## Adding a new service

1. Pick a language that matches the domain (see `docs/architecture.md`).
2. Scaffold under `services/<name>/`.
3. Add a CI workflow under `.github/workflows/<name>.yml`.
4. Register the owning team in `CODEOWNERS`.
5. Wire the service into `tools/coverage-dashboard/` (TBD).

## Coverage

Each service reports coverage in its native format (JaCoCo XML, Istanbul
JSON, coverage.py XML). The aggregation tooling under `tools/` is still
being built out.

## Contacts

- Platform Engineering: `@bofa/platform-eng`
- Identity Platform:    `@bofa/identity-platform`
- Payments:             `@bofa/payments-team`
- Data Security:        `@bofa/data-security`
- Compliance liaison:   see `COMPLIANCE.md`
