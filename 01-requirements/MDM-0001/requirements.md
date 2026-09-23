# 需求文档 — 主数据管理平台（backend + frontend）

## 文档版本

| 版本 | 日期 | 修改说明 | 作者 |
|------|------|----------|------|
| v1.0 | 2026-09-23 | 初始版本（两轮 Q&A 收敛后） | TL |

---

## 简介

主数据管理平台由 mdm-backend（Java 17 + Spring Boot 3 + SQLite + JPA + REST API）与 mdm-frontend（Vue 3 + Vite + Element Plus + Pinia）组成，覆盖 backend 与 frontend 两个 Workbench。后端提供元数据驱动的模型定义、主数据全生命周期维护、编码生成、查重、质量校验、版本、导入导出、推送协同与操作日志能力；前端面向数据管理员/录入员/审核员/系统管理员提供动态渲染的管理界面。

## 术语表

| 术语 | 说明 |
|------|------|
| 模型（Model） | 某类主数据（如"原材料"）的元数据定义：基础信息 + 字段集合 + 扩展配置 + 编码规则 |
| 字段定义（FieldDef） | 模型中单个字段的 18 项业务属性（名称/类型/必填/唯一/加密/分级等） |
| 编码规则（CodeRule） | 主数据编码的四段式生成规则：固定值/对象引用/顺序值/编码引用 |
| 主数据（MasterData） | 基于已上线模型录入的业务数据，含动态属性 JSON |
| 版本快照（DataVersion） | 主数据每次变更生成的完整 JSON 快照 |
| 质量规则（QualityRule） | 合规性/一致性/完整性三类校验规则，输出严重/告警/提示三级告警 |
| 协同确认（Collaboration） | 敏感字段变更后需数据审核员确认才可推送的流程状态 |
| 下游系统（DownstreamSystem） | 消费主数据的 ERP/MES/WMS 等系统，通过适配器对接 |

---

## 需求

### REQ-BKD01：分类体系管理

**用户故事：** 作为数据管理员，我希望维护分层分类树，以便主数据按业务属性归类管理。

**关联 PRD：** 用户故事#1

#### 验收标准

1. THE System SHALL 以 parent_id 自关联存储分类，支持无限层级，接口返回完整树
2. THE System SHALL 支持新增同级/子级分类、修改、删除
3. IF 分类存在子分类或关联模型, THEN THE System SHALL 拒绝删除并返回明确错误码
4. THE System SHALL 支持按关键字过滤树节点
5. THE System SHALL 对每个分类维护 code/name/sort/audit 字段

---

### REQ-BKD02：数据模型管理

**用户故事：** 作为数据管理员，我希望创建并管理数据模型，以便定义主数据标准。

**关联 PRD：** 用户故事#2

#### 验收标准

1. THE System SHALL 支持空白创建与继承创建（复制字段/编码规则/扩展配置）
2. IF 模型编码已存在, THEN THE System SHALL 返回 MODEL_CODE_DUPLICATED 错误
3. THE System SHALL 维护模型状态 ONLINE/OFFLINE；仅 ONLINE 模型可录入数据
4. IF 模型已上线, THEN THE System SHALL 锁定字段结构属性，仅允许新增字段与展示类属性修改
5. THE System SHALL 在模型每次变更时生成版本快照，支持版本列表、回滚与差异对比
6. THE System SHALL 在模型列表返回每模型的数据条数

---

### REQ-BKD03：模型详情配置

**用户故事：** 作为数据管理员，我希望配置模型的基础信息、字段、扩展与编码规则。

**关联 PRD：** 用户故事#3

#### 验收标准

1. THE System SHALL 持久化基础信息：标准编码（唯一）/标准名称/所属分类/归口部门
2. THE System SHALL 持久化字段定义的 18 项属性（字段名称、显示名称、数据类型、必填、唯一、列表展示、下拉选项、值域来源、多选、检索条件、弹窗选择、加密、数据分级、安全规则、默认值、字段分组、自定义校验）
3. THE System SHALL 支持字段值域引用其他模型并保存引用过滤条件
4. THE System SHALL 持久化扩展配置（流程标题字段、树形结构上级/子级/展示字段）
5. THE System SHALL 持久化编码规则段列表（类型/值/长度/补位符/方向），字段定义与编码规则以 JSON 存储于模型表

---

### REQ-BKD04：编码生成引擎

**用户故事：** 作为系统，我希望按模型编码规则自动生成全局唯一编码。

**关联 PRD：** 用户故事#3、#5

#### 验收标准

1. THE System SHALL 顺序拼接编码规则段：FIXED 常量 / FIELD_REF 取本条数据字段值 / SEQ 顺序值 / MODEL_REF 取被引用模型数据的编码
2. IF 段类型为 MODEL_REF, THEN 该段 SHALL 且仅 SHALL 出现在第一段
3. THE System SHALL 按 SEQ 段配置（长度/补位符/左补或右补/起始值/步长）生成顺序值，按模型独立计数
4. THE System SHALL 生成后校验唯一性，冲突时递增顺序值重试（上限 10 次）
5. THE System SHALL 输出形如 `MATYUANL000001` 的编码（MAT + 分类编码 + 6 位顺序号）

---

### REQ-BKD05：主数据动态 CRUD

**用户故事：** 作为数据录入员，我希望基于已上线模型录入、修改、禁用、删除主数据。

**关联 PRD：** 用户故事#4、#5、#6、#7

#### 验收标准

1. WHEN 查询列表, THE System SHALL 按模型字段定义动态返回展示列与检索条件元数据，支持分页与动态筛选
2. WHEN 新增/修改, THE System SHALL 校验必填、唯一、自定义校验规则，失败返回字段级错误
3. IF 字段配置加密, THEN THE System SHALL AES 加密存储、脱敏返回
4. IF 模型未上线, THEN THE System SHALL 拒绝数据写入
5. WHEN 禁用/删除, THE System SHALL 先逐下游系统预检（Mock 适配器），全部通过或用户强制确认后执行；删除为逻辑删除（del_flag）
6. THE System SHALL 维护数据状态 VALID/DISABLED/DRAFT
7. WHEN 修改涉及绝密/机密分级字段变更, THEN THE System SHALL 置数据为待协同状态（PENDING_CONFIRM）

---

### REQ-BKD06：版本管理与差异

**用户故事：** 作为数据审核员，我希望追溯并回滚主数据的历史版本。

**关联 PRD：** 用户故事#8

#### 验收标准

1. THE System SHALL 在每次新增/修改/回滚时生成版本快照（版本号、操作类型、操作人、时间、完整属性 JSON）
2. THE System SHALL 支持任两版本逐字段差异对比（字段、旧值、新值）
3. THE System SHALL 支持回滚：将历史版本属性写为当前数据并生成新版本（操作类型 ROLLBACK）
4. THE System SHALL 支持查看任意版本详情（解密脱敏规则与列表一致）

---

### REQ-BKD07：数据导入导出

**用户故事：** 作为数据录入员，我希望通过 Excel 批量导入导出主数据。

**关联 PRD：** 用户故事#9

#### 验收标准

1. THE System SHALL 生成导入模板 Excel（字段显示名称表头 + 格式说明行 + 示例行）
2. THE System SHALL 按当前筛选条件导出数据 Excel（脱敏规则与列表一致）
3. WHEN 批量导入, THE System SHALL 逐行执行必填/格式/唯一/质量校验，通过行统一提交
4. THE System SHALL 返回导入结果摘要（总数/成功/失败），失败行可导出错误明细 Excel（原始数据 + 错误说明列）
5. IF 导入含查重命中行, THEN THE System SHALL 按告警级提示并允许用户忽略继续

---

### REQ-BKD08：推送与协同

**用户故事：** 作为数据审核员，我希望将主数据变更推送至下游系统并管理协同确认。

**关联 PRD：** 用户故事#10

#### 验收标准

1. THE System SHALL 提供下游系统注册表（编码/名称/类型/启用状态）
2. WHEN 推送前, THE System SHALL 对每个目标系统执行预检（Mock 适配器返回 通过/失败/校验中）
3. THE System SHALL 记录推送日志（数据、目标系统、时间、结果、操作人）
4. IF 数据处于待协同状态, THEN THE System SHALL 阻止推送直至协同人确认（CONFIRMED）或驳回（REJECTED）
5. THE System SHALL 支持批量推送至多系统

---

### REQ-BKD09：AI 查重

**用户故事：** 作为数据录入员，我希望新增数据时系统自动识别可能的重复。

**关联 PRD：** 用户故事#5

#### 验收标准

1. WHEN 提交前查重, THE System SHALL 提取关键字段（名称/规格/型号类文本字段）与同模型已有有效数据比对
2. THE System SHALL 以 Levenshtein（归一化）与 Jaccard（分词）加权计算相似度，返回 Top N 相似列表与百分比
3. IF 最高相似度 ≥ 80%, THEN THE System SHALL 标记为"高度相似"
4. THE System SHALL 以适配器接口（DuplicateCheckAdapter）封装算法，支持替换外部 AI 服务

---

### REQ-BKD10：质量校验与操作日志

**用户故事：** 作为数据管理员，我希望数据提交前执行质量规则校验，且所有操作留痕可审计。

**关联 PRD：** 用户故事#5、#6、#11

#### 验收标准

1. THE System SHALL 支持质量规则配置：类型（COMPLIANCE/CONSISTENCY/COMPLETENESS）、字段、表达式/参数、告警等级（CRITICAL/WARNING/INFO）、启用状态
2. WHEN 数据提交前, THE System SHALL 执行启用规则并返回逐条结果（规则、等级、消息）
3. IF 存在 CRITICAL 结果, THEN 前端 SHALL 阻止提交；WARNING/INFO 可忽略（记录忽略原因）
4. THE System SHALL 对新增/修改/禁用/删除/推送/协同/导入/回滚全部写操作日志
5. THE System SHALL 支持按时间范围与操作类型筛选日志，记录操作人/类型/时间/详情（JSON）

---

### REQ-FED01：应用框架与角色切换

**用户故事：** 作为任意角色用户，我希望通过统一框架访问平台并切换角色演示。

**关联 PRD：** 全部

#### 验收标准

1. THE App SHALL 基于 Vue 3 + Vite + Element Plus + Pinia + Vue Router 构建
2. THE App SHALL 提供侧边菜单 + 顶栏布局，顶栏含角色切换（数据管理员/数据录入员/数据审核员/系统管理员）
3. THE App SHALL 通过 Axios 统一封装请求，附加 X-User-Id / X-User-Name / X-User-Role 头
4. THE App SHALL 统一处理 ApiResponse 错误码并 ElMessage 提示

---

### REQ-FED02：分类与模型管理页面

**用户故事：** 作为数据管理员，我希望在页面上管理分类树与模型列表。

**关联 PRD：** 用户故事#1、#2

#### 验收标准

1. THE App SHALL 左侧渲染分类树（新增同级/子级、编辑、删除、搜索、展开/折叠全部）
2. THE App SHALL 右侧展示模型列表（名称/编码/分类/状态/数据条数/操作）
3. THE App SHALL 支持空白创建与继承创建对话框
4. THE App SHALL 提供模型上线/下线操作与版本历史（列表/对比/回滚）

---

### REQ-FED03：模型设计器

**用户故事：** 作为数据管理员，我希望在多 Tab 设计器中配置模型。

**关联 PRD：** 用户故事#3

#### 验收标准

1. THE App SHALL 提供基础信息/字段配置/扩展配置/编码规则/版本历史 5 个 Tab
2. THE App SHALL 在字段 Tab 以表格编辑 18 项属性，支持添加/删除（未上线）、上线后结构列锁定
3. THE App SHALL 在编码规则 Tab 以段列表编辑（类型/值/长度/补位符/方向），实时预览生成示例
4. THE App SHALL 校验"编码引用仅第一段"并阻止保存

---

### REQ-FED04：数据维护页面

**用户故事：** 作为数据录入员，我希望浏览并筛选主数据。

**关联 PRD：** 用户故事#4

#### 验收标准

1. THE App SHALL 左侧渲染"分类→模型"树（仅已上线模型可选）
2. THE App SHALL 右侧按模型元数据动态渲染表格列与筛选表单
3. THE App SHALL 支持分页、导出 Excel、导入对话框（模板下载/上传/错误明细下载）

---

### REQ-FED05：动态表单抽屉

**用户故事：** 作为数据录入员，我希望按模型定义动态生成的表单录入数据。

**关联 PRD：** 用户故事#5、#6

#### 验收标准

1. THE App SHALL 以抽屉（Drawer）承载动态表单，字段按分组分区折叠展示
2. THE App SHALL 按数据类型渲染控件：文本/长文本/数字/日期/下拉单选/下拉多选/引用弹窗
3. THE App SHALL 渲染必填星号、默认值、自定义校验提示；加密字段值脱敏回显
4. THE App SHALL 提交前调用查重与质量接口并展示结果面板

---

### REQ-FED06：查重与质量交互

**用户故事：** 作为数据录入员，我希望在提交前看到相似数据与质量告警并做出选择。

**关联 PRD：** 用户故事#5、#6

#### 验收标准

1. THE App SHALL 展示相似数据弹窗（编码/名称/规格/相似度进度条），提供"使用已有物料"与"确认是新物料"操作
2. THE App SHALL 以红/黄/蓝三级展示质量结果；CRITICAL 阻止提交按钮，WARNING/INFO 忽略需填写原因
3. THE App SHALL 版本对比以表格逐字段展示旧值/新值并高亮差异

---

### REQ-FED07：推送/日志/统计页面

**用户故事：** 作为数据审核员/系统管理员，我希望管理推送、查看日志与统计。

**关联 PRD：** 用户故事#9、#10、#11

#### 验收标准

1. THE App SHALL 提供推送中心：选择数据→选择目标系统→预检结果列表（通过/失败/校验中）→执行推送；待协同数据列表支持确认/驳回
2. THE App SHALL 提供操作日志页面（时间范围 + 操作类型筛选）
3. THE App SHALL 首页展示统计卡片（模型数/数据总量/今日操作/推送成功率）

---

### 数据权限控制（通用）

**用户故事：** 作为系统，我希望模块遵循权限规则。

#### 验收标准

1. THE System SHALL 对所有接口执行角色校验（@RequirePermission 注解，X-User-Role 头识别）
2. IF 角色无权限, THEN THE System SHALL 返回 FORBIDDEN 错误
3. THE System SHALL 对所有接口响应采用 ApiResponse 统一结构

### 接口安全与规范（通用）

#### 验收标准

1. THE System SHALL 统一 API 前缀 `/api/v1`，RESTful 语义
2. THE System SHALL 全局异常兜底为 ApiResponse 结构
3. THE System SHALL 开启 CORS 允许前端开发端口访问
