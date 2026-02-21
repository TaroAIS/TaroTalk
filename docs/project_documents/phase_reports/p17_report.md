# P17 Report - Goal Economy MVP

## Scope
- Extended `agent_goal` model with economy fields: `budget/expected_reward/risk_penalty/momentum`.
- Added world-service goal economy APIs:
  - `GET /api/v2/worlds/{worldId}/goals/economy`
  - `POST /api/v2/worlds/{worldId}/goals/evaluate`
- Integrated goal utility into orchestrator speaker scoring:
  - `speaker_score = relation_weight + goal_utility + recency_factor`.

## Code Changes
- `services/world-service/src/main/java/com/tarotalk/world/domain/AgentGoal.java`
- `services/world-service/src/main/java/com/tarotalk/world/service/WorldService.java`
- `services/world-service/src/main/java/com/tarotalk/world/api/WorldController.java`
- `services/world-service/src/main/java/com/tarotalk/world/api/GoalResponse.java`
- `services/world-service/src/main/java/com/tarotalk/world/api/GoalEconomyEvaluateRequest.java`
- `services/world-service/src/main/java/com/tarotalk/world/api/GoalEconomyResponse.java`
- `services/world-service/src/main/java/com/tarotalk/world/api/GoalEconomyEvaluateResponse.java`
- `services/world-service/src/test/java/com/tarotalk/world/WorldServiceTest.java`
- `orchestrator/app/orchestrator.py`
- `orchestrator/tests/test_orchestrator.py`

## Behavior
- Goal economy utility now reflects:
  - base score, priority, expected reward, momentum, risk penalty, and budget factor.
- Budget exhaustion causes utility degradation (deweight path).
- Orchestrator role bindings include:
  - `relation_weight`, `goal_utility`, `recency_factor`, final `weight`.
- Round speaker selection automatically reflects world goal economy output.

## Test Results
- `D:\apache-maven-3.9.12\bin\mvn.cmd -pl services/world-service -am test` passed.
- `orchestrator\.venv\Scripts\python.exe -m pytest orchestrator\tests -q` passed (`11`).
- `npm test -- --runInBand` in frontend passed (`4/4`).
- Full Maven gate passed.
- `D:\apache-maven-3.9.12\bin\mvn.cmd -DskipTests compile` passed.
