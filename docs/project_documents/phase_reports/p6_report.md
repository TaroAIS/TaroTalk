# P6 安全闭环报告

## 摘要
- 目标：形成最小可用的网关鉴权闭环。
- 结果：新增网关 JWT 过滤器，支持解析 Bearer token 与用户上下文透传。

## 核心实现
- `gateway/src/main/java/.../JwtGatewayFilter.java`：
  - 公共路径白名单。
  - 受保护路径匹配。
  - JWT 解析后注入 `X-User-Id`、`X-Role`。
  - 支持 `security.jwt.enforce` 开关。
- `gateway/.../application.yml` 增加 `security.jwt.secret` 与 `security.jwt.enforce`。
- 路由继续收敛，移除外部 `/internal/events/**`。

## 测试
- 前端与编排层门禁通过，验证主调用链未被错误拦截。
- 网关专门自动化安全测试尚需补全（见后续优化）。

## 风险与后续
- 风险：当前默认 `enforce=false`，属于兼容优先策略。
- 后续：进入 P7，引入 Kafka/Temporal 基础设施并验证本地联调。
