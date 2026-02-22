# P19 Optimization Input (Post P19)

## What Worked
- Startup stability issue is now guarded by explicit context-load regression tests in scheduler/world.
- Trace propagation became consistent from frontend -> gateway -> orchestrator response artifacts.
- Replay/explain parsing overhead reduced by removing repeated payload parsing per channel.
- Memory compile path now avoids cross-world contamination in normal cases while preserving fallback compatibility.

## Remaining Gaps
- Unified error mapping is still code-string based and duplicated per service class.
- `AUTH_FAILED` style legacy code is not mapped to `401` unless renamed to `UNAUTHORIZED_*`.
- Gateway currently applies gradual enforcement by profile but does not yet centralize full resource-level auth policy.
- Maven plugin-version warnings remain and should be normalized in parent plugin management.

## Recommended Next Step (P20 Candidate)
1. Introduce shared exception-to-http mapper in `libs/common` to remove per-service duplication.
2. Standardize auth-related error code vocabulary (`UNAUTHORIZED_*`, `FORBIDDEN_*`) and migrate legacy codes.
3. Add contract tests for status-code semantics across representative services.
4. Add gateway integration tests for trace header propagation and enforce-mode behavior.
5. Normalize Maven plugin versions in parent to remove current warnings and harden build reproducibility.

