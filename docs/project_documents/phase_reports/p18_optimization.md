# P18 Optimization Input (Post P18)

## What Worked
- Safety linter established a deterministic guardrail layer before final state effect output.
- Hard-block/soft-warning split improved control without fully sacrificing generation continuity.
- Safety evidence is now trace-attached and replay-compatible.

## Gaps
- Current rules are static and pattern-based; policy-as-config is not yet available.
- Tool execution is guarded pre-call, but broader compensating actions are still minimal.
- Safety severity scoring is not yet linked to runtime throttling or alerting.

## Suggested Next Focus
- Externalize safety policy to config/service.
- Add policy versioning in safety report for reproducibility.
- Wire safety metrics into observability dashboards and alert pipeline.
