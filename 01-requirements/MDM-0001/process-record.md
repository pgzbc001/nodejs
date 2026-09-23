---
title: "MDM-0001 交付过程记录（CBCC AI SOP 2.0）"
type: process-record
project: "MDM-0001 主数据管理平台 Demo"
status: "测试完成（最终阶段）"
date: "2026-09-23"
---

# MDM-0001 交付过程记录（CBCC AI SOP 2.0 / Dev Harness Framework）

> **用途**：作业交付物之二 —— 保留「方法论 → 工具 → 代码 → 交付物」全流程的过程记录（关键 Prompt、决策与迭代记录），可直接导入看板按 Markdown 渲染查看。
> **自包含**：本文档不依赖外部链接，章节结构即看板目录。
> **关联交付物**：`01-requirements/MDM-0001/`（需求空间）、`02-backend/01_function/MDM-0001/`（后端工作台）、`03-frontend/01_function/MDM-0001/`（前端工作台）、`mdm-backend/`、`mdm-frontend/`。

---

## 0. 作业要求对照表

| # | 作业要求 | 状态 | 对应交付物 |
|---|----------|------|------------|
| 1 | 初始化工作台与前后端项目，按《主数据管理需求文档》完成完整功能模块 | ✅ 完成 | 需求空间 + 双工作台文档 + MDM 全栈代码 |
| 2 | 全流程使用 CBCC AI SOP2.0 与 Dev Harness Framework；保留过程记录 | ✅ 完成 | 六阶段文档链（见 §2）+ **本记录** |
| 3 | 技术栈：前端任意框架 / 后端 Java / 数据 SQLite | ✅ 完成 | Vue 3 + Spring Boot 3.2.5（Java 17）+ SQLite |
| 4 | 部署 BTP / Cloud Foundry，提交可访问 URL | ⏭ 跳过（用户决定） | 决策见 §4 D11 |
| 5 | 交付物：URL / SOP2.0 过程记录 / 看板（项目结构 + 文件预览） | 部分完成 | 过程记录 = 本文件（导入看板查看）；URL 随部署跳过 |

---

## 1. 交付物结构索引（看板导航）

```text
01-requirements/MDM-0001/
├── requirements_plan.md      # 需求澄清计划（范围 / 角色 / 澄清项）
├── requirements.md           # 需求规格（REQ-BKD01~10、REQ-FED01~07 共 17 条）
├── prd.md                    # 产品需求文档
└── process-record.md         # ★ 本文件：SOP2.0 过程记录（导入看板查看）
02-backend/01_function/MDM-0001/
├── sql/sql-mdm-0001-ddl.md   # 12 张表 DDL 契约
├── api/api-mdm-0001.md       # 9 组 45+ 接口契约
├── design/design.md          # 后端技术设计
├── task/tasks.md             # 后端任务拆解
└── change-log/
    ├── change-log-mdm-0001.md      # 后端变更记录（v1.0）
    └── verification-checklist.md   # 后端验证清单（13 项静态核对 + 4 项待执行）
03-frontend/01_function/MDM-0001/
├── design/design.md          # 前端技术设计
├── task/tasks.md             # 前端任务拆解
└── change-log/
    ├── change-log-mdm-0001.md      # 前端变更记录（v1.0）
    └── verification-checklist.md   # 前端验证清单（13 项静态核对 + 4 项待执行）
mdm-backend/                  # Java 17 + Spring Boot 3.2.5 + SQLite + Apache POI
└── 9 Controller / 10 Service / 12 表 / 52 端点 / 4 测试类 34 用例
mdm-frontend/                 # Vue 3.4 + Vite 5 + Element Plus 2.7 + Pinia
└── 7 页面 / 10 组件 / 8 API 模块 / 7 路由
```

---

## 2. 全流程总览（SOP 六阶段）

| 阶段 | SOP 环节（Skill） | 产物 | 状态 |
|------|------------------|------|------|
| ① 需求澄清 | requirement-kickoff → requirements-analyst → prd-writer | requirements_plan.md → requirements.md（17 条 REQ）→ prd.md | ✅ |
| ② 方案设计 | solution-designer（SQL / API / 技术设计 / 任务拆解） | sql-mdm-0001-ddl.md（12 表）、api-mdm-0001.md（45+ 契约）、design.md ×2、tasks.md ×2 | ✅ |
| ③ 编码实现 | feature-developer（后端工作台 + 前端工作台） | mdm-backend（9 Controller / 52 端点 / 12 表）、mdm-frontend（32 文件） | ✅ |
| ④ 测试验证 | dev-self-test | 单元测试 4 类 34 用例；静态设计-代码核对清单 ×2 | ⏸ 执行待环境（§5 I5） |
| ⑤ 交付物 | change-log + verification-checklist | change-log ×2、verification-checklist ×2、README §8 交付说明 | ✅ |
| ⑥ 部署上线 | BTP / Cloud Foundry | —— | ⏭ 用户决定跳过（§4 D11） |

---

## 3. 关键 Prompt 记录

| # | 阶段 | 关键 Prompt（原文节选） | 意图与响应 |
|---|------|------------------------|-----------|
| P1 | 立项 | "请按照主数据管理需求文档.md 和 README.md 实现，并按照 CBCC 项目 AI 交付手册 SOP_version2.0.html 里边的框架实现代码" | 以 SOP2.0 + Dev Harness 交付 MDM 全栈；产出四阶段实施计划（A 文档 / B 后端 / C 前端 / D 收尾） |
| P2 | 计划批准 | "Implement the plan as specified, it is attached for your reference. Do NOT edit the plan file itself." | 批准执行计划，锁定 Phase A→D 顺序执行 |
| P3 | 过程推进 | "继续执行" | 分段续跑：Phase B 后端编码 → Phase C 前端编码 → Phase D 收尾 |
| P4 | 作业补强 | "（作业）用 Dev Harness Framework 完成一个 Demo 项目全栈开发并发布至 BTP……提供看板用于查看项目结构，支持文件预览" | 触发查缺补漏：梳理缺口（BTP 部署 / 过程记录 / 看板） |
| P5 | 查缺补漏 | "按照这个查缺补漏" | 对照作业要求逐条盘点交付物与缺口清单 |
| P6 | 范围调整 | "跳过 BTP/Cloud Foundry 部署" | 部署环节移出范围（§4 D11） |
| P7 | 范围调整 | "不是实现看板的模块，需要完善过程记录，以便我能导入看板查看" | 不新增看板代码；改为产出可导入看板的完整过程记录（本文件） |

---

## 4. 关键决策记录

| # | 决策点 | 结论 | 依据 / 权衡 |
|---|--------|------|-------------|
| D1 | 工作台划分 | 需求空间（01-requirements）+ 后端工作台（02-backend）+ 前端工作台（03-frontend），需求编号 MDM-0001 | Dev Harness「N-Workbench」模型；SQLite 单库无需独立 DB 工作台 |
| D2 | 技术栈 | Java 17 + Spring Boot 3.2.5 + SQLite（hibernate-community-dialects）；Vue 3.4 + Vite 5 + Element Plus 2.7 + Pinia | 满足作业技术栈要求（Java / SQLite / 任意前端） |
| D3 | 接口契约 | 统一前缀 `/api/v1`；响应 `{code,message,data}`；业务错误 HTTP 200 携带错误码；40000 时 data = 字段级提示 map | 前端拦截器统一解包；字段级错误直接渲染表单红字 |
| D4 | 认证简化 | `X-User-Id / X-User-Name / X-User-Role` 请求头 + `@RequirePermission` 角色校验；4 角色演示切换 | 演示环境不引入真实认证；用户名 ASCII 规避 header 中文乱码 |
| D5 | 元数据驱动 | 模型 `field_defs`（18 属性）/ `code_rules` / `ext_config` 以 JSON 列存储，驱动动态表单 / 列表 / 筛选 | 新增字段零代码；上线后结构锁定（40903 白名单） |
| D6 | 编码规则 | FIXED / FIELD_REF / SEQ / MODEL_REF 四类段类型；MODEL_REF 仅允许第一段（40910） | 覆盖「前缀 + 字段 + 流水 + 引用」主数据编码场景 |
| D7 | 查重策略 | 相似度 Top5：≥60 提示、≥80 高度相似（弹窗确认后可继续） | 平衡录入效率与数据质量 |
| D8 | 质量规则 | 三级告警：CRITICAL 禁止提交；WARNING / INFO 勾选忽略 + 填写原因 | 表达式以 JSON 字符串存库（COMPLIANCE / CONSISTENCY / COMPLETENESS） |
| D9 | 版本管理 | 模型与数据均双表设计（主表 + version 表），支持版本 diff 与回滚 | 审计与可追溯诉求 |
| D10 | 交付一致性核对 | 逐接口 / 逐字段核对设计-代码契约（12 表、52 端点、15 错误码），差异即时回写 | Phase D 以静态核对替代不可执行的构建验证 |
| D11 | 部署范围 | 跳过 BTP / Cloud Foundry（用户指令，2026-09-23）；保留本地运行方案 | 本机无 JDK / Maven / Node / CF CLI 工具链，且用户决定不部署 |
| D12 | 过程记录载体 | 单一自包含 Markdown（本文件）：标题层级 = 看板目录，表格 = 决策 / 迭代视图 | 供用户直接导入看板按 Markdown 渲染查看 |

---

## 5. 迭代记录（问题 → 修复 → 验证）

| # | 问题 | 修复 / 处理 | 验证方式 |
|---|------|-------------|----------|
| I1 | 前后端路径契约不一致（前端 `/api`，后端 `/api/v1`） | 统一为 `/api/v1`：9 Controller + vite proxy + axios baseURL 对齐 | 全量端点逐一核对（52/52） |
| I2 | FieldDef 属性集前后端不一致 | 对齐 18 属性（domainSource / securityLevel / customRule 等），前端「完整配置」弹窗补齐 | 字段定义 JSON 双向核对 |
| I3 | 质量规则表达式格式漂移 | 表达式统一为 JSON 字符串；三类规则格式固化并同步前端解析展示 | QualityRuleService 分支逐条比对 |
| I4 | 单元测试编译错误 `ex.getCode()` | 改为 `ex.getErrorCode().getCode()` | CodeRuleEngineTest 修复核对 |
| I5 | 本机无 JDK / Maven / Node，无法构建与跑测 | 如实登记为「待环境可用」，给出补跑命令；以静态核对清单替代执行验证 | verification-checklist ×2（各 13 项静态核对） |
| I6 | 作业新要求缺口（部署 / 过程记录 / 看板） | 部署经用户决定跳过；看板改为「过程记录导入查看」；产出本记录 | 作业对照表（§0） |
| I7 | README 末尾残留空代码块 | Phase D 一并清理并新增 §8 交付说明 | git diff 核对（+33/-3） |
| I8 | 看板显示「需求状态：未启动」（缺票级进度日志与状态字段） | 补建 `01-requirements/MDM-0001/_progress.md`（需求启动 ✅ / 整体 🔄 进行中）；本记录补 `status: 启动` | 看板刷新 / 重新导入后显示「启动」 |
| I9 | 看板红项：根 `AGENTS.md` / `CLAUDE.md` 缺失、`local_profile.yaml` 为未填写模板 | 生成根 `AGENTS.md` + `CLAUDE.md`（内容一致）与 `mdm-backend/AGENTS.md`、`mdm-frontend/AGENTS.md`（仓库级 Agent 上下文）；填写 `local_profile.yaml`（dev-lead + code_roots）；精简 `_progress.md` 当前阶段 / 当前 Skill 字段以适配看板列展示 | 看板刷新后 AGENTS.md 转绿、需求卡片显示「进行中」 |
| I10 | 测试阶段收口（工具链定位 + 实跑范围决议） | 定位便携工具链（JDK17 / Maven / Node，`Desktop\Work\_tools\`）并可驱动构建；按用户决议跳过 `mvn test` / `npm run build` 本机实跑——测试阶段以「4 测试类 34 用例就绪 + 静态一致性核对清单」关闭，流水线推进至「测试完成（最终阶段）」 | 用户确认（2026-09-23） |

---

## 6. 复现与查看指引

**本地运行**

```bash
cd mdm-backend && mvn spring-boot:run            # http://localhost:8080（首次启动自动建库 + 种子数据）
cd mdm-frontend && npm install && npm run dev    # http://localhost:5173（/api 代理至 8080）
```

**看板查看**：将本文件（及 01 / 02 / 03 三个文档目录）导入看板，按 Markdown 渲染即可查看项目结构、关键 Prompt、决策与迭代记录。

> **工具链与补跑**：本机（Windows）系统环境无 JDK / Maven / Node，但已定位便携工具链（`C:\Users\wenbchen\Desktop\Work\_tools\` 下 jdk17 / maven / node）。仓库根提供一键脚本：`_run_backend_test.ps1`（mvn test）与 `_run_frontend_build.ps1`（npm install + build），依赖缓存在仓库 `.cache/`（已 gitignore），可随时补跑。

**演示链路**：右上角切换 4 个演示角色（数据管理员 / 录入员 / 审核员 / 系统管理员）→ 模型设计器定义元数据 → 上线锁定 → 数据维护（查重 → 质量校验 → 提交）→ 推送中心预检 / 推送 → 协同确认。

**已知限制**：
1. 测试阶段按用户决议关闭：自测用例（4 类 34 例）与静态核对清单就绪，本机 `mvn test` / `npm run build` 实跑跳过（便携工具链已定位、补跑脚本已备）；
2. SQLite 演示密钥与 Mock 下游系统为 v1.0 简化实现；
3. BTP / Cloud Foundry 部署按用户决定未执行，无对外可访问 URL。
