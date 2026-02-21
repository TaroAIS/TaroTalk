# P18 Report - Narrative Safety Linter MVP

## Scope
- Added orchestrator safety linter to audit tool calls and state effects.
- Added hard-block and soft-warning policies with structured `safety_report`.
- Wired `safety_report` into A2A chat response and director trace.

## Code Changes
- `orchestrator/app/safety_linter.py`
- `orchestrator/app/orchestrator.py`
- `orchestrator/app/models.py`
- `orchestrator/app/main.py`
- `orchestrator/tests/test_orchestrator.py`

## Behavior
- Hard rules (`SAFETY_BLOCKED`) block unsafe actions:
  - illegal relationship type (`RELATIONSHIP_TYPE_WHITELIST`)
  - world boundary mismatch
  - dangerous payload pattern / oversized payload
- Soft rules (`SOFT_WARNING`) keep effect but emit warning:
  - high content length and similar non-fatal risks.
- Output fields:
  - `director_trace.safety_report[]`
  - top-level `safety_report[]`
  - `trace_id` injected into each report entry at API layer.

## Test Results
- `orchestrator\.venv\Scripts\python.exe -m pytest orchestrator\tests -q` passed (`13`).
- `npm test -- --runInBand` in frontend passed (`4/4`).
- Full Maven gate passed.
- `D:\apache-maven-3.9.12\bin\mvn.cmd -DskipTests compile` passed.
