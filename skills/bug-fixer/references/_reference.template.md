<!--
  bug-fixer Workbench 定位/验证参考 —— 框架源模板
  ============================================================
  本文件是 framework 源，**不复制到实例项目**。
  project-setup 对每个启用 Workbench，读取本模板 → 填充每个 `<!-- TECH_SPECIFIC -->` 区块 →
  生成 {target}/skills/bug-fixer/references/{Workbench key}.md。

  【N 个 Workbench 原则 —— 本模板第一铁律】
  本模板**不预设任何固定 Workbench 集合**。每个 `<!-- TECH_SPECIFIC -->` 区块给出的是一组**Workbench 无关的问题/维度**，
  project-setup 在 Phase 2 采集本 Workbench 时按**该 Workbench 的技术栈、测试/构建命令**直接填充本文件（Workbench 知识直接落 references，不进 config）。

  区块内出现的 backend / data / frontend 仅为**示意答案形态**（来自种子项目三 Workbench），
  **不是枚举、不是查表项**。遇到任何其他 Workbench（如 python / ml / mobile / infra / go ...），
  一律按该 Workbench 自己的 tech_stack 派生，**严禁把新 Workbench 硬塞进某个示例 Workbench**。

  填充原则：用 config 落盘值；Claude 知识补充；无法确定标 `TODO: 根据实际情况填写`，不留空白、不编造。
  本模板只承载 ② 技术绑定内容；① 修复纪律/Phase 流程/验证回路在 SKILL.md，本文件不重复。
-->

# {{Workbench_NAME}} Workbench — Bug 修复定位/验证参考

> 本文件由 bug-fixer 在 role={{Workbench_NAME}} 时加载，回答"本 Workbench 怎么定位代码、怎么跑回归"，
> 与 SKILL.md 的 Phase 1 / Phase 3 配合使用。具体仓库结构以 `{code_roots.{{Workbench_NAME}}}/AGENTS.md` 为准。

---

## 1. 代码定位命令（对应 SKILL.md Phase 1）

<!-- TECH_SPECIFIC: 代码定位命令 - project-setup 按本 Workbench tech_stack 派生 -->
[为本 Workbench 回答：从 Bug 描述关键词出发，用什么 grep/find 命令按本 Workbench 文件类型定位嫌疑代码。

 示例（仅示意答案形态，非枚举；其他 Workbench 按其文件类型派生）：
  - backend（如 Java）：grep -r "{接口路径/方法名/业务关键词}" {code_roots.backend} --include="*.java" -l
  - data（如 Python/SQL）：grep -r "{表名/字段名/任务名}" {code_roots.data} --include="*.py" --include="*.sql" -l
  - frontend（如 Vue/JS）：grep -r "{组件名/接口路径/页面关键词}" {code_roots.frontend} --include="*.vue" --include="*.js" -l]
<!-- /TECH_SPECIFIC -->

---

## 2. 回归验证命令（对应 SKILL.md Phase 3）

<!-- TECH_SPECIFIC: 回归验证命令 - Phase 2 采集本 Workbench 的测试/构建命令，直接写入本文件 -->
[为本 Workbench 给出运行触碰模块相关测试的命令（命令在 Phase 2 采集时确定，**直接写在本文件**，不引用 config；无测试框架时退化为构建/语法检查）。

 示例（仅示意，非枚举；命令为 Phase 2 采集所得，直接写出）：
  - backend：mvn test -pl {service_module} -Dtest={XxxTest} -q（无对应单测时 mvn compile 兜底）
  - data：python3 -m pytest {code_roots.data}/tests/ -q（无测试时 python3 -m py_compile 兜底）；Flink 任务需测试环境，BLOCK 等人工确认
  - frontend：npm run test（无单测时 npm run build 兜底）]
<!-- /TECH_SPECIFIC -->
