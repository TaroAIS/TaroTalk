# P4 工作流化起步报告

## 摘要
- 目标：把“定时触发 simulate”升级为“可追踪任务执行”。
- 结果：scheduler 引入持久任务实体、API 扩展、状态机执行和幂等注册。

## 核心实现
- 新增持久实体与仓储：`ScheduledTask`、`TaskStatus`、`ScheduledTaskRepository`。
- 请求/响应升级：`TaskRequest` 增加 `taskId/idempotencyKey/enabled/scheduleExpr/status`；新增 `TaskResponse`。
- 执行引擎升级：`TaskSchedulerService` 支持数据库注册/查询、状态迁移、幂等键去重。
- 控制器升级：`SchedulerController` 的 POST/GET 返回结构化任务视图。

## 接口变化
- `POST /api/scheduler/tasks` 返回 `taskId/status/nextRunAt/...`。
- `GET /api/scheduler/tasks` 返回任务状态与最近执行信息。

## 测试
- Java（最近 surefire）：scheduler-service 2 tests, 0 failures。
- 说明：新增退避逻辑后，待补一次本轮 Java 全量重跑确认。

## 风险与后续
- 风险：simulate 执行结果仍偏“触发成功即完成”，效果落库闭环需要继续加强。
- 后续：进入 P5，补事件去重与可靠回放语义。
