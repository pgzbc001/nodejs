---
name: code-reviewer
description: |
  代码审查专家。可在任意分支、PR、diff 或代码片段上独立执行结构化审查；
  若提供 Ticket 上下文，则升级为 Ticket-aware Review，并将报告写入对应 Workbench 目录。
  审查覆盖架构合规性、业务逻辑正确性、编码规范、安全性和测试覆盖率五个维度，
  区分阻断性问题（BLOCK）、待裁决问题（QUESTION）和建议性改进（SUGGEST）。

  按**被审查 Workbench** 加载对应的审查参考（references/{被审查 Workbench}.md），而非按当前角色；
  dev-lead 审查多个 Workbench 时分别加载，普通角色即本人 Workbench。

  当用户表达"review 代码"、"审查 PR"、"代码 review"、"code review"、
  "帮我看看代码"、"审查变更"时触发此 Skill。
---

# Code Reviewer

对 PR / 分支 / diff / 代码片段进行多维度结构化审查，产出可操作的 Review 报告。

> **本 Skill 的分层**：下文为**各 Workbench 通用**的审查维度、报告结构与纪律。本 Workbench"有哪些架构/分层/业务高风险/安全/测试检查项"
> 这类技术绑定内容，运行时从 `references/{被审查 Workbench}.md` 加载。
>
> **独立性原则**：本 Skill 不要求 `feature-developer`、`code-walkthrough`、`dev-self-test` 已执行。
> 这些产物若存在则作为证据加载；不存在时不阻断临时审查，只在报告中标注上下文限制。

> **占位符显式声明**（消除跨 Skill 命名不一致）：
> - `{Workbench_root}` — 本 Workbench 的**文档根目录**（来源：`project-config.yaml` 的 Workbench `doc_root`，或 `_progress.md` 跨 Workbench 信息表）。
>   它**等价于**其他 Skill（feature-developer / code-walkthrough / requirements-analyst 等）中的 `{module_root}`——指向同一目录树（其下含 `01_requirements/`、`04_design/`、`05_task/`、`10_review/` 等）。**本 Skill 内一律写作 `{Workbench_root}`**。
> - `{被审查 Workbench}` — 本次**审查对象所属的 Workbench**（可有多个），由审查范围归属决定，**不一定等于** `local_profile.yaml` 的 role（见 Phase 0/1）。

---

## Memory 契约

### 始终加载（每次执行前必读）
- `local_profile.yaml` — 获取 role 和 code_roots
- `project-memory/MEMORY.md` — Memory 索引；先读索引，再按 Review Scope 判断加载哪些 Memory 文件
- `references/{被审查 Workbench}.md` — **被审查 Workbench 的审查参考**（架构/分层/业务高风险/编码/安全/测试门槛）。按**审查对象所属 Workbench** 加载，**不按当前角色**；dev-lead 跨 Workbench 审查时分别加载每个被审查 Workbench 的参考，普通角色即本人 Workbench（`{被审查 Workbench}` 的确定见 Phase 0/1）

### Scope 驱动按需加载（不得全量加载 project-memory/）
- `{changed_file}` 最近的 `AGENTS.md`（优先级 1）/ `CLAUDE.md`（优先级 2）— 确认代码符合仓库/模块约定；PR/分支/本地 diff 审查必读。两者皆无 → BLOCK；纯代码片段审查无法定位仓库时标注上下文限制
- `project-memory/architectural_constraints.md` — diff 涉及架构边界、数据访问、权限、安全、跨 Workbench 调用、状态机、金额、租户隔离等高风险点时加载；违反即 BLOCK
- `project-memory/coding_standards.md` — diff 涉及新增类/方法/模块结构，或需要判断命名/分层/风格时加载
- `project-memory/project_glossary.md` — diff 涉及业务命名、DTO/API 字段、用户可见文案、核心领域对象时加载
- `project-memory/adr_index.md` — diff 涉及架构选型、存储、接口契约、跨 Workbench 调用、技术方案变更时加载；如命中再加载对应 ADR 全文
- `project-memory/security_checklist.md` — 若存在，且 diff 涉及鉴权、输入、日志、PII、租户隔离、外部请求时加载
- `project-memory/review_standards.md` — 若存在，加载用于复用历史审查规则；不存在则跳过
- `{Workbench_root}/01_requirements/requirements.md`、`{Workbench_root}/04_design/design.md`、`{Workbench_root}/05_task/tasks.md`、`{Workbench_root}/05_task/changelog.md` — **Ticket-aware Review** 时按存在情况加载，作为交付契约证据
- `code-walkthrough` 发现的「实现与设计偏离」清单 — 若用户提供，则作为 Review 输入证据

> **知识优先级**：`AGENTS.md`（仓库/模块级最细）＞ `references/{被审查 Workbench}.md`（Workbench 级审查参考）＞ `coding_standards.md`（项目级通用）。
> `architectural_constraints.md` 与 ACCEPTED ADR 是硬约束；Ticket 文档是本次交付契约。

### 执行后写入
- **Ticket-aware Review**：`{Workbench_root}/10_review/code_review.md`，并更新 `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md`
  - **变更重跑场景**：若本次由 requirement-change-router 路由触发，完成后勾掉 `_progress.md` 重跑清单中本 Skill 对应行（☐→☑）
- **Ad-hoc Review**：不写文件，仅在对话中输出报告

> **变更重跑时的写入规范**：`code_review.md` 属**累积目标态文档（一般产物）**——按「文档历史保留约定」**增量更新**：针对本次变更新增/修改的代码补充 review 结论，保留与本次变更无关的既有结论，标记变更及驱动 CR，并做一致性扫描，**不整篇重生成**。仅需履历表头，旧版靠 git 追溯，无需 `_history/` 快照。

---

## 输入

| 输入 | 必填 | 说明 |
|------|------|------|
| **Review 对象** | **是** | 文件/目录/模块路径、文件列表、代码片段，或用户指定的 PR 范围。**默认审查代码当前内容本身，不主动 git diff** |
| 对比基线 | 否 | 用户**显式要求**与某 commit/branch/tag 对比时提供；提供后进入 **Diff 对比模式**，仅审查变更集。未提供则不主动推断变更 |
| 关联票据 ID | 否 | `{{TICKET_PREFIX}}-xxxx`；提供后进入 Ticket-aware Review，加载对应 Ticket 文档并落盘报告 |
| 目标 Workbench/仓库 | 否 | dev-lead 多 Workbench 审查或 diff 横跨多仓库时用于确认审查范围 |
| 走读发现 | 否 | code-walkthrough 过程中发现的偏离/风险清单 |

---

## 输出

### Ticket-aware Review

当提供 Ticket ID，或 Review 对象能明确关联到 Ticket 目录时：

- 写入 `{Workbench_root}/10_review/code_review.md`
- 更新 `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md`
- 在对话中摘要报告结论和 BLOCK/QUESTION 数量

其中 `{Workbench_root}` 优先从 `_progress.md` 的跨 Workbench 信息表读取；若不存在但用户显式提供本 Workbench 目录，则使用用户提供路径。

### Ad-hoc Review

当没有 Ticket 上下文时：

- 不写文件
- 只在对话中输出结构化 Review 报告
- 报告头部必须标注：`Review 模式：Ad-hoc Review`、`持久化：否`、`限制：未绑定 Ticket，未检查完整需求/设计/任务覆盖`

### 报告格式

```markdown
# Code Review Report — {{TICKET_PREFIX}}-xxxx

## 1. Review 结论
- Review 模式：Ticket-aware Review / Ad-hoc Review
- 持久化：是 / 否
- 结论：PASS / PASS_WITH_SUGGESTIONS / NEEDS_DECISION / BLOCKED
- Reviewer：{name}（{role}）
- 日期：{YYYY-MM-DD}
- 上下文限制：{若无 Ticket 或缺少某证据，在此说明}

## 2. Review Scope
| 文件 | 变更类型 | 所属仓库/模块 | 对应 Task/REQ/AC | 风险等级 |
|------|----------|---------------|------------------|----------|

## 3. Evidence Pack
| 证据 | 路径 | 状态 | 说明 |
|------|------|------|------|

## 4. BLOCKING Issues（必须修复才能合并）

### BLOCK-1：{问题标题}
**位置**：{file}:{line}
**证据**：{代码/设计/约束/ADR 引用}
**问题**：{具体描述}
**影响**：{为什么阻断}
**修复建议**：{具体修复方法}
**违反约束**：{约束 ID 或规范条目}

## 5. QUESTIONS（需要人类裁决）

### QUESTION-1：{问题标题}
**位置**：{file}:{line 或 文档路径}
**问题**：{无法从现有证据判断的点}
**需要谁裁决**：开发 / Dev Lead / BA / 安全负责人
**不同答案的影响**：{影响说明}

## 6. SUGGESTIONS（不影响合并，建议后续优化）

### SUGGEST-1：{建议标题}
**位置**：{file}:{line}
**现状**：{当前写法}
**建议**：{改进方向}

## 7. 测试与验证证据
| 项目 | 状态 | 说明 |
|------|------|------|

## 8. Review Standards 候选沉淀
{本次发现的可复用审查规则草稿；若无则写"无"}

## 9. 正向反馈
{值得肯定的实现点，如有}
```

---

## Guardrails

| 规则 | 触发条件 | 执行动作 |
|------|---------|---------|
| **local_profile 缺失** | `local_profile.yaml` 不存在 | BLOCK：必须先创建并声明 role |
| **默认不主动 git 对比** | 用户未显式要求与某 commit/branch/tag 对比 | 审查代码**当前内容本身**，不自行运行 `git diff`/`git log` 等推断变更集；确需基线时向用户澄清，不擅自选定 |
| **Review 阶段只读** | 用户要求在本 Skill 内直接修改代码、补测试、提交 commit | BLOCK：本 Skill 只审查不修复；修复请切换到 feature-developer 或 bug-fixer |
| **无代码证据的 BLOCK** | 给出阻断结论但无具体文件:行号证据或明确文档/约束证据 | 降为 QUESTION/SUGGEST 或补充证据 |
| **大文件禁止全量加载** | 文件超过 300 行 | 使用 grep/head 定位目标方法，不全量加载 |
| **PR/分支审查缺 agent 文档** | 变更文件所属仓库/模块无 `AGENTS.md` 且无 `CLAUDE.md` | BLOCK：提示先补充 agent 文档；不得凭空判断仓库分层 |
| **Ticket-aware Review 缺 _progress.md** | 提供 Ticket ID 但找不到对应 `_progress.md` | WARN：降级为 Ticket-light Review；可读取用户提供的 Ticket 文档，但不更新进度日志 |
| **违反硬约束/ACCEPTED ADR** | 命中 `architectural_constraints.md` BLOCK 级约束或 ACCEPTED ADR 冲突 | BLOCK：引用约束/ADR 证据 |
| **高风险变更无测试证据** | 变更命中本 Workbench 高风险模式，但无对应测试或验证说明 | BLOCK（Ticket-aware）/ QUESTION（Ad-hoc，若无法看到完整测试范围） |
| **无法判断业务正确性** | 缺少 requirements/design 等业务上下文 | 不得伪造结论；输出 QUESTION 或上下文限制说明 |

---

## 执行流程

### Phase 0：前置条件与 Review 模式识别

1. 读取 `local_profile.yaml`：
   - 不存在 → BLOCK
   - 获取 `name`、`role`、`code_roots`
   - 确定 `{被审查 Workbench}`：普通角色 = 本人 Workbench（只能审查本域）；`dev-lead` = **审查对象所属 Workbench**，可为多个（最终归属在 Phase 1 按审查范围确认）
2. 读取 `project-memory/MEMORY.md`（只读索引，不展开全部 Memory）
3. 加载 `references/{被审查 Workbench}.md`（被审查 Workbench 的审查参考）；`dev-lead` 审查多个 Workbench 时**分别加载每个被审查 Workbench 的参考**，**不按当前角色加载**。若被审查 Workbench 需在 Phase 1 归属明确后才能确定，则在 Phase 1 补载
4. 判断 Review 模式：
   - **Ticket-aware Review**：用户提供 Ticket ID，或 Review 对象能明确关联到 Ticket 目录
   - **Ad-hoc Review**：只有代码范围（文件/目录/模块/片段或 PR），无法关联 Ticket
5. 声明本次模式与限制：
   - Ticket-aware：将尝试加载 Ticket 文档并落盘报告
   - Ad-hoc：不落盘；不能完整判断需求/设计/任务覆盖

---

### Phase 1：Review Scope 建模

界定本次审查的代码范围。**默认审查"代码本身"，不主动执行 git 推断变更集**：

1. 确定审查范围 = 用户给定的代码（文件 / 目录 / 模块 / 代码片段，或用户指定的 PR 范围）。**默认审查这些代码的当前内容**，不自行运行 `git diff`/`git log` 推断变更集
   - **仅当用户显式要求**与某 commit/branch/tag/基线对比时，才进入 **Diff 对比模式**：基线由用户指定，按变更集（新增/修改/删除/重命名）审查；用户未指定基线则向其澄清，不擅自选定
2. 识别审查范围内文件所属的仓库/模块，据此确认 `{被审查 Workbench}`（dev-lead 多 Workbench 时可有多个），并按需补载对应 `references/{被审查 Workbench}.md`
3. 为每个文件查找最近的 `AGENTS.md`；缺失则回退 `CLAUDE.md`；两者皆无 → BLOCK。纯代码片段无法定位仓库时，记录"无法定位仓库 agent 文档"，只做片段级审查
4. 初步识别风险等级：
   - 高风险：权限/鉴权、金额/精度、状态机、数据同步、批处理/回刷、PII、租户隔离、跨 Workbench 调用、持久化模型变更
   - 中风险：核心业务逻辑、公共组件、接口契约、错误处理
   - 低风险：文案、样式、局部重构、测试辅助
5. 输出内部 Review Scope 表（报告中保留）

---

### Phase 2：Evidence Pack 加载（索引驱动）

1. 根据 Review Scope 和 `MEMORY.md` 索引，按需加载 Memory 文件：
   - 命中架构/安全/跨 Workbench/状态/金额/租户等高风险 → 加载 `architectural_constraints.md`
   - 涉及架构选型/存储/接口契约/跨 Workbench 调用 → 加载 `adr_index.md`，命中后加载 ADR 全文
   - 涉及命名/分层/新增模块 → 加载 `coding_standards.md`
   - 涉及业务对象/API 字段/用户可见文案 → 加载 `project_glossary.md`
   - 涉及安全攻击面 → 加载 `security_checklist.md`（若存在）
   - `review_standards.md` 若存在则加载，用于复用历史审查规则
2. Ticket-aware Review 额外加载：
   - `_progress.md`（若存在）
   - `requirements.md`（若存在）
   - `design.md`（若存在）
   - `tasks.md`（若存在）
   - `changelog.md`（若存在）
3. 缺失证据不一概 BLOCK；按模式记录限制：
   - Ticket-aware 缺核心文档 → 在 Evidence Pack 标 WARN，并影响业务/设计判断强度
   - Ad-hoc 缺 Ticket 文档 → 标注上下文限制，不检查完整需求覆盖

---

### Phase 3：Review Map 构建

将代码变更挂到交付契约上：

- Ticket-aware：对每个 changed file，尝试关联 Task、design section、REQ/AC、changelog 条目
- Ad-hoc：只关联仓库/模块约定与本 Workbench 审查参考，不强制 Task/REQ 映射

可产生的问题：

| 问题 | Ticket-aware | Ad-hoc |
|------|--------------|--------|
| 变更无任务/设计来源 | QUESTION 或 BLOCK（高风险时 BLOCK） | 不适用 |
| Task 声称完成但 diff 未覆盖 | QUESTION | 不适用 |
| 实现偏离 design.md 且无偏差说明 | BLOCK / QUESTION | 不适用 |
| changelog 缺失关键变更 | SUGGEST / QUESTION（按团队规则） | 不适用 |

---

### Phase 4：五个审查维度

#### 维度 1：架构合规性
- 检查代码是否违反已加载的 `architectural_constraints.md` 相关约束
- 检查代码是否与 ACCEPTED 状态 ADR 冲突
- 检查分层是否符合 AGENTS.md 的分层约定（如各层职责边界是否越界）
- 逐项核对 `references/{被审查 Workbench}.md` 的「本 Workbench 架构/分层合规检查项」

#### 维度 2：业务逻辑正确性
- Ticket-aware：实现是否与 requirements.md 的验收标准（AC）一致；若缺 requirements.md，标注上下文限制
- Ad-hoc：只判断 diff 内可见逻辑，不声明完整业务正确性
- 边界条件处理：空值、空集合、边界数值
- 状态机转换是否覆盖所有合法和非法转换
- 逐项核对 `references/{被审查 Workbench}.md` 的「本 Workbench 业务逻辑高风险模式」

#### 维度 3：编码规范
- 命名是否符合已加载的 coding_standards.md 和 project_glossary.md
- 代码结构是否符合 AGENTS.md 的分层约定
- 注释质量：是否只在"为什么"非显而易见时写注释
- 逐项核对 `references/{被审查 Workbench}.md` 的「本 Workbench 编码规范重点」

#### 维度 4：安全性
- 逐项核对 `references/{被审查 Workbench}.md` 的「本 Workbench 安全检查项」
- 命中 `security_checklist.md`（若存在）的相关条目一并核对
- 无安全上下文时，不得宣称"安全已通过"，只能说明"未发现可见安全问题"

#### 维度 5：测试覆盖率
- 新增/修改的核心业务方法是否有对应测试
- 正向路径和主要边界条件是否均有测试用例
- 逐项核对 `references/{被审查 Workbench}.md` 的「本 Workbench 测试覆盖门槛」
- feature-developer / CI 未能运行测试时，检查是否有明确原因；基础设施失败不等于逻辑通过

---

### Phase 5：输出报告与进度回写

1. 汇总结论（**未裁决的 QUESTION 不得判为终态 PASS**）：
   - 有 BLOCK → `BLOCKED`
   - 无 BLOCK 但有未裁决 QUESTION → `NEEDS_DECISION`（待人类裁决；裁决结果可能反转为 BLOCK 或降级为 SUGGEST，裁决前不视为通过）
   - 无 BLOCK、无未裁决 QUESTION，但有 SUGGEST → `PASS_WITH_SUGGESTIONS`
   - 无 BLOCK / QUESTION / SUGGEST → `PASS`
2. Ticket-aware Review：
   - 写入 `{Workbench_root}/10_review/code_review.md`（必要时创建目录）
   - 若 `_progress.md` 存在，更新：
     - `BLOCKED`：`代码审查` 阶段标 `⏸️`，`代码审查 BLOCKING 处理` 门控标等待开发修复
     - `NEEDS_DECISION`：`代码审查` 阶段标 `⏸️`（待裁决），**不标 `✅`**；在报告中列明待裁决 QUESTION 及需谁裁决
     - `PASS` / `PASS_WITH_SUGGESTIONS`：`代码审查` 阶段标 `✅`，记录报告路径
   - 在对话中输出报告摘要
3. Ad-hoc Review：
   - 不写文件
   - 在对话中输出完整结构化报告，并标注上下文限制
4. 若发现可复用规则，输出 `Review Standards 候选沉淀` 草稿；不得自动写入正式 Memory，需人类确认后由 memory-curator 或人工维护

---

## 人类门控

| 门控点 | 参与者 | 通过标准 |
|--------|-------|---------|
| Review 结论确认 | 提交者 + Reviewer | BLOCK 问题全部修复或达成处理共识；QUESTION 已裁决；SUGGEST 已知悉 |

---

## 验证回路

- **Scope 完整性**：Review Scope 必须列出所有 changed files；无法解析的变更范围必须标注
- **索引驱动 Memory 加载**：先读 `MEMORY.md`，再按 Review Scope 加载相关 Memory；不得全量展开 project-memory
- **约束逐条扫描**：已加载的 `architectural_constraints.md` 中相关约束必须逐条核对，命中即 BLOCK
- **BLOCK 证据完整性**：每个 BLOCK 必须有 `文件:行号` 或明确文档/约束证据；无证据则降为 QUESTION/SUGGEST 或补证
- **设计一致性**：Ticket-aware 且有 design.md 时，实现偏离设计的点必须在报告中指出
- **测试覆盖核对**：变更涉及的核心业务方法，确认有对应测试或验证说明，否则列入报告

---

## 关键原则

1. **BLOCK 需要证据**：每个阻断问题必须附文件:行号或明确文档/约束证据
2. **独立审查**：Review 可在任意分支/PR/diff 上执行，不绑定 feature-developer 或 test 流程
3. **Ticket 是增强上下文**：有 Ticket 则落盘并检查交付契约；无 Ticket 则轻量审查并明确限制
4. **区分 BLOCK / QUESTION / SUGGEST**：违反硬约束/安全/高风险无测试 → BLOCK；证据不足需裁决 → QUESTION；风格/优化 → SUGGEST
5. **只读不修复**：本 Skill 不修改代码；修复由 feature-developer / bug-fixer 承担
6. **索引优先**：Memory 先读索引，按需加载，不把 project-memory 当全量上下文包
7. **正向反馈不遗漏**：好的实现值得指出，帮助团队形成良好习惯
