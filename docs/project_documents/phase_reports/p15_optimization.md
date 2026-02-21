# P15 Optimization Input (for P16)

## What Worked
- Shadow mode fully decouples online behavior and experimental policy evaluation.
- Decision logs now carry enough context for offline analysis and replay extension.
- Reward backfill path is connected to real user interactions.

## Gaps
- Explainability output is still backend-internal; no aggregated debug API yet.
- Shadow decision reward is latest-record based and not yet trace-window aware.
- No UI surface to correlate director/tool/state effects with ranking decisions.

## Next-step Focus (P16)
- Add explain aggregation API in event-service.
- Extend replay payload to include ranking decisions and drift/safety metadata channels.
- Add internal debug trace page panels controlled by env switch.
