<!--
  rules 数据库/存储规范 —— 框架源模板（按类型，逐 Workbench 实例化）
  ============================================================
  framework 源，**不复制到实例项目**。project-setup 仅对**有数据存储职责的 Workbench**生成
  {target}/rules/{Workbench key}/database.md（无存储职责的 Workbench 不生成本文件）。

  【N Workbench 原则】区块给的是 Workbench 无关问题；backend(OLTP)/data(数仓) 仅示意答案形态，非枚举。
  其他 Workbench 按其存储介质（关系库/数仓/KV/文档/列存…）派生。无法确定标 TODO，不编造。
-->
---
name: {{Workbench_NAME}} Workbench 数据库规范
description: {{Workbench_NAME}} Workbench 库表设计约定（命名/类型/索引/分区）。SKILL 按需自主加载（设计 Schema 时）。
scope: {{Workbench_NAME}}
type: rules
---

# {{Workbench_NAME}} Workbench — 数据库/存储规范

> **加载方式**：各 SKILL **按需自主加载**（非强制）。仅当本次任务涉及库表/Schema 设计时加载。
> 与 references/{Workbench}.md 的「Workbench schema 红线」配合：本文件是团队**风格规范**，红线是 SOP **硬约束**。

---

## 1. 命名规范

<!-- TECH_SPECIFIC: 库表命名 - project-setup 按本 Workbench 存储介质派生 -->
[为本 Workbench 回答：表/字段/索引命名规则、前缀约定、大小写。
 示例（仅示意，非枚举）：
  - backend：t_ 前缀、snake_case、索引 idx_/uk_ 命名
  - data：数仓分层命名（ods/dwd/dws/ads）、宽表命名]
<!-- /TECH_SPECIFIC -->

## 2. 字段类型与必备字段

<!-- TECH_SPECIFIC: 字段类型约定 - project-setup 按本 Workbench 派生 -->
[为本 Workbench 回答：类型强约束（金额精度等）、必备/审计字段（或"本 Workbench 无"）、默认值约定。]
<!-- /TECH_SPECIFIC -->

## 3. 索引 / 分区 / 存储设计

<!-- TECH_SPECIFIC: 存储设计 - project-setup 按本 Workbench 派生 -->
[为本 Workbench 回答：索引设计原则、分区/分桶策略、存储格式、更新模式。]
<!-- /TECH_SPECIFIC -->

---

## 变更记录

| 日期 | 变更内容 | 变更人 |
|------|---------|-------|
| （初始为空，由团队维护） | — | — |
