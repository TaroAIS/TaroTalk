# API 设计文档

## RESTful API

| 功能 | 方法 | 路径 | 简要描述 |
| --- | --- | --- | --- |
| 注册 | POST | /api/auth/register | 创建新用户账号，返回 JWT |
| 登录 | POST | /api/auth/login | 用户登录，返回 JWT |
| 刷新 Token | POST | /api/auth/refresh | 刷新访问 Token |
| 获取用户信息 | GET | /api/users/{userId} | 获取用户基本资料 |
| 更新用户资料 | PUT | /api/users/{userId} | 修改头像、昵称、标签 |
| 获取 persona | GET | /api/personas/{userId} | 获取用户/AI 的 persona 摘要 |
| 创建 persona | POST | /api/personas | 根据自述生成 persona |
| 更新 persona | PUT | /api/personas/{personaId} | 更新 persona 描述 |
| 获取联系人列表 | GET | /api/contacts | 分页返回通讯录联系人 |
| 获取联系人ID | GET | /api/contacts/ids | 返回通讯录联系人 userId 列表（AI-only、排除 blocked） |
| 获取可见作者 | GET | /api/contacts/owners | 返回把该 user 作为联系人添加过的作者 userId 列表 |
| 创建联系人 | POST | /api/contacts | 创建 AI 联系人（通讯录仅展示 AI） |
| 搜索联系人 | GET | /api/contacts/search | 通过关键词搜索 |
| 修改联系人分组 | PUT | /api/contacts/{contactId}/group | 设置分组或标签 |
| 创建会话 | POST | /api/conversations | 创建单聊或群聊 |
| 获取会话列表 | GET | /api/conversations | 列出用户所有会话 |
| 获取会话详情 | GET | /api/conversations/{conversationId} | 获取参与者、最近消息 |
| 发送消息 | POST | /api/conversations/{conversationId}/messages | 发送文本或多媒体消息 |
| 获取消息列表 | GET | /api/conversations/{conversationId}/messages | 分页获取历史消息 |
| 撤回/删除消息 | DELETE | /api/messages/{messageId} | 撤回或删除消息 |
| 消息已读 | POST | /api/messages/{messageId}/read | 标记消息已读 |
| 输入状态 | POST | /api/conversations/{conversationId}/typing | 上报输入中状态 |
| 上传媒体 | POST | /api/media/upload | 上传图片、视频或文件 |
| 发布动态 | POST | /api/feeds | 发布朋友圈动态 |
| 获取动态流 | GET | /api/feeds | 获取推荐的动态（可选参数：viewerId, visibility, limit, cursor） |
| 评论动态 | POST | /api/feeds/{feedId}/comments | 发表评论 |
| 点赞动态 | POST | /api/feeds/{feedId}/like | 点赞或取消点赞 |
| 通知列表 | GET | /api/notifications | 获取用户通知（可选参数：types, limit） |
| AI 回复 | POST | /api/ai/reply | 提交对话上下文，生成 AI 回复 |
| AI 动态 | POST | /api/ai/feed | 请求 AI 生成朋友圈动态 |
| A2A 对话 | POST | /api/a2a/chat | 多代理编排对话 |
| A2A 自举 | POST | /api/a2a/bootstrap | 初始化 self-agent 与 AI 联系人 |
| A2A 模拟 | POST | /api/a2a/simulate | 后台剧情/自发互动 |
| A2A 工具 | GET | /api/a2a/tools | Tool schema 查询 |
| 图关系查询 | GET | /api/relationships/{userId} | 查询用户与代理的关系 |
| 定时任务管理 | POST | /api/scheduler/tasks | 创建/更新 AI 行为计划 |

### 请求与响应示例
- **发送消息**：
  - 请求体示例：
    ```json
    {
      "senderId": "u123",
      "content": "你好！",
      "type": "text",
      "replyTo": null
    }
    ```
  - 响应示例（简要）：
    ```json
    {
      "messageId": "m456",
      "status": "sent",
      "sentAt": "2025-06-01T12:00:00Z"
    }
    ```

- **AI 回复**：
  - 请求体含 persona 描述、上下文消息和记忆检索结果；
  - AI Service 返回生成的文本以及所用的参考记忆 id。

- **通知列表过滤**：
  - 请求参数示例：
    ```
    /api/notifications?userId=...&types=FEED_CREATED,FEED_LIKED&limit=10
    ```

- **动态列表响应补充字段**：
  - `likeCount` / `commentCount` / `likedByViewer`

- **A2A 对话响应（增强，向后兼容）**：
  - 保留字段：`reply` / `tool_calls` / `trace_id`
  - 新增字段：
    - `turns`: 多角色结构化回复（`round`, `role`, `user_id`, `content`）
    - `role_user_map`: 角色到 userId 的映射
  - 响应示例（简要）：
    ```json
    {
      "reply": "[self-agent] 我先看看\n[friend] 我来补充",
      "turns": [
        {"round": 1, "role": "self-agent", "user_id": "u-self", "content": "我先看看"},
        {"round": 2, "role": "friend", "user_id": "u-friend", "content": "我来补充"}
      ],
      "role_user_map": {
        "self-agent": "u-self",
        "friend": "u-friend"
      },
      "trace_id": "..."
    }
    ```

## GraphQL Schema（可选）
为客户端提供更灵活的查询，可使用 GraphQL 聚合多服务数据。示例 schema 片段：

```graphql
type Query {
  me: User
  contacts(first: Int, after: String): ContactConnection
  conversation(id: ID!): Conversation
  feed(first: Int, after: String): FeedConnection
}

type Mutation {
  sendMessage(conversationId: ID!, input: MessageInput!): Message
  createPersona(input: PersonaInput!): Persona
  postFeed(input: FeedInput!): Feed
}

type Subscription {
  messageAdded(conversationId: ID!): Message
  feedUpdated: Feed
}
```

GraphQL 通过单一端点 `/graphql`，支持查询（Query）、变更（Mutation）和实时订阅（Subscription）；后端使用 Apollo Server 或 Spring GraphQL 实现，并通过 WebSocket 提供订阅功能。

## P11 API Additions (2026-02-21)
- `POST /api/v2/worlds/{worldId}/memories/compile`
  - Optional query: `ownerId`, `limit`, `minSalience`.
  - Returns compiled memory snapshot with dedup counters.
- `GET /api/v2/worlds/{worldId}/memories`
  - Optional query: `ownerId`, `limit`, `minSalience`.
  - Returns active memory list ordered by salience.

## P12 API Additions (2026-02-21)
- `POST /api/v2/worlds/{worldId}/causal/build?traceId=`
- `GET /api/v2/worlds/{worldId}/causal?rootEventId=&depth=`
- `GET /api/v2/traces/{traceId}/replay/aggregate` now includes `causal_edges`.

## P13 API Additions (2026-02-21)
- `POST /api/v2/a2a/simulate/what-if`
- `POST /api/v2/worlds/{worldId}/branches`
- `GET /api/v2/worlds/{worldId}/branches`

## P14 Protocol Additions (2026-02-21)
- `director_trace` now carries `drift_decisions[]` in orchestrator chat response.

## P15 API/Schema Notes (2026-02-21)
- No public API break for feed list/read path.
- New internal persistence schema:
  - `feed_ranking_decision(decision_id, viewer_id, feed_id, policy, context_json, score, chosen, reward, trace_id, created_at)`
- Reward updates are triggered by existing interaction APIs:
  - `POST /api/v2/feeds/{feedId}/like`
  - `POST /api/v2/feeds/{feedId}/comments`

## P16 API Additions (2026-02-21)
- `GET /api/v2/traces/{traceId}/explain`
  - Returns aggregated replay + explain channels:
  - `events`, `eventTypeCounts`, `sourceServiceCounts`, `causalEdges`,
  - `directorTrace`, `toolCalls`, `stateEffects`, `banditDecisions`, `driftDecisions`, `safetyReport`.

## P17 API Additions (2026-02-21)
- `GET /api/v2/worlds/{worldId}/goals/economy`
  - Returns goal economy rows with `utility` and economy factors.
- `POST /api/v2/worlds/{worldId}/goals/evaluate`
  - Request: `actorIds[]`, `objective`.
  - Response: evaluated goal utilities ordered by utility score.
