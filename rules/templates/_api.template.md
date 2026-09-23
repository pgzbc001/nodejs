<!--
  rules API/接口规范 —— 框架源模板（按类型，逐 Workbench 实例化）
  ============================================================
  framework 源，**不复制到实例项目**。project-setup 仅对**有对外/消费接口职责的 Workbench**生成
  {target}/rules/{Workbench key}/api.md（无接口职责的 Workbench 不生成本文件）。

  【N Workbench 原则】区块给的是 Workbench 无关问题；backend(产出契约)/frontend(消费契约) 仅示意答案形态，非枚举。
  其他 Workbench 按其接口角色（提供方/消费方/双向）派生。无法确定标 TODO，不编造。
-->
---
name: {{Workbench_NAME}} Workbench 接口规范
description: {{Workbench_NAME}} Workbench 接口契约约定（命名/参数/响应/错误码/版本）。SKILL 按需自主加载（设计或消费接口时）。
scope: {{Workbench_NAME}}
type: rules
---

# {{Workbench_NAME}} Workbench — 接口/API 规范

> **加载方式**：各 SKILL **按需自主加载**（非强制）。仅当本次任务涉及接口设计/消费/契约比对时加载。

---

## 1. 接口命名与风格

<!-- TECH_SPECIFIC: 接口风格 - project-setup 按本 Workbench API 风格派生 -->
[为本 Workbench 回答：接口风格（REST/GraphQL/gRPC/事件）、路径或方法命名规则、版本与网关约定。
 示例（仅示意，非枚举）：
  - backend（提供方）：RESTful 路径规则、网关前缀、版本策略
  - frontend（消费方）：调用封装约定、请求时机、鉴权头]
<!-- /TECH_SPECIFIC -->

## 2. 请求 / 响应契约

<!-- TECH_SPECIFIC: 请求响应契约 - project-setup 按本 Workbench 派生 -->
[为本 Workbench 回答：参数校验约定、统一响应结构、分页约定、空值处理。]
<!-- /TECH_SPECIFIC -->

## 3. 错误码 / 异常约定

<!-- TECH_SPECIFIC: 错误码约定 - project-setup 按本 Workbench 派生 -->
[为本 Workbench 回答：错误码分段规则、错误响应格式、消费方按码的处理策略。]
<!-- /TECH_SPECIFIC -->

---

## 变更记录

| 日期 | 变更内容 | 变更人 |
|------|---------|-------|
| （初始为空，由团队维护） | — | — |
