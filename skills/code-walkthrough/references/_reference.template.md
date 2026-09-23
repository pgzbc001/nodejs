<!--
  code-walkthrough Workbench 分段范式参考 —— 框架源模板
  ============================================================
  本文件是 framework 源，**不复制到实例项目**。
  project-setup 对每个启用 Workbench，读取本模板 → 填充 `<!-- TECH_SPECIFIC -->` 区块 →
  生成 {target}/skills/code-walkthrough/references/{Workbench key}.md。

  【N 个 Workbench 原则 —— 本模板第一铁律】
  本模板**不预设任何固定 Workbench 集合**。`<!-- TECH_SPECIFIC -->` 区块给出的是一组**Workbench 无关的问题/维度**，
  project-setup 在 Phase 2 采集本 Workbench 时按**该 Workbench 的 tech_stack / layer_pattern** 派生答案。

  区块内出现的 backend / data / frontend 仅为**示意答案形态**（来自种子项目三 Workbench），
  **不是枚举、不是查表项**。遇到任何其他 Workbench（如 python / ml / mobile / infra / go ...），
  一律按该 Workbench 自己的 tech_stack 派生，**严禁把新 Workbench 硬塞进某个示例 Workbench**。

  填充原则：用 config 落盘值；Claude 知识补充；无法确定标 `TODO: 根据实际情况填写`，不留空白、不编造。
  本模板只承载 ② 技术绑定内容；① 走读流程/diff 解析/讲解结构/偏离检测在 SKILL.md，本文件不重复。
-->

# {{Workbench_NAME}} Workbench — 代码走读分段范式参考

> 本文件由 code-walkthrough 在 role={{Workbench_NAME}} 时加载，回答"本 Workbench 改动按什么逻辑线分段走读"，
> 与 SKILL.md 的 Step 1（走读地图）配合使用。具体仓库结构以 `{code_roots.{{Workbench_NAME}}}/AGENTS.md` 为准。

---

## 本 Workbench 分段逻辑线（对应 SKILL.md Step 1）

<!-- TECH_SPECIFIC: 本 Workbench 分段范式 - project-setup 按本 Workbench tech_stack/layer_pattern 派生 -->
[为本 Workbench 回答：①本 Workbench 代码改动应顺着**什么逻辑线**分段（不按文件字母序）②该逻辑线的各段次序③本 Workbench 走读时该额外关注的"决策点/风险点"类型。

 示例（仅示意答案形态，非枚举；其他 Workbench 按其分层/数据走向派生对应逻辑线）：
  - backend：顺**一次请求的调用链**分层 —— 入口/参数校验(Controller) → 业务逻辑(Service/Manager) → 数据访问(Mapper/DAO) → 数据结构(DO/DTO/枚举)；多接口先按接口聚类，再在接口内按层分段。额外关注：事务边界/跨服务调用/校验完整性
  - data：顺**数据流向** —— Source(Kafka/上游表) → 算子/转换/派生字段 → Sink(目标存储)；多 Job 按 Job 聚类，Job 内按数据流分段。额外关注：幂等/回刷影响/分区与写入模式
  - frontend：顺**路由到接口的消费链** —— 路由/页面入口 → 组件层级与状态 → 交互逻辑 → API 调用与响应映射；多页面按页面聚类。额外关注：状态管理边界/鉴权渲染/响应字段映射]
<!-- /TECH_SPECIFIC -->
