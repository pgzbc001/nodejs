# solution-design.md 产出模板（HLD 主文档）

> 本文件是 solution-designer **Step 6** 的产出骨架。`solution-design.md` 是被下游**每个 Workbench**
> 消费的承重产物——impact-analyzer 按 §号取数（§4/§5/§8），architecture-advisor 据 §5 约束做 LLD。
> 故各节必须填**实质内容**，不得只留标题占位。
>
> **接口不变量**：§5、§8 是与 impact-analyzer 的紧耦合接口（它按这两个 §号取数），
> 节号与含义不可随意改动；如需调整须同步改 impact-analyzer 的消费逻辑。

---

## §1 业务目标与约束
*来源：Step 1*

- 核心业务目标（1–3 句）
- 关键 NFR（性能 / 并发 / 数据量级 / 实时性）
- 命中的 `architectural_constraints.md` 条目（逐条列编号）

## §2 组件选型矩阵
*来源：Step 2*

每个有选择空间的组件一行：

| 组件 | 候选项 | 推荐项 | 选择理由 | 未选理由 | 与现有架构兼容性 |
|------|-------|-------|---------|---------|----------------|

> 依赖既有代码但未核实的选型，在对应行标 `⚠️ 假设未验证：{假设}`。

## §3 数据流设计
*来源：Step 3*

数据从产生到消费的端到端路径，覆盖所有涉及 Workbench；每个传递点标：传输方式 / 时效 / 高层格式。

## §4 跨 Workbench 数据契约
*来源：Step 4*

| 契约名称 | 提供 Workbench | 消费 Workbench | 传输方式 | 关键字段（高层） | 时效要求 |
|---------|--------------|--------------|---------|---------------|---------|

> **对下游硬要求**：impact-analyzer 据此判断本 Workbench 是提供方还是消费方；
> 字段名/类型在 LLD 细化，但不得超出本契约范围。

## §5 对下游各 Workbench 的输入约束
*来源：汇总 §2/§3/§4*

**按 Workbench 分列**：每个涉及 Workbench 在 impact-analyzer（影响分析）与 architecture-advisor（LLD）
阶段必须遵守的架构方向（如"X Workbench 走 Kafka 异步，不分析同步路径"）。

> **对下游硬要求**：impact-analyzer 与 architecture-advisor **均在 §5 本 Workbench 约束内展开**，不另行假设架构方向。

## §6 已产出 ADR 草稿
*来源：Step 2 draft-adr*

列本次 draft-adr 生成的 `ADR-{N}-{标题}`（PROPOSED）+ 一句话决策摘要 + 文件链接。

## §7 需求修订记录
*来源：Step 5*

- 有修订 → 引用 `requirements-delta.md` 逐条列
- 无修订 → 明确写"本次 HLD 无需求修订"

## §8 未验证假设清单
*来源：汇总全文*

汇总全文所有 `⚠️ 假设未验证` 项：

| 假设内容 | 涉及 Workbench | 待验证方 |
|---------|--------------|---------|

> **对下游硬要求**：impact-analyzer 加载 HLD 后**逐条码级验证**：一致则确认，冲突则标注并回流「待澄清问题」。
