---
name: architecture-advisor
description: |
  架构设计专家。按当前 Workbench（由 local_profile.yaml 的 role 决定）加载对应的 Workbench 设计参考，
  基于需求文档完成 Schema/Pipeline 设计、接口/数据流设计、技术设计、任务拆分四个阶段。
  每个阶段先查 ADR 合规，结束前做决策扫描并自动生成 ADR 草稿。

  本 Skill 是「瘦主体」：SKILL.md 只承载通用设计流程与纪律；本 Workbench 的技术规范、产出骨架、
  红线约束等技术绑定内容，运行时从 references/{role}.md 单独加载，不加载其他 Workbench 的无用信息。

  范围边界：本 Skill 负责 HLD 之后的详细设计（LLD）——Schema/接口/技术设计/任务拆分；
  若仍处于 HLD 阶段（整体组件选型、数据流编排、跨 Workbench 契约），请改用 solution-designer。

  当用户表达"开始设计"、"做技术方案"、"SQL 设计"、"API 设计"、"技术设计"、
  "任务拆分"、"设计文档"、"做 design"、"详细设计"、"LLD"时触发此 Skill。
---

# Architecture Advisor

从需求文档到完整技术方案（Schema + 接口/管道 + 技术设计 + 任务清单）的端到端设计流程。

> **本 Skill 的分层**：下文为**各 Workbench 通用**的流程、纪律与产出骨架。凡涉及"本 Workbench 具体怎么写"
> （字段命名、API 格式、分层结构、任务粒度、Workbench 红线等技术细节），一律见运行时加载的
> `references/{role}.md`。SKILL.md 不内联任何单个 Workbench 技术内容。

---

## Memory 契约

### 始终加载（每次执行前必读）
- `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md` — 票级进度日志（判断是否从断点续跑）
- `project-memory/project_glossary.md` — 项目术语表，确保设计命名与业务术语一致
- `project-memory/architectural_constraints.md` — 架构约束，所有设计决策必须遵守
- `project-memory/adr_index.md` — ADR 目录，设计前检查相关决策记录
- `references/{role}.md` — **本 Workbench 设计参考**（role 来自 local_profile.yaml；只加载本 Workbench 那一份）

### 按需加载
- `project-memory/adrs/{ADR-xxx}.md` — 遇到与已有 ADR 相关的设计时加载完整 ADR
- `rules/{role}/`（编码/接口/库规范，按 Workbench 存在）— 团队工程规范。**按需自主加载（非强制）**：Schema 设计时可取 `database.md`、接口设计时可取 `api.md`、任务拆分前可取 `coding.md`；规范较大时只取相关章节，不强制全文，文件不存在则跳过
- `{{REQUIREMENTS_DIR}}/{ticket-id}/solution-design.md` — 若存在，将 HLD 约束作为设计输入
- `{module_root}/00_impact_analysis.md` — impact-analyzer 产出；存量 CR 时作为「涉及哪些既有代码」的锚点
- `{code_roots.{role}}/AGENTS.md`（优先级 1）/ 同路径 `CLAUDE.md`（优先级 2）— **存量 CR 必读**：分层/命名/约定的设计前提；按 impact-analysis 锚点定点查既有表/实体/接口（不广度重探索）。两者皆无 → BLOCK。greenfield 跳过

### 执行后写入（草稿，需人工确认后生效）
- `project-memory/adrs/ADR-{编号}-{标题}.md` — 重要架构决策后生成 ADR 草稿
- `project-memory/adr_index.md` — 新增 ADR 条目（草稿状态）
- `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md` — 每次人类门控前更新（阶段状态 + 产出路径）
  - **变更重跑场景**：若本次由 requirement-change-router 路由触发，完成后勾掉 `_progress.md` 重跑清单中本 Skill 对应行（☐→☑）
- 本 Skill 各 Step 产出的设计文档（`02_schema/`、`03_api/` 或 `03_pipeline/`、`04_design/design.md`、`05_task/tasks.md`）

> **变更重跑时的写入规范**：当本 Skill 因需求变更被重跑（由 requirement-change-router 路由），
> 按「文档历史保留约定」**增量更新**——以现有设计文档为基线应用 delta（可增/改/删，标记每处变更及驱动 CR，并做一致性扫描），**不整篇重生成**。
> 例：原设计有 A 表，变更新增 B 表 → 在设计文档中**追加 B 表设计、保留 A 表设计**，并同步更新被波及的汇总/计数/交叉引用；Type C 若 A 方案被取代则删除 A。
> 顶部维护变更履历表（只记 delta+why）。`design.md`（技术设计主文档）属**核心产物**，Type B/C 重跑前须先将旧全文快照到同级
> `_history/design.v{n}.md`；其余设计文档属一般产物，仅需履历表头，旧版靠 git 追溯。
> 首次生成（非重跑）无需履历表头。

---

## 输入

| 输入 | 必填 | 说明 |
|------|------|------|
| 需求文档路径 | **是** | `{module_root}/01_requirements/requirements.md`（requirements-analyst 产物） |
| Workbench 输出目录映射 | **是** | 本 Workbench 文档产物的存放根目录 |
| 设计 Workbench 类型 | **是** | 本次设计所属的 Workbench（取自 local_profile.yaml 的 role；跨 Workbench 需求各 Workbench 分别执行本 Skill） |
| 已有数据模型 | 否 | 如有已存在的表结构/Schema，一并读取 |
| 执行模式 | 否 | 标准流程 / 快速通道（跳过 Schema 与接口/管道设计，直接做技术设计 + 任务拆分）；默认标准流程 |

---

## 输出

各 Workbench 产出文件统一放在 `{module_root}/` 下（具体文件名与适用 Workbench 见 `references/{role}.md`）：

```
{module_root}/
├── 01_requirements/   # requirements-analyst 产物（输入）
├── 02_schema/         # Step 2.1：数据 Schema 设计（无数据存储的 Workbench 可跳过）
├── 03_api/ 或 03_pipeline/   # Step 2.2：接口/管道设计（按 Workbench）
├── 04_design/         # Step 3：技术设计主文档 design.md
└── 05_task/           # Step 4：任务清单 tasks.md
```

---

## Guardrails

| 规则 | 触发条件 | 执行动作 |
|------|---------|---------|
| **无需求文档禁止开始** | `requirements.md` 不存在或质量关卡未通过 | BLOCK：须先完成 requirements-analyst |
| **架构约束违规** | 设计违反 `architectural_constraints.md` 中任一约束 | BLOCK：指出违反的具体约束编号，要求决策（改方案 or 新建 ADR 覆盖） |
| **ADR 冲突未处理** | 设计方案与 ACCEPTED 状态 ADR 冲突 | BLOCK：说明冲突 ADR 编号与内容，要求决策 |
| **决策后必须生成 ADR 草稿** | 每个设计 Step 完成时 | 主动列出本次架构决策，对无现有 ADR 覆盖的决策自动生成 PROPOSED 草稿；在下一质量关卡由人类逐条确认（ACCEPTED / 删除）。**此为硬性步骤，不可省略**（见各 Step 的 draft-adr 子步骤） |
| **Workbench 设计规范混用** | 使用了不属于本 Workbench 的设计规范（如本 Workbench 无数据存储却生成了 DDL，或套用了其他 Workbench 的命名/分层约定） | BLOCK：本 Workbench 设计规范以 `references/{role}.md` 为准；告知用户并要求确认正确规范 |
| **Workbench 红线违反** | 触碰本 Workbench `references/{role}.md` 列明的"Workbench schema/设计红线" | BLOCK：引用该红线条目，要求修正 |
| **HLD 约束违反** | 设计与 `solution-design.md` 的契约或约束冲突 | BLOCK：明确说明冲突点，不得静默绕过 |
| **存量设计缺 agent 文档** | 存量 CR 需读既有代码，但目标服务 `AGENTS.md` 与 `CLAUDE.md` 均不存在 | BLOCK：提示用户先在该仓库补充 agent 文档后重跑；不得凭空对既有代码做 delta 设计 |
| **门控未确认禁止继续** | Step 完成但用户尚未确认 | BLOCK：等待确认，不自动进入下一步 |

---

## 执行流程

> **Step 编号说明**：Step 编号对应整体交付流水线的 Phase（Phase 1=需求，由 requirements-analyst 承担，
> 故本 Skill 从 Step 2 起）。本节"前置条件检查"用 a/b/c… 编号，与 Step 编号空间分离，避免混淆。

### 前置条件检查

a. 读取 `local_profile.yaml`：
   - **不存在** → BLOCK：必须先创建并显式声明 role
   - 获取 `role` 和 `code_roots`；非 `dev-lead` 仅允许操作本人 Workbench 的文档目录
   - **加载 `references/{role}.md`**（本 Workbench 设计参考）；dev-lead 须先确认本次设计针对哪个 Workbench，再加载对应参考
b. 读取 `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md`（若存在）：
   - **存在** → 读取状态，定位上次暂停阶段，从该节点续跑
   - **不存在** → 全新执行；首个人类门控前初始化
c. 读取 `requirements.md`（确认需求已就绪）
d. 读取 `project-memory/architectural_constraints.md`、`adr_index.md`、`project_glossary.md`
e. 若 `solution-design.md` 存在，读取并提取本 Workbench 约束
f. 读取本 Workbench `00_impact_analysis.md`（若存在，impact-analyzer 产出）：作为本次设计涉及既有代码的**锚点**（指出涉及哪些既有表/实体/接口/文件）。
g. **存量 CR（impact-analysis 表明涉及既有代码）→ 读取目标服务 agent 文档**，作为分层/命名/约定的设计前提：
   优先级 1=`{code_roots.{role}}/AGENTS.md`；缺失则回退同路径 `CLAUDE.md`（优先级 2）；两者皆无 → BLOCK，提示用户先在该仓库补充后重跑。
   设计各步（Schema/接口/任务拆分）**按 impact-analysis 锚点对既有表/实体/接口做定点查阅**，以设计准确的 delta（扩展既有 vs 新建）；**不重做 impact-analyzer 的广度探索**。
   **greenfield（无既有代码涉及）→ 跳过本步，纯按需求/HLD 设计。**
h. 询问用户执行模式：
   - **标准流程**（默认）→ 依次执行 Step 2.1 → 2.2 → 3 → 4
   - **快速通道** → **跳过 Step 2.1 与 Step 2.2（Schema 与接口/管道设计），直接进入 Step 3（技术设计）与 Step 4（任务拆分）**。适用于无需新增/变更数据结构与对外契约的纯逻辑改动；若中途发现需改 Schema/接口，回退标准流程。

---

### Step 2.1：数据 Schema 设计

> **适用性**：若本 Workbench 无数据存储职责（见 `references/{role}.md`），跳过此步直接进入 Step 2.2。
> **快速通道**：跳过 Step 2.1 与 2.2，直接进入 Step 3。
> **参照模板**：`doc-templates/03_schema_design_template.md`。

**执行者**：AI 生成　**输出**：`02_schema/` 下的 Schema 设计文档（文件名见 Workbench 参考）

1. **子步骤 check-adr-compliance**：查 `adr_index.md` 是否有与数据模型相关的 ADR；有则确认本设计是否沿用。
2. **子步骤 generate-schema**：从 `requirements.md` 的业务实体提取 Schema，**遵循 `references/{role}.md` 的
   「Schema 设计规范」与「结构化产出骨架」**（不同 Workbench 的命名/字段/存储/血缘维度不同，全部以 Workbench 参考为准）。
   **存量 CR：先按 impact-analysis 锚点定点查既有表/实体，再决定扩展既有 vs 新建，不凭空设计 delta。**
3. **子步骤 draft-adr（必做）**：本步结束前执行决策扫描——
   ① 列出本次 Schema 设计的所有架构决策（决策项 | 决策内容 | 已有 ADR 覆盖？）；
   ② 对"无 ADR 覆盖"的决策，自动生成 `project-memory/adrs/ADR-{下一编号}-{标题}.md`（PROPOSED，**格式引用 `doc-templates/08_adr_template.md`**，与 solution-designer 共用同一模板），并在 `adr_index.md` 追加条目；
   ③ 草稿清单留待质量关卡逐条确认。

**Schema 设计完成后不单独门控**，继续 Step 2.2（Schema 与接口/管道设计可并行，统一在下一道质量关卡评审）。Step 2.1 的 draft-adr 草稿留待合并门控逐条确认。

---

### Step 2.2：接口/管道设计

> **参照模板**：`doc-templates/03_schema_design_template.md` 的接口契约章节（按 Workbench 裁剪）。

**执行者**：AI 生成　**输出**：`03_api/` 或 `03_pipeline/` 下的设计文档（形态与文件名见 Workbench 参考）

1. **子步骤 check-adr-compliance**：查 `adr_index.md` 是否有与接口/管道/服务分层相关的 ADR。
2. **子步骤 generate-interface**：从 `requirements.md` 推导本 Workbench 接口/管道设计，
   **遵循 `references/{role}.md` 的「接口/管道设计规范」与产出骨架**
   （本 Workbench 的设计形态——对外接口 / 数据管道 / 接口消费 / 其他——以 Workbench 参考为准，SKILL.md 不预设 Workbench 种类）。
3. **子步骤 draft-adr（必做）**：同 Step 2.1 的决策扫描三步，对接口/管道设计决策执行。

> Schema（Step 2.1）与接口/管道（Step 2.2）设计可并行，都完成后统一暂停，进入合并门控。

**Step 2.1 + 2.2 都完成后暂停，等待人类确认。** 📝 更新 `_progress.md`

---

### 质量关卡：Schema + 接口/管道 设计评审（合并门控）

**参与者**：本 Workbench 工程师 + 数据/DB 负责人（必要时跨 Workbench 相关方）
**检查内容**：① Schema 是否符合本 Workbench 设计规范、字段/实体覆盖需求无遗漏　② 接口/管道契约是否完整合理、有无遗漏的接口/字段/Job　③ Schema 与接口/管道是否相互对齐，并与 `architectural_constraints.md` / ADR 对齐　④ Step 2.1 与 Step 2.2 的 draft-adr 草稿逐条确认（ACCEPTED / 删除）
**不通过** → 返回对应 Step（2.1 或 2.2）修改　**通过** → 进入 Step 3

---

### Step 3：技术设计

> **参照模板**：`doc-templates/04_technical_design_template.md`。
> **重要**：`design.md` 聚焦架构决策、业务逻辑、组件交互；Schema 与接口/管道详设已在
> `02_schema/`、`03_*/` 定义，design.md **引用并补充分析，不重复 DDL 和详参**。

**执行者**：AI 生成　**输出**：`04_design/design.md`

**子步骤 check-adr-compliance**：查 `adr_index.md` 是否有与本次技术设计（架构分层、组件交互、错误处理策略）相关的 ADR；有则确认本设计是否沿用。

**子步骤 generate-tech-design**：生成包含以下章节的设计文档。
**§1 §2 §5 §8 为各 Workbench 共同骨架；§3 §4 §6 §7 §9 的本 Workbench 填法见 `references/{role}.md`**：

| 章节 | 内容（各 Workbench 骨架） | 本 Workbench 具体填法 |
|------|----------------|------------|
| **§1 设计决策表** | 关键决策（决策项、内容、理由——必须解释"为什么"） | 各 Workbench 共同 |
| **§2 系统架构图** | Mermaid：本 Workbench 在整体架构中的位置 + 组件关系 | 各 Workbench 共同 |
| **§3 代码结构变更** | 新增/修改的代码单元（路径 + 职责） | 见 Workbench 参考（分层/Job/组件粒度不同） |
| **§4 数据模型分析** | 引用 §2_schema，补充关系/流向/查询模式 | 见 Workbench 参考 |
| **§5 核心业务流程** | flowchart / sequenceDiagram / stateDiagram（按需裁剪） | 各 Workbench 共同 |
| **§6 组件与实现设计** | 各单元内部设计 | 见 Workbench 参考 |
| **§7 错误处理设计** | 异常分类与处理策略 | 见 Workbench 参考 |
| **§8 设计追溯矩阵** | 设计产物 → 对应 REQ 编号（**必须覆盖全部需求**） | 各 Workbench 共同 |
| **§9 测试策略** | 单元/集成/端到端测试重点 | 见 Workbench 参考 |

**子步骤 draft-adr（必做）**：同前，对技术设计决策执行决策扫描三步。

**完成后暂停，等待人类确认。** 📝 更新 `_progress.md`

---

### 质量关卡：技术设计确认

**参与者**：本 Workbench 工程师 + Dev Lead
**检查内容**：① 系统架构与分层是否合理　② 数据模型是否与 Schema 文档一致
③ 流程图是否完整准确　④ §8 追溯矩阵是否覆盖全部 REQ　⑤ draft-adr 草稿逐条确认
**不通过** → 返回 Step 3 修改　**通过** → 进入 Step 4

---

### Step 4：任务拆分

> **参照模板**：`doc-templates/05_tasks_template.md`。
> **无 ADR 扫描**：任务拆分不产生架构决策，本步不含 check-adr-compliance / draft-adr 子步骤。

**执行者**：AI 生成 → 工程师 review　**输出**：`05_task/tasks.md`

**子步骤 generate-tasks**：根据 `design.md` 的组件设计，**按 `references/{role}.md` 的「任务阶段划分」
动态裁剪阶段**（不限固定模式）。每个任务包含：

- 任务编号和标题
- 关联需求/设计章节（来源锚点，可追溯）
- 预估工作量（小时）
- 前置依赖
- **任务粒度：每个任务可在 2–4 小时内完成**

**完成后暂停，等待人类确认。** 📝 更新 `_progress.md`，将所有设计阶段标记为 ✅

---

### 质量关卡：任务清单确认

**参与者**：本 Workbench 工程师
**检查内容**：任务可独立实现、粒度合理（每个任务 2–4 小时内完成）、依赖明确、覆盖 `design.md §3` 全部新增/修改代码单元
**不通过** → 返回 Step 4 修改　**通过** → 设计阶段完成

---

## 验证回路

- **ADR 合规性**：每个设计步骤前检查 `adr_index.md`，发现冲突立即阻断
- **设计追溯矩阵**：§8 必须覆盖 `requirements.md` 中所有 REQ-xx 需求
- **约束扫描**：`design.md` 生成后逐条扫描 `architectural_constraints.md`，无违反
- **任务覆盖率**：`tasks.md` 中的任务必须覆盖 `design.md §3` 所有新增/修改的代码单元

---

## 关键原则

1. **决策即扫描**：每个 Step 结束做 draft-adr 决策扫描，关键选型立即草稿 ADR——不积压、不遗漏，避免事后补记失真。
2. **引用不复制**：`design.md` 引用 Schema 和接口文档，不重复 DDL 与详参，避免两处双写漂移。
3. **决策透明**：设计决策表（§1）必须解释"为什么"，不只写"做了什么"。
4. **Workbench 内聚焦**：只加载并遵循 `references/{role}.md`，不引入其他 Workbench 的设计规范。

> ADR 合规、架构约束、人类门控均为硬约束：拦截规则见 `Guardrails`，自检动作见 `验证回路`，此处不再复述。
