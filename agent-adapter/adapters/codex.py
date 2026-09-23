"""
codex.py — OpenAI Codex CLI 适配器

Codex 以 AGENTS.md（纯 Markdown）为核心指令机制，支持目录层级嵌套（最近的
AGENTS.md 优先）。Codex 不使用 SKILL.md 体系，也没有 Cursor 的 .mdc 格式。

注意：Codex 的 rules/ 目录仅支持 *.rules（Starlark 语法，控制命令权限），
不放编码规范 Markdown，因此本适配器不生成 rules/ 目录。

适配策略：
1. 将 project-memory/constraints 转为 .codex/constraints.md
2. 将 project-memory/glossary 转为 .codex/glossary.md
3. 将每个 SKILL.md 转为 .codex/skills/{name}.md（去掉 frontmatter，纯 Markdown）
4. 生成 .codex/development-workflow.md（Skill 流水线索引，供 Codex 参考）
5. AGENTS.md 已在根目录，Codex 自动读取
"""

import os
import re

from .base import (
    BaseAdapter, clean_dir, copy_tree,
    strip_frontmatter, write_file,
)


class CodexAdapter(BaseAdapter):
    name = "codex"
    target_dir = ".codex"

    def adapt(self):
        # 1. 转换 constraints -> .codex/constraints.md
        self._adapt_constraints()

        # 2. 转换 glossary -> .codex/glossary.md
        self._adapt_glossary()

        # 3. 转换 skills -> .codex/skills/*.md + 复制 references
        self._adapt_skills()

        # 4. 生成 development-workflow.md
        self._adapt_workflow()

        # AGENTS.md 已在根目录，Codex 自动读取
        self.log(f"Done. Adapted content -> {self.target_root()}")
        self.log("AGENTS.md already at workspace root (auto-loaded by Codex).")
        self.log("Tip: 在 AGENTS.md 中添加 .codex/ 下文件的引用以自动加载。")

    # ---- constraints 转换 ----

    def _adapt_constraints(self):
        constraints = self.ws.read_file("project-memory/architectural_constraints.md")
        if not constraints:
            return

        body = strip_frontmatter(constraints)
        write_file(
            os.path.join(self.target_root(), "constraints.md"),
            "# 项目架构约束（BLOCK/WARN 级别）\n\n"
            "> 开发前必读。以下约束具有最高优先级。\n\n"
            f"{body}\n",
        )
        self.log("Created: constraints.md")

    # ---- glossary 转换 ----

    def _adapt_glossary(self):
        glossary = self.ws.read_file("project-memory/project_glossary.md")
        if not glossary:
            return

        body = strip_frontmatter(glossary)
        write_file(
            os.path.join(self.target_root(), "glossary.md"),
            "# 项目术语表\n\n"
            f"{body}\n",
        )
        self.log("Created: glossary.md")

    # ---- skills/ 转换 ----

    def _adapt_skills(self):
        """
        将每个 SKILL.md 转为 .codex/skills/{name}.md（纯 Markdown）。
        Codex 不原生支持 Skill 触发，但可通过在 AGENTS.md 中引用来手动加载。
        同时复制 references/ 供 Codex 在执行时按需读取。
        """
        skills = self.ws.list_skills()
        if not skills:
            self.log("No skills found, skipping.")
            return

        skills_dir = os.path.join(self.target_root(), "skills")
        clean_dir(skills_dir)

        skill_count = 0
        for skill_name in skills:
            skill_dir = os.path.join(self.ws.skills_dir(), skill_name)
            skill_md_path = os.path.join(skill_dir, "SKILL.md")

            if not os.path.isfile(skill_md_path):
                continue

            with open(skill_md_path, "r", encoding="utf-8") as f:
                content = f.read()

            # 提取 description（从 frontmatter）
            original_desc = self._extract_skill_description(content)
            body = strip_frontmatter(content)

            # 检查是否有 references/
            ref_dir = os.path.join(skill_dir, "references")
            has_refs = os.path.isdir(ref_dir) and os.listdir(ref_dir)

            # 检查是否有 templates/
            tpl_dir = os.path.join(skill_dir, "templates")
            has_tpls = os.path.isdir(tpl_dir) and os.listdir(tpl_dir)

            ref_note = ""
            if has_refs or has_tpls:
                ref_parts = []
                if has_refs:
                    ref_parts.append("references/（本 Workbench 的技术绑定知识）")
                    # 复制 references 到 .codex/skills/{name}/references/
                    ref_target = os.path.join(skills_dir, skill_name, "references")
                    copy_tree(ref_dir, ref_target)
                if has_tpls:
                    ref_parts.append("templates/（测试模板）")
                    tpl_target = os.path.join(skills_dir, skill_name, "templates")
                    copy_tree(tpl_dir, tpl_target)

                ref_note = (
                    f"\n> **关联资源**：执行本 Skill 时，按需读取 "
                    f"`skills/{skill_name}/` 下的 {' 和 '.join(ref_parts)}。\n"
                )

            # 构建纯 Markdown 文件
            header = f"# Skill: {skill_name}\n"
            if original_desc:
                header += f"> {original_desc}\n"

            write_file(
                os.path.join(skills_dir, f"{skill_name}.md"),
                f"{header}\n{ref_note}{body}\n",
            )
            skill_count += 1

        self.log(f"Converted {skill_count} SKILL.md -> .codex/skills/*.md")

    def _extract_skill_description(self, content):
        """从 SKILL.md 的 YAML frontmatter 中提取 description 字段。"""
        match = re.match(r'^---\s*\n(.*?)\n---\s*\n', content, re.DOTALL)
        if not match:
            return None
        fm_text = match.group(1)
        desc_match = re.search(r'description:\s*[>)]?\s*\n?(.*?)(?:\n[a-z]|\n---|\Z)', fm_text, re.DOTALL)
        if desc_match:
            desc = desc_match.group(1).strip()
            desc = re.sub(r'^\s+', '', desc, flags=re.MULTILINE)
            return desc if desc else None
        return None

    # ---- development-workflow.md 生成 ----

    def _adapt_workflow(self):
        """
        生成 development-workflow.md，包含核心开发纪律和 Skill 流水线映射。
        供 Codex 作为开发参考（可在 AGENTS.md 中引用）。
        """
        skills = self.ws.list_skills()

        lines = [
            "# 开发工作流纪律\n",
            "> 本文件由 harness-framework agent-adapter 从 skills/ 提取生成。\n",
            "## 核心原则\n",
            "- **确认即落盘**：所有已确认的产出物立即写入文件，不囤在对话上下文\n",
            "- **文件权威 > 对话记忆**：约束、配置以文件为准，执行前读文件\n",
            "- **人类门控**：关键决策点 BLOCK/QUESTION，由人确认后才推进\n",
            "- **可重入**：长流程能读文件判断进度，可随时中断续跑\n",
            "## 执行前检查\n",
            "- 检查 local_profile.yaml 是否存在（不存在则 BLOCK）\n",
            "- 读取 project-config.yaml 获取项目配置\n",
            "- 读取 .codex/constraints.md 获取架构约束\n",
            "## 开发流水线\n",
            "| 阶段 | 对应 Skill 文件 | 说明 |\n",
            "|------|----------------|------|\n",
        ]

        pipeline = [
            ("需求启动", "requirement-kickoff", "读 PRD -> 业务澄清 -> 派活"),
            ("影响分析", "impact-analyzer", "分析需求对本 Workbench 的影响"),
            ("需求汇总", "requirements-analyst", "收敛为带 AC 的 requirements.md"),
            ("架构设计", "architecture-advisor", "Schema/API/技术设计/任务拆分"),
            ("功能开发", "feature-developer", "Gather-Act-Verify 循环实现"),
            ("代码走读", "code-walkthrough", "讲师式分段带读"),
            ("代码审查", "code-reviewer", "结构化审查"),
            ("开发自测", "dev-self-test", "白盒自测用例"),
            ("造数脚本", "test-data-script-generator", "造数/清数脚本"),
        ]

        for stage, skill, desc in pipeline:
            if skill in skills:
                lines.append(f"| {stage} | `skills/{skill}.md` | {desc} |\n")

        lines.append('\n> 各 Skill 已转为 .codex/skills/*.md，在 AGENTS.md 中引用以加载。\n')

        write_file(
            os.path.join(self.target_root(), "development-workflow.md"),
            "".join(lines),
        )
        self.log("Created: development-workflow.md")
