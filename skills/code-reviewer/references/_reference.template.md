<!--
  code-reviewer Workbench 审查参考 —— 框架源模板
  ============================================================
  本文件是 framework 源，**不复制到实例项目**。
  project-setup 对每个启用 Workbench，读取本模板 → 填充每个 `<!-- TECH_SPECIFIC -->` 区块 →
  生成 {target}/skills/code-reviewer/references/{Workbench key}.md。

  【N 个 Workbench 原则 —— 本模板第一铁律】
  本模板**不预设任何固定 Workbench 集合**。每个 `<!-- TECH_SPECIFIC -->` 区块给出的是一组**Workbench 无关的问题/维度**，
  project-setup 在 Phase 2 采集本 Workbench 时按**该 Workbench 的 tech_stack / layer_pattern / 构建测试命令 / 项目约束**
  派生答案。

  区块内出现的 backend / data / frontend 仅为**示意答案形态**（来自种子项目三 Workbench），
  **不是枚举、不是查表项**。遇到任何其他 Workbench（如 python / ml / mobile / infra / go ...），
  一律按该 Workbench 自己的 tech_stack 派生，**严禁把新 Workbench 硬塞进某个示例 Workbench**。

  填充原则：用 Phase 2 落盘的 Workbench 知识；Claude 知识补充；无法确定标 `TODO: 根据实际情况填写`，
  不留空白、不编造。
  本模板只承载 ② 技术绑定内容；① Review 模式/流程/报告结构/纪律在 SKILL.md，本文件不重复。
-->

# {{Workbench_NAME}} Workbench — 代码审查参考

> 本文件由 code-reviewer 在 role={{Workbench_NAME}} 时加载，回答"本 Workbench 审查时具体查什么"，
> 与 SKILL.md 的五个审查维度配合使用。具体仓库分层以 `{code_roots.{{Workbench_NAME}}}` 下最近的
> `AGENTS.md` / `CLAUDE.md` 为准。

---

## 1. 本 Workbench 架构/分层合规检查项（对应 SKILL.md 维度 1）

<!-- TECH_SPECIFIC: 架构/分层合规检查项 - project-setup 按本 Workbench tech_stack/layer_pattern/约束派生 -->
[为本 Workbench 列出：
  1. 本 Workbench 常见的架构违规模式；
  2. 各层职责边界检查点（什么逻辑不该出现在哪层）；
  3. 数据类型/字段/接口契约约束检查点；
  4. 与跨 Workbench 调用、持久化、消息/任务调度相关的红线。

 示例（仅示意答案形态，非枚举；其他 Workbench 按其分层与约束派生）：
  - backend：Controller 不得含业务逻辑 / Mapper 不得含业务判断 / 跨服务不得直连其他 Workbench DB；金额字段类型约束；新表审计字段；对外 API 不绕过统一返回/错误码约定
  - data：业务转换不得散落在 Source/Sink（应集中 Transform）/ 写入模式与分区须与现有表一致 / 回刷须幂等；宽表不得套应用层审计字段
  - frontend：组件不得直连 axios（走封装层）/ 不得硬编码接口 Workbench 名 / 全局状态不得滥用；响应字段映射的类型转换检查]
<!-- /TECH_SPECIFIC -->

---

## 2. 本 Workbench 业务逻辑高风险模式（对应 SKILL.md 维度 2）

<!-- TECH_SPECIFIC: 业务逻辑高风险模式 - project-setup 按本 Workbench tech_stack/业务约束派生 -->
[为本 Workbench 列出 Review 时必须重点核对的业务逻辑风险：
  1. 本 Workbench 最容易出错的状态/流程/计算/同步/展示逻辑；
  2. 必须覆盖的边界条件类型；
  3. 哪些变更应判为 HIGH_RISK，并要求更强证据；
  4. 无 Ticket 上下文时哪些业务结论不得轻易下判断。

 示例（仅示意，非枚举）：
  - backend：金额/积分/等级/库存等计算；状态机流转；幂等提交；并发更新；分页/过滤组合；空集合与不存在资源处理
  - data：增量窗口、去重键、迟到数据、历史回刷、幂等写入、口径过滤、上下游字段血缘
  - frontend：权限渲染、表单校验、分页/筛选状态保持、接口空值展示、Loading/Error 状态、路由守卫]
<!-- /TECH_SPECIFIC -->

---

## 3. 本 Workbench 编码规范重点（对应 SKILL.md 维度 3）

<!-- TECH_SPECIFIC: 编码规范重点 - project-setup 按本 Workbench tech_stack/layer_pattern/关键约定派生 -->
[为本 Workbench 列出比通用 coding_standards 更具体的 Review 检查点：
  1. 命名与目录/包结构约定；
  2. 公共基类、注解、装饰器、hook、工具封装等使用约定；
  3. 错误处理与日志规范；
  4. 可维护性红线（重复、过度抽象、魔法值、不可测试结构等）。

 示例（仅示意，非枚举）：
  - backend：DTO/VO/Entity/Mapper 命名；统一异常与错误码；日志不得打印 PII；业务逻辑不写在 Controller；公共查询条件复用
  - data：Job/表/字段命名；配置与 SQL/代码分离；血缘注释；并行度/checkpoint/重试配置显式化
  - frontend：组件/Store/Service 命名；组合式逻辑封装；统一请求封装；避免组件内大段业务转换；i18n/文案约定]
<!-- /TECH_SPECIFIC -->

---

## 4. 本 Workbench 安全检查项（对应 SKILL.md 维度 4）

<!-- TECH_SPECIFIC: 安全检查项 - project-setup 按本 Workbench tech_stack/攻击面派生 -->
[为本 Workbench 列出：
  1. 输入验证（注入、越界、反序列化、文件上传等，按本 Workbench 技术栈）；
  2. 认证/鉴权检查；
  3. 敏感数据处理（脱敏、加密、日志、前端存储）；
  4. 隔离约束（如多租户、数据 Workbench、权限边界）；
  5. 外部系统调用/依赖可信边界。

 示例（仅示意，非枚举；其他 Workbench 按其攻击面派生）：
  - backend：SQL 注入/参数校验、接口鉴权、PII 日志脱敏与加密存储、租户隔离、外部 API 超时与错误处理
  - data：上游数据可信边界、PII 字段在数仓的脱敏/加密、跨租户数据隔离、导出文件权限
  - frontend：XSS/输入转义、鉴权渲染（无权限不渲染入口）、敏感信息不落前端日志/storage、上传文件类型/大小限制]
<!-- /TECH_SPECIFIC -->

---

## 5. 本 Workbench 测试覆盖门槛（对应 SKILL.md 维度 5）

<!-- TECH_SPECIFIC: 测试覆盖门槛 - project-setup 按本 Workbench tech_stack/构建测试命令/风险模式派生 -->
[为本 Workbench 列出 Review 判断"测试证据是否足够"的标准：
  1. 哪类变更必须有单元测试；
  2. 哪类变更必须有集成/E2E/数据验证；
  3. HIGH_RISK 变更必须覆盖哪些正向、负向、边界用例；
  4. 若测试无法运行，最低可接受的验证证据是什么；
  5. 本 Workbench 常用验证命令或 CI 信号（如 Phase 2 已采集）。

 示例（仅示意，非枚举）：
  - backend：核心 service/manager 方法须有单测；接口契约改动须有 controller/integration 测试；金额/权限/状态机须覆盖负向与边界；构建命令与单测命令列明
  - data：Transform 逻辑须有样例输入输出测试；回刷/幂等须有重复执行验证；目标表字段与质量规则须有 SQL 校验；Flink/Spark Job 按项目可用方式验证
  - frontend：组件交互须有组件测试或 E2E；接口消费须覆盖 loading/error/empty/success；权限渲染和路由守卫须有用例；可用时运行 lint/build/test]
<!-- /TECH_SPECIFIC -->
