# P10 发布就绪报告

## 摘要
- 目标：形成可上线、可回滚、可观测的发布交付形态。
- 结果：完成阶段文档闭环，形成 P3.0 -> P10 的执行链与发布 runbook。

## 发布就绪项
- 协议主路径：V2 作为默认主路径（feed/chat/replay/simulate）。
- 兼容策略：V1 保留过渡周期，逐步收缩写路径。
- 风险控制：
  - 网关鉴权开关可控（`security.jwt.enforce`）。
  - scheduler 支持 direct/temporal 双模式回退。
  - event 幂等键减少重复写污染。
- 追踪能力：`X-Trace-Id` 从入口到事件回放可串联。

## 测试结果
- Python 门禁：通过。
- Frontend 门禁：通过。
- Java 门禁：当前会话环境无 `mvn`，无法复跑；使用 surefire 历史成功产物作为辅助证据。

## 发布文档
- 发布/回滚 runbook：`p10_release_runbook.md`。
- 阶段总索引：`index.md`。

## 残余风险
- 需要在具备 Maven 环境的 CI 或本机补做一次 Java 全量回归。
- Kafka/Temporal 的生产化参数（认证、持久化、容量）仍需按环境二次落参。
