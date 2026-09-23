# 断点续跑协议 — 背景与运行指引

## 为什么要分步运行

本 Skill 流程长、轮次多。**单会话跑完整套 N 个 Workbench 采集会导致上下文膨胀**——采集问答、
模式 B 读入的整份文档、Phase 3 生成的文件正文层层累积且永不驱逐，Workbench 数 N 越大膨胀越严重。

本 Skill 设计为**可分步、可重入**：在任意阶段边界结束会话（或 `/clear`），
下次重新进入时从磁盘恢复进度，只续做未完成的部分。这是「确认即落盘」的回报——磁盘是唯一事实源，会话可随时丢弃重建。

## 推荐运行姿势

把下列边界视为**独立调用**，每个边界后另起 fresh 会话：

```
① Phase 1（基础信息）
→ ② 每个 Workbench 的 Phase 2 各跑一个独立会话（一 Workbench 一会话）
→ ③ Phase 3（生成）
```

这样每个会话只承载「一个阶段 / 一个 Workbench」的上下文，与 Workbench 数量 N 解耦。
Marathon 单会话仍可用，但不推荐用于多 Workbench（≥ 3）项目。

## 进度探测逻辑（详细展开）

§0 精简版的完整判断规则：

1. **目标路径**：询问后才能定位已落盘产物，续跑必须先知道路径。
2. **`project-config.yaml` 全局字段**：
   - `project.*`（name/slug/ticket_prefix）、`dirs.*`（requirements/module_exploration）、`roles` 任一含 `{{PLACEHOLDER}}` → Phase 1 落盘步未完成
   - 所有全局字段已填实 → Phase 1 完成
3. **逐 Workbench 判断**（对 `Workbenchs` 列表每个条目）：
   - config 条目的 `doc_root` 或 `repositories` 含 `# TODO` → Phase 2 未完成
   - 任一按 Workbench skill 的 `references/{Workbench}.md` 文件缺失，或文件内含未填的 `<!-- TECH_SPECIFIC -->` → Phase 2 未完成
   - dev-self-test 额外检查：`templates/{Workbench}_dev_test_template.md` 是否存在且已填充
   - 以上均通过 → 该 Workbench Phase 2 完成
4. **Phase 3 内部探测**（仅在全部 Workbench Phase 2 完成时）。
   **落到 Phase 3 即自主执行**：探测出哪些步未完成后，**直接按 3.1→3.9 把未完成步跑到底，不停下等用户指令**——Phase 3 无用户输入需求，唯一会停的是 3.1 校验发现残留桩值时 BLOCK。逐步探测规则：
   - `CLAUDE.md` / `AGENTS.md` 存在 → 3.2 已完成，跳过
   - `local_profile.yaml.example` 存在 → 3.3 已完成，跳过
   - `.gitignore` 存在 → 3.4 已完成，跳过
   - **3.5（本地化全部 Skill）完成判定**——枚举对象是 **framework `skills/` 目录的完整 Skill 列表（`project-setup` 除外）**，**不是** target 目录里已有的子集。对该列表每个 skill：当且仅当 `{target}/skills/{skill}/SKILL.md` **存在且**无 `{{PLACEHOLDER}}` 残留，才算该 skill 已本地化。
     - **⚠️ SKILL.md 缺失 = 未完成，必须本地化，绝不可跳过。** Phase 2 只写 `references/{Workbench}.md`、**从不写 SKILL.md**；故 Phase 2 刚结束时 target 里 SKILL.md 数为 0，3.5 必然待执行。
     - **不要拿「target 里只有几个带 references 的 skill 目录」当作 skill 全集**——那只是 Phase 2 落 references 的子集；要本地化的是 framework 全集（含无 references 的 skill，如 requirement-change-router / memory-curator / prd-writer）。
     - 完成标准：framework 全集（除 project-setup）的**每个** skill 在 target 都有无占位符的 `SKILL.md`。任一缺失/含占位符 → 3.5 未完成。
   - `project-memory/MEMORY.md` 存在 → 3.6 已完成，跳过
   - `doc-templates/` 和 `global-info/` 存在 → 3.7 已完成，跳过
   - 各 Workbench `doc_root/` 目录存在 → 3.8 已完成，跳过
