# P3.5 稳定化闭环报告

## 摘要
- 目标：消除 V1/V2 断层，补齐聊天和 feed 的核心边界。
- 结果：前端 Feed 切 V2，后端补 includeSelf 与参与者校验，trace 与事件元数据贯通。

## 核心实现
- Feed 前端联动：`frontend/pages/feed.tsx` 改为调用 `/api/v2/feeds/{feedId}/like`（`action: LIKE|UNLIKE`），评论走 `/api/v2/feeds/{feedId}/comments`，展示 `event_id`。
- Feed 可见性：`services/feed-service/.../FeedController.java` 新增 `includeSelf`；`FeedService.listVisible(...)` 改为 viewer 正向联系人集合 `/api/contacts/ids`。
- Chat 边界：`services/chat-service/.../MessageService.java` 在发送前校验 `senderId` 属于会话参与者，违规返回 `FORBIDDEN_NOT_PARTICIPANT`。
- Trace 贯通：新增 `TraceContext`，feed/chat 事件优先继承请求 trace，随机兜底。
- HTTP 客户端统一：引入 `RestTemplateFactory`，接入统一超时与重试拦截。
- 网关收敛：`gateway/.../application.yml` 去掉外部路由 `/internal/events/**`。

## 接口变化
- `GET /api/feeds` 增加 `includeSelf`（默认 `true`）。
- `POST /api/v2/feeds/{feedId}/like` 使用 toggle 语义，返回 `liked` + `event_id`。
- `POST /api/v2/conversations/{conversationId}/messages` 明确非参与者错误语义。

## 测试
- Python：通过（6 passed）。
- Frontend：通过（4 suites passed, 5 tests passed）。
- Java（最近一次 surefire 汇总）：
  - chat-service: 5 tests, 0 failures
  - feed-service: 16 tests, 0 failures
- 限制：本轮环境无法执行新的 `mvn test`（`mvn` 命令缺失）。

## 风险与后续
- 风险：Java 侧需补一次“本轮改动后的全量复验”。
- 后续：进入 P4，做调度任务持久化与状态机。
