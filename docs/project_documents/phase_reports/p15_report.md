# P15 Report - Bandit Ranking Shadow MVP

## Scope
- Added feed ranking shadow decision model and persistence.
- Added shadow policy scoring service (`LinUCB/epsilon-greedy style` deterministic MVP).
- Added reward backfill on feed interactions (like/comment/unlike).
- Kept user-visible feed ordering unchanged in shadow mode.

## Code Changes
- `services/feed-service/src/main/java/com/tarotalk/feed/domain/FeedRankingDecision.java`
- `services/feed-service/src/main/java/com/tarotalk/feed/repo/FeedRankingDecisionRepository.java`
- `services/feed-service/src/main/java/com/tarotalk/feed/service/BanditPolicyService.java`
- `services/feed-service/src/main/java/com/tarotalk/feed/service/FeedService.java`
- `services/feed-service/src/main/resources/application.yml`
- `services/feed-service/src/test/java/com/tarotalk/feed/FeedBanditShadowTest.java`

## Behavior
- Ranking still uses existing baseline weights and ordering.
- When `feed.ranking.bandit.mode=shadow`:
  - feed candidates are shadow-scored and written to `feed_ranking_decision`.
  - `chosen` marks baseline selected top result (no rank rewrite).
  - decision context captures baseline score + feature vector + shadow score.
- Interaction reward path:
  - `LIKE`: +1 reward
  - `COMMENT`: +2 reward
  - `UNLIKE`: -1 reward
  - reward is applied to latest `(viewerId, feedId)` decision.

## Test Results
- `D:\apache-maven-3.9.12\bin\mvn.cmd -pl services/feed-service -am test` passed.
- `orchestrator\.venv\Scripts\python.exe -m pytest orchestrator\tests -q` passed (`10`).
- `npm test -- --runInBand` in frontend passed (`4/4`).
- Full Maven gate passed.
- `D:\apache-maven-3.9.12\bin\mvn.cmd -DskipTests compile` passed.
