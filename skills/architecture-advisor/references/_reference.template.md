<!--
  architecture-advisor Workbench 设计参考 —— 框架源模板
  ============================================================
  本文件是 framework 源，**不复制到实例项目**。
  project-setup 对每个启用 Workbench，读取本模板 → 填充每个 `<!-- TECH_SPECIFIC -->` 区块 →
  生成 {target}/skills/architecture-advisor/references/{Workbench key}.md。

  【N 个 Workbench 原则 —— 本模板第一铁律】
  本模板**不预设任何固定 Workbench 集合**。每个 `<!-- TECH_SPECIFIC -->` 区块给出的是一组**Workbench 无关的问题/维度**，
  project-setup 在 Phase 2 采集本 Workbench 时按**该 Workbench 的 tech_stack / layer_pattern** 派生答案，
  并以 Claude 对该技术栈的标准知识补充。

  区块内出现的 backend / data / frontend 仅为**示意答案形态**（来自种子项目三 Workbench），
  **不是枚举、不是查表项**。遇到任何其他 Workbench（如 python / ml / mobile / infra / go ...），
  一律按该 Workbench 自己的 tech_stack 派生，**严禁把新 Workbench 硬塞进某个示例 Workbench**。

  填充原则：用 config 落盘值；Claude 知识补充；无法确定标 `TODO: 根据实际情况填写`，不留空白、不编造。
  本模板只承载 ② 技术绑定内容；① 通用流程/纪律/章节骨架在 SKILL.md，本文件不重复。
-->

# {{workbench_NAME}} Workbench — 架构设计参考

> 本文件由 architecture-advisor 在 role={{workbench_NAME}} 时加载，回答"本 Workbench 具体怎么设计"，
> 与 SKILL.md 的通用流程配合使用。

---

## 0. Workbench 设计画像

<!-- TECH_SPECIFIC: Workbench 设计画像 - project-setup 按本 Workbench tech_stack 派生 -->
[为本 Workbench 回答以下**Workbench 无关问题**：
  Q1 本 Workbench 是否有**数据存储职责**？（决定 SKILL.md Step 2.1 是否执行）
  Q2 本 Workbench Step 2.2 的**设计形态**是什么？（对外接口 / 数据管道 / 接口消费 / 其他）
  Q3 产出文件**命名规则**：02_schema/ 用什么文件名；03_api 还是 03_pipeline，文件名模式。

 示例（种子三 Workbench，仅示意答案形态，非枚举；其他 Workbench 按其 tech_stack 同样回答这三问）：
  - backend：有数据存储；Step 2.2=对外 REST 接口；02_schema/sql_ddl_{xxx}.md(+sql_dml)；03_api/api_design_{xxx}.md
  - data：有数据存储；Step 2.2=数据管道；02_schema/schema_{xxx}.md（宽表字段清单）；03_pipeline/pipeline_design_{ticket}.md
  - frontend：无数据存储（跳过 Step 2.1）；Step 2.2=接口消费规范；03_api/interface_spec_{xxx}.md]
<!-- /TECH_SPECIFIC -->

---

## 1. Schema 设计规范（对应 SKILL.md Step 2.1）

<!-- TECH_SPECIFIC: Schema 设计规范 - project-setup 按本 Workbench tech_stack 派生 -->
[为本 Workbench 回答：本 Workbench 数据结构/Schema 的 ①命名约定（前缀、大小写、命名模式）②必备/审计字段（或"本 Workbench 无"）
 ③字段类型强约束（精度/金额等）④存储/分区/索引设计原则。

 示例（仅示意，非枚举）：
  - backend：t_ 前缀、snake_case、必备审计字段(id/is_deleted/created_by/created_time…)、金额必须 DECIMAL、布尔须 DEFAULT、按查询模式建索引
  - data：无 t_ 前缀与应用审计字段、遵循数仓分层命名、声明存储格式/分区字段/更新模式
  - frontend：通常无 Schema；如有本地状态/Store 结构，在此约定其设计规范]
<!-- /TECH_SPECIFIC -->

### 1.1 结构化产出骨架（Schema 文档应覆盖的维度）

<!-- TECH_SPECIFIC: Schema 产出骨架 - project-setup 按本 Workbench tech_stack 派生 -->
[为本 Workbench 列出 Schema 文档的固定章节骨架（本 Workbench 的"思考维度清单"，防漏项）。

 示例（仅示意，非枚举）：
  - backend：表结构概览表 + 字段详情表 + DDL 语句 + ER 关系说明
  - data：① 字段清单(字段名/类型/业务含义/数据来源 上游表.字段/加工逻辑 直传|聚合|派生)
          ② 表级说明(存储格式/分区字段/更新模式) ③ 历史回溯(是否回刷/范围/优先级) ④ 数据质量约束(NOT NULL/唯一键/业务完整性)
  - frontend：通常省略，或 Store 状态结构清单]
<!-- /TECH_SPECIFIC -->

---

## 2. 接口/管道设计规范（对应 SKILL.md Step 2.2）

<!-- TECH_SPECIFIC: 接口/管道设计规范 - project-setup 按本 Workbench tech_stack 派生 -->
[为本 Workbench 回答：本 Workbench Step 2.2 设计形态的**规范 + 产出骨架（应覆盖的维度）**。

 示例（仅示意，非枚举；其他 Workbench 按其对外契约形态派生对应维度清单）：
  - backend（对外接口）：每个接口含 方法+路径(路径/网关约定)/功能+关联REQ/请求参数(类型必填校验)/响应(成功+失败)/示例/错误码；+ 项目级返回约定(如有)
  - data（数据管道）：① 拓扑图(来源→处理 Job→目标存储) ② Job 清单(名称/触发 流|批|定时/输入输出/逻辑摘要) ③ ETL 描述(关键转换/空值处理) ④ 依赖关系 ⑤ 历史回刷方案(分片/幂等) ⑥ 数据质量检测点
  - frontend（接口消费）：① 消费清单(路径/方法/用途) ② 请求策略(时机/参数/鉴权) ③ 响应处理(提取/转换/空值) ④ 错误处理(按码分类 UI 响应) ⑤ Loading 策略 ⑥ 关联REQ]
<!-- /TECH_SPECIFIC -->

---

## 3. 技术设计逐 Workbench 章节填法（对应 SKILL.md Step 3 的 §3/§4/§6/§7/§9）

<!-- TECH_SPECIFIC: 技术设计逐 Workbench 章节 - project-setup 按本 Workbench tech_stack/layer_pattern 派生 -->
[为本 Workbench 说明 design.md 各按 Workbench 章节该写什么。维度固定（§3 代码结构 / §4 数据模型分析 / §6 组件与实现 / §7 错误处理 / §9 测试策略），内容按本 Workbench tech_stack 与分层模式派生。

 示例（仅示意，非枚举）：
  - backend：§3 新增/修改类与包(按分层) §4 引用 02_schema+ER图+索引策略 §6 各层职责(校验→业务→数据访问→DTO) §7 错误码/异常分类/全局处理 §9 重点类单测+集成测试
  - data：§3 新增/修改 Job/脚本(按拓扑序) §4 引用字段清单与血缘+数据流图+分区与回溯窗口+质量指标 §6 Job 内部(并行度/Checkpoint/算子逻辑/容错幂等/回刷) §7 ETL 失败(死信/告警)+数据质量异常 §9 Job 单测(miniCluster)+质量验证脚本
  - frontend：§3 新增/修改组件/视图/Store(按页面层级) §4 引用接口消费规范+响应到组件状态映射+全局状态结构 §6 组件层级与通信+生命周期钩子+可复用识别+路由 §7 接口错误 UI+超时降级 §9 组件单测+E2E]
<!-- /TECH_SPECIFIC -->

---

## 4. 任务阶段划分（对应 SKILL.md Step 4）

<!-- TECH_SPECIFIC: 任务阶段划分 - project-setup 按本 Workbench tech_stack/layer_pattern 派生 -->
[为本 Workbench 列出常见任务阶段（generate-tasks 按 design.md §3/§6 动态裁剪）。阶段按本 Workbench 分层与构建方式派生。

 示例（仅示意，非枚举）：
  - backend：① 数据层(DDL/实体/Mapper) ② 业务层(核心逻辑) ③ 接口层(入口+DTO+校验) ④ 集成层(Feign/MQ/缓存 按需) ⑤ 测试(单元+集成)
  - data：① Schema 变更(数仓表迁移) ② Job 实现(Source/Transform/Sink) ③ ETL 脚本(按需) ④ 历史回刷 Job ⑤ 数据质量验证 ⑥ 测试(miniCluster+线下端到端)
  - frontend：① 路由与页面骨架 ② UI 组件 ③ 接口对接(封装/参数/转换) ④ 状态管理(按需) ⑤ 交互逻辑(Loading/错误/分页/校验) ⑥ 测试(组件+E2E)]
<!-- /TECH_SPECIFIC -->

---

## 5. Workbench schema / 设计红线（对应 SKILL.md Guardrail「Workbench 红线违反」）

<!-- TECH_SPECIFIC: Workbench 设计红线 - project-setup 按本 Workbench tech_stack 派生 -->
[为本 Workbench 列出必须 BLOCK 的**技术绑定硬红线**（区别于 architectural_constraints.md 的项目级通用约束）。

 示例（仅示意，非枚举）：
  - backend：金额禁用 FLOAT/DOUBLE(须 DECIMAL/BIGINT)；新建表禁缺审计字段；禁止上层绕过服务层直查大数据量存储
  - data：宽表禁套应用层审计字段/t_ 前缀；声明的分区字段必须落到分区定义；NOT NULL 业务键不得为空
  - frontend：如禁止组件内硬编码接口 Workbench 名/绕过统一请求封装
 说明：项目级、跨 Workbench 通用约束归 architectural_constraints.md，本处只放本 Workbench 技术红线。]
<!-- /TECH_SPECIFIC -->
