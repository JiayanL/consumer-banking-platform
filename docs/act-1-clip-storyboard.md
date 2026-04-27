# Act 1 — pre-recorded clip storyboard

The 12-minute BofA leadership demo opens with a ~60-second pre-recorded
clip of Devin operating on a single file in this repo, then transitions
to the live Act 2 fleet view. This document is the storyboard for that
clip; it locks the file, the narration beats, and the on-screen
evidence so the recording can be reproduced.

> **Why pre-recorded?** Act 1 is evidence, not a demo. The panel does
> not need to be convinced Devin can write a test — they need to be
> convinced Devin can be deployed. We use the clip as social proof of
> capability, then spend the live time on the fleet-scale workflow
> (Act 2) and governance (Act 3 artifacts).

## 1. The target file

- **Path:** `services/customer-profile-api/src/masking.ts`
- **Owner:** `@bofa/retail-accounts`
- **Compliance category:** `PII_HANDLING`
- **Why this file:**
  - Pure function (no DB, no external calls). The test seam is clean.
  - High visible compliance value — PII masking is regulator-facing.
  - Owner is stable (no active migration or owner-locked freeze).
  - Existing Jest harness in `customer-profile-api`; no infra work.

This file is the rank-5 approved candidate in the live Act 2 list
(see `coverage-dashboard/lib/demo/data.ts`, `demoRecommendations[]`),
so the panel sees the same file referenced in both acts — Act 1 as
"this is what one engineer did", Act 2 as "this is what the fleet
recommends".

## 2. Numbers (locked for the clip)

These are the headline numbers the dashboard's "Act 1 evidence" card
will show on the overview page. The clip narration must match.

| Metric                | Before | After | Delta |
|---                    |---     |---    |---    |
| Statement coverage    | 41%    | 89%   | +48pp |
| Mutation score        | 31%    | 63%   | +32pp |
| Time to PR            |  —     | 12m   |  —    |
| PR number on the demo branch | — | #14 | — |

Source: `coverage-dashboard/lib/demo/data.ts:demoActOneEvidence`.

If those numbers ever change (e.g. a future re-recording uses a
different baseline), they must be updated in both this file AND
`demoActOneEvidence` so the dashboard card and the clip stay in sync.

## 3. The 60 seconds, beat by beat

### 0:00 – 0:08 — open with the file
- On-screen: VS Code with `services/customer-profile-api/src/masking.ts` open. Coverage gutter showing 41% (red).
- Narration: *"This is one TypeScript file in customer-profile-api. PII masking — regulator-visible. Coverage at 41%."*

### 0:08 – 0:18 — Devin reads the repo
- On-screen: Devin session pane on the right. The Deep Wiki view briefly opens, then collapses. The session prompt visible in the recording is short:
  > Raise statement coverage and mutation score on
  > `services/customer-profile-api/src/masking.ts` without modifying
  > production code. Use the existing Jest harness.
- Narration: *"It reads the repo. It pulls in the testing standards. It learns that this service uses Jest, ts-jest, and Istanbul, and that PII masking is a compliance-critical category."*

### 0:18 – 0:38 — the test write
- On-screen: the new test file appears under `services/customer-profile-api/src/__tests__/masking.test.ts`. Cursor scrolls through 6–8 `describe` blocks: `maskEmail`, `maskPhone`, `maskedView`, `initials`, edge cases (`empty user`, `non-ASCII domain`, `phone with letters`).
- Narration: *"It writes the test matrix that the testing-standards doc requires for compliance-critical code: happy path, failure path, edge cases. No production code change."*

### 0:38 – 0:50 — Devin runs the tests, opens the PR
- On-screen: Terminal pane runs `npm test` from the service directory. All green. Then `git push` and a PR opens — `#14` on the demo branch. Coverage gutter updates to 89% (green). Mutation report panel ticks up.
- Narration: *"Tests pass. PR is open against the right branch with the right CODEOWNERS team tagged. Twelve minutes wall-clock. Coverage 41 to 89. Mutation 31 to 63 — that's the real number, not an inflated coverage figure."*

### 0:50 – 1:00 — the transition line
- On-screen: cut to the coverage-dashboard overview, with the "Act 1 evidence" card visible on the left and the "62% across 847 services" stat on the right. Hold for 2 seconds.
- Narration: *"That's one file. The interesting question is what happens at fleet scale. Live, now."*

## 4. Recording requirements

- The terminal must use a non-distracting font (Berkeley Mono or similar).
- Hide the system tray and dock; full-screen the editor.
- Speed up only the dependency install / test runtime if it's longer than 4 seconds. Do NOT speed up Devin's reasoning — that's the part the panel is supposed to see is real-time.
- Audio: single take, no music, no SFX. The voice is the only "production".

## 5. What NOT to do

- DO NOT show Devin reasoning over a service that is in the do-not-touch list (no auth-service, no wire-transfer-service, no transaction-processor).
- DO NOT show a clip in which Devin had to be re-prompted to fix a mistake. Either re-record or pick a different attempt. (The panel will assume the clip is representative; we have to make that assumption true.)
- DO NOT include the "stuck" session moment in the Act 1 clip. That beat is reserved for Act 2 (audit-logger / `ForwardingSink`).
- DO NOT mention "AI" or "agent" generically in the narration. Use "Devin" — it's the product name and they should leave the room saying it.

## 6. Cross-references

- Live Act 2 list (the same file is rank 5 of 10): `coverage-dashboard/lib/demo/data.ts`, `demoRecommendations[]`
- Dashboard "Act 1 evidence" card source: `coverage-dashboard/lib/demo/data.ts`, `demoActOneEvidence`
- Coverage-dashboard surface mapping: see the `Demo mode (Acts 1 / 2 / 3)` section of `coverage-dashboard/README.md`
- Triage rationale and rejection reasons: see the `CBP — demo Act mapping and rehearsal cheat-sheet` knowledge note pinned to this repo.
