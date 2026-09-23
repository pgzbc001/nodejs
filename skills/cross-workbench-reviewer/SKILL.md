---
name: cross-workbench-reviewer
description: |
  跨 Workbench 一致性 Review 专家。当所有 Workbench 的 impact-analyzer 完成后，读取各 Workbench 影响分析文档，
  自动对比 API 契约、数据模型、责任边界、技术风险、术语使用五个维度，草稿 cross-workbench-review.md，
  并标注 ⚠️ 不一致项供 Dev Lead 裁决（仅限 role: dev-lead 执行）。
  当用户表达"做跨 Workbench review"、"cross-workbench review"、"跨 Workbench 一致性检查"、
  "Phase 1.5"、"合并各 Workbench 分析" 时触发此 Skill。
---

# Cross-Workbench Reviewer（跨 Workbench 一致性审查）

读取所有 Workbench 影响分析文档，自动对比五个维度的一致性，产出供 Dev Lead 裁决的草稿报告。

**定位**：Dev Lead 的职责是"裁决"而非"生产"——AI 负责信息聚合与问题识别，Dev Lead
负责判断每个不一致项如何处理（谁改、何时改、是否接受风险）。详见下方「关键原则」。

---

## Memory 契约

### 始终加载（每次执行前必读）
- `local_profile.yaml` — 确认 role 为 dev-lead（唯一授权角色）
- `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md` — 读取各 Workbench 状态和产出路径
- `project-memory/project_glossary.md` — 术语权威参照，识别各 Workbench 术语漂移
- `project-memory/architectural_constraints.md` — 识别跨 Workbench 架构约束命中

### 按需加载
- `project-memory/adr_index.md` — 发现跨 Workbench 数据契约决策时加载
- `rules/{各 Workbench}/api.md`（存在者）— 检查跨 Workbench 接口约定一致性时加载（按需自主加载）

### 执行后写入
- `{{REQUIREMENTS_DIR}}/{ticket-id}/cross-workbench-review.md` — 跨 Workbench Review 草稿（AI 生成，Dev Lead 补充裁决后生效）
- `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md` — Dev Lead 确认无遗留问题后，更新"跨 Workbench 一致性 review"状态为 ✅
  - **变更重跑场景**：若本次由 requirement-change-router 路由触发，完成后勾掉 `_progress.md` 重跑清单中本 Skill 对应行（☐→☑）

> **变更重跑时的写入规范**：当本 Skill 因需求变更被重跑（由 requirement-change-router 路由），按「文档历史保留约定」**增量更新**——`cross-workbench-review.md` 属**累积目标态文档（一般产物）**：以现有 review 为基线应用 delta（更新被本次变更波及的维度结论与裁决，保留未受影响维度的既有结论，标记变更及驱动 CR，并做一致性扫描），**不整篇重生成**。顶部维护变更履历表（只记 delta+why），旧全文靠 git 追溯，无需 `_history/` 快照。首次生成（非重跑）无需履历表头。

---

## 输入

| 输入 | 必填 | 说明 |
|------|------|------|
| **票据 ID** | **是** | `{{TICKET_PREFIX}}-xxxx`，用于定位 `_progress.md` 和各 Workbench 影响分析文档 |

各 Workbench 影响分析文档路径从 `_progress.md` Workbench 分析分配表"产出文件"列自动读取，无需手动提供。

---

## 输出

```
{{REQUIREMENTS_DIR}}/{ticket-id}/
└── cross-workbench-review.md    # 跨 Workbench Review 报告（AI 草稿 → Dev Lead 补充裁决）
```

同时写入：
```
{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md    # 跨 Workbench 一致性状态更新为 ✅（Dev Lead 确认后）
```

---

## Guardrails

| 规则 | 触发条件 | 执行动作 |
|------|---------|---------|
| **非 Dev Lead 禁止执行** | `local_profile.yaml` 中 role 不是 dev-lead | BLOCK：此 Skill 仅限 Dev Lead 执行，Workbench 工程师请运行 impact-analyzer |
| **Workbench 分析未全部完成** | `_progress.md` Workbench 分析分配表中任意 Workbench 状态不为 ✅ | BLOCK：列出未完成 Workbench 和负责人，等待其推送分析结果后再运行 |
| **单个 Workbench 需求禁止执行** | `_progress.md` 显示仅涉及 1 个 Workbench | BLOCK：单个 Workbench 需求无需跨 Workbench Review，直接进入 requirements-analyst（汇总） |
| **影响分析文档不存在** | `_progress.md` 中记录的产出路径文件不存在 | BLOCK：列出缺失文件和对应 Workbench，要求工程师重新执行 impact-analyzer 并提交 |
| **禁止代为裁决** | 发现不一致项 | 仅标注 ⚠️ 并说明冲突，不自行决定哪一侧正确；裁决必须由 Dev Lead 填写 |
| **门控未确认禁止写入 _progress.md** | Dev Lead 尚未处理所有 ⚠️ 项 | BLOCK：等待 Dev Lead 逐项填写处理决定，不自动标记跨 Workbench review 为 ✅ |

---

## 执行流程

### 前置条件检查

1. 读取 `local_profile.yaml`：
    - 不存在 → BLOCK
    - role 不是 dev-lead → BLOCK
2. 读取 `{{REQUIREMENTS_DIR}}/{ticket-id}/_progress.md`：
    - 不存在 → BLOCK（requirement-kickoff 尚未执行）
    - 涉及 Workbench 数量 = 1 → BLOCK（单个 Workbench 不需要跨 Workbench Review）
    - 任意 Workbench 状态不为 ✅ → BLOCK（列出未完成 Workbench）
    - 全部 ✅ 且 `cross-workbench-review.md` 已存在 → 提示"已有草稿，是否重新生成？"
3. 从 `_progress.md` Workbench 分析分配表读取各 Workbench 产出文件路径，依次读取所有 `00_impact_analysis.md`
4. 读取 `project-memory/project_glossary.md`
5. 读取 `project-memory/architectural_constraints.md`

---

### Step 1：API 契约对齐分析（接口提供 ↔ 消费关系）

> **闸门按关系判定，不按 Workbench 名判定。** 本维度检查的是「一个 Workbench**提供**接口、另一个 Workbench**消费**接口」这层关系，而非固定的 frontend/backend Workbench 名对。Workbench 名由项目定义（见 `project-config.yaml`），可以是任意组合——frontend↔backend 是最常见形态，但 mobile↔backend、**两个 backend 微服务之间**、任意服务↔网关等同样适用。**不要因为 Workbench 不叫 "frontend"/"backend" 就跳过。**

**执行条件**：扫描各 Workbench `00_impact_analysis.md`，**识别所有存在「接口提供↔消费」关系的 Workbench 对**（一侧定义/修改了接口，另一侧声明要调用它）。每识别出一对，就对该 Workbench 对执行一次本分析；可能有 0 对、1 对或多对。若全部影响分析中均无接口提供/消费描述 → 标注"维度 1：无可比对的接口关系"。

对**每个**接口提供↔消费 Workbench 对，从影响分析中提取接口相关内容：
- 消费侧分析中：需要调用的接口路径、请求参数、期望的响应字段
- 提供侧分析中：新增/修改的接口路径、请求参数定义、响应字段定义

逐接口比对，记录：
- ✅ 一致：两侧描述吻合
- ⚠️ 不一致：字段名/类型/约定有差异，**仅列出冲突，不自行裁决**
- ⚠️ 仅出现在一侧：接口在消费侧分析中存在但提供侧未提及（或反之）

---

### Step 2：数据模型一致性分析（数据模型生产 ↔ 消费关系）

> **闸门按关系判定，不按 Workbench 名判定。** 本维度检查的是「一个 Workbench**生产**某数据模型（表/字段/Schema），另一个 Workbench**消费/传递**它」这层关系，而非固定的 backend/data Workbench 名对。backend↔data 是最常见形态，但 backend↔ml（特征表）、iot↔data、**两个 backend 微服务共享的表**、任意 Workbench↔数仓等同样适用。**不要因为 Workbench 不叫 "backend"/"data" 就跳过——本项目即有 assetbase/custom/rmc 多个 backend 服务，服务间共享的数据模型也属本维度。**

**执行条件**：扫描各 Workbench `00_impact_analysis.md`，**识别所有存在「数据模型生产↔消费/传递」关系的 Workbench 对**（一侧定义/修改了表/字段/Schema，另一侧读取、EDL 消费或映射它）。每识别出一对，执行一次本分析；可能有 0 对、1 对或多对。若全部影响分析中均无数据模型传递描述 → 标注"维度 2：无可比对的数据模型关系"。

对**每个**数据模型生产↔消费 Workbench 对，从影响分析中提取字段/数据结构相关内容：
- 生产侧分析中：新增/修改的表字段、字段类型、枚举值
- 消费侧分析中：消费的来源字段、衍生/输出字段、数据存储字段

逐字段比对（同一业务概念在两侧的名称和类型），记录不一致。

若涉及共享数据模式/Schema：加载对应的 Schema 文档（从 project-memory/ 中查找）作为权威，标注与权威不符的描述。

---

### Step 3：责任边界完整性检查

**所有 Workbench 均执行。**

构建用户故事 → Workbench 覆盖矩阵：
1. 从 PRD（或 `_progress.md` 中记录的 PRD 路径）提取用户故事列表
2. 扫描每份影响分析，标注每个用户故事被哪些 Workbench 的分析覆盖
3. 识别：
   - **责任真空**：某用户故事在所有 Workbench 分析中均未提及 → ⚠️
   - **责任推诿**：某 Workbench 分析写"由 XX Workbench 处理"，但 XX Workbench 分析中无对应内容 → ⚠️
   - **完整覆盖**：用户故事在预期 Workbench 中有明确分析 → ✅

---

### Step 4：技术风险合并评估

**所有 Workbench 均执行。**

汇总各 Workbench 影响分析中的"技术风险"章节：
1. 将所有 Workbench 的风险条目合并到一张表
2. 识别**跨 Workbench 叠加效应**：单个 Workbench 评级为"低/中"的风险，在与其他 Workbench 组合后可能升级。
   按关系判定，不按 Workbench 名判定（与维度 1/2 一致）：
   - 典型叠加场景：某侧有大量写入 + 另一侧同期有回刷/批处理任务 → 峰值写入压力叠加
   - 高并发请求侧 + 被调用侧未声明缓存/限流策略 → 标注风险升级
3. 叠加后评级升级的条目标注说明，其余保持原评级

---

### Step 5：术语一致性检查

**所有 Workbench 均执行。**

扫描各 Workbench 影响分析文档，提取业务术语（名词、状态值、操作词），与 `project-memory/project_glossary.md` 对比：
- **术语表无此词**：标注"建议新增术语"
- **同一概念多个称呼**：标注 ⚠️，列出各 Workbench 用法，指出以术语表为准的标准写法
- 无差异项不需要列出

---

### Step 6：生成草稿报告

写入 `{{REQUIREMENTS_DIR}}/{ticket-id}/cross-workbench-review.md`，**结构与各占位符填法统一引用 `doc-templates/09_cross_workbench_review_template.md`**（报告骨架在模板中维护，本 Skill 不内联；格式调整只改模板，逻辑与模板解耦）。

报告含以下章节（完整骨架见模板）：
- **汇总状态**：各 Workbench 分析人 / 完成时间 / 文档路径
- **维度 1 API 契约对齐**（每个「接口提供↔消费」Workbench 对一组；无此关系则写"无可比对的接口关系"）+ ⚠️ 不一致项裁决表
- **维度 2 数据模型一致性**（每个「数据模型生产↔消费」Workbench 对一组；无此关系则写"无可比对的数据模型关系"）+ ⚠️ 不一致项裁决表
- **维度 3 责任边界**：用户故事 → 覆盖 Workbench 矩阵 + ⚠️ 责任真空裁决表
- **维度 4 技术风险合并评估**：单个 Workbench 评级 → 合并评级（跨 Workbench 叠加）
- **维度 5 术语一致性**：各 Workbench 用法 → 以 glossary 为准
- **遗留问题汇总**：各维度 ⚠️ 项计数 + requirements-analyst 启动条件
- **Dev Lead 确认**：逐项裁决 checkbox + 签名

> 所有 ⚠️ 裁决单元格均以 `👉 _待填写` 开头作为占位（部分单元格后接说明文字，如 `👉 _待填写：指定哪一侧修改_`）由 Dev Lead 填写——Step 7 的裁决完整性检查扫描 `👉 _待填写` 这一前缀，故模板中各裁决单元格必须保留该前缀。

---

### Step 7：等待 Dev Lead 裁决，更新进度日志

**完成草稿后 BLOCK**，等待 Dev Lead：
1. 打开 `cross-workbench-review.md`，逐项填写"裁决（Dev Lead 填写）"列
2. 勾选底部所有确认 checkbox
3. 通知 AI 继续（"review 完成"/"已裁决"/"确认"）

收到确认后：
1. 检查文档中是否仍有 `👉 _待填写`（前缀）或未勾选的 checkbox
   - 有 → BLOCK：列出未处理项，要求补全
   - 无 → 继续
2. 更新 `_progress.md`："跨 Workbench 一致性 review"状态改为 ✅，记录文件路径和完成时间
3. 提示 Dev Lead 执行：提交代码，通知 BA 启动 requirements-analyst（汇总）

---

## 人类门控

| 门控点 | 参与者 | 通过标准 |
|--------|-------|---------|
| 裁决所有 ⚠️ 项 | Dev Lead | 每个不一致项均有明确处理决定（修改/接受/后续跟进）；无"待填写"剩余 |
| 责任真空处理 | Dev Lead | 每个无 Workbench 声明的用户故事均已分配责任方并获负责人确认 |

---

## 验证回路

- **完整性检查**：草稿生成后统计各维度 ⚠️ 项总数，0 项时明确说明"本次各 Workbench 分析一致，无不一致项"（而非跳过）
- **裁决完整性检查**：提交确认前扫描文档，检测是否存在 `👉 _待填写` 前缀（覆盖带说明文字的变体），有则 BLOCK
- **_progress.md 一致性**：状态更新为 ✅ 前，确认文件已写入且路径正确

---

## 关键原则

1. **AI 聚合，人类裁决**：AI 负责识别不一致，Dev Lead 负责判断如何处理；禁止 AI 代为裁决
2. **只做五维比对，不碰开放问题**：各 Workbench 影响分析里的「待澄清问题」section **不归本 Skill**——它们由 requirements-analyst 第二轮技术 Q&A 处理（跨 Workbench 性质的由其回呈 DL 裁决）。本 Skill 只比对 API 契约/数据模型/责任边界/风险/术语五维，产出经 DL 裁决的不一致结论，供 requirements-analyst **消费**（不重裁）。
3. **以术语表为权威**：术语冲突一律以 `project_glossary.md` 为准，不自行选择"更合理"的一侧
4. **完整性优先**：责任边界检查必须覆盖所有用户故事，不因"显然属于某 Workbench"而跳过
5. **0 项也要输出**：各维度无不一致时明确写"无不一致项"，而非省略该维度
6. **门控不跳过**：Dev Lead 确认前不更新 `_progress.md`，不触发 requirements-analyst（汇总）
