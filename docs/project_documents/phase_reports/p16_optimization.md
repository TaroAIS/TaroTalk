# P16 Optimization Input (for P17)

## What Worked
- Explain API now provides one-shot debug aggregation, reducing manual cross-service lookup.
- Frontend debug page exposes causal/ranking/safety channels in a single trace context.
- Internal debug switch prevents accidental exposure of engineering-only tools.

## Gaps
- Explain channels are payload-extraction based; semantic normalization is still weak.
- Ranking/safety views are generic JSON rendering, not domain-typed cards.
- Goal-driven agent economics is still missing from director scoring.

## Next-step Focus (P17)
- Introduce goal economy model in world-service and explicit utility evaluation API.
- Wire goal utility into orchestrator speaker scoring.
- Add tests for budget/utility-driven speaker selection behavior.
