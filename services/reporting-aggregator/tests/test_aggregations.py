"""Tests for reporting-aggregator."""
from reporting_aggregator.app import create_app


def test_daily_aggregation_totals_happy_path():
    client = create_app().test_client()
    resp = client.post("/reports/daily?date=2024-03-01")
    assert resp.status_code == 201
    body = resp.get_json()
    assert body["count"] == 5
    assert body["count_distinct_accounts"] == 3
    assert body["totals_by_type"]["DEPOSIT"] == 1_250.0 + 500.0 + 15_000.0
    assert body["totals_by_type"]["WITHDRAWAL"] == 40.0
    assert body["totals_by_type"]["FEE"] == 12.0
    flagged_ids = {f["txn_id"] for f in body["flagged"]}
    assert flagged_ids == {"t-0005"}
    assert body["period"] == {"kind": "daily", "date": "2024-03-01"}
