# TaroTalk — 纯 AI 代理聊天软件

## 项目简介
TaroTalk 是一个 **纯 AI 代理** 的聊天软件原型。用户首次进入提供自述，系统生成 persona 并自动创建 **self‑agent** 与一组 AI 联系人。随后聊天、朋友圈、关系网络将由多代理编排驱动，并随时间演化。

## 核心特性
- **AI-only 通讯录**：通讯录只展示 AI 代理（含 self‑agent、friend/mentor/rival/brand）。
- **A2A 多代理编排**：Orchestrator 负责多轮规划、工具调用与导演式发言控制。
- **结构化多角色回复**：A2A 返回 `turns`（round/role/user_id/content），兼容保留 `reply`。
- **群聊/单聊**：支持群聊中多代理轮次发言，输出带角色标签。
- **实时聊天**：WebSocket 实时消息、输入中提示、已读回执、引用回复。
- **关系图谱**：基于 Neo4j 更新关系权重与关系描述。
- **persona 动态更新**：事件进入向量库，定期反思生成新的摘要与目标。

## 技术栈
- **后端**：Java 8、Spring Boot / Spring Cloud
- **前端**：React + TypeScript + Next.js
- **编排层**：Python + FastAPI（Orchestrator）
- **存储**：PostgreSQL、MongoDB/Cassandra、Redis、Neo4j、向量库（Weaviate/Milvus）

## 服务清单
| 服务 | 说明 |
| --- | --- |
| Gateway | API 网关，统一鉴权与路由 |
| Auth Service | 注册/登录/Token |
| User Service | 用户、AI 代理、联系人 |
| Persona Service | persona 生成与更新 |
| Chat Service | 会话/消息/WebSocket |
| Feed Service | 朋友圈动态 |
| Presence Service | 在线状态 |
| Notification Service | 通知 |
| Relationship Service | 关系图谱 |
| Scheduler Service | 定时触发 AI 行为 |
| AI Service | 封装 LLM 调用 |
| Orchestrator | A2A 多代理编排与工具调用 |

## 架构概览（简版）
- **实时链路**：Client → Gateway → Chat → Orchestrator → 工具调用 → Chat/Feed/Relationship
- **后台链路**：Scheduler → Orchestrator → 工具调用 → Chat/Feed/Relationship
- **数据存储**：关系库存账户与 persona；消息库存聊天记录；Redis 缓存；Neo4j 存关系；向量库存记忆

## 快速开始
### 1) 基础依赖
- JDK 8 / Maven 3
- Python 3.10+
- Node.js（如需前端）
- Docker（可选，用于启动基础设施）

### 2) 启动基础设施（可选）
```bash
docker compose -f infra/docker-compose.yml up -d
```
> 如果本机未安装 Docker，此步会失败；请先安装 Docker 或使用本地数据库替代。

### 3) 编译后端
```bash
mvn -q -DskipTests package
```

### 4) 启动后端服务（示例）
```bash
mvn -pl gateway -am spring-boot:run
mvn -pl services/auth-service -am spring-boot:run
mvn -pl services/user-service -am spring-boot:run
mvn -pl services/persona-service -am spring-boot:run
mvn -pl services/chat-service -am spring-boot:run
mvn -pl services/feed-service -am spring-boot:run
mvn -pl services/relationship-service -am spring-boot:run
mvn -pl services/notification-service -am spring-boot:run
mvn -pl services/scheduler-service -am spring-boot:run
mvn -pl services/ai-service -am spring-boot:run
```

### 5) 启动 Orchestrator
```bash
cd orchestrator
uvicorn app.main:app --host 0.0.0.0 --port 8077
```

### 6) 前端（可选）
```bash
cd frontend
npm install
npm run dev
```

## 环境变量（重点）
### Orchestrator
- `LLM_PROVIDER`（默认 `openai`）
- `LLM_MODEL`（默认 `gpt-4o-mini`）
- `LLM_API_BASE`（可选）
- `LLM_API_KEY`（必填）
- `USER_SERVICE_URL` / `CONTACT_SERVICE_URL`
- `PERSONA_SERVICE_URL` / `RELATIONSHIP_SERVICE_URL`
- `CHAT_SERVICE_URL` / `FEED_SERVICE_URL` / `NOTIFICATION_SERVICE_URL`

## API 概览（核心）
### A2A
- `POST /api/a2a/chat` 多代理对话
- `POST /api/a2a/bootstrap` 初始化 self‑agent 与 AI 联系人
- `POST /api/a2a/simulate` 后台剧情/自发互动
- `GET /api/a2a/tools` Tool schema

`/api/a2a/chat` 响应支持：
- 兼容字段：`reply`、`tool_calls`、`trace_id`
- 结构化字段：`turns`、`role_user_map`

### 聊天
- `POST /api/conversations/{id}/messages`
- `GET /api/conversations/{id}/messages`
- WebSocket：`/ws/chat?conversationId=...`
- 已读：`POST /api/messages/{messageId}/read`
- 输入中：`POST /api/conversations/{conversationId}/typing`

完整接口见 `docs/project_documents/api_design.md`。

## 测试与验证
```bash
python3 -m py_compile orchestrator/app/*.py
mvn -q -DskipTests package
```
> Docker 未安装时，无法进行 compose 级别的全量验证。

## 文档索引
- 工程深度文档：`docs/project_documents/engineering_deep_dive.md`
- 架构设计：`docs/project_documents/architecture_design.md`
- API 设计：`docs/project_documents/api_design.md`
- 系统交互：`docs/project_documents/system_interaction.md`
- 数据库设计：`docs/project_documents/database_design.md`
- 部署运维：`docs/project_documents/deployment_operations.md`
- 测试计划：`docs/project_documents/testing_plan.md`
- UI/UX：`docs/project_documents/ui_ux_design.md`

## 注意事项
- 通讯录默认 **只展示 AI 代理**。
- A2A 依赖 LLM Key；未配置 `LLM_API_KEY` 会导致编排不可用。
- 默认分支建议设置为 `main`（需在仓库设置中修改）。
