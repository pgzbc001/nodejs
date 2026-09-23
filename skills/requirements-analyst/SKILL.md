---
name: requirements-analyst
description: |
  需求汇总专家（流水线汇总棒）。在各 Workbench impact-analyzer（及跨 Workbench cross-workbench-reviewer）全部完成后，
  消费各 Workbench 影响分析，做**第二轮技术澄清**，生成可追溯的用户故事和正式需求文档 requirements.md。
  永不推断未明确说明的内容；发现需进一步澄清的点时生成问题而非自行消解。

  上游是 requirement-kickoff（已完成业务澄清与派活）。本 Skill 负责把"业务需求 + 各 Workbench 代码现状"
  收敛为带可测试验收标准（AC）的正式需求文档，作为 architecture-advisor（各 Workbench LLD）的输入。

  当用户表达"汇总需求"、"生成需求文档"、"出 requirements"、"各 Workbench 分析完了汇总一下"、
  "需求汇总"、"impact 分析完成后汇总"时触发此 Skill。
---

# Requirements Analyst（需求汇总）

把"业务需求 + 各 Workbench 影响分析"收敛为正式需求文档 `requirements.md` 的汇总流程。

---

## 在流水线中的位置

```
PRD → requirement-kickoff（业务澄清 + 派活） → (solution-designer HLD) → 各 Workbench impact-analyzer
     → (cross-workbench-reviewer) → 【requirements-analyst：第二轮技术澄清 + 汇总】 → architecture-advisor → …
```

- **上游 requirement-kickoff**：已完成 Workbench 检测、第一轮业务 Q&A、术语写入、派活，产出各 Workbench `requirements_plan.md`（含第一轮 Q&A）。
- **本 Skill**：消费各 Workbench 影响分析 + 跨 Workbench review，**汇总第二轮技术 Q&A** 并裁决，生成 `requirements.md`。

> 两轮澄清分工：业务歧义已在 kickoff 派活前消解；本 Skill 处理**看了代码才暴露的技术歧义**（可行性、存量约束、跨 Workbench 口径冲突）。

---

## Memory 契约

### 始终加载（每次执行前必读）
- `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md` — 票级进度日志（判断各 Workbench impact 是否齐、跨 Workbench review 是否通过、是否断点续跑）
- `project-memory/project_glossary.md` — 项目术语表，所有输出必须使用其中的标准术语
- `project-memory/architectural_constraints.md` — 架构约束，影响需求转化策略

### 按需加载
- `project-memory/adr_index.md` — 涉及设计方向判断时检查是否有相关 ADR

### 执行后写入（草稿，需人工确认后生效）
- `project-memory/project_glossary.md` — 技术分析阶段发现的新术语，人类逐条确认后追加
- `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md` — 每次人类门控前更新（阶段状态 + 产出路径）
  - **变更重跑场景**：若本次由 requirement-change-router 路由触发，完成后勾掉 `_progress.md` 重跑清单中本 Skill 对应行（☐→☑）
- `{module_root}/01_requirements/requirements_plan.md` — 追加第二轮技术 Q&A 节
- `{module_root}/01_requirements/requirements.md` — 产出正式需求文档

> **变更重跑时的写入规范**：当本 Skill 因需求变更被重跑（由 requirement-change-router 路由），`requirements.md` 属**累积目标态文档**——按「文档历史保留约定」**增量更新**（以现有文档为基线应用 delta，可增/改/删，标记每处变更及驱动 CR，并做一致性扫描），**不整篇重生成**：变更需求要纳入需求范围、更新相关章节，而非覆盖掉与本次变更无关的既有内容。顶部维护变更履历表（只记 delta+why）。`requirements.md` 属核心产物，Type B/C 重跑前须先将旧全文快照到同级 `_history/requirements.v{n}.md`；首次生成无需履历表头。

---

## 输入

| 输入 | 必填 | 说明 |
|------|------|------|
| `_progress.md` | **是** | 从中读取各 Workbench impact 分析产出路径、跨 Workbench review 状态 |
| 各 Workbench `00_impact_analysis.md` | **是** | 各 Workbench 影响分析，含「待澄清问题（回流第二轮 Q&A）」section（第二轮 Q&A 来源） |
| `requirements_plan.md` | **是** | kickoff 产出，含第一轮业务 Q&A 与 PRD→需求映射 |
| PRD 文档 | **是** | 来源追溯锚点 |
| `solution-design.md` | 否 | 若存在（大型新需求），作为架构方向上下文 |
| `cross-Workbench-review.md` | 否 | 跨 Workbench 需求必有。其 ⚠️ 项已裁决完毕，作为 AC 约束消费、不重裁（见关键原则 4） |

---

## 输出

```
{module_root}/01_requirements/
├── requirements_plan.md    # 追加「第二轮·技术 Q&A」节（kickoff 已生成主体）
└── requirements.md         # 结构化需求文档（含可测试 AC）— 本 Skill 核心产物
```

**文档模板**：
- `requirements_plan.md` → 参照 `doc-templates/01_requirements_plan_template.md`（本 Skill 只补「第二轮·技术」Q&A 节）
- `requirements.md` → 参照 `doc-templates/02_requirements_template.md`

---

## Guardrails

| 规则 | 触发条件 | 执行动作 |
|------|---------|---------|
| **kickoff 未完成禁止开始** | `_progress.md` 不存在或"需求启动"未 ✅ | BLOCK：提示先运行 `requirement-kickoff` |
| **影响分析未齐禁止汇总** | Workbench 分配表中存在非 ✅ 的 Workbench | BLOCK：等待该 Workbench impact-analyzer 完成 |
| **跨 Workbench review 未过禁止汇总** | 跨 Workbench 需求但 `cross-Workbench-review.md` 状态非 ✅ | BLOCK：提示先运行 `/cross-workbench-reviewer` |
| **需求必须有来源** | 生成的用户故事无法追溯到 PRD 段落 | BLOCK：在对应需求上标注 ⚠️ 来源缺失 |
| **禁止自动消解技术歧义** | 第二轮发现需澄清的技术问题 | BLOCK：生成具体问题、标注建议回答人，不自行选择解读 |
| **范围变更不自行路由** | 第二轮答案实质改变需求范围（可能使已完成的影响分析失效） | BLOCK：标注 `⚠️ 范围变更`，抛给人（DL/BA）裁决（见关键原则 5） |
| **门控未确认禁止继续** | Step 完成但用户尚未确认 | BLOCK：等待确认，不自动进入下一步 |

---

## 执行流程

### 前置条件检查

1. 读取 `local_profile.yaml`：
    - **不存在** → BLOCK：提示创建 local_profile.yaml 并声明 role
    - 本 Skill 仅允许 `role: dev-lead` 或 `role: ba` 执行
2. 读取 `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md`：
   - **不存在 或 "需求启动" 未 ✅** → BLOCK：先运行 `requirement-kickoff`
   - **Workbench 分配表存在非 ✅ 的 Workbench** → BLOCK：等待该 Workbench impact-analyzer 完成
   - **跨 Workbench 需求且跨 Workbench review 未 ✅** → BLOCK：先运行 `cross-workbench-reviewer`
   - **全部就绪** → 从对应 Step 续跑（按已生成产物跳步）
3. 读取各 Workbench `00_impact_analysis.md`（路径从 Workbench 分配表读取）、`requirements_plan.md`、PRD
4. 读取 `project-memory/project_glossary.md`、`architectural_constraints.md`
5. 若存在则读取 `solution-design.md`、`cross-Workbench-review.md`

---

### Step 1：汇总第二轮技术 Q&A（汇总前必做）

**进来不直接写需求文档，先把代码层暴露的开放问题收拢、裁决。**

1. 扫描所有 Workbench `00_impact_analysis.md` 的「待澄清问题（回流第二轮 Q&A）」section，合并成**第二轮技术 Q&A 清单**。
   > **来源只此一处**。`cross-Workbench-review.md` 的 ⚠️ 不一致项已裁决完毕、不进第二轮重裁，其结论在 Step 2 作为 AC 约束消费（见关键原则 4）。
2. 逐条**标注建议回答人**：偏业务的 → BA；纯技术/Workbench 内的 → Dev Lead；**跨 Workbench 性质的问题**（某 Workbench 怀疑与其他 Workbench 口径冲突，因 role 隔离无法自行确认）→ Dev Lead（由其跨 Workbench 视角裁决）。
3. 追加到 `requirements_plan.md` 的「Q&A 第二轮·技术」节。
4. **完成后暂停，等待裁决。**

> 📝 更新 `_progress.md`，将"第二轮技术 Q&A"标记为 ⏸️

---

### 质量关卡 1：第二轮技术 Q&A 裁决门

- 第二轮每条 Q 都有明确 A（BA / DL 按建议回答人填写）
- 逐条判定答案性质：
  - **填空型**（澄清了原本模糊点，不改范围）→ 在门控内当场消解，继续往下。
  - **范围变更型**（答案实质改变需求范围、可能使某 Workbench 已完成的影响分析失效）→ 标注 `⚠️ 范围变更`，**抛给人（DL/BA）裁决**（见关键原则 5）。

**全部为填空型并裁决通过** → 进入 Step 2。**存在未处理的范围变更 → BLOCK，交人处理后再继续。**

---

### Step 2：生成需求文档（requirements.md）

**输入**：PRD + requirements_plan.md（含两轮 Q&A 的 Answers）+ 各 Workbench 影响分析 + project_glossary.md + （若有）solution-design.md + （若跨 Workbench）`cross-Workbench-review.md` 的**已裁决结论**

> **消费 cross-review 裁决，不重裁**：`cross-Workbench-review.md` 里 DL 已裁决的每条跨 Workbench 不一致（如"backend 改字段名对齐 data"），作为**约束**直接落进对应 REQ 的 AC，不再作为开放问题处理。

#### Sub-skill: enrich-glossary（按需）
若第二轮技术分析暴露了新的**技术术语**，对比 `project_glossary.md`，将新术语标注为 `⬜ 待确认写入 Memory`，等待人类逐条确认后写入（业务术语已在 kickoff 阶段处理）。

#### Sub-skill: draft-user-story
逐条生成需求（含 REQ 编号、用户故事、WHEN/IF/THEN...SHALL 格式 AC、关联 PRD 来源）。AC 必须反映两轮 Q&A 裁决后的真相与各 Workbench 影响分析的现实约束。

#### Sub-skill: trace-requirements
生成可追溯性矩阵：每条需求 → 对应 PRD 用户故事 + （如适用）驱动它的 Q&A 条目。无法追溯的自动标红。

**完成后暂停，等待开发确认。**

> 📝 更新 `_progress.md`，将"需求汇总"标记为 ⏸️

---

### Step 3：术语写入 Memory（若 Step 2 有新技术术语）

以表格展示所有 `⬜ 待确认写入 Memory` 术语，等待人类逐条操作（✅ 确认 / ❌ 跳过 / ✏️ 修改后写入）。

> 📝 更新 `_progress.md`，将"需求汇总"标记为 ✅

---

## 人类门控

| 门控点 | 参与者 | 通过标准 |
|--------|-------|---------|
| 第二轮技术 Q&A 裁决（质量关卡 1） | BA + Dev Lead | 所有 Q 有明确 A；范围变更项已交人裁决处理 |
| requirements.md 确认 | 开发 | 无歧义、AC 可测试、术语一致、反映影响分析约束 |
| 新技术术语写入 Memory（Step 3，若有） | Dev Lead / BA | 候选术语逐条处理完毕 |

---

## 验证回路

- **来源追溯检查**：每条需求必须有 PRD 来源，无来源自动标红
- **开放问题清零检查**：汇总前所有 Workbench「待澄清问题」必须已纳入第二轮 Q&A 并裁决（跨 Workbench ⚠️ 项不在此列——它们已在 cross-workbench-reviewer 裁决，此处只核对其结论已落进 AC）
- **术语一致性检查**：发现同一概念的不同称呼即报警
- **AC 可测试性检查**：每条 AC 必须符合 WHEN/IF/THEN...SHALL 格式
- **覆盖率统计**：输出 PRD 用户故事覆盖率（目标 100%）

---

## 关键原则

1. **事实优先**：只陈述 PRD 与影响分析明确说明的内容，从不推断
2. **来源锚定**：每条需求必须附带 PRD 的段落引用
3. **先澄清后汇总**：进来先收拢第二轮技术开放问题并裁决，再写需求文档——不带着未决问题生成 AC
4. **跨 Workbench 不一致不重裁**：cross-review 的 ⚠️ 已由 cross-workbench-reviewer 的 DL 门裁决；本 Skill 只**消费其裁决结论**落进 AC，不再纳入第二轮重裁（第二轮来源只有 impact「待澄清问题」）
5. **识别但不路由**：发现范围变更只标注、抛给人裁决；**绝不自动调用 requirement-change-router 或自行重跑影响分析**（AI 识别、人类裁决）
6. **术语一致**：所有输出使用 project_glossary.md 中的标准术语
7. **门控不跳过**：每个 Step 完成后必须等待人类确认再继续
