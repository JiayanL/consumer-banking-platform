# session-manager

Session lifecycle + JWT issuance for authenticated users.

## Run

```bash
(cd ../../libs/logging-sdk && npm install && npm run build)
npm install
npm run build
npm start
```

Listens on `:8090`.

## Test

```bash
npm test
```

## Known

- One test relies on real elapsed time (setTimeout-based) rather than a
  mocked clock. See `src/__tests__/idleReap.test.ts` and PLAT-1871.
- JWT `exp` semantics here differ subtly from common-auth (Java); they
  haven't been reconciled yet.
