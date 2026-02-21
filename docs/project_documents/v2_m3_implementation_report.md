# TaroTalk V2 + M3 实施与验证报告

更新时间：2026-02-21  
基线分支：`feature/init`

## 1. 本次目标与范围
本次交付按“先补文档、再测试新增能力、再执行 M3”推进，范围覆盖：

1. V2 基础设施与协议骨架落地（事件域、世界域、A2A V2、聊天/动态/调度联动）。
2. M3 闭环落地（feed 传播网络、推荐公式升级、互动反哺关系网络）。
3. 自动化测试补齐并执行（Python/Java/Frontend）。

---

## 2. V2 已落地能力（摘要）

### 2.1 新增服务
1. `event-service`：支持事件写入、查询、trace 回放。
- `POST /internal/events`
- `GET /api/v2/events`
- `GET /api/v2/traces/{traceId}/replay`
- 关键实现：`services/event-service/src/main/java/com/tarotalk/event/api/EventController.java`

2. `world-service`：支持世界状态、时间线、事件注入、目标重计算。
- `GET /api/v2/worlds/{worldId}/state`
- `POST /api/v2/worlds/{worldId}/events`
- `GET /api/v2/worlds/{worldId}/timeline`
- `POST /api/v2/worlds/{worldId}/goals/recompute`
- 关键实现：`services/world-service/src/main/java/com/tarotalk/world/api/WorldController.java`

### 2.2 编排层 V2
1. 新增 `/api/v2/a2a/chat` 与 `/api/v2/a2a/simulate`。
2. chat 响应新增 `director_trace` 与 `state_effects`，保留 `reply/tool_calls/turns/role_user_map/trace_id` 兼容字段。
3. simulate 响应新增 `workflow_id/scheduled_events/status`。
- 关键实现：`orchestrator/app/main.py`、`orchestrator/app/models.py`、`orchestrator/app/orchestrator.py`

### 2.3 Chat/Feed/Scheduler 联动
1. `chat-service` 增加 V2 元数据落库字段：`role/round/traceId/source/effectRef`。
2. 新增 chat 事件双写：消息落库后写入 event-log。
3. `scheduler-service` 调用升级为 `/api/v2/a2a/simulate`（失败回退旧接口）。
- 关键实现：
- `services/chat-service/src/main/java/com/tarotalk/chat/service/MessageService.java`
- `services/chat-service/src/main/java/com/tarotalk/chat/service/ChatEventPublisher.java`
- `services/scheduler-service/src/main/java/com/tarotalk/scheduler/service/TaskSchedulerService.java`

---

## 3. M3 执行结果（feed 传播 + 推荐 + 网络闭环）

### 3.1 传播策略升级（Fanout V2）
从“仅联系人”升级为“联系人 + 关系 + 世界上下文”混合传播。

1. 新增 fanout 策略配置：
- `feed.fanout.strategy: hybrid`
- `feed.fanout.max-recipients: 60`
2. fanout 候选来源：
- user-service 联系人：`/api/contacts/ids`
- relationship-service 关系对象：`/api/relationships/{userId}`
- world-service 时间线 actor：`/api/v2/worlds/{worldId}/timeline`
3. 对点赞/评论传播时，自动排除操作者本人，避免回声通知。
- 关键实现：`services/feed-service/src/main/java/com/tarotalk/feed/service/FeedEventPublisher.java`

### 3.2 推荐公式升级（Ranking V2）
从旧 `time + interactions + relationship` 升级为：

`freshness * 0.35 + interaction_velocity * 0.25 + relationship_affinity * 0.20 + narrative_relevance * 0.20`

1. `freshness`：基于发布时间指数衰减。
2. `interaction_velocity`：按最近窗口互动密度计算（评论权重高于点赞）。
3. `relationship_affinity`：基于 intimacy/interaction/commercial 归一化权重。
4. `narrative_relevance`：基于世界时间线 actor 命中 + 关键词重合度。
- 关键实现：`services/feed-service/src/main/java/com/tarotalk/feed/service/FeedService.java`

### 3.3 互动网络闭环（Relationship Feedback）
点赞/评论/取消点赞会反哺 relationship-service，更新 actor -> author 关系强度。

1. LIKE：正向增益 intimacy + interactionCount。
2. COMMENT：更高正向增益 intimacy + interactionCount。
3. UNLIKE：轻微负向调整 intimacy。
4. 类型安全：仅使用白名单关系类型（默认 `friend`，兼容已有类型）。
- 关键实现：`services/feed-service/src/main/java/com/tarotalk/feed/service/FeedService.java`

---

## 4. 本次新增/变更接口

1. Feed V2：
- `POST /api/v2/feeds/{feedId}/like`，请求 `action: LIKE|UNLIKE`，幂等。
- `POST /api/v2/feeds/{feedId}/comments`，返回 `event_id`。
- 关键实现：`services/feed-service/src/main/java/com/tarotalk/feed/api/FeedV2Controller.java`

2. A2A V2：
- `POST /api/v2/a2a/chat`
- `POST /api/v2/a2a/simulate`

3. Worlds V2：
- `GET /api/v2/worlds/{worldId}/state`
- `POST /api/v2/worlds/{worldId}/events`
- `GET /api/v2/worlds/{worldId}/timeline`
- `POST /api/v2/worlds/{worldId}/goals/recompute`

4. Events V2：
- `GET /api/v2/events`
- `GET /api/v2/traces/{traceId}/replay`

---

## 5. 测试与验证结果

### 5.1 Python（Orchestrator）
命令：`orchestrator\\.venv\\Scripts\\python.exe -m pytest orchestrator\\tests -q`  
结果：`4 passed`

新增覆盖：
1. `director_trace/state_effects` 输出结构。
2. `run_simulation` 的 V2 返回 shape。

文件：`orchestrator/tests/test_orchestrator.py`

### 5.2 Java（Chat + Feed）
命令：`D:\\apache-maven-3.9.12\\bin\\mvn.cmd -pl services/feed-service,services/chat-service -am test`  
结果：`Tests run: 15, Failures: 0, Errors: 0`

新增/更新覆盖：
1. feed fanout 混合传播与 actor 排除。
2. ranking narrative relevance。
3. V2 like toggle 幂等。
4. comment 触发 relationship 反哺。
5. chat 测试适配 V2 编排签名与事件发布。
6. Feed V2 控制器集成路径（`/api/v2/feeds/{feedId}/like`、`/comments`）。

文件：
- `services/feed-service/src/test/java/com/tarotalk/feed/FeedEventPublisherTest.java`
- `services/feed-service/src/test/java/com/tarotalk/feed/FeedRankingTest.java`
- `services/feed-service/src/test/java/com/tarotalk/feed/FeedV2InteractionTest.java`
- `services/feed-service/src/test/java/com/tarotalk/feed/FeedControllerTest.java`
- `services/chat-service/src/test/java/com/tarotalk/chat/MessageServiceTest.java`

### 5.3 Frontend
命令：`npm test -- --runInBand`（目录 `frontend`）  
结果：`3 suites passed`

### 5.4 其他编译验证
命令：`D:\\apache-maven-3.9.12\\bin\\mvn.cmd -pl services/event-service,services/world-service,services/scheduler-service -am -DskipTests compile`  
结果：`BUILD SUCCESS`

---

## 6. 已知限制与后续建议

1. 当前 fanout 与 relationship 反哺是同步 HTTP，建议下一阶段切换为异步事件总线（Kafka + Outbox）。
2. world narrative relevance 目前是轻量关键词与 actor 匹配，建议后续替换为 embedding 相似度。
3. `spring-boot-maven-plugin` 版本缺失在多模块有 warning，建议统一补齐版本以消除构建隐患。
4. M4 可继续推进：simulate workflow 化（Temporal）、失败补偿与 trace 回放 UI。
5. `FeedControllerTest` 在本地未启动外部依赖时会出现连接拒绝警告日志（notification/event/world/relationship），这是当前“弱依赖容错”设计下的预期现象，不影响测试通过。
