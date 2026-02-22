# P19 Report - P0+P1 Mainline Stabilization and Consistency

## Scope
- Phase target: P0+P1 mainline fixes on `feature/init`.
- Execution policy: 3 themed implementation commits (startup stability, trace/auth consistency, error/replay/memory consistency).
- Non-goals: no new infra, no breaking API rename, no business flow redesign.

## Deliverables
### Commit 1
- Commit: `01c662f`
- Message: `fix: stabilize scheduler/world startup and context wiring`
- Changes:
  - Fixed constructor injection ambiguity in:
    - `services/scheduler-service/src/main/java/com/tarotalk/scheduler/service/TaskSchedulerService.java`
    - `services/world-service/src/main/java/com/tarotalk/world/service/WorldService.java`
  - Added context load regressions:
    - `services/scheduler-service/src/test/java/com/tarotalk/scheduler/SchedulerContextLoadTest.java`
    - `services/world-service/src/test/java/com/tarotalk/world/WorldContextLoadTest.java`

### Commit 2
- Commit: `7d4fa28`
- Message: `feat: propagate trace and add gradual auth path`
- Changes:
  - Orchestrator request models support optional `trace_id`:
    - `orchestrator/app/models.py`
  - `chat/simulate/what-if` now prefer request trace id and fallback to generated trace:
    - `orchestrator/app/main.py`
  - Gateway gradual auth and trace propagation:
    - `gateway/src/main/java/com/tarotalk/gateway/config/JwtGatewayFilter.java`
    - `gateway/src/main/resources/application-prod.yml` (`security.jwt.enforce=true`)
  - Frontend API client auto-attaches bearer token (if present) while keeping `X-Trace-Id`:
    - `frontend/lib/api.ts`
  - Added tests:
    - `orchestrator/tests/test_main_trace.py`
    - `frontend/__tests__/api-client.test.ts`

### Commit 3
- Commit: `9b9734c`
- Message: `refactor: unify error semantics and tighten replay/memory correctness`
- Changes:
  - Unified `ApiException` -> HTTP status mapping in all service `RestExceptionHandler` classes:
    - `NOT_FOUND -> 404`
    - `FORBIDDEN* -> 403`
    - `UNAUTHORIZED* -> 401`
    - `VALIDATION_ERROR -> 400`
    - fallback `-> 400`
  - Event explain parsing optimization:
    - `services/event-service/src/main/java/com/tarotalk/event/api/EventController.java`
    - switched to injected `ObjectMapper`
    - payload JSON parse once + reuse in explain aggregation channels
  - World memory compile world-scoped query first + fallback path:
    - `services/world-service/src/main/java/com/tarotalk/world/service/WorldService.java`
    - prioritize `/api/v2/events?entityType=WORLD&entityId={worldId}`
    - fallback to legacy broad query when no world-scoped rows exist
  - Updated tests:
    - `services/event-service/src/test/java/com/tarotalk/event/EventControllerTest.java`
    - `services/world-service/src/test/java/com/tarotalk/world/WorldServiceTest.java`

## API and Behavior Notes
- Orchestrator V2-compatible request additions:
  - `trace_id` (optional) for chat/simulate/what-if request models.
- Gateway behavior:
  - dev profile keeps gradual auth (`security.jwt.enforce=false` from `application.yml`).
  - prod profile enforces bearer token (`application-prod.yml`).
  - `X-Trace-Id` propagation priority: incoming value first, generated fallback.
- Frontend request headers:
  - auto `Authorization: Bearer <token>` when token exists in localStorage key:
    - `NEXT_PUBLIC_AUTH_TOKEN_KEY` or default `tarotalk_token`.

## Test Evidence
### Commit 1 gate
- `D:\apache-maven-3.9.12\bin\mvn.cmd -pl services/scheduler-service,services/world-service -am test`
- Result: PASS

### Commit 2 gate
- `orchestrator\.venv\Scripts\python.exe -m pytest orchestrator\tests -q`
- `npm test -- --runInBand` (frontend)
- `D:\apache-maven-3.9.12\bin\mvn.cmd -pl gateway -am test`
- Result: PASS

### Commit 3 gate
- `D:\apache-maven-3.9.12\bin\mvn.cmd -pl services/event-service,services/world-service,services/chat-service,services/feed-service -am test`
- `D:\apache-maven-3.9.12\bin\mvn.cmd -DskipTests compile`
- Result: PASS

### Final full gate
- `orchestrator\.venv\Scripts\python.exe -m pytest orchestrator\tests -q` -> PASS (`16 passed`)
- `npm test -- --runInBand` -> PASS (`5 suites`, `9 tests`)
- `D:\apache-maven-3.9.12\bin\mvn.cmd -pl libs/common,gateway,services/auth-service,services/user-service,services/persona-service,services/chat-service,services/feed-service,services/notification-service,services/relationship-service,services/scheduler-service,services/event-service,services/world-service -am test` -> PASS
- `D:\apache-maven-3.9.12\bin\mvn.cmd -DskipTests compile` -> PASS

## Known Risks
- Services returning `AUTH_FAILED` (not `UNAUTHORIZED*`) now map to `400` under the unified rule set.
- Maven warning baseline remains: missing explicit `spring-boot-maven-plugin` version declarations across modules.
