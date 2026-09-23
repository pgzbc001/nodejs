# 开发任务清单 — mdm-frontend

## 文档版本

| 版本 | 日期 | 修改说明 | 作者 |
|------|------|----------|------|
| v1.0 | 2026-09-23 | 初始版本 | TL |

---

## 任务总览

| 状态 | 阶段 | 任务数 | 预估工时 |
|------|------|--------|----------|
| [ ] | 1. 工程骨架与基础设施 | 3 | 3h |
| [ ] | 2. 模型定义页面 | 3 | 8h |
| [ ] | 3. 数据维护页面（动态渲染核心） | 4 | 12h |
| [ ] | 4. 推送/质量/日志/首页 | 4 | 6h |
| [ ] | 5. 构建验证 | 1 | 2h |
|     | 合计 | 15 | 31h |

---

## 阶段 1：工程骨架与基础设施

### Task 1.1：工程初始化
- **关联：** REQ-FED01
- **内容：** package.json（vue/vite/element-plus/pinia/vue-router/axios）、vite.config.js（proxy /api→8080）、index.html
- **预估：** 1h
- **前置依赖：** 无

### Task 1.2：路由 + 布局 + 用户 Store
- **关联：** REQ-FED01
- **内容：** router/index.js（7 路由）、MainLayout.vue（菜单/顶栏/角色切换）、stores/user.js（localStorage）
- **预估：** 1h
- **前置依赖：** Task 1.1

### Task 1.3：Axios 封装与 API 模块
- **关联：** REQ-FED01
- **内容：** http.js（拦截器/X-User 注入/错误提示/blob 下载）+ 8 个 api 模块
- **预估：** 1h
- **前置依赖：** Task 1.1

---

## 阶段 2：模型定义页面

### Task 2.1：CategoryTreePanel
- **关联：** REQ-FED02
- **内容：** 树渲染、新增同级/子级、编辑、删除（40901 提示）、搜索、展开/折叠
- **预估：** 3h
- **前置依赖：** Task 1.3

### Task 2.2：ModelList + 创建对话框
- **关联：** REQ-FED02
- **内容：** 模型表格、空白/继承创建、上线/下线、ModelVersionDrawer（列表/diff/回滚）
- **预估：** 3h
- **前置依赖：** Task 2.1

### Task 2.3：ModelDesigner（5 Tab）
- **关联：** REQ-FED03
- **内容：** 基础信息、字段配置可编辑表格（18 属性、上线锁定）、扩展配置、编码规则段编辑+预览、版本历史
- **预估：** 2h
- **前置依赖：** Task 2.2

---

## 阶段 3：数据维护页面（动态渲染核心）

### Task 3.1：DynamicField 组件
- **关联：** REQ-FED05
- **内容：** 六类控件映射（TEXT/LONG_TEXT/NUMBER/DATE/SELECT/REF）、多选、必填星号、禁用
- **预估：** 3h
- **前置依赖：** Task 1.3

### Task 3.2：DataMaintenance 主页面
- **关联：** REQ-FED04
- **内容：** 分类-模型树、view 元数据动态表格、动态筛选、分页、工具栏、行选择
- **预估：** 3h
- **前置依赖：** Task 3.1

### Task 3.3：DynamicFormDrawer + RefSelectDialog
- **关联：** REQ-FED05
- **内容：** 分组折叠表单、动态渲染、加密脱敏回显、引用弹窗选择
- **预估：** 3h
- **前置依赖：** Task 3.1

### Task 3.4：SimilarityDialog + QualityPanel + VersionDrawer + DiffTable
- **关联：** REQ-FED06
- **内容：** 相似度弹窗（使用已有/继续）、质量三级面板（忽略原因）、版本抽屉与差异表格
- **预估：** 3h
- **前置依赖：** Task 3.3

---

## 阶段 4：推送/质量/日志/首页

### Task 4.1：ImportExportDialog
- **关联：** REQ-FED07
- **内容：** 模板下载、导出、上传导入、结果摘要、错误明细下载
- **预估：** 2h
- **前置依赖：** Task 3.2

### Task 4.2：PushCenter
- **关联：** REQ-FED07
- **内容：** 数据/系统选择、预检结果表、执行推送、协同工单确认/驳回、推送日志
- **预估：** 2h
- **前置依赖：** Task 1.3

### Task 4.3：QualityRules + OperationLogs
- **关联：** REQ-FED07
- **内容：** 质量规则 CRUD 表格、操作日志筛选与详情
- **预估：** 1h
- **前置依赖：** Task 1.3

### Task 4.4：Dashboard
- **关联：** REQ-FED07
- **内容：** 统计卡片 + 最近操作
- **预估：** 1h
- **前置依赖：** Task 1.3

---

## 阶段 5：构建验证

### Task 5.1：npm build
- **关联：** design.md 七、测试策略
- **内容：** `npm install && npm run build` 通过；关键页面人工冒烟
- **预估：** 2h
- **前置依赖：** 阶段 1~4
