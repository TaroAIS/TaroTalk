# P11 Optimization Input (for P12)

## What Worked
- Memory compile/list APIs are stable and testable.
- Orchestrator now has deterministic memory priority and fallback path.
- Dedup key avoids repeated writes from same source event summary.

## Gaps
- Causal relation between memory source and downstream state changes is not explicit yet.
- Replay aggregate API does not include memory/causal context.
- Current memory salience decay is request-time; no background compaction task yet.

## Next-step Focus (P12)
- Add `world_causal_edge` model and build endpoint.
- Extend replay aggregate response with `causal_edges`.
- Keep changes additive under V2 and avoid breaking existing replay consumers.
