# P13 Optimization Input (for P14)

## What Worked
- What-if API produces deterministic branch list and recommendation.
- Branch scenarios are persisted with trace and policy for replay/audit.
- Dry-run behavior is explicit and does not mutate world-state snapshots.

## Gaps
- Branch quality is currently heuristic score only.
- No role-style consistency filter in branch generation output.
- No explicit drift diagnostics in director trace.

## Next-step Focus (P14)
- Introduce role drift guard for speaker output.
- Add drift decisions into `director_trace`.
- Add fallback regeneration/deweight path when drift score crosses threshold.
