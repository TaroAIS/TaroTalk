# P7 Kafka + Temporal 落地报告

## 摘要
- 目标：提供可在本地落地联调的事件/工作流基础设施。
- 结果：compose 增加 Kafka、Temporal、Temporal UI，并在 scheduler 加入 temporal 提交路径。

## 核心实现
- `infra/docker-compose.yml`：新增 `zookeeper`、`kafka`、`temporal`、`temporal-ui`。
- `scheduler-service`：
  - 配置新增 `integrations.temporal-service.base-url`。
  - `scheduler.workflow.mode` 支持 `temporal`。
  - temporal 提交失败时自动回退 direct 调度。

## 测试
- 本地代码层面通过 Python/Frontend 门禁。
- Java 与 compose 全链联调需在具备 `mvn + docker` 的统一环境补执行。

## 风险与后续
- 风险：当前 temporal 接入为“提交 + 回退”模式，尚未完整补偿编排。
- 后续：进入 P8，落地可视化回放能力。
