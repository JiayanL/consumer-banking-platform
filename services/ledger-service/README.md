# ledger-service

Spring Boot 3.3 double-entry ledger. Exposes journal submission
(debits == credits enforced), posting queries, and derived balances.
Storage is in-memory H2 via Spring Data JPA.

A Testcontainers integration test (`LedgerIntegrationIT`) is present
but intentionally not wired into `maven-failsafe-plugin` yet — see the
TODO on the class.

Latest JaCoCo instruction coverage: **33.46%**.
