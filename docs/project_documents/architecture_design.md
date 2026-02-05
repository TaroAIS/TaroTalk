# 架构设计文档

## 系统高层架构
系统采用前后端分离的微服务架构，分为客户端层、网关层、业务服务层、翻译层、基础能力层和基础设施层。

1. **客户端层**：包括 Web 前端和移动端应用，负责 UI 展现、用户交互和基本数据处理，通过 HTTP/WebSocket 与网关层通信。
2. **网关层**：API 网关统一暴露 RESTful、GraphQL 和 WebSocket 接口，实现鉴权、路由、限流、协议适配和日志审计。
3. **业务服务层**：由多个独立的微服务组成，每个服务完成单一职责，例如 Auth、User、Persona、Chat、Feed、Presence、Notification、Relationship、Scheduler、AI 等，通过内部 gRPC/REST 调用。
4. **翻译层**：封装基础能力层的访问，包括数据库访问库、消息队列 SDK、缓存客户端等，为业务层屏蔽底层技术细节。
5. **基础能力层**：提供存储、缓存、消息队列、向量数据库、图数据库、AI 接口等技术服务。
6. **基础设施层**：运维相关，如容器平台、服务发现、负载均衡、监控、日志、配置中心等。

### 示例交互流程
1. **用户注册与 persona 生成**：
   1. 客户端调用 Auth Service 完成注册并获取 Token。
   2. 客户端提交用户自述信息到 Persona Service。
   3. Persona Service 调用 AI 接口生成 persona，存入关系数据库和向量数据库，返回 persona 摘要。
   4. Scheduler 服务初始化用户的 AI 联系人，调用 Relationship Service 建立关系。

2. **发送消息**：
   1. 客户端通过 WebSocket 连接 Chat Service 发送消息。
   2. Chat Service 按会话路由消息并写入消息存储（Cassandra/MongoDB）。
   3. Presence Service 更新 sender 的在线状态，Notification Service 向离线接收者发送通知。
   4. 若消息由 AI 代理发送，则 Chat Service 调用 AI Service 生成回复，按相同流程返回。

3. **动态发布与推荐**：
   1. 用户或 AI 调用 Feed Service 发布动态；Feed Service 将动态写入数据库并发送事件到 Kafka。
   2. 推荐子系统消费事件并更新推荐算法输入。
   3. 用户浏览动态时，Feed Service 根据用户画像、关系和历史行为调用推荐算法返回动态流。

## 模块划分
- **Auth Service**：身份认证与授权。
- **User Service**：用户及好友关系管理。
- **Persona Service**：生成与更新用户/AI 的 persona。
- **Chat Service**：会话和消息管理；提供 WebSocket 通道；集成 AI 回复生成。
- **Feed Service**：朋友圈动态的发布、查询和推荐。
- **Presence Service**：在线状态监控和心跳管理。
- **Notification Service**：推送通知到移动设备、邮件或短信。
- **Relationship Service**：使用图数据库维护用户与 AI 的关系网络。
- **Scheduler Service**：定时调度 AI 行为、任务和广告推送。
- **AI Service**：封装 LLM API 调用，为其他服务提供文本生成、对话管理。
- **Gateway/Translate Layer**：SDK 封装数据库、中间件等基础能力。

## 数据流与控制流
- **数据流**：消息、动态、事件等数据主要在客户端、Chat Service、Feed Service、存储层之间流动；AI Service 会调用 LLM 处理文本并将结果返回业务层。
- **控制流**：用户的操作通过 API 网关路由到对应服务，服务间通过异步消息队列或同步 RPC 调用协调；Scheduler Service 根据定时任务触发 AI 行为或系统维护任务。

## 第三方系统集成
- **LLM 提供商**：例如 OpenAI、DeepSeek；通过 AI Service 调用 RESTful API。
- **推送服务**：集成 APNs/FCM 发送移动推送；集成邮件服务（如 SendGrid）和短信网关。
- **支付与广告平台**（可选）：若需内购或广告变现，可接入支付宝/微信支付、广告平台，并通过第三方 SDK 集成。
- **社交分享**：调用系统分享 SDK 转发动态。
