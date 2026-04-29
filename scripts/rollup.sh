#!/usr/bin/env bash
# Run every service's tests + coverage and print a unified roll-up.
#
# Usage:
#   bash scripts/rollup.sh              # build libs, run all services, print table
#   bash scripts/rollup.sh --skip-libs  # skip rebuild of shared libs (already installed)
#   bash scripts/rollup.sh --java       # java services only
#   bash scripts/rollup.sh --ts         # ts services only
#   bash scripts/rollup.sh --py         # python services only
#
# Writes:
#   tools/coverage-aggregate.json   (per-service + overall % from aggregate_coverage.py)
#   tools/coverage-files.json       (per-file line coverage rows for every source file)
#   tools/test-summary.json         (per-service tests run/passed/failed/skipped)
#
# A service that can't build counts as 1 "failed" row — the script does not
# abort on test failures. Exit code is 1 if any service had any failing or
# erroring test, 2 if a build step blew up before tests even ran, 0 otherwise.

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

SKIP_LIBS=0
ONLY=""
for arg in "$@"; do
  case "$arg" in
    --skip-libs) SKIP_LIBS=1 ;;
    --java) ONLY="java" ;;
    --ts)   ONLY="ts" ;;
    --py)   ONLY="py" ;;
    -h|--help)
      sed -n '2,18p' "$0"; exit 0 ;;
    *) echo "unknown arg: $arg" >&2; exit 2 ;;
  esac
done

JAVA_SERVICES=(account-service ledger-service auth-service wire-transfer-service transaction-processor audit-logger)
TS_SERVICES=(notification-service customer-profile-api api-gateway session-manager)
PY_SERVICES=(kyc-enrichment reporting-aggregator)

SUMMARY_FILE="tools/test-summary.json"
echo '{"services":{}}' > "$SUMMARY_FILE"

BUILD_ERR=0

record_service() {
  # $1=service $2=lang $3=run $4=passed $5=failed $6=errors $7=skipped
  python3 - "$@" <<'PY'
import json, sys, pathlib
p = pathlib.Path("tools/test-summary.json")
d = json.loads(p.read_text())
svc, lang, run, passed, failed, errors, skipped = sys.argv[1:]
d["services"][svc] = {
    "lang": lang,
    "run": int(run), "passed": int(passed),
    "failed": int(failed), "errors": int(errors), "skipped": int(skipped),
}
p.write_text(json.dumps(d, indent=2))
PY
}

# ---------------- shared libs ----------------
if [[ $SKIP_LIBS -eq 0 ]]; then
  echo "==> building common-auth (Java)"
  mvn -f libs/common-auth/pom.xml -q install -DskipTests || BUILD_ERR=1
  echo "==> building logging-sdk (TypeScript)"
  ( cd libs/logging-sdk && npm install --silent --no-audit --no-fund && npm run build ) || BUILD_ERR=1
  echo "==> installing pii-utils (Python)"
  pip install -q -e libs/pii-utils || BUILD_ERR=1
fi

# ---------------- Java ----------------
if [[ -z "$ONLY" || "$ONLY" == "java" ]]; then
  for svc in "${JAVA_SERVICES[@]}"; do
    echo "==> java: $svc"
    rm -rf "services/$svc/target/surefire-reports" "services/$svc/target/site/jacoco"
    mvn -f "services/$svc/pom.xml" -q test 2>/dev/null
    # Parse surefire XMLs regardless of exit (we want counts even on failures).
    read -r run failed errors skipped < <(
      python3 - "services/$svc/target/surefire-reports" <<'PY'
import sys, pathlib, xml.etree.ElementTree as ET
d = pathlib.Path(sys.argv[1])
run = failed = errors = skipped = 0
if d.exists():
    for f in d.glob("TEST-*.xml"):
        try:
            r = ET.parse(f).getroot()
        except ET.ParseError:
            continue
        run     += int(r.get("tests", 0))
        failed  += int(r.get("failures", 0))
        errors  += int(r.get("errors", 0))
        skipped += int(r.get("skipped", 0))
print(run, failed, errors, skipped)
PY
    )
    passed=$(( run - failed - errors - skipped ))
    record_service "$svc" java "$run" "$passed" "$failed" "$errors" "$skipped"
  done
fi

# ---------------- TypeScript ----------------
if [[ -z "$ONLY" || "$ONLY" == "ts" ]]; then
  for svc in "${TS_SERVICES[@]}"; do
    echo "==> ts: $svc"
    ( cd "services/$svc" && npm install --silent --no-audit --no-fund ) >/dev/null 2>&1
    jest_out="services/$svc/.jest-results.json"
    # Jest --json dumps a machine-readable summary we can sum.
    ( cd "services/$svc" && npx --no-install jest --coverage --json --outputFile=.jest-results.json ) >/dev/null 2>&1 || true
    if [[ -f "$jest_out" ]]; then
      read -r run passed failed skipped < <(
        python3 - "$jest_out" <<'PY'
import json, sys, pathlib
d = json.loads(pathlib.Path(sys.argv[1]).read_text())
print(d["numTotalTests"], d["numPassedTests"], d["numFailedTests"], d.get("numPendingTests", 0) + d.get("numTodoTests", 0))
PY
      )
      errors=0
    else
      run=0; passed=0; failed=0; errors=1; skipped=0
    fi
    record_service "$svc" ts "$run" "$passed" "$failed" "$errors" "$skipped"
  done
fi

# ---------------- Python ----------------
if [[ -z "$ONLY" || "$ONLY" == "py" ]]; then
  for svc in "${PY_SERVICES[@]}"; do
    echo "==> py: $svc"
    ( cd "services/$svc" && pip install -q -e ".[dev]" ) >/dev/null 2>&1
    junit="services/$svc/.pytest-junit.xml"
    ( cd "services/$svc" && python -m pytest --cov --cov-report=xml --junitxml=.pytest-junit.xml ) >/dev/null 2>&1 || true
    if [[ -f "$junit" ]]; then
      read -r run failed errors skipped < <(
        python3 - "$junit" <<'PY'
import sys, xml.etree.ElementTree as ET
r = ET.parse(sys.argv[1]).getroot()
# Top-level <testsuites> or single <testsuite>
suites = r.findall("testsuite") if r.tag == "testsuites" else [r]
run = failed = errors = skipped = 0
for s in suites:
    run     += int(s.get("tests", 0))
    failed  += int(s.get("failures", 0))
    errors  += int(s.get("errors", 0))
    skipped += int(s.get("skipped", 0))
print(run, failed, errors, skipped)
PY
      )
      passed=$(( run - failed - errors - skipped ))
    else
      run=0; passed=0; failed=0; errors=1; skipped=0
    fi
    record_service "$svc" python "$run" "$passed" "$failed" "$errors" "$skipped"
  done
fi

# ---------------- coverage roll-up ----------------
python3 tools/aggregate_coverage.py --out tools/coverage-aggregate.json \
  --files-out tools/coverage-files.json >/dev/null

# ---------------- print unified table ----------------
python3 - <<'PY'
import json, pathlib
cov = json.loads(pathlib.Path("tools/coverage-aggregate.json").read_text())
tests = json.loads(pathlib.Path("tools/test-summary.json").read_text())

rows = []
for svc, t in tests["services"].items():
    c = cov["services"].get(svc, {})
    rows.append((
        svc, t["lang"],
        t["run"], t["passed"], t["failed"] + t["errors"], t["skipped"],
        c.get("pct", None),
    ))
rows.sort(key=lambda r: (r[1], r[0]))

w = max(len(r[0]) for r in rows) + 2
print()
print(f"{'service':<{w}}{'lang':<8}{'run':>5}{'pass':>6}{'fail':>6}{'skip':>6}  coverage")
print("-" * (w + 8 + 5 + 6 + 6 + 6 + 11))
for svc, lang, run, p, f, s, pct in rows:
    pct_s = f"{pct*100:5.1f}%" if pct is not None else "  n/a "
    print(f"{svc:<{w}}{lang:<8}{run:>5}{p:>6}{f:>6}{s:>6}  {pct_s}")

total_run    = sum(r[2] for r in rows)
total_passed = sum(r[3] for r in rows)
total_failed = sum(r[4] for r in rows)
total_skip   = sum(r[5] for r in rows)
overall_pct  = cov["overall"]["pct"]
print("-" * (w + 8 + 5 + 6 + 6 + 6 + 11))
print(f"{'TOTAL':<{w}}{'':<8}{total_run:>5}{total_passed:>6}{total_failed:>6}{total_skip:>6}  {overall_pct*100:5.1f}%")
print()
print(f"Overall line/instruction coverage: {overall_pct*100:.1f}% "
      f"({cov['overall']['covered']}/{cov['overall']['total']})")
print(f"Tests: {total_passed}/{total_run} passing"
      f"{' — ' + str(total_failed) + ' failing' if total_failed else ''}"
      f"{' — ' + str(total_skip) + ' skipped' if total_skip else ''}")

import sys
sys.exit(1 if total_failed else 0)
PY

rc=$?

# If any build step failed outright, override exit with 2.
[[ $BUILD_ERR -ne 0 ]] && exit 2
exit $rc
