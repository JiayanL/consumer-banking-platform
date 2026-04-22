"""Coverage delta bot.

- Compares two aggregate coverage JSON files (output of aggregate_coverage.py).
- Prints a markdown delta table.
- Posts the table as a sticky PR comment when run inside GitHub Actions
  (requires GH_TOKEN, PR_NUMBER, REPO env vars).
- In --gate mode, exits non-zero if any compliance-tagged service regresses.

Compliance tagging is resolved via tools/compliance_map.json (service -> list
of OCC categories). If a service listed there regresses, the gate fails.
"""
from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
COMPLIANCE_MAP = REPO_ROOT / "tools" / "compliance_map.json"
STICKY_MARKER = "<!-- cbp-coverage-delta-bot -->"


def _load(p: Path) -> dict:
    try:
        return json.loads(p.read_text())
    except Exception:
        return {}


def _load_compliance_map() -> dict[str, list[str]]:
    if not COMPLIANCE_MAP.exists():
        return {}
    return json.loads(COMPLIANCE_MAP.read_text())


def _pct(d: dict, svc: str) -> float | None:
    svcs = d.get("services", {})
    s = svcs.get(svc)
    if not s:
        return None
    return float(s.get("pct", 0.0))


def render_markdown(base: dict, head: dict) -> str:
    all_services = sorted(set((base.get("services") or {}).keys()) | set((head.get("services") or {}).keys()))
    lines = [
        STICKY_MARKER,
        "### Coverage delta",
        "",
        "| Service | Base | Head | Δ |",
        "|---|---:|---:|---:|",
    ]
    for svc in all_services:
        b = _pct(base, svc)
        h = _pct(head, svc)
        b_s = f"{b * 100:.2f}%" if b is not None else "—"
        h_s = f"{h * 100:.2f}%" if h is not None else "—"
        if b is not None and h is not None:
            d = (h - b) * 100
            sign = "+" if d >= 0 else ""
            d_s = f"{sign}{d:.2f}pp"
        else:
            d_s = "—"
        lines.append(f"| `{svc}` | {b_s} | {h_s} | {d_s} |")

    overall_b = base.get("overall", {}).get("pct")
    overall_h = head.get("overall", {}).get("pct")
    if overall_b is not None and overall_h is not None:
        lines.append("")
        lines.append(
            f"**Overall:** {overall_b*100:.2f}% → {overall_h*100:.2f}% "
            f"({'+' if overall_h>=overall_b else ''}{(overall_h-overall_b)*100:.2f}pp)"
        )
    return "\n".join(lines)


def _gh(method: str, url: str, body: dict | None = None) -> dict:
    token = os.environ["GH_TOKEN"]
    data = json.dumps(body).encode() if body else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Authorization", f"Bearer {token}")
    req.add_header("Accept", "application/vnd.github+json")
    req.add_header("X-GitHub-Api-Version", "2022-11-28")
    if data:
        req.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode() or "{}")


def upsert_sticky_comment(markdown: str) -> None:
    repo = os.environ.get("REPO")
    pr = os.environ.get("PR_NUMBER")
    if not (repo and pr and os.environ.get("GH_TOKEN")):
        print("no GH_TOKEN/REPO/PR_NUMBER, skipping comment")
        return
    url = f"https://api.github.com/repos/{repo}/issues/{pr}/comments"
    try:
        existing = _gh("GET", url)
    except urllib.error.HTTPError as e:
        print(f"GET comments failed: {e}", file=sys.stderr)
        return
    for c in existing if isinstance(existing, list) else []:
        if STICKY_MARKER in c.get("body", ""):
            _gh("PATCH", c["url"], {"body": markdown})
            return
    _gh("POST", url, {"body": markdown})


def enforce_gate(base: dict, head: dict) -> int:
    mapping = _load_compliance_map()
    regressions: list[str] = []
    for svc, categories in mapping.items():
        b = _pct(base, svc)
        h = _pct(head, svc)
        if b is None or h is None:
            continue
        if h + 1e-6 < b:
            regressions.append(
                f"- `{svc}` [{', '.join(categories)}]: {b*100:.2f}% → {h*100:.2f}%"
            )
    if regressions:
        print("Compliance-tagged coverage regressed:")
        print("\n".join(regressions))
        return 1
    print("Compliance-tagged coverage did not regress.")
    return 0


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", required=True)
    ap.add_argument("--head", required=True)
    ap.add_argument("--gate", action="store_true")
    args = ap.parse_args(argv)

    base = _load(Path(args.base))
    head = _load(Path(args.head))

    if args.gate:
        return enforce_gate(base, head)

    md = render_markdown(base, head)
    print(md)
    upsert_sticky_comment(md)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
