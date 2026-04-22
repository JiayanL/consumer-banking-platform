"""Alert detection helpers over aggregated reports.

Surfaces anomalies (large FEE totals, unusual deposit spikes) that the
compliance team wants to see on a daily dashboard. Wiring into the
alerting bus is pending.
"""
from __future__ import annotations

from dataclasses import dataclass
from typing import Iterable


@dataclass
class Alert:
    kind: str
    severity: str  # LOW | MEDIUM | HIGH
    message: str
    report_id: str


def _fee_ratio(report: dict) -> float:
    totals = report.get("totals_by_type", {})
    fees = totals.get("FEE", 0.0)
    deposits = totals.get("DEPOSIT", 0.0)
    if deposits <= 0:
        return 0.0
    return fees / deposits


def detect_alerts(report: dict) -> list[Alert]:
    alerts: list[Alert] = []

    ratio = _fee_ratio(report)
    if ratio > 0.05:
        alerts.append(
            Alert(
                kind="HIGH_FEE_RATIO",
                severity="MEDIUM",
                message=f"FEE total is {ratio:.1%} of DEPOSIT total",
                report_id=report.get("report_id", "?"),
            )
        )

    flagged = report.get("flagged", [])
    if len(flagged) >= 3:
        alerts.append(
            Alert(
                kind="MANY_FLAGGED_TXNS",
                severity="HIGH",
                message=f"{len(flagged)} transactions exceed the reporting threshold",
                report_id=report.get("report_id", "?"),
            )
        )

    totals_by_account = report.get("totals_by_account", {})
    for account_id, amount in totals_by_account.items():
        if amount > 50_000:
            alerts.append(
                Alert(
                    kind="ACCOUNT_VOLUME_SPIKE",
                    severity="HIGH",
                    message=f"account {account_id} aggregated {amount:.2f}",
                    report_id=report.get("report_id", "?"),
                )
            )

    return alerts


def summarize(alerts: Iterable[Alert]) -> dict:
    out = {"LOW": 0, "MEDIUM": 0, "HIGH": 0}
    for a in alerts:
        if a.severity in out:
            out[a.severity] += 1
    return out
