# 测试计划

## 单元测试
### 前端
- 使用 Jest 和 React Testing Library 编写组件单元测试，覆盖主要 UI 组件的渲染、交互和状态变化。
- 使用 Cypress/Playwright 做端到端测试，模拟用户注册、发送消息、发布动态等流程。
- 验证国际化文本替换正确，表单验证符合规则。

### 后端
- 使用 JUnit5 编写各微服务的业务逻辑测试，包括 Auth、User、Persona、Chat、Feed、Notification、Relationship 等。
- 使用 MockMvc/RestAssured 测试 REST API 返回的状态码、响应体和错误处理。
- 对 AI Service 使用 Mock 工具（如 WireMock）模拟 LLM 调用，验证 Prompt 构建和返回处理逻辑。
- 对数据库访问层编写 Repository 测试，使用 Testcontainers 启动临时数据库。

### Agent
- 测试 Persona 生成：模拟用户描述输入，验证 persona 模板渲染和字段解析。
- 测试记忆检索：构造事件向量并查询最近的记忆条目；验证召回结果与预期相关。
- 测试对话生成：给定 persona、上下文和记忆，检查生成回复的格式和长度；使用脱敏数据评估语义正确性。

## 集成测试
- **服务集成**：在测试环境启动部分微服务，通过 API 网关调用完整流程，如注册→生成 persona→创建会话→发送消息→AI 回复。
- **前后端集成**：使用 Cypress 连接前端与后端接口，模拟真实用户操作。
- **自动化脚本**：编写脚本定期运行集成测试套件，并在 CI 流程中阻断不通过的代码合并。

## 性能测试
- **负载测试**：使用 JMeter 或 Locust 模拟高并发用户发送消息、发布动态和查看信息流，记录响应时间与错误率。
- **容量测试**：增加消息和会话数量，观察数据库性能和缓存命中率。
- **WebSocket 压力测试**：模拟大量客户端建立 WebSocket 连接并发送心跳，验证 Presence Service 的可扩展性。

## 安全测试
- **渗透测试**：模拟恶意用户攻击，包括 SQL 注入、XSS、CSRF、暴力破解等，检查系统防护。
- **越权测试**：验证不同角色对资源的访问权限，确保无未授权访问。
- **数据加密验证**：检查密码和敏感数据是否正确加密存储，消息加密端到端是否可行。
- **依赖扫描**：使用工具（如 OWASP Dependency-Check）扫描依赖库漏洞。

## 可用性与恢复测试
- **故障注入**：使用 Chaos Engineering 框架（如 Chaos Monkey）随机关闭服务实例，验证熔断和重试逻辑。
- **灾备恢复**：模拟数据库故障，验证备份和恢复流程。
- **滚动更新测试**：在预生产环境模拟版本升级，确保不影响现有连接。

## 测试工具
- 前端：Jest, React Testing Library, Cypress/Playwright。
- 后端：JUnit5, Mockito, Spring Boot Test, Testcontainers, Gatling。
- 性能：JMeter, Locust。
- 安全：OWASP ZAP, Burp Suite。

## P11 Test Additions (2026-02-21)
- world-service:
  - memory dedupe test.
  - expired memory filtering test.
- orchestrator:
  - world memory priority and fallback test.
- auth-service:
  - controller test isolated from external user-service dependency.

## P12 Test Additions (2026-02-21)
- world-service causal graph build test.
- event-service replay aggregate causal edge schema test.

## P13 Test Additions (2026-02-21)
- orchestrator what-if branch ordering/recommendation test.
- world-service branch scenario persistence test with no world-state writes.

## P14 Test Additions (2026-02-21)
- Drift threshold detection test.
- Drift regeneration + deweight path test.
- Stable output no-extra-call regression test.

## P15 Test Additions (2026-02-21)
- `FeedBanditShadowTest`:
  - verifies shadow mode keeps baseline ordering unchanged.
  - verifies shadow decisions are persisted with chosen marker.
  - verifies like/comment interaction updates bandit reward.

## P16 Test Additions (2026-02-21)
- event-service:
  - explain API output channel extraction test.
- frontend:
  - trace page explain endpoint rendering test.
  - layout trace-entry debug gate test (`NEXT_PUBLIC_INTERNAL_DEBUG`).
