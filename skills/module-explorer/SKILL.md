---
name: module-explorer
description: |
  模块探索专家。探索现有代码库或文档库中的模块，产出以业务逻辑为主线的现状分析文档。
  让开发者在修改代码或设计新功能之前，快速理解"这个功能做什么、为什么这样做、有哪些规则"。

  与 impact-analyzer 的区别：
  - module-explorer：通用模块探索，无票据上下文，理解"现有代码做什么"
  - impact-analyzer：面向特定需求，分析"这个需求对我的 Workbench 要改什么"

  target-first：由**探索目标**推断所属 Workbench 并加载对应探索参考（references/{Workbench}.md）；
  local_profile.yaml 的 role 仅作推断初值、不绑定，可手动指定覆盖。支持任意角色使用。

  当用户表达"帮我理解这个模块"、"分析现有代码"、"看看这个功能怎么实现的"、
  "探索模块"、"现状分析"、"module explore"时触发此 Skill。
---

# Module Explorer

产出以业务逻辑为主线、技术细节为配套的现状分析文档。

> **本 Skill 的分层**：下文为**各 Workbench 通用**的探索方法（深度优先、业务优先、产出结构）。
> 本 Workbench"优先探索什么、用什么命令定位"这类技术绑定内容，运行时从 `references/{推断 Workbench}.md` 加载。

---

## Memory 契约

### 始终加载（每次执行前必读）
- `project-memory/project_glossary.md` — 项目术语表
- `project-memory/architectural_constraints.md` — 架构约束，帮助识别代码是否符合架构规范
- `CLAUDE.md` — 项目结构与代码目录位置（target-first 推断探索目标所属 Workbench 的依据）
- `references/{推断 Workbench}.md` — **本 Workbench 探索参考**（Workbench 由探索目标推断，见前置条件 Step 2）
- `{code_roots.{推断 Workbench}/AGENTS.md` — 服务结构、模块划分、分层约定（**权威性高于 Workbench 参考与 rules 规范**）。
  **资源回退链（唯一事实源，下游各步引用此链不复述）**：
  `{code_roots.{推断 Workbench}/AGENTS.md`（优先级 1）→ 同路径 `CLAUDE.md`（优先级 2）→ 该 Workbench 文档产物 `{Workbench_root}/04_design/` 等（优先级 3）→ **全部缺失 → BLOCK，不凭空探索**。
  前两级有 = 代码探索；仅第 3 级有 = 降级文档探索。多子服务时读与目标模块最近的 AGENTS.md。

### 按需加载
- `project-memory/adr_index.md` — 发现代码中的重要设计决策时，核对是否有对应 ADR

### 执行后写入
- 无（探索结果不写入 Memory；发现与 ADR 冲突的代码，在文档中标注，由人类决策）

> **本 skill 无票据上下文、无状态**：不读写 `_progress.md`。"待确认事项"为**对话级门控**——
> 产出文档里的 ⚠️ 项由领域专家在对话中答复、据此补充文档即可，不依赖跨会话进度勾稽。

---

## 输入

| 输入 | 必填 | 说明 |
|------|------|------|
| 目标模块/功能描述 | **是** | 用户指定要探索的模块名、功能名、或票据编号（target-first 解析起点） |
| 探索 Workbench | 否 | 默认由**探索目标自动推断**所属 Workbench；`local_profile.yaml` 的 `role` 仅作推断初值；可手动指定覆盖 |

---

## 输出

```
{{MODULE_EXPLORATION_DIR}}/
└── {module-name}-{Workbench}.md     # 业务现状分析文档（{Workbench} = 探索目标推断出的 Workbench）
```

**只写文件，不在对话中输出完整内容**（避免消耗大量上下文）。

---

## Guardrails

| 规则 | 触发条件 | 执行动作 |
|------|---------|---------|
| **禁止推断** | 无法从代码/文档直接确认的规则 | 标注"⚠️ 待确认：{具体问题}"，不猜测 |
| **只写文件** | 完成后倾向于直接输出内容 | BLOCK：必须写入文件，不在对话中大段输出 |
| **深度优先** | 探索范围过宽导致分析浮于表面 | WARN：提示"建议聚焦 2-3 条核心流程深入分析，而非列举所有文件名" |
| **业务优先** | 输出大量技术细节但业务逻辑不清晰 | WARN：技术细节作为附录，主体必须是业务规则和流程 |
| **无锚点禁止凭空探索** | 探索目标对应的代码锚点（AGENTS/CLAUDE）与文档锚点（`04_design/` 等）**全部缺失**（资源回退链触底） | BLOCK：提示先补充 agent 文档或设计文档后重跑，不得凭空编造现状 |

---

## 执行流程

### 前置条件检查

> **解析方向：target-first。** 先认"探索什么"，再由探索目标反推该加载哪个 Workbench 的 reference 与哪套代码——
> 不绑定 `local_profile.role`（它只作推断初值）。

1. **确认探索目标**：用户指定的模块名/功能名/票据编号——target-first 解析的起点。
2. **推断所属 Workbench 并加载参考**：读 `CLAUDE.md`（项目结构、目录/仓库映射），由探索目标推断其所属 Workbench；
   `local_profile.yaml` 的 `role` 仅作推断初值、**不绑定**；用户显式指定探索 Workbench 时以其为准。**加载 `references/{推断 Workbench}.md`**。
3. 读取 `local_profile.yaml`（获取 name、`code_roots`）。
4. **按资源回退链定位锚点**（链路定义见 Memory 契约，此处不复述）：
   - 有代码锚点（`{code_roots.{推断 Workbench}/AGENTS.md` → 同路径 `CLAUDE.md`）→ 走**代码探索**。
   - 无本 Workbench 代码访问 / 无 agent 文档，但有该 Workbench 文档产物（`04_design/` 等）→ **降级为文档探索**。
   - 回退链**全部缺失 → BLOCK**，提示先补充 agent/设计文档后重跑，不凭空探索。
5. 读取 `project-memory/project_glossary.md` + `architectural_constraints.md`。

---

### Step 1：定位模块入口

按 `references/{推断 Workbench}.md` 的「探索策略」（本 Workbench 优先探索目标 + 关注重点）和「定位命令」，结合 AGENTS.md 描述的模块划分，定位入口文件。

> 代码探索 vs 文档探索的分支由前置条件 Step 4 的资源回退链确定：有代码锚点 → 定位代码入口；
> 降级为文档探索时 → 定位文档仓库对应 Workbench 的设计文档（`{Workbench_root}/04_design/design.md`）。

---

### Step 2：深度分析核心流程（深度优先）

**选择 2-3 条最核心的业务流程**，逐条深入分析：
1. 正常路径：主流程是什么？数据如何流动？
2. 关键业务规则（优先从代码/文档中找原文）
3. 边界条件：空值怎么处理？特殊状态怎么处理？
4. 错误路径：哪些情况会失败？

**无法确认的规则**：标注 `⚠️ 待确认：{具体问题}`，不推断。

---

### Step 3：生成现状分析文档

写入 `{{MODULE_EXPLORATION_DIR}}/{module-name}-{Workbench}.md`：

```markdown
# {模块名} — {Workbench} 视角现状分析

## 模块概览
一句话描述：这个模块做什么、归属哪个服务、服务哪个业务 Workbench

## 核心业务规则
（列出从代码/文档中直接确认的业务规则，每条注明来源）

## 主要业务流程
（2-3 条核心流程，用 flowchart 或文字描述）

## 关键数据结构
（输入/输出的主要字段，业务含义说明）

## 已知边界和限制
（已确认的限制条件）

## 待确认事项
⚠️ （探索中无法确认的规则，供人类补充）

## 发现的潜在问题
（如代码与 ADR 不符、未处理的边界条件等，不自行修复，只记录）

## 参考文件
（探索过程中读取的文件路径列表）
```

---

## 人类门控

| 门控点 | 参与者 | 通过标准 |
|--------|-------|---------|
| 待确认事项确认 | 领域专家/开发 | 所有 ⚠️ 待确认项都有明确答案 |

---

## 验证回路

- **术语一致性**：文档中的业务术语必须与 `project_glossary.md` 一致，发现不一致的用语标注
- **架构合规检查**：探索中发现代码违反 `architectural_constraints.md` 的约束，在"发现的潜在问题"中记录

---

## 关键原则

1. **业务优先**：技术实现是配套，业务逻辑是主体
2. **深度优先**：搞透 2-3 条核心流程，远比浅析所有文件有价值
3. **事实不推断**：无法确认的规则一律标注"待确认"
4. **只写文件**：完成后写入文档，不在对话中大段输出
5. **引用来源**：每条业务规则标注来自哪个文件/哪个方法
