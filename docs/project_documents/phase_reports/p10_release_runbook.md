# P10 发布与回滚 Runbook

## 1. 发布前检查
- 确认分支：`feature/init`。
- 确认配置：
  - `security.jwt.secret` 非默认值。
  - `scheduler.workflow.mode` 与基础设施一致（`direct` 或 `temporal`）。
  - 各服务 `http.client.*` 与环境容量匹配。
- 确认门禁：
  - `orchestrator/.venv/Scripts/python.exe -m pytest orchestrator/tests -q`
  - `npm test -- --runInBand`
  - Java 全量回归（在可用 Maven 环境执行）。

## 2. 发布步骤
1. 先发布 `event-service`、`world-service`、`scheduler-service`。
2. 再发布 `feed-service`、`chat-service`、`gateway`、`orchestrator`、`frontend`。
3. 灰度验证主链路：注册 -> persona -> bootstrap -> chat -> feed -> replay。
4. 观察指标：错误率、P95、任务失败率、重复事件率。

## 3. 回滚策略
1. 网关回退到上一稳定版本路由配置。
2. `scheduler.workflow.mode` 切回 `direct`（如 temporal 链路异常）。
3. 前端回退到上一稳定包版本。
4. 保持 event/world 数据只追加，不做破坏性回退。

## 4. 故障处置优先级
- P0：主链路不可用（聊天/发帖失败）
- P1：回放不可用或事件重复明显上升
- P2：排序异常、体验降级但核心功能可用

## 5. 发布完成标准
- 主链路冒烟全部通过。
- 关键错误率低于发布前阈值。
- 30 分钟观察窗口无持续恶化趋势。
