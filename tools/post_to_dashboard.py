"""Post coverage roll-up to the coverage-dashboard ingest endpoint.

Reads tools/coverage-aggregate.json (required), tools/coverage-files.json
(optional), and tools/test-summary.json (optional), assembles the dashboard
ingest payload, and POSTs it.

Auth is via the COVERAGE_DASHBOARD_INGEST_TOKEN env var (Bearer token).
The dashboard URL comes from COVERAGE_DASHBOARD_URL.

Designed to be called from the GitHub Actions coverage-aggregation workflow
after aggregate_coverage.py runs.
"""
from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any

REPO_ROOT = Path(__file__).resolve().parent.parent


def _read_json(path: Path) -> dict[str, Any] | None:
    if not path.exists():
        return None
    try:
        return json.loads(path.read_text())
    except json.JSONDecodeError:
        return None


def build_payload(args: argparse.Namespace) -> dict[str, Any]:
    aggregate = _read_json(Path(args.aggregate))
    if aggregate is None:
        raise SystemExit(f"aggregate file not found or invalid: {args.aggregate}")

    files_doc = _read_json(Path(args.files)) if args.files else None
    summary_doc = _read_json(Path(args.test_summary)) if args.test_summary else None

    payload: dict[str, Any] = {
        "repo": args.repo,
        "sha": args.sha,
        "ref": args.ref,
        "timestamp": dt.datetime.now(dt.timezone.utc).isoformat(),
        "services": aggregate.get("services", {}),
        "overall": aggregate.get("overall", {}),
    }
    if args.run_id:
        payload["run_id"] = args.run_id
    if args.display_name:
        payload["display_name"] = args.display_name
    if args.kind:
        payload["kind"] = args.kind
    if files_doc and isinstance(files_doc.get("files"), list):
        payload["files"] = files_doc["files"]
    if summary_doc and isinstance(summary_doc.get("services"), dict):
        payload["test_summary"] = {"services": summary_doc["services"]}
    return payload


def post(url: str, token: str, payload: dict[str, Any]) -> tuple[int, str]:
    data = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data,
        method="POST",
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return resp.status, resp.read().decode("utf-8")
    except urllib.error.HTTPError as exc:
        return exc.code, exc.read().decode("utf-8", errors="replace")


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--url", default=os.environ.get("COVERAGE_DASHBOARD_URL"))
    ap.add_argument(
        "--token", default=os.environ.get("COVERAGE_DASHBOARD_INGEST_TOKEN")
    )
    ap.add_argument(
        "--repo",
        default=os.environ.get("GITHUB_REPOSITORY", "JiayanL/consumer-banking-platform"),
    )
    ap.add_argument("--sha", default=os.environ.get("GITHUB_SHA", "local"))
    ap.add_argument("--ref", default=os.environ.get("GITHUB_REF_NAME", "main"))
    ap.add_argument("--run-id", default=os.environ.get("GITHUB_RUN_ID"))
    ap.add_argument("--display-name", default="consumer-banking-platform")
    ap.add_argument("--kind", default="monorepo")
    ap.add_argument(
        "--aggregate", default=str(REPO_ROOT / "tools" / "coverage-aggregate.json")
    )
    ap.add_argument(
        "--files", default=str(REPO_ROOT / "tools" / "coverage-files.json")
    )
    ap.add_argument(
        "--test-summary", default=str(REPO_ROOT / "tools" / "test-summary.json")
    )
    ap.add_argument(
        "--dry-run",
        action="store_true",
        help="Print the payload instead of POSTing it.",
    )
    args = ap.parse_args(argv)

    payload = build_payload(args)

    if args.dry_run:
        print(json.dumps(payload, indent=2))
        return 0

    if not args.url:
        print("COVERAGE_DASHBOARD_URL is not set; skipping post.", file=sys.stderr)
        return 0
    if not args.token:
        print(
            "COVERAGE_DASHBOARD_INGEST_TOKEN is not set; skipping post.",
            file=sys.stderr,
        )
        return 0

    endpoint = args.url.rstrip("/") + "/api/ingest/coverage"
    status, body = post(endpoint, args.token, payload)
    print(f"POST {endpoint} -> {status}")
    print(body)
    if status >= 400:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
