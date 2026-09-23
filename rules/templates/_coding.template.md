<!--
  rules 编码规范 —— 框架源模板（按类型，逐 Workbench 实例化）
  ============================================================
  本文件是 framework 源，**不复制到实例项目**。
  project-setup 对每个启用 Workbench，读取本模板 → 填充每个 <!-- TECH_SPECIFIC --> 区块 →
  生成 {target}/rules/{Workbench key}/coding.md。

  【N Workbench 原则 —— 本模板第一铁律】
  本模板**不预设任何固定 Workbench 集合**。每个 <!-- TECH_SPECIFIC --> 区块给出的是一组**Workbench 无关的问题/维度**，
  project-setup 在 Phase 2 采集本 Workbench 时按**该 Workbench 的 tech_stack / layer_pattern** 派生答案。
  区块内出现的 backend / data / frontend 仅为**示意答案形态**，不是枚举、不是查表项；
  遇到任何其他 Workbench（python / ml / mobile / infra / go …），一律按该 Workbench 自己的 tech_stack 派生。

  填充原则：用 config/采集值；Claude 知识补充；无法确定标 `TODO: 根据实际情况填写`，不留空白、不编造。
-->
---
name: {{Workbench_NAME}} Workbench 编码规范
description: {{Workbench_NAME}} Workbench 代码风格、命名、分层、禁止事项。SKILL 按需自主加载（涉及命名/分层/实现规范时）。
scope: {{Workbench_NAME}}
type: rules
---

# {{Workbench_NAME}} Workbench — 编码规范

> **加载方式**：本文件由各 SKILL **按需自主加载**（非强制）。规范可能较大，SKILL 应先看本段 description，
> 仅当本次任务涉及命名/分层/接口约定/实现习惯等覆盖维度时再加载相关章节。
> 通用语言规范（如 PEP8、Google Java Style、Airbnb React Style）不在此重复，以语言官方规范为准。

---

## 1. 分层 / 结构约定

<!-- TECH_SPECIFIC: 分层约定 - project-setup 按本 Workbench layer_pattern 派生 -->
[为本 Workbench 回答：本 Workbench 代码的分层/模块结构，每层职责与**禁止事项**。
 示例（仅示意，非枚举）：
  - backend：接口层(只校验+转换,禁业务逻辑) / 服务层(业务入口,事务在此) / 数据层(只 SQL,禁业务判断)
  - frontend：视图组件 / 容器组件 / Store / api 封装层，各层通信与边界
  - data：Source / Transform / Sink 各算子职责，禁止在 Sink 写业务转换]
<!-- /TECH_SPECIFIC -->

## 2. 命名约定

<!-- TECH_SPECIFIC: 命名约定 - project-setup 按本 Workbench tech_stack 派生 -->
[为本 Workbench 回答：类/文件/变量/常量的命名规则与示例。
 示例（仅示意，非枚举）：
  - backend：DO/ReqVO/RespVO/Service/ServiceImpl 命名模式
  - frontend：组件 PascalCase、事件 kebab-case、Props camelCase
  - data：Job 命名、宽表/中间表命名前缀]
<!-- /TECH_SPECIFIC -->

## 3. 禁止事项 / 红线

<!-- TECH_SPECIFIC: 编码红线 - project-setup 按本 Workbench tech_stack 派生 -->
[为本 Workbench 列出代码级 BLOCK/WARN 禁止项（区别于 architectural_constraints.md 的项目级约束、
 区别于 references 的 schema/设计红线；本处是**代码写法**红线）。
 示例（仅示意，非枚举）：
  - backend：禁 Controller 直接操作 DB、禁 N+1 查询、禁大事务
  - frontend：禁组件内直接 fetch（须走 api 封装）、禁硬编码接口 Workbench 名
  - data：宽表字段须有 NULL 处理、去重用 ROW_NUMBER() 而非 DISTINCT]
<!-- /TECH_SPECIFIC -->

## 4. 通用约定（注释 / 日志 / 错误处理）

<!-- TECH_SPECIFIC: 通用代码约定 - project-setup 按本 Workbench tech_stack 派生 -->
[为本 Workbench 回答：注释语言与时机、日志级别与脱敏、错误处理（业务异常 vs 技术异常）的本 Workbench 写法。
 这些维度各 Workbench 通用，但具体写法按本 Workbench 语言/框架派生。]
<!-- /TECH_SPECIFIC -->

---

## 变更记录

| 日期 | 变更内容 | 变更人 |
|------|---------|-------|
| （初始为空，由团队在代码审查中增量维护） | — | — |
