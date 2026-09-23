"""
claude_code.py — Claude Code 适配器

Claude Code 原生兼容 Agent Skills 标准（SKILL.md 格式），
适配只需将 skills/ 目录复制到 .claude/skills/ 下即可。
"""

import os

from .base import BaseAdapter, clean_dir, copy_tree


class ClaudeCodeAdapter(BaseAdapter):
    name = "claude-code"
    target_dir = ".claude"

    def adapt(self):
        skills = self.ws.list_skills()
        if not skills:
            self.log("No skills found, skipping.")
            return

        target_skills = os.path.join(self.target_root(), "skills")
        clean_dir(target_skills)

        for skill_name in skills:
            src = os.path.join(self.ws.skills_dir(), skill_name)
            dst = os.path.join(target_skills, skill_name)
            copy_tree(src, dst)
            self.log(f"Copied skill: {skill_name}")

        # CLAUDE.md 已在根目录，Claude Code 自动读取，无需额外操作
        self.log(f"Done. {len(skills)} skills -> {target_skills}")
        self.log("CLAUDE.md already at workspace root (auto-loaded by Claude Code).")
