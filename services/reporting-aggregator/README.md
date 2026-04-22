# reporting-aggregator

Flask service that produces daily and monthly aggregation reports over consumer transactions. Reads from an in-memory seed of fake transactions and emits totals by type, totals by account, distinct-account counts, and flagged transactions (> $10k).

## Dev setup

```bash
pip install -e ../../libs/pii-utils
pip install -e .[dev]
pytest --cov --cov-report=term --cov-report=xml
```
