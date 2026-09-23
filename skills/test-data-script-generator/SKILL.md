---
name: test-data-script-generator
description: |
  测试数据脚本生成专家。基于 dev-self-test 产出的自测用例文档，分析每条用例的前置条件，
  按 Workbench 生成可直接执行的造数、清数脚本，并产出可直接执行的自测副本
  （{变量} 占位符已替换为实际造数数据值）。

  按当前 Workbench（由 local_profile.yaml 的 role 决定）加载对应造数参考（references/{role}.md）；
  无自主造数能力的纯消费 Workbench（如纯前端 Workbench）执行时 BLOCK，引导联系数据提供 Workbench。

  当用户表达"生成测试数据"、"造数"、"造数脚本"、"测试数据脚本"、"写造数 SQL"、
  "帮我准备测试数据"、"generate test data"时触发此 Skill。
---

# Test Data Script Generator

基于 dev-self-test 自测用例文档，生成造数/清数脚本，并输出已填入真实数据值的可执行自测副本。

> **本 Skill 的分层**：下文为**各 Workbench 通用**的流程、前缀纪律、两文件产出与 runnable 副本机制。
> 本 Workbench"各数据源用什么语法造数/验证/清数"这类技术绑定内容，运行时从 `references/{role}.md` 加载。

---

## Memory 契约

### 始终加载（每次执行前必读）
- `local_profile.yaml` — 获取 role，确定执行 Workbench
- `references/{role}.md` — **本 Workbench 造数参考**（含本 Workbench 是否自主造数、数据源清单、造数/验证/清数语法）
- `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md` — 票级进度日志（判断是否从断点续跑）
- `project-memory/project_glossary.md` — 确保数据设计使用正确的业务术语
- `project-memory/architectural_constraints.md` — 数据约束，确保造数符合业务规则

### 执行后写入
- `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md` — 造数脚本完成后更新（阶段状态 + 产出路径）
  - **变更重跑场景**：若本次由 requirement-change-router 路由触发，完成后勾掉 `_progress.md` 重跑清单中本 Skill 对应行（☐→☑）

> **变更重跑时的写入规范**：造数/清数脚本与可执行自测副本属**累积目标态文档（一般产物）**——按「文档历史保留约定」**增量更新**：针对变更后的用例补充/修改对应造数（增/改/删），保留与本次变更无关的既有脚本，标记变更及驱动 CR，并做一致性扫描（尤其检查清数脚本与造数脚本是否仍配对、占位符替换是否同步更新），**不整篇重生成**。仅需履历表头，旧版靠 git 追溯，无需 `_history/` 快照。

---

## 输入

| 输入 | 必填 | 来源 |
|------|------|------|
| **dev-self-test 用例文档** | **是** | dev-self-test 产物（`06_test/dev_self_test_{xxx}.md`） |
| Schema 设计文档 | 推荐 | architecture-advisor Step 2.1（了解字段约束和枚举值） |
| 接口/管道设计文档 | 按需 | architecture-advisor Step 2.2（bigdata 确认 Job 触发方式） |

---

## 输出

```
{module_root}/06_test/
├── test_data_scripts.md              # 造数+清数脚本
└── dev_self_test_{xxx}_runnable.md   # 可执行自测副本（{变量} 占位符已替换为实际值）
```

| 文档 | 内容 | 生命周期 |
|------|------|---------|
| `test_data_scripts.md` | 造数/清数脚本 + 变量映射表 | 随测试数据重建而重新生成 |
| `dev_self_test_{xxx}_runnable.md` | 原自测用例副本，`{变量:xxx}` 已替换为真实值，可直接执行 | 同上，绑定本次造数数据 |

> **原始 `dev_self_test_{xxx}.md` 不修改**，保留为可复用的测试设计模板。

---

## Guardrails

| 规则 | 触发条件 | 执行动作 |
|------|---------|---------|
| **local_profile 缺失** | `local_profile.yaml` 不存在 | BLOCK：必须先创建并声明 role |
| **纯消费 Workbench 禁止执行** | 本 Workbench `references/{role}.md` Workbench 画像标明"无自主造数能力"（如纯前端 Workbench） | BLOCK：本 Workbench 无需自己造数。请联系数据提供 Workbench（backend/data 等）在测试环境准备数据，数据就绪后将实际值填入 `{变量}` 占位符，或请数据 Workbench 运行本 Skill 并共享 `_runnable.md` |
| **无 dev-self-test 产物** | `dev_self_test_{xxx}.md` 不存在 | BLOCK：必须先运行 dev-self-test 生成自测用例 |
| **数据前缀格式错误** | 造数前缀不符合本 Workbench 约定的模块前缀格式 | BLOCK：前缀是清数精确性的基础，格式错误会有误删风险 |
| **清数脚本不幂等** | 清数脚本无 WHERE 条件限定 | BLOCK：必须使用精确的 WHERE 条件，支持重复执行 |
| **未解析占位符存在** | 生成 runnable 副本后仍有 `{变量:xxx}` 未被替换 | WARN：在 runnable 文档头部列出未解析占位符，提示手动填入 |

---

## 数据前缀规则（各 Workbench 通用原则）

**为什么必须有前缀**：清数脚本的精确性完全依赖前缀——没有前缀无法用 WHERE 条件安全删除测试数据，有误删生产数据的风险。

**通用格式**：`{MODULE_PREFIX}_{类型}{序号}`，其中 `MODULE_PREFIX` 由票号派生（如票据 `{{TICKET_PREFIX}}-2440` → 前缀 `2440`）。
**每个数据源的具体前缀格式**（如主键字段、文档 _id、消息业务 ID、缓存 Key 各自的前缀写法）见 `references/{role}.md`。

---

## 执行流程

### 前置条件检查

-1. 读取 `local_profile.yaml`：获取 `name`、`role`；**加载 `references/{role}.md`**。
    - 若 Workbench 画像标明"无自主造数能力" → BLOCK（见 Guardrails）
0. 读取 `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md`（若存在）：读取"造数脚本"阶段状态，从中断处续跑；不存在则全新执行。
1. 读取 `project-memory/project_glossary.md`、`architectural_constraints.md`
2. 确认 `dev_self_test_{xxx}.md` 路径（必填）；不存在则 BLOCK
3. 从模块目录名提取票号，确定模块前缀
4. 按 `references/{role}.md` Workbench 画像加载本 Workbench 需要的数据源信息（如管道设计确认 Job 触发方式）

---

### Step 1：分析用例前置条件

读取 `dev_self_test_{xxx}.md`，逐条扫描：

1. **识别 `{变量:xxx}` 占位符**：建立占位符清单（后续 Step 5 填入真实值）
2. **分析前置数据需求**：每条用例的"前置条件"描述了需要哪些数据
3. **识别数据源**：需要哪些数据源（见本 Workbench 画像的数据源清单），各需几条记录
4. **分析依赖关系**：数据之间的外键/引用关系（决定造数顺序和清数反序）
5. **聚合类用例预计算**：含统计字段的用例，提前手工计算预期值，记录到对照表

**输出**：数据设计总览（数据模型树 + 变量-数据映射草稿 + 预期统计结果速查表）

---

### Step 2：生成造数脚本

按 `references/{role}.md` 的「造数语法」对每个数据源生成脚本。通用规范（各 Workbench）：
- 每条造数前加注释说明对应哪个测试用例
- 关键标识字段使用模块前缀
- 有依赖关系的数据按依赖顺序造（父先于子）
- 不同测试场景需要的字段取值差异，按用例覆盖要求设置

> 部分 Workbench 的造数方式随单元触发方式分支（如数据 Workbench：流式单元造数=发消息 / 批量单元造数=写上游表），具体见 Workbench 参考。

---

### Step 3：生成造数验证查询

每个数据集造数后，按 `references/{role}.md` 的「验证查询」提供确认数据正确写入的查询，并标注预期记录数/关键字段值。

---

### Step 4：生成清数脚本

**清数顺序**：子表/关联数据先清，主表后清（**与造数顺序相反**）。
按 `references/{role}.md` 的「清数语法」对每个数据源生成**幂等**清数脚本（必须带精确 WHERE 条件，可重复执行）。

---

### Step 5：生成变量映射表与用例对照表

在 `test_data_scripts.md` 末尾输出两张表：

**变量映射表**（Step 6 生成 runnable 副本时使用）：

| 占位符 | 实际值 | 数据来源 |
|--------|-------|---------|
| `{变量:xxx}` | {实际值，或"执行 INSERT 后填入"} | {数据源} |

**用例与造数对照表**：

| 测试用例 | 需要的数据 | 预期结果 |
|---------|----------|---------|
| {TC-ID} | {数据组} | {预期统计/状态} |

---

### Step 6：生成可执行自测副本（`_runnable.md`）

**目的**：基于变量映射表，将 `dev_self_test_{xxx}.md` 中所有 `{变量:xxx}` 替换为真实值，输出可直接从头执行的自测文档。

1. 读取 `dev_self_test_{xxx}.md`（原始模板，不修改）
2. 读取 Step 5 变量映射表
3. 全量扫描，对每个 `{变量:xxx}`：有映射值 → 替换；无映射值 → 保留占位符并记入"未解析占位符列表"
4. 在副本**头部**插入说明块：

```markdown
> **⚠️ 可执行副本（Runnable Copy）**
> 本文档基于以下造数数据生成，直接执行勿需查阅其他文档。
> 造数前缀：{本 Workbench 模块前缀}
> 原始模板：`dev_self_test_{xxx}.md`（保留占位符，可复用）
> 如重新造数，请重新运行 test-data-script-generator 更新本副本。
> **未解析占位符**（需手动填入）：{列表，若无则写"无"}
```

5. 写入 `{module_root}/06_test/dev_self_test_{xxx}_runnable.md`

> **注意**：部分值（如数据库自增主键）在执行造数**之前**不可知。这类占位符在变量映射表标注"执行后填入"，生成 runnable 副本时保留并列入"未解析占位符列表"。

---

## 人类门控

| 门控点 | 参与者 | 通过标准 |
|--------|-------|---------|
| 脚本确认 | 开发者本人 | 脚本可执行、前缀正确、依赖顺序正确、runnable 副本占位符无遗漏 |

> 📝 **完成后写入进度日志**：更新 `_progress.md`，将"造数脚本"标记为 ⏸️，记录 `test_data_scripts.md` 和 `_runnable.md` 路径。

---

## 验证回路

- **前缀覆盖检查**：所有造数脚本必须包含模块前缀，缺失则 BLOCK
- **幂等性自检**：所有清数脚本必须有精确的 WHERE 条件，无条件全表操作则 BLOCK
- **依赖顺序检查**：造数按依赖顺序，清数反序
- **变量解析完整性**：统计未解析占位符数量，在 runnable 副本头部全部列出
- **自增/运行时值提示**：造数脚本中凡需要运行时才可知的变量（如自增 ID），明确标注"执行后填入"

---

## 关键原则

1. **前缀隔离**：清数精确性的基础，不可妥协
2. **原始模板不修改**：`dev_self_test_{xxx}.md` 是可复用的测试设计，只生成副本
3. **runnable 副本绑定数据**：副本与具体造数数据绑定，重新造数后必须重新生成副本
4. **最小化数据集**：只造必要的测试数据，不造冗余
5. **造数方式随单元触发分支**：按 Workbench 参考区分单元触发方式（如流式=发消息 / 批量=写上游表）
6. **纯消费 Workbench 不执行**：无自主造数能力的 Workbench 触发时直接 BLOCK，引导联系数据提供 Workbench
