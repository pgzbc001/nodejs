---
name: ADR 目录
description: 架构决策记录目录。按需加载；architecture-advisor、cross-workbench-reviewer 执行时读取。
type: reference
---

# ADR 目录

> 架构决策记录（Architecture Decision Records）。
> 每条重要的技术/架构选型都应有对应的 ADR，供后续开发者理解"为什么这样设计"。

---

## ADR 状态说明

| 状态 | 含义 |
|------|------|
| PROPOSED | 已提出，待评审 |
| ACCEPTED | 已接受，当前有效 |
| DEPRECATED | 已废弃（附说明被哪条 ADR 替代） |
| SUPERSEDED | 被新决策替代 |

---

## ADR 列表

<!-- 由 Dev Lead 在 architecture-advisor 执行中维护 -->
<!-- 示例格式：

| ADR | 标题 | 状态 | 日期 | 摘要 |
|-----|------|:----:|------|------|
| [ADR-001](adrs/ADR-001.md) | 微服务间通信方式选择 | ACCEPTED | 2024-01-15 | 选择 REST over gRPC，原因：团队熟悉度和调试便利性 |
| [ADR-002](adrs/ADR-002.md) | 数据缓存策略 | ACCEPTED | 2024-02-01 | Redis 作为分布式缓存，TTL 策略按业务 Workbench 分别配置 |

-->

| ADR | 标题 | 状态 | 日期 | 摘要 |
|-----|------|:----:|------|------|
| （初始为空，由 Dev Lead 填充） | — | — | — | — |

---

## ADR 模板

新建 ADR 时，在 `adrs/` 目录下创建 `ADR-{NNN}.md`，使用以下模板：

```markdown
# ADR-{NNN}：{标题}

**状态**：PROPOSED / ACCEPTED / DEPRECATED / SUPERSEDED
**日期**：{YYYY-MM-DD}
**决策者**：{姓名}

## 背景

{为什么需要做这个决策？面临的约束是什么？}

## 决策

{选择了什么方案？}

## 后果

**正面影响**：
- {优点1}

**负面影响 / 取舍**：
- {缺点或风险1}

## 备选方案（已评估但未采用）

| 方案 | 放弃理由 |
|------|---------|
| {方案A} | {理由} |
```
