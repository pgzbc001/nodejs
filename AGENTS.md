# 主数据管理平台 — 项目全局上下文

> 本文件（AGENTS.md 与 CLAUDE.md 内容一致，两份均写出）由 Dev Harness Framework 的
> project-setup 约定生成，供所有 AI Agent 作为工作区级可信上下文加载。

---

## 项目概览

**项目**：主数据管理平台（需求编号 MDM-0001）
**架构模式**：N-Workbench（backend + frontend）+ 元数据驱动单体应用；框架源与交付实例同仓

---

## 仓库结构

```text
dev-harness-framework/            # 工作区根（Dev Harness 框架源 + MDM 交付实例同仓）
├── AGENTS.md / CLAUDE.md         # 本文件：内容一致，两份均写出
├── project-config.yaml           # 实例索引事实源（Workbench / 目录配置）
├── local_profile.yaml            # 个人角色配置（不提交 git）
├── project-memory/               # 知识库（MEMORY / 术语 / 约束 / ADR 索引）
├── skills/                       # Dev Harness Framework 全套 Skill
├── doc-templates/                # 文档产物模板
├── rules/templates/              # 工程规范模板
├── global-info/                  # 项目级静态事实
├── 01-requirements/              # 需求文档（MDM-0001：prd / requirements / _progress / process-record）
├── 02-backend/01_function/MDM-0001/    # 后端工作台文档（sql / api / design / task / change-log）
├── 03-frontend/01_function/MDM-0001/   # 前端工作台文档（design / task / change-log）
├── mdm-backend/                  # 后端代码仓库（Java 17 + Spring Boot 3 + SQLite）
└── mdm-frontend/                 # 前端代码仓库（Vue 3 + Vite 5 + Element Plus）
```

---

## 技术栈

（权威记录见 `project-config.yaml`）

- **后端**：Java 17 + Spring Boot 3.2.5 + Spring Data JPA + SQLite（hibernate-community-dialects）+ Apache POI
- **前端**：Vue 3.4 + Vite 5 + Element Plus 2.7 + Pinia + Vue Router + Axios
- **契约**：REST `/api/v1`；响应 `{code,message,data}`；业务错误 HTTP 200 + 错误码（15 项）
- **数据库**：SQLite 单库；`schema.sql` 维护 12 张 `mdm_*` 表，`data.sql` 提供种子数据

---

## 角色与访问控制

### 工程师本地配置

`local_profile.yaml`（每位工程师的本地角色配置，声明 role 与代码路径）：

```yaml
role: dev-lead   # 可选值：backend / frontend / dev-lead / ba / qa
name: 姓名

code_roots:
  backend: <mdm-backend 仓库绝对路径>
  frontend: <mdm-frontend 仓库绝对路径>
```

**Skill 行为规则**：
- `local_profile.yaml` **不存在或 role 未填写** → 所有 Skill **BLOCK**，要求先创建并声明 role
- `role: dev-lead` → 允许全域访问（必须显式声明）
- 其他 role → 只能访问本人域的代码目录和文档目录

### 代码仓库导航规则

Skill 访问代码时，从 `local_profile.yaml` 读取 `code_roots`，禁止硬编码路径。

获得路径后，**进入任何代码阅读 / 探索 / 分析步骤之前，必须先读取该路径下的 agent 文档作为可信上下文**，按优先级：

1. **优先级 1 — `AGENTS.md`**：该仓库根目录、或离目标模块最近的 `AGENTS.md`。
2. **优先级 2 — `CLAUDE.md`**：同路径下的 `CLAUDE.md`，仅在无 `AGENTS.md` 时回退读取。
3. **两者皆无 → BLOCK**：禁止后续一切代码探索步骤。

| 仓库 | Agent 文档 | 状态 |
|------|-----------|------|
| 工作区根 | `AGENTS.md` / `CLAUDE.md` | ✅ 已提供（内容一致） |
| mdm-backend | `mdm-backend/AGENTS.md` | ✅ 已提供 |
| mdm-frontend | `mdm-frontend/AGENTS.md` | ✅ 已提供 |

---

## 需求状态入口

- **票级进度日志**：`01-requirements/MDM-0001/_progress.md` — 看板读取「需求状态 / 当前阶段 / 当前 Skill」的唯一事实源（状态图例：🔄 进行中 / ⏸️ 等待人类确认 / ✅ 已完成 / ❌ 阻断）
- **过程记录**：`01-requirements/MDM-0001/process-record.md` — 关键 Prompt / 决策 / 迭代记录，可直接导入看板查看

---

## Memory 系统说明

`project-memory/` 是跨会话的共享知识库。

- **始终加载**：`MEMORY.md`（索引）、`project_glossary.md`（术语）、`architectural_constraints.md`（约束）
- **按需加载**：其他文件，在需要时由 Skill 明确加载
- **草稿机制**：所有 AI 写入 Memory 的内容均为草稿状态，需人工确认后生效
