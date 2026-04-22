# kyc-enrichment

FastAPI service that enriches inbound KYC payloads with sanctions and PEP screening (stubbed) and returns a risk tier. Uses `cbp-pii-utils` for SSN/DOB validation, masking, and stable identity hashing.

## Dev setup

```bash
pip install -e ../../libs/pii-utils
pip install -e .[dev]
pytest --cov --cov-report=term --cov-report=xml
```

Run locally with `uvicorn kyc_enrichment.main:app --reload`.
