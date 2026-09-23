---
name: dev-onboarding
description: |
  新开发者接入向导。帮助新工程师接手需求时，系统性确认所有必要的上下文，
  确保在开始开发前掌握：需求文档、设计文档、代码现状、架构约束、团队约定。

  当用户表达"我是新来的"、"接手这个需求"、"新开发者接入"、"dev onboarding"、
  "我刚接到这个任务"时触发此 Skill。
---

# Dev Onboarding

帮助新工程师在开始开发前，系统性确认所有必要的上下文。

---

## Memory 契约

### 始终加载（每次执行前必读）
- `local_profile.yaml` — 获取 role 和 code_roots
- `project-memory/project_glossary.md` — 项目术语表
- `project-memory/architectural_constraints.md` — 架构约束
- `rules/{role}/`（编码/接口/库规范，按 Workbench 存在）— 团队工程规范目录；据此提示新人需阅读哪些规范文件。**按需自主加载**：清单阶段通常只需列出存在哪些规范文件，规范较大时不必全文载入。

---

## 输入

| 输入 | 必填 | 说明 |
|------|------|------|
| **票据 ID** | **是** | `{{TICKET_PREFIX}}-xxxx`，需要接手的需求 |

---

## 输出

在对话中输出接入确认清单（不写文件）。

---

## Guardrails

| 规则 | 触发条件 | 执行动作 |
|------|---------|---------|
| **local_profile 缺失** | `local_profile.yaml` 不存在 | BLOCK：必须先创建并声明 role、name、code_roots |
| **code_roots 未配置** | `code_roots.{role}` 为空或目录不存在 | BLOCK：提示在 local_profile.yaml 中配置本 Workbench 代码路径 |

---

## 执行流程

### Step 1：环境确认

检查并输出：
- local_profile.yaml 配置是否完整（role、name、code_roots）
- code_roots 目录是否存在
- 本 Workbench 代码仓库的 AGENTS.md 是否可读

---

### Step 2：需求上下文确认

读取 `{{REQUIREMENTS_DIR}}/{ticket-id}/` 下的文档，输出确认清单：

| 文档 | 路径 | 状态 |
|------|------|------|
| _progress.md | `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md` | ✅ 已就绪 / ❌ 缺失 |
| PRD | `{{REQUIREMENTS_DIR}}/{ticket-id}/prd.md` | ✅ / ❌ |
| requirements.md（本 Workbench） | `{Workbench_root}/01_requirements/requirements.md` | ✅ / ❌ |
| solution-design.md（若有） | `{{REQUIREMENTS_DIR}}/{ticket-id}/solution-design.md` | ✅ / 无（正常） |

---

### Step 3：设计文档确认

读取本 Workbench 设计文档：

| 文档 | 路径 | 状态 |
|------|------|------|
| impact_analysis.md | `{Workbench_root}/00_impact_analysis.md` | ✅ / ❌ |
| Schema 文档 | `{Workbench_root}/02_schema/` | ✅ / ❌ |
| 接口/管道设计 | `{Workbench_root}/03_api/` | ✅ / ❌ |
| design.md | `{Workbench_root}/04_design/design.md` | ✅ / ❌ |
| tasks.md | `{Workbench_root}/05_task/tasks.md` | ✅ / ❌ |

---

### Step 4：代码现状确认

读取 `{code_roots.{role}}/AGENTS.md`（**优先级 1；缺失则回退读同路径 `CLAUDE.md`，优先级 2；两者皆无 → BLOCK，提示用户先在该仓库补充 agent 文档后重跑**），输出：
- 服务结构和模块划分摘要
- 本需求涉及的模块（来自 impact_analysis.md）
- 关键入口文件路径

---

### Step 5：架构约束摘要

从 `architectural_constraints.md` 提取与本 Workbench 最相关的约束条目，输出简明摘要。

---

### Step 6：接入确认报告

输出完整确认报告，包含：
- ✅ 已就绪的文档/环境
- ❌ 缺失的文档（及建议的解决方式：由谁运行哪个 Skill 生成）
- ⚠️ 需要特别注意的约束和规范
- **建议的开发起点**（下一步应该运行哪个 Skill）

---

## 关键原则

1. **确认而非推断**：只报告文件是否存在和可读，不猜测内容
2. **缺失引导**：缺少文档时，告知应由谁运行哪个 Skill 来生成，而非自行生成
3. **约束前置**：在开始开发前确保工程师了解关键约束，避免"踩坑后返工"
