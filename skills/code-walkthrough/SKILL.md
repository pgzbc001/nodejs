---
name: code-walkthrough
description: |
  代码走读讲解专家。在 feature-developer 完成代码实现后，以"讲师带读"的形式，
  基于本次需求的真实改动（changelog 登记 ∪ 当前 git 改动，含未提交），按调用链/数据流分段讲解代码，
  每段讲完暂停，等工程师确认无疑问后再进入下一段。

  目标是修复"AI 写代码导致人类心智模型偏低"的门控失效问题：
  让作为人类门控者的你真正理解 AI 实现了什么，并在走读中自然发现实现与设计的偏离。

  与 module-explorer 的区别：
  - module-explorer：通用现状探索，无票据上下文，产出文档，理解"既有代码做什么"
  - code-walkthrough：面向本票据的改动，交互式分段讲解，纯对话，理解"这次实现了什么、为什么这么写"

  按当前 Workbench（由 local_profile.yaml 的 role 决定）加载对应的分段范式（references/{role}.md）。

  当用户表达"带我走读代码"、"讲解一下这次实现"、"code walkthrough"、"带我读代码"、
  "我想理解这次改动"、"走读"、"讲讲这个需求的代码"时触发此 Skill。
---

# Code Walkthrough

基于本次需求的真实改动，像讲师一样分段带读代码，逐段确认理解。

> **本 Skill 的分层**：下文为**各 Workbench 通用**的走读流程与纪律（diff 解析、走读地图、分段讲解、偏离检测）。
> 本 Workbench"按什么逻辑线分段"这一技术绑定内容，运行时从 `references/{role}.md` 加载。

---

## Memory 契约

### 始终加载（每次执行前必读）
- `local_profile.yaml` — 获取 role 和 code_roots（必须存在），确定走读哪个 Workbench 的代码
- `CLAUDE.md` — 全局架构约定、文档与代码目录定位
- `references/{role}.md` — **本 Workbench 分段范式**（role 来自 local_profile.yaml）
- `{code_roots.{role}}/AGENTS.md` — 服务结构、模块划分、分层约定（理解每段代码归属的依据）
- `project-memory/project_glossary.md` — 业务术语，讲解时用标准术语描述业务含义
- `project-memory/architectural_constraints.md` — 架构约束，走读时识别实现是否命中/违反约束

### 按需加载（提供"为什么"的上下文，不作为走读范围权威）
- `{module_root}/05_task/changelog.md` — feature-developer 登记的变更文件、偏差说明、遗留风险
- `{module_root}/04_design/design.md` — 技术设计，用于比对"实现是否偏离设计"
- `{module_root}/05_task/tasks.md` — 任务清单，理解改动的拆分意图
- `project-memory/adr_index.md` / `adrs/{ADR-xxx}.md` — 走读涉及架构决策时加载

### 执行后写入
- 无（纯对话讲解，不产出文档，不写 Memory，不写 `_progress.md`；走读进度在对话中维护）
  - **唯一例外——变更重跑场景**：若本次走读由 requirement-change-router 路由触发（编码后变更需重新走读），走读完成后勾掉 `_progress.md` 重跑清单中本 Skill 对应行（☐→☑），使失效链闭环

---

## 输入

| 输入 | 必填 | 说明 |
|------|------|------|
| **票据 ID** | **是** | `{{TICKET_PREFIX}}-xxxx`，用于定位本 Workbench `{module_root}` 下的 changelog/design 上下文 |
| diff 基线分支 | 否 | **不默认 main**。仅"改动已提交在特性分支"时才需要；缺省时按 上游分支/merge-base 自动探测，无法确定则询问 |
| 走读范围 | 否 | 默认全量改动；可指定只走某个模块/某几个文件 |
| 起始分段 | 否 | 续读时指定从第几段开始（默认从第一段） |

---

## 输出

**无文件产物**。本 Skill 是交互式对话讲解：

- 启动时在对话中给出一份"走读地图"（分段清单 + 建议顺序）
- 之后逐段讲解，每段结束暂停等待确认
- 全部讲完后做一次整体串讲收尾

> 走读**文件范围** = `changelog.md` 登记文件 ∪ 当前 git 改动（含未提交）；每个文件**改了什么**由 git 自适应取 diff；`design.md` 提供"为什么这么改"的上下文。不依赖仓库干净度，不依赖固定基线。

---

## Guardrails

| 规则 | 触发条件 | 执行动作 |
|------|---------|---------|
| **local_profile 缺失** | `local_profile.yaml` 不存在 | BLOCK：必须先创建并显式声明 role |
| **角色无代码 Workbench** | role 是无代码权限的协作角色（如 ba） | BLOCK：该角色无代码 Workbench，无法走读；dev-lead 需显式指定走读哪个 Workbench |
| **无改动可走读** | changelog 文件清单为空 **且** working tree 与已提交 diff 均无改动 | BLOCK：未发现本票据的代码改动，确认票据/分支/基线是否正确 |
| **基线不可写死 main** | 需要 base 比较但用户未指定 | BLOCK：不得默认 main；先按 上游分支/merge-base 探测，仍无法确定则询问用户基线 |
| **禁止一次性倾倒** | 倾向于把所有改动一口气讲完 | BLOCK：必须分段讲解，每段讲完暂停等确认，禁止单条消息讲完整次实现 |
| **未确认禁止推进** | 用户尚未确认本段就想进入下一段 | BLOCK：必须等用户明确"无疑问"后才进入下一段；用户提问则就地解答 |
| **每段必须点出关键决策/风险** | 某段只是逐行复述代码，无"为什么/边界/风险" | BLOCK：每段至少显性点出本段的关键决策点或风险点，否则不算讲完（防止退化成"念代码"） |
| **实现偏离设计必须显性指出** | 走读发现实现与 design.md 不一致 | WARN：明确标注"⚠️ 实现与设计偏离：{点}"，交由人类判断是设计过时还是实现有误 |
| **禁止逐行念 trivial 代码** | 对 getter/setter、样板代码逐行讲解 | WARN：跳过样板，token 花在非显而易见的业务逻辑、边界、跨切面关注点上 |
| **按段加载，禁止全量** | 一次性把所有改动文件读进上下文 | WARN：每段按需 grep/head 加载本段文件（JIT），不全量加载 |

---

## 执行流程

### 前置条件检查

-1. 读取 `local_profile.yaml`：
    - **不存在** → BLOCK：必须先创建 local_profile.yaml 并显式声明 role
    - 获取 `name`、`role`、`code_roots`
    - role 为无代码 Workbench 的协作角色（如 ba）→ BLOCK
    - role=dev-lead → 要求用户显式指定走读哪个 Workbench，据此选 `code_roots.{Workbench}`
    - 其他 role → 走读本 Workbench，校验 `code_roots.{role}` 存在，否则 BLOCK
    - **加载 `references/{role}.md`**（本 Workbench 分段范式）
0. 确认票据 ID，定位本 Workbench `{module_root}`（Workbench 文档根目录，从 project-config.yaml Workbench `doc_root` 或 `_progress.md` 跨 Workbench 信息表得到）。
   > 注意：代码仓库是独立 git 仓库，git 命令在 `{code_roots.{Workbench}}` 下执行，不是文档仓库。
1. **解析改动范围（多源，自适应 git 状态，不假设干净仓库 / 不写死基线）**：

   **1a. 票据范围锚点** — 读 `{module_root}/05_task/changelog.md`，得到本票据登记的改动文件清单 `F_log`（与提交状态、基线无关）。changelog 缺失则跳过，仅靠 git 探测。

   **1b. 探测 git 状态**（`-C {code_roots.{Workbench}}`）：
   ```bash
   git -C {code_root} branch --show-current
   git -C {code_root} status --porcelain        # 有无未提交改动（暂存/未暂存/未跟踪）
   ```

   **1c. 收集 git 改动文件 `F_git`**，按状态取：
   - 未提交改动（最常见，feature-developer 刚写完未提交）：
     ```bash
     git -C {code_root} diff HEAD --name-only          # 已跟踪文件的改动
     git -C {code_root} ls-files --others --exclude-standard   # 未跟踪的新文件
     ```
     —— **此路径无需任何基线**。
   - 已提交在特性分支（working tree 干净但有本票据的提交）：需要基线 `{base}`，按优先级解析：
     1. 用户显式指定 → 用之
     2. 上游分支：`git -C {code_root} rev-parse --abbrev-ref --symbolic-full-name @{u}`
     3. 仍无法确定 → **询问用户基线，不默认 main**
     ```bash
     git -C {code_root} diff {base}...HEAD --name-only
     ```
   - 混合状态：上述两者并集。

   **1d. 走读文件集 = `F_log ∪ F_git`**，并做交叉校验并标注：
   - 在 changelog 但 git 无改动 → 可能已提交到更早基线 / changelog 过时 → 标注 `⚠️ 待确认`
   - 在 git 但不在 changelog → 改动可能超出票据范围 / feature-developer 漏记 → 标注，问用户是否纳入走读
   - 文件集为空 → BLOCK
2. 加载 `CLAUDE.md`、`{code_roots}/AGENTS.md`、`project_glossary.md`、`architectural_constraints.md`。**代码仓库侧：优先级 1=`{code_roots}/AGENTS.md`；缺失则回退读同路径 `CLAUDE.md`（优先级 2）；两者皆无 → BLOCK，提示用户先在该仓库补充 agent 文档后重跑**
3. 加载"为什么"上下文：`{module_root}/04_design/design.md`（若存在；changelog 已在 1a 读取）
4. 确认走读范围（全量改动 / 指定模块）和起始分段

---

### Step 1：构建走读地图（分段规划）

把 Step 1d 得到的走读文件集，按本 Workbench 代码逻辑分成若干段（**不按文件字母序，按调用链/数据流**），产出一份"走读地图"。

**分段范式**：按 `references/{role}.md` 的「本 Workbench 分段逻辑线」组织（如调用链 / 数据流 / 消费链）；多个独立单元先聚类，再在单元内按该逻辑线分段。

**走读地图格式**（在对话中输出，不写文件）：

```
本次 {ticket-id} 改动共 {N} 个文件，我建议分 {M} 段走读：

第 1 段：{段标题}
  - 涉及文件：{file}（{改了什么}）
  - 本段要点：{关键逻辑、与 design 契约的对应}

第 2 段：{...}

走读顺序按 {本 Workbench 逻辑线} 组织。这个分段你认可吗？要调整顺序或合并/拆分某段吗？
```

确认走读地图后进入 Step 2。

---

### Step 2：分段讲解循环（核心）

对走读地图中的每一段，执行一个完整的"讲解 → 确认"回合：

**a. JIT 加载本段文件**
- 取本段每个文件的实际改动（自适应 git 状态）：未提交用 `git -C {code_root} diff HEAD -- {file}`，已提交用 `git -C {code_root} diff {base}...HEAD -- {file}`，未跟踪新文件直接读全文并标注"新增文件"。
- 配合 `grep`/`head` 定位本段相关方法/代码块，只读本段需要的部分，不全量加载，不预读后续段。

**b. 讲解本段**（结构固定，确保不退化成念代码）：
1. **定位**：这段在整体调用链/数据流里处于什么位置，承接上一段什么、为下一段准备什么
2. **核心逻辑**：这段做什么、数据怎么流、关键方法/算子的职责（业务语言为主，用 glossary 术语）
3. **关键决策点 / 风险点**（**硬约束，每段必有**）：
   - 为什么这么写（而不是另一种写法）
   - 边界条件如何处理（空值、异常、并发、幂等、隔离、加密等本 Workbench 关注点）
   - 与 `design.md` 是否一致；若偏离 → 标注 `⚠️ 实现与设计偏离`
   - 是否命中/违反 `architectural_constraints.md`
4. **引导阅读**：给出 `文件路径:行号`，请你对照看真实代码——**AI 是讲师，代码由你亲自读**，不替你通读

**c. 暂停确认**（人类门控，贯穿式）：
- 结尾固定问："这段到这里，有疑问吗？没有的话我们进入第 {n+1} 段。"
- 用户**提问** → 就地解答，可下钻更深，直到用户表示理解
- 用户**确认无疑问** → 进入下一段
- 用户**未确认** → 禁止推进（Guardrail）

逐段重复，直到所有段讲完。

> **续读**：若对话中断后重新触发，重新计算 diff 并复述走读地图，询问"上次走到第几段，从哪段继续"，从指定段恢复（进度在对话中维护，不落文件）。

---

### Step 3：整体串讲收尾

所有段讲完后，做一次收尾（仍为纯对话）：

1. **串讲全貌**：把各段串成一条完整的调用链/数据流，让你建立整体心智模型
2. **重点提示**：点出本次实现里最值得你记住的 2-3 个点（最易出错处、最关键的业务规则、最需要后续维护注意的约束）
3. **遗留与偏离汇总**：把走读中标注的所有 `⚠️ 实现与设计偏离` / 约束命中点汇总复述一遍，提示是否需要走 code-reviewer 或回头修订 design
4. 结束，不产出文档

---

## 人类门控

| 门控点 | 参与者 | 通过标准 |
|--------|-------|---------|
| 走读地图确认 | 工程师本人 | 分段与顺序认可后才开始讲解 |
| 每段确认（贯穿式） | 工程师本人 | 用户明确"无疑问"才进入下一段；未确认即阻断推进 |

---

## 验证回路

- **实现与设计一致性**：每段讲解时比对 `design.md`，发现偏离显性标注 `⚠️`，由人类判断处理（人类侧 review 的核心价值）
- **约束合规**：走读中发现实现违反 `architectural_constraints.md`，当段指出
- **术语一致性**：讲解使用 `project_glossary.md` 标准术语，发现代码命名与业务术语不一致时标注

---

## 关键原则

1. **分段讲解，逐段确认**：永不一次性倾倒；每段是一个独立的"讲解→确认"回合
2. **你读我讲**：AI 给文件:行号并讲解，真实代码由你亲自对照阅读，AI 不替你通读
3. **讲非显而易见的**：跳过样板代码，token 集中在业务逻辑、边界条件、"为什么这么写"
4. **每段必有决策点/风险点**：防止退化成"念代码"，这是讲解区别于朗读的核心
5. **改动来源多源、自适应仓库状态**：走读文件集 = changelog 登记 ∪ 当前 git 改动（含未提交）；内容由 git 按提交状态自适应取 diff；不假设仓库干净，不写死基线（基线无法确定就问，绝不默认 main）
6. **顺逻辑而非顺文件**：按本 Workbench 逻辑线（调用链 / 数据流 / 消费链）组织，而非文件字母序
7. **偏离即显性**：实现与设计不一致是走读最有价值的产出之一，必须主动指出而非掩盖
