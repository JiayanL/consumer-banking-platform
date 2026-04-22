"""Report exporters (CSV / JSONL).

Used by the downstream BI pipeline to pull reports out of the service
for warehouse ingestion. Not yet hooked into the Flask app.
"""
from __future__ import annotations

import csv
import io
import json
from typing import Iterable


def report_to_csv(report: dict) -> str:
    """Render the 'totals_by_account' section of a report as CSV."""
    buf = io.StringIO()
    writer = csv.writer(buf)
    writer.writerow(["report_id", "account_id", "total_amount"])
    for account_id, amount in sorted(report.get("totals_by_account", {}).items()):
        writer.writerow([report["report_id"], account_id, f"{amount:.2f}"])
    return buf.getvalue()


def flagged_to_csv(report: dict) -> str:
    buf = io.StringIO()
    writer = csv.writer(buf)
    writer.writerow(["report_id", "txn_id", "account_id", "type", "amount"])
    for row in report.get("flagged", []):
        writer.writerow(
            [
                report["report_id"],
                row["txn_id"],
                row["account_id"],
                row["type"],
                f"{row['amount']:.2f}",
            ]
        )
    return buf.getvalue()


def reports_to_jsonl(reports: Iterable[dict]) -> str:
    lines = []
    for report in reports:
        lines.append(json.dumps(report, separators=(",", ":"), sort_keys=True))
    return "\n".join(lines) + ("\n" if lines else "")
