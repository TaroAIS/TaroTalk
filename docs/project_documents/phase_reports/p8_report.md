# P8 可观测与回放产品化报告

## 摘要
- 目标：把可回放能力从后端能力变成可直接使用的页面能力。
- 结果：后端增加聚合回放 API，前端增加 trace 回放页并补测试。

## 核心实现
- event-service：新增 `GET /api/v2/traces/{traceId}/replay/aggregate`。
- 数据结构：新增 `TraceAggregateResponse`，返回 `events + eventTypeCounts + sourceServiceCounts`。
- frontend：新增 `pages/trace/[traceId].tsx`、`__tests__/trace-page.test.tsx`，并在 `Layout` 加入口。

## 测试
- Frontend 门禁：4 suites 全通过（包含 trace-page）。
- Python 门禁：6 tests 通过。

## 风险与后续
- 风险：回放目前聚焦事件域，director/tool/state_effects 全聚合仍可继续扩展。
- 后续：进入 P9，做性能与韧性强化。
