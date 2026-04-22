package com.bofa.cbp.ledger;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// TODO: finish integration test wiring — needs maven-failsafe-plugin hooked up in pom.xml
//       and a proper Spring Boot @DynamicPropertySource to point datasource at the container.
//       Tracked against the PLAT-1730 ledger durability work item.
@Testcontainers
@Disabled("aspirational — not wired into failsafe yet")
class LedgerIntegrationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("ledger")
            .withUsername("ledger")
            .withPassword("ledger");

    @Test
    void placeholder_writesAndReadsBackAcrossRealPostgres() {
        // TODO: once wired, assert a journal round-trips through real JPA + Postgres
        //       and that derived balances match the H2 unit-test expectations.
    }
}
