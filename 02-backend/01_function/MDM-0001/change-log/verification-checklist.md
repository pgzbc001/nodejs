# 设计-代码一致性验证清单 — mdm-backend

> 核对基准：`02-backend/01_function/MDM-0001/` 的 sql / api / design / task 文档
> 核对方式：静态审查（关键词、文件清单、注解与枚举比对）　执行时间：2026-09-23

## 1. 静态核对结果（已执行）

| # | 核对项 | 基准文档 | 代码证据 | 结论 |
|---|--------|----------|----------|------|
| 1 | 数据表 12 张 | sql-mdm-0001-ddl.md | schema.sql 12 个 CREATE TABLE（category / model / model_version / master_data / data_version / code_sequence / quality_rule / quality_result / downstream_system / push_log / collaboration / operation_log），表名与列逐一比对一致 | ✅ 一致 |
| 2 | API 前缀 `/api/v1` | api-mdm-0001.md | 9 个 Controller `@RequestMapping` 均为 `/api/v1/xxx`（修复前为 `/api/xxx`，本轮修正） | ✅ 一致 |
| 3 | 接口覆盖（契约 45 个） | api-mdm-0001.md | 52 个端点全覆盖，另含兼容端点：`POST /push/logs/batch`、`GET /push/precheck/{dataId}`、`POST /models/{id}/diff` | ✅ 覆盖 |
| 4 | 统一响应结构 | api 契约「统一响应」 | `ApiResponse{code,message,data}`；`GlobalExceptionHandler`：BusinessException → HTTP 200 + code；参数校验 → 40000；未知 → 50000 | ✅ 一致 |
| 5 | 错误码定义 | api 契约「错误码」 | `ErrorCode` 枚举 15 项：0 / 40000 / 40101 / 40300 / 40400 / 40901~40910 / 42200 / 42201 / 50000，与契约一一对应 | ✅ 一致 |
| 6 | 审计字段 | design 数据建模 | 业务表均含 `created_by/created_time/updated_by/updated_time/del_flag`（`mdm_code_sequence` 为纯序号表，符合设计） | ✅ 一致 |
| 7 | 模型上线锁定 | REQ-BKD03 | `ModelService.DISPLAY_PROPS`（label/group/listShow/searchable/popup/defaultValue）白名单 + `assertStructuralUnchanged` → 40903；允许上线后新增字段 | ✅ 一致 |
| 8 | 编码规则引擎 | 需求 4.3.4 / design | `CodeRuleSegment` 四类型 FIXED/FIELD_REF/SEQ/MODEL_REF + 补位（padChar/padSide/seqStart/seqStep）；`CodeRuleValidator` MODEL_REF 仅第一段 → 40910 | ✅ 一致 |
| 9 | 查重算法 | design 3.6 | `DuplicateCheckService` 关键字段加权，Top5、≥60 相似列出、≥80 标记 highSimilar | ✅ 一致 |
| 10 | 质量三级告警 | REQ-BKD10 | CRITICAL 阻止提交（40907）；WARNING/INFO 支持忽略 + 原因；三类规则求值分支齐备 | ✅ 一致 |
| 11 | 协同状态机 | design 状态机 | PENDING → CONFIRMED / REJECTED；待协同数据禁推 40908；确认/驳回接口带处理意见 | ✅ 一致 |
| 12 | 权限矩阵 | requirements 角色表 | 写操作均标注 `@RequirePermission({Role.X})`；PushController/CollaborationController 角色组合与需求一致（审核员处理协同） | ✅ 一致 |
| 13 | 单元测试 | task/tasks.md 测试任务 | 4 个测试类 34 用例（CodeRuleEngine 10 / LocalSimilarity 8 / QualityRuleService 8 / CategoryService 8） | ✅ 齐备 |

## 2. 待环境可用的执行验证（未执行）

| # | 验证项 | 命令 | 状态 |
|---|--------|------|------|
| 1 | 编译 | `cd mdm-backend; mvn -q compile` | ⏸ 未执行 — 本机无 JDK 17 / Maven（`Get-Command java,mvn` 无结果；`C:\Program Files\Java`、`.m2` 均不存在） |
| 2 | 单元测试 | `mvn test` | ⏸ 未执行（同 1），预期 34 用例全绿 |
| 3 | 启动建库 | `mvn spring-boot:run` | ⏸ 未执行；预期首次启动生成 `mdm.db` 并执行 schema.sql / data.sql |
| 4 | 接口冒烟 | `curl http://localhost:8080/api/v1/stats/overview` 等 | ⏸ 未执行；预期返回 11 项指标 |

> 结论：交付物（代码 + 文档）已按 Spec 完整落盘并通过静态核对；上表执行验证需在具备 **JDK 17 + Maven 3.9** 的机器上按命令补跑，结果可追加至本文件。
