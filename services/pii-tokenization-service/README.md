# pii-tokenization-service

Tokenize and detokenize PII (SSN, DOB, account numbers) via the
on-prem HSM cluster. Internal-only — reachable from kyc-enrichment,
reporting-aggregator, and customer-profile-api via the service mesh
allowlist.

## Status

**Greenfield.** No tests yet. Build pipelines are being set up. Do not
call this service from new code paths until the data-security team
signs off on the tokenization scheme.

## Run

```bash
pip install -e ../../libs/pii-utils
pip install -e .
uvicorn pii_tokenization.main:app --port 8088
```

## Endpoints

- `POST /tokenize`         `{ field, value }` -> `{ token, field }`
- `POST /tokenize/bulk`    `{ field, values[] }` -> `{ tokens[], field }`
- `POST /detokenize`       `{ token }` -> `{ value }` (requires `X-Service-Token` header)
- `GET  /fingerprint/{field}/{value}`
- `GET  /healthz`

## TODO

- Wire up pytest + coverage.py
- Add CI job
- Swap stubbed HSM for real client (tracked in DATASEC-204)
- Confirm token format with data-platform consumers
