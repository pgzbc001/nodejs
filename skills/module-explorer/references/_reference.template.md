<!--
  module-explorer Workbench 探索参考 —— 框架源模板
  ============================================================
  本文件是 framework 源，**不复制到实例项目**。
  project-setup 对每个启用 Workbench，读取本模板 → 填充每个 `<!-- TECH_SPECIFIC -->` 区块 →
  生成 {target}/skills/module-explorer/references/{Workbench key}.md。

  【N 个 Workbench 原则 —— 本模板第一铁律】
  本模板**不预设任何固定 Workbench 集合**。每个 `<!-- TECH_SPECIFIC -->` 区块给出的是一组**Workbench 无关的问题/维度**，
  project-setup 在 Phase 2 采集本 Workbench 时按**该 Workbench 的 tech_stack / layer_pattern** 派生答案。

  区块内出现的 backend / data / frontend 仅为**示意答案形态**（来自种子项目三 Workbench），
  **不是枚举、不是查表项**。遇到任何其他 Workbench（如 python / ml / mobile / infra / go ...），
  一律按该 Workbench 自己的 tech_stack 派生，**严禁把新 Workbench 硬塞进某个示例 Workbench**。

  填充原则：用 config 落盘值；Claude 知识补充；无法确定标 `TODO: 根据实际情况填写`，不留空白、不编造。
  本模板只承载 ② 技术绑定内容；① 探索方法/产出结构/验证回路在 SKILL.md，本文件不重复。
-->

# {{Workbench_NAME}} Workbench — 模块探索参考

> 本文件由 module-explorer 在 role={{Workbench_NAME}} 时加载，回答"本 Workbench 优先探索什么、怎么定位"，
> 与 SKILL.md 的探索方法配合使用。具体仓库结构以 `{code_roots.{{Workbench_NAME}}}/AGENTS.md` 为准。

---

## 1. 探索策略（对应 SKILL.md Step 1）

<!-- TECH_SPECIFIC: 探索策略 - project-setup 按本 Workbench tech_stack/layer_pattern 派生 -->
[为本 Workbench 回答：①**优先探索目标**（按本 Workbench 分层，从哪一层切入最能看清业务）②**关注重点**（本 Workbench 最该看清的逻辑类型）。

 示例（仅示意答案形态，非枚举；其他 Workbench 按其分层/职责派生）：
  - backend：优先 业务逻辑层 → 接口层校验 → 数据访问层查询；关注 计算公式/数据过滤条件/跨服务调用链路
  - data：优先 数据来源 → 字段映射逻辑 → 聚合计算规则；关注 字段血缘/去重逻辑/聚合规则/分区
  - frontend：优先 接口调用链路 → 数据展示逻辑 → 组件状态管理；关注 入参出参/字段命名与业务术语对应]
<!-- /TECH_SPECIFIC -->

---

## 2. 定位命令（对应 SKILL.md Step 1）

<!-- TECH_SPECIFIC: 模块入口定位命令 - project-setup 按本 Workbench tech_stack 派生 -->
[为本 Workbench 回答：用什么 find/grep/head 命令，按本 Workbench 文件类型与目录约定定位模块入口文件。

 示例（仅示意，非枚举；其他 Workbench 按其文件类型派生）：
  - backend（如 Java）：find {code_roots.backend}/ -name "XxxController.java" -not -path "*/target/*"；grep -r "@RequestMapping" --include="*.java" -l
  - data（如 Python/SQL）：find {code_roots.data}/ -name "*.py" -o -name "*.sql" | grep -v __pycache__；grep -n "def |class |INSERT INTO|FROM " path/to/job.py
  - frontend（如 Vue/JS）：find {code_roots.frontend}/src -name "*.vue" | xargs grep -l "{功能关键词}"；grep -rn "axios|router|emit|props" path/to/component.vue]
<!-- /TECH_SPECIFIC -->
