# Agent Adapter — AI Agent 多目标适配工具

将 harness-framework 生成的 doc-workspace 内容适配并分发到主流 AI Agent 开发工具的约定目录。

---

## 支持的 AI Agent

| Agent | 目标目录 | 适配方式 |
|-------|---------|---------|
| **Claude Code** | `.claude/skills/` | 直接复制 SKILL.md（原生兼容） |
| **Qoder** | `.qoder/skills/` | 直接复制 SKILL.md（原生兼容） |
| **Kiro** | `.kiro/steering/` + `.kiro/skills/` | rules/memory 转为 steering 格式 + skills 复制 |
| **Cursor** | `.cursor/rules/*.mdc` | rules/constraints 转为 .mdc 格式 |
| **Codex** | `.codex/rules/` + `.codex/skills/` | rules/constraints 转为纯 Markdown + skills 转为 Markdown |

---

## 快速开始

```bash
# 前提：已通过 project-setup 生成 doc-workspace

# 自动检测用户机器上已安装的 AI Agent（推荐）
# 在 workspace 目录本身下生成 .kiro / .claude 等配置
python agent-adapter/adapt.py -w ./my-doc-workspace --auto

# 适配所有 Agent
python agent-adapter/adapt.py -w ./my-doc-workspace -t all

# 仅适配 Claude Code
python agent-adapter/adapt.py -w ./my-doc-workspace -t claude-code

# 适配 Kiro，同时分发到代码仓库（额外目标）
python agent-adapter/adapt.py -w ./my-doc-workspace -t kiro -d /path/to/code-repo
```

适配完成后，用目标 AI Agent 工具打开 workspace 目录即可生效。

---

## 命令参数

| 参数 | 简写 | 必填 | 说明 |
|------|------|------|------|
| `--workspace` | `-w` | 是 | doc-workspace 路径（适配产物默认生成在此目录下） |
| `--target` | `-t` | 二选一 | 目标 Agent：`claude-code` / `qoder` / `kiro` / `cursor` / `codex` / `all` |
| `--auto` | `-a` | 二选一 | 自动检测用户机器上已安装的 AI Agent |
| `--dest` | `-d` | 否 | 额外分发目标路径（不指定则只在 workspace 目录下生成） |

> `--target` 与 `--auto` 互斥，必须指定其中一个。

### `--auto` 自动检测逻辑

脚本会**扫描用户主目录**（跨平台），检测已安装的 AI Agent：

**Windows 检测路径：**
| Agent | 检测位置 |
|-------|---------|
| Claude Code | `~/.claude/`、`%LOCALAPPDATA%/claude/`、`%APPDATA%/claude/` |
| Qoder | `%LOCALAPPDATA%/Qoder/`、`%APPDATA%/Qoder/` |
| Kiro | `~/.kiro/`、`%LOCALAPPDATA%/kiro/`、`%LOCALAPPDATA%/Programs/kiro/` |
| Cursor | `%LOCALAPPDATA%/cursor/`、`%APPDATA%/Cursor/`、`~/.cursor/` |
| Codex | `~/.codex/`、`%LOCALAPPDATA%/codex/` |

**macOS 检测路径：**
| Agent | 检测位置 |
|-------|---------|
| Claude Code | `~/.claude/`、`/Applications/Claude Code.app` |
| Qoder | `/Applications/Qoder.app`、`~/Library/Application Support/Qoder/` |
| Kiro | `~/.kiro/`、`/Applications/Kiro.app`、`~/Library/Application Support/kiro/` |
| Cursor | `~/.cursor/`、`/Applications/Cursor.app`、`~/Library/Application Support/Cursor/` |
| Codex | `~/.codex/` |

**Linux 检测路径：**
| Agent | 检测位置 |
|-------|---------|
| Claude Code | `~/.claude/`、`~/.local/share/claude/` |
| Qoder | `~/.qoder/`、`~/.local/share/Qoder/` |
| Kiro | `~/.kiro/` |
| Cursor | `~/.cursor/`、`~/.config/Cursor/` |
| Codex | `~/.codex/` |

检测逻辑：
- **检测到已安装 Agent** → 仅适配检测到的
- **未检测到任何 Agent** → 进入交互选择菜单

### 适配产物生成位置

默认在 **workspace 目录本身**下生成 `.kiro/`、`.claude/` 等。如果通过 `--dest` 指定了额外目标，则同时向该目标分发。

---

## 适配规则详解

### Claude Code

| 源文件 | 目标 | 说明 |
|--------|------|------|
| `skills/{name}/` | `.claude/skills/{name}/` | 完整复制（含 SKILL.md + references/） |
| `CLAUDE.md` | 根目录已有 | Claude Code 自动读取，无需操作 |

### Qoder

| 源文件 | 目标 | 说明 |
|--------|------|------|
| `skills/{name}/` | `.qoder/skills/{name}/` | 完整复制（含 SKILL.md + references/） |
| `AGENTS.md` | 根目录已有 | Qoder 自动读取，无需操作 |

### Kiro

| 源文件 | 目标 | 说明 |
|--------|------|------|
| `CLAUDE.md` | `.kiro/steering/project-overview.md` 等 | 拆分为多个 steering 文件 |
| `rules/{wb}/*.md` | `.kiro/steering/{wb}-*.md` | 去 frontmatter，加 steering 格式（description + globs） |
| `project-memory/constraints.md` | `.kiro/steering/constraints.md` | 去 frontmatter，加 steering 格式 |
| `project-memory/glossary.md` | `.kiro/steering/glossary.md` | 同上 |
| `skills/{name}/` | `.kiro/skills/{name}/` | 完整复制（Kiro 兼容 Agent Skills 标准） |

**注意**：Kiro 需要重启 IDE 才能加载新生成的 steering 文件。

### Cursor

| 源文件 | 目标 | 说明 |
|--------|------|------|
| `rules/{wb}/*.md` | `.cursor/rules/{wb}-*.mdc` | 转 .mdc 格式（description + globs + alwaysApply: false） |
| `project-memory/constraints.md` | `.cursor/rules/constraints.mdc` | 转 .mdc（alwaysApply: true，始终生效） |
| skills 核心纪律 | `.cursor/rules/development-workflow.mdc` | 提取流水线映射（alwaysApply: true） |
| `AGENTS.md` | 根目录已有 | Cursor 原生支持，无需操作 |

**注意**：Cursor 不使用 SKILL.md 体系，开发纪律通过 rules + AGENTS.md 实现。

### Codex (OpenAI Codex CLI)

| 源文件 | 目标 | 说明 |
|--------|------|------|
| `rules/{wb}/*.md` | `.codex/rules/{wb}-*.md` | 去 frontmatter，纯 Markdown |
| `project-memory/constraints.md` | `.codex/constraints.md` | 去 frontmatter，纯 Markdown |
| `project-memory/glossary.md` | `.codex/glossary.md` | 去 frontmatter，纯 Markdown |
| `skills/{name}/SKILL.md` | `.codex/skills/{name}.md` | 去 frontmatter，纯 Markdown + 复制 references/ |
| skills 核心纪律 | `.codex/development-workflow.md` | 提取流水线映射 |
| `AGENTS.md` | 根目录已有 | Codex 自动读取，无需操作 |

**注意**：Codex 以 AGENTS.md 为核心指令机制，支持目录层级嵌套（最近的 AGENTS.md 优先）。
建议在根目录 `AGENTS.md` 中添加 `.codex/` 下文件的引用（如 `See .codex/development-workflow.md`）以自动加载。

---

## 设计原则

1. **doc-workspace 是唯一事实源**：适配是单向投影，不回写
2. **幂等执行**：每次运行先清空目标目录再写入，可安全重复执行
3. **格式隔离**：各 Agent 的 frontmatter 格式独立生成，互不干扰
4. **策略模式**：新增 Agent 只需在 `adapters/` 下添加一个 Python 文件
5. **workspace 内生优先**：默认在 workspace 自身生成配置，用 `--dest` 可额外向代码仓库分发

---

## 目录结构

```
agent-adapter/
├── README.md                  # 本文件
├── adapt.py                   # 主入口脚本
└── adapters/
    ├── __init__.py
    ├── base.py                # 适配基类（Workspace 读取 + frontmatter 工具 + glob 推断）
    ├── claude_code.py         # Claude Code 适配器
    ├── qoder.py               # Qoder 适配器
    ├── kiro.py                # Kiro 适配器
    ├── cursor.py              # Cursor 适配器
    └── codex.py               # OpenAI Codex 适配器
```

---

## 扩展：新增 Agent 支持

1. 在 `adapters/` 下新建 `{agent_name}.py`
2. 继承 `BaseAdapter`，设置 `name` 和 `target_dir`
3. 实现 `adapt()` 方法
4. 在 `adapt.py` 的 `ADAPTERS` 字典中注册
5. 在 `adapt.py` 的 `_get_agent_install_paths()` 中添加新 Agent 的检测路径
