"""Tests for reporting_aggregator.exporters."""
import json

from reporting_aggregator.exporters import (
    flagged_to_csv,
    report_to_csv,
    reports_to_jsonl,
)


# ---------------------------------------------------------------------------
# report_to_csv
# ---------------------------------------------------------------------------

class TestReportToCsv:
    def test_happy_path_multiple_accounts(self):
        report = {
            "report_id": "rpt-001",
            "totals_by_account": {
                "acct-b": 512.0,
                "acct-a": 1290.0,
            },
        }
        csv_text = report_to_csv(report)
        lines = csv_text.strip().splitlines()
        assert lines[0] == "report_id,account_id,total_amount"
        # sorted by account_id
        assert lines[1] == "rpt-001,acct-a,1290.00"
        assert lines[2] == "rpt-001,acct-b,512.00"

    def test_empty_totals(self):
        report = {"report_id": "rpt-empty", "totals_by_account": {}}
        csv_text = report_to_csv(report)
        lines = csv_text.strip().splitlines()
        assert len(lines) == 1  # header only
        assert lines[0] == "report_id,account_id,total_amount"

    def test_missing_totals_key(self):
        report = {"report_id": "rpt-none"}
        csv_text = report_to_csv(report)
        lines = csv_text.strip().splitlines()
        assert len(lines) == 1

    def test_amounts_formatted_two_decimals(self):
        report = {
            "report_id": "rpt-fmt",
            "totals_by_account": {"acct-x": 7},
        }
        csv_text = report_to_csv(report)
        assert "7.00" in csv_text

    def test_single_account(self):
        report = {
            "report_id": "rpt-one",
            "totals_by_account": {"acct-z": 99.9},
        }
        csv_text = report_to_csv(report)
        lines = csv_text.strip().splitlines()
        assert len(lines) == 2
        assert lines[1] == "rpt-one,acct-z,99.90"

    def test_large_amount(self):
        report = {
            "report_id": "rpt-big",
            "totals_by_account": {"acct-a": 1_000_000.50},
        }
        csv_text = report_to_csv(report)
        assert "1000000.50" in csv_text


# ---------------------------------------------------------------------------
# flagged_to_csv
# ---------------------------------------------------------------------------

class TestFlaggedToCsv:
    def test_happy_path(self):
        report = {
            "report_id": "rpt-f01",
            "flagged": [
                {"txn_id": "t-100", "account_id": "acct-a", "type": "DEPOSIT", "amount": 15000.0},
                {"txn_id": "t-200", "account_id": "acct-b", "type": "TRANSFER", "amount": 12500.0},
            ],
        }
        csv_text = flagged_to_csv(report)
        lines = csv_text.strip().splitlines()
        assert lines[0] == "report_id,txn_id,account_id,type,amount"
        assert lines[1] == "rpt-f01,t-100,acct-a,DEPOSIT,15000.00"
        assert lines[2] == "rpt-f01,t-200,acct-b,TRANSFER,12500.00"

    def test_no_flagged_transactions(self):
        report = {"report_id": "rpt-clean", "flagged": []}
        csv_text = flagged_to_csv(report)
        lines = csv_text.strip().splitlines()
        assert len(lines) == 1  # header only

    def test_missing_flagged_key(self):
        report = {"report_id": "rpt-missing"}
        csv_text = flagged_to_csv(report)
        lines = csv_text.strip().splitlines()
        assert len(lines) == 1

    def test_amounts_formatted_two_decimals(self):
        report = {
            "report_id": "rpt-fmt",
            "flagged": [
                {"txn_id": "t-1", "account_id": "a-1", "type": "FEE", "amount": 50000},
            ],
        }
        csv_text = flagged_to_csv(report)
        assert "50000.00" in csv_text

    def test_single_flagged_row(self):
        report = {
            "report_id": "rpt-one",
            "flagged": [
                {"txn_id": "t-9", "account_id": "acct-c", "type": "WITHDRAWAL", "amount": 20000.75},
            ],
        }
        csv_text = flagged_to_csv(report)
        lines = csv_text.strip().splitlines()
        assert len(lines) == 2
        assert lines[1] == "rpt-one,t-9,acct-c,WITHDRAWAL,20000.75"


# ---------------------------------------------------------------------------
# reports_to_jsonl
# ---------------------------------------------------------------------------

class TestReportsToJsonl:
    def test_multiple_reports(self):
        reports = [
            {"report_id": "rpt-1", "count": 5},
            {"report_id": "rpt-2", "count": 3},
        ]
        jsonl = reports_to_jsonl(reports)
        lines = jsonl.strip().splitlines()
        assert len(lines) == 2
        assert json.loads(lines[0]) == {"report_id": "rpt-1", "count": 5}
        assert json.loads(lines[1]) == {"report_id": "rpt-2", "count": 3}

    def test_single_report(self):
        reports = [{"report_id": "rpt-solo"}]
        jsonl = reports_to_jsonl(reports)
        lines = jsonl.strip().splitlines()
        assert len(lines) == 1
        assert json.loads(lines[0]) == {"report_id": "rpt-solo"}

    def test_empty_iterable(self):
        jsonl = reports_to_jsonl([])
        assert jsonl == ""

    def test_trailing_newline(self):
        reports = [{"a": 1}]
        jsonl = reports_to_jsonl(reports)
        assert jsonl.endswith("\n")

    def test_keys_sorted(self):
        reports = [{"z_field": 1, "a_field": 2}]
        jsonl = reports_to_jsonl(reports)
        line = jsonl.strip()
        parsed = json.loads(line)
        keys = list(parsed.keys())
        assert keys == sorted(keys)
        # raw string should have a_field before z_field
        assert line.index('"a_field"') < line.index('"z_field"')

    def test_compact_separators(self):
        reports = [{"key": "value", "num": 42}]
        jsonl = reports_to_jsonl(reports)
        line = jsonl.strip()
        # compact separators: no space after , or :
        assert ", " not in line
        assert ": " not in line

    def test_generator_input(self):
        def gen():
            yield {"report_id": "rpt-g1"}
            yield {"report_id": "rpt-g2"}

        jsonl = reports_to_jsonl(gen())
        lines = jsonl.strip().splitlines()
        assert len(lines) == 2
