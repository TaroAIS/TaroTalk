# P13 Report - What-if Timeline API MVP

## Scope
- Added orchestrator API for branch simulation:
  - `POST /api/v2/a2a/simulate/what-if`
- Added world-service branch scenario persistence (record-only, dry-run semantics).
- Added what-if tests for branch shape, ordering, and recommendation.

## Code Changes
- `orchestrator/app/models.py`
- `orchestrator/app/orchestrator.py`
- `orchestrator/app/main.py`
- `orchestrator/tests/test_orchestrator.py`
- `services/world-service/src/main/java/com/tarotalk/world/domain/WorldBranchScenario.java`
- `services/world-service/src/main/java/com/tarotalk/world/repo/WorldBranchScenarioRepository.java`
- `services/world-service/src/main/java/com/tarotalk/world/api/BranchScenarioRequest.java`
- `services/world-service/src/main/java/com/tarotalk/world/api/BranchScenarioResponse.java`
- `services/world-service/src/main/java/com/tarotalk/world/api/WorldController.java`
- `services/world-service/src/main/java/com/tarotalk/world/service/WorldService.java`
- `services/world-service/src/test/java/com/tarotalk/world/WorldServiceTest.java`

## API Additions
- `POST /api/v2/a2a/simulate/what-if`
  - Request extensions: `branch_count`, `selection_policy`, `dry_run`.
  - Response: `branches[]`, `recommended_branch_id`, `selection_policy`, `trace_id`.
- `POST /api/v2/worlds/{worldId}/branches`
  - Persists branch scenarios for audit only (no world-state mutation).
- `GET /api/v2/worlds/{worldId}/branches`
  - Queries recent recorded branch scenarios.

## Test Results
- `D:\\apache-maven-3.9.12\\bin\\mvn.cmd -pl services/world-service -am test` passed.
- `orchestrator\\.venv\\Scripts\\python.exe -m pytest orchestrator\\tests -q` passed (`8`).
- `npm test -- --runInBand` in frontend passed (`4/4`).
- Full Maven gate passed.
- Compile gate (`-DskipTests compile`) passed.
