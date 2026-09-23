"""
base.py — 适配基类

提供所有适配器共享的通用能力：
- 读取 doc-workspace 内容（project-config.yaml、CLAUDE.md、skills/、rules/、project-memory/）
- Markdown frontmatter 处理（剥离、生成）
- 技术栈到 glob 模式的推断
- 文件系统工具（安全创建目录、写入文件、清空目录）
"""

import os
import re
import shutil


class Workspace:
    """封装对 doc-workspace 的读取操作。"""

    def __init__(self, workspace_path):
        self.path = os.path.abspath(workspace_path)
        if not os.path.isdir(self.path):
            raise FileNotFoundError(f"Workspace not found: {self.path}")

    # ---- 路径快捷方式 ----

    def skills_dir(self):
        return os.path.join(self.path, "skills")

    def rules_dir(self):
        return os.path.join(self.path, "rules")

    def memory_dir(self):
        return os.path.join(self.path, "project-memory")

    def claude_md_path(self):
        return os.path.join(self.path, "CLAUDE.md")

    def config_path(self):
        return os.path.join(self.path, "project-config.yaml")

    # ---- 读取操作 ----

    def read_file(self, relative_path):
        """读取 workspace 内文件，返回字符串内容。"""
        filepath = os.path.join(self.path, relative_path)
        if not os.path.isfile(filepath):
            return None
        with open(filepath, "r", encoding="utf-8") as f:
            return f.read()

    def read_claude_md(self):
        """读取 CLAUDE.md 全文。"""
        return self.read_file("CLAUDE.md")

    def read_config(self):
        """读取 project-config.yaml 全文（原始字符串，不解析）。"""
        return self.read_file("project-config.yaml")

    def list_skills(self, exclude=("project-setup",)):
        """
        列出 skills/ 下所有 Skill 目录名。
        exclude: 要排除的 Skill 名称元组。
        """
        skills_dir = self.skills_dir()
        if not os.path.isdir(skills_dir):
            return []
        result = []
        for name in sorted(os.listdir(skills_dir)):
            full = os.path.join(skills_dir, name)
            if os.path.isdir(full) and name not in exclude:
                result.append(name)
        return result

    def list_rules(self):
        """
        列出 rules/ 下所有规范文件。
        返回 [(workbench, filename, abspath), ...]
        """
        rules_dir = self.rules_dir()
        if not os.path.isdir(rules_dir):
            return []
        result = []
        for workbench in sorted(os.listdir(rules_dir)):
            wb_dir = os.path.join(rules_dir, workbench)
            if not os.path.isdir(wb_dir):
                continue
            for fname in sorted(os.listdir(wb_dir)):
                if fname.endswith(".md"):
                    result.append((workbench, fname, os.path.join(wb_dir, fname)))
        return result

    def get_workbench_keys(self):
        """从 rules/ 子目录推断 Workbench 列表。"""
        rules_dir = self.rules_dir()
        if not os.path.isdir(rules_dir):
            return []
        return [d for d in sorted(os.listdir(rules_dir))
                if os.path.isdir(os.path.join(rules_dir, d))]


# ============================================================
# Markdown frontmatter 工具
# ============================================================

_FRONTMATTER_PATTERN = re.compile(r'^---\s*\n(.*?)\n---\s*\n', re.DOTALL)


def strip_frontmatter(content):
    """
    去掉 Markdown 文件开头的 YAML frontmatter（--- ... ---），返回正文。
    如果没有 frontmatter，返回原文。
    """
    match = _FRONTMATTER_PATTERN.match(content)
    if match:
        return content[match.end():]
    return content


def extract_frontmatter(content):
    """
    提取 YAML frontmatter 为字符串。如果没有，返回 None。
    """
    match = _FRONTMATTER_PATTERN.match(content)
    if match:
        return match.group(1)
    return None


def build_frontmatter(fields_dict):
    """
    从有序字段字典构建 YAML frontmatter 字符串。
    fields_dict: OrderedDict 或普通 dict，key=value。
    返回 '---\nkey: value\n---\n'
    """
    lines = ["---"]
    for k, v in fields_dict.items():
        lines.append(f"{k}: {v}")
    lines.append("---")
    return "\n".join(lines) + "\n"


# ============================================================
# glob 推断
# ============================================================

# 技术栈关键词 -> glob 模式
_GLOB_MAP = [
    ("java", "**/*.java"),
    ("spring", "**/*.java"),
    ("kotlin", "**/*.kt"),
    ("typescript", "**/*.{ts,tsx}"),
    ("react", "**/*.{ts,tsx,js,jsx}"),
    ("vue", "**/*.vue"),
    ("python", "**/*.py"),
    ("fastapi", "**/*.py"),
    ("django", "**/*.py"),
    ("go", "**/*.go"),
    ("rust", "**/*.rs"),
    ("csharp", "**/*.cs"),
    (".net", "**/*.cs"),
    ("node", "**/*.{js,ts}"),
    ("flutter", "**/*.dart"),
    ("swift", "**/*.swift"),
    ("objective-c", "**/*.{m,mm}"),
]


def infer_globs(tech_stack_str):
    """
    根据技术栈字符串（如 'Java 8 + Spring Boot 2.7'）推断 glob 模式。
    返回 glob 字符串（逗号分隔多个模式）。
    """
    if not tech_stack_str:
        return "**/*"
    text = tech_stack_str.lower()
    globs = []
    for keyword, pattern in _GLOB_MAP:
        if keyword in text and pattern not in globs:
            globs.append(pattern)
    if not globs:
        return "**/*"
    return ", ".join(globs)


# ============================================================
# 文件系统工具
# ============================================================

def ensure_dir(path):
    """安全创建目录（含父目录）。"""
    os.makedirs(path, exist_ok=True)


def write_file(filepath, content):
    """写入文件（UTF-8，自动创建父目录）。"""
    ensure_dir(os.path.dirname(filepath))
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)


def copy_tree(src, dst):
    """复制目录树（覆盖目标）。"""
    if os.path.exists(dst):
        shutil.rmtree(dst)
    shutil.copytree(src, dst)


def clean_dir(path):
    """清空目录内容（保留目录本身）。"""
    if os.path.exists(path):
        shutil.rmtree(path)
    os.makedirs(path, exist_ok=True)


def copy_file(src, dst):
    """复制单个文件（自动创建父目录）。"""
    ensure_dir(os.path.dirname(dst))
    shutil.copy2(src, dst)


# ============================================================
# 适配器基类
# ============================================================

class BaseAdapter:
    """
    所有适配器的基类。
    子类需实现 adapt() 方法。
    """

    # 子类必须覆盖
    name = "base"
    target_dir = ""  # 如 ".claude", ".qoder", ".kiro", ".cursor"

    def __init__(self, workspace, dest=None):
        """
        workspace: Workspace 实例
        dest: 分发目标根目录（默认 = workspace.path）
        """
        self.ws = workspace
        self.dest = os.path.abspath(dest) if dest else workspace.path

    def target_root(self):
        """返回目标 Agent 目录的完整路径。"""
        return os.path.join(self.dest, self.target_dir)

    def log(self, message):
        print(f"  [{self.name}] {message}")

    def adapt(self):
        """子类必须实现。"""
        raise NotImplementedError
