---
name: dev-self-test
description: |
  开发自测用例生成专家。基于本 Workbench 交付流水线产物（需求文档、Workbench 设计文档、技术设计），
  以白盒视角系统性生成可直接执行的开发自测文档。每条用例附带数据验证查询语句，
  覆盖正向、校验、业务逻辑、错误处理、边界、安全等测试维度。

  各 Workbench 有专属测试范式（由 local_profile.yaml 的 role 决定）：本 Workbench 的「被测单元概念、测试维度集、
  验证查询语言、提取要点」运行时从 templates/{role}_dev_test_template.md 加载（该模板既是本 Workbench
  测试范式参考，又是输出文档骨架）。

  当用户表达"生成自测用例"、"开发自测"、"dev 自测"、"白盒测试"、
  "生成 TC"、"test case"、"自测文档"、"写测试用例"时触发此 Skill。
---

# Dev Self Test

基于本 Workbench 设计产物，按白盒视角为开发者生成可直接执行的自测用例文档。

> **本 Skill 的分层**：下文为**各 Workbench 通用**的测试方法（AC 追溯、多维白盒、覆盖率、产出结构）。
> 本 Workbench"被测单元是什么、有哪些测试维度、用什么验证查询语言、从设计文档提取什么"这类技术绑定内容，
> 运行时从 `templates/{role}_dev_test_template.md` 加载——**该模板是本 Skill 的本 Workbench 机制（等价于其他 Skill 的 `references/{role}.md`），同时充当输出文档骨架。**

---

## Memory 契约

### 始终加载（每次执行前必读）
- `local_profile.yaml` — 获取 role，确定执行 Workbench
- `templates/{role}_dev_test_template.md` — **本 Workbench 测试范式 + 输出骨架**（被测单元概念、维度集、验证查询语言）
- `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md` — 票级进度日志（判断是否从断点续跑）
- `project-memory/project_glossary.md` — 项目术语表
- `project-memory/architectural_constraints.md` — 架构约束（影响业务逻辑维度的测试设计）

### 按需加载
- `project-memory/security_checklist.md` — 生成安全性维度用例时加载（若项目有此文件）

### 执行后写入
- `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md` — 完成后更新阶段状态和产出路径
  - **变更重跑场景**：若本次由 requirement-change-router 路由触发，完成后勾掉 `_progress.md` 重跑清单中本 Skill 对应行（☐→☑）

> **变更重跑时的写入规范**：自测文档属**累积目标态文档（一般产物）**——按「文档历史保留约定」**增量更新**：针对变更后的功能补充/修改对应用例（增/改/删），保留与本次变更无关的既有用例，标记变更及驱动 CR，并做一致性扫描，**不整篇重生成**。仅需履历表头，旧版靠 git 追溯，无需 `_history/` 快照。

---

## 输入

| 文档 | 必填 | 说明 |
|------|------|------|
| **PRD** | **所有 Workbench** | 理解业务意图、用户故事根源 |
| **requirements.md** | **所有 Workbench** | 提取验收标准（AC）作为测试断言依据 |
| **Workbench 设计文档** | **各 Workbench 必填** | 本 Workbench 必填的设计文档名见 `templates/{role}_dev_test_template.md` 文档信息表（如对外接口设计 / 管道设计 / 接口消费规范） |
| schema 文档 | 推荐 | 字段约束、枚举值 |
| design.md | 推荐 | 业务流程、状态机、组件交互逻辑 |
| tasks.md | 可选 | 确认已实现范围，排除未实现部分 |

---

## 输出

```
{module_root}/
└── 06_test/
    └── dev_self_test_{xxx}.md    # 开发自测白盒用例（含数据验证查询语句）
```

按 `templates/{role}_dev_test_template.md`（本 Workbench 骨架）生成。原始模板不修改。

---

## Guardrails

| 规则 | 触发条件 | 执行动作 |
|------|---------|---------|
| **local_profile 缺失** | `local_profile.yaml` 不存在 | BLOCK：必须先创建并声明 role |
| **角色不支持** | role 为无代码 Workbench 的协作角色（如 ba） | BLOCK：此 Skill 由 Workbench 工程师执行 |
| **无 PRD 禁止开始** | 用户未提供 PRD 路径 | BLOCK：PRD 是必填项，缺失则无法追溯测试意图 |
| **Workbench 设计文档缺失** | 本 Workbench 必填的 Workbench 设计文档不存在 | BLOCK：必须先运行 architecture-advisor |
| **测试用例必须追溯 AC** | 正向用例无法追溯到 requirements.md AC | BLOCK：标注 ⚠️，要求补充 AC 来源，禁止生成无根据的用例 |
| **AC 覆盖率不达标** | 某条 AC 无对应正向用例 | BLOCK：列出未覆盖 AC，补充后再输出文档 |
| **验证查询缺失** | 用例缺少本 Workbench 数据验证查询语句（适用于有数据存储的 Workbench） | WARN：标注"待补充验证查询"，继续执行 |

---

## 执行流程

### 前置条件检查

-1. 读取 `local_profile.yaml`（获取 role、name；无代码 Workbench 协作角色 → BLOCK）；**加载 `templates/{role}_dev_test_template.md`**
0. 读取 `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md`（判断断点续跑）
1. 读取 `project-memory/project_glossary.md` + `architectural_constraints.md`
2. 确认 PRD 文档路径
3. 确认模块输出目录（`{module_root}/`）和测试范围（默认全量）
4. 按本 Workbench 模板的"文档信息"表检查本 Workbench 必填设计文档是否存在

---

### Step 1：收集产出物

**从 PRD 提取（各 Workbench 通用）**：业务背景和目标、用户故事、业务规则和约束；对比 PRD 与设计文档，标注遗漏的业务场景。

**从 requirements.md 提取（各 Workbench 通用）**：每条 REQ 的验收标准（AC，作为正向用例断言依据）、非功能需求（性能/并发/安全场景）。

**从本 Workbench 设计文档提取**：被测单元清单、契约/字段约束、状态机、数据加工规则等——**具体提取要点见 `templates/{role}_dev_test_template.md`**（不同 Workbench 从不同设计文档提取不同信息）。

---

### Step 2：生成测试用例

对每个被测单元，**按 `templates/{role}_dev_test_template.md` 定义的「本 Workbench 测试维度集」逐维度生成用例**
（不同 Workbench 维度集不同：有的 Workbench 6 维、有的 7 维，场景代码也不同；以本 Workbench 模板为准）。

每条用例遵循模板的用例结构（前置条件 / 测试步骤 / 输入 / 预期输出 / 验证点 / 追溯 AC），
并附**本 Workbench 验证查询**（查询语言以本 Workbench 模板为准——SQL / 搜索 DSL / 数仓 SQL / HTTP 请求响应断言等）。

> **业务逻辑维度是最需结合 `architectural_constraints.md` 的部分**：每条硬性业务约束都应有对应业务逻辑用例。
> 变量用 `{变量:xxx}` 标记，供 test-data-script-generator 替换为真实值。

---

### Step 3：覆盖率检查

1. 将测试用例追溯到 PRD 用户故事 → requirements.md AC，形成完整追溯链
2. 检查每条 AC 是否有对应正向用例（HP）
3. 按本 Workbench 模板的"附加追溯维度"检查本 Workbench 特有覆盖（如错误码覆盖、字段映射覆盖、UI 状态覆盖等）
4. 对比 PRD 与设计文档，列出遗漏的业务场景
5. 输出覆盖率统计（AC 覆盖率 + 本 Workbench 附加维度覆盖 + 遗漏项列表）

---

### Step 4：输出自测文档

按本 Workbench 模板输出到 `{module_root}/06_test/dev_self_test_{xxx}.md`。

**用例编号规则（各 Workbench 通用格式）**：
```
TC-{模块缩写}-{编号}-{场景类型}-{序号}
其中「场景类型」取本 Workbench 维度集的代码（见本 Workbench 模板，如 HP/PV/BL/... 因 Workbench 而异）
示例：TC-FA-001-HP-01
```

**完成后暂停，等待开发确认。**
> 📝 更新 `_progress.md`，将"自测用例"标记为 ⏸️

---

## 人类门控

| 门控点 | 参与者 | 通过标准 |
|--------|-------|---------|
| 自测用例确认 | 开发者本人 | AC 覆盖率 100%、用例可直接执行、验证查询语句无误 |

---

## 验证回路

- **AC 覆盖率**：每条 AC 必须有正向用例，否则 BLOCK
- **本 Workbench 附加覆盖**：本 Workbench 模板定义的附加追溯维度（如错误码/字段映射/UI 状态）逐项核对，缺失则 WARN
- **验证查询完整性**：有数据存储的 Workbench，每条用例必须有验证查询语句，缺失则 WARN
- **PRD vs. 技术文档比对**：发现遗漏业务场景在覆盖率报告中列出

---

## 关键原则

1. **开发者视角**：每条用例是开发者在本地/测试环境可直接执行的步骤，不是 QA 测试脚本
2. **Workbench 隔离**：只读取本 Workbench 设计文档，不依赖其他 Workbench 的内部实现
3. **需求驱动**：每条用例必须可追溯到 PRD 或 AC，禁止生成无依据的用例
4. **验证查询直可用**：验证语句使用实际表名/索引名，变量用 `{变量名}` 标记，可直接替换执行
5. **业务语言**：用例描述使用 `project_glossary.md` 中的标准业务术语
