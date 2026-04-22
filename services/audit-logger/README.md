# audit-logger

Write-path for every audit event emitted in the platform. Events land
in an append-only store and are forwarded to the immutable long-term
archive by a downstream job.

## Run

```bash
(cd ../../libs/common-auth && mvn -q -DskipTests install)
mvn spring-boot:run
```

Listens on `:8085`.

## Tests

```bash
mvn test
```
