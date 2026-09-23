<!--
  impact-analyzer Workbench 代码定位参考 —— 框架源模板
  ============================================================
  本文件是 framework 源，**不复制到实例项目**。
  project-setup 对每个启用 Workbench，读取本模板 → 填充 `<!-- TECH_SPECIFIC -->` 区块 →
  生成 {target}/skills/impact-analyzer/references/{Workbench key}.md。

  【N 个 Workbench 原则 —— 本模板第一铁律】
  本模板**不预设任何固定 Workbench 集合**。`<!-- TECH_SPECIFIC -->` 区块给出的是一组**Workbench 无关的问题/维度**，
  project-setup 在 Phase 2 采集本 Workbench 时按**该 Workbench 的 tech_stack** 派生答案。
  区块内的 backend / data / frontend 仅为**示意答案形态**（非枚举）；遇到任何其他 Workbench
  （python / ml / mobile / infra ...），按该 Workbench 自己的 tech_stack 派生，**严禁把新 Workbench 硬塞进某个示例 Workbench**。

  填充原则：用 config 落盘值；无法确定标 `TODO: 根据实际情况填写`，不留空白、不编造。
  本模板只承载 ② 技术绑定内容；① 分析纪律/产出结构/验证回路在 SKILL.md，本文件不重复。
-->

#  {{Workbench_NAME}} Workbench — 影响分析代码定位参考

> 本文件由 impact-analyzer 在 role={{Workbench_NAME}} 时加载，回答"本 Workbench 怎么定位与 PRD 相关的代码"，
> 与 SKILL.md 的 Step 1 配合使用。具体仓库结构以 `{code_roots.{{Workbench_NAME}}}/AGENTS.md` 为准。

---

## 代码定位策略（对应 SKILL.md Step 1）

<!-- TECH_SPECIFIC: 代码定位命令与入口策略 - project-setup 按本 Workbench tech_stack 派生 -->

[为本 Workbench 回答：①用什么 grep/find/head 命令，按本 Workbench 文件类型与目录约定定位与 PRD 关键词相关的代码
②本 Workbench 定位时应优先关注哪些入口（决定从哪里展开影响分析）。

示例（仅示意答案形态，非枚举；其他 Workbench 按其文件类型/目录结构派生对应命令）：

- backend（如 Java）：
  grep -r "{PRD 关键词}" {code_roots.backend} --include="*.java" -l
  find {code_roots.backend} -name "*{模块名}*" -not -path "*/target/*"
  入口优先：Controller/Facade → Manager/Service → Mapper
- data（如 Python/SQL）：
  grep -r "{表名/字段名}" {code_roots.data} --include="*.py" --include="*.sql" -l
  find {code_roots.data} -name "*{模块名}*" | grep -v __pycache__
  入口优先：Job 主类/ETL 脚本 → 上游源/目标表写入
- frontend（如 Vue/JS）：
  grep -r "{组件名/接口路径}" {code_roots.frontend} --include="*.vue" --include="*.js" -l
  入口优先：页面/视图 → 子组件 → API 调用封装]

<!-- /TECH_SPECIFIC -->
