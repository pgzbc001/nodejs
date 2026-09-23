---
name: impact-analyzer
description: |
  Workbench 影响分析专家。针对特定需求（CR/新功能），分析该需求对本工程师所负责 Workbench 的代码现状要改什么，
  产出结构化影响分析文档（含可回流第二轮技术 Q&A 的「待澄清问题」）。

  与 module-explorer 的区别：
  - module-explorer：通用模块探索，无票据上下文，理解"现有代码做什么"
  - impact-analyzer：面向特定需求，分析"这个需求对我的 Workbench 要改什么"

  当用户表达"分析这个需求对我的影响"、"impact analysis"、"这个需求我这边要改什么"、
  "CR 影响分析"、"帮我做 Workbench 分析"时触发此 Skill。
---

# Impact Analyzer

针对特定需求，分析本 Workbench 代码的影响范围，产出结构化影响分析文档。

> **本 Skill 的分层**：下文为**各 Workbench 通用**的分析纪律、产出结构与验证回路。本 Workbench"用什么命令定位代码"
> 这类技术绑定内容，运行时从 `references/{role}.md` 加载。SKILL.md 不内联单个 Workbench 定位命令。

---

## Memory 契约

### 始终加载（每次执行前必读）
- `local_profile.yaml` — 获取当前工程师的 role 和 code_roots（必须存在）
- `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md` — 读取 requirement-kickoff 写入的 Workbench 分配信息，确认本人负责的 Workbench
- `references/{role}.md` — **本 Workbench 代码定位参考**（role 来自 local_profile.yaml）
- `project-memory/project_glossary.md` — 项目术语表，确保分析描述使用标准术语
- `project-memory/architectural_constraints.md` — 架构约束，识别影响分析中的约束命中点

### 按需加载
- `{{REQUIREMENTS_DIR}}/{ticket-id}/solution-design.md` — 若 HLD 已完成，加载为架构方向上下文（见前置检查 3a）
- `project-memory/adr_index.md` — 发现现有代码与 ADR 可能冲突时加载

### 执行后写入
- `{Workbench_root}/00_impact_analysis.md` — 影响分析文档（`Workbench_root` 从 `_progress.md` 跨 Workbench 信息表读取）
- `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md` — 更新本 Workbench 状态为 ✅，记录产出路径和完成时间
  - **变更重跑场景**：若本次由 requirement-change-router 路由触发，完成后勾掉 `_progress.md` 重跑清单中本 Skill 对应行（☐→☑）

> **变更重跑时的写入规范**：`00_impact_analysis.md` 是 **delta 文档**，描述"从当前代码出发要改什么"。
> 它是 requirement-change-router「文档历史保留约定」中唯一**覆盖正文**的产物：重跑基于已改过的代码重新推导，旧版自然作废，直接覆盖为最新分析；旧版靠 git 追溯，不需 `_history/` 快照。
> 仍在文档顶部维护变更履历表，记一句"为何重新分析"。首次生成则无需履历表头。

---

## 输入

| 输入 | 必填 | 说明 |
|------|------|------|
| **票据 ID** | **是** | `{{TICKET_PREFIX}}-xxxx`，用于定位 PRD 和 `_progress.md` |
| PRD 文档路径 | 否 | 默认从 `{{REQUIREMENTS_DIR}}/{ticket-id}/` 下自动查找 |

---

## 输出

```
{Workbench_root}/
└── 00_impact_analysis.md    # 本 Workbench 影响分析文档
```

其中 `{Workbench_root}` 由 requirement-kickoff 写入 `_progress.md` 跨 Workbench 信息表的"输出目录"列。
同时更新 `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md`（本 Workbench 状态 → ✅，并登记本 Workbench「待澄清问题」条数）。

---

## Guardrails

| 规则 | 触发条件 | 执行动作 |
|------|---------|---------|
| **local_profile 缺失** | `local_profile.yaml` 不存在 | BLOCK：必须先创建并显式声明 role |
| **角色不匹配** | role 是协作角色（如 ba / dev-lead 等非 Workbench 工程师角色） | BLOCK：impact-analyzer 由 Workbench 工程师执行；BA/Dev Lead 应运行 requirement-kickoff / requirements-analyst |
| **code_root 未配置** | `code_roots.{role}` 为空或路径不存在 | BLOCK：提示在 local_profile.yaml 中配置本 Workbench code_root |
| **_progress.md 缺失** | 票据对应 `_progress.md` 不存在 | BLOCK：requirement-kickoff 尚未执行，请 Dev Lead/BA 先运行 |
| **本 Workbench 未分配** | `_progress.md` Workbench 分配表中无本人 role 对应的 Workbench | WARN：本 Workbench 可能不在此需求范围内，提示确认后再继续 |
| **大文件禁止全量加载** | 代码文件超过 300 行 | 使用 grep/head 定位关键方法，不全量读取 |
| **禁止推断** | 无法从代码/文档直接确认的影响 | 标注 `⚠️ 待确认：{具体问题}`，不猜测 |

---

## 执行流程

### 前置条件检查

1. 读取 `local_profile.yaml`：
    - 不存在 → BLOCK
    - 获取 `role`（须为 Workbench 工程师角色）和 `code_roots.{role}`（必须配置且目录存在）
    - **加载 `references/{role}.md`**（本 Workbench 代码定位参考）
2. 读取 `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md`：
    - 不存在 → BLOCK（requirement-kickoff 未执行）
    - 存在 → 确认本 Workbench 在分配表中且状态为 `⏸️ 待分析`（若已 ✅ 提示"是否重新分析？"）
    - 从"跨 Workbench 信息"表读取本 Workbench `输出目录`（即 `Workbench_root`），不存在则自动创建
3. 读取 PRD（自动查找或用户指定路径）
4. 读取 `project-memory/project_glossary.md` + `architectural_constraints.md`
5. **（按需）读取 `solution-design.md`（HLD）**：
    - 若 `_progress.md` 中"解决方案设计（HLD）"状态为 ✅ → **加载**，作为本次影响分析的架构方向上下文，
      重点读三处：**跨 Workbench 数据契约**、**对本 Workbench 的输入约束**、**未验证假设清单**
      （当前 HLD 中通常为 §4/§5/§8，但**以标题语义为准定位**——节号对不上时按标题找，不因节号漂移而漏读或读错节）。
      **影响分析须在 HLD 确定的架构方向下进行，不独立假设组件选型**（例：HLD 已定 Kafka 异步，则不分析同步路径的影响）。
      **§8 中涉及本 Workbench 的"假设未验证"项须逐条码级验证**：与代码现状一致则确认，冲突则按下方 Step 2 标注并回流「待澄清问题」。
    - 若不存在（状态非 ✅）→ 跳过，按存量 CR 模式正常执行。
6. 读取本 Workbench 代码仓库的 `AGENTS.md`（从 `code_roots.{role}` + 仓库名拼接路径）。**优先级 1=AGENTS.md；缺失则回退读同路径 `CLAUDE.md`（优先级 2）；两者皆无 → BLOCK，提示用户先在该仓库补充 agent 文档后重跑，不得凭空探索代码**

---

### Step 1：定位本 Workbench 相关代码

按 `references/{role}.md` 的「代码定位策略」执行本 Workbench 的 grep/find/head 命令，定位与 PRD 各用户故事相关的现有模块/文件/函数。

**决策原则**：
- 找到相关文件 → 用 head/grep 读取关键逻辑，**不全量加载**
- 无法确认的规则 → 标注 `⚠️ 待确认`

---

### Step 2：分析影响范围

> 若已加载 HLD，影响分析须在 §5 本 Workbench 约束和 §4 跨 Workbench 契约范围内展开，不另行假设架构方向。
> 若 HLD 与 PRD 矛盾，标注 `⚠️ HLD 与 PRD 冲突：{具体内容}`。
> 若 HLD §8 未验证假设与代码现状冲突，标注 `⚠️ HLD 假设与代码现状冲突：{假设} vs {实际}`，并写入「待澄清问题」回流第二轮 Q&A。
>
> **§-契约提示（与 solution-designer 松耦合）**：本 Skill 向 HLD 取三类数——跨 Workbench 契约 / 本 Workbench 输入约束 / 未验证假设，当前 HLD 中分别落在 §4/§5/§8。
> **定位以标题语义为准，节号仅作提示**：solution-designer 重排节号不应让本处静默失效。真正的消费契约是这三类**语义**的存在与含义,而非裸节号。
> 仅当某类语义在 HLD 中整体缺失（而非换号）时，才需与 solution-designer 对齐（对称约束见 solution-designer Step 6）。

**前置：PRD 场景枚举清单**

在分析任何模块之前，先从每条用户故事的验收标准中提取**所有场景分支**（正常路径 + 边界路径 + 明文列举的 if/else 条件），形成检查清单。分析文档中**每个场景必须对应一个代码路径引用**；未找到代码路径的场景标注 `⚠️ 场景未覆盖`——**不允许只分析"最显眼的路径"而跳过其余**。

对每个相关模块，分析：

1. **现有实现**：当前做什么（直接引用代码，不推断）
2. **变更影响**：PRD 要求（在 HLD 架构方向下）与现有实现的 delta
   - 新增：需要新建的文件/方法/字段
   - 修改：需要改动的现有逻辑（引用具体 `文件:行号`）
   - 不变：确认不受影响的部分
3. **架构约束命中**：对照 `architectural_constraints.md`，标注命中的约束
4. **技术风险**：可能的难点、副作用、兼容性问题
5. **对 requirements 的建议**：基于代码现状，补充 PRD 可能遗漏的边界条件

> **分析完成后，逐条过一遍文末「验证回路」**——尤其「方案可行性追踪」「新分支键唯一性」「共享逻辑完整性」三条事故经验规则，本步的 delta 结论须经它们校验后才下定论。

---

### Step 3：生成影响分析文档

写入 `{Workbench_root}/00_impact_analysis.md`：

```markdown
# {{TICKET_PREFIX}}-xxxx 影响分析 — {Workbench} Workbench

**分析人**：{name}（{role}）
**分析日期**：{YYYY-MM-DD}
**代码仓库**：{code_roots.role}

---

## 涉及现有模块

| 模块/文件 | 路径 | 职责说明 |
|---------|------|---------|
| {ModuleName} | {file:line} | {说明} |

## 变更影响评估

### 新增
- {需要新建的内容，引用 PRD 用户故事编号}

### 修改
- `{文件路径}:{行号}` — {当前逻辑} → {需要改成什么}

### 不变
- {确认不受影响的模块}

## 架构约束命中

| 约束 | 命中点 | 处理建议 |
|------|-------|---------|
| {约束编号} | {具体位置} | {建议} |

## 技术风险

| 风险 | 严重性 | 说明 |
|------|:-----:|------|
| {风险描述} | 高/中/低 | {说明} |

## 对 requirements 的建议补充

> 基于代码现状，PRD 可能未覆盖以下边界条件：
- {建议}

## 待澄清问题（回流第二轮 Q&A）

> 记录看了代码才暴露、且无法从代码/文档自行消解的技术歧义：可行性、存量约束、跨 Workbench 口径怀疑。
> 此 section 是 requirements-analyst **第二轮技术 Q&A 的唯一来源**——它汇总前会扫描此处。
> 跨 Workbench 口径怀疑也写这里：因 role 隔离你无法核实，建议回答人填 DL，由其在第二轮以跨 Workbench 视角裁决。
> （cross-workbench-reviewer 不读此 section，它只做五维比对。）
> 每条标注建议回答人：业务→BA / Workbench 内技术→DL / 跨 Workbench→DL。无则写"无"。

| 编号 | 待澄清问题（指向具体代码/场景） | 建议回答人 | 不同答案的影响 |
|------|------------------------------|-----------|--------------|
| Q-{role}-01 | ⚠️ {具体问题} | BA / DL | {不同答案导致的不同实现方向} |
```

---

### Step 4：更新进度日志并推送

1. 更新 `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md`：本 Workbench 状态改为 ✅，记录产出路径和完成时间，**在 Workbench 分配表登记本 Workbench「待澄清问题」条数**（供 requirements-analyst 判断第二轮 Q&A 工作量；无则记 0），贡献记录追加一行。
2. 📝 git push（让其他人看到进度，影响分析完成后驱动 requirements-analyst 汇总）。

---

## 人类门控

| 门控点 | 参与者 | 通过标准 |
|--------|-------|---------|
| 影响分析 review | Dev Lead（或下游 cross-workbench-reviewer） | 分析完整、⚠️ 待确认项已列出、无明显遗漏 |

> 发现重大风险或需求矛盾时，在对话中即时明确告知工程师，不等到 review。

---

## 验证回路

- **来源检查**：影响评估中的每条"修改"必须引用具体 `文件:行号`，不允许模糊描述
- **约束覆盖**：`architectural_constraints.md` 至少扫描一遍，命中的必须列出
- **推断检查**：所有无法直接从代码确认的内容必须标注 ⚠️
- **方案可行性追踪**：对每条实现建议，不止描述要改什么，还要在代码中追踪该改法的执行机制，确认它能产生预期结果。若只读了"入口处"而未确认"执行路径末端"的实际行为，则不能下确定性结论，须标注 ⚠️
- **新分支/新路径键唯一性检查**：当在现有并行选择结构（分支、路由、条件分派等）中新增路径时，必须列举所有现有同级路径，说明新路径的主键/标识集合与每条现有路径不重叠；若允许重叠，须说明由何处负责去重
- **共享逻辑完整性检查**：当修改某个条件判断或计算逻辑时，必须在当前文件/模块内搜索该逻辑的**所有出现位置**，说明每处是否需要同步修改；只改一处而遗漏其他出现位置，会导致同一模块内行为不一致

---

## 关键原则

1. **只分析本 Workbench**：role 决定访问范围，不读取其他 Workbench 的代码
2. **事实不推断**：无法确认的内容一律标注"待确认"
3. **引用代码**：每条影响评估必须有代码 `文件:行号` 引用，不允许凭记忆描述
4. **推送驱动协调**：完成后必须 git push，`_progress.md` 是团队协调信号
