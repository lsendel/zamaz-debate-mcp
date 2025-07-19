#!/usr/bin/env python3
"""
Generate a detailed linting report with class names, method names, and line numbers.
"""

import ast
import json
import re
import subprocess
from pathlib import Path
from typing import Any


class DetailedLintingReporter:
    def __init__(self):
        self.issues_by_file = {}
        self.python_issues = []
        self.shell_issues = []
        self.typescript_issues = []

    def extract_python_context(self, file_path: str, line_number: int) -> dict[str, Any]:
        """Extract class and method context for a Python file at a specific line."""
        try:
            with Path(file_path).open() as f:
                content = f.read()

            tree = ast.parse(content)

            # Find the context for the given line
            class_name = None
            method_name = None

            for node in ast.walk(tree):
                if hasattr(node, "lineno"):
                    if isinstance(node, ast.ClassDef) and node.lineno <= line_number:
                        # Check if line is within this class
                        end_line = node.end_lineno if hasattr(node, "end_lineno") else line_number + 1
                        if line_number <= end_line:
                            class_name = node.name

                    if isinstance(node, ast.FunctionDef) and node.lineno <= line_number:
                        # Check if line is within this function
                        end_line = node.end_lineno if hasattr(node, "end_lineno") else line_number + 1
                        if line_number <= end_line:
                            method_name = node.name

            # Get the actual line content
            lines = content.split("\n")
            line_content = lines[line_number - 1] if line_number <= len(lines) else ""

            return {"class_name": class_name, "method_name": method_name, "line_content": line_content.strip()}
        except Exception as e:
            return {"class_name": None, "method_name": None, "line_content": f"Error reading file: {e}"}

    def get_python_issues(self):
        """Get all Python linting issues with detailed context."""

        # Run ruff and get JSON output
        result = subprocess.run(  # noqa: S603 (calling known development tool)
            ["ruff", "check", ".", "--output-format=json"], capture_output=True, text=True, check=False  # noqa: S607 (development tool path)
        )

        if result.stdout:
            issues = json.loads(result.stdout)

            for issue in issues:
                # Extract context for each issue
                context = self.extract_python_context(issue["filename"], issue["location"]["row"])

                detailed_issue = {
                    "file": issue["filename"],
                    "line": issue["location"]["row"],
                    "column": issue["location"]["column"],
                    "code": issue["code"],
                    "message": issue["message"],
                    "class_name": context["class_name"],
                    "method_name": context["method_name"],
                    "line_content": context["line_content"],
                    "fix_available": issue.get("fix") is not None,
                }

                self.python_issues.append(detailed_issue)

                # Group by file
                if issue["filename"] not in self.issues_by_file:
                    self.issues_by_file[issue["filename"]] = []
                self.issues_by_file[issue["filename"]].append(detailed_issue)

        return self.python_issues

    def get_shell_issues(self):
        """Get all Shell script issues with detailed context."""

        shell_files = list(Path().rglob("*.sh"))

        for file_path in shell_files:
            if ".git" in str(file_path) or "node_modules" in str(file_path):
                continue

            result = subprocess.run(  # noqa: S603 (calling known development tool)
                ["shellcheck", "--format=json", str(file_path)], capture_output=True, text=True, check=False  # noqa: S607 (development tool path)
            )

            if result.stdout:
                try:
                    issues = json.loads(result.stdout)

                    for issue in issues:
                        with file_path.open() as f:
                            lines = f.readlines()
                            line_content = lines[issue["line"] - 1].strip() if issue["line"] <= len(lines) else ""

                        detailed_issue = {
                            "file": str(file_path),
                            "line": issue["line"],
                            "column": issue["column"],
                            "code": f"SC{issue['code']}",
                            "level": issue["level"],
                            "message": issue["message"],
                            "line_content": line_content,
                            "fix": issue.get("fix", {}),
                        }

                        self.shell_issues.append(detailed_issue)

                        if str(file_path) not in self.issues_by_file:
                            self.issues_by_file[str(file_path)] = []
                        self.issues_by_file[str(file_path)].append(detailed_issue)
                except json.JSONDecodeError as e:
                    print(f"Warning: Failed to parse shellcheck JSON output for {file_path}: {e}")

        return self.shell_issues

    def get_typescript_issues(self):
        """Get all TypeScript/JavaScript issues with detailed context."""

        # Change to debate-ui directory
        debate_ui_path = Path("debate-ui")
        if debate_ui_path.exists():
            result = subprocess.run(  # noqa: S603 (calling known development tool)
                ["npx", "eslint", "src", "--ext", ".ts,.tsx,.js,.jsx", "--format=json"],  # noqa: S607 (development tool path)
                cwd=debate_ui_path,
                capture_output=True,
                text=True,
                check=False,
            )

            if result.stdout:
                try:
                    results = json.loads(result.stdout)

                    for file_result in results:
                        file_path = file_result["filePath"]

                        for message in file_result["messages"]:
                            with Path(file_path).open() as f:
                                lines = f.readlines()
                                line_content = (
                                    lines[message["line"] - 1].strip() if message["line"] <= len(lines) else ""
                                )

                            # Extract function/class context from line
                            class_match = re.search(r"class\s+(\w+)", line_content)
                            function_match = re.search(
                                r"(?:function\s+(\w+)|const\s+(\w+)\s*=|(\w+)\s*\()", line_content
                            )

                            detailed_issue = {
                                "file": file_path.replace(str(debate_ui_path.absolute()) + "/", "debate-ui/"),
                                "line": message["line"],
                                "column": message["column"],
                                "severity": message["severity"],
                                "message": message["message"],
                                "ruleId": message["ruleId"],
                                "line_content": line_content,
                                "class_name": class_match.group(1) if class_match else None,
                                "function_name": next(
                                    (
                                        m
                                        for m in [
                                            function_match.group(1),
                                            function_match.group(2),
                                            function_match.group(3),
                                        ]
                                        if m
                                    ),
                                    None,
                                )
                                if function_match
                                else None,
                                "fix": message.get("fix"),
                            }

                            self.typescript_issues.append(detailed_issue)

                            if file_path not in self.issues_by_file:
                                self.issues_by_file[file_path] = []
                            self.issues_by_file[file_path].append(detailed_issue)
                except json.JSONDecodeError as e:
                    print(f"Warning: Failed to parse eslint JSON output: {e}")

        return self.typescript_issues

    def generate_markdown_report(self, output_file: str = "DETAILED_LINTING_REPORT.md"):
        """Generate a detailed markdown report."""
        with Path(output_file).open("w") as f:
            f.write("# Detailed Linting Report\n\n")
            f.write(f"**Generated**: {Path.cwd()}\n")
            f.write(f"**Total Files with Issues**: {len(self.issues_by_file)}\n")
            f.write(
                f"**Total Issues**: {len(self.python_issues) + len(self.shell_issues) + len(self.typescript_issues)}\n\n"
            )

            # Python Issues
            f.write("## 🐍 Python Issues (Ruff)\n\n")
            f.write(f"**Total Python Issues**: {len(self.python_issues)}\n\n")

            # Group by issue type
            issue_types = {}
            for issue in self.python_issues:
                code = issue["code"]
                if code not in issue_types:
                    issue_types[code] = []
                issue_types[code].append(issue)

            for code, issues in sorted(issue_types.items()):
                f.write(f"### {code} ({len(issues)} issues)\n\n")

                for issue in issues[:5]:  # Show first 5 of each type
                    f.write(f"#### File: `{issue['file']}`\n")
                    f.write(f"- **Line**: {issue['line']}, Column: {issue['column']}\n")
                    if issue["class_name"]:
                        f.write(f"- **Class**: `{issue['class_name']}`\n")
                    if issue["method_name"]:
                        f.write(f"- **Method**: `{issue['method_name']}`\n")
                    f.write(f"- **Message**: {issue['message']}\n")
                    f.write(f"- **Code**: `{issue['line_content']}`\n")
                    f.write(f"- **Auto-fixable**: {'Yes' if issue['fix_available'] else 'No'}\n")
                    f.write("\n")

                if len(issues) > 5:
                    f.write(f"*... and {len(issues) - 5} more {code} issues*\n\n")

            # Shell Issues
            f.write("\n## 🐚 Shell Script Issues (ShellCheck)\n\n")
            f.write(f"**Total Shell Issues**: {len(self.shell_issues)}\n\n")

            # Group by severity
            severities = {}
            for issue in self.shell_issues:
                level = issue["level"]
                if level not in severities:
                    severities[level] = []
                severities[level].append(issue)

            for level, issues in sorted(severities.items()):
                f.write(f"### {level.upper()} ({len(issues)} issues)\n\n")

                for issue in issues[:5]:
                    f.write(f"#### File: `{issue['file']}`\n")
                    f.write(f"- **Line**: {issue['line']}, Column: {issue['column']}\n")
                    f.write(f"- **Code**: {issue['code']}\n")
                    f.write(f"- **Message**: {issue['message']}\n")
                    f.write(f"- **Code**: `{issue['line_content']}`\n")
                    if issue.get("fix"):
                        f.write("- **Suggested Fix**: Available\n")
                    f.write("\n")

                if len(issues) > 5:
                    f.write(f"*... and {len(issues) - 5} more {level} issues*\n\n")

            # TypeScript Issues
            f.write("\n## 📦 TypeScript/JavaScript Issues (ESLint)\n\n")
            f.write(f"**Total TypeScript Issues**: {len(self.typescript_issues)}\n\n")

            for issue in self.typescript_issues:
                f.write(f"#### File: `{issue['file']}`\n")
                f.write(f"- **Line**: {issue['line']}, Column: {issue['column']}\n")
                f.write(f"- **Rule**: {issue['ruleId']}\n")
                f.write(f"- **Severity**: {'Error' if issue['severity'] == 2 else 'Warning'}\n")
                f.write(f"- **Message**: {issue['message']}\n")
                f.write(f"- **Code**: `{issue['line_content']}`\n")
                if issue["class_name"]:
                    f.write(f"- **Class**: `{issue['class_name']}`\n")
                if issue["function_name"]:
                    f.write(f"- **Function**: `{issue['function_name']}`\n")
                f.write("\n")

            # Summary by file
            f.write("\n## 📁 Issues by File\n\n")

            for file_path, issues in sorted(self.issues_by_file.items()):
                f.write(f"### `{file_path}` ({len(issues)} issues)\n")

                # Group by line number
                issues_by_line = {}
                for issue in issues:
                    line = issue["line"]
                    if line not in issues_by_line:
                        issues_by_line[line] = []
                    issues_by_line[line].append(issue)

                for line, line_issues in sorted(issues_by_line.items()):
                    f.write(f"- **Line {line}**: ")
                    codes = [issue.get("code", issue.get("ruleId", "Unknown")) for issue in line_issues]
                    f.write(f"{', '.join(codes)}\n")

                f.write("\n")

    def generate_json_report(self, output_file: str = "linting_issues.json"):
        """Generate a JSON report for programmatic fixing."""
        report = {
            "summary": {
                "total_files": len(self.issues_by_file),
                "python_issues": len(self.python_issues),
                "shell_issues": len(self.shell_issues),
                "typescript_issues": len(self.typescript_issues),
            },
            "python": self.python_issues,
            "shell": self.shell_issues,
            "typescript": self.typescript_issues,
            "by_file": self.issues_by_file,
        }

        with Path(output_file).open("w") as f:
            json.dump(report, f, indent=2)

        return report


def main():
    reporter = DetailedLintingReporter()

    # Collect all issues
    reporter.get_python_issues()
    reporter.get_shell_issues()
    reporter.get_typescript_issues()

    # Generate reports
    reporter.generate_markdown_report()
    reporter.generate_json_report()

    # Print summary


if __name__ == "__main__":
    main()
