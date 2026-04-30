"""Tests for alert detection."""
from reporting_aggregator.alerts import detect_alerts, summarize, Alert


def test_no_alerts_for_normal_report():
    report = {
        "report_id": "rep-1",
        "totals_by_type": {"DEPOSIT": 10000.0, "FEE": 10.0},
        "flagged": [],
        "totals_by_account": {"acct-a": 5000.0},
    }
    alerts = detect_alerts(report)
    assert len(alerts) == 0


def test_high_fee_ratio_alert():
    report = {
        "report_id": "rep-1",
        "totals_by_type": {"DEPOSIT": 100.0, "FEE": 10.0},
        "flagged": [],
        "totals_by_account": {},
    }
    alerts = detect_alerts(report)
    kinds = [a.kind for a in alerts]
    assert "HIGH_FEE_RATIO" in kinds


def test_many_flagged_transactions_alert():
    report = {
        "report_id": "rep-1",
        "totals_by_type": {"DEPOSIT": 100000.0},
        "flagged": [{"txn_id": f"t-{i}"} for i in range(5)],
        "totals_by_account": {},
    }
    alerts = detect_alerts(report)
    kinds = [a.kind for a in alerts]
    assert "MANY_FLAGGED_TXNS" in kinds


def test_account_volume_spike_alert():
    report = {
        "report_id": "rep-1",
        "totals_by_type": {"DEPOSIT": 100000.0},
        "flagged": [],
        "totals_by_account": {"acct-a": 75000.0},
    }
    alerts = detect_alerts(report)
    kinds = [a.kind for a in alerts]
    assert "ACCOUNT_VOLUME_SPIKE" in kinds


def test_summarize():
    alerts = [
        Alert("A", "HIGH", "msg", "rep-1"),
        Alert("B", "MEDIUM", "msg", "rep-1"),
        Alert("C", "HIGH", "msg", "rep-1"),
    ]
    summary = summarize(alerts)
    assert summary == {"LOW": 0, "MEDIUM": 1, "HIGH": 2}


def test_summarize_empty():
    assert summarize([]) == {"LOW": 0, "MEDIUM": 0, "HIGH": 0}
