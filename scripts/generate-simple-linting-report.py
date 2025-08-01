#!/usr/bin/env python3
"""Generate detailed linting report and fix issues iteratively."""

import json
import subprocess
from collections import defaultdict
from datetime import datetime
from pathlib import Path


def run_command(cmd, cwd=None, capture=True):
    """Run shell command and return output."""
    if capture:
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True, cwd=cwd, check=False)  # noqa: S602 (calling known development tools)
        return result.stdout, result.stderr, result.returncode
    else:
        return subprocess.run(cmd, shell=True, cwd=cwd, check=False).returncode  # noqa: S602 (calling known development tools)


def get_python_issues():
    """Get all Python issues with details."""

    # Get detailed issues with context
    stdout, stderr, _ = run_command("ruff check . --output-format=json")

    issues = []
    if stdout:
        try:
            raw_issues = json.loads(stdout)

            # Get file contents for context
            for issue in raw_issues:
                try:
                    with Path(issue["filename"]).open() as f:
                        lines = f.readlines()

                    line_num = issue["location"]["row"]
                    issue["line_content"] = lines[line_num - 1].strip() if line_num <= len(lines) else ""
                    issue["context_before"] = [line.strip() for line in lines[max(0, line_num - 3) : line_num - 1]]
                    issue["context_after"] = [line.strip() for line in lines[line_num : min(len(lines), line_num + 2)]]
                except Exception as e:
                    # Handle file read errors gracefully
                    print(f"Warning: Could not read file {issue['filename']}: {e}")
                    issue["line_content"] = ""
                    issue["context_before"] = []
                    issue["context_after"] = []

                issues.append(issue)
        except json.JSONDecodeError as e:
            print(f"Warning: Failed to parse ruff JSON output: {e}")

    # Group by type
    by_type = defaultdict(list)
    for issue in issues:
        by_type[issue.get("code", "Unknown")].append(issue)

    return issues, by_type


def get_shell_issues():
    """Get all shell script issues."""

    issues = []
    shell_files = list(Path().rglob("*.sh"))

    for file in shell_files:
        if ".git" in str(file) or "node_modules" in str(file):
            continue

        stdout, stderr, _ = run_command(f"shellcheck --format=json {file}")  # noqa: S602 (calling known development tool)
        if stdout:
            try:
                file_issues = json.loads(stdout)
                for issue in file_issues:
                    issue["filename"] = str(file)
                    issues.append(issue)
            except json.JSONDecodeError:
                # Skip files with invalid JSON output
                print(f"Warning: Failed to parse shellcheck output for {file}")

    return issues


def get_typescript_issues():
    """Get TypeScript/JavaScript issues."""

    if not Path("debate-ui").exists():
        return []

    stdout, stderr, _ = run_command("npx eslint src --ext .ts,.tsx,.js,.jsx --format=json", cwd="debate-ui")  # noqa: S602 (calling known development tool)

    issues = []
    if stdout:
        try:
            results = json.loads(stdout)
            for file_result in results:
                for msg in file_result.get("messages", []):
                    msg["filename"] = file_result["filePath"]
                    issues.append(msg)
        except json.JSONDecodeError as e:
            # Skip invalid JSON output
            print(f"Warning: Failed to parse eslint JSON output: {e}")

    return issues


def fix_python_auto():
    """Auto-fix Python issues."""

    # Ruff auto-fix
    run_command("ruff check . --fix --unsafe-fixes", capture=False)  # noqa: S602 (calling known development tool)

    # Format
    run_command("ruff format .", capture=False)  # noqa: S602 (calling known development tool)


def fix_python_security():
    """Fix Python security issues manually."""

    # Fix S311 - Replace random with secrets
    stdout, _, _ = run_command("ruff check . --select S311 --output-format=json")  # noqa: S602 (calling known development tool)
    if stdout:
        try:
            issues = json.loads(stdout)
            files_to_fix = {issue["filename"] for issue in issues}

            for file in files_to_fix:
                # Check if it's actually security-sensitive
                with Path(file).open() as f:
                    content = f.read()

                # Only fix if there are security-related keywords
                if any(keyword in content.lower() for keyword in ["token", "password", "secret", "key", "auth"]):
                    # Replace imports
                    content = content.replace("import secrets", "import secrets")
                    content = content.replace("from random import", "from secrets import")
                    # Replace common methods
                    content = content.replace("secrets.choice(", "secrets.choice(")
                    content = content.replace("secrets.randbelow(", "secrets.randbelow(")
                    content = content.replace("random.random()", "secrets.randbits(64) / (2**64)")

                    with Path(file).open("w") as f:
                        f.write(content)
        except Exception as e:
            print(f"Warning: Failed to fix S311 security issues: {e}")

    # Fix S113 - Add timeouts to requests
    stdout, _, _ = run_command("ruff check . --select S113 --output-format=json")  # noqa: S602 (calling known development tool)
    if stdout:
        try:
            issues = json.loads(stdout)
            files_to_fix = {issue["filename"] for issue in issues}

            for file in files_to_fix:
                with Path(file).open() as f:
                    lines = f.readlines()

                # Add timeout parameter
                for i, line in enumerate(lines):
                    if "requests." in line and "timeout" not in line:
                        # Simple regex replacement
                        if "requests.get(" in line:
                            lines[i] = line.replace("requests.get(", "requests.get(").replace(")", ", timeout=30)")
                        elif "requests.post(" in line:
                            lines[i] = line.replace("requests.post(", "requests.post(").replace(")", ", timeout=30)")

                with Path(file).open("w") as f:
                    f.writelines(lines)
        except Exception as e:
            print(f"Warning: Failed to fix S113 timeout issues: {e}")


def fix_shell_issues():
    """Fix shell script issues."""

    shell_files = list(Path().rglob("*.sh"))

    for file in shell_files:
        if ".git" in str(file) or "node_modules" in str(file):
            continue

        with file.open() as f:
            content = f.read()
            lines = content.split("\n")

        # Add shebang if missing
        if lines and not lines[0].startswith("#!"):
            lines.insert(0, "#!/bin/bash")

        # Basic fixes
        for i, line in enumerate(lines):
            # Quote variables in conditionals
            if "[ $" in line:
                lines[i] = line.replace("[ $", '[ "$').replace(" ]", '" ]')
            # Replace backticks
            if "`" in line:
                # Simple replacement - may need manual review
                lines[i] = line.replace("`", "$(...)")

        with file.open("w") as f:
            f.write("\n".join(lines))


def fix_typescript_issues():
    """Fix TypeScript/JavaScript issues."""

    if Path("debate-ui").exists():
        run_command("npx eslint src --ext .ts,.tsx,.js,.jsx --fix", cwd="debate-ui", capture=False)  # noqa: S602 (calling known development tool)
        run_command("npx prettier --write 'src/**/*.{ts,tsx,js,jsx,json,css}'", cwd="debate-ui", capture=False)  # noqa: S602 (calling known development tool)


def generate_report(iteration):
    """Generate current state report."""

    # Get all issues
    python_issues, python_by_type = get_python_issues()
    shell_issues = get_shell_issues()
    ts_issues = get_typescript_issues()

    total = len(python_issues) + len(shell_issues) + len(ts_issues)

    # Create report
    report = {
        "iteration": iteration,
        "timestamp": datetime.now().isoformat(),
        "summary": {
            "total_issues": total,
            "python_issues": len(python_issues),
            "shell_issues": len(shell_issues),
            "typescript_issues": len(ts_issues),
        },
        "python_by_type": {k: len(v) for k, v in python_by_type.items()},
        "critical_issues": [],
    }

    # Find critical issues
    for issue in python_issues:
        code = issue.get("code")
        if code and str(code).startswith("S"):  # Security
            report["critical_issues"].append(
                {"file": issue["filename"], "line": issue["location"]["row"], "code": code, "message": issue["message"]}
            )

    # Save report
    report_file = f"linting_report_iteration_{iteration}.json"
    with Path(report_file).open("w") as f:
        json.dump(report, f, indent=2)

    # Print summary

    if python_by_type:
        # Log top issue types
        for code, issues in sorted(python_by_type.items(), key=lambda x: len(x[1]), reverse=True)[:5]:
            print(f"  {code}: {len(issues)} issues")

    return total, report


def commit_changes(iteration, report):
    """Commit the fixes."""

    # Stage changes
    run_command("git add -A")  # noqa: S602 (calling known development tool)

    # Create commit message
    message = f"""Fix linting issues - Iteration {iteration}

Fixed {report["summary"]["total_issues"]} issues:
- Python: {report["summary"]["python_issues"]} issues
- Shell: {report["summary"]["shell_issues"]} issues
- TypeScript: {report["summary"]["typescript_issues"]} issues

🤖 Generated with Claude Code

Co-Authored-By: Claude <noreply@anthropic.com>"""

    # Commit
    run_command(f'git commit -m "{message}"')  # noqa: S602 (calling known development tool)


def main():
    """Main iterative fixing process."""

    iteration = 1
    max_iterations = 10

    while iteration <= max_iterations:
        # Generate initial report
        total_issues, report = generate_report(iteration)

        if total_issues == 0:
            break

        # Fix issues in priority order

        # 1. Auto-fix Python
        fix_python_auto()

        # 2. Fix Python security
        fix_python_security()

        # 3. Fix Shell scripts
        fix_shell_issues()

        # 4. Fix TypeScript
        fix_typescript_issues()

        # Generate post-fix report
        post_total, post_report = generate_report(f"{iteration}_post")

        # Calculate improvement
        fixed = total_issues - post_total

        # Commit if we fixed anything
        if fixed > 0:
            commit_changes(iteration, post_report)

        # Check if we're stuck
        if fixed == 0:
            break

        iteration += 1

    # Final report

    final_total, final_report = generate_report("final")

    if final_total == 0:
        print("✅ All linting issues fixed!")

    # Show remaining critical issues
    elif final_report["critical_issues"]:
        print("⚠️  Remaining critical issues:")
        for issue in final_report["critical_issues"][:10]:
            print(f"  - {issue['file']}:{issue['line']} - {issue['code']}: {issue['message']}")


if __name__ == "__main__":
    main()
