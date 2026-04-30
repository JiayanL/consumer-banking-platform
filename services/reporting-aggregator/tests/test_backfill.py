"""Tests for backfill logic."""
from reporting_aggregator.backfill import (
    backfill_daily_reports,
    backfill_monthly_reports,
    diff_reports,
)


def test_backfill_daily_returns_reports():
    reports = backfill_daily_reports("2024-03-01", "2024-03-03")
    assert len(reports) >= 2
    for r in reports:
        assert r["backfilled"] is True
        assert r["period"]["kind"] == "daily"


def test_backfill_daily_empty_range():
    reports = backfill_daily_reports("2020-01-01", "2020-01-05")
    assert reports == []


def test_backfill_monthly():
    reports = backfill_monthly_reports(2024)
    assert len(reports) >= 1
    for r in reports:
        assert r["backfilled"] is True
        assert r["period"]["kind"] == "monthly"


def test_diff_reports_no_changes():
    old = {"count": 5, "count_distinct_accounts": 3, "totals_by_type": {"DEPOSIT": 100.0}}
    new = {"count": 5, "count_distinct_accounts": 3, "totals_by_type": {"DEPOSIT": 100.0}}
    diff = diff_reports(old, new)
    assert diff["changed_fields"] == []
    assert diff["totals_by_type"] == {}


def test_diff_reports_with_changes():
    old = {"count": 5, "count_distinct_accounts": 3, "totals_by_type": {"DEPOSIT": 100.0}}
    new = {"count": 6, "count_distinct_accounts": 3, "totals_by_type": {"DEPOSIT": 150.0}}
    diff = diff_reports(old, new)
    assert "count" in diff["changed_fields"]
    assert "DEPOSIT" in diff["totals_by_type"]
    assert diff["totals_by_type"]["DEPOSIT"]["old"] == 100.0
    assert diff["totals_by_type"]["DEPOSIT"]["new"] == 150.0


def test_diff_reports_new_type():
    old = {"count": 5, "totals_by_type": {"DEPOSIT": 100.0}}
    new = {"count": 5, "totals_by_type": {"DEPOSIT": 100.0, "FEE": 5.0}}
    diff = diff_reports(old, new)
    assert "FEE" in diff["totals_by_type"]
