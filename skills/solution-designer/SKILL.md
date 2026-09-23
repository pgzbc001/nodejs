---
name: solution-designer
description: |-
  解决方案设计专家（HLD）。在 requirement-kickoff 完成后、
  各 Workbench impact-analyzer 执行前，与 Dev Lead 一起**确认高层技术方向**：识别涉及组件、设计跨 Workbench 数据流、
  选型关键技术方案，并将技术约束预警反馈给 BA/Dev Lead。

  **本 Skill 的核心是"定方向"，不是"发现代码现状"（后者是 impact-analyzer 的职责）。**
  工作方式以**人机技术问答**为主：读需求文档后与 Dev Lead 逐点澄清技术信息。
  **默认不读代码**；仅当某个选型岔口靠问答无法定夺、且 Dev Lead **授权**时，才按其指定的**锚点窄读**既有代码，
  绝不自行展开代码广度探索（"闷头分析"）。未经代码核实的选型假设一律显性标注，留待下游 impact-analyzer 码级验证。

  产出 solution-design.md 作为各 Workbench impact-analyzer 和 architecture-advisor 的架构方向上下文，
  确保影响分析和 LLD 在统一的技术前提下展开，避免工程师在错误的架构假设上做无效分析。

  适用于大型新功能（Greenfield）或涉及显著架构变更的需求；
  存量代码小型 CR（组件不变、架构不变）可跳过此步骤，但需在 _progress.md 中显式记录。

  当用户表达"做高层设计"、"HLD"、"技术方案选型"、"数据流设计"、
  "组件设计"、"solution design"、"架构选型"、"做方案"时触发此 Skill。
---

# Solution Designer

在业务需求与详细设计之间，与 Dev Lead 一起确认跨 Workbench 高层架构方向（HLD）。

> **定位本质**：HLD 是**判断/定向**活动，不是**发现/分析**活动。代码现状的广度探索由下游 impact-analyzer 负责；
> 本 Skill 靠"读需求文档 + 人机技术问答"定方向，读代码须人类授权（详见「HLD 技术问答回路」节）。

---

## 定位

**大型新需求**：requirement-kickoff → **solution-designer（本 Skill）** → impact-analyzer（各 Workbench）→ requirements-analyst（汇总）→ architecture-advisor（各 Workbench LLD）。

本 Skill 产出 `solution-design.md`，被下游 impact-analyzer / architecture-advisor 消费（接口见 §5/§8）。

**存量 CR**：跳过本 Skill，直接 requirement-kickoff → impact-analyzer →…（跳过判据见下方「判断：是否需要 HLD」）。

---

## Memory 契约

### 始终加载（每次执行前必读）
- `local_profile.yaml` — 必须为 role: dev-lead（HLD 需要跨 Workbench 视角）
- `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md` — 确认 requirement-kickoff 已完成，判断断点续跑状态
- `project-memory/architectural_constraints.md` — 硬约束，不可违反
- `project-memory/adr_index.md` — 现有架构决策目录（选型前必查）

### 按需加载
- `project-memory/adrs/{file}.md` — 当选型可能与已有 ADR 冲突时加载全文
- `project-memory/project_glossary.md` — 输出文档中的业务术语须与术语表一致
- 各 Workbench `00_impact_analysis.md` — **仅在重跑时可用**（首轮 HLD 跑在 impact-analyzer 之前，该文件尚不存在）。
  首轮 HLD 的现状根基来自**人机技术问答** + 人类授权下的锚点查代码，不依赖此文件。
- `{{REQUIREMENTS_DIR}}/{ticket-id}/cross-Workbench-review.md` — 同上，仅重跑时可用

### 人类授权下按需读取（默认不读，见 Guardrail「禁止自主代码探索」）
- `{code_roots.{Workbench}}/AGENTS.md`（优先级 1）/ `CLAUDE.md`（优先级 2）— **仅当 Dev Lead 授权查阅某既有组件时**读取；两者皆无 → BLOCK 该次查阅，不得凭空探索
- 既有代码文件 — **仅按 Dev Lead 指定的锚点窄读**（定位特定组件/接口以定夺某选型），读完即回到问答，不展开广度探索

### 执行后写入
- `{{REQUIREMENTS_DIR}}/{ticket-id}/solution-design.md` — HLD 主文档
- `{{REQUIREMENTS_DIR}}/{ticket-id}/requirements-delta.md` — 需求修订建议（仅当技术约束导致需求变更时）
- `project-memory/adrs/ADR-{N}-{title}.md` — 架构决策草稿（每个关键选型一份）
- `project-memory/adr_index.md` — 追加新 ADR 条目（PROPOSED 状态）
- `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md` — 更新 HLD 阶段状态
  - **变更重跑场景**：若本次由 requirement-change-router 路由触发，完成后勾掉 `_progress.md` 重跑清单中本 Skill 对应行（☐→☑）

> **变更重跑时的写入规范**：`solution-design.md` 属**累积目标态文档**——按「文档历史保留约定」**增量更新**（以现有 HLD 为基线应用 delta，可增/改/删，标记每处变更及驱动 CR，并做一致性扫描），**不整篇重生成**。`solution-design.md` 属核心产物，Type B/C 重跑前须先将旧全文快照到同级 `_history/solution-design.v{n}.md`；`requirements-delta.md` 本身即增量文档，按需追加。首次生成无需履历表头。

---

## 输入

| 输入 | 必填 | 说明 |
|------|------|------|
| 票据 ID | **是** | `{{TICKET_PREFIX}}-xxxx` |
| PRD 文档路径 | 否 | 默认从 `{{REQUIREMENTS_DIR}}/{ticket-id}/` 下自动查找 |

---

## Guardrails

| 规则 | 触发条件 | 执行动作 |
|------|---------|---------|
| **非 Dev Lead 禁止执行** | role 不为 dev-lead | BLOCK：HLD 需要跨 Workbench 视角，请 Dev Lead 执行 |
| **需求启动未执行** | `_progress.md` 不存在或"需求启动"状态不为 ✅ | BLOCK：须先完成 requirement-kickoff |
| **选型无理由** | 组件选型未说明理由和备选方案 | BLOCK：每个选型必须附带选择原因和未选理由 |
| **ADR 冲突未处理** | 选型违反 ACCEPTED 状态的 ADR | BLOCK：必须明确说明冲突，产出新 ADR 或修改选型 |
| **需求变更未显性化** | 技术约束导致需求须变更但未写入 requirements-delta.md | BLOCK：需求变更必须显性化 |
| **架构约束冲突** | 选型违反 architectural_constraints.md 硬约束 | BLOCK：不得继续，必须重新选型或上报 |
| **禁止自主代码探索** | skill 想读代码但未获 Dev Lead 授权，或想展开广度探索 | BLOCK：HLD 定方向不靠闷头读代码。可**建议**"读 X 有助于定夺，是否授权？"，但须 Dev Lead 同意后才读，且只锚点窄读、读完即回问答；读代码须走 AGENTS.md(P1)/CLAUDE.md(P2)/BLOCK 门 |
| **未验证假设未标注** | 某选型依赖对既有代码的假设、但未经代码核实也未标注 | BLOCK：每个基于未核实假设的选型必须标 `⚠️ 假设未验证：{假设}`，留待 impact-analyzer 码级验证 |

---

## 执行流程

### 前置条件检查

1. 读取 `local_profile.yaml`：role 不为 dev-lead → BLOCK
2. 读取 `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md`：
    - 不存在 → BLOCK（requirement-kickoff 未执行）
    - "需求启动" 状态不为 ✅ → BLOCK
    - "解决方案设计（HLD）" 已为 ✅ → 提示已完成，询问是否重新设计

### 判断：是否需要 HLD

| 信号 | 判断 |
|------|------|
| 需求涉及新建服务/新建数据库表/新消息队列 Topic/新数据管道 Job | **需要 HLD** |
| 需求跨 2 个以上 Workbench 且 Workbench 间有数据传递 | **需要 HLD** |
| 需求涉及组件选型决策（同步 vs 异步、引入新技术） | **需要 HLD** |
| 存量代码小改，所有组件不变，架构不变 | 可跳过 HLD |
| 单个 Workbench 纯 CRUD 扩展，无新依赖，无跨 Workbench 数据流 | 可跳过 HLD |

若判断"可跳过"：告知用户理由，等待确认。若跳过，在 `_progress.md` 备注"已评估，跳过 HLD：{原因}"，状态标为 ✅（跳过）。

> **brownfield + 改架构** 也属"需要 HLD"。这类需求的选型依赖存量代码现状——但**不**因此把它另立成一条流水线，而是用下方
> 「HLD 技术问答回路」+「人类授权按需查代码」就地解决：靠问答拿现状信息，问答不够时由 Dev Lead 授权锚点查代码。

---

### 贯穿全程：HLD 技术问答回路（人机定方向）

HLD 所需的技术信息，**很多在 Dev Lead 脑子里**（现有基础设施、团队约束、历史决策），靠问答比靠读代码更快更准。
因此本 Skill 在 Step 1–4 全程驱动一个**人机技术问答回路**：

1. 每遇到一个定方向所需、但文档/Memory 未覆盖的技术信息点 → **向 Dev Lead 提一个具体问题**（一次一问，不批量列单）。
2. 若某选型岔口靠问答仍无法定夺，且**读既有代码能解决** → **AI 建议**："要在 A/B 之间定，建议查一下既有的 `{组件}` 怎么实现的，是否授权我读？"
   - Dev Lead **授权** → 按其指定锚点窄读（走 AGENTS.md 门），读完回呈结论、回到问答。
   - Dev Lead **不授权 / 暂不读** → 按当前假设继续，并在该选型标 `⚠️ 假设未验证`。
3. **AI 永不自行决定去爬代码**（见 Guardrail「禁止自主代码探索」）。

---

### Step 1：业务目标与约束梳理

从 PRD 提炼：
1. 核心业务目标（1-3 句话）
2. 关键非功能需求（性能、并发、数据量级、实时性）
3. 架构约束命中点（逐条扫描 architectural_constraints.md）

---

### Step 2：组件识别与选型

识别涉及的所有技术组件（现有复用 vs 新建），对每个存在选择空间的组件执行选型分析：
- 列出 2-3 个候选选项
- 逐项分析优缺点及与现有架构的兼容性
- 给出推荐选项及理由
- 检查是否与现有 ADR 冲突
- **标注现状依赖**：若该选型依赖某个对既有代码的假设（如"现有 `OrderService` 是同步调用"），
  优先通过问答向 Dev Lead 求证；仍无法确认时按「HLD 技术问答回路」第 2 步**建议授权查代码**；
  若未核实即采用，必须标 `⚠️ 假设未验证：{假设}`（该标注会随 HLD 传给 impact-analyzer 做码级验证）

**选型完成后立即触发 Sub-skill: draft-adr**（每个关键选型一份）

#### Sub-skill: draft-adr

每个关键选型生成一份 `project-memory/adrs/ADR-{N}-{标题}.md`（status: PROPOSED），并在 `adr_index.md` 追加条目。
**ADR 格式统一引用 `doc-templates/08_adr_template.md`**（与 architecture-advisor 共用同一模板，保证格式一致）；
草稿留待人类门控逐条确认（ACCEPTED / 删除）。

---

### Step 3：数据流设计

描述数据从产生到消费的完整路径，覆盖所有涉及 Workbench。每个数据传递点标注：
- **传输方式**：同步 REST / 异步消息队列 / 批量 ETL / 定时轮询
- **时效性要求**：实时（<1s）/ 准实时 / 批量
- **数据格式约定**（高层）

---

### Step 4：跨 Workbench 数据契约（高层）

| 契约名称 | 提供 Workbench | 消费 Workbench | 传输方式 | 关键字段（高层描述） | 时效要求 |
|---------|-------|-------|---------|------------------|---------|

> 注：此处只需高层字段列举，具体字段名/类型在各 Workbench architecture-advisor LLD 阶段详细定义，但必须在此契约约束范围内。

---

### Step 5：需求修订检查

对照组件选型和数据流设计，检查是否有需要修订的需求项（实时性降级、范围收窄、新约束产生等）。

- **有修订建议** → 写入 `requirements-delta.md`，**不直接修改原 requirements.md**
- **无修订建议** → 在 solution-design.md §7 明确记录"本次 HLD 无需求修订"

**完成后暂停，等待 BA + Dev Lead 确认修订方案。**

---

### 人类门控 1：需求修订确认

**通过** → BA 更新 requirements.md（人工操作），继续 Step 6

---

### Step 6：生成 HLD 主文档

汇总写入 `{{REQUIREMENTS_DIR}}/{ticket-id}/solution-design.md`，**骨架与各节填法见 `references/solution-design-template.md`**。
该文档是被下游**每个 Workbench**消费的承重产物（impact-analyzer 按 §号取数、architecture-advisor 据 §5 约束做 LLD），
各节必须填**实质内容**，不得只留标题占位。

> **接口不变量**：§5（对下游各 Workbench 的输入约束）、§8（未验证假设清单）是与 impact-analyzer 的紧耦合接口
> （它按这两个 §号取数），节号与含义不可随意改动；如需调整须同步改 impact-analyzer 的消费逻辑。

**完成后 BLOCK → 人类门控 2：HLD 整体审批**

---

## 人类门控

| 门控点 | 参与者 | 通过标准 |
|--------|-------|---------|
| 需求修订确认 | BA + Dev Lead | 技术约束导致的需求变化得到确认，requirements.md 已更新 |
| HLD 整体审批 | Dev Lead | 选型·数据流·契约·约束均已确认，无遗留问题 |

---

## 关键原则

1. **定方向，不发现**：HLD 是判断/定向活动；代码现状的广度发现是 impact-analyzer 的职责，本 Skill 不越界
2. **问答优先、读代码须授权、假设须标注**：定方向先靠人机问答；读代码仅在 Dev Lead 授权下锚点窄读；未核实假设一律标 `⚠️ 假设未验证`，交下游 impact-analyzer 码级验证（完整规则见「HLD 技术问答回路」节与 Guardrails，此处不复述）
3. **HLD 约束 LLD，LLD 不覆盖 HLD**
4. **选型必有理由**：没有理由的选型等于没有决策
5. **需求变更显性化**：技术约束影响需求时，必须产出 requirements-delta.md
6. **ADR 即时生成**：选型完成后立即草稿 ADR，不推迟
7. **Dev Lead 专属**：HLD 需要跨 Workbench 视角，各 Workbench 工程师不执行此 Skill
