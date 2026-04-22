# Testing standards

> Status: draft. Originally written 2022-Q3 by the Platform Engineering
> quality guild. Updated piecemeal since then — some sections below are
> out of date and flagged inline.

## Expected coverage targets

The long-term target set by the 2022 quality guild was **70% line
coverage** across all services, with **80%** for anything that touches
the money movement path. We are not there yet. Current per-service
numbers are tracked in `COMPLIANCE.md` and, eventually, in the coverage
dashboard under `tools/coverage-dashboard/`.

## Tooling by language

| Language   | Test runner | Coverage tool          | Report format   |
|------------|-------------|------------------------|-----------------|
| Java       | JUnit 5     | JaCoCo                 | `jacoco.xml`    |
| TypeScript | Jest        | built-in / nyc         | Istanbul JSON   |
| Python     | pytest      | `coverage.py`          | `coverage.xml`  |

> Note (2023): a few services still use Mocha or plain stubs for
> historical reasons. New services must use the runners above.

## Compliance-critical code paths

Code that falls into one of the OCC categories defined in
`COMPLIANCE.md` **must** be annotated:

- Java:       `@ComplianceCritical(category = ...)` (from `common-auth`)
- TypeScript: JSDoc `@compliance-critical <CATEGORY>`
- Python:     `@compliance_critical(category="...")` (from `pii-utils`)

Annotated code paths are expected to have **meaningful** test coverage —
coverage-gaming tests (e.g., calling a function without asserting on
its output) do not count toward the compliance goal, though we do not
currently have tooling to detect that.

## Expectations for new PRs

1. Tests live next to the code they exercise, using the language's
   native convention (`src/test/java/...`, `__tests__/`, `tests/`).
2. New compliance-critical code paths must ship with tests that cover
   at least the happy path and one failure path.
3. The coverage delta bot will post a comment on every PR. A decrease
   in any compliance-tagged category is a hard failure.

## Known gaps

- Integration test scaffolding is aspirational in a few services
  (notably `ledger-service`) — the Testcontainers config exists but is
  not wired into CI.
- `test-utils/` is a placeholder for the cross-language fixture sharing
  story. It is mostly empty.
- `pii-tokenization-service` has no test infrastructure at all — this
  is a known finding (see `COMPLIANCE.md`).
