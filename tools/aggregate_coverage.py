"""Aggregate coverage across Java (JaCoCo), TS (Istanbul), and Python (coverage.py).

Walks a known list of service dirs, reads the native coverage artifact from
each, normalizes to a simple dict, and writes a JSON roll-up.

Live output shape (for the delta bot / dashboard):
{
  "services": {
    "account-service":       {"lang": "java",   "covered": N, "total": M, "pct": 0.34},
    "transaction-processor": {"lang": "java",   "covered": N, "total": M, "pct": 0.18},
    "notification-service":  {"lang": "ts",     "covered": N, "total": M, "pct": 0.42},
    ...
  },
  "overall": {"covered": N, "total": M, "pct": 0.30}
}
"""
from __future__ import annotations

import argparse
import json
import os
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Any

REPO_ROOT = Path(__file__).resolve().parent.parent

JAVA_SERVICES = [
    "account-service",
    "ledger-service",
    "auth-service",
    "wire-transfer-service",
    "transaction-processor",
    "audit-logger",
]

TS_SERVICES = [
    "notification-service",
    "customer-profile-api",
    "api-gateway",
    "session-manager",
]

PY_SERVICES = [
    "kyc-enrichment",
    "reporting-aggregator",
]


def _jacoco(service: str) -> dict[str, Any] | None:
    path = REPO_ROOT / "services" / service / "target" / "site" / "jacoco" / "jacoco.xml"
    if not path.exists():
        return None
    root = ET.parse(path).getroot()
    for counter in root.findall("counter"):
        if counter.get("type") == "INSTRUCTION":
            missed = int(counter.get("missed", "0"))
            covered = int(counter.get("covered", "0"))
            total = missed + covered
            pct = covered / total if total else 0.0
            return {"lang": "java", "covered": covered, "total": total, "pct": round(pct, 4)}
    return None


def _istanbul(service: str) -> dict[str, Any] | None:
    path = REPO_ROOT / "services" / service / "coverage" / "coverage-summary.json"
    if not path.exists():
        return None
    data = json.loads(path.read_text())
    stmts = data.get("total", {}).get("statements", {})
    covered = int(stmts.get("covered", 0))
    total = int(stmts.get("total", 0))
    pct = covered / total if total else 0.0
    return {"lang": "ts", "covered": covered, "total": total, "pct": round(pct, 4)}


def _coverage_py(service: str) -> dict[str, Any] | None:
    path = REPO_ROOT / "services" / service / "coverage.xml"
    if not path.exists():
        return None
    root = ET.parse(path).getroot()
    covered_total = 0
    total_total = 0
    for pkg in root.iter("class"):
        lines = pkg.find("lines")
        if lines is None:
            continue
        for line in lines.findall("line"):
            total_total += 1
            if int(line.get("hits", "0")) > 0:
                covered_total += 1
    pct = covered_total / total_total if total_total else 0.0
    return {"lang": "python", "covered": covered_total, "total": total_total, "pct": round(pct, 4)}


def aggregate() -> dict[str, Any]:
    services: dict[str, Any] = {}
    for svc in JAVA_SERVICES:
        r = _jacoco(svc)
        if r:
            services[svc] = r
    for svc in TS_SERVICES:
        r = _istanbul(svc)
        if r:
            services[svc] = r
    for svc in PY_SERVICES:
        r = _coverage_py(svc)
        if r:
            services[svc] = r

    covered = sum(s["covered"] for s in services.values())
    total = sum(s["total"] for s in services.values())
    pct = covered / total if total else 0.0
    return {
        "services": services,
        "overall": {"covered": covered, "total": total, "pct": round(pct, 4)},
    }


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=str(REPO_ROOT / "tools" / "coverage-aggregate.json"))
    args = ap.parse_args(argv)
    result = aggregate()
    Path(args.out).write_text(json.dumps(result, indent=2))
    print(json.dumps(result, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
