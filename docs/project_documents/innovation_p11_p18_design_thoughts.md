# Innovation P11-P18 Design Thoughts

## P11 Memory Compiler
- Design goal: convert transient interaction events into durable social memory primitives.
- Design tradeoff: use deterministic compile + dedupe first, defer background compaction to later phases.
- Compatibility: fully additive to V2, no V1 contract breaks.
