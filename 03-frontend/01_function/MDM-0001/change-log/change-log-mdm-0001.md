# 变更记录 — mdm-frontend（主数据管理平台）

## v1.0 — 2026-09-23

**变更类型：** 代码实现

**变更内容：**
- 脚手架：package.json（Vue 3.4 / Vite 5 / Element Plus 2.7 / Pinia / Vue Router 4 / Axios / @element-plus/icons-vue）、vite.config.js（dev proxy `/api` → `localhost:8080`）、index.html、main.js、App.vue、global.css
- 基础设施：api/http.js（baseURL `/api/v1`、X-User-* 注入、ApiResponse 解包、40300 提示、blob 下载 + `filename*=UTF-8''` 解析）、8 个 api 模块（category / model / data / io / push / quality / log / stats）、stores/user.js（4 演示角色 + localStorage 持久化 + can() 权限判断）、router/index.js（7 路由）
- 布局：MainLayout（侧边菜单 6 项 + 顶栏角色切换 + `meta.activeMenu` 高亮）
- 组件 10 个：
  - DynamicField（FieldDef 18 属性 → 控件映射：TEXT/LONG_TEXT/NUMBER/DATE/REF/SELECT）
  - DynamicFormDrawer（字段分组 Collapse 渲染 + 必填校验 → 查重 → SimilarityDialog → 质量校验 → QualityPanel 忽略原因 → POST/PUT 闭环 + 40000 字段级错误映射 + REF 弹窗回填）
  - SimilarityDialog（相似度进度条 + 使用已有 / 继续提交）
  - QualityPanel（CRITICAL/WARNING/INFO 三级分组 + 忽略勾选 + 原因必填）
  - DiffTable（差异高亮）、CategoryTreePanel（树 CRUD + 搜索 + 防环 + 展开折叠）
  - RefSelectDialog（refModelCode → 模型解析 → 分页选择 → 回填 code）
  - VersionDrawer / ModelVersionDrawer（版本列表 + 两版本 diff + 回滚）
  - ImportExportDialog（模板下载 / 导出 / 拖拽导入 / 结果摘要 / 错误明细下载）
- 页面 7 个：
  - Dashboard（11 指标卡 + 最近操作列表）、ModelList（分类树 + 模型表格 + 空白/继承创建 + 上线/版本）、
  - ModelDesigner（5 Tab：基础信息 / 字段配置（含上线锁定 disabled）/ 扩展配置 / 编码规则（段编辑 + 预览）/ 版本历史）
  - DataMaintenance（分类-模型树 + view 元数据驱动表格 + 动态筛选 + 行选工具栏 + 40909 强制操作 + use-existing 定位）
  - PushCenter（3 Tab：数据×系统预检/执行推送 / 协同工单确认驳回 / 推送日志）、
  - QualityRules（规则 CRUD + 表达式可视化编辑）、OperationLogs（时间范围 + 类型筛选 + 详情 JSON 弹窗）
- 契约对齐：全部对接 `/api/v1`；filters 传 URL 编码 JSON；统计 11 指标；错误码 40909 二次强制确认；角色名用 ASCII（model_admin 等）避免中文请求头乱码

**影响范围：**
- `mdm-frontend/`（32 个源文件）
- `03-frontend/01_function/MDM-0001/`（design / task 文档）

**关联任务：** MDM-0001 · REQ-FED01 ~ REQ-FED07（03-frontend/01_function/MDM-0001/task/tasks.md）

**验证状态：** 本机无 Node / npm 环境，未执行 `npm install && npm run build`；静态设计-代码一致性核对见 [verification-checklist.md](./verification-checklist.md)

---

<!--
变更记录规则：版本号递增（v1.0, v1.1...）；最新版本在文件顶部；每次改动后追加。
-->
