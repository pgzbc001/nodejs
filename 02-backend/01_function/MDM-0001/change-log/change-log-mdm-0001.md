# 变更记录 — mdm-backend（主数据管理平台）

## v1.0 — 2026-09-23

**变更类型：** 代码实现

**变更内容：**
- 工程骨架：pom.xml（Spring Boot 3.2.5 / Java 17 / SQLite / Apache POI 5.2.5 / Mockito）、application.yml（SQLite 数据源 + hibernate-community-dialect）、MdmApplication 启动类
- 数据层：schema.sql（12 张表 + 审计字段 + del_flag）、data.sql（分类树 / 演示模型「原材料」/ 下游系统 / 质量规则种子）
- 通用层：ApiResponse / PageResult / ErrorCode（15 个错误码）/ BusinessException / GlobalExceptionHandler（BusinessException → HTTP 200 + code；参数校验 → 40000；未知 → 50000）
- 服务层十大服务：
  - CategoryService（树 CRUD / 删除保护 40901 / 搜索补全祖先链）
  - ModelService（空白/继承创建、上线锁定 40903 仅展示类属性合并、版本快照 / diff / 回滚）
  - CodeRuleEngine（FIXED / FIELD_REF / SEQ / MODEL_REF 四类段 + 补位 + 唯一性重试；MODEL_REF 仅第一段 40910）
  - MasterDataService（动态 CRUD / 必填 / 唯一 / 自定义校验 / 加密字段 / 敏感变更触发协同）
  - DuplicateCheckService（Levenshtein + Jaccard 加权，Top5、≥60 列出、≥80 高度相似）
  - QualityRuleService（COMPLIANCE / CONSISTENCY / COMPLETENESS 三类规则 + CRITICAL/WARNING/INFO 三级告警）
  - VersionService（数据快照 / diff / 回滚）、ImportExportService（POI 模板 / 导出 / 导入 / 错误明细）
  - PushService（预检 PASS/FAIL/CHECKING、推送执行、日志、协同状态机 PENDING→CONFIRMED/REJECTED）
  - OperationLogService（全操作留痕）
- 接口层 9 个 Controller：Category / Model / MasterData / ImportExport / Push / Collaboration / QualityRule / OperationLog / Stats，共 52 个端点
- 契约对齐修正：9 个 Controller 路径由 `/api/xxx` 修正为 `/api/v1/xxx`（对齐 api-mdm-0001.md）
- 横切：WebConfig CORS（/api/v1/**）+ X-User 拦截、@RequirePermission 角色校验、@NoRepeatSubmit 防重复提交
- 单元测试 4 个类 34 个用例：CodeRuleEngineTest（10）、LocalSimilarityAdapterTest（8）、QualityRuleServiceTest（8）、CategoryServiceTest（8）；修复 CodeRuleEngineTest 断言 `ex.getCode()` → `ex.getErrorCode().getCode()`

**影响范围：**
- `mdm-backend/`（93 个源文件 + schema.sql / data.sql / application.yml）
- `02-backend/01_function/MDM-0001/`（api / design / sql / task 文档）

**关联任务：** MDM-0001 · REQ-BKD01 ~ REQ-BKD10（02-backend/01_function/MDM-0001/task/tasks.md）

**验证状态：** 本机无 JDK 17 / Maven 环境，未执行 `mvn test`；静态设计-代码一致性核对见 [verification-checklist.md](./verification-checklist.md)

---

<!--
变更记录规则：版本号递增（v1.0, v1.1...）；最新版本在文件顶部；每次改动后追加。
-->
