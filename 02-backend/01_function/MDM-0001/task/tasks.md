# 开发任务清单 — mdm-backend

## 文档版本

| 版本 | 日期 | 修改说明 | 作者 |
|------|------|----------|------|
| v1.0 | 2026-09-23 | 初始版本 | TL |

---

## 任务总览

| 状态 | 阶段 | 任务数 | 预估工时 |
|------|------|--------|----------|
| [ ] | 1. 工程骨架与数据层 | 4 | 4h |
| [ ] | 2. 服务层（核心引擎） | 5 | 10h |
| [ ] | 3. 服务层（业务流程） | 5 | 12h |
| [ ] | 4. 接口层与横切能力 | 3 | 6h |
| [ ] | 5. 测试 | 2 | 4h |
|     | 合计 | 19 | 36h |

---

## 阶段 1：工程骨架与数据层

### Task 1.1：Maven 工程骨架
- **关联：** REQ-BKD01~10（通用）
- **内容：** pom.xml（web/data-jpa/validation/sqlite-jdbc/community-dialect/POI/test）、application.yml（SQLite 路径、SQL init、AES key）、MdmApplication 启动类
- **预估：** 1h
- **前置依赖：** 无

#### 子任务

- [ ] pom 依赖与插件配置
- [ ] application.yml（含 `app.security.aes-key`）
- [ ] 启动类 + 包结构创建

### Task 1.2：通用层
- **关联：** 通用安全/规范
- **内容：** ApiResponse、PageResult、ErrorCode、BusinessException、GlobalExceptionHandler、Times 工具
- **预估：** 1h
- **前置依赖：** Task 1.1

#### 子任务

- [ ] 统一响应与错误码
- [ ] 全局异常处理（40000 字段级明细、50000 兜底）

### Task 1.3：schema.sql + data.sql
- **关联：** sql-mdm-0001-ddl.md 全部 12 表
- **内容：** 12 表 DDL（IF NOT EXISTS）+ 种子数据（分类树、原材料演示模型、下游系统、质量规则、6 条演示数据）
- **预估：** 1h
- **前置依赖：** Task 1.1

#### 子任务

- [ ] 12 表 DDL 与索引
- [ ] 种子数据（含 field_defs/code_rules JSON）

### Task 1.4：实体与 Repository
- **关联：** REQ-BKD01~10
- **内容：** 12 个 Entity + 12 个 JPA Repository；模型/数据实体的 JSON 列以 @Converter 或服务层 Jackson 序列化
- **预估：** 1h
- **前置依赖：** Task 1.3

#### 子任务

- [ ] Entity 12 个（含审计字段映射）
- [ ] Repository 12 个（含派生查询方法）

---

## 阶段 2：服务层（核心引擎）

### Task 2.1：CodeRuleEngine + CodeRuleValidator
- **关联：** REQ-BKD04
- **内容：** 四段式求值、SEQ 原子计数（@Transactional UPDATE）、补位、唯一性重试（10 次）、规则校验（40910）
- **预估：** 3h
- **前置依赖：** Task 1.4

#### 子任务

- [ ] 段求值器（FIXED/FIELD_REF/SEQ/MODEL_REF）
- [ ] 顺序值原子递增
- [ ] 唯一性重试与 42200

### Task 2.2：LocalSimilarityAdapter
- **关联：** REQ-BKD09
- **内容：** DuplicateCheckAdapter 接口 + Levenshtein/Jaccard 加权实现 + 关键字段提取（名称/规格/型号）
- **预估：** 2h
- **前置依赖：** Task 1.4

#### 子任务

- [ ] 编辑距离与 Jaccard
- [ ] 关键字段选择逻辑 + Top5/阈值

### Task 2.3：QualityRuleService
- **关联：** REQ-BKD10
- **内容：** 三类规则求值器、三级告警、忽略列表合并、结果留痕
- **预估：** 2h
- **前置依赖：** Task 1.4

#### 子任务

- [ ] COMPLIANCE/CONSISTENCY/COMPLETENESS 求值
- [ ] ignoredWarnings 覆盖判定

### Task 2.4：CryptoService
- **关联：** REQ-BKD05（加密）
- **内容：** AES 加解密 + 脱敏（保留前 4 后 4）
- **预估：** 1h
- **前置依赖：** Task 1.1

#### 子任务

- [ ] encrypt/decrypt
- [ ] mask 脱敏

### Task 2.5：OperationLogService
- **关联：** REQ-BKD10
- **内容：** log(bizType, operation, target, detail) 统一入口 + 筛选查询
- **预估：** 2h
- **前置依赖：** Task 1.4

#### 子任务

- [ ] 日志写入 API
- [ ] 时间范围/类型筛选分页

---

## 阶段 3：服务层（业务流程）

### Task 3.1：CategoryService
- **关联：** REQ-BKD01
- **内容：** 树构建、CRUD、删除保护（40901）、关键字过滤
- **预估：** 2h
- **前置依赖：** Task 2.5

#### 子任务

- [ ] 树 VO 组装（含模型数）
- [ ] 删除保护与搜索

### Task 3.2：ModelService
- **关联：** REQ-BKD02/03
- **内容：** 空白/继承创建、更新（上线锁定 40903）、上线/下线、版本快照/列表/diff/回滚、数据条数统计
- **预估：** 4h
- **前置依赖：** Task 2.1、Task 2.5

#### 子任务

- [ ] 生命周期与锁定规则
- [ ] 版本快照与 diff（Jackson 深比较）
- [ ] 继承创建深拷贝

### Task 3.3：MasterDataService + VersionService
- **关联：** REQ-BKD05/06
- **内容：** 动态校验（必填/唯一/自定义/类型）、新增（编码+加密+快照）、修改（敏感判定→协同）、禁用/启用/删除（下游预检）、动态视图元数据、版本列表/diff/回滚
- **预估：** 4h
- **前置依赖：** Task 2.1~2.4

#### 子任务

- [ ] 动态校验器
- [ ] CRUD 全流程 + 脱敏输出
- [ ] 协同判定（绝密/机密字段变更）

### Task 3.4：PushService + MockDownstreamAdapter
- **关联：** REQ-BKD08
- **内容：** 预检（确定性 Mock）、执行推送（40908/40909）、推送日志、协同工单确认/驳回
- **预估：** 3h
- **前置依赖：** Task 3.3

#### 子任务

- [ ] 适配器与预检
- [ ] 协同状态机

### Task 3.5：ImportExportService
- **关联：** REQ-BKD07
- **内容：** POI 模板（表头+说明+示例）、导出（脱敏）、导入（逐行校验、通过行统一提交、错误明细回传）
- **预估：** 3h
- **前置依赖：** Task 3.3

#### 子任务

- [ ] 模板与导出
- [ ] 导入校验循环与错误行收集

---

## 阶段 4：接口层与横切能力

### Task 4.1：Controller × 9
- **关联：** api-mdm-0001.md 全部 45 接口
- **内容：** Category/Model/MasterData/Quality/Push/Collaboration/OperationLog/Stats/ImportExport Controller
- **预估：** 3h
- **前置依赖：** 阶段 3

#### 子任务

- [ ] 9 个 Controller + DTO 映射
- [ ] 文件下载（Content-Type/Disposition）

### Task 4.2：安全与横切
- **关联：** 通用安全
- **内容：** @RequirePermission + PermissionInterceptor + UserContext（X-User-*）、CORS 配置
- **预估：** 2h
- **前置依赖：** Task 1.2

#### 子任务

- [ ] 注解与拦截器（40300/40101）
- [ ] CORS

### Task 4.3：防重复提交
- **关联：** 通用规范
- **内容：** 内存幂等拦截（同一用户+URL+参数 3 秒内去重）
- **预估：** 1h
- **前置依赖：** Task 4.2

#### 子任务

- [ ] IdempotentInterceptor

---

## 阶段 5：测试

### Task 5.1：单元测试
- **关联：** design.md 七、测试策略
- **内容：** CodeRuleEngineTest、LocalSimilarityAdapterTest、QualityRuleServiceTest、CategoryServiceTest
- **预估：** 3h
- **前置依赖：** 阶段 2、3

#### 子任务

- [ ] 四组纯 JUnit 测试（含边界：空值/超长/冲突）

### Task 5.2：集成验证
- **关联：** design.md 7.2
- **内容：** 启动初始化验证、编码端到端、协同闭环、导入闭环
- **预估：** 1h
- **前置依赖：** Task 5.1

#### 子任务

- [ ] mvn 编译 + 单测通过
- [ ] 人工冒烟脚本核对种子数据与编码格式
