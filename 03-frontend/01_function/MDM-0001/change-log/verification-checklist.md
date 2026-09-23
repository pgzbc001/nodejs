# 设计-代码一致性验证清单 — mdm-frontend

> 核对基准：`03-frontend/01_function/MDM-0001/design/design.md` 与 `task/tasks.md`
> 核对方式：静态审查（文件清单、路由/组件比对、导入链核对）　执行时间：2026-09-23

## 1. 静态核对结果（已执行）

| # | 核对项 | 基准文档 | 代码证据 | 结论 |
|---|--------|----------|----------|------|
| 1 | 页面结构 7 路由 | design 2.1 页面结构 | router/index.js：/dashboard、/models、/models/:id/design（meta.activeMenu=/models）、/data、/push、/quality、/logs | ✅ 一致 |
| 2 | 目录结构 | design 2.2 目录结构 | src/ 下 api(9) / stores(1) / router(1) / layouts(1) / views(7) / components(10) / styles(1) + 脚手架 6 文件，共 32 文件 | ✅ 一致 |
| 3 | 组件清单 10 个 | design 2.2 / 四、组件设计 | CategoryTreePanel / DynamicField / DynamicFormDrawer / SimilarityDialog / QualityPanel / VersionDrawer / DiffTable / ImportExportDialog / RefSelectDialog / ModelVersionDrawer 全部就位，Props/事件与设计表一致 | ✅ 一致 |
| 4 | 技术决策 | design 一、决策表 | Vue 3 `<script setup>` + Vite 5 + Element Plus + Pinia + Router 4 + Axios 实例统一封装（baseURL `/api/v1`） | ✅ 一致 |
| 5 | 接口封装覆盖 | api-mdm-0001.md 45 接口 | 8 个 api 模块共 52 函数，覆盖后端 52 端点（含兼容端点） | ✅ 覆盖 |
| 6 | 提交闭环链路 | design 五、关键调用链 | DynamicFormDrawer：必填 → check-duplicate → SimilarityDialog → check-quality → QualityPanel（CRITICAL 禁提交）→ POST/PUT（ignoredWarnings + ignoreReason） | ✅ 一致 |
| 7 | 错误处理设计 | design 六 | http.js：code≠0 → ElMessage；40300 附「请切换角色」提示；40000 detail map → 字段级红字（applyFieldErrors）；blob 下载 a[download] | ✅ 一致 |
| 8 | 角色模拟 | REQ-FED01 | 顶栏 4 角色切换（Pinia + localStorage）；X-User-Id/Name/Role 注入；按钮级 can() 权限；用户名 ASCII 防 header 乱码 | ✅ 一致 |
| 9 | 动态渲染 | REQ-FED04/05 | DynamicField 控件映射（TEXT/LONG_TEXT/NUMBER/DATE/REF/SELECT+multiSelect）；DataMaintenance 表格列由 listFields 驱动、筛选由 searchFields 驱动 | ✅ 一致 |
| 10 | REF 字段语义 | design 3.4 | FieldDef.domainSource=REF → DynamicField 触发 RefSelectDialog → 回填被引用数据 code 字符串；multiSelect 数组拼接 | ✅ 一致 |
| 11 | 导入导出 | REQ-FED07 | ImportExportDialog：模板/导出/拖拽上传/结果摘要（total/successCount/failCount）/错误明细（row/field/message）+ 下载 | ✅ 一致 |
| 12 | 40909 强制语义 | api 契约 40909 | DataMaintenance 禁用/删除捕获 40909 → 二次确认「强制操作」→ force=true 重试 | ✅ 一致 |
| 13 | 版本管理 | REQ-FED03/06 | VersionDrawer / ModelVersionDrawer：selection 限 2 → diff 弹窗 → 回滚（生成新版本）；ModelDesigner 版本 Tab 同能力 | ✅ 一致 |

## 2. 待环境可用的执行验证（未执行）

| # | 验证项 | 命令 | 状态 |
|---|--------|------|------|
| 1 | 依赖安装 | `cd mdm-frontend; npm install` | ⏸ 未执行 — 本机无 Node / npm（`Get-Command node,npm` 无结果；`C:\Program Files\nodejs` 不存在） |
| 2 | 生产构建 | `npm run build` | ⏸ 未执行（同 1），预期 dist 产物生成、无编译告警 |
| 3 | 联调启动 | `npm run dev` + 后端 `mvn spring-boot:run` | ⏸ 未执行；预期 5173 端口 + `/api` 代理 8080 正常 |
| 4 | 手工 E2E | 浏览器走查（角色切换→模型设计→数据新增查重→质量忽略→推送→协同） | ⏸ 未执行（依赖 1~3） |

> 结论：前端代码已按 design/tasks 完整落盘并通过静态核对；上表执行验证需在具备 **Node 18+ / npm 9+** 的机器上按命令补跑，结果可追加至本文件。
