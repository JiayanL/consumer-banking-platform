"""Tests for report exporters."""
from reporting_aggregator.exporters import report_to_csv, flagged_to_csv, reports_to_jsonl


SAMPLE_REPORT = {
    "report_id": "rep-001",
    "totals_by_account": {"acct-a": 1000.0, "acct-b": 500.0},
    "flagged": [
        {"txn_id": "t-1", "account_id": "acct-a", "type": "DEPOSIT", "amount": 15000.0}
    ],
}


def test_report_to_csv():
    csv_out = report_to_csv(SAMPLE_REPORT)
    assert "report_id,account_id,total_amount" in csv_out
    assert "acct-a" in csv_out
    assert "1000.00" in csv_out
    assert "acct-b" in csv_out


def test_report_to_csv_empty_accounts():
    report = {"report_id": "rep-empty", "totals_by_account": {}}
    csv_out = report_to_csv(report)
    assert "report_id,account_id,total_amount" in csv_out
    assert csv_out.strip().count("\n") == 0


def test_flagged_to_csv():
    csv_out = flagged_to_csv(SAMPLE_REPORT)
    assert "report_id,txn_id,account_id,type,amount" in csv_out
    assert "t-1" in csv_out
    assert "15000.00" in csv_out


def test_flagged_to_csv_no_flagged():
    report = {"report_id": "rep-clean", "flagged": []}
    csv_out = flagged_to_csv(report)
    assert "report_id,txn_id,account_id,type,amount" in csv_out
    lines = [l for l in csv_out.strip().splitlines() if l.strip()]
    assert len(lines) == 1


def test_reports_to_jsonl():
    reports = [{"report_id": "a"}, {"report_id": "b"}]
    jsonl = reports_to_jsonl(reports)
    lines = jsonl.strip().split("\n")
    assert len(lines) == 2
    assert '"report_id":"a"' in lines[0]


def test_reports_to_jsonl_empty():
    assert reports_to_jsonl([]) == ""
