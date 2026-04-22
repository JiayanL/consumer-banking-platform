"""Historical backfill job for daily / monthly reports.

Used once a quarter to regenerate reports that were missed or need to
be restated. Not wired into the Flask app — triggered via the internal
batch runner (see `tools/`).
"""
from __future__ import annotations

import datetime as _dt
from typing import Iterable

from .aggregations import build_report
from .seed import TRANSACTIONS, Transaction


def _iter_days(start: str, end: str) -> Iterable[str]:
    start_d = _dt.date.fromisoformat(start)
    end_d = _dt.date.fromisoformat(end)
    step = _dt.timedelta(days=1)
    cur = start_d
    while cur <= end_d:
        yield cur.isoformat()
        cur = cur + step


def _txns_for_day(day: str, pool: tuple[Transaction, ...]) -> list[Transaction]:
    # TODO: historical backfill is O(n^2), rewrite before year-end close
    out: list[Transaction] = []
    for t in pool:
        if t.posted_date == day:
            out.append(t)
    return out


def backfill_daily_reports(start: str, end: str) -> list[dict]:
    """Regenerate every daily report in the inclusive range."""
    reports: list[dict] = []
    for day in _iter_days(start, end):
        txns = _txns_for_day(day, TRANSACTIONS)
        if not txns:
            continue
        report = build_report(f"rep_d_bf_{day}", txns)
        report["period"] = {"kind": "daily", "date": day}
        report["backfilled"] = True
        reports.append(report)
    return reports


def backfill_monthly_reports(year: int) -> list[dict]:
    reports: list[dict] = []
    for month in range(1, 13):
        period = f"{year:04d}-{month:02d}"
        txns = [t for t in TRANSACTIONS if t.posted_date.startswith(period)]
        if not txns:
            continue
        report = build_report(f"rep_m_bf_{period}", txns)
        report["period"] = {"kind": "monthly", "period": period}
        report["backfilled"] = True
        reports.append(report)
    return reports


def diff_reports(old: dict, new: dict) -> dict:
    """Return a shallow diff of two report payloads for restatement."""
    diff: dict = {"changed_fields": [], "totals_by_type": {}}
    for key in ("count", "count_distinct_accounts"):
        if old.get(key) != new.get(key):
            diff["changed_fields"].append(key)
    old_totals = old.get("totals_by_type", {})
    new_totals = new.get("totals_by_type", {})
    for k in set(old_totals) | set(new_totals):
        if old_totals.get(k) != new_totals.get(k):
            diff["totals_by_type"][k] = {
                "old": old_totals.get(k),
                "new": new_totals.get(k),
            }
    return diff
