---
name: project-setup
description: 将 dev-harness framework 实例化为项目专属工程交付工作台
---

# Project Setup

将 framework 实例化为项目专属交付工作台的端到端流程。

---

## 执行前提

- 在 dev-harness framework 仓库中运行此 Skill
- 准备好目标项目的输出路径（可以是新建目录）
- 可用的 Python 3 环境（Phase 4 执行 `agent-adapter/adapt.py` 需要）

---

## 输出

目标路径下生成完整的项目文档仓库：

```
{target-path}/
├── CLAUDE.md / AGENTS.md        # 内容一致，两份均写出
├── project-config.yaml          # 索引事实源（Phase 1/2 增量落盘）
├── local_profile.yaml.example
├── .gitignore
├── skills/                      # 全部 Skill；按 Workbench 知识填入各自 references/（dev-self-test 为 templates/）
├── project-memory/              # MEMORY / project_glossary / architectural_constraints / adr_index
├── doc-templates/
├── global-info/README.md        # 维护规范；Skill 不自动加载，需主动 @ 引用
├── rules/{Workbench}/           # coding.md（全部）+ api.md/database.md（按职责）
└── {Workbench-dirs}/01_function/   # 按各 Workbench doc_root 创建
```

Phase 4（可选）执行后，还会在目标路径下额外生成对应 AI Agent 工具的适配目录（如 `.claude/`、`.kiro/`、`.cursor/` 等），详见该 Phase。

---

## Guardrails

| 规则 | 触发条件 | 执行动作 |
|------|---------|---------|
| **首次写盘前检查覆盖** | 进入 Phase 1 落盘步骤前，目标路径已存在且含文件 | WARN：列出已有文件，询问是覆盖、续跑（走 §0 断点续跑协议）还是换路径，等待确认后再写第一个文件 |
| **未确认即落盘** | Phase 1 摘要 / 某 Workbench Phase 2 摘要 / 模式 B 派生摘要 未经用户确认 | BLOCK：禁止写盘。确认是落盘的前提（确认即落盘，落盘必先确认） |
| **Workbench 信息不完整** | 某个 Workbench 的 Phase 2 采集未完成（其 config 条目仍含桩值/TODO） | BLOCK：必须完成所有 Workbench 的 2.0–2.5 采集并落盘，再进入 Phase 3 |
| **模式 B 派生未确认** | 用 B 模式从文档派生技术栈，但派生摘要未经使用者确认 | BLOCK：派生内容是草稿，必须确认后才能落盘 |
| **模式 B 文档缺口** | 文档无法覆盖某项（尤其 2.5 业务约束） | WARN：显式列出缺口，对缺口项转为提问，不得静默留空或编造 |
| **目标路径无法创建** | 路径无写入权限或父目录不存在 | BLOCK：提示用户先创建父目录或检查权限 |
| **跳过 Workbench 采集** | 用户要求跳过某 Workbench 的 Phase 2 采集 | WARN：该 Workbench config 条目以 TODO 标注落盘，其 `<!-- TECH_SPECIFIC -->` 区块亦以 TODO 标注，提示后续手动补充 |
| **Phase 4 命令未确认即执行** | 组装好 `agent-adapter/adapt.py` 调用命令后 | BLOCK：禁止直接执行，必须先完整展示命令并等待用户确认，确认后才能用 Bash 运行 |

---

## 0. 断点续跑协议（每次进入本 Skill 的第一步，先跑这一节）

> 分步运行的背景原理见 `references/resume-protocol.md`（含 Phase 3 各步的文件级探测细则）。本节只保留进入即执行的全局/Workbench 探测序列。

**探测序列**（读文件判断，**严禁盲目从 Phase 1 重头问**）：

1. **目标路径未知** → 先问目标输出路径（续跑也需要它来定位已落盘产物），再继续探测。
2. **`{target}/project-config.yaml` 不存在** → 全新开始，进 **Phase 1**。
3. **config 全局字段含 `{{PLACEHOLDER}}`**（`project.*`/`dirs.*`/`roles` 有未填占位符）→ Phase 1 落盘未完成，回 **Phase 1 落盘步**。
4. **某 Workbench config 条目含 `# TODO`**，或其 `references/{Workbench}.md` 缺失/含未填 `<!-- TECH_SPECIFIC -->`（dev-self-test 查 `templates/{Workbench}_dev_test_template.md`）→ 该 Workbench **Phase 2 未完成**，列出所有未完成 Workbench 后逐个续做；**已完成 Workbench 不重问**。
5. **全部 Workbench Phase 2 完成** → 进 **Phase 3**（Phase 3 内部同样探测各步是否已生成，跳过已完成步）。
6. **Phase 3 全部步骤已完成**（3.9 完成报告已出过）→ Phase 1–3 已无剩余工作；询问用户是否要现在进入 **Phase 4**（适配分发到 AI Agent 工具），Phase 4 是可选阶段，不询问也可结束会话。

向用户一句话汇报探测结论后继续。例：「检测到 Phase 1 已完成、backend 已落盘、data 待采集——将从 data Workbench Phase 2 续跑」。

> **续跑落到 Phase 3 时，立即自主跑完，不要停下等指令。** Phase 3 是纯派生阶段、**不需要任何用户输入**（唯一的门是 3.1 入口校验，发现残留桩值才 BLOCK）。
> 汇报「将从 Phase 3 续跑」之后，**直接按 3.1→3.9 顺序把所有未完成步骤执行到底**——尤其 **3.5 本地化全部 Skill**。
> Phase 2 只写了 `references/{Workbench}.md`，**从未写过任何 `SKILL.md`**；所以 fresh 续跑进 Phase 3 时，target 的 `skills/` 里 SKILL.md 数量为 0（或不全），3.5 **必然待执行**，绝不能因「目标里已有 Phase 2 落的 references」就误判 Phase 3 已完成。

> 探测只读文件、不读对话记忆。与 Guardrail「首次写盘前检查覆盖」配合：路径已有内容时，「续跑」分支即走本协议。

---

## 执行流程

### 增量持久化模型（贯穿全程，先读这一节）

本 Skill 采集的信息量大、轮次多。**严禁把已确认的采集结果一直留在对话上下文里等到 Phase 3 才消费**——
那样越靠后的生成越容易遗漏早期确认的 Workbench。遵循框架核心原则「Memory/文件的权威性高于对话上下文」，
本 Skill 采用**确认即落盘**：

| 阶段 | 确认后立即落盘的内容 | 写入位置 |
|------|---------------------|---------|
| Phase 1 确认 | 全局字段 + Workbenchs 列表（每 Workbench 仅 key 桩条目） | `{target}/project-config.yaml`（**骨架，仅索引**） |
| Phase 2 每 Workbench 确认 | 该 Workbench**索引**（doc_root/repositories） | `{target}/project-config.yaml`（**补全索引条目**） |
| Phase 2 每 Workbench 确认 | 该 Workbench**技术栈知识**（2.1–2.5）→ 填满各含 `_reference.template.md` 的 skill 的 reference 槽 | `{target}/skills/{skill}/references/{Workbench}.md` + dev-self-test `templates/{Workbench}_dev_test_template.md` |
| Phase 2 每 Workbench 确认 | 该 Workbench 2.4 关键约定（编码/接口/库规范） | `rules/{Workbench}/coding.md`（+ 按职责的 `api.md`/`database.md`），**按 Workbench 生成** |
| Phase 2 每 Workbench 确认 | 该 Workbench 2.5 业务约束（散文） | `project-memory/architectural_constraints.md`（**追加草稿段**） |
| Phase 3 | 纯派生：SKILL.md 全局替换 + 生成 CLAUDE.md/目录（reference 已在 Phase 2 就绪） | 见 Phase 3 各步 |

**四条铁律**：
1. **确认是落盘的前提，落盘是下一步的前提**——没确认不写，没写完不进下一步。
2. **配置/索引 vs Workbench 知识分流**：config 只存索引（定位/枚举）；**Workbench 知识直接进 references，绝不压进 config**。
3. **Phase 3 一律从文件读，不从对话记忆读**——config 是索引事实源、references 是 Workbench 知识事实源。
4. **天然可重入**：落盘机制使会话可随时中断、fresh 重建——重入逻辑统一由 **§0 断点续跑协议**驱动（读 `project-config.yaml` 与 `references/` 探测进度，只续做未完成部分）。**推荐分阶段、分 Workbench 各跑独立会话**以规避上下文膨胀。

### Phase 1：基础项目信息采集

依次询问以下信息（每问一条，等待回答后再问下一条）：

1. **项目名称**（如"订单管理系统"、"客户关系平台"）
2. **票据格式**
   - 票据前缀（如 `PROJ`、`ISSUE`、`LION`）
   - 完整格式（如 `PROJ-1234` 或 `#1234`）
3. **目标输出路径**（实例化后的工作台存放位置，如 `/path/to/my-project-doc-workspace/`）
4. **业务 Workbench 清单**（开放式，数量不限，**不预设默认 Workbench 集合**）
   - 项目有哪些业务/技术 Workbench？逐个声明 —— 可以是单个 Workbench（如 `backend` only），也可以是任意组合
     （如 `backend + frontend + data + mobile + ml + infra`）。不要替用户假定一定是 backend/frontend/data。
   - 对**每个**Workbench 只采集 **Workbench key**（唯一标识，建议全小写，如 `backend`、`data`、`mobile`），该值复用。

> **以下三项不再向用户提问，由上面的采集结果 / 全局约定直接派生：**
> - **团队角色列表**：自动 = 第 4 项各 Workbench key + 固定协作角色 `dev-lead` / `ba` / `qa`。直接落入 config 的 `roles`。
> - **需求文档目录名**：固定为 `01-requirements`（全局约定），落入 config 的 `dirs.requirements`。
> - **模块探索产物目录名**：固定为 `00-analysis`（全局约定），落入 config 的 `dirs.module_exploration`。

> **Phase 1 只确定「有哪些 Workbench」和全局信息。** 各 Workbench 的代码仓库、文档产物目录、技术栈等
> **Workbench 专属信息**留到 Phase 2 逐 Workbench 采集 —— 每个 Workbench 的完整画像在 Phase 2 一次问齐，与 N 个 Workbench config 结构对齐。

**Phase 1 完成后（确认 → 落盘 → 进入 Phase 2）**：

1. 以表格形式展示采集摘要（含 Workbench 清单：各 Workbench key），并**回呈三项派生值供确认**：`roles` = 各 Workbench key + `dev-lead`/`ba`/`qa`；`dirs.requirements` = `01-requirements`；`dirs.module_exploration` = `00-analysis`。等待用户确认。
2. **首次写盘前**：执行 Guardrail「首次写盘前检查覆盖」——若目标路径已有内容，先询问覆盖/续跑/换路径。
3. 确认后**立即生成 `{target}/project-config.yaml` 骨架**（config 仅索引）：
   - 填入全部全局字段（`project.*`、`dirs.*`、`roles`）。
   - `Workbenchs` 列表为每个 Workbench 生成一个**桩条目**：仅 `key` 填实际值，索引字段
     （`doc_root`/`repositories`）先写 `# TODO: Phase 2 采集` 占位。
     **config 不含技术栈/分层/命令字段**——那些是 Workbench 知识，Phase 2 直接写入 references，不进 config。
4. 向用户报告"已落盘 project-config.yaml 骨架（含 N 个待采集 Workbench）"，再进入 Phase 2。

---

### Phase 2：逐 Workbench 技术栈采集

针对 Phase 1 采集的**每个启用 Workbench**，依次采集 2.0–2.5 信息（一次处理一个 Workbench，确认后再下一个 Workbench）。

#### 2.0 Workbench 身份与产物位置（始终由对话确认）

- 复述确认本 Workbench 的 **key**（Phase 1 已采集）
- 本 Workbench**文档产物根目录** `doc_root`（如 `02-backend/01_function`；**新 Workbench 无默认值，由用户指定**）
- 本 Workbench**代码仓库清单**：每个仓库的名称 + 一句话描述（可多个，对应 config 该 Workbench 条目的 `repositories`）
- 本 Workbench 在 `local_profile.yaml` 的 **`code_roots` 键名**（默认 = Workbench key，一般无需改）

#### 采集模式选择（适用于下方 2.1–2.5，由执行者在每个 Workbench 开始时选，可逐 Workbench 不同）

| 模式 | 做法 | 适用 |
|------|------|------|
| **A — 对话确认** | 按 2.1–2.5 逐条向使用者提问、逐条确认 | 无现成文档，或希望边问边梳理 |
| **B — 文档派生** | 执行者提供本 Workbench 的 `AGENTS.md` / `CLAUDE.md` / `README` / 既有 `project-config` 等文档路径；AI 阅读分析，自行理解技术栈细节，产出 2.1–2.5 的**派生摘要** | 代码仓库已有较完整的 AGENTS.md / CLAUDE.md |

**每个 Workbench 进入 2.1–2.5 前，用以下话术询问采集方式（2.0 信息已确认后立即问）：**

> 接下来采集 **{Workbench key}** 的技术栈信息（2.1–2.5），请选择方式：
> - **A — 逐条问答**：我依次提问，适合无现成文档或边问边梳理
> - **B — 文档派生**：请提供 AGENTS.md / CLAUDE.md / README 路径（可多个），我读取后产出派生摘要供您确认
>
> 请选 A 或 B，或直接发送文档路径（视为选 B）。

**模式 B 的强制规则**：
- AI 派生的理解是**草稿**，必须以「派生摘要」形式回呈，经使用者确认后才生效（遵循框架草稿机制，**不得静默假定**）
- 文档**未覆盖的项**——尤其 **2.5 业务约束/「雷区」规则**（通常不写在 AGENTS.md 里）——AI 必须**显式列出缺口并转为提问**（对这些项回退到模式 A）
- 文档之间冲突、或与 Phase 1 声明不一致 → 标注 ⚠️，要求使用者裁决
- 提供的文档路径不可读 → 提示后回退模式 A，不得编造技术栈

> 无论 A/B，最终都要产出本 Workbench 完整的 2.1–2.5 信息，并在 Workbench 采集结束时统一确认。

#### 2.1 技术栈详情

- 主语言和版本（如 Java 8 / Python 3.10 / Go 1.21 / Node.js 18）
- 核心框架和版本（如 Spring Boot 2.3.9 / FastAPI 0.100 / Gin 1.9 / React 18）
- 数据访问层（如 MyBatis-Plus 3.4.0 / SQLAlchemy / GORM / Prisma）
- 数据库（如 MySQL 8.0 / PostgreSQL 14 / MongoDB 6.0）
- 其他关键依赖（如 Kafka 2.5 / Redis 7 / Elasticsearch 7.10）

#### 2.2 架构分层模式

- 代码组织方式（如 controller→manager→mapper / MVC / Clean Architecture / DDD）
- 各层的主要职责（一句话描述每层做什么）
- 包或目录命名约定（如 `com.company.service.xxx.controller` 或 `src/modules/xxx/`）

#### 2.3 构建与测试命令

- 编译/构建命令（如 `mvn compile -pl {module}` / `npm run build` / `go build ./...`）
- 单元测试运行命令（如 `mvn test -pl {module} -Dtest={XxxTest}` / `pytest tests/` / `go test ./...`）
- Lint/格式化命令（如 `npm run lint` / `flake8` / `golangci-lint run`；无则填空）

#### 2.4 关键约定

- 命名规范（如 snake_case 字段名 / camelCase 变量 / PascalCase 类名）
- 公共基类或注解（如所有 Entity 继承 `BaseEntity`，所有 Controller 标注 `@RestController`）
- 数据库实体必备字段（如 `id`、`created_at`、`updated_at`、`is_deleted` 等审计字段）
- 特殊的查询或过滤约定（如"所有金额查询必须排除退款订单"）

#### 2.5 业务约束（将编码为 architectural_constraints.md）

- 金额/精度相关的硬性约束（如"金额字段禁止使用 FLOAT，必须 DECIMAL"）
- 安全/权限相关的硬性约束（如"所有接口必须校验租户 ID"）
- 性能/并发相关的硬性约束（如"查询必须走缓存，缓存 TTL 不低于 5 分钟"）
- 数据质量约束（如"统计数据必须过滤某类无效记录"）
- 其他历史经验总结的"雷区"规则

**每个 Workbench 采集结束后（确认 → 落盘 → 下一个 Workbench）**：

> **落盘分流**（铁律 2 的落地）：2.0 定位信息进 config 索引，2.1–2.5 Workbench 知识直接写
> `references/{Workbench}.md`（及 memory 草稿），**绝不压进 config**——趁深度上下文新鲜直接生成 reference，避免"压扁进 config 再展开"的有损往返。

1. 展示该 Workbench 的采集摘要（模式 A 为问答结果，模式 B 为派生摘要），等待用户确认。
2. 确认后**立即落盘**（不等其他 Workbench，不留到 Phase 3）：
   - **`{target}/project-config.yaml`（只索引）**：用 2.0 信息**替换该 Workbench 桩条目**——仅 `key`/`doc_root`/`repositories`，**不含任何技术栈/分层/命令字段**。
   - **`{target}/skills/{skill}/references/{Workbench}.md`（Workbench 知识，核心）**：对每个**含 `references/_reference.template.md` 的 skill**（含 `requirement-kickoff` 的 Workbench 识别信号），用本 Workbench 2.1–2.5 采集结果填充其全部 `<!-- TECH_SPECIFIC -->` 槽，写出本 Workbench reference 文件。填不出的槽**当场转为提问**（Phase 2 是交互的，不静默留空）。
     > **填槽回呈：按「有据 / 无据」分流，不按「派生 / 采集」分流**。多数槽都是 Claude 据 tech_stack 派生的（架构 Workbench 7 槽全派生），逐条确认会架空 Mode B；真正要拦的不是"派生"，而是"无依据的臆测"。每个 `<!-- TECH_SPECIFIC -->` 槽按内容来源分两路：
     >   - **有据**——用户采集的 2.1–2.5、或其提供的 AGENTS.md / CLAUDE.md / README 中可追溯 → 直接填，纳入该 Workbench 落盘摘要供用户扫读即可，不逐条确认。
     >   - **无据**——文档与采集均未覆盖、靠 Claude 通用知识或跨 Workbench 类比补足的空白 → 槽内标 `⚠️ 推断无文档依据`，在 Workbench 采集摘要中单独拎出**逐条回呈确认**，不得静默选定（即把上文「模式 B 强制规则」中"文档未覆盖项须显式列出并转为提问"落实到每个槽）。
     > 如此确认量随 AI 推断量自动伸缩：文档喂得足→几乎无需确认（保住 Mode B 价值）；文档稀薄或新 Workbench 无先例→无据项多→相应多确认。**高危无据项务必纳入回呈**，典型如 architecture-advisor「Workbench 红线」（易把其他 Workbench 硬约束跨 Workbench 硬套）、「Schema 产出骨架」（凭空拟定章节）、dev-self-test「测试维度集」。
   - **`{target}/skills/dev-self-test/templates/{Workbench}_dev_test_template.md`**：同理，用本 Workbench 信息填充 base 模板（dev-self-test 的等价机制）。
   - **`{target}/rules/{Workbench}/`（团队工程规范，按 Workbench 生成）**：用 framework `rules/templates/_coding.template.md` 填充本 Workbench `<!-- TECH_SPECIFIC -->` 槽，写出 `rules/{Workbench}/coding.md`（所有 Workbench）；**该 Workbench 有接口职责** → 同理用 `_api.template.md` 写出 `api.md`；**该 Workbench 有存储职责** → 用 `_database.template.md` 写出 `database.md`（职责由 2.0/2.1 采集判断，无则不生成该文件）。填不出的槽**当场转为提问**。
   - **`{target}/project-memory/architectural_constraints.md`**：把 2.5 业务约束追加为该 Workbench 约束条目（标注 `<!-- draft -->`）。
   > 被跳过的 Workbench（见 Guardrail）：config 条目保留 `# TODO` 占位，其 reference/rules/模板的 TECH_SPECIFIC 标 `TODO`，不写 memory 草稿。
3. 向用户报告"已落盘 {Workbench 名}：config 索引条目 + N 份 reference + N 份 rules 规范 + memory 草稿"，再处理下一个 Workbench。

**全部 Workbench 采集并落盘后**：复核 ① `project-config.yaml` 各 Workbench 索引条目完整（无 `# TODO`）② 每个含 `_reference.template.md` 的 skill 的
`references/` 下每个启用 Workbench 都有对应 `{Workbench}.md`（无遗漏、无未填 TECH_SPECIFIC），再进入 Phase 3。

---

### Phase 3：生成 Project Adaptation Layer（纯派生阶段）

**Phase 3 不"从对话记忆生成"，而是"从已落盘的文件派生"。** 所有采集结果在 Phase 1/2 已增量落盘到
`{target}/project-config.yaml` 和 `{target}/project-memory/` 草稿。Phase 3 每一步都以这些文件为输入，
按以下顺序执行，全部完成后展示总结报告。

> **Phase 3 是自主派生阶段，不需要任何用户输入——进入后（含 fresh 续跑进入）直接把 3.1→3.9 全部未完成步执行到底，不要停下来等用户"明确指令"。** 唯一会暂停的是 3.1 校验发现残留桩值时 BLOCK。**切勿只跑前几步就停**：3.5（本地化全部 Skill）是最易被漏掉的一步——Phase 2 从未写过 SKILL.md，本地化必须在此完成。

#### 3.1 校验已落盘产物（不再生成）

本步**只校验，不重新生成**（产物已在 Phase 1/2 增量落盘）：

1. 读取 `{target}/project-config.yaml`，校验：
   - 全局字段（`project.*`、`dirs.*`、`roles`）已填实际值，无 `{{PLACEHOLDER}}` 残留。
   - `Workbenchs` 每个条目**仅索引字段**（`key / doc_root / repositories`）齐全、无 `# TODO`
     （`key` 同时是该 Workbench role 和 code_roots 键名）。**config 内不应出现技术栈/命令等知识字段**——若有，是落盘分流错误。
2. 校验每个**含 `_reference.template.md` 的 skill** 的 `references/` 下，每个启用 Workbench 都有对应 `{Workbench}.md`，且无未填的 `<!-- TECH_SPECIFIC -->`
   （dev-self-test 则校验 `templates/{Workbench}_dev_test_template.md`）。
3. 若发现残留桩值/缺失 reference → BLOCK，回到对应 Phase 补采并落盘。

> config = 索引事实源；references = Workbench 知识事实源；memory = 约束事实源。三者均文件化，不依赖对话记忆。

#### 3.2 生成 CLAUDE.md

读取 `references/CLAUDE.md.template`（此 Skill 内置副本），按下表替换所有 `{{PLACEHOLDER}}`，
写入 `{target-path}/CLAUDE.md` 和 `{target-path}/AGENTS.md`（内容相同，两份均写出）。

**Placeholder 替换清单（CLAUDE.md.template）：**

| Placeholder | 来源 | 类型 |
|---|---|---|
| `{{PROJECT_NAME}}` | `project.name` | 直接取值 |
| `{{PROJECT_SLUG}}` | `project.name` 全小写、空格换连字符 | 构造 |
| `{{REQUIREMENTS_DIR}}` | `dirs.requirements` | 直接取值 |
| `{{TICKET_PREFIX}}` | `project.ticket_prefix` | 直接取值 |
| `{{EXAMPLE_ROLE}}` | `roles` 列表第一个 Workbench key | 直接取值 |
| `{{ROLES_COMMENT}}` | `roles` 所有值拼成说明串（如 `backend \| frontend \| dev-lead \| ba \| qa`） | 构造 |
| `{{EXAMPLE_CODE_ROOTS}}` | 遍历 `Workbenchs[*].key`，每个生成一行 `  {key}: /path/to/{key}-repo` | 构造（多行） |
| `{{Workbench_DIRS_COMMENT}}` | 遍历 `Workbenchs[*].doc_root`，每个生成一行目录树注释 | 构造（多行） |
| `{{AGENTS_MD_TABLE}}` | 遍历 `Workbenchs[*]`，按各仓库生成 Markdown 表格行 | 构造（表格） |
| `{{TECH_STACK_SUMMARY}}` | 读各 Workbench `references/{Workbench}.md` 首节，逐 Workbench 提炼一行技术栈摘要 | 构造（需读文件） |

#### 3.3 生成 local_profile.yaml.example

按 `roles` 和各 Workbench `key` 生成，列出所有角色和对应的 `code_roots` 键名（Workbench 工程师的 role = Workbench key = code_roots 键名）。

写入 `{target-path}/local_profile.yaml.example`。

#### 3.4 生成 .gitignore

标准内容（`.DS_Store`、`*.pyc`、`node_modules/` 等）+ `local_profile.yaml`。

写入 `{target-path}/.gitignore`。

#### 3.5 本地化 Skills（SKILL.md 全局替换 + 携带 references）

> **重要**：framework 的 SKILL.md 均为**瘦主体，无内联 `<!-- TECH_SPECIFIC -->` 块**——Workbench 知识全部承载在各自 `references/{Workbench}.md`（`requirement-kickoff` 的 Workbench 识别信号同此机制），且**已在 Phase 2 逐 Workbench 生成**。故本步对所有 skill 一律只做下表 `{{PLACEHOLDER}}` 替换，不再填 TECH_SPECIFIC，无任何特例。

**枚举对象 = framework `skills/` 目录的完整 Skill 列表（`project-setup` 除外）**，**不是** target 里已有的子集。
Phase 2 只往 target 写过 `references/{Workbench}.md`、**从未写过任何 SKILL.md**——所以本步（无论首跑还是 fresh 续跑进来）
**必然要为 framework 全集的每个 skill 写出 SKILL.md**，包含没有 references 的 skill（如 `requirement-change-router`、`memory-curator`、`prd-writer`）。
**不要因「target 里只有几个带 references 的目录」就以为 skill 已就绪而跳过本步。**

对 framework `skills/` 目录下的**每个 Skill**（`project-setup` 除外）：

1. 读取 framework 版 `SKILL.md`
2. 按下表替换所有 `{{PLACEHOLDER}}`（Skill 文件内共 3 种，均直接取值）：

   | Placeholder | 来源字段 |
   |---|---|
   | `{{REQUIREMENTS_DIR}}` | `dirs.requirements` |
   | `{{TICKET_PREFIX}}` | `project.ticket_prefix` |
   | `{{MODULE_EXPLORATION_DIR}}` | `dirs.module_exploration` |

3. 写入 `{target-path}/skills/{skill-name}/SKILL.md`
4. **不复制** framework 的 `references/_reference.template.md`（它是框架源）；
   Phase 2 已生成的 `references/{Workbench}.md` 保持在位即可。

**本步完成标准**：对 framework 全集（除 `project-setup`）逐一核对——target 里**每个** skill 都有一份无 `{{PLACEHOLDER}}` 残留的 `SKILL.md`。任一缺失即本步未完成，补齐后才算通过。

> `requirement-kickoff` 的 detect-Workbench 信号此前是内联 TECH_SPECIFIC、需本步特殊回填；现已改为 `references/{Workbench}.md` 机制，由 Phase 2 逐 Workbench 生成，本步与其它 skill 完全一致，**不再有例外**。

#### 3.5b 校验 dev-self-test 各 Workbench 测试模板

`dev-self-test` 的各 Workbench `templates/{Workbench}_dev_test_template.md` **已在 Phase 2 逐 Workbench 生成**，本步只校验：
每个启用 Workbench 都有对应模板、无未填 `<!-- TECH_SPECIFIC -->`；**不复制** framework 的 `_dev_test_template.base.md`（框架源）。

> dev-self-test 的 `SKILL.md` 通过 Memory 契约加载 `templates/{role}_dev_test_template.md`，**无内联 TECH_SPECIFIC 块、无需回填路径**。
> **AC 追溯结构是各 Workbench 通用骨架；测试维度集随 Workbench 变化**（base 模板已按 N 个 Workbench 处理，project-setup 按 Workbench 生成对应维度小节）。

#### 3.6 完善 project-memory/

> `architectural_constraints.md` 的各 Workbench 草稿段已在 Phase 2 增量落盘，
> 本步**不重新从访谈生成**，只补齐模板骨架并规整已有草稿。
> （编码/接口/库规范不在 memory，已在 Phase 2 按 Workbench 落到 `rules/{Workbench}/`，见 3.7。）

- **`project-memory/MEMORY.md`**：从 `project-memory/templates/MEMORY.template.md` 复制，将 `{{PROJECT_NAME}}` 替换为项目名，初始化索引条目
- **`project-memory/project_glossary.md`**：从 `project-memory/templates/project_glossary.template.md` 复制（空模板，等待项目运行中积累）
- **`project-memory/architectural_constraints.md`**：以 `project-memory/templates/architectural_constraints.template.md` 为骨架，将 Phase 2 已落盘的各 Workbench 草稿段填入对应章节，规整格式；去除 `<!-- draft -->` 标记前请确认内容完整
- **`project-memory/adr_index.md`**：从 `project-memory/templates/adr_index.template.md` 复制（空模板，等待第一个架构决策时填充）

> 若 Phase 2 因故未落盘草稿（如全部 Workbench 被跳过），此处回退为从模板生成空骨架并标注 TODO。

#### 3.7 复制 doc-templates/ + global-info/ + 校验 rules/

复制 framework 的 `doc-templates/` 目录到目标路径。

复制 framework 的 `global-info/README.md` 到 `{target-path}/global-info/README.md`。

> **global-info/ 只复制 README.md**，不预填任何内容文件——具体内容（服务拓扑、技术栈清单、业务概述等）由团队在项目启动时手动创建维护。README.md 告知团队成员此目录的使用规范：内容由人维护，Skill 不自动加载，需主动 `@` 引用。

> **rules/ 不整体复制**：其规范按 Workbench 生成——`rules/{Workbench}/` 下的 `coding.md`/`api.md`/`database.md`
> **已在 Phase 2 逐 Workbench 写出**（用 `rules/templates/_*.template.md` 填槽），framework 的 `rules/templates/`（源）**不复制**。
> 本步只校验：每个启用 Workbench 都有 `coding.md`、按职责该有的 `api.md`/`database.md` 都在、无未填 `<!-- TECH_SPECIFIC -->`。

**关于其中的 `<!-- TECH_SPECIFIC -->` 区块**：
- `00_progress_template.md` 的「跨 Workbench 信息」表：project-setup **按 `Workbenchs` 列表预填各 Workbench 行**（每个 Workbench 一行：Workbench key + 该 Workbench doc_root），让进度模板开箱即带项目的 Workbench 集合
- 其余 doc-templates（`03_schema_design`、`04_technical_design`、`05_tasks` 等）内的 `TECH_SPECIFIC`：属**运行时填充**标记（工程师按本模块/本 Workbench 使用模板时填），复制时**原样保留**，不在实例化阶段填充

#### 3.8 创建目录结构

按 `project-config.yaml` 中的各 Workbench `doc_root` 创建目录，并补充 `01_function/` 子目录：

```
{target-path}/
├── {requirements_dir}/          # 固定为 01-requirements/
├── {module_exploration_dir}/    # 固定为 00-analysis/（module-explorer 产物目录）
└── 对 Workbenchs 列表中的每个 Workbench，按其 doc_root 创建一个目录（含 01_function/ 子目录）
    # 例：backend → 02-backend/01_function/ ；data → 04-data/01_function/ ；mobile → 06-mobile/01_function/
    # 目录数量 = 声明的 Workbench 数量，不固定为三个
```

每个目录放置 `.gitkeep` 文件以确保 git 追踪。

#### 3.9 输出完成报告

在对话中输出：
- 已生成的文件列表（按目录组织）
- 需要手动补充的 TODO 项（若有未能自动填充的区块）
- **下一步行动**：
  1. 进入目标目录，运行 `git init && git add . && git commit -m "init: project setup from dev-harness framework"`
  2. 推送到团队共享 git 仓库
  3. 每位工程师 clone 后，复制 `local_profile.yaml.example` 为 `local_profile.yaml` 并填写个人信息
  4. 运行 `/dev-onboarding` 完成首位工程师接入
  5. 可选：进入 **Phase 4**，把这套工作台适配分发给团队实际使用的 AI Agent 工具

---

### Phase 4：适配分发到 AI Agent 工具（组装并执行 agent-adapter 脚本）

Phase 3 产出的是 Agent-中立的 doc-workspace——`SKILL.md`、`rules/`、`project-memory/` 这些内容本身不被任何 AI Agent 工具直接识别。
真正让 Claude Code / Qoder / Kiro / Cursor / Codex 读懂这套工作台，靠的是 `agent-adapter/adapt.py` 做的格式转换（例如 Cursor 需要 `.mdc`、Kiro 需要 steering 格式）。
Phase 4 不采集任何新信息——只读 Phase 1 已确认的目标路径，向用户确认「适配给哪个 Agent」，组装出脚本调用命令，经确认后执行。

**Phase 4 是可选阶段**：用户也可以先跳过，之后随时手动运行 `agent-adapter/adapt.py`（脚本幂等，随时可跑）。

#### 4.1 确定适配目标 Agent

询问用户：团队/本机实际使用哪个（或哪些）AI Agent 工具？

- 单个或多个：`claude-code` / `qoder` / `kiro` / `cursor` / `codex`
- `all`：全部适配
- **自动检测**：用户不确定用哪个、或希望脚本扫描本机已安装的 Agent 时选此项（对应 `--auto`）

#### 4.2 组装命令

按 4.1 的确认结果，套用以下模板之一（`{target-path}` 取自 Phase 1 已确认的目标路径）：

```
python agent-adapter/adapt.py -w {target-path} -t {target}
python agent-adapter/adapt.py -w {target-path} --auto
```

`agent-adapter/adapt.py` 是相对 dev-harness framework 仓库根目录的路径（本 Skill 的执行前提已要求在该仓库内运行，无需改写为绝对路径）。

> 本 Skill 只组装、执行**默认行为**（适配产物写入 `{target-path}` 自身）——不使用 `--dest` 分发到额外的代码仓库；如后续需要把适配结果同步进某个代码仓库，由用户在此 Skill 之外自行运行该脚本并按需加 `--dest`。

#### 4.3 执行前确认（人类门控）

完整展示组装好的命令，并说明其效果：脚本幂等——会先清空 `{target-path}` 下对应 Agent 的适配目录再重写，可安全重复执行。等待用户确认后，再用 Bash 执行。

#### 4.4 汇总报告

- 执行完成后，将脚本打印的 Summary（各 Agent OK/FAILED）呈现给用户
- 若有 FAILED，摘出错误信息，提示可在修复后重新运行同一条命令（脚本幂等）
- 提示下一步：用对应 Agent 工具打开 `{target-path}` 目录验证配置生效

---

## 人类门控

> 本 Skill 的每个确认门同时是**落盘门**：确认通过 → 立即写盘 → 才进下一步（见「增量持久化模型」）。

| 门控点 | 时机 | 通过标准 |
|--------|------|---------|
| 覆盖确认 | Phase 1 首次写盘前，目标路径已有内容 | 用户明确确认覆盖、续跑或换路径 |
| Phase 1 摘要确认 | Phase 1 全部问题回答完毕，**写 config 骨架前** | 项目基础信息无误 |
| 每 Workbench 采集摘要确认 | 每个 Workbench Phase 2 结束，**写该 Workbench config 条目 + memory 草稿前** | 技术栈和约束信息准确（模式 B 须确认 AI 派生摘要无误） |
| Phase 3 入口校验 | 全部 Workbench 落盘后 | `project-config.yaml` 无桩值/占位符残留 |
| Phase 4 命令确认 | 命令组装完成，执行前 | 用户确认目标 Agent 与命令内容无误 |

---

## 关键原则

贯穿全程的不变量（机制详见对应章节，此处只作索引）：

1. **逐问逐答**：Phase 1 与 Phase 2 模式 A 每次只问一个问题，不批量列问题单；模式 B 改为「读文档 → 派生摘要 → 一次性回呈确认」。
2. **复制不引用、TODO 优于空白**：生成后的 Skills 完全独立于 framework；无法确定的内容标 `TODO: 根据实际情况填写`，不留空白区块。
3. **可重入、分步运行**：见 §0 断点续跑协议——多 Workbench 项目推荐每 Workbench Phase 2 各跑一个独立会话，避免上下文膨胀。

> 「确认即落盘 / Phase 3 只从文件读」「config 索引 vs references 知识分流」「Workbench 独立采集」三条核心机制不在此重复，权威定义见「增量持久化模型」的四条铁律。
