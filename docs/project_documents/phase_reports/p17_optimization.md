# P17 Optimization Input (for P18)

## What Worked
- Goal economy turned speaker selection from pure relationship weighting into mixed utility optimization.
- world-service now exposes explicit economy introspection and evaluation APIs.
- Orchestrator bindings now carry explainable scoring factors for later replay usage.

## Gaps
- Safety checks on generated `state_effects` are still post-hoc and non-blocking.
- Goal economy objective matching is token-based and not semantically calibrated.
- Explain payload has no strict typed safety audit channel yet.

## Next-step Focus (P18)
- Add narrative safety linter in orchestrator before effect execution/return.
- Introduce hard-block and soft-warning policies with replay-visible reports.
- Expand explain output with first-class `safety_report`.
