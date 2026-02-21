# P11 Report - Memory Compiler MVP

## Scope
- Implemented world memory storage model and compile/list APIs.
- Added orchestrator memory priority: world memories first, notification memories as fallback.
- Ensured dedup behavior by `owner_id + source_event_id + summary_hash`.

## Code Changes
- `services/world-service/src/main/java/com/tarotalk/world/domain/MemoryItem.java`
- `services/world-service/src/main/java/com/tarotalk/world/repo/MemoryItemRepository.java`
- `services/world-service/src/main/java/com/tarotalk/world/service/WorldService.java`
- `services/world-service/src/main/java/com/tarotalk/world/api/WorldController.java`
- `services/world-service/src/main/java/com/tarotalk/world/api/MemoryCompileResponse.java`
- `services/world-service/src/main/java/com/tarotalk/world/api/MemoryItemResponse.java`
- `services/world-service/src/main/java/com/tarotalk/world/config/WorldConfig.java`
- `services/world-service/src/main/resources/application.yml`
- `orchestrator/app/orchestrator.py`
- `orchestrator/tests/test_orchestrator.py`
- `services/world-service/src/test/java/com/tarotalk/world/WorldServiceTest.java`
- `services/auth-service/src/test/java/com/tarotalk/auth/AuthControllerTest.java`

## API Additions
- `POST /api/v2/worlds/{worldId}/memories/compile`
  - Query params: `ownerId`, `limit`, `minSalience`.
  - Behavior: compile memories from world events and event log data, deduplicate, apply salience decay/expiry filtering, return compiled snapshot.
- `GET /api/v2/worlds/{worldId}/memories`
  - Query params: `ownerId`, `limit`, `minSalience`.
  - Behavior: return active memories ordered by salience.

## Test Results
- `orchestrator\\.venv\\Scripts\\python.exe -m pytest orchestrator\\tests -q`
  - Passed: `7`.
- `npm test -- --runInBand` (`frontend`)
  - Passed: all suites (`4/4`).
- `D:\\apache-maven-3.9.12\\bin\\mvn.cmd -pl libs/common,gateway,services/auth-service,services/user-service,services/persona-service,services/chat-service,services/feed-service,services/notification-service,services/relationship-service,services/scheduler-service,services/event-service,services/world-service -am test`
  - Passed.
- `D:\\apache-maven-3.9.12\\bin\\mvn.cmd -DskipTests compile`
  - Passed.

## Notes
- Full Maven gate initially failed due `auth-service` test hard dependency on local `user-service`.
- Resolved by mocking `UserProfileClient` in `AuthControllerTest`; this keeps gate reproducible in local CI-less environments.
