# 系统交互文档

## Agent 与系统的交互

### Persona 构建与维护
1. **创建 persona**：当用户首次提供描述时，Persona Service 调用 AI Service，发送包含用户描述的 Prompt，LLM 返回结构化的 persona，包括性格关键词、兴趣、职业等。Persona Service 将结果保存到关系数据库并生成嵌入向量存入向量数据库。
2. **自举 AI 联系人**：Persona 创建完成后，Persona Service 调用 Orchestrator 的 /a2a/bootstrap，生成 self-agent 与多名 AI 联系人；User/Contact/Relationship 服务建立 AI-only 通讯录与关系边。
3. **记忆事件记录**：每当用户或 AI 代理发生行为（发送消息、发布动态、点赞等），业务服务生成一个 Event 记录写入事件表，并异步推送到 AI Service。
4. **反思与总结**：Scheduler Service 周期性触发任务，从向量数据库检索与当前主题或目标相关的记忆，生成总结并更新 persona 描述；更新后通知相关服务（如 Feed 推荐）刷新缓存。
5. **关系网络更新**：Relationship Service 监听事件流，根据互动频次和亲密度调整 Neo4j 中的边权重；当权重达到阈值时触发系统推送（如新推荐的 AI 角色）。

### 对话生成流程
1. 当客户端发送消息到 AI 代理时，Chat Service 将消息作为用户输入，调用 Orchestrator；
2. Orchestrator 构建 Prompt：包含当前对话上下文、相关记忆、双方 persona 以及高层目标，并进行导演式轮次控制；
3. 导演机制采用“关系权重驱动”：首轮固定 self-agent，后续轮次按 intimacy/commercial/interaction 计算权重选择 1~2 位代理发言（群聊最多 2 位）；
4. LLM 返回多角色回复文本；Orchestrator 在需要时调用工具接口（send_message/post_feed/update_relationship 等）；
5. Orchestrator 输出结构化 `turns`（含 role/user_id/round/content），Chat Service 按 turn 逐条落库并逐条推送 WebSocket；
6. 若未返回 `turns`，Chat Service 回退为单条回复写入，保证兼容。

### 朋友圈事件闭环（通知驱动 + 记忆注入）
1. **C 发布动态**：Feed Service 写入 Feed，并基于“作者通讯录可见”获取联系人列表；
2. **通知传播**：对每个可见联系人创建 FEED_CREATED 通知（作为事件记忆）；
3. **B 对话提及**：Orchestrator 拉取 B 的 FEED_* 通知并注入上下文，促使在与 A 对话中提及；
4. **A 点赞反馈**：A 点赞后触发 FEED_LIKED 通知，通知给 C；
5. **C 再次感知**：C 后续对话时读取 FEED_LIKED 记忆并可提及。

### 记忆注入策略（A2A）
1. Orchestrator 并发拉取每个角色的 FEED_* 通知记忆；
2. 优先解析通知 `content` JSON（feedId/authorId/actorId/summary），解析失败降级为原文；
3. 以 `(type, feedId, actorId)` 去重并限量（每角色最多 5 条）；
4. 按 `Memory for [role]` 块注入 prompt，提升跨代理事件引用的一致性与可追溯性。

### 可见动态列表
1. 客户端请求 `/api/feeds?viewerId=...`；
2. Feed Service 调用 User Service `/api/contacts/owners?contactUserId=...` 获取“可见作者列表”；
3. Feed Service 返回这些作者的最新动态列表。

### 自动剧情与任务调度
- Scheduler Service 根据配置规则（如每天 9:00）触发 Orchestrator /a2a/simulate 推进剧情。
- Orchestrator 调用 Chat/Feed/Relationship 等工具接口发布消息与动态。
- 所有自动生成的消息和动态都记录事件，参与亲密度计算。

## 错误处理与异常管理
- **客户端错误**：前端在调用 API 时捕获异常并展示友好提示；对于网络错误支持重试。
- **服务间调用错误**：微服务通过统一的异常格式返回错误码和错误信息；Gateway 统一封装错误响应。
- **AI 生成错误**：如果 AI Service 超时或返回不当内容，系统返回默认回复并记录日志；管理员可查看错误样本用于调整 Prompt。
- **回退策略**：当 WebSocket 连接失败时，客户端自动改用 HTTP 长轮询；数据库写入失败时走重试队列；消息发送失败时保存草稿。
- **降级与熔断**：当某服务不可用或响应过慢时，通过服务发现将流量切换到备用实例；对外暴露的接口启用限流，保证系统稳定。

## 高可用与恢复能力
- 所有服务部署至少两个实例，使用负载均衡分配请求；关键服务如 Chat、Presence 在多机房部署。
- 消息和事件写入采用异步队列 + 重试机制，防止单点故障导致数据丢失。
- 使用事务与幂等设计确保重复请求不会产生副作用。
- 定期进行灾难恢复演练，验证数据库备份和恢复流程。

## P11 Interaction Flow (2026-02-21)
1. Client/orchestrator triggers `POST /api/v2/worlds/{worldId}/memories/compile`.
2. world-service compiles memory items from `world_event` and `event_log` data.
3. Orchestrator chat reads `GET /api/v2/worlds/{worldId}/memories?ownerId=...` first.
4. If world memory is empty/unavailable, orchestrator falls back to notification feed memories.

## P12 Interaction Flow (2026-02-21)
1. world-service receives build request for causal graph.
2. service links adjacent world events into `STATE_EFFECT` edges.
3. trace replay aggregate emits `causal_edges` for UI/ops diagnosis.

## P13 Interaction Flow (2026-02-21)
1. Client calls orchestrator what-if API.
2. Orchestrator builds candidate branches and recommendation.
3. Orchestrator writes branch scenarios to world-service in dry-run mode.
4. Caller receives branch list without mutating primary timeline.

## P14 Interaction Flow (2026-02-21)
1. Orchestrator generates raw turn candidates.
2. Drift guard scores each turn by role consistency.
3. Drifting turns are regenerated once; unresolved turns are deweighted/filtered.
4. Drift decisions are persisted in director trace.

## P15 Interaction Flow (2026-02-21)
1. Client reads feed list through existing API path.
2. feed-service computes baseline ranking and returns baseline order.
3. In shadow mode, feed-service writes parallel bandit decisions (`chosen` from baseline top).
4. User like/comment/unlike updates latest shadow decision reward for `(viewerId, feedId)`.
