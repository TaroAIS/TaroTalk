# P14 Report - Role Drift Guard MVP

## Scope
- Added drift guard module in orchestrator.
- Added drift detection, one-time regeneration, and deweight strategy inside chat generation loop.
- Added drift decisions into `director_trace`.

## Code Changes
- `orchestrator/app/drift_guard.py`
- `orchestrator/app/orchestrator.py`
- `orchestrator/tests/test_orchestrator.py`

## Behavior
- For each generated turn:
  - Compute drift score based on role style, persona overlap, and novelty.
  - If drift exceeds threshold:
    - Trigger one regeneration attempt for drifting roles.
    - If still drifting, drop/deweight that role for the current round.
- Persist drift diagnostics:
  - `director_trace.drift_decisions[]`
  - round-level `drift_decisions` in trace rounds.

## Test Results
- `orchestrator\\.venv\\Scripts\\python.exe -m pytest orchestrator\\tests -q` passed (`10`).
- `npm test -- --runInBand` in frontend passed (`4/4`).
- `D:\\apache-maven-3.9.12\\bin\\mvn.cmd -pl services/world-service -am test` passed.
- Full Maven gate passed.
- `-DskipTests compile` passed.
