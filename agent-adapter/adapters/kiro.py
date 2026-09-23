"""
kiro.py — Kiro 适配器

Kiro 以 Steering + Specs 为核心，不直接使用 SKILL.md 体系。
适配需要：
1. 将 CLAUDE.md 拆分为 steering 文件
2. 将 rules/ 转为 steering 格式（加 description + globs frontmatter）
3. 将 project-memory/constraints 转为 steering 格式
4. 将 skills/ 复制到 .kiro/skills/（Kiro 兼容 Agent Skills 标准）
"""

import os
import re

from .base import (
    BaseAdapter, clean_dir, copy_tree,
    strip_frontmatter, build_frontmatter, infer_globs, write_file,
)


class KiroAdapter(BaseAdapter):
    name = "kiro"
    target_dir = ".kiro"

    def adapt(self):
        steering_dir = os.path.join(self.target_root(), "steering")
        clean_dir(steering_dir)

        # 1. 拆分 CLAUDE.md -> steering 文件
        self._adapt_claude_md(steering_dir)

        # 2. 转换 rules/ -> steering
        self._adapt_rules(steering_dir)

        # 3. 转换 project-memory -> steering
        self._adapt_memory(steering_dir)

        # 4. 复制 skills/
        self._adapt_skills()

        self.log(f"Done. Steering -> {steering_dir}")

    # ---- CLAUDE.md 拆分 ----

    def _adapt_claude_md(self, steering_dir):
        content = self.ws.read_claude_md()
        if not content:
            self.log("CLAUDE.md not found, skipping steering split.")
            return

        # 提取项目名称（第一行 # 开头）
        name_match = re.search(r'^#\s+(.+)$', content, re.MULTILINE)
        project_name = name_match.group(1).strip() if name_match else "Project"

        # 拆分主要 section（以 --- 或 ## 分隔）
        sections = self._split_sections(content)

        # 写入 project-overview.md（概览 + 角色访问控制 + memory 说明）
        overview_parts = []
        for title in ["项目概览", "角色与访问控制", "Memory 系统说明"]:
            if title in sections:
                overview_parts.append(f"## {title}\n\n{sections[title]}")
        if overview_parts:
            fm = build_frontmatter({
                "description": f"{project_name} 项目概览与角色配置",
                "inclusion": "always",
            })
            write_file(
                os.path.join(steering_dir, "project-overview.md"),
                fm + "\n" + "\n\n".join(overview_parts) + "\n",
            )
            self.log("Created: project-overview.md")

        # 写入 tech-stack.md（技术栈 section）
        if "技术栈" in sections:
            fm = build_frontmatter({
                "description": f"{project_name} 技术栈清单",
                "inclusion": "always",
            })
            write_file(
                os.path.join(steering_dir, "tech-stack.md"),
                fm + "\n## 技术栈\n\n" + sections["技术栈"] + "\n",
            )
            self.log("Created: tech-stack.md")

        # 写入 repo-structure.md（仓库结构 section）
        if "仓库结构" in sections:
            fm = build_frontmatter({
                "description": f"{project_name} 仓库结构与导航",
                "inclusion": "manual",
            })
            write_file(
                os.path.join(steering_dir, "repo-structure.md"),
                fm + "\n## 仓库结构\n\n" + sections["仓库结构"] + "\n",
            )
            self.log("Created: repo-structure.md")

    def _split_sections(self, content):
        """按 ## 标题拆分内容，返回 {标题: 正文}。"""
        sections = {}
        # 去掉一级标题行
        # 按 ## 分割
        parts = re.split(r'\n##\s+', content)
        for part in parts[1:]:  # 跳过第一个（一级标题前的内容）
            lines = part.split("\n", 1)
            title = lines[0].strip()
            body = lines[1].strip() if len(lines) > 1 else ""
            sections[title] = body
        return sections

    # ---- rules/ 转换 ----

    def _adapt_rules(self, steering_dir):
        rules = self.ws.list_rules()
        if not rules:
            self.log("No rules/ found, skipping.")
            return

        # 推断每个 workbench 的 glob
        config = self.ws.read_config() or ""
        wb_globs = {}
        for workbench in self.ws.get_workbench_keys():
            # 从 config 中粗略提取技术栈信息
            glob = infer_globs(config)
            wb_globs[workbench] = glob

        for workbench, fname, filepath in rules:
            with open(filepath, "r", encoding="utf-8") as f:
                content = f.read()

            body = strip_frontmatter(content)

            # 生成描述
            base_name = fname.replace(".md", "")
            description = f"{workbench} {base_name} 规范"

            # 推断 glob
            globs = wb_globs.get(workbench, "**/*")

            fm = build_frontmatter({
                "description": description,
                "inclusion": "fileMatch",
                "globs": globs,
            })

            target_name = f"{workbench}-{base_name}.md"
            write_file(
                os.path.join(steering_dir, target_name),
                fm + "\n" + body,
            )
            self.log(f"Created: {target_name}")

    # ---- project-memory 转换 ----

    def _adapt_memory(self, steering_dir):
        # architectural_constraints.md
        constraints = self.ws.read_file("project-memory/architectural_constraints.md")
        if constraints:
            body = strip_frontmatter(constraints)
            fm = build_frontmatter({
                "description": "项目架构约束（BLOCK/WARN 级别），开发前必读",
                "inclusion": "always",
            })
            write_file(
                os.path.join(steering_dir, "constraints.md"),
                fm + "\n" + body,
            )
            self.log("Created: constraints.md")

        # project_glossary.md
        glossary = self.ws.read_file("project-memory/project_glossary.md")
        if glossary:
            body = strip_frontmatter(glossary)
            fm = build_frontmatter({
                "description": "项目术语表",
                "inclusion": "manual",
            })
            write_file(
                os.path.join(steering_dir, "glossary.md"),
                fm + "\n" + body,
            )
            self.log("Created: glossary.md")

    # ---- skills/ 复制 ----

    def _adapt_skills(self):
        skills = self.ws.list_skills()
        if not skills:
            self.log("No skills to copy.")
            return

        target_skills = os.path.join(self.target_root(), "skills")
        clean_dir(target_skills)

        for skill_name in skills:
            src = os.path.join(self.ws.skills_dir(), skill_name)
            dst = os.path.join(target_skills, skill_name)
            copy_tree(src, dst)
            self.log(f"Copied skill: {skill_name}")
