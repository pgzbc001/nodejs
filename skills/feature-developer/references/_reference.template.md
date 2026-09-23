<!--
  feature-developer Workbench 开发参考 —— 框架源模板
  ============================================================
  本文件是 framework 源，**不复制到实例项目**。
  project-setup 对每个启用 Workbench，读取本模板 → 填充每个 `<!-- TECH_SPECIFIC -->` 区块 →
  生成 {target}/skills/feature-developer/references/{Workbench key}.md。

  【N 个 Workbench 原则 —— 本模板第一铁律】
  本模板**不预设任何固定 Workbench 集合**。每个 `<!-- TECH_SPECIFIC -->` 区块给出的是一组**Workbench 无关的问题/维度**，
  project-setup 在 Phase 2 采集本 Workbench 时按**该 Workbench 的技术栈、分层模式、构建/测试/lint 命令**直接填充本文件，
  并以 Claude 对该技术栈的标准知识补充。这些都是 Workbench 知识，直接落 references，**不进 config**。

  区块内出现的 backend / data / frontend 仅为**示意答案形态**（来自种子项目三 Workbench），
  **不是枚举、不是查表项**。遇到任何其他 Workbench（如 python / ml / mobile / infra / go ...），
  一律按该 Workbench 自己的 tech_stack 派生，**严禁把新 Workbench 硬塞进某个示例 Workbench**。

  填充原则：用 Phase 2 采集所得（含 VERIFY 命令，直接写入本文件）；Claude 知识补充；
  无法确定标 `TODO: 根据实际情况填写`，不留空白、不编造。
  本模板只承载 ② 技术绑定内容；① 通用循环/纪律/门控在 SKILL.md，本文件不重复。
-->

# {{Workbench_NAME}} Workbench — 功能开发参考

> 本文件由 feature-developer 在 role={{Workbench_NAME}} 时加载，回答"本 Workbench 具体怎么定位/实现/验证"，
> 与 SKILL.md 的 Gather-Act-Verify 循环配合使用。
> 注意：本文件是**Workbench 级**参考；具体仓库的结构与约定以 `{code_roots.{{Workbench_NAME}}}/AGENTS.md` 为准（更细、更权威）。

---

## 1. GATHER 策略（对应 SKILL.md Step 1）

<!-- TECH_SPECIFIC: GATHER 定位命令与收集清单 - project-setup 按本 Workbench tech_stack 派生 -->
[为本 Workbench 回答：①用什么 find/grep/head 命令定位相关代码（按本 Workbench 文件类型与目录约定）②每次 GATHER 应覆盖哪些关键单元（收集清单）。

 示例（仅示意答案形态，非枚举；其他 Workbench 按其文件类型/目录结构派生对应命令与清单）：
  - backend（如 Java/Maven）：
      find {code_roots.backend}/ -name "XxxManager.java" -not -path "*/target/*"
      grep -r "class XxxManager" {code_roots.backend}/ --include="*.java" -l
      收集清单：入口(Controller/Facade)、业务逻辑(Manager/Service)、数据访问(Mapper/DAO)、实体与 DTO、现有测试
  - data（如 Python/Spark/Flink）：
      find {code_roots.data}/ -name "*.py" -o -name "*.sql" | grep -v __pycache__
      grep -r "{表名或关键业务词}" {code_roots.data}/ --include="*.py" --include="*.sql" -l
      收集清单：Job 主类/ETL 脚本入口、上游数据源(Topic/上游表)、目标表写入逻辑、字段引用、现有测试
  - frontend（如 Vue/JS）：
      find {code_roots.frontend}/src -name "*.vue" -o -name "*.js" | xargs grep -l "{功能关键词}"
      收集清单：页面/视图、子组件、API 调用封装、路由配置、Store 模块]
<!-- /TECH_SPECIFIC -->

---

## 2. ACT 实现顺序与分层职责（对应 SKILL.md Step 2）

<!-- TECH_SPECIFIC: ACT 实现顺序与分层职责 - project-setup 按本 Workbench layer_pattern 派生 -->
[为本 Workbench 回答：①实现顺序（从底层到上层的推荐次序）②各层的职责边界（每层只做什么、不做什么）。按本 Workbench 分层模式派生。

 示例（仅示意，非枚举）：
  - backend：顺序 Entity→Mapper/DAO→Manager/Service→Facade→Controller→DTO；
    Controller 只做校验与 DTO 转换 / Manager 承载核心业务 / Mapper 只查不判 / Entity 含审计字段
  - data：顺序 Schema 变更→Source→Transform→Sink→历史回刷 Job→数据质量脚本；
    业务逻辑集中在 Transform，不放 Source/Sink；Sink 确认写入模式与分区；回刷保证幂等可分片
  - frontend：顺序 路由→页面骨架→子组件→API 封装→状态管理→交互逻辑；
    View 只布局组合 / Component 经 Props-Emit 通信不直连 API / API 统一走封装层 / Store 仅跨组件共享状态]
<!-- /TECH_SPECIFIC -->

---

## 3. VERIFY 命令（对应 SKILL.md Step 3）

<!-- TECH_SPECIFIC: VERIFY 命令 - Phase 2 采集本 Workbench 的构建/测试/lint 命令，直接写入本文件 -->
[为本 Workbench 给出两级验证命令（命令在 Phase 2 采集本 Workbench 技术栈时确定，**直接写在本文件**，不引用 config）：
  · 必须通过（失败即 BLOCK）：编译/构建/语法检查
  · 尽力执行（失败按 SKILL.md 失败处理表）：单元测试 / lint

 示例（仅示意，非枚举；命令为 Phase 2 采集所得，直接写出）：
  - backend：必须通过 `mvn compile -pl {service_module} -am -q`（{service_module} 从 AGENTS.md 读）；
    尽力执行 `mvn test -pl {service_module} -Dtest={XxxTest} -q`
  - data：必须通过 `python3 -m py_compile {changed_script}.py`；
    尽力执行 `python3 -m pytest {code_roots.data}/tests/ -q`；Flink 任务需提交测试环境，BLOCK 等人工确认
  - frontend：必须通过 `npm run build`；尽力执行 `npm run lint`]
<!-- /TECH_SPECIFIC -->

---

## 4. Workbench 红线 / 边界检查清单（对应 SKILL.md Guardrail「高风险修改」「Workbench 模式混用」与 VERIFY 边界检查）

<!-- TECH_SPECIFIC: Workbench 红线与边界检查 - project-setup 按本 Workbench tech_stack 与 architectural_constraints 派生 -->
[为本 Workbench 列出：①必须 BLOCK 的技术硬红线 ②VERIFY 阶段的边界检查清单 ③HIGH_RISK 修改的判定清单。
 （这些条目从本 Workbench tech_stack 与项目 architectural_constraints.md 派生；项目级通用约束仍归 constraints 文件。）

 示例（仅示意，非枚举）：
  - backend：金额禁 FLOAT/DOUBLE(须 DECIMAL/BIGINT)；新表须含必备审计字段；跨服务调用走声明式接口不直连 DB；
    边界：空值/空集合返回约定；HIGH_RISK：金额计算/状态机/租户路由
  - data：宽表禁套应用审计字段；写入模式与分区须与现有表一致；回刷须幂等；
    边界：来源缺失/空值处理；HIGH_RISK：历史回刷影响存量数据
  - frontend：禁组件内直连 axios（走封装层）；禁硬编码接口 Workbench 名；
    边界：Loading/错误态覆盖；HIGH_RISK：涉及鉴权/权限渲染的改动]
<!-- /TECH_SPECIFIC -->
