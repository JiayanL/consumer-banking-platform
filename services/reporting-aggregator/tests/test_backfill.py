"""Tests for reporting_aggregator.backfill — coverage target 80% (Tier 1).

OCC category: PII_HANDLING
Jira: KAN-7
Owner: @bofa/finance-systems
"""
from __future__ import annotations

import datetime as _dt

from reporting_aggregator.backfill import (
    _iter_days,
    _txns_for_day,
    backfill_daily_reports,
    backfill_monthly_reports,
    diff_reports,
)
from reporting_aggregator.seed import Transaction


# ---------------------------------------------------------------------------
# _iter_days
# ---------------------------------------------------------------------------

class TestIterDays:
    def test_single_day(self):
        result = list(_iter_days("2024-03-01", "2024-03-01"))
        assert result == ["2024-03-01"]

    def test_multi_day_range(self):
        result = list(_iter_days("2024-03-01", "2024-03-03"))
        assert result == ["2024-03-01", "2024-03-02", "2024-03-03"]

    def test_empty_range(self):
        result = list(_iter_days("2024-03-05", "2024-03-01"))
        assert result == []

    def test_month_boundary(self):
        result = list(_iter_days("2024-03-30", "2024-04-02"))
        assert result == ["2024-03-30", "2024-03-31", "2024-04-01", "2024-04-02"]

    def test_iso_format(self):
        for day in _iter_days("2024-01-01", "2024-01-03"):
            _dt.date.fromisoformat(day)


# ---------------------------------------------------------------------------
# _txns_for_day
# ---------------------------------------------------------------------------

_POOL = (
    Transaction("t-1", "acct-a", "2024-03-01", "DEPOSIT", 100.0),
    Transaction("t-2", "acct-b", "2024-03-01", "FEE", 5.0),
    Transaction("t-3", "acct-a", "2024-03-02", "WITHDRAWAL", 50.0),
)


class TestTxnsForDay:
    def test_matching_day(self):
        result = _txns_for_day("2024-03-01", _POOL)
        assert len(result) == 2
        assert all(t.posted_date == "2024-03-01" for t in result)

    def test_single_match(self):
        result = _txns_for_day("2024-03-02", _POOL)
        assert len(result) == 1
        assert result[0].txn_id == "t-3"

    def test_no_match(self):
        result = _txns_for_day("2024-12-25", _POOL)
        assert result == []

    def test_returns_list(self):
        result = _txns_for_day("2024-03-01", _POOL)
        assert isinstance(result, list)

    def test_preserves_transaction_data(self):
        result = _txns_for_day("2024-03-01", _POOL)
        ids = {t.txn_id for t in result}
        assert ids == {"t-1", "t-2"}


# ---------------------------------------------------------------------------
# backfill_daily_reports  (uses global TRANSACTIONS from seed)
# ---------------------------------------------------------------------------

class TestBackfillDailyReports:
    def test_happy_path_single_day(self):
        reports = backfill_daily_reports("2024-03-01", "2024-03-01")
        assert len(reports) == 1
        r = reports[0]
        assert r["report_id"] == "rep_d_bf_2024-03-01"
        assert r["period"] == {"kind": "daily", "date": "2024-03-01"}
        assert r["backfilled"] is True
        assert r["count"] == 5
        assert r["count_distinct_accounts"] == 3

    def test_happy_path_multi_day(self):
        reports = backfill_daily_reports("2024-03-01", "2024-03-03")
        dates = [r["period"]["date"] for r in reports]
        assert "2024-03-01" in dates
        assert "2024-03-02" in dates
        assert "2024-03-03" in dates
        assert len(reports) == 3

    def test_skips_days_without_transactions(self):
        reports = backfill_daily_reports("2024-03-04", "2024-03-14")
        assert len(reports) == 0

    def test_range_with_gaps(self):
        reports = backfill_daily_reports("2024-03-01", "2024-03-15")
        dates = {r["period"]["date"] for r in reports}
        assert "2024-03-04" not in dates
        assert "2024-03-01" in dates
        assert "2024-03-15" in dates

    def test_totals_by_type_present(self):
        reports = backfill_daily_reports("2024-03-01", "2024-03-01")
        r = reports[0]
        assert "DEPOSIT" in r["totals_by_type"]
        assert "WITHDRAWAL" in r["totals_by_type"]
        assert "FEE" in r["totals_by_type"]

    def test_flagged_transactions_included(self):
        reports = backfill_daily_reports("2024-03-01", "2024-03-01")
        r = reports[0]
        flagged_ids = {f["txn_id"] for f in r["flagged"]}
        assert "t-0005" in flagged_ids

    def test_empty_range_returns_empty(self):
        reports = backfill_daily_reports("2025-01-01", "2025-01-05")
        assert reports == []

    def test_reversed_range_returns_empty(self):
        reports = backfill_daily_reports("2024-03-03", "2024-03-01")
        assert reports == []


# ---------------------------------------------------------------------------
# backfill_monthly_reports  (uses global TRANSACTIONS from seed)
# ---------------------------------------------------------------------------

class TestBackfillMonthlyReports:
    def test_happy_path_2024(self):
        reports = backfill_monthly_reports(2024)
        periods = [r["period"]["period"] for r in reports]
        assert "2024-03" in periods
        assert "2024-04" in periods
        assert len(reports) == 2

    def test_monthly_report_structure(self):
        reports = backfill_monthly_reports(2024)
        for r in reports:
            assert r["period"]["kind"] == "monthly"
            assert r["backfilled"] is True
            assert "report_id" in r
            assert r["report_id"].startswith("rep_m_bf_")

    def test_march_aggregation(self):
        reports = backfill_monthly_reports(2024)
        march = [r for r in reports if r["period"]["period"] == "2024-03"][0]
        assert march["count"] == 10
        assert march["count_distinct_accounts"] == 4

    def test_april_aggregation(self):
        reports = backfill_monthly_reports(2024)
        april = [r for r in reports if r["period"]["period"] == "2024-04"][0]
        assert april["count"] == 3
        assert april["count_distinct_accounts"] == 3

    def test_year_with_no_data(self):
        reports = backfill_monthly_reports(2020)
        assert reports == []

    def test_flagged_in_monthly(self):
        reports = backfill_monthly_reports(2024)
        march = [r for r in reports if r["period"]["period"] == "2024-03"][0]
        flagged_ids = {f["txn_id"] for f in march["flagged"]}
        assert "t-0005" in flagged_ids
        assert "t-0008" in flagged_ids


# ---------------------------------------------------------------------------
# diff_reports
# ---------------------------------------------------------------------------

class TestDiffReports:
    def test_identical_reports(self):
        report = {
            "count": 5,
            "count_distinct_accounts": 3,
            "totals_by_type": {"DEPOSIT": 100.0, "FEE": 5.0},
        }
        result = diff_reports(report, report)
        assert result["changed_fields"] == []
        assert result["totals_by_type"] == {}

    def test_count_changed(self):
        old = {"count": 5, "count_distinct_accounts": 3, "totals_by_type": {}}
        new = {"count": 6, "count_distinct_accounts": 3, "totals_by_type": {}}
        result = diff_reports(old, new)
        assert "count" in result["changed_fields"]
        assert "count_distinct_accounts" not in result["changed_fields"]

    def test_count_distinct_accounts_changed(self):
        old = {"count": 5, "count_distinct_accounts": 3, "totals_by_type": {}}
        new = {"count": 5, "count_distinct_accounts": 4, "totals_by_type": {}}
        result = diff_reports(old, new)
        assert "count_distinct_accounts" in result["changed_fields"]
        assert "count" not in result["changed_fields"]

    def test_both_fields_changed(self):
        old = {"count": 5, "count_distinct_accounts": 3, "totals_by_type": {}}
        new = {"count": 7, "count_distinct_accounts": 4, "totals_by_type": {}}
        result = diff_reports(old, new)
        assert set(result["changed_fields"]) == {"count", "count_distinct_accounts"}

    def test_totals_changed(self):
        old = {"count": 5, "count_distinct_accounts": 3, "totals_by_type": {"DEPOSIT": 100.0}}
        new = {"count": 5, "count_distinct_accounts": 3, "totals_by_type": {"DEPOSIT": 200.0}}
        result = diff_reports(old, new)
        assert "DEPOSIT" in result["totals_by_type"]
        assert result["totals_by_type"]["DEPOSIT"] == {"old": 100.0, "new": 200.0}

    def test_new_type_added(self):
        old = {"count": 5, "count_distinct_accounts": 3, "totals_by_type": {"DEPOSIT": 100.0}}
        new = {"count": 5, "count_distinct_accounts": 3, "totals_by_type": {"DEPOSIT": 100.0, "FEE": 5.0}}
        result = diff_reports(old, new)
        assert "FEE" in result["totals_by_type"]
        assert result["totals_by_type"]["FEE"] == {"old": None, "new": 5.0}

    def test_type_removed(self):
        old = {"count": 5, "count_distinct_accounts": 3, "totals_by_type": {"DEPOSIT": 100.0, "FEE": 5.0}}
        new = {"count": 5, "count_distinct_accounts": 3, "totals_by_type": {"DEPOSIT": 100.0}}
        result = diff_reports(old, new)
        assert "FEE" in result["totals_by_type"]
        assert result["totals_by_type"]["FEE"] == {"old": 5.0, "new": None}

    def test_missing_totals_by_type_key(self):
        old = {"count": 5, "count_distinct_accounts": 3}
        new = {"count": 5, "count_distinct_accounts": 3, "totals_by_type": {"DEPOSIT": 100.0}}
        result = diff_reports(old, new)
        assert "DEPOSIT" in result["totals_by_type"]

    def test_missing_count_keys(self):
        old = {"totals_by_type": {}}
        new = {"count": 5, "totals_by_type": {}}
        result = diff_reports(old, new)
        assert "count" in result["changed_fields"]
