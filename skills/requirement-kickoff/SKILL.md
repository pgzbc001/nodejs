---
name: requirement-kickoff
description: |-
  需求启动专家（流水线第一棒）。收到 PRD 后，检测涉及 Workbench、提取业务实体、做**第一轮业务澄清**、
  写入术语表、评估 HLD 必要性、建立 Workbench 分配表并派活各 Workbench。
  当用户表达"收到 PRD 开始分析"、"启动需求"、"读一下 PRD"、"帮我做需求分析"、"需求派活"、
  "PRD 转需求"、"需求启动"时触发此 Skill。
  不要在以下场景触发（那是下游 requirements-analyst 的职责，发生在各 Workbench impact-analyzer 完成之后）：
  第二轮技术澄清、消费各 Workbench 影响分析、汇总生成 requirements.md——这些都不属于需求启动阶段。
---
# Requirement Kickoff

从 PRD 到「业务澄清完成 + 派活各 Workbench」的需求启动流程。它是整条交付流水线的第一棒。

---

## 在流水线中的位置

```
PRD → 【requirement-kickoff】 → (solution-designer HLD) → 各 Workbench impact-analyzer
     → (cross-workbench-reviewer) → requirements-analyst（汇总成 requirements.md）→ architecture-advisor → …
```

- **本 Skill（kickoff）**：检测 Workbench、提取实体、**第一轮业务澄清**、建分配表、派活。产出 `requirements_plan.md`（含第一轮 Q&A）+ `_progress.md` Workbench 分配。
- **下游 requirements-analyst**：各 Workbench 影响分析 + 跨 Workbench review 完成后，消费影响分析、做**第二轮技术澄清**、生成 `requirements.md`。

> 两轮澄清分工：**业务歧义（PRD 本身不清晰）在本 Skill 派活前消解**；**技术歧义（看了代码才暴露）在 requirements-analyst 汇总前消解**。

---

## Memory 契约

### 始终加载（每次执行前必读）

- `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md` — 票级进度日志（若存在则必读，判断是否从断点续跑）
- `project-memory/project_glossary.md` — 项目术语表，所有输出必须使用其中的标准术语
- `project-memory/architectural_constraints.md` — 架构约束，影响需求转化策略

### 按需加载

- `project-memory/adr_index.md` — 涉及架构方向判断时检查是否有相关 ADR
- `skills/requirement-kickoff/references/*.md` — detect-Workbench 子环节读取**全部**启用 Workbench 的识别信号文件（project-setup 逐 Workbench 生成），汇成识别信号表

### 执行后写入（草稿，需人工确认后生效）

- `project-memory/project_glossary.md` — 业务术语逐条人类确认后追加（见 Step 5）
- `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md` — 每次人类门控前更新（阶段状态 + 产出路径 + Workbench 分配表）
  - **变更重跑场景**：若本次由 requirement-change-router 路由触发，完成后勾掉 `_progress.md` 重跑清单中本 Skill 对应行（☐→☑）
- `{module_root}/01_requirements/requirements_plan.md` — 产出需求规划文档（含第一轮 Q&A）

> **变更重跑时的写入规范**：当本 Skill 因需求变更被重跑（由 requirement-change-router 路由），`requirements_plan.md` 属**累积目标态文档**——按「文档历史保留约定」**增量更新**（以现有文档为基线应用 delta，可增/改/删，标记每处变更及驱动 CR，并做一致性扫描），**不整篇重生成**。顶部维护变更履历表（只记 delta+why）。`requirements_plan.md` 属一般产物，仅需履历表头、无需 `_history/` 快照；首次生成无需履历表头。

---

## 输入


| 输入           | 必填   | 说明                                                 |
| -------------- | ------ | ---------------------------------------------------- |
| PRD 文档路径   | **是** | 用户提供的 PRD 文件路径                              |
| Workbench 输出目录映射 | **是** | 各业务 Workbench 文档产物的存放根目录（跨 Workbench 需求按 Workbench 分别提供） |
| 项目约束文件   | 否     | 如有额外约束文件，一并读取                           |

---

## 输出

**单个 Workbench 需求**：

```
{module_root}/01_requirements/
└── requirements_plan.md    # PRD→需求映射 + 第一轮业务 Q&A
```

**跨 Workbench 需求**（每个检测到的 Workbench 独立输出一份 `requirements_plan.md`）：

```
{backend_Workbench_root}/01_requirements/requirements_plan.md
{data_Workbench_root}/01_requirements/requirements_plan.md
...（按检测到的 Workbench 逐一）
```

外加 `_progress.md` Workbench 分析分配表与跨 Workbench 信息表的填写。

**文档模板**：`requirements_plan.md` → 参照 `doc-templates/01_requirements_plan_template.md`（其 Q&A 表分「第一轮·业务」「第二轮·技术」两节，本 Skill 只填第一轮节）。

---

## Guardrails


| 规则                        | 触发条件                                          | 执行动作                                              |
| --------------------------- | ------------------------------------------------- | ----------------------------------------------------- |
| **无 PRD 禁止开始**         | 用户未提供 PRD 路径                               | BLOCK：明确说明需要 PRD 路径                          |
| **需求必须有来源**          | 提取的实体/需求映射无法追溯到 PRD 的具体段落      | BLOCK：在对应项上标注 ⚠️ 来源缺失                   |
| **禁止自动消解业务歧义**    | 发现 PRD 中可多种理解的表述                       | BLOCK：生成具体澄清问题，不自行选择解读               |
| **禁止覆盖术语定义**        | `project_glossary.md` 中已有的术语有不同定义      | WARN：追加"待讨论的替代定义"，不覆盖原有定义          |
| **跨 Workbench 需求单一目录**        | 检测到需求跨 2 个以上 Workbench 但用户只提供了单一输出目录 | BLOCK：列出各 Workbench，要求为每 Workbench 提供输出目录               |
| **业务 Q&A 未答完禁止派活** | 第一轮 Q&A 仍有未回答项                           | BLOCK：派活前业务歧义必须消解，不带着未答问题通知各 Workbench |
| **门控未确认禁止继续**      | Step 完成但用户尚未确认                           | BLOCK：等待确认，不自动进入下一步                     |

---

## 执行流程

### 前置条件检查

1. 读取 `local_profile.yaml`：

- **不存在** → BLOCK：提示创建 local_profile.yaml 并声明 role
- 本 Skill 仅允许 `role: dev-lead` 或 `role: ba` 执行

2. 读取 `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md`（若存在）：

- **存在且"需求启动"已 ✅** → 已派活，提示无需重跑（除非需求变更，由 requirement-change-router 路由）
- **存在但"需求启动"未完成** → 从对应 Step 续跑
- **不存在** → 全新执行；依据 `doc-templates/00_progress_template.md` 初始化

3. PRD 文档路径（用户提供）
4. 读取 `project-memory/project_glossary.md`
5. 读取 `project-memory/architectural_constraints.md`

---

### Step 1：Workbench 检测与实体提取

#### Sub-skill: detect-Workbench

扫描 PRD，识别需求涉及的技术 Workbench：

读取本 skill `references/` 下**全部** `{Workbench}.md`（project-setup 已为每个启用 Workbench 生成一份，各含该 Workbench 的识别信号，由其技术栈派生），汇成「Workbench | 识别信号」对照表，逐 Workbench 将 PRD 内容与其信号比对：

- 单个 Workbench：记录 Workbench 类型，继续执行
- 跨 Workbench（2 个以上）：**BLOCK**，列出各 Workbench 和识别信号，要求用户提供各 Workbench 输出目录映射

#### Sub-skill: extract-entities

读取 PRD，提取所有用户故事，按模块分组统计。每个实体必须附 PRD 来源段落/用户故事编号。

---

### Step 2：业务歧义检测与第一轮 Q&A

#### Sub-skill: detect-ambiguity

扫描 PRD，识别**业务级**歧义：术语歧义、概念重叠、前提缺失、边界模糊。
每个歧义必须生成具体澄清问题：指向 PRD 具体位置、可被 Yes/No 或具体数值回答、说明不同答案导致的不同实现方向。

> **聚焦业务层**：本轮只澄清"PRD 本身不清晰"的问题。涉及代码可行性/存量约束的技术歧义，
> 留给各 Workbench impact-analyzer 暴露、由 requirements-analyst 在第二轮汇总裁决——不要在此臆测技术细节。

#### Sub-skill: generate-clarifying-questions

将业务歧义转化为**第一轮 Q&A 清单**，按优先级排序（影响架构方向的 > 影响实现细节的），写入 `requirements_plan.md` 的「Q&A 第一轮·业务」节。

**完成后暂停，等待 BA / 业务方补充第一轮 Q&A 的 Answers。**

> 📝 更新 `_progress.md`，将"需求启动 · 业务 Q&A"标记为 ⏸️

---

### 质量关卡：业务澄清双签

按「人类门控」表"业务澄清双签"行校验（实体提取完整可追溯、第一轮 Q&A 的 Answers 充分、转化策略合理）。

**通过（BA + 技术双签）** → 进入 Step 3。**未通过或仍有未答问题 → BLOCK，不得派活。**

---

### Step 3：术语写入 Memory

#### Sub-skill: build-glossary

对比 PRD 与第一轮 Q&A 中的术语 vs 当前 `project_glossary.md`，将新术语标注为 `⬜ 待确认写入 Memory`。
以表格展示所有候选术语，等待人类逐条操作（✅ 确认 / ❌ 跳过 / ✏️ 修改后写入），确认项写入 `project_glossary.md`。

> 技术分析阶段（impact-analyzer）若发现新的技术术语，由 requirements-analyst 在汇总时补充，本 Skill 只负责业务术语。

> 📝 更新 `_progress.md`，将"需求启动 · 术语写入 Memory"标记为 ✅

---

### Step 4：生成 requirements_plan.md

整合 Step 1–3 成果，按 `doc-templates/01_requirements_plan_template.md` 写出各 Workbench `requirements_plan.md`：
PRD 概览、业务 Workbench 划分、需求转化策略、PRD→需求映射表、覆盖率统计（PRD 用户故事覆盖率，目标 100%）、第一轮 Q&A（含 Answers）。

> 第二轮·技术 Q&A 节先留空（标注"待 impact-analyzer 完成后由 requirements-analyst 填写"）。

---

### Step 5：建分配表、HLD 建议、派活

1. 执行 Workbench 检测结果 → 在 `_progress.md` 的**Workbench 分析分配表**中为每个检测到的 Workbench 创建一行，状态 `⏸️ 待分析`
2. 在**跨 Workbench 信息表**中填写各 Workbench 输出目录
3. 给出 **HLD 必要性建议**：


   | 信号                                                              | 建议                                                                          |
   | ----------------------------------------------------------------- | ----------------------------------------------------------------------------- |
   | 涉及新建组件（新服务/新数据库表/新消息队列 Topic/新数据管道 Job） | **建议先运行 `/solution-designer`（HLD），再通知各 Workbench 运行 impact-analyzer**    |
   | 跨 2 个以上 Workbench 且有新的 Workbench 间数据传递                                 | **建议运行 `/solution-designer`（HLD）**                                      |
   | 存量 CR，所有组件不变，单个 Workbench CRUD 扩展                             | 可直接通知各 Workbench 运行 impact-analyzer，在`_progress.md` HLD 行备注"已评估，跳过" |
4. 将"需求启动"标记为 ✅
5. git push，通知各 Workbench 工程师运行 `impact-analyzer`

> 📝 **写入进度日志**：标记"需求启动"为 ✅，填写 Workbench 分配表与跨 Workbench 信息表，记录 HLD 评估结论与阻断条件。

**Step 5 完成后 BLOCK**：等待所有 Workbench impact-analyzer（及跨 Workbench 需求的 cross-workbench-reviewer）完成 → 由 **requirements-analyst** 汇总。

---

## 人类门控


| 门控点                    | 参与者         | 通过标准                                            |
| ------------------------- | -------------- | --------------------------------------------------- |
| 第一轮业务 Q&A 补充完成   | BA             | 所有 Q 都有明确的 A                                 |
| 业务澄清双签              | BA + 技术      | 实体提取完整可追溯、Answers 充分、转化策略合理      |
| 术语写入 Memory（Step 3） | Dev Lead / BA | 候选术语逐条处理完毕                                |
| HLD 必要性评估            | Dev Lead      | 已按信号判断是否跑 HLD，并在`_progress.md` 记录结论 |

---

## 关键原则

> 以下是 Guardrails 背后的取向解释（why 层）；硬规则（含触发条件与动作）见上方 Guardrails 表，不在此重复。

1. **事实优先**：只陈述 PRD 明确说明的内容，从不推断
2. **来源锚定**：每个实体/需求映射必须附带 PRD 的段落引用——无来源则标 ⚠️，便于人工补全
3. **澄清前置**：业务歧义必须在派活前消解——让各 Workbench 拿到清晰的 PRD，避免在模糊需求上做无效影响分析
4. **业务/技术分轮**：本 Skill 只管业务歧义；技术歧义由 impact-analyzer 暴露、requirements-analyst 第二轮裁决
