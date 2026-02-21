# TaroTalk 阶段执行总索引（P3.0 -> P10）

## 执行规则
- 阶段流程：实现 -> 门禁测试 -> 阶段报告 -> 阶段复盘优化 -> 下一阶段。
- 报告目录：`docs/project_documents/phase_reports/`。
- 本次执行基线：`feature/init`（2026-02-21）。

## 阶段追踪
| 阶段 | 状态 | 报告 | 复盘 | 关键产出 | 更新时间 |
|---|---|---|---|---|---|
| P3.0 | completed | p3_0_baseline.md | - | 基线清理、忽略规则、报告目录骨架 | 2026-02-21 |
| P3.5 | completed | p3_5_report.md | p3_5_optimization.md | Feed V2/可见性修复、Chat 参与者校验、trace 贯通 | 2026-02-21 |
| P4 | completed | p4_report.md | p4_optimization.md | Scheduler 持久化、任务状态机、simulate 任务化 | 2026-02-21 |
| P5 | completed | p5_report.md | p5_optimization.md | 事件幂等键、去重写入、回放元数据扩展 | 2026-02-21 |
| P6 | completed | p6_report.md | p6_optimization.md | 网关 JWT 过滤器、上下文透传、路由收敛 | 2026-02-21 |
| P7 | completed | p7_report.md | p7_optimization.md | Kafka/Temporal compose、workflow 模式接入 | 2026-02-21 |
| P8 | completed | p8_report.md | p8_optimization.md | 聚合回放 API、前端 trace 回放页 | 2026-02-21 |
| P9 | completed | p9_report.md | p9_optimization.md | 超时重试参数化、索引增强、调度退避、编排工具韧性 | 2026-02-21 |
| P10 | completed | p10_release_report.md | p10_optimization.md | 发布就绪基线、上线/回滚 runbook | 2026-02-21 |
| P11 | completed | p11_report.md | p11_optimization.md | 记忆编译器（world memories）、编排层优先记忆读取与回退 | 2026-02-21 |
| P12 | completed | p12_report.md | p12_optimization.md | 因果剧情图（world causal edges）与 replay 聚合增强 | 2026-02-21 |
| P13 | completed | p13_report.md | p13_optimization.md | what-if 分叉模拟 API 与 world 分支场景记录 | 2026-02-21 |
| P14 | completed | p14_report.md | p14_optimization.md | 角色漂移守卫（检测、重生成、降权）与 trace 决策记录 | 2026-02-21 |
| P15 | completed | p15_report.md | p15_optimization.md | Feed 排序 bandit 影子模式、决策日志与 reward 回灌 | 2026-02-21 |
| P16 | completed | p16_report.md | p16_optimization.md | explain 聚合 API、Trace 多面板调试页、内部调试开关 | 2026-02-21 |

## 交付附件
- `p10_release_runbook.md`
- `tarotalk_deep_architecture_analysis.md`
- `v2_m3_implementation_report.md`
