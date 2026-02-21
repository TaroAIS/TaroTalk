# P12 Optimization Input (for P13)

## What Worked
- Causal edges are now persisted and queryable in world-service.
- Replay aggregate payload can display causal sequence immediately.

## Gaps
- Current causal confidence is heuristic, not model-calibrated.
- Replay aggregate `causal_edges` currently uses trace-order sequence, not world graph merge.
- Missing branch simulation API to compare alternate causal paths.

## Next-step Focus (P13)
- Implement what-if branching endpoint in orchestrator.
- Record branch scenarios in world-service without mutating primary world timeline.
- Return branch score + reason + recommended branch id for API consumers.
