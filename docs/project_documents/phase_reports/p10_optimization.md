# P10 后续优化建议

## 已完成闭环
- 协议、任务、事件、回放、网关、基础设施、文档链路已形成可迭代基线。

## 建议继续推进（P10+）
1. 把 Java 全量门禁固定到 CI，避免本地依赖差异。
2. 把 outbox + dispatcher 做成默认事件派发路径，替换同步写扩散。
3. 完善 replay 聚合：合并 director_trace/tool_calls/state_effects。
4. 建立容量压测与故障注入常态化脚本（Kafka 堆积、world/event 不可用）。
5. 收敛 V1 生命周期，按发布窗口删除 V1 写接口。
