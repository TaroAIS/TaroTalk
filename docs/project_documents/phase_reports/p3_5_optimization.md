# P3.5 复盘与优化输入

## 复盘结论
- 正向效果：V2 主链路已打通，聊天权限边界和 feed 可见性默认体验显著改善。
- 暴露问题：调度仍偏“触发型”，缺少任务生命周期与故障重试能力。

## 优化输入（给 P4）
- 把 scheduler 从内存任务升级到持久化任务模型。
- 增加任务状态机（PENDING/RUNNING/FAILED/SUCCEEDED）。
- 增加 `taskId`、`idempotencyKey`、`scheduleExpr` 等生命周期字段。
- 增加 simulate 触发后的执行状态回传能力。
