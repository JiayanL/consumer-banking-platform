"""Tests for retention policy logic."""
import datetime as _dt

from reporting_aggregator.retention import decide, DAILY_RETENTION_DAYS, MONTHLY_RETENTION_DAYS


def test_daily_within_retention():
    report = {"report_id": "rep-1", "period": {"kind": "daily", "date": "2024-01-01"}}
    result = decide(report, today=_dt.date(2024, 6, 1))
    assert result.action == "KEEP"


def test_daily_near_expiry():
    # Within 90 days of expiry: archive
    report = {"report_id": "rep-1", "period": {"kind": "daily", "date": "2017-03-10"}}
    result = decide(report, today=_dt.date(2024, 3, 1))
    assert result.action == "ARCHIVE"


def test_daily_expired():
    report = {"report_id": "rep-1", "period": {"kind": "daily", "date": "2015-01-01"}}
    result = decide(report, today=_dt.date(2024, 1, 1))
    assert result.action == "DELETE"


def test_daily_missing_date():
    report = {"report_id": "rep-1", "period": {"kind": "daily"}}
    result = decide(report)
    assert result.action == "KEEP"
    assert "no date" in result.reason


def test_monthly_within_retention():
    report = {"report_id": "rep-1", "period": {"kind": "monthly", "period": "2024-01"}}
    result = decide(report, today=_dt.date(2024, 6, 1))
    assert result.action == "KEEP"


def test_monthly_expired():
    report = {"report_id": "rep-1", "period": {"kind": "monthly", "period": "2010-01"}}
    result = decide(report, today=_dt.date(2024, 1, 1))
    assert result.action == "DELETE"


def test_monthly_malformed_period():
    report = {"report_id": "rep-1", "period": {"kind": "monthly", "period": "bad"}}
    result = decide(report)
    assert result.action == "KEEP"
    assert "malformed" in result.reason


def test_unknown_kind():
    report = {"report_id": "rep-1", "period": {"kind": "weekly"}}
    result = decide(report)
    assert result.action == "KEEP"
    assert "unknown kind" in result.reason
