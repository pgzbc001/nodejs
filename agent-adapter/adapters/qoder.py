"""
qoder.py — Qoder 适配器

Qoder 同样原生兼容 Agent Skills 标准（SKILL.md 格式），
适配只需将 skills/ 目录复制到 .qoder/skills/ 下即可。
"""

import os

from .base import BaseAdapter, clean_dir, copy_tree


class QoderAdapter(BaseAdapter):
    name = "qoder"
    target_dir = ".qoder"

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

        # AGENTS.md 已在根目录，Qoder 自动读取
        self.log(f"Done. {len(skills)} skills -> {target_skills}")
        self.log("AGENTS.md already at workspace root (auto-loaded by Qoder).")
