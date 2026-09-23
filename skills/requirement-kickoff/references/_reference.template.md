<!--
  requirement-kickoff Workbench 识别信号参考 —— 框架源模板
  ============================================================
  本文件是 framework 源，**不复制到实例项目**。
  project-setup 对每个启用 Workbench，读取本模板 → 填充 `<!-- TECH_SPECIFIC -->` 区块 →
  生成 {target}/skills/requirement-kickoff/references/{Workbench key}.md。

  【N 个 Workbench 原则】
  本模板不预设任何固定 Workbench 集合。每个启用 Workbench 生成一份本文件，
  各承载"扫 PRD 时如何认出本 Workbench"的识别信号；信号由该 Workbench 的 tech_stack / 职责派生。
  requirement-kickoff 运行时读取本 skill references/ 下**全部** {Workbench}.md，汇成识别信号表后逐 Workbench 匹配。

  区块内出现的 backend / data / frontend 仅为**示意答案形态**，不是枚举、不是查表项。
  遇到任何其他 Workbench（python / ml / mobile / infra / go ...），一律按该 Workbench 自己的 tech_stack 派生。

  填充原则：用 config/采集落盘值 + Claude 对该技术栈的标准知识；无法确定标 `TODO: 根据实际情况填写`，不留空白、不编造。
-->

# {{workbench_NAME}} Workbench — 需求识别信号

> 本文件由 requirement-kickoff 的 detect-Workbench 子环节加载（连同其他 Workbench 的同名文件一起读取），
> 用于扫描 PRD 时判断需求是否涉及本 Workbench。

<!-- TECH_SPECIFIC: 识别信号 - project-setup 按本 Workbench tech_stack 派生 -->
[为本 Workbench 给出一组**识别信号**：PRD 中出现哪些关键词 / 功能形态 / 改动类型时，判定需求涉及本 Workbench。
 信号由本 Workbench 的 tech_stack / 职责派生。

 示例（仅示意答案形态，非枚举）：
  - backend：HTTP API、数据库 CRUD、后端服务逻辑、事务 / 校验
  - data：ETL 管道、数据仓库、数据流处理、数据湖、宽表 / 血缘
  - frontend：UI 组件、前端交互、页面展示、用户界面改动]
<!-- /TECH_SPECIFIC -->
