# 开发自测用例基础模板（框架源）

> **本文件是 framework 源模板，不直接使用。**
> `project-setup` 实例化时为**每个启用 Workbench**复制本骨架一份，填充其中的
> `<!-- TECH_SPECIFIC -->` 区块，产出 `skills/dev-self-test/templates/{Workbench 名}_dev_test_template.md`。
>
> 【N 个 Workbench 原则 —— 本模板第一铁律】
> 骨架定义**各 Workbench 通用**的结构：AC 追溯 + 用例六要素（前置/步骤/输入/预期/验证点/追溯AC）+ 数据验证查询 + 覆盖矩阵。
> 各 Workbench 差异（由 project-setup 按该 Workbench tech_stack 派生，**不预设固定 Workbench 集合**）：
>   ① 「被测单元」的概念
>   ② 「测试维度集」与场景代码（**维度集是 Workbench 绑定的，数量与代码因 Workbench 而异，不是固定六维**）
>   ③ 「数据验证查询」的语言
> 区块内出现的 backend / data / frontend 仅为**示意答案形态**（来自种子三 Workbench），**非枚举**；
> 其他 Workbench（python / ml / mobile ...）按其自身测试关注点派生维度集，**严禁硬塞进示例 Workbench**。

---

# 开发自测用例 — {模块名称}（{{Workbench_NAME}} Workbench）

> **模式：开发自测（白盒）**
> <!-- TECH_SPECIFIC: 本 Workbench 验证方式一句话说明 - project-setup 按 Workbench 生成 -->
> [此区块由 project-setup 生成，如：
>  - backend → 每条用例包含 MySQL / ES 数据验证查询语句，可直接在测试环境执行
>  - data → 每条用例包含上游输入表 / 下游输出表的对比查询，验证管道数据正确性
>  - frontend → 每条用例包含接口请求/响应断言与 UI 状态验证点]
> <!-- /TECH_SPECIFIC -->

## 文档版本

| 版本 | 日期 | 修改说明 | 作者 |
|------|------|----------|------|
| v1.0 | {日期} | 初始版本 | {作者} |

## 文档信息

| 项目 | 内容 |
|------|------|
| 需求文档 | {requirements.md 路径} |
| PRD | {PRD 路径} |
| Workbench 设计文档 | <!-- TECH_SPECIFIC: 本 Workbench 设计文档名 -->{此区块由 project-setup 生成：backend→api_design 路径；data→pipeline_design 路径；frontend→interface_spec 路径}<!-- /TECH_SPECIFIC --> |
| 测试范围 | {被测单元清单} |
| 测试环境 | {dev / sit} |

---

## 数据源说明

<!-- TECH_SPECIFIC: 本 Workbench 数据源类型表 - project-setup 按 Workbench 技术栈生成 -->
[此区块由 project-setup 生成本 Workbench 数据源类型表，如：
 - backend → ES 索引 / DB 表 / Redis Key 模式
 - data → 上游源表 / 中间表 / 下游目标表 / Kafka Topic
 - frontend → 消费接口清单 / 本地状态 store]
<!-- /TECH_SPECIFIC -->

---

## 用例统计（本 Workbench 测试维度集）

<!-- TECH_SPECIFIC: 本 Workbench 测试维度集 - project-setup 按本 Workbench tech_stack 派生（维度数量与代码因 Workbench 而异，非固定六维） -->
[为本 Workbench 生成测试维度集统计表（场景类型 | 代码 | 用例数 + 合计）。

 示例（仅示意答案形态，非枚举；其他 Workbench 按其测试关注点定义维度集）：
  - backend：HP 正向 / PV 参数校验 / BL 业务逻辑 / EH 错误处理 / DB 数据边界 / SE 安全性（6 维）
  - data：HP 正向 / DI 数据完整性 / BL 业务逻辑 / IM 幂等性 / PT 分区边界 / LN 数据血缘 / EH 异常处理（7 维）
  - frontend：HP 正向 / UI 三态 / IA 交互行为 / AC 接口消费 / PE 权限控制 / EH 错误处理 / DB 数据边界（7 维）]
<!-- /TECH_SPECIFIC -->

---

## 一、{被测单元名称}

<!-- TECH_SPECIFIC: 被测单元头部字段 - project-setup 按 Workbench 生成 -->
[此区块由 project-setup 生成被测单元的头部元信息，如：
 - backend → Method / Path / 关联需求 / 关联错误码 / 写入数据源
 - data → Job 名称 / 触发方式（Kafka/调度）/ 上游表 / 下游表 / 关联需求
 - frontend → 组件/页面名 / 路由 / 消费接口 / 关联需求]
<!-- /TECH_SPECIFIC -->

> **以下 1.1–1.6 小节以 backend 六维（HP/PV/BL/EH/DB/SE）为示例骨架。**
> project-setup 实例化时**按本 Workbench 维度集生成对应数量与代码的维度小节**（如 data Workbench 生成 HP/DI/BL/IM/PT/LN/EH 七节，
> frontend Workbench 生成 HP/UI/IA/AC/PE/EH/DB 七节）；每个维度小节沿用下方用例六要素 + 数据验证查询结构。

### 1.1 正向路径（HP）

#### TC-{MOD}-001-HP-01：{用例标题}

| 项目 | 内容 |
|------|------|
| 前置条件 | {前置数据/状态描述} |
| 测试步骤 | {步骤描述} |
| 输入 | <!-- TECH_SPECIFIC: 输入字段名 -->{此区块由 project-setup 生成：backend→请求参数 JSON；data→上游表/消息构造；frontend→用户操作+接口入参}<!-- /TECH_SPECIFIC --> |
| 预期输出 | <!-- TECH_SPECIFIC: 输出字段名 -->{此区块由 project-setup 生成：backend→响应结构+状态码；data→下游表期望行/字段；frontend→UI 状态+渲染断言}<!-- /TECH_SPECIFIC --> |
| 验证点 | {需要验证的关键断言} |
| 追溯 AC | {AC 编号} |

**数据验证查询：**

<!-- TECH_SPECIFIC: 本 Workbench 验证查询代码块 - project-setup 按 Workbench 查询语言生成 -->
```
[此区块由 project-setup 生成本 Workbench 可直接执行的验证查询，如：
 - backend → SQL（SELECT ... FROM {table} WHERE id='{变量:recordId}'）+ ES DSL（GET /{index}/_doc/{id}）
 - data → Hudi/Presto SQL，对比上游输入与下游输出的行数/字段值
 - frontend → HTTP 请求构造 + 期望响应 JSON 断言]
```
<!-- /TECH_SPECIFIC -->

---

### 1.2 数据/参数校验（PV）

#### TC-{MOD}-001-PV-01：{校验项} 缺失/非法

| 项目 | 内容 |
|------|------|
| 前置条件 | {前置} |
| 测试步骤 | {传入缺失或非法的输入} |
| 输入 | {省略必填/给非法值} |
| 预期输出 | {被拒绝的错误反馈} |
| 验证点 | 返回校验失败；数据层无脏数据写入 |
| 追溯 AC | {AC 编号} |

**数据验证查询：**

```
-- 验证请求被拒绝后无脏数据写入（按本 Workbench 查询语言填充）
-- 预期：无新增记录 / 下游无脏数据
```

---

### 1.3 业务逻辑（BL）

#### TC-{MOD}-001-BL-01：{核心业务规则/状态流转场景}

| 项目 | 内容 |
|------|------|
| 前置条件 | {数据处于特定状态} |
| 测试步骤 | {操作描述} |
| 输入 | {输入} |
| 预期输出 | {输出} |
| 验证点 | 业务规则正确执行；状态正确流转；后置动作已触发 |
| 追溯 AC | {AC 编号} |

> **本维度是六维中最需结合 `architectural_constraints.md` 的部分**：
> 每条硬性业务约束（金额过滤、权限、数据质量规则）都应有对应 BL 用例。

**数据验证查询：**

```
-- 前置状态确认（执行前运行）+ 后置状态验证（执行后运行），按本 Workbench 查询语言填充
```

---

### 1.4 错误处理（EH）

#### TC-{MOD}-001-EH-01：触发 {错误场景}

| 项目 | 内容 |
|------|------|
| 前置条件 | {满足错误触发条件} |
| 测试步骤 | {操作描述} |
| 输入 | {输入} |
| 预期输出 | {正确的错误反馈} |
| 验证点 | 返回正确错误；数据无意外变更 |
| 追溯 AC | {AC 编号} |

**数据验证查询：**

```
-- 验证错误后数据状态未被修改，按本 Workbench 查询语言填充
```

---

### 1.5 数据边界（DB）

#### TC-{MOD}-001-DB-01：空数据 / 极值边界

| 项目 | 内容 |
|------|------|
| 前置条件 | {范围内无数据 / 极大极小值} |
| 测试步骤 | {使用边界条件触发} |
| 输入 | {边界输入} |
| 预期输出 | {数值返回 0 而非 null / 边界正确处理} |
| 验证点 | {边界断言} |
| 追溯 AC | {AC 编号} |

**数据验证查询：**

```
-- 确认前置确实为边界状态，按本 Workbench 查询语言填充
```

---

### 1.6 安全性（SE）

#### TC-{MOD}-001-SE-01：{未认证 / 越权 / 注入}

| 项目 | 内容 |
|------|------|
| 前置条件 | {安全场景前置} |
| 测试步骤 | {越权/未认证/注入操作} |
| 输入 | {恶意/越权输入} |
| 预期输出 | {拒绝访问，无数据泄露} |
| 验证点 | 无越权数据返回 |
| 追溯 AC | {AC 编号} |

> 跨租户/多租户隔离、PII 字段保护等约束应在此维度落用例（参 `architectural_constraints.md`）。

**数据验证查询：**

```
-- 验证越权访问未成功（COUNT=0 / 无数据返回），按本 Workbench 查询语言填充
```

---

## 二、{下一个被测单元名称}

（结构同上）

---

## 覆盖追溯矩阵

### 验收标准覆盖

| AC 编号 | AC 描述 | 覆盖用例 | 状态 |
|---------|---------|---------|------|
| {AC-REQ-XX-01-1} | {AC 描述} | TC-{MOD}-001-HP-01 | ✅ 已覆盖 |

<!-- TECH_SPECIFIC: 本 Workbench 附加追溯维度 - project-setup 按 Workbench 生成 -->
[此区块由 project-setup 生成本 Workbench 特有的追溯维度，如：
 - backend → 错误码覆盖、状态流转覆盖、API 接口覆盖
 - data → 字段映射覆盖、数据质量规则覆盖、Job 触发路径覆盖
 - frontend → 接口消费覆盖、UI 状态覆盖、交互路径覆盖]
<!-- /TECH_SPECIFIC -->

---

## 覆盖率统计

| 维度 | 总数 | 已覆盖 | 覆盖率 |
|------|------|--------|--------|
| 验收标准（AC） | {N} | {N} | {N%} |
| 被测单元 | {N} | {N} | {N%} |

---

## 查询变量速查表

| 变量名 | 说明 | 获取方式 |
|--------|------|---------|
| `{变量:recordId}` | {主键/标识} | {获取方式} |
