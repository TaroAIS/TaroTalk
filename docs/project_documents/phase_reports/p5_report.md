# P5 事件可靠投递报告

## 摘要
- 目标：降低重复事件污染，增强事件可回放一致性。
- 结果：event-service 完成幂等键建模与去重写入。

## 核心实现
- `InternalEventRequest` 新增 `idempotencyKey`。
- `EventLog` 新增 `idempotency_key`，并加唯一约束 `uk_event_source_idempotency`。
- `EventLogRepository` 增加按 source+idempotency 查询。
- `EventService.append(...)` 支持幂等去重（存在则直接返回旧事件）。
- `EventWriteResponse` 与 `EventResponse` 增加幂等键回显。

## 测试
- Java（最近 surefire）：event-service 1 test, 0 failures。
- 说明：仍需在具备 `mvn` 环境时补一轮全量重跑。

## 风险与后续
- 风险：当前仍是同步写路径，尚未落地 outbox 异步派发。
- 后续：进入 P6，补安全边界与网关统一鉴权。
