---
title: 票级进度日志模板
usage: 由各 Skill 在人类门控处写入；Skill 执行前读取以判断是否从断点续跑
---

# {TICKET-ID} 进度日志

> 由 Skill 自动维护，勿手动修改核心字段。如需备注可在"备注"列追加。

---

## 当前状态

| 字段 | 内容 |
|------|------|
| **当前 Skill** | — |
| **当前阶段** | — |
| **状态** | — |
| **最后更新** | {YYYY-MM-DD} |

**状态图例**：`🔄 进行中` / `⏸️ 等待人类确认` / `✅ 已完成` / `❌ 阻断`

---

## Workbench 分析分配（requirement-kickoff 创建）

> 仅跨 Workbench 需求填写。requirements-analyst（汇总）的阻断条件：所有 Workbench 状态必须为 ✅。
> 「待澄清问题数」由各 Workbench impact-analyzer 完成时登记，供 requirements-analyst 估算第二轮技术 Q&A 工作量。

| Workbench | 负责人 | 状态 | 产出文件 | 待澄清问题数 | 完成时间 |
|---|-------|------|---------|:-----------:|---------|
| backend | — | — | — | — | — |
| data | — | — | — | — | — |
| frontend | — | — | — | — | — |

**阻断条件**：
- `requirements-analyst（汇总）`：所有参与 Workbench 状态必须为 ✅，否则 BLOCK

---

## 流程进度

> Skill 执行时按需追加行；未涉及的阶段可删除。
> **路径说明**：大型新需求：需求启动 → HLD → impact-analyzer → 跨 Workbench review → 需求汇总；存量 CR：需求启动 → impact-analyzer → 需求汇总（删除 HLD 行并在该行备注"已评估，跳过"）。

| 阶段 | Skill | 执行者 | 状态 | 完成时间 | 产出文件路径 |
|------|-------|-------|------|---------|------------|
| 需求启动（Workbench 检测+业务澄清+派活） | requirement-kickoff | Dev Lead/BA | — | — | — |
| 解决方案设计 HLD（大型新需求） | solution-designer | Dev Lead | — | — | {{REQUIREMENTS_DIR}}/{TICKET-ID}/solution-design.md |
| Workbench 影响分析（各 Workbench 并行） | impact-analyzer | 各 Workbench 工程师 | — | — | — |
| 跨 Workbench 一致性 review（仅跨 Workbench 需求） | cross-workbench-reviewer | Dev Lead | — | — | {{REQUIREMENTS_DIR}}/{TICKET-ID}/cross-Workbench-review.md |
| 需求汇总（第二轮技术 Q&A + requirements.md） | requirements-analyst | Dev Lead/BA | — | — | — |
| 架构设计（Schema+接口） | architecture-advisor | 各 Workbench 工程师 | — | — | — |
| 技术设计 | architecture-advisor | 各 Workbench 工程师 | — | — | — |
| 任务拆分 | architecture-advisor | 各 Workbench 工程师 | — | — | — |
| 代码实现 | feature-developer | 各 Workbench 工程师 | — | — | — |
| 代码审查 | code-reviewer | 各 Workbench 工程师 | — | — | {Workbench_root}/10_review/code_review.md |
| 开发自测用例 | dev-self-test | 各 Workbench 工程师 | — | — | — |
| 造数脚本 | test-data-script-generator | 各 Workbench 工程师/QA | — | — | — |

---

## 人类门控状态

| 门控点 | Skill | 状态 | 等待项 | 完成时间 |
|-------|-------|------|-------|---------|
| 所有 Workbench 影响分析完成 | impact-analyzer | — | 各 Workbench 工程师 提交 ✅ | — |
| 跨 Workbench 一致性确认（仅跨 Workbench 需求） | cross-workbench-reviewer | — | Dev Lead 完成跨 Workbench Review | — |
| 第一轮业务 Q&A 补充完成 | requirement-kickoff | — | BA 填写 Answers | — |
| 业务澄清双签 | requirement-kickoff | — | BA + 技术双签 | — |
| 第二轮技术 Q&A 裁决（质量关卡 1） | requirements-analyst | — | BA + DL 裁决（范围变更项交人处理） | — |
| requirements.md 确认 | requirements-analyst | — | 开发确认 | — |
| HLD 整体审批（大型新需求） | solution-designer | — | Dev Lead 审批 solution-design.md | — |
| Schema + 接口确认 | architecture-advisor | — | 开发确认 | — |
| 质量关卡 2（架构评审） | architecture-advisor | — | 开发确认 | — |
| 质量关卡 3（设计评审） | architecture-advisor | — | 开发确认 | — |
| tasks.md 确认 | architecture-advisor | — | 开发确认 | — |
| 代码审查 BLOCKING 处理 | code-reviewer | — | 开发修复 | — |
| 自测用例确认 | dev-self-test | — | 开发确认 | — |
| 造数脚本确认 | test-data-script-generator | — | 开发确认 | — |

---

## 贡献记录

| 时间 | 操作者（角色） | Skill | 操作 |
|------|-------------|-------|------|
| {YYYY-MM-DD} | — | — | — |

---

## 关键决策记录

| 时间 | Skill | 决策内容 |
|------|-------|---------|
| {YYYY-MM-DD} | — | — |

---

## 需求变更记录

> 由 requirement-change-router Skill 维护。初始版本 v1.0；每次变更后版本递增。
> 各 Skill 重跑完成后，"流程进度"表中的 ⚠️ 状态由对应 Skill 自行恢复为 ✅。

| 版本 | 日期 | 类型 | 变更描述摘要 | 影响 Workbench |
|-----|------|------|------------|-------|
| v1.0 | {创建日期} | — | 初始版本 | — |

---

## 跨 Workbench 信息

<!-- TECH_SPECIFIC: 跨 Workbench 信息表 - project-setup 按 project-config.yaml 的 Workbench 列表逐 Workbench 生成一行 -->
| Workbench | 负责人 | 输出目录 |
|----|-------|---------|
| {Workbench key} | — | {该 Workbench doc_root}/{TICKET-ID}/ |
<!-- 此区块由 project-setup 为 Workbench 列表中每个声明的 Workbench 生成一行；单个 Workbench 项目一行，N Workbench 项目 N 行 -->
<!-- /TECH_SPECIFIC -->
