<!--
  test-data-script-generator Workbench 造数参考 —— 框架源模板
  ============================================================
  本文件是 framework 源，**不复制到实例项目**。
  project-setup 对每个启用 Workbench，读取本模板 → 填充每个 `<!-- TECH_SPECIFIC -->` 区块 →
  生成 {target}/skills/test-data-script-generator/references/{Workbench key}.md。

  【N 个 Workbench 原则 —— 本模板第一铁律】
  本模板**不预设任何固定 Workbench 集合**。每个 `<!-- TECH_SPECIFIC -->` 区块给出的是一组**Workbench 无关的问题/维度**，
  project-setup 在 Phase 2 采集本 Workbench 时按**该 Workbench 的 tech_stack（数据存储类型）** 派生答案。

  区块内出现的 backend / data / frontend 仅为**示意答案形态**（来自种子项目三 Workbench），
  **不是枚举、不是查表项**。遇到任何其他 Workbench（如 python / ml / mobile / infra / go ...），
  一律按该 Workbench 自己的数据存储与触发方式派生，**严禁把新 Workbench 硬塞进某个示例 Workbench**。

  填充原则：用 config 落盘值；Claude 知识补充；无法确定标 `TODO: 根据实际情况填写`，不留空白、不编造。
  本模板只承载 ② 技术绑定内容；① 流程/前缀纪律/runnable 机制在 SKILL.md，本文件不重复。
-->

# {{Workbench_NAME}} Workbench — 测试数据脚本参考

> 本文件由 test-data-script-generator 在 role={{Workbench_NAME}} 时加载，回答"本 Workbench 各数据源怎么造数/验证/清数"。

---

## 0. Workbench 画像

<!-- TECH_SPECIFIC: Workbench 画像 - project-setup 按本 Workbench tech_stack 派生 -->
[为本 Workbench 回答：①本 Workbench**是否有自主造数能力**（有自己的数据存储 → 有；纯消费下游接口、不直接持有数据 → 无，执行时 BLOCK 引导联系数据提供 Workbench）②本 Workbench 涉及的**数据源清单**（数据库/搜索引擎/缓存/消息队列/数仓表等）③造数是否**随单元触发方式分支**（如流式单元 vs 批量单元）。

 示例（仅示意答案形态，非枚举；其他 Workbench 按其实际数据存储派生）：
  - backend：有自主造数；数据源 = 关系数据库 + 搜索引擎 + 缓存；无触发分支
  - data：有自主造数；数据源 = 消息队列 + 上游表 + 数仓表；**有触发分支**（流式单元造数=发消息 / 批量单元造数=写上游表）
  - frontend：**无自主造数能力**（纯消费后端接口）→ 本 Workbench 执行时 BLOCK]
<!-- /TECH_SPECIFIC -->

---

## 1. 造数语法 + 各数据源前缀格式（对应 SKILL.md Step 2 与「数据前缀规则」）

<!-- TECH_SPECIFIC: 造数语法与前缀 - project-setup 按本 Workbench 数据源派生 -->
[为本 Workbench 每个数据源回答：①造数语句的语法模板（带模块前缀字段）②该数据源的前缀格式（用于清数 WHERE）。

 示例（仅示意，非枚举；其他 Workbench 按其数据源派生）：
  - backend 关系库：INSERT INTO {table}(...,created_by) VALUES (...,'{前缀}_TEST')；前缀=created_by 字段
    搜索引擎：POST /{index}/_bulk，_id 用 '{前缀}-AT{序号}'
    缓存：SET test:{前缀}:{业务}:{序号} '{json}' EX {ttl}
  - data 流式单元：kafka-console-producer 发消息，业务 ID 用 '{前缀}-K{序号}'
    批量单元：INSERT INTO {上游表}(...,created_by) VALUES (...,'{前缀}_TEST')；执行 Job 触发命令
    数仓表标识：'{前缀}-H{序号}']
<!-- /TECH_SPECIFIC -->

---

## 2. 验证查询（对应 SKILL.md Step 3）

<!-- TECH_SPECIFIC: 验证查询 - project-setup 按本 Workbench 数据源派生 -->
[为本 Workbench 每个数据源回答：造数后如何查询确认数据正确写入（带预期记录数/字段值断言）。

 示例（仅示意，非枚举）：
  - backend：SELECT ... FROM {table} WHERE created_by='{前缀}_TEST' ORDER BY id DESC LIMIT 10；GET /{index}/_doc/{前缀}-AT001
  - data：SELECT ... FROM {数仓表} WHERE business_id LIKE '{前缀}%' LIMIT 10（流式单元注意造数后有延迟，需等待再查）]
<!-- /TECH_SPECIFIC -->

---

## 3. 清数语法（对应 SKILL.md Step 4，幂等、反序）

<!-- TECH_SPECIFIC: 清数语法 - project-setup 按本 Workbench 数据源派生 -->
[为本 Workbench 每个数据源回答：如何用模块前缀做**幂等**清数（精确 WHERE，可重复执行），并提示反序清理。

 示例（仅示意，非枚举）：
  - backend：DELETE FROM {子表} WHERE created_by='{前缀}_TEST'; DELETE FROM {主表} WHERE created_by='{前缀}_TEST';
    搜索引擎：POST /{index}/_delete_by_query {"query":{"prefix":{"_id":"{前缀}-"}}}
    缓存：scan --pattern "test:{前缀}:*" | xargs DEL
  - data：DELETE FROM {数仓表} WHERE business_id LIKE '{前缀}%'（不支持 DELETE 的表用软删除 UPDATE is_deleted=1）；消息队列无需清数（清目标存储即可）]
<!-- /TECH_SPECIFIC -->
