# mdm-frontend — 前端代码仓库 Agent 上下文

## 目录结构（src/）

| 目录 | 职责 |
|------|------|
| api/ | 8 个接口模块；`http.js` 统一封装（baseURL `/api/v1`、X-User-* 注入、ApiResponse 解包、40300/40000 处理） |
| components/ | 10 个复用组件（分类树 / 动态字段 / 动态表单抽屉 / 相似度弹窗 / 质量面板 / 版本抽屉 / 导入导出 / REF 选择弹窗等） |
| layouts/ | MainLayout（侧边菜单 + 角色切换） |
| router/ | 7 个业务路由 |
| stores/ | Pinia（userStore：4 角色模拟 + localStorage 持久化） |
| views/ | 7 个页面（Dashboard / ModelList / ModelDesigner / DataMaintenance / PushCenter / QualityRules / OperationLogs） |

## 关键入口

- 入口：`src/main.js` → `src/App.vue`
- 代理与构建：`vite.config.js`（`/api` → `http://localhost:8080`）
- 请求基建：`src/api/http.js`（blob 下载 `download()`；错误码语义见 `../02-backend/01_function/MDM-0001/api/api-mdm-0001.md`）

## 本地命令

```bash
npm install
npm run dev      # http://localhost:5173（/api 代理至 8080）
npm run build    # 产物 dist/
```

## 约定

- Vue 3 `<script setup>` + Element Plus；列表 / 表单 / 筛选动态渲染由后端 `viewSchema` / `FieldDef` 驱动
- 演示用户名 ASCII（如 model_admin / data_staff），避免请求头中文编码问题
- 权限显示：按钮级 `can(role)` 判断，与后端 `@RequirePermission` 语义保持一致
- 错误处理：40300 提示切换角色；40000 detail map 渲染表单字段级红字；40909 触发强制操作二次确认
