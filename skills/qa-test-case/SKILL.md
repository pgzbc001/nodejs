---
name: qa-test-case
description: |
  QA 测试用例生成专家。以黑盒视角，基于需求文档（requirements.md）和 PRD，
  跨 Workbench 生成可直接执行的 QA 测试用例文档，独立于开发自测流程。

  覆盖六个维度：验收测试（AT）、跨 Workbench E2E（E2E）、负向场景（NEG）、
  权限矩阵（PERM）、回归场景（REG）、UAT（UAT）。

  不依赖 api_design / pipeline_design 等实现文档；
  可在需求确认后与开发阶段并行执行。

  当用户表达"生成 QA 用例"、"QA 测试用例"、"功能测试用例"、"黑盒测试"、
  "生成测试用例"、"写 QA TC"时触发此 Skill。
---

# QA Test Case

基于需求文档，以黑盒视角跨 Workbench 生成 QA 测试用例，独立于开发自测。

---

## Memory 契约

### 始终加载（每次执行前必读）
- `local_profile.yaml` — 获取 role，确认 QA 执行权限
- `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md` — 发现涉及 Workbench、各 Workbench requirements.md 路径
- `project-memory/project_glossary.md` — 项目术语表

### 按需加载
- 各 Workbench `requirements.md` — 路径从 `_progress.md` Workbench 分配表读取，逐 Workbench 加载
- 各 Workbench `00_impact_analysis.md` — 生成回归用例（REG）时加载
- `project-memory/architectural_constraints.md` — 生成权限/租户相关用例时加载

### 执行后写入
- `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md` — 完成后更新 QA 用例阶段状态和产出路径
  - **变更重跑场景**：若本次由 requirement-change-router 路由触发，完成后勾掉 `_progress.md` 重跑清单中本 Skill 对应行（☐→☑）

> **变更重跑时的写入规范**：QA 用例文档属**累积目标态文档（一般产物）**——按「文档历史保留约定」**增量更新**：针对变更后的需求补充/修改对应用例（增/改/删，覆盖 AT/E2E/NEG/PERM/REG/UAT 各维度），保留与本次变更无关的既有用例，标记变更及驱动 CR，并做一致性扫描，**不整篇重生成**。仅需履历表头，旧版靠 git 追溯，无需 `_history/` 快照。

---

## 输入

| 输入 | 必填 | 说明 |
|------|------|------|
| **票据 ID** | **是** | `{{TICKET_PREFIX}}-xxxx`，用于定位 `_progress.md` 和所有下游产物 |
| **PRD 文档路径** | **是** | 理解用户故事根源和业务意图 |

---

## 输出

```
{{REQUIREMENTS_DIR}}/{ticket-id}/qa/
├── qa_test_cases.md          # 主测试用例文档（六个维度）
└── qa_data_requirements.md   # 数据需求清单（供开发配合准备测试数据）
```

---

## Guardrails

| 规则 | 触发条件 | 执行动作 |
|------|---------|---------|
| **local_profile 缺失** | `local_profile.yaml` 不存在 | BLOCK |
| **角色不支持** | role 不是 qa 或 dev-lead | BLOCK：此 Skill 由 QA 工程师或 Dev Lead 执行 |
| **requirements.md 未就绪** | 任意 Workbench 的 requirements.md 状态不为 ✅ | BLOCK：列出未完成 Workbench，等待所有 Workbench ✅ 后继续 |
| **PRD 未提供** | 用户未提供 PRD 路径 | BLOCK |
| **禁止读取实现文档** | 试图加载 api_design / pipeline_design 等技术设计文档 | BLOCK：QA 测试基于需求而非实现 |
| **用例必须追溯 AC** | AT 用例无法追溯到 requirements.md 的 AC | BLOCK：标注 ⚠️ 来源缺失 |
| **AC 覆盖率不达标** | 任意 AC 无对应 AT 用例 | BLOCK：列出未覆盖 AC，补充后再输出 |

---

## 执行流程

### 前置条件检查

-1. 读取 `local_profile.yaml`（role 不是 qa/dev-lead → BLOCK）
0. 读取 `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md`（获取各 Workbench requirements.md 路径）
1. 逐 Workbench 检查 requirements.md 状态（任意 Workbench 未 ✅ → BLOCK）
2. 加载 PRD + 各 Workbench requirements.md
3. 尝试加载各 Workbench `00_impact_analysis.md`（存在则加载，用于 REG 维度）

---

### Step 1：构建跨 Workbench 需求全视图

综合 PRD 和各 Workbench requirements.md，建立：
1. **AC 清单**（按 Workbench 汇总每条 REQ 的验收标准）
2. **跨 Workbench 交互点**（需要多个 Workbench 协作的用户故事）
3. **权限规则**（角色可见性、操作限制）
4. **回归风险点**（来自 impact_analysis.md 的"涉及现有模块"）
5. **UAT 场景**（面向业务方验收的核心操作路径）

---

### Step 2：生成测试用例

#### 统一用例格式

```markdown
| 字段 | 内容 |
|------|------|
| **TC_ID** | TC-QA-{维度}-{seq} |
| **TC_Desc** | {用例描述} |
| **Precondition** | {前置数据/账号/环境状态，使用业务语言} |
| **Step_Desc** | 1. {步骤1}<br>2. {步骤2} |
| **Expect** | {可观测的预期结果——UI 显示 / HTTP 响应语义 / 业务状态} |
```

#### 2.1 验收测试（AT）：每条 AC 至少一个用例，AC 编号标注在 TC_Desc

#### 2.2 跨 Workbench E2E：仅覆盖涉及 2 个以上 Workbench 的完整用户旅程

#### 2.3 负向场景（NEG）：非法输入、越权操作、重复提交、业务规则违反

#### 2.4 权限矩阵（PERM）：先建矩阵（角色×操作），从矩阵中为差异显著单元格各生成一条用例

#### 2.5 回归场景（REG）：基于 impact_analysis.md 的"修改"类模块（文件不存在则跳过，在报告中注明"待补充"）

#### 2.6 UAT 场景：使用纯业务语言，每个核心用户故事至少一个场景

---

### Step 3：覆盖率检查

- AC 覆盖率（目标 100%，未覆盖则 BLOCK）
- 跨 Workbench E2E 覆盖（所有跨 Workbench 交互点）
- 权限矩阵完整性（有权限/无权限各至少一条）

---

### Step 4：生成数据需求清单

用业务语言描述每个测试场景所需数据，写入 `qa_data_requirements.md`。

---

### Step 5：输出用例文档，等待 QA 负责人确认

> 📝 更新 `_progress.md`，将"QA 用例"标记为 ⏸️

---

## 人类门控

| 门控点 | 参与者 | 通过标准 |
|--------|-------|---------|
| 用例设计确认 | QA 负责人 | AC 覆盖率 100%、用例可直接执行 |
| UAT 场景确认 | BA / 业务方 | 业务语言准确、无技术术语 |

---

## 关键原则

1. **独立于开发**：不依赖 dev-self-test 产物，可与开发阶段并行
2. **需求驱动，不读实现**：输入只有 PRD 和 requirements.md
3. **黑盒验证**：预期结果只描述用户可见的输出，不写数据库验证查询
4. **跨 Workbench 视角**：不受 role Workbench 隔离限制，读取所有 Workbench 的需求文档
