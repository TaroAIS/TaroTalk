# P14 Optimization Input (for P15)

## What Worked
- Drift guard now provides explicit governance over role-style consistency.
- Regeneration path improves unstable turns without requiring external moderation service.
- Drift decisions are replayable through director trace metadata.

## Gaps
- Drift score is still rule-based and not calibrated against labeled data.
- Deweight is round-local and does not persist across session horizon.
- No ranking feedback loop from user interactions yet.

## Next-step Focus (P15)
- Add bandit shadow ranking to feed-service.
- Capture decision logs and rewards (like/comment) without changing current ranking output.
- Keep default user-visible order unchanged in shadow mode.
