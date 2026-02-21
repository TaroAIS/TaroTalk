# TaroTalk 工程深度文档（设计 / 架构 / 思考 / 挑战 / 技术贡献）

## 1. 项目定位与设计目标

### 1.1 项目定位
TaroTalk 的定位不是传统 IM，而是一个“纯 AI 代理社交系统”的工程化原型。系统以用户自述为输入，生成 persona 后自动创建 self-agent 与多类 AI 联系人，并在聊天与朋友圈场景中持续演化代理行为。

核心目标不是“做出一次回答”，而是“让多代理在长时程内保持行为一致、关系可演化、状态可追踪”。

### 1.2 设计目标（优先级）
1. **一致性**：LLM 输出必须可落为业务状态，不能只停留在文本层。
2. **可演化**：Agent 策略可独立迭代，不破坏核心业务服务。
3. **可追踪**：关键链路要可审计、可回放、可定位问题。
4. **可解释**：群聊中“谁说话、为什么说”需要有确定性机制。
5. **可降级**：外部依赖失败时，主流程可继续运行。

### 1.3 非目标边界
- 不以超大并发或高 QPS 作为本阶段目标。
- 不追求一次性覆盖所有复杂社交规则，优先打通闭环并保证工程质量。
- 不把模型能力等同于系统能力，强调“编排 + 数据 + 状态”的协同。

---

## 2. 总体架构与分层职责（Why）

### 2.1 采用 Java 微服务 + Python 编排层的原因
TaroTalk 选择混合架构：
- **Java 微服务层**（Spring Boot/Spring Cloud）负责业务状态与一致性，承载账户、会话、消息、动态、通知、关系等核心数据。
- **Python Orchestrator 层**（FastAPI）负责 Agent 编排、工具调用、多轮推理与剧情推进。

该架构的核心收益：
- 把“高变化的推理策略”与“高稳定性的业务状态”解耦。
- 避免每次调整 prompt 或角色策略都改动核心业务服务。
- 让 LLM 能力以可控方式进入业务域（通过工具注册表和结构化输出）。

### 2.2 服务边界
- 网关统一入口：`gateway/src/main/resources/application.yml`
- 编排入口：`orchestrator/app/main.py`
- 聊天核心：`services/chat-service/src/main/java/com/tarotalk/chat/service/MessageService.java`
- 朋友圈核心：`services/feed-service/src/main/java/com/tarotalk/feed/service/FeedService.java`
- 通知记忆源：`services/notification-service/src/main/java/com/tarotalk/notification/service/NotificationService.java`
- 关系图谱：`services/relationship-service/src/main/java/com/tarotalk/relationship/service/RelationshipService.java`

### 2.3 实时链路与后台链路
- **实时链路**：Client -> Gateway -> Chat Service -> Orchestrator -> Tool Executor -> Chat/Feed/Relationship
- **后台链路**：Scheduler -> Orchestrator `/a2a/simulate` -> Tool Executor -> 业务服务

参考实现：
- 调度触发：`services/scheduler-service/src/main/java/com/tarotalk/scheduler/service/TaskSchedulerService.java`
- 编排执行：`orchestrator/app/orchestrator.py`

---

## 3. A2A 编排内核设计（What + How）

### 3.1 A2A 协议设计思路
A2A（Agent-to-Agent）在本项目中不是简单“让模型输出多句文本”，而是一个可执行协议：
- 输入包含会话、参与者、发送者、persona、上下文。
- 输出同时保留兼容文本 `reply` 与结构化 `turns`、`role_user_map`。
- 通过工具调用将决策映射为真实业务动作。

关键模型定义：`orchestrator/app/models.py`

### 3.2 角色绑定：从参与者列表到 Agent 语义角色
在 `OrchestratorEngine._build_role_bindings` 中实现：
- `self-agent` 固定绑定 `sender_id`。
- 其他参与者先尝试读取关系服务中的类型与权重。
- 无明确关系时按候选角色回退（friend/mentor/rival/advertiser）。
- 重名角色通过后缀自动去冲突。

参考实现：`orchestrator/app/orchestrator.py`

### 3.3 导演机制：关系权重驱动 + 稳定顺序
在 `pick_round_speakers` 中实施：
- Round 1 强制 `self-agent` 发言。
- Round 2+ 按权重排序选发言者，分数相同按参与者顺序稳定打破。
- 群聊每轮最多 2 位发言者（控制输出密度）。

参考实现：`orchestrator/app/director.py`

### 3.4 多轮工具调用执行环
在 `run_chat` 内采用“规划 -> 工具执行 -> 回写 -> 再规划”的循环：
- 每轮最多 `max_steps=3`。
- 每步最多 `max_tool_calls=3`。
- 当返回 `tool_calls` 时，交给 `ToolExecutor` 执行并以 `tool` 角色写回上下文。
- 当返回内容符合角色格式时，解析为结构化 turn。

参考实现：
- 编排循环：`orchestrator/app/orchestrator.py`
- 工具执行：`orchestrator/app/tool_executor.py`
- 工具注册表：`orchestrator/app/tools.py`

### 3.5 向后兼容策略
为了避免影响旧调用方：
- 保留 `reply` 作为兼容字段。
- 同时新增 `turns` 与 `role_user_map` 供新调用方逐条落库。

这一策略在 `orchestrator/app/models.py` 与 `services/chat-service/.../OrchestratorClient.java` 中共同实现。

### 3.6 为什么不是“单轮直出”
单轮直出的主要问题：
- 无法可靠执行工具调用。
- 无法保证多代理发言的角色边界。
- 无法把动作沉淀为可追踪状态。

因此采用多轮编排 + 工具调用 + 结构化输出的组合，牺牲部分实现复杂度，换取可维护性和可解释性。

---

## 4. 聊天链路工程化落地

### 4.1 服务端上下文自动补齐
为了降低前端负担、避免上下文遗漏：
- 当请求未提供 context 时，聊天服务自动读取最近 20 条消息构造上下文。
- 自己消息映射为 `user`，其他参与者映射为 `assistant`。

参考实现：`MessageService.buildContextIfMissing`，文件路径：
`services/chat-service/src/main/java/com/tarotalk/chat/service/MessageService.java`

### 4.2 按 turn 拆分持久化（解决消息归属问题）
当 Orchestrator 返回结构化 turns：
- 聊天服务逐条写入 `ChatMessage`，并使用 turn 内 user_id 作为 sender。
- 每条消息单独触发 WebSocket 推送。
- 会话 `lastMessageId` 指向最后一条 AI turn。

参考实现：`persistOrchestratorTurns`，同上文件。

### 4.3 降级策略
- 若 orchestrator 不可用或未返回 turns，先尝试 `reply`。
- 若 reply 为空，再回退到 AI Service 单回复模式。
- fallback sender 选择首个非当前 sender 的参与者，避免“AI 回复被写成自己发的”。

参考实现：`chooseFallbackAiSender`、`aiClient.generateReply` 调用逻辑。

### 4.4 实时事件能力
聊天服务补齐 typing/read 事件推送能力，保持前端实时体验：
- typing：`publishTyping`
- read：`markRead`

参考实现：`MessageService` 与 `websocket` 包。

---

## 5. 朋友圈事件记忆闭环设计

### 5.1 设计原则
目标是让“动态行为”成为后续对话中的可引用记忆，而不是一次性通知。

### 5.2 事件标准化
Feed 侧把三类事件统一推入通知域：
- FEED_CREATED
- FEED_LIKED
- FEED_COMMENTED

并将 `feedId/authorId/actorId/summary` 序列化进 content。

参考实现：
`services/feed-service/src/main/java/com/tarotalk/feed/service/FeedEventPublisher.java`

### 5.3 记忆注入机制
Orchestrator 侧按角色并发拉取通知记忆：
- 每角色拉取 FEED_* 事件
- 解析 JSON 内容
- 去重（type + feedId + actorId）
- 限量（每角色最多 5 条）
- 注入 round prompt 的 `Memory for [role]` 块

参考实现：`_load_memories`、`_fetch_feed_memories`、`_normalize_memory_item`、`_build_memory_block`，文件：
`orchestrator/app/orchestrator.py`

### 5.4 可见性与排序策略
Feed 服务提供 contact / relationship / hybrid 可见策略，并加入时间衰减与互动加权排序：
- `score = timeDecay + interactionScore + relationshipWeight`
- 互动计数：like/comment
- viewer 维度：likedByViewer

参考实现：`services/feed-service/src/main/java/com/tarotalk/feed/service/FeedService.java`

---

## 6. 数据模型与多存储协同

### 6.1 AI-only 联系人与 self-agent 约束
在用户域引入 `UserType` 与 `ownerUserId`：
- AI/BRAND 用户必须有 owner。
- 联系人创建时拒绝 HUMAN 类型。
- 联系人查询默认过滤 HUMAN，保证通讯录 AI-only。

参考实现：
- `services/user-service/src/main/java/com/tarotalk/user/service/UserService.java`
- `services/user-service/src/main/java/com/tarotalk/user/service/ContactService.java`
- `services/user-service/src/main/java/com/tarotalk/user/domain/UserType.java`

### 6.2 多存储职责边界
- JPA/H2(Postgres 可替换)：用户、persona、feed、notification 等事务型实体。
- Mongo：聊天消息文档。
- Neo4j：关系网络与权重。
- Redis：缓存与在线态支持。

该边界设计对应“强一致状态”与“高频消息/关系查询”不同特性。

### 6.3 UUID 映射一致性问题与修复
曾出现“写入成功但按 UUID 查询失败”的问题，根因是 H2 上 UUID JDBC 映射不一致。修复策略：
- JPA 服务统一配置 `hibernate.type.preferred_uuid_jdbc_type: CHAR`。

参考配置示例：
- `services/user-service/src/main/resources/application.yml`
- `services/feed-service/src/main/resources/application.yml`
- `services/notification-service/src/main/resources/application.yml`
- `services/persona-service/src/main/resources/application.yml`
- `services/chat-service/src/main/resources/application.yml`
- `services/auth-service/src/main/resources/application.yml`

---

## 7. 稳定性、安全与可观测性

### 7.1 链路追踪
在 common 库统一注入 TraceId 过滤器：
- 透传/生成 `X-Trace-Id`
- 挂载 MDC `traceId/userId`
- 响应头回写 traceId

参考实现：
- `libs/common/src/main/java/com/tarotalk/common/web/TraceIdFilter.java`
- `libs/common/src/main/java/com/tarotalk/common/web/LoggingAutoConfiguration.java`

### 7.2 失败隔离与降级
- Tool 调用失败返回 warning 日志，不阻断 feed 主流程。
- 外部依赖不可用时，编排层回退空记忆或空关系，不抛硬错误。
- 聊天侧 orchestrator 异常仍可回落 AI 单回复路径。

### 7.3 关系写入安全硬化
关系类型采用双层约束：
1. 请求模型白名单约束。
2. 服务层正则约束。

避免任意关系类型拼接导致的 Cypher 注入风险。

参考实现：
- `services/relationship-service/src/main/java/com/tarotalk/relationship/api/RelationshipUpdateRequest.java`
- `services/relationship-service/src/main/java/com/tarotalk/relationship/service/RelationshipService.java`

---

## 8. 关键挑战与问题复盘（现象 -> 根因 -> 修复 -> 预防）

### 挑战 1：多代理回复归属错误
- **现象**：AI 多角色回复被合并成单条消息，sender 归属错误。
- **根因**：上游仅返回字符串，聊天层缺少角色级结构。
- **修复**：A2A 响应增加 `turns` 与 `role_user_map`，Chat 按 turn 拆分落库。
- **预防**：新增 `MessageServiceTest` 覆盖 turn 归属与 fallback sender 场景。
- **证据**：`orchestrator/app/models.py`、`services/chat-service/.../MessageService.java`、`services/chat-service/.../MessageServiceTest.java`

### 挑战 2：群聊角色顺序漂移
- **现象**：多次触发同一群聊，发言角色顺序不稳定。
- **根因**：缺少稳定排序与明确 tie-break。
- **修复**：引入关系权重排序 + 参与者顺序打破平分，Round1 固定 self-agent。
- **预防**：通过 orchestrator 单测固定轮次与角色断言。
- **证据**：`orchestrator/app/director.py`、`orchestrator/tests/test_orchestrator.py`

### 挑战 3：事件记忆串角色与冗余
- **现象**：代理可能引用不属于自身视角的动态记忆，或记忆重复刷屏。
- **根因**：缺少角色级拉取、去重与限流。
- **修复**：按 role->user_id 拉取通知，解析结构化内容，去重并限制条数。
- **预防**：统一 Memory block 格式，提升 prompt 可验证性。
- **证据**：`orchestrator/app/orchestrator.py`

### 挑战 4：UUID 查询失效
- **现象**：创建成功但按 UUID 查询返回 404 或空结果。
- **根因**：H2 与 Hibernate UUID 映射策略不一致。
- **修复**：统一 JPA UUID JDBC 类型为 CHAR。
- **预防**：在各服务模板配置中保持 UUID 映射一致。
- **证据**：各 JPA 服务 `application.yml`

### 挑战 5：外部依赖缺失导致链路中断
- **现象**：Mongo/Neo4j/LLM Key 缺失时，聊天或 persona 链路失败。
- **根因**：依赖条件未满足；某些流程缺少明确降级。
- **修复**：在编排和服务调用处增加容错；补充脚本化测试入口和健康检查。
- **预防**：通过 `scripts/test-*.sh` 与 `scripts/test-all.sh` 明确依赖与执行顺序。
- **证据**：`scripts/_common.sh`、`scripts/test-all.sh`

---

## 9. 个人技术贡献映射（职责 + 深度 + 影响）

| 贡献主题 | 关键动作 | 技术深度 | 影响 |
| --- | --- | --- | --- |
| 双层架构落地 | 设计 Java 业务层 + Python 编排层边界 | 将“状态一致性”和“推理策略”解耦 | 降低 Agent 策略迭代对核心服务侵入 |
| A2A 编排实现 | 多轮规划、工具调用、角色导演、结构化输出 | 将 LLM 能力转化为可执行协议 | 支持群聊/单聊可追踪 Agent 协作 |
| 聊天链路工程化 | turn 级落库与 sender 纠偏、上下文自动补齐 | 解决多代理落库一致性问题 | 提升可审计性与前端渲染稳定性 |
| 事件记忆闭环 | Feed -> Notification -> Memory -> 对话引用 | 事件化设计与低耦合记忆注入 | 形成跨会话“可感知、可引用”的社交记忆 |
| 稳定性与安全治理 | UUID 映射修复、关系类型白名单、traceId | 跨存储一致性 + 图谱写入安全 | 降低隐性数据错误与注入风险 |

---

## 10. 设计思考与参考依据

### 10.1 思考框架
本项目的关键思考不是“如何让模型回答更好”，而是“如何让系统在多代理、多事件、多存储条件下仍然可控”。

采用的工程思路：
1. **能力分层**：把生成能力封装为编排能力，再落地为业务能力。
2. **协议先行**：先定义输入输出结构，再优化 prompt 与模型策略。
3. **状态优先**：所有关键动作必须可持久化、可追踪、可审计。
4. **最小可闭环**：优先打通“可见 -> 互动 -> 反馈 -> 记忆”主路径。

### 10.2 参考模式（非逐字照搬）
- **ReAct / Tool Calling 思路**：将思考与动作分离，形成“规划-执行-观察”循环。
- **事件驱动设计**：把动态互动转换为可消费事件，支持跨上下文复用。
- **CQRS 风格边界**：读写逻辑按职责分层，保证演化时局部可替换。
- **确定性调度思想**：在多代理场景引入稳定排序与规则约束，减少随机漂移。

---

## 11. 实现路径复盘（怎么做的）

### 阶段 1：打通主闭环
- 完成用户注册、persona 生成、AI 联系人自举。
- 形成最小聊天链路与基础朋友圈能力。

### 阶段 2：引入编排与角色化
- 建立 A2A 接口，接入工具调用。
- 引入导演机制与角色风格约束。

### 阶段 3：工程化与一致性治理
- 将 A2A 输出结构化并在 chat 按 turn 持久化。
- 修复 UUID 映射一致性问题，补齐可观测与错误处理。

### 阶段 4：事件记忆闭环
- Feed 事件通知化。
- Orchestrator 记忆注入、去重、限流。
- 实现“朋友圈事件可在后续对话中被引用”。

---

## 12. 现阶段能力边界与后续演进路线

### 12.1 现阶段边界
- 具备可演示的工程化闭环，但仍是原型级系统。
- 关系权重和推荐策略是规则驱动，未进入学习化阶段。
- 记忆管理复用通知域，尚未引入专门长期记忆服务。

### 12.2 后续演进建议
1. **记忆生命周期管理**：增加衰减、优先级和冲突消解机制。
2. **排序学习化**：将 feed ranking 从规则权重过渡到可训练策略。
3. **编排回放平台**：按 traceId 回放导演决策与工具调用轨迹。
4. **策略灰度发布**：让角色导演策略按版本和人群灰度。
5. **一致性测试矩阵**：补充跨存储一致性与故障注入测试。

---

## 13. 文档与代码证据清单（便于评审/面试）

- 编排核心：`orchestrator/app/orchestrator.py`
- 导演策略：`orchestrator/app/director.py`
- 工具层：`orchestrator/app/tools.py`、`orchestrator/app/tool_executor.py`
- A2A 接口：`orchestrator/app/main.py`、`orchestrator/app/models.py`
- 聊天落库：`services/chat-service/src/main/java/com/tarotalk/chat/service/MessageService.java`
- 聊天编排客户端：`services/chat-service/src/main/java/com/tarotalk/chat/service/OrchestratorClient.java`
- 朋友圈与排序：`services/feed-service/src/main/java/com/tarotalk/feed/service/FeedService.java`
- 事件发布：`services/feed-service/src/main/java/com/tarotalk/feed/service/FeedEventPublisher.java`
- AI-only 联系人与 owner 约束：
  - `services/user-service/src/main/java/com/tarotalk/user/service/UserService.java`
  - `services/user-service/src/main/java/com/tarotalk/user/service/ContactService.java`
- 关系安全：
  - `services/relationship-service/src/main/java/com/tarotalk/relationship/api/RelationshipUpdateRequest.java`
  - `services/relationship-service/src/main/java/com/tarotalk/relationship/service/RelationshipService.java`
- 链路日志：`libs/common/src/main/java/com/tarotalk/common/web/TraceIdFilter.java`
- 测试入口：`scripts/test-all.sh`、`orchestrator/tests/test_orchestrator.py`

---

## 14. 总结
TaroTalk 的核心价值在于：将“生成式能力”从演示级 prompt 效果，推进到“可编排、可落库、可追踪、可治理”的工程系统能力。项目重点不在模型参数规模，而在系统化能力建设：

- 用 A2A 协议把多代理协作标准化；
- 用结构化输出把 LLM 结果转成业务状态；
- 用事件记忆闭环实现跨会话社交连续性；
- 用一致性与安全治理保证系统可持续演进。

这也是本项目在后端与 AI 工程化维度上的核心技术贡献。

## P11 Engineering Notes (2026-02-21)
- Memory compile path consumes world events and event-log payload summaries.
- Dedupe key: `world_id + owner_id + source_event_id + summary_hash`.
- Salience decay is applied on read/compile path; expired rows are filtered.
- Auth test baseline hardened by mocking `UserProfileClient` to avoid external dependency in gate runs.

## P12 Engineering Notes (2026-02-21)
- Causal edge relation is generated from event order with dedupe checks.
- `traceId` can constrain edge building to a single trace context.
- Replay aggregate edge output is currently sequence-based for deterministic output.

## P13 Engineering Notes (2026-02-21)
- What-if scoring is deterministic hash-based for reproducibility in MVP.
- Branch persistence includes trace id, policy, reason, and serialized event list.
- Main world-state write path is intentionally untouched in branch recording.

## P14 Engineering Notes (2026-02-21)
- Drift scoring uses role-style keyword overlap + persona overlap + novelty signal.
- Regeneration prompt rewrites drifting role lines once.
- Persistent drift triggers per-round speaker deweight.
