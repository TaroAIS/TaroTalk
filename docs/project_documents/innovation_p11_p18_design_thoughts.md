# Innovation P11-P18 Design Thoughts

## P11 Memory Compiler
- Design goal: convert transient interaction events into durable social memory primitives.
- Design tradeoff: use deterministic compile + dedupe first, defer background compaction to later phases.
- Compatibility: fully additive to V2, no V1 contract breaks.

## P12 Causal Story Graph
- Design goal: make storyline transitions auditable as explicit cause-effect links.
- Current strategy: deterministic sequence graph first, model-based confidence calibration deferred.

## P13 What-if Timeline
- Design goal: evaluate alternative story evolutions without contaminating mainline state.
- Design strategy: deterministic branch generation + explicit dry-run persistence for audit.

## P14 Role Drift Guard
- Design goal: keep character consistency under multi-round, multi-role generation.
- Design strategy: detect -> regenerate once -> deweight as deterministic safety fallback.

## P15 Bandit Ranking (Shadow)
- Design goal: introduce online-learnable ranking signals without changing current feed UX.
- Design strategy: baseline rank serves traffic; bandit rank logs decisions + rewards in shadow.
- Tradeoff: learnability improves, but immediate ranking gain is deferred until policy switch phase.

## P16 Internal Explainability UI
- Design goal: make trace-level decision evidence visible to engineers in one place.
- Design strategy: backend explain aggregation + frontend multi-panel debug page.
- Security posture: debug entry is gated by environment flag to avoid accidental external exposure.
