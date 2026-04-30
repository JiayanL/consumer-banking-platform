"""Tests for reporting-aggregator."""
from reporting_aggregator.app import create_app
from reporting_aggregator.aggregations import build_report, _totals_by_type, _totals_by_account, _flagged
from reporting_aggregator.seed import Transaction


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


def test_monthly_aggregation():
    client = create_app().test_client()
    resp = client.post("/reports/monthly?period=2024-03")
    assert resp.status_code == 201
    body = resp.get_json()
    assert body["count"] > 0
    assert body["period"]["kind"] == "monthly"
    assert body["period"]["period"] == "2024-03"


def test_daily_no_transactions_for_date():
    client = create_app().test_client()
    resp = client.post("/reports/daily?date=2020-01-01")
    assert resp.status_code == 201
    body = resp.get_json()
    assert body["count"] == 0
    assert body["flagged"] == []


def test_daily_missing_date_param():
    client = create_app().test_client()
    resp = client.post("/reports/daily")
    assert resp.status_code == 400


def test_monthly_missing_period_param():
    client = create_app().test_client()
    resp = client.post("/reports/monthly")
    assert resp.status_code == 400


def test_get_report_not_found():
    client = create_app().test_client()
    resp = client.get("/reports/nonexistent")
    assert resp.status_code == 404


def test_get_report_after_creation():
    client = create_app().test_client()
    create_resp = client.post("/reports/daily?date=2024-03-01")
    report_id = create_resp.get_json()["report_id"]
    get_resp = client.get(f"/reports/{report_id}")
    assert get_resp.status_code == 200
    assert get_resp.get_json()["report_id"] == report_id


def test_build_report_single_transaction():
    txns = [Transaction("t-1", "acct-a", "2024-01-01", "DEPOSIT", 500.0)]
    report = build_report("rep-test", txns)
    assert report["count"] == 1
    assert report["count_distinct_accounts"] == 1
    assert report["totals_by_type"]["DEPOSIT"] == 500.0
    assert report["flagged"] == []


def test_build_report_all_same_type():
    txns = [
        Transaction("t-1", "acct-a", "2024-01-01", "DEPOSIT", 100.0),
        Transaction("t-2", "acct-b", "2024-01-01", "DEPOSIT", 200.0),
    ]
    report = build_report("rep-test", txns)
    assert report["totals_by_type"] == {"DEPOSIT": 300.0}


def test_flagged_transactions():
    txns = [
        Transaction("t-1", "acct-a", "2024-01-01", "DEPOSIT", 5000.0),
        Transaction("t-2", "acct-b", "2024-01-01", "DEPOSIT", 15000.0),
    ]
    flagged = _flagged(txns)
    assert len(flagged) == 1
    assert flagged[0]["txn_id"] == "t-2"


def test_totals_by_type():
    txns = [
        Transaction("t-1", "a", "2024-01-01", "DEPOSIT", 100.0),
        Transaction("t-2", "a", "2024-01-01", "FEE", 5.0),
    ]
    totals = _totals_by_type(txns)
    assert totals["DEPOSIT"] == 100.0
    assert totals["FEE"] == 5.0


def test_totals_by_account():
    txns = [
        Transaction("t-1", "acct-a", "2024-01-01", "DEPOSIT", 100.0),
        Transaction("t-2", "acct-a", "2024-01-01", "DEPOSIT", 200.0),
        Transaction("t-3", "acct-b", "2024-01-01", "DEPOSIT", 50.0),
    ]
    totals = _totals_by_account(txns)
    assert totals["acct-a"] == 300.0
    assert totals["acct-b"] == 50.0


def test_daily_aggregation_different_date():
    client = create_app().test_client()
    resp = client.post("/reports/daily?date=2024-03-02")
    assert resp.status_code == 201
    body = resp.get_json()
    assert body["count"] == 3
    flagged_ids = {f["txn_id"] for f in body["flagged"]}
    assert "t-0008" in flagged_ids
