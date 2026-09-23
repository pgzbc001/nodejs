"""
cursor.py — Cursor 适配器

Cursor 使用 .cursor/rules/*.mdc 格式（YAML frontmatter + Markdown 正文）。
适配需要：
1. 将 rules/ 转为 .mdc 格式（加 description + globs + alwaysApply）
2. 将 constraints 转为始终生效的 .mdc（alwaysApply: true）
3. 将每个 SKILL.md 转为 .mdc（alwaysApply: false，通过 @skill-name 手动触发）
4. 复制 skills/references/ 供 AI 在执行 Skill 时按需读取
5. 生成 development-workflow.mdc（alwaysApply: true，Skill 索引）
6. AGENTS.md 已在根目录，Cursor 原生支持
"""

import os
import re

from .base import (
    BaseAdapter, clean_dir, copy_tree,
    strip_frontmatter, build_frontmatter, infer_globs, write_file,
)


class CursorAdapter(BaseAdapter):
    name = "cursor"
    target_dir = ".cursor"

    def adapt(self):
        rules_dir = os.path.join(self.target_root(), "rules")
        clean_dir(rules_dir)

        # 推断 glob
        config = self.ws.read_config() or ""
        default_glob = infer_globs(config)

        # 1. 转换 rules/ -> .mdc
        self._adapt_rules(rules_dir, default_glob)

        # 2. 转换 constraints -> .mdc (alwaysApply: true)
        self._adapt_constraints(rules_dir)

        # 3. 将 SKILL.md 转为 .mdc（手动触发）+ 复制 references
        self._adapt_skills(rules_dir)

        # 4. 生成 development-workflow.mdc（Skill 索引，alwaysApply: true）
        self._adapt_workflow(rules_dir)

        # AGENTS.md 已在根目录，Cursor 原生支持
        self.log(f"Done. Rules -> {rules_dir}")
        self.log("AGENTS.md already at workspace root (auto-loaded by Cursor).")

    def _adapt_rules(self, rules_dir, default_glob):
        rules = self.ws.list_rules()
        if not rules:
            self.log("No rules/ found, skipping.")
            return

        for workbench, fname, filepath in rules:
            with open(filepath, "r", encoding="utf-8") as f:
                content = f.read()

            body = strip_frontmatter(content)
            base_name = fname.replace(".md", "")
            description = f"{workbench} {base_name} 规范"

            fm = build_frontmatter({
                "description": description,
                "globs": default_glob,
                "alwaysApply": "false",
            })

            target_name = f"{workbench}-{base_name}.mdc"
            write_file(
                os.path.join(rules_dir, target_name),
                fm + "\n" + body,
            )
            self.log(f"Created: {target_name}")

    def _adapt_constraints(self, rules_dir):
        constraints = self.ws.read_file("project-memory/architectural_constraints.md")
        if not constraints:
            return

        body = strip_frontmatter(constraints)
        fm = build_frontmatter({
            "description": "项目架构约束（BLOCK/WARN 级别），始终生效",
            "alwaysApply": "true",
        })
        write_file(
            os.path.join(rules_dir, "constraints.mdc"),
            fm + "\n" + body,
        )
        self.log("Created: constraints.mdc (alwaysApply: true)")

    def _adapt_skills(self, rules_dir):
        """
        将每个 SKILL.md 转为 .cursor/rules/{skill-name}.mdc
        设为 alwaysApply: false（仅通过 @skill-name 手动触发）。
        同时复制 references/ 和 templates/ 供 AI 执行时按需读取。
        """
        skills = self.ws.list_skills()
        if not skills:
            self.log("No skills found, skipping.")
            return

        # 创建 skills 子目录存放 references
        skills_ref_dir = os.path.join(rules_dir, "skill-refs")
        skill_count = 0

        for skill_name in skills:
            skill_dir = os.path.join(self.ws.skills_dir(), skill_name)
            skill_md_path = os.path.join(skill_dir, "SKILL.md")

            if not os.path.isfile(skill_md_path):
                continue

            with open(skill_md_path, "r", encoding="utf-8") as f:
                content = f.read()

            # 提取原始 SKILL.md 的 description（从 frontmatter）
            original_desc = self._extract_skill_description(content)
            body = strip_frontmatter(content)

            # 构建 Cursor .mdc frontmatter
            # alwaysApply: false + 无 globs = 仅手动 @触发
            description = original_desc or f"harness-framework Skill: {skill_name}"
            fm = build_frontmatter({
                "description": description,
                "alwaysApply": "false",
            })

            # 如果该 Skill 有 references/，在 body 前面加引用说明
            ref_dir = os.path.join(skill_dir, "references")
            has_refs = os.path.isdir(ref_dir) and os.listdir(ref_dir)

            # 如果有 templates/（dev-self-test）
            tpl_dir = os.path.join(skill_dir, "templates")
            has_tpls = os.path.isdir(tpl_dir) and os.listdir(tpl_dir)

            ref_note = ""
            if has_refs or has_tpls:
                ref_parts = []
                if has_refs:
                    ref_parts.append(f"references/（本 Workbench 的技术绑定知识）")
                    # 复制 references 到 .cursor/rules/skill-refs/{skill-name}/
                    ref_target = os.path.join(skills_ref_dir, skill_name, "references")
                    copy_tree(ref_dir, ref_target)
                if has_tpls:
                    ref_parts.append(f"templates/（测试模板）")
                    tpl_target = os.path.join(skills_ref_dir, skill_name, "templates")
                    copy_tree(tpl_dir, tpl_target)

                ref_note = (
                    f"\n> **关联资源**：执行本 Skill 时，按需读取 "
                    f"`rules/skill-refs/{skill_name}/` 下的 {' 和 '.join(ref_parts)}。\n"
                )

            # 写入 .mdc 文件
            mdc_content = fm + "\n" + ref_note + body
            write_file(
                os.path.join(rules_dir, f"{skill_name}.mdc"),
                mdc_content,
            )
            skill_count += 1

        self.log(f"Converted {skill_count} SKILL.md -> .mdc (manual trigger via @{skill_name})")
        if os.path.isdir(skills_ref_dir):
            self.log(f"Copied skill references -> {skills_ref_dir}")

    def _extract_skill_description(self, content):
        """从 SKILL.md 的 YAML frontmatter 中提取 description 字段。"""
        match = re.match(r'^---\s*\n(.*?)\n---\s*\n', content, re.DOTALL)
        if not match:
            return None
        fm_text = match.group(1)
        # 匹配 description: 后的内容（支持多行 > 格式和单行）
        desc_match = re.search(r'description:\s*[>)]?\s*\n?(.*?)(?:\n[a-z]|\n---|\Z)', fm_text, re.DOTALL)
        if desc_match:
            desc = desc_match.group(1).strip()
            # 清理 YAML 多行格式
            desc = re.sub(r'^\s+', '', desc, flags=re.MULTILINE)
            return desc if desc else None
        return None

    def _adapt_workflow(self, rules_dir):
        """
        从 skills/ 中提取核心开发纪律，生成一个 alwaysApply 的 .mdc 文件。
        Cursor 没有 Skill 触发机制，需要把关键工作流纪律固化为 rule。
        """
        skills = self.ws.list_skills()
        if not skills:
            return

        lines = [
            "# 开发工作流纪律\n",
            "> 本文件由 harness-framework agent-adapter 从 skills/ 提取生成。\n",
            "## 核心原则\n",
            "- 确认即落盘：所有已确认的产出物立即写入文件，不囤在对话上下文\n",
            "- 文件权威 > 对话记忆：约束、配置以文件为准，执行前读文件\n",
            "- 人类门控：关键决策点 BLOCK/QUESTION，由人确认后才推进\n",
            "- 可重入：长流程能读文件判断进度，可随时中断续跑\n",
            "## 执行前检查\n",
            "- 检查 local_profile.yaml 是否存在（不存在则 BLOCK）\n",
            "- 读取 project-config.yaml 获取项目配置\n",
            "- 读取 project-memory/architectural_constraints.md 获取约束\n",
            "- 读取 rules/{workbench}/ 获取编码规范\n",
            "## 开发流水线参考\n",
            "| 阶段 | 对应 Skill | 说明 |\n",
            "|------|-----------|------|\n",
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
                lines.append(f"| {stage} | {skill} | {desc} |\n")

        lines.append('\n> 各 Skill 已转为 .mdc 文件，在 Cursor 中通过 `@{skill-name}` 手动触发。\n')

        fm = build_frontmatter({
            "description": "harness-framework 开发工作流纪律与流水线映射",
            "alwaysApply": "true",
        })
        write_file(
            os.path.join(rules_dir, "development-workflow.mdc"),
            fm + "\n" + "".join(lines),
        )
        self.log("Created: development-workflow.mdc (alwaysApply: true)")
