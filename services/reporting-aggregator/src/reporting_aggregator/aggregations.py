"""Aggregation logic for daily / monthly reports."""
from __future__ import annotations

from collections import defaultdict
from typing import Iterable

from pii_utils import ComplianceCategory, compliance_critical

from .seed import Transaction


FLAG_THRESHOLD = 10_000.0


def _totals_by_type(txns: Iterable[Transaction]) -> dict[str, float]:
    out: dict[str, float] = defaultdict(float)
    for t in txns:
        out[t.type] += t.amount
    return dict(out)


def _totals_by_account(txns: Iterable[Transaction]) -> dict[str, float]:
    out: dict[str, float] = defaultdict(float)
    for t in txns:
        out[t.account_id] += t.amount
    return dict(out)


def _flagged(txns: Iterable[Transaction]) -> list[dict]:
    return [
        {
            "txn_id": t.txn_id,
            "account_id": t.account_id,
            "type": t.type,
            "amount": t.amount,
        }
        for t in txns
        if t.amount > FLAG_THRESHOLD
    ]


@compliance_critical(category=ComplianceCategory.AUDIT_TRAIL)
def build_report(report_id: str, txns: Iterable[Transaction]) -> dict:
    """Build a report payload from a set of transactions.

    This is the compliance-critical entrypoint: generated reports are
    retained as part of the audit trail and relied on during exam.
    """
    txn_list = list(txns)
    return {
        "report_id": report_id,
        "count": len(txn_list),
        "count_distinct_accounts": len({t.account_id for t in txn_list}),
        "totals_by_type": _totals_by_type(txn_list),
        "totals_by_account": _totals_by_account(txn_list),
        "flagged": _flagged(txn_list),
    }
