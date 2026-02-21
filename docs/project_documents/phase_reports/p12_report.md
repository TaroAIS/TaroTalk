# P12 Report - Causal Story Graph MVP

## Scope
- Added world-level causal edge model and causal graph APIs.
- Enhanced trace aggregate replay to expose `causal_edges`.
- Added unit tests for causal graph build and replay aggregate shape.

## Code Changes
- `services/world-service/src/main/java/com/tarotalk/world/domain/WorldCausalEdge.java`
- `services/world-service/src/main/java/com/tarotalk/world/repo/WorldCausalEdgeRepository.java`
- `services/world-service/src/main/java/com/tarotalk/world/repo/WorldEventRepository.java`
- `services/world-service/src/main/java/com/tarotalk/world/api/CausalEdgeResponse.java`
- `services/world-service/src/main/java/com/tarotalk/world/service/WorldService.java`
- `services/world-service/src/main/java/com/tarotalk/world/api/WorldController.java`
- `services/world-service/src/test/java/com/tarotalk/world/WorldServiceTest.java`
- `services/event-service/src/main/java/com/tarotalk/event/api/TraceAggregateResponse.java`
- `services/event-service/src/main/java/com/tarotalk/event/api/EventController.java`
- `services/event-service/src/test/java/com/tarotalk/event/EventControllerTest.java`

## API Additions
- `POST /api/v2/worlds/{worldId}/causal/build?traceId=`
  - Builds causal edges from ordered world events (trace-aware when provided).
- `GET /api/v2/worlds/{worldId}/causal?rootEventId=&depth=`
  - Traverses causal edges from a root event with bounded depth.
- `GET /api/v2/traces/{traceId}/replay/aggregate`
  - Added `causal_edges` in response payload.

## Test Results
- `D:\\apache-maven-3.9.12\\bin\\mvn.cmd -pl services/world-service,services/event-service -am test` passed.
- `orchestrator\\.venv\\Scripts\\python.exe -m pytest orchestrator\\tests -q` passed (`7`).
- `npm test -- --runInBand` in frontend passed (`4/4`).
- Full Maven gate passed:
  - `D:\\apache-maven-3.9.12\\bin\\mvn.cmd -pl libs/common,gateway,services/auth-service,services/user-service,services/persona-service,services/chat-service,services/feed-service,services/notification-service,services/relationship-service,services/scheduler-service,services/event-service,services/world-service -am test`
- Compile gate passed:
  - `D:\\apache-maven-3.9.12\\bin\\mvn.cmd -DskipTests compile`
