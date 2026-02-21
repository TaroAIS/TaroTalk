# P6 复盘与优化输入

## 复盘结论
- 安全入口已建立，但工作流与事件链路的基础设施化还未完成。

## 优化输入（给 P7）
- 在 `infra/docker-compose.yml` 加入 Kafka 与 Temporal。
- scheduler 支持 workflow 模式切换（direct/temporal）。
- 保留 direct 回退，避免环境不完整导致主链路不可用。
