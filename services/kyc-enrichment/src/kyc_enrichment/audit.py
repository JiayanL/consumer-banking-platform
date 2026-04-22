"""Audit log events emitted by KYC enrichment.

Eventually these flow into the central audit pipeline. For now we
serialize them to a JSONL sink configurable via env; the production
wiring is handled by the audit-service team.
"""
from __future__ import annotations

import datetime as _dt
import json
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Optional


@dataclass
class AuditEvent:
    actor: str
    action: str
    resource: str
    outcome: str
    occurred_at: str
    correlation_id: Optional[str] = None
    detail: Optional[str] = None


def _now_iso() -> str:
    return _dt.datetime.now(tz=_dt.timezone.utc).isoformat()


def build_enrich_event(
    actor: str,
    customer_id: str,
    outcome: str,
    correlation_id: Optional[str] = None,
) -> AuditEvent:
    return AuditEvent(
        actor=actor,
        action="kyc.enrich",
        resource=f"customer:{customer_id}",
        outcome=outcome,
        occurred_at=_now_iso(),
        correlation_id=correlation_id,
    )


def build_reverify_event(
    actor: str,
    customer_id: str,
    outcome: str,
    correlation_id: Optional[str] = None,
) -> AuditEvent:
    return AuditEvent(
        actor=actor,
        action="kyc.reverify",
        resource=f"customer:{customer_id}",
        outcome=outcome,
        occurred_at=_now_iso(),
        correlation_id=correlation_id,
    )


class JsonlAuditSink:
    """Append-only JSONL sink. File is created on first write."""

    def __init__(self, path: Path | str) -> None:
        self.path = Path(path)

    def write(self, event: AuditEvent) -> None:
        payload = json.dumps(asdict(event), separators=(",", ":"))
        with self.path.open("a", encoding="utf-8") as fh:
            fh.write(payload + "\n")


class NullAuditSink:
    """No-op sink used in tests and local dev."""

    def write(self, event: AuditEvent) -> None:  # noqa: D401 - trivial
        return None
