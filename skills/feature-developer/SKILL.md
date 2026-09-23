---
name: feature-developer
description: |
  功能实现专家。基于技术设计文档（design.md + tasks.md），按 Gather-Act-Verify 循环实现代码变更：
  先收集上下文、再实现、再验证，最多两次失败后上报人类。按当前 Workbench（local_profile.yaml 的 role）
  加载对应开发参考，遵循架构约束、先写测试再写实现，支持从 _progress.md 断点续跑。

  当用户表达"开始写代码"、"实现这个功能"、"按设计写"、"按 tasks.md 做"、"开始开发"、
  "继续上次的任务"、"接着写"、"implement feature"、"写代码"时触发此 Skill。
---

# Feature Developer

基于设计文档，按 Gather-Act-Verify 循环实现功能代码。

> **本 Skill 的分层**：下文为**各 Workbench 通用**的循环、纪律与门控。凡涉及"本 Workbench 具体怎么定位/实现/验证"
> （find/grep 命令、实现顺序、分层职责、构建测试命令、Workbench 红线），一律见运行时加载的
> `references/{role}.md`。SKILL.md 不内联任何单个 Workbench 技术内容。

---

## Memory 契约

### 始终加载（每次执行前必读）
- `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md` — 票级进度日志（判断从哪个任务续跑）
- `references/{role}.md` — **本 Workbench 开发参考**（role 来自 local_profile.yaml；只加载本 Workbench 那一份）
- `{code_roots.{role}}/AGENTS.md` — 服务结构、模块划分、分层约定（**权威性高于 rules 工程规范和 Workbench 参考**）
- `project-memory/project_glossary.md` — 业务术语，确保代码命名与业务语义一致
- `project-memory/architectural_constraints.md` — 架构约束，实现时必须遵守

> **三层知识的优先级**：`AGENTS.md`（仓库级最细，运行时读）＞ `references/{role}.md`（Workbench 级 SKILL 参考）＞ `rules/{role}/`（团队工程规范）。冲突时以更细者为准。

### 按需加载
- `rules/{role}/coding.md`（及该 Workbench 存在的 `api.md` / `database.md`）— 团队工程规范。**按需自主加载（非强制）**：规范可能较大，不无条件全文载入；当本次实现涉及命名/分层/接口/库设计等其覆盖维度时，再加载相关文件的相关章节。文件不存在则跳过。
- `project-memory/adr_index.md` — 遇到架构选择时检查是否有相关 ADR
- `project-memory/adrs/{ADR-xxx}.md` — 需要完整决策内容时加载全文

### 执行后写入
- `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md` — 每个任务完成后更新
  - **变更重跑场景**：若本次由 requirement-change-router 路由触发，全部任务完成后勾掉 `_progress.md` 重跑清单中本 Skill 对应行（☐→☑）
- `{module_root}/05_task/changelog.md` — 每个任务完成后追加变更记录

> **变更重跑时的写入规范**：当本 Skill 因需求变更被重跑（由 requirement-change-router 路由），按「文档历史保留约定」**增量实现**——对照更新后的设计 delta，**只改受变更影响的代码**（增/改/删），保留与本次变更无关的既有实现，**不重写整个模块**。代码本身由代码仓库 git 追溯，无需 `_history/` 快照。`changelog.md` 维持 **append 语义**（它是历史账本，追加本次变更记录，不覆盖旧条目）。

---

## 输入

| 输入 | 必填 | 说明 |
|------|------|------|
| **tasks.md 路径** | **是** | 开发任务清单（architecture-advisor 产物） |
| **design.md 路径** | **是** | 技术设计文档 |
| 任务编号 | 否 | 从指定任务开始（默认从第一个未完成任务开始） |

---

## 输出

直接修改 `{code_roots.{role}}` 目录下的源码文件；同时在文档仓库产出：

```
{module_root}/05_task/
└── changelog.md    # 变更日志（每个任务完成后追加）
```

---

## Guardrails

| 规则 | 触发条件 | 执行动作 |
|------|---------|---------|
| **无设计文档禁止开始** | tasks.md 或 design.md 不存在 | BLOCK：提示需先运行 architecture-advisor |
| **架构约束违规** | 实现代码违反 architectural_constraints.md | BLOCK：说明违反的具体约束，要求用户决策 |
| **大文件禁止全量加载** | 文件超过 300 行 | BLOCK：必须使用 grep/head 定位目标方法，不得全量读取（全量加载会挤占上下文、引入无关代码噪声，降低定位与改动精度） |
| **测试覆盖率下降** | 修改后现有测试失败且未新增对应测试 | BLOCK：必须先修复测试再继续 |
| **连续失败超限** | 同一问题修复尝试超过 2 次仍失败 | BLOCK：停止尝试，将错误信息和已尝试方案告知用户，等待人类决策 |
| **高风险修改** | 修改涉及高风险逻辑（如金额计算、状态机核心、权限/租户路由等，具体清单见 `references/{role}.md` Workbench 红线） | WARN：标注 HIGH_RISK，完成后等待人类审查再继续 |
| **ADR 违规** | 实现方案与 ACCEPTED 状态 ADR 冲突 | BLOCK：说明冲突的 ADR 编号，要求决策 |
| **Workbench 模式混用** | 对目标服务使用了不属于本 Workbench 的开发/构建模式（语言、构建工具、分层范式不匹配） | BLOCK：本 Workbench 开发模式以 `references/{role}.md` 和目标仓库 AGENTS.md 为准，告知用户并要求确认后再继续 |

---

## 执行流程

### 前置条件检查

> 本节用 a/b/c… 编号，与下文 GATHER/ACT/VERIFY 的 Step 编号空间分离，避免混淆。

a. 读取 `local_profile.yaml`：
    - **不存在** → BLOCK：必须先创建并显式声明 role
    - 获取 `name`、`role`、`code_roots`；`code_roots.{role}` 必须配置且目录存在 → 否则 BLOCK
    - 代码操作路径全部使用 `{code_roots.{role}}`，不使用硬编码路径
    - **加载 `references/{role}.md`**（本 Workbench 开发参考）
b. 读取 `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md`（若存在）：找到上次完成的任务编号，从下一个续跑；不存在则从第一个任务开始
c. 读取 `{code_roots.{role}}/AGENTS.md`（服务结构、模块划分、分层模式；多子服务时读与 tasks.md 对应服务的 AGENTS.md）。**优先级 1=AGENTS.md；缺失则回退读同路径 `CLAUDE.md`（优先级 2）；两者皆无 → BLOCK，提示用户先在该仓库补充 agent 文档后重跑，不得凭空写代码**
d. **按需自主加载** `rules/{role}/coding.md`（及按 Workbench 存在的 `api.md`/`database.md`）——涉及命名/分层/实现规范时才加载，规范较大时只取相关章节，不强制全文；与 AGENTS.md 冲突时以 AGENTS.md 为准
e. 读取 `project-memory/architectural_constraints.md`
f. 读取 tasks.md（任务列表）和 design.md（设计意图与组件结构）
g. 确认从哪个任务开始

---

### 核心模式：Gather-Act-Verify 循环

每个任务执行一次完整循环：

```
┌─────────────────────────────┐
│  GATHER：收集上下文           │
│  用 grep/glob/head 定位相关  │
│  代码，不全量加载大文件        │
└────────────┬────────────────┘
             ↓
┌─────────────────────────────┐
│  ACT：实现变更               │
│  先写/更新测试，再写实现       │
│  遵循目标服务的分层约定        │
└────────────┬────────────────┘
             ↓
┌─────────────────────────────┐
│  VERIFY：验证结果            │
│  运行测试 → linter → 边界检查 │
│  失败最多重试 2 次            │
└─────────────────────────────┘
```

---

### Step 1：GATHER — 收集上下文

**目标**：精确定位需要修改/新建的代码，不加载无关文件。

按 `references/{role}.md` 的「GATHER 策略」执行本 Workbench 的 find/grep/head 定位命令，并对照其「收集清单」确认覆盖了本 Workbench 所有关键单元（入口、业务逻辑、数据访问、测试等）。

**决策原则（各 Workbench 通用）**：
- 找不到目标文件 → **新建**（遵循 AGENTS.md 的命名和路径约定）
- 找到但目标方法/函数不存在 → 在现有文件中**新增**
- 找到且已存在 → **修改**（先读懂原有逻辑再动手）

---

### Step 2：ACT — 实现变更

**测试先行（各 Workbench 通用）**：
- 有现有测试文件 → 先更新测试覆盖新功能，再写实现
- 无现有测试文件 → 新建测试，至少覆盖正向路径和主要边界条件

**实现规范**：按 `references/{role}.md` 的「ACT 实现顺序与分层职责」执行——遵循本 Workbench 从底层到上层的实现顺序，恪守各层职责边界（不越层放业务逻辑）。实现中持续对照 `architectural_constraints.md` 与本 Workbench 红线。

---

### Step 3：VERIFY — 验证结果

验证分两级：**必须通过**（失败则 BLOCK）和**尽力执行**（失败时区分原因再决策）。
两级各自的具体命令见 `references/{role}.md` 的「VERIFY 命令」（必须通过 = 编译/构建/语法；尽力执行 = 测试/lint）。

**完成判定清单**（每次 VERIFY 逐项核对）：
- **构建/测试命令**：按 `references/{role}.md` 「VERIFY 命令」执行两级验证（结果进入下方失败处理）
- **架构约束扫描**：本次改动不违反 `architectural_constraints.md` 任何约束、不冲突 ACCEPTED 状态 ADR
- **编码规范与分层**：命名、分层职责符合目标服务约定（以 AGENTS.md / Workbench 参考为准）
- **测试覆盖**：本次修改的每个核心业务方法都有对应测试
- **边界检查**：对照 `references/{role}.md` 「Workbench 红线/边界检查清单」逐项核对（如类型约束、必备字段、跨服务调用方式、空值/空集合返回约定等）

**失败处理**：

| 失败类型 | 处理方式 |
|---------|---------|
| 编译/构建/语法失败 | BLOCK：修复，最多重试 2 次；超限上报人类 |
| 测试失败（基础设施不可达：数据库/外部服务） | ⚠️ WARN：标注"基础设施限制，测试未验证"，继续执行 |
| 测试失败（代码逻辑错误） | BLOCK：修复，最多重试 2 次；超限上报人类 |
| lint 失败 | 修复后重试，最多 2 次；超限 WARN 并列出问题清单 |

---

### Step 4：任务完成确认

每个任务完成后：
1. 列出修改/新建的文件列表
2. 标注通过/失败的验证项
3. HIGH_RISK 修改等待人类明确确认后再继续下一任务
4. 正常任务询问"是否继续下一个任务"
5. 📝 **写入变更日志**：追加到 `{module_root}/05_task/changelog.md`（首次创建参照 `doc-templates/06_changelog_template.md`）
6. 📝 **写入进度日志**：更新 `_progress.md`，当前任务标记为 ✅，记录已修改文件；HIGH_RISK 任务标记为 ⏸️

**changelog.md 追加格式**：

```markdown
## Task-{N}：{任务标题}

**完成时间**：{YYYY-MM-DD}
**实现者**：{name}（{role}）

### 变更文件
| 文件路径 | 操作 | 说明 |
|---------|------|------|
| {path} | 新增/修改 | {说明} |

### 偏差说明
{与 design.md / tasks.md 的偏差；若无偏差则写"按设计实现，无偏差"}

### 遗留风险
{已知但未处理的风险；若无则写"无"}
```

---

## 人类门控

| 门控点 | 参与者 | 通过标准 |
|--------|-------|---------|
| HIGH_RISK 修改确认 | 开发/架构师 | 高风险逻辑（金额/状态机/权限路由等）已验证正确 |
| 连续失败上报 | 开发 | 人类提供修复方向或决定跳过 |
| ADR 冲突决策 | 架构师 | 确定修改实现还是更新 ADR |

---

## 关键原则

1. **小步前进**：每次只实现一个任务，验证通过后再进入下一个
2. **定位不加载**：用 grep/glob/head 定位代码，绝不全量加载 300 行以上的文件
3. **测试先行**：先写测试表达预期，再写实现满足测试
4. **约束优先**：`architectural_constraints.md` 中的约束是硬性边界，不可绕过
5. **上报不猜测**：失败超过 2 次立即上报，不靠猜测继续尝试
6. **模式匹配**：按目标服务选择正确的开发/分层模式（以 AGENTS.md 和 Workbench 参考为准），不混用其他 Workbench 模式
