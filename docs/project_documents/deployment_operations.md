# 部署与运维文档

## 部署流程

1. **环境准备**
   - 为开发、测试、预发布和生产环境准备 Kubernetes 集群，配置网络和存储。
   - 配置持续集成/持续部署（CI/CD）系统，如 Jenkins、GitHub Actions 或 GitLab CI，并创建凭证和配置仓库。

2. **构建与测试**
   - 开发者提交代码到主仓库后触发 CI 流程，自动执行单元测试和静态分析。
   - 构建前端应用（Next.js/Flutter）和后端各微服务的 Docker 镜像，打上版本标签。
   - 运行集成测试和性能测试，若测试通过则推进部署阶段。

3. **镜像推送**
   - 将构建好的 Docker 镜像推送到私有镜像仓库（如 Harbor、GitHub Container Registry）。

4. **部署到 Kubernetes**
   - 使用 Helm 或 Kustomize 管理 Kubernetes 清单文件，配置 Deployment、Service、Ingress、ConfigMap、Secret 等。
   - 在测试环境先行部署，运行回归测试。
   - 使用灰度发布或蓝绿部署策略逐步将新版本发布到生产环境；监控关键指标，如请求延迟、错误率、CPU 内存使用率。
   - Orchestrator 作为独立服务部署（Python/FastAPI），需要配置 LLM 与下游服务地址：
     - `LLM_API_KEY` / `LLM_API_BASE` / `LLM_MODEL`
     - `USER_SERVICE_URL` / `CONTACT_SERVICE_URL` / `PERSONA_SERVICE_URL`
     - `RELATIONSHIP_SERVICE_URL` / `CHAT_SERVICE_URL` / `FEED_SERVICE_URL` / `NOTIFICATION_SERVICE_URL`

5. **数据库迁移**
   - 发布新版本前执行数据库迁移脚本（Liquibase/Flyway），保证表结构一致。
   - 对关系数据库进行备份，确保有回滚方案。

6. **回滚策略**
   - 在发布过程中检测到重大故障时，立即回滚至上一个稳定版本；保留旧版本的容器和数据库备份。

## 运维手册

### 监控与告警
- 使用 Prometheus 收集服务指标，如请求数、错误率、响应时间、CPU/内存。
- 使用 Grafana 配置仪表盘展示整体健康状况；针对每个服务设置阈值告警。
- 利用 ELK/EFK 堆栈集中管理日志，通过 Kibana 搜索异常日志。
- 使用 Jaeger/Zipkin 实现分布式链路追踪，定位性能瓶颈。

### 日志管理
- 每个微服务将结构化日志输出到 stdout，由容器平台收集。
- 日志字段包含时间戳、traceId、spanId、level、serviceName、message，便于关联。
- 设置日志保留周期和归档策略，防止磁盘占满。

### 故障处理
- **连接故障**：检查网络、负载均衡和 DNS 配置；查看 WebSocket 心跳和 Presence 状态。
- **性能下降**：查看监控指标，确认是否为资源瓶颈；扩容相应服务或增加缓存。
- **AI 服务异常**：查看外部 API 日志和返回码，调整重试机制或切换备用模型。

### 数据备份与恢复
- 定期（每日/每小时）对关系数据库和消息数据库进行增量备份；每周一次全量备份。
- 图数据库和向量数据库使用厂商提供的备份工具进行快照。
- 将备份存储在不同区域或云存储，防止同机房故障。
- 定期演练恢复流程，确保备份可用。

### 安全与权限管理
- 对外暴露的端口通过防火墙/安全组限制访问。
- 管理员界面和内部调试接口仅在内网开放，通过 VPN 或跳板机访问。
- 定期轮换密钥和证书，使用自动化工具管理 Secret。
- 使用 SAST/DAST 工具扫描代码安全问题，及时修复。

### 持续优化
- 根据业务增长定期评估容量规划，优化服务分片数量、缓存策略和数据库索引。
- 收集用户反馈并根据数据分析调整推荐算法和 UI 交互。

## P11 Runtime Config (2026-02-21)
- world-service now exposes event-service integration key:
  - `integrations.event-service.base-url`
- world-service HTTP client settings:
  - `http.client.connect-timeout-ms`
  - `http.client.read-timeout-ms`
  - `http.client.max-attempts`
  - `http.client.retry-backoff-ms`

## P12 Ops Note (2026-02-21)
- No new external infra required for causal edge MVP.
- Causal edge table is managed by JPA schema auto-update in current local profile.

## P13 Ops Note (2026-02-21)
- Branch scenario records are persisted in `world_branch_scenario` table.
- Branch simulation remains API-first; no user-facing page required in this phase.
