# P16 Report - Internal Explainability UI MVP

## Scope
- Added trace explain aggregation API in event-service.
- Added frontend trace page multi-panel view (Timeline/Causal/Ranking/Safety).
- Added internal debug entry guard via `NEXT_PUBLIC_INTERNAL_DEBUG`.

## Code Changes
- `services/event-service/src/main/java/com/tarotalk/event/api/EventController.java`
- `services/event-service/src/main/java/com/tarotalk/event/api/TraceExplainResponse.java`
- `services/event-service/src/test/java/com/tarotalk/event/EventControllerTest.java`
- `frontend/pages/trace/[traceId].tsx`
- `frontend/components/Layout.tsx`
- `frontend/__tests__/trace-page.test.tsx`
- `frontend/__tests__/layout.test.tsx`

## Behavior
- New endpoint: `GET /api/v2/traces/{traceId}/explain`
  - Aggregates replay events plus explain channels:
  - `director_trace`, `tool_calls`, `state_effects`, `causal_edges`, `bandit_decisions`, `drift_decisions`, `safety_report`.
- Frontend trace page now calls explain endpoint and renders multi-panel diagnostics.
- Trace nav entry is hidden unless `NEXT_PUBLIC_INTERNAL_DEBUG=true`.

## Test Results
- `D:\apache-maven-3.9.12\bin\mvn.cmd -pl services/event-service -am test` passed.
- `npm test -- --runInBand` in frontend passed (`4/4`, 7 tests).
- `orchestrator\.venv\Scripts\python.exe -m pytest orchestrator\tests -q` passed (`10`).
- Full Maven gate passed.
- `D:\apache-maven-3.9.12\bin\mvn.cmd -DskipTests compile` passed.
