# tools/

Cross-service coverage helpers used by CI.

- `aggregate_coverage.py` — walks each service, reads its native coverage
  artifact (JaCoCo XML / Istanbul `coverage-summary.json` / coverage.py XML),
  writes a unified roll-up.
- `coverage_delta_bot.py` — compares two rollups, posts a sticky PR comment,
  fails the job if any compliance-tagged service regresses.
- `compliance_map.json` — service → OCC categories. Kept in-repo so the
  gate is reviewable in a PR.

## Future

The `coverage-dashboard/` sub-directory is the planned unified web UI
(JaCoCo + Istanbul + coverage.py + parsed `@ComplianceCritical`
annotations). Not built yet — tracked under PLAT-1903.
