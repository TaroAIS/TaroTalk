# P9 性能与韧性报告

## 摘要
- 目标：从“能跑”升级为“稳跑”，减少慢依赖和重复失败对主链路影响。
- 结果：完成 HTTP 参数化、索引强化、调度退避、编排器工具层容错升级。

## 核心实现
- HTTP 客户端增强：
  - `RestTemplateFactory` 新增状态码重试（429/5xx）与线性退避。
  - `ai/auth/persona/chat/feed/scheduler` 配置类改为参数化注入。
- Feed 排序参数化：
  - `FeedService` 引入 `feed.ranking.weights.*` 配置。
  - 权重归一化计算，避免硬编码耦合。
- 数据索引增强：
  - `Feed`、`FeedInteraction`、`EventLog`、`WorldEvent`、`ScheduledTask` 增加关键索引。
- 调度韧性：
  - `TaskSchedulerService` 增加 `max-retries` 与指数退避。
  - 达到阈值后自动禁用任务，避免无穷重试。
- 编排器工具韧性：
  - `tool_executor.py` 增加统一请求重试与超时。
  - `like_feed` 切到 `/api/v2/feeds/{feedId}/like`，支持 `action`。
  - `tools.py` 更新工具 schema。

## 测试
- Python：`pytest orchestrator/tests -q` -> 6 passed。
- Frontend：`npm test -- --runInBand` -> 4 suites passed。
- Java：当前环境缺失 `mvn`，本轮无法重新执行 Java 回归。

## 风险与后续
- 风险：Java 最新改动尚未在本机完成 `mvn test` 复核。
- 后续：进入 P10，补发布就绪文档与回滚 runbook。
