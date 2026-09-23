# mdm-backend — 后端代码仓库 Agent 上下文

## 模块划分（包 com.mdm.platform.*）

| 包 | 职责 | 关键错误码 |
|----|------|-----------|
| category | 分类体系（树存储、层级维护） | 40901 子级/已关联禁删 |
| model | 模型定义 / 字段元数据 / 上线锁定 / 模型版本 | 40902 编码重复、40903 上线结构锁定 |
| data | 主数据 CRUD / 状态流转 / 数据版本与回滚 | 40904 未上线禁录入、40905 数据编码重复、40906 唯一字段重复 |
| coderule | 编码引擎（FIXED / FIELD_REF / SEQ / MODEL_REF 四段式） | 40910 编码引用仅限首段、42200 生成冲突重试超限 |
| duplicate | 相似度查重（Levenshtein + Jaccard 加权，Top5） | — |
| quality | 质量规则引擎（COMPLIANCE / CONSISTENCY / COMPLETENESS，三级告警） | 40907 CRITICAL 阻断提交 |
| push | 下游预检与推送 / 协同状态机（PENDING→CONFIRMED/REJECTED） | 40908 待协同禁推、40909 预检未过未强制 |
| io | Excel 导入导出（Apache POI） | 42201 导入文件解析失败 |
| audit | 操作日志（bizType / operation 枚举，全操作留痕） | — |
| common | ApiResponse / ErrorCode（15 项）/ BusinessException / PageResult | — |
| config | WebConfig（CORS + 权限与防重提交拦截器） | — |
| security | PermissionInterceptor / @RequirePermission / CryptoService | 40101 未识别操作人、40300 无权 |

## 关键入口

- 启动类：`src/main/java/com/mdm/platform/MdmApplication.java`
- 契约文档（权威）：`../02-backend/01_function/MDM-0001/{sql,api,design}/`
- 库表：`src/main/resources/schema.sql`（12 张 `mdm_*` 表）+ `data.sql`（种子数据）

## 本地命令

```bash
mvn -q compile        # 编译（需 JDK 17 + Maven 3.9）
mvn test              # 单元测试（4 类 34 用例）
mvn spring-boot:run   # 启动 http://localhost:8080（首次自动建库 + 种子数据）
```

## 约定

- 统一响应 `ApiResponse{code,message,data}`；业务错误 HTTP 200 + 错误码（40000 时 data = 字段级提示 map）
- 认证：`X-User-Id / X-User-Name / X-User-Role` 请求头；写操作标注 `@RequirePermission`
- 审计：所有增删改经 `OperationLogService` 留痕；时间统一 `Times.now()`
- `ddl-auto: none`：表结构只由 `schema.sql` 维护，禁止依赖自动建表
- 表前缀 `mdm_`；逻辑删除 `del_flag`；数据源 SQLite（`data/mdm.db`）
