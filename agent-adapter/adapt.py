#!/usr/bin/env python3
"""
adapt.py — AI Agent 多目标适配脚本

将 harness-framework doc-workspace 的生成物适配并分发到
Claude Code / Qoder / Kiro / Cursor / Codex 各自的约定目录下。

使用方式：
  # 自动检测用户机器上安装的 AI Agent（推荐）
  # 在 workspace 目录本身下生成 .kiro / .claude 等配置
  python agent-adapter/adapt.py --workspace ./my-doc-workspace --auto

  # 适配所有 Agent
  python agent-adapter/adapt.py --workspace ./my-doc-workspace --target all

  # 仅适配 Claude Code
  python agent-adapter/adapt.py --workspace ./my-doc-workspace --target claude-code

  # 适配 Kiro 并分发到代码仓库（额外目标）
  python agent-adapter/adapt.py --workspace ./my-doc-workspace --target kiro --dest /path/to/repo
"""

import argparse
import os
import sys

# 将当前目录加入 path，使 adapters 包可被导入
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from adapters.base import Workspace
from adapters.claude_code import ClaudeCodeAdapter
from adapters.qoder import QoderAdapter
from adapters.kiro import KiroAdapter
from adapters.cursor import CursorAdapter
from adapters.codex import CodexAdapter


ADAPTERS = {
    "claude-code": ClaudeCodeAdapter,
    "qoder": QoderAdapter,
    "kiro": KiroAdapter,
    "cursor": CursorAdapter,
    "codex": CodexAdapter,
}

# Agent 特征目录 -> 适配器名称
AGENT_SIGNATURES = {
    ".claude": "claude-code",
    ".qoder": "qoder",
    ".kiro": "kiro",
    ".cursor": "cursor",
    ".codex": "codex",
}

# 反向映射：适配器名称 -> 特征目录（去掉前导点）
AGENT_SIGNATURES_REV = {v: k.lstrip(".") for k, v in AGENT_SIGNATURES.items()}


def run_adapter(target_name, workspace, dest_targets):
    """
    执行单个适配器，向一个或多个目标路径分发。
    dest_targets: 路径列表（至少包含 workspace 本身）
    """
    adapter_class = ADAPTERS[target_name]
    all_ok = True
    for dest in dest_targets:
        adapter = adapter_class(workspace, dest)
        print(f"\n{'='*60}")
        print(f"  Adapting for: {adapter.name}")
        print(f"  Target dir:   {adapter.target_root()}")
        print(f"{'='*60}")
        try:
            adapter.adapt()
        except Exception as e:
            print(f"  [ERROR] {adapter.name} adaptation failed: {e}")
            import traceback
            traceback.print_exc()
            all_ok = False
    return all_ok


def detect_agents(scan_path):
    """
    扫描目标路径下已存在的 AI Agent 特征目录。
    返回检测到的适配器名称列表（按特征目录名排序）。
    """
    detected = []
    for dirname, agent_name in AGENT_SIGNATURES.items():
        full_path = os.path.join(scan_path, dirname)
        if os.path.isdir(full_path):
            detected.append(agent_name)
    return detected


def _get_agent_install_paths():
    """
    返回每个 Agent 在用户机器上可能存在的安装/配置路径。
    区分 Windows / macOS / Linux。
    """
    home = os.path.expanduser("~")

    if sys.platform == "win32":
        local_appdata = os.environ.get(
            "LOCALAPPDATA", os.path.join(home, "AppData", "Local")
        )
        roaming_appdata = os.environ.get(
            "APPDATA", os.path.join(home, "AppData", "Roaming")
        )
        program_files = os.environ.get("ProgramFiles", r"C:\Program Files")
        return {
            "claude-code": [
                os.path.join(home, ".claude"),
                os.path.join(local_appdata, "claude"),
                os.path.join(roaming_appdata, "claude"),
            ],
            "qoder": [
                os.path.join(local_appdata, "Qoder"),
                os.path.join(roaming_appdata, "Qoder"),
            ],
            "kiro": [
                os.path.join(home, ".kiro"),
                os.path.join(local_appdata, "kiro"),
                os.path.join(local_appdata, "Programs", "kiro"),
                os.path.join(roaming_appdata, "kiro"),
            ],
            "cursor": [
                os.path.join(local_appdata, "cursor"),
                os.path.join(local_appdata, "Programs", "cursor"),
                os.path.join(roaming_appdata, "Cursor"),
                os.path.join(home, ".cursor"),
            ],
            "codex": [
                os.path.join(home, ".codex"),
                os.path.join(local_appdata, "codex"),
            ],
        }

    elif sys.platform == "darwin":
        return {
            "claude-code": [
                os.path.join(home, ".claude"),
                "/Applications/Claude Code.app",
                "/Applications/Claude.app",
            ],
            "qoder": [
                os.path.join(home, ".qoder"),
                "/Applications/Qoder.app",
                os.path.join(home, "Library", "Application Support", "Qoder"),
            ],
            "kiro": [
                os.path.join(home, ".kiro"),
                "/Applications/Kiro.app",
                os.path.join(home, "Library", "Application Support", "kiro"),
            ],
            "cursor": [
                os.path.join(home, ".cursor"),
                "/Applications/Cursor.app",
                os.path.join(home, "Library", "Application Support", "Cursor"),
            ],
            "codex": [
                os.path.join(home, ".codex"),
            ],
        }

    else:
        # Linux / 其他 Unix-like
        return {
            "claude-code": [
                os.path.join(home, ".claude"),
                os.path.join(home, ".local", "share", "claude"),
            ],
            "qoder": [
                os.path.join(home, ".qoder"),
                os.path.join(home, ".local", "share", "Qoder"),
            ],
            "kiro": [
                os.path.join(home, ".kiro"),
            ],
            "cursor": [
                os.path.join(home, ".cursor"),
                os.path.join(home, ".config", "Cursor"),
            ],
            "codex": [
                os.path.join(home, ".codex"),
            ],
        }


def detect_installed_agents():
    """
    检测用户机器上已安装的 AI Agent。
    扫描用户主目录及常见安装路径（Windows / macOS / Linux 兼容）。
    返回检测到的适配器名称列表（按 ADAPTERS 字典顺序）。
    """
    install_paths = _get_agent_install_paths()
    detected = []
    for agent_name in ADAPTERS:
        paths = install_paths.get(agent_name, [])
        for path in paths:
            if os.path.exists(path):
                detected.append(agent_name)
                break
    return detected


# 用于识别代码项目的特征文件
PROJECT_MARKERS = {
    # Node.js / 前端
    "package.json", "package-lock.json", "yarn.lock", "pnpm-lock.yaml",
    ".npmrc", "tsconfig.json", "jsconfig.json",
    "vite.config.js", "vite.config.ts", "webpack.config.js", "next.config.js",
    "nuxt.config.js", "angular.json", "vue.config.js",
    # Java / JVM
    "pom.xml", "build.gradle", "build.gradle.kts", "settings.gradle",
    "settings.gradle.kts", "gradle.properties", ".mvn", "build.xml",
    # Python
    "requirements.txt", "pyproject.toml", "setup.py", "setup.cfg",
    "Pipfile", "Pipfile.lock", "poetry.lock", "tox.ini", "manage.py",
    # Rust
    "Cargo.toml",
    # Go
    "go.mod", "go.sum", "go.work",
    # C / C++
    "Makefile", "CMakeLists.txt", "configure", "configure.ac",
    "meson.build", "BMakefile",
    # .NET / C#
    "*.csproj", "*.sln", "*.fsproj",
    # Ruby
    "Gemfile", "Gemfile.lock", "Rakefile", ".ruby-version",
    # PHP
    "composer.json", "composer.lock", "artisan",
    # Flutter / Dart
    "pubspec.yaml", "pubspec.lock",
    # Swift / iOS
    "Package.swift", "Podfile", "*.xcodeproj", "*.xcworkspace",
    # Kotlin (非 Gradle)
    "*.kt",
    # 通用
    ".gitignore", ".git", ".gitattributes",
    "Dockerfile", "docker-compose.yml", "docker-compose.yaml",
    ".editorconfig", "LICENSE", "README.md",
}


# 框架/工具目录黑名单（这些目录看起来像项目但不是代码项目）
FRAMEWORK_DIR_BLOCKLIST = {
    "agent-adapter", "harness-framework", "doc-workspace",
    "node_modules", ".vscode", ".idea",
}


def detect_dest_project(workspace_path):
    """
    当未指定 --dest 时，自动扫描 workspace 的同级目录，
    寻找可能是代码项目的目录。
    排除 workspace 自身、脚本自身目录、以及已知的框架/工具目录。
    返回候选项目路径列表。
    """
    parent = os.path.dirname(workspace_path)
    ws_name = os.path.basename(workspace_path)
    script_dir = os.path.dirname(os.path.abspath(__file__))
    candidates = []
    for name in os.listdir(parent):
        if name.startswith("."):
            continue
        if name == ws_name:
            continue
        full = os.path.join(parent, name)
        if not os.path.isdir(full):
            continue
        # 排除脚本自身所在目录
        if os.path.abspath(full) == script_dir:
            continue
        # 排除已知的框架/工具目录
        if name in FRAMEWORK_DIR_BLOCKLIST:
            continue
        # 检查是否包含项目特征文件或 agent 特征目录
        is_project = _match_project_markers(full)
        has_agent = any(
            os.path.isdir(os.path.join(full, d)) for d in AGENT_SIGNATURES
        )
        if is_project or has_agent:
            candidates.append(full)
    return candidates


def _match_project_markers(dir_path):
    """
    检查目录是否包含 PROJECT_MARKERS 中的特征文件。
    支持精确文件名（如 'package.json'）、精确目录名（如 '.git'）、
    以及通配符模式（如 '*.csproj'、'*.xcodeproj'）。
    """
    import fnmatch
    try:
        entries = os.listdir(dir_path)
    except OSError:
        return False
    for marker in PROJECT_MARKERS:
        if "*" in marker:
            # 通配符模式：匹配文件和目录
            if any(fnmatch.fnmatch(e, marker) for e in entries):
                return True
        else:
            # 精确匹配：文件或目录均可
            if marker in entries:
                return True
    return False


def interactive_select(detected, all_agents):
    """
    检测到多个或未检测到时，让用户交互选择。
    """
    if not detected:
        print("\n  未检测到任何已安装的 AI Agent。")
        print("  可能原因：")
        print("    1. 当前机器未安装支持的 AI Agent")
        print("    2. Agent 安装在非标准路径")
        print()
    else:
        print(f"\n  检测到 {len(detected)} 个已安装的 AI Agent：")
        for i, name in enumerate(detected, 1):
            print(f"    {i}. {name}")
        print()

    print("  请选择要适配的目标：")
    options = detected if detected else list(all_agents.keys())
    for i, name in enumerate(options, 1):
        marker = " (detected)" if name in detected else ""
        print(f"    {i}. {name}{marker}")
    print(f"    a. all（全部适配）")
    print(f"    q. 退出")
    print()

    while True:
        try:
            choice = input("  请输入选项编号 [1-{} / a / q]: ".format(len(options))).strip().lower()
        except (EOFError, KeyboardInterrupt):
            print("\n  已取消。")
            sys.exit(0)

        if choice == "q":
            print("  已退出。")
            sys.exit(0)
        if choice == "a":
            return list(all_agents.keys())
        try:
            idx = int(choice) - 1
            if 0 <= idx < len(options):
                return [options[idx]]
        except ValueError:
            pass
        print("  无效输入，请重试。")


def main():
    parser = argparse.ArgumentParser(
        description="AI Agent Adapter — 将 doc-workspace 生成物适配到目标 Agent",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  # 自动检测用户机器上安装的 AI Agent（推荐）
  # 在 workspace 目录本身下生成 .kiro / .claude 等配置
  python agent-adapter/adapt.py -w ./my-doc-workspace --auto

  # 适配所有 Agent
  python agent-adapter/adapt.py -w ./my-doc-workspace -t all

  # 仅适配 Claude Code
  python agent-adapter/adapt.py -w ./my-doc-workspace -t claude-code

  # 适配 Kiro 并分发到代码仓库（额外目标）
  python agent-adapter/adapt.py -w ./my-doc-workspace -t kiro -d /path/to/repo
        """,
    )
    parser.add_argument(
        "--workspace", "-w",
        required=True,
        help="doc-workspace 路径（必填），适配产物默认生成在此目录下",
    )

    # --target 和 --auto 互斥
    target_group = parser.add_mutually_exclusive_group(required=True)
    target_group.add_argument(
        "--target", "-t",
        choices=list(ADAPTERS.keys()) + ["all"],
        help="目标 AI Agent（claude-code / qoder / kiro / cursor / codex / all）",
    )
    target_group.add_argument(
        "--auto", "-a",
        action="store_true",
        help="自动检测用户机器上已安装的 AI Agent（扫描用户主目录）",
    )

    parser.add_argument(
        "--dest", "-d",
        default=None,
        help="额外分发目标路径（不指定则只在 workspace 目录下生成）",
    )

    args = parser.parse_args()

    # 验证 workspace
    workspace_path = os.path.abspath(args.workspace)
    if not os.path.isdir(workspace_path):
        print(f"ERROR: Workspace not found: {workspace_path}")
        sys.exit(1)

    workspace = Workspace(workspace_path)

    # 验证关键文件
    if not os.path.isfile(workspace.config_path()):
        print(f"ERROR: project-config.yaml not found in workspace")
        sys.exit(1)

    # 确定适配目标列表：默认是 workspace 本身
    # 如果指定了 --dest，则额外向 dest 分发
    dest_targets = [workspace_path]
    if args.dest:
        dest_path = os.path.abspath(args.dest)
        if not os.path.isdir(dest_path):
            print(f"ERROR: Dest path not found: {dest_path}")
            sys.exit(1)
        dest_targets.append(dest_path)

    print(f"Workspace: {workspace_path}")
    print(f"Skills:    {', '.join(workspace.list_skills()) or '(none)'}")
    print(f"Rules:     {len(workspace.list_rules())} files")
    print(f"Targets:   {', '.join(dest_targets)}")

    # 确定适配的 Agent
    if args.auto:
        # --auto 模式：检测用户机器上安装的 AI Agent
        print(f"\n{'='*60}")
        print("  Detecting installed AI Agents on this machine...")
        print(f"{'='*60}")

        detected = detect_installed_agents()
        if detected:
            targets = detected
            print(f"\n  Detected {len(detected)} installed Agent(s):")
            for name in detected:
                print(f"    - {name}")
            print(f"\n  Auto-selecting: {', '.join(detected)}")
        else:
            # 未检测到任何已安装 Agent，进入交互选择
            print(f"\n  No AI Agent detected on this machine.")
            print(f"  Platform: {sys.platform}")
            print(f"  Home: {os.path.expanduser('~')}")
            selected = interactive_select([], ADAPTERS)
            targets = selected
    else:
        # --target 模式：手动指定
        targets = list(ADAPTERS.keys()) if args.target == "all" else [args.target]

    # 执行适配
    results = {}
    for target in targets:
        results[target] = run_adapter(target, workspace, dest_targets)

    # 汇总
    print(f"\n{'='*60}")
    print("  Summary")
    print(f"{'='*60}")
    for target, success in results.items():
        status = "OK" if success else "FAILED"
        print(f"  {target:20s}  {status}")

    if all(results.values()):
        print("\nAll adaptations completed successfully.")
    else:
        print("\nSome adaptations failed. Check output above.")
        sys.exit(1)


if __name__ == "__main__":
    main()
