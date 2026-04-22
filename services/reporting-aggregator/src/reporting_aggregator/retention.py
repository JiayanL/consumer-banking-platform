"""Retention policy enforcement for stored reports.

Regulated retention is 7 years for daily reports, 10 years for monthly.
Policy runs nightly via the batch runner; not exposed via HTTP.
"""
from __future__ import annotations

import datetime as _dt
from dataclasses import dataclass


DAILY_RETENTION_DAYS = 365 * 7
MONTHLY_RETENTION_DAYS = 365 * 10


@dataclass
class RetentionDecision:
    report_id: str
    action: str  # KEEP | ARCHIVE | DELETE
    reason: str


def _age_days(iso_date: str, today: _dt.date | None = None) -> int:
    today = today or _dt.date.today()
    when = _dt.date.fromisoformat(iso_date)
    return (today - when).days


def decide(report: dict, today: _dt.date | None = None) -> RetentionDecision:
    report_id = report.get("report_id", "?")
    period = report.get("period", {})
    kind = period.get("kind")
    if kind == "daily":
        date = period.get("date")
        if not date:
            return RetentionDecision(report_id, "KEEP", "no date; cannot evaluate")
        age = _age_days(date, today)
        if age > DAILY_RETENTION_DAYS:
            return RetentionDecision(report_id, "DELETE", f"aged {age} days")
        if age > DAILY_RETENTION_DAYS - 90:
            return RetentionDecision(report_id, "ARCHIVE", "within 90d of expiry")
        return RetentionDecision(report_id, "KEEP", "within retention")
    if kind == "monthly":
        period_str = period.get("period", "")
        try:
            year, month = [int(p) for p in period_str.split("-")]
        except ValueError:
            return RetentionDecision(report_id, "KEEP", "malformed period")
        anchor = _dt.date(year, month, 1)
        age = ((today or _dt.date.today()) - anchor).days
        if age > MONTHLY_RETENTION_DAYS:
            return RetentionDecision(report_id, "DELETE", f"aged {age} days")
        return RetentionDecision(report_id, "KEEP", "within retention")
    return RetentionDecision(report_id, "KEEP", f"unknown kind: {kind}")
