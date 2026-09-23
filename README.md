# Dev-Harness Framework

> 基于 Agent Harness 理论的企业级 AI 软件交付 SOP + Skill 框架。
> 把"从需求到上线"的完整工程流程拆成一组可触发、可门控、可续跑的 Skill，
> 让 AI 在每个环节都有明确的上下文边界、产出契约和人类确认点。

本仓库是**框架源（framework）**，不是某个具体项目。它通过 `project-setup` 一键**实例化**为
任意项目专属的交付工作台。同一套 Skill 与纪律，可服务后端、前端、大数据、移动、ML、基础设施……
任意 Workbench 组合，是面向全公司的通用底座。

---

## 1. 设计哲学：为什么是 "Harness"

Agent Harness 关注的不是"模型多聪明"，而是**围绕模型搭建的脚手架**——上下文怎么喂、记忆放哪里、
工具循环怎么设计、人类在哪里把关。本框架把这些理论落成五条贯穿所有 Skill 的不变量：


| 不变量                    | 含义                                                                                                                                           | 在 Skill 中的体现                                                                                 |
| ------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| **瘦主体 + 按需加载**     | SKILL.md 只承载**通用流程与纪律**；技术绑定知识（技术栈、产出骨架、红线）拆到 `references/{Workbench}.md`，运行时只加载当前 Workbench 的那一份 | 几乎所有核心 Skill 都是「瘦主体 + references」结构，避免无关 Workbench 信息污染上下文             |
| **文件权威 > 对话上下文** | 约束、配置、知识以**文件**为唯一事实源；AI 执行前读文件，而非靠用户在对话里重述                                                                | `project-config.yaml`（索引事实源）、`references/`（知识事实源）、`project-memory/`（约束事实源） |
| **确认即落盘**            | 长流程中已确认的结果**立即写盘**，不囤在上下文里等后面消费，从而可随时中断、fresh 重建                                                         | `project-setup` 的增量持久化模型；各 Skill 产出文档而非只在对话里回答                             |
| **人类门控**              | AI 产出默认是**草稿**；关键决策点 BLOCK/QUESTION，由人确认后才生效或推进                                                                       | `BLOCK / QUESTION / SUGGEST` 分级、草稿机制、Guardrails 表                                        |
| **可重入 / 断点续跑**     | 每个长 Skill 都能读文件判断"做到哪了"，只续做未完成部分                                                                                        | `project-setup` §0 断点续跑协议、`feature-developer` 的 `_progress.md`                           |

> 进一步的理论背景见 `_harness-study/`（参考资料，非权威源；**Skill 本身才是 single source of truth**）。

---

## 2. 核心概念：N-Workbench 模型

**Workbench（工作台）** 是本框架的组织单元——一个业务/技术域（如 `backend`、`frontend`、`data`、
`mobile`、`ml`、`infra`）。框架**不预设固定的 Workbench 集合**，项目有几个就声明几个。

每个 Workbench 的 `key` 在三处共用同一个值，把"人 — 配置 — 知识"串起来：

```
local_profile.yaml: role  ──┐
local_profile.yaml: code_roots 键名  ──┼──  同一个 key（如 backend）
project-config.yaml: Workbench 条目 key ──┘
        │
        └──→ Skill 运行时据此加载 skills/{skill}/references/{key}.md（本 Workbench 的技术绑定知识）
```

- **每位工程师**在本地建 `local_profile.yaml`（不提交 git），声明自己的 `role` 和有权限的 `code_roots`。
- **Skill 行为**：无 `local_profile.yaml` → BLOCK；`role: dev-lead` → 全域访问；其他 role → 仅本 Workbench。
- **加载策略因 Skill 而异**：多数按"当前 role"加载；`code-reviewer` 按**被审查 Workbench**加载；
  `module-explorer` 按**探索目标**推断 Workbench（target-first，role 仅作初值）。

---

## 3. 两层架构：框架 vs 实例

```
                    ┌─────────────────────────────────────┐
                    │   dev-harness framework（本仓库）      │
                    │  · 瘦主体 SKILL.md（通用流程）          │
                    │  · references/_reference.template.md  │
                    │    （Workbench 无关的"问题清单"模板）    │
                    │  · doc-templates / rules/templates    │
                    └──────────────┬──────────────────────┘
                                   │  project-setup
                                   │  （Phase 1 全局 → Phase 2 逐 Workbench 采集 → Phase 3 派生）
                                   ▼
                    ┌─────────────────────────────────────┐
                    │   {project}-doc-workspace（实例）      │
                    │  · 已填好 {Workbench}.md 的 Skills     │
                    │  · project-config.yaml（索引事实源）    │
                    │  · project-memory / rules/{Workbench} │
                    │  · CLAUDE.md / AGENTS.md / 目录骨架     │
                    └─────────────────────────────────────┘
```

模板里的 `<!-- TECH_SPECIFIC -->` 区块是一组**Workbench 无关的问题**（不是固定的 backend/frontend 枚举）。
`project-setup` 在 Phase 2 按每个 Workbench 的技术栈**派生答案**填槽，生成该 Workbench 专属的 reference。

---

## 4. Skill 全景：一条交付流水线

19 个 Skill 按"需求 → 方案 → 设计 → 实现 → 审查 → 测试"的交付流水线编排，外加横切支撑与初始化。

### 4.1 主流水线

```
  [PM]            prd-writer            需求描述 → PRD
                       │
  [TL/BA] ① requirement-kickoff         PRD → 检测 Workbench · 第一轮业务澄清 · 术语表 · 评估 HLD · 派活
                       │
  [TL]    ② solution-designer (可选)     HLD：组件选型 · 跨 Workbench 数据流 · 定方向（大型需求才跑，小 CR 可跳过）
                       │
  [各 WB] ③ impact-analyzer             各 Workbench 并行：这个需求对"我的"代码现状要改什么
                       │
  [Lead]  ④ cross-workbench-reviewer    Phase 1.5：跨 Workbench 对比 API 契约/数据模型/责任边界，标注 ⚠️
                       │
  [汇总]  ⑤ requirements-analyst        第二轮技术澄清 → 收敛为带 AC 的 requirements.md
                       │
  [各 WB] ⑥ architecture-advisor        各 Workbench LLD：Schema · 接口/管道 · 技术设计 · 任务拆分（自动生成 ADR 草稿）
                       │
  [各 WB] ⑦ feature-developer           Gather-Act-Verify 循环实现，两次失败上报人类，支持 _progress.md 续跑
                       │
  [各 WB] ⑧ code-walkthrough            讲师式分段带读，修复"AI 写码→人类心智模型偏低"的门控失效
                       │
  [各 WB] ⑨ code-reviewer              结构化审查（架构/逻辑/规范/安全/测试），分 BLOCK/QUESTION/SUGGEST
                       │
  [各 WB] ⑩ dev-self-test              白盒自测用例，每条附数据验证查询
                       │
  [各 WB] ⑪ test-data-script-generator  造数/清数脚本 + 占位符已替换的可执行自测副本
```

`qa-test-case`（QA，黑盒）不依赖实现文档，**需求确认后即可与开发并行**，独立产出 AT/E2E/NEG/PERM/REG/UAT 六维用例。

### 4.2 Skill 速查表


| #  | Skill                        | 触发场景                        | 执行角色 | 关键纪律                                      |
| -- | ---------------------------- | ------------------------------- | -------- | --------------------------------------------- |
| — | `project-setup`              | 把框架实例化为项目工作台        | 搭建者   | 增量落盘 · 断点续跑 · 三阶段                |
| 0  | `prd-writer`                 | 需求描述 → PRD                 | PM       | 只写业务/用户故事，不碰技术实现               |
| 1  | `requirement-kickoff`        | 收到 PRD，流水线第一棒          | TL / BA  | 第一轮业务澄清 · 派活 · 评估 HLD 必要性     |
| 2  | `solution-designer`          | 大型需求 HLD，定方向            | TL       | 默认不读代码 · 选型假设显式标注待下游验证    |
| 3  | `impact-analyzer`            | 需求对本 Workbench 要改什么     | 各 WB    | 产出可回流第二轮澄清的"待澄清问题"            |
| 4  | `cross-workbench-reviewer`   | 各 WB 影响分析后的一致性 review | dev-lead | 只标注 ⚠️ 不一致，供裁决                    |
| 5  | `requirements-analyst`       | 汇总 → requirements.md         | 汇总者   | 第二轮技术澄清 · 永不推断未明说内容          |
| 6  | `architecture-advisor`       | LLD：Schema/API/技术设计/任务   | 各 WB    | 每阶段查 ADR 合规 · 收尾自动生成 ADR 草稿    |
| 7  | `feature-developer`          | 按 design+tasks 写代码          | 各 WB    | Gather-Act-Verify · 两次失败上报 · 先测后码 |
| 8  | `code-walkthrough`           | 带读本次改动                    | 各 WB    | 分段讲解 · 每段确认 · 纯对话不产文档        |
| 9  | `code-reviewer`              | PR / diff / 片段审查            | 各 WB    | 按**被审 Workbench** 加载参考                 |
| 10 | `dev-self-test`              | 白盒自测用例                    | 各 WB    | 各 WB 专属测试范式（templates/）              |
| 11 | `test-data-script-generator` | 造数/清数                       | 各 WB    | 纯消费 Workbench（如前端）BLOCK 引导找数据方  |
| ∥ | `qa-test-case`               | 黑盒 QA 用例（可并行）          | QA       | 不依赖实现文档                                |
| ✦ | `module-explorer`            | 通用模块探索（无票据）          | 任意     | target-first 推断 Workbench                   |
| ✦ | `dev-onboarding`             | 新人接手需求确认清单            | 新开发者 | 开发前系统性确认上下文                        |
| ✦ | `bug-fixer`                  | 测试报 Bug 后修复               | 各 WB    | 纯静态分析 · 最窄修复 · 回归验证            |
| ✦ | `requirement-change-router`  | BA 改需求/失误                  | TL / BA  | 只更`_progress.md` 状态 · 产重跑清单         |
| ✦ | `memory-curator`             | 知识库审计（手动两周一次）      | TL       | 禁止自动运行 · 全为草稿                      |

`✦` = 横切支撑 Skill，按需触发，不在主流水线固定位置。

---

## 5. 仓库结构

```
dev-harness-framework/
├── README.md                      # 本文件
├── skills/                        # 19 个 Skill（框架源，瘦主体）
│   └── {skill}/
│       ├── SKILL.md               # 通用流程与纪律
│       ├── references/
│       │   └── _reference.template.md   # Workbench 无关的"问题清单"模板（不复制到实例）
│       └── templates/             # dev-self-test 专用：_dev_test_template.base.md
├── doc-templates/                 # 文档产物模板（进度/Schema/技术设计/任务/changelog…）
├── rules/templates/               # 工程规范模板（_coding / _api / _database）
├── project-memory/                # 知识库模板（MEMORY/术语/约束/ADR 索引）
├── global-info/                   # 全局信息目录规范（只随实例复制 README）
├── project-config.yaml.template   # 实例的索引事实源模板
├── local_profile.yaml.example     # 个人角色配置模板（实例中不提交 git）
└── _harness-study/                # 理论参考资料（非权威源）
```

实例化后，`{project}-doc-workspace/` 额外生成：`project-config.yaml`、`CLAUDE.md`/`AGENTS.md`、
`rules/{Workbench}/`、各 Workbench 的 `{doc_root}/01_function/`，以及填好 `references/{Workbench}.md` 的全套 Skill。

---

## 6. 快速开始

### 6.1 搭建一个新项目工作台

```text
在本框架仓库中触发：  /project-setup
```

按三阶段交互完成（每阶段**确认即落盘**，可随时中断、分会话续跑）：

1. **Phase 1 — 全局信息**：项目名、票据格式、目标路径、Workbench 清单 → 落盘 `project-config.yaml` 骨架。
2. **Phase 2 — 逐 Workbench 采集**：每个 Workbench 选 `A 逐条问答` 或 `B 文档派生`（喂 AGENTS.md/CLAUDE.md），
   采集技术栈/分层/命令/约定/业务约束 → 填满各 Skill 的 `references/{Workbench}.md` 与 `rules/{Workbench}/`。
3. **Phase 3 — 纯派生**：从已落盘文件生成 CLAUDE.md、本地化全部 SKILL.md、建目录骨架（无需用户输入）。

完成后：

```bash
cd {target-path}
git init && git add . && git commit -m "init: project setup from dev-harness framework"
# 推送到团队共享仓库
```

### 6.2 每位工程师接入

```bash
cp local_profile.yaml.example local_profile.yaml   # 填 role + 自己有权限的 code_roots（不提交 git）
```

### 6.3 跑一个需求

按 §4.1 流水线顺次触发 Skill 即可。每个 Skill 会自行读取 `local_profile.yaml`、`project-config.yaml`、
`project-memory/` 与本 Workbench 的 `references/`，无需在对话里重述上下文。

---

## 7. 给框架贡献者

- **改 Skill 通用流程** → 改 `skills/{skill}/SKILL.md`（瘦主体，不要塞 Workbench 专属知识）。
- **改 Workbench 技术维度** → 改 `skills/{skill}/references/_reference.template.md` 的 `<!-- TECH_SPECIFIC -->` 问题清单，
  保持"Workbench 无关、不预设固定 Workbench 集合"。
- **SKILL.md 是唯一权威**：description 决定触发，正文决定行为。改完确保触发短语、Guardrails、人类门控自洽。
- 不要在 SKILL.md 里硬编码代码路径或具体 Workbench 名——一切从 `local_profile.yaml` / `project-config.yaml` 读取。

---

## 8. 主数据管理平台（MDM-0001）交付说明

本仓库在框架源之外，同时包含一个**按本框架 SOP 完整交付的示例项目**（框架源与实例同仓），用于演示 Spec Coding 从文档到代码的全流程落地：

| 交付物 | 路径 | 说明 |
|--------|------|------|
| 需求文档空间 | `01-requirements/MDM-0001/` | prd / requirements_plan / requirements（REQ-BKD01~10、REQ-FED01~07）/ _progress（票级进度日志，状态：进行中） |
| 后端工作台文档 | `02-backend/01_function/MDM-0001/` | sql（12 表 DDL）/ api（45+ 接口契约）/ design / task / change-log |
| 前端工作台文档 | `03-frontend/01_function/MDM-0001/` | design / task / change-log |
| 后端代码 | `mdm-backend/` | Java 17 + Spring Boot 3.2.5 + SQLite + POI；9 Controller / 10 Service / 12 表 / 4 测试类 34 用例 |
| 前端代码 | `mdm-frontend/` | Vue 3 + Vite 5 + Element Plus；7 页面 / 10 组件 / 8 API 模块 / 7 路由 |
| 过程记录 | `01-requirements/MDM-0001/process-record.md` | CBCC AI SOP2.0 全流程记录：关键 Prompt / 决策 / 迭代，可直接导入看板查看 |
| Agent 上下文 | `AGENTS.md` / `CLAUDE.md`（根）+ `mdm-backend/AGENTS.md` + `mdm-frontend/AGENTS.md` | 工作区与各代码仓库的 Agent 可信上下文（Skill 代码探索前置 + 看板识别） |

### 本地运行

```bash
# 后端（需 JDK 17 + Maven 3.9）
cd mdm-backend
mvn spring-boot:run            # http://localhost:8080，首次启动自动建库并初始化种子数据

# 前端（需 Node 18+）
cd mdm-frontend
npm install
npm run dev                    # http://localhost:5173（/api 代理至 8080）
```

### 演示方式

顶栏切换 4 个演示角色（数据管理员 / 录入员 / 审核员 / 系统管理员），前端以 `X-User-*` 请求头与后端 `@RequirePermission` 联动权限。核心链路：
**模型设计器定义元数据 → 上线锁定 → 数据维护动态表单（查重 → 质量校验 → 提交）→ 推送中心预检/推送 → 协同确认**。

> 交付验证状态：测试阶段已关闭（测试完成 · 最终阶段）——自测用例（4 类 34 例）与静态一致性核对清单（各 13 项）就绪；本机实跑（`mvn test` / `npm run build`）按用户决议跳过，便携工具链（JDK17 / Maven / Node）与补跑脚本 `_run_backend_test.ps1` / `_run_frontend_build.ps1` 已随仓库提供。
