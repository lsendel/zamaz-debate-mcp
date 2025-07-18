#!/usr/bin/env python3
"""
Generate a comprehensive linting report with maximum detail for fixing issues.
"""

import ast
import json
import os
import re
import subprocess
from collections import defaultdict
from datetime import datetime
from pathlib import Path
from typing import Any


class ComprehensiveLintingReporter:
    def __init__(self):
        self.issues_by_file = defaultdict(list)
        self.python_issues = []
        self.shell_issues = []
        self.typescript_issues = []
        self.java_issues = []
        self.security_issues = []
        self.performance_issues = []
        self.style_issues = []
        self.complexity_issues = []

        # Track fixes
        self.auto_fixable = []
        self.manual_fixes_required = []
        self.fix_commands = []

        # Statistics
        self.stats = {
            "start_time": datetime.now(),
            "total_files_scanned": 0,
            "files_with_issues": 0,
            "auto_fixable_count": 0,
            "security_critical": 0,
            "performance_critical": 0,
        }

    def get_file_metadata(self, file_path: str) -> dict[str, Any]:
        """Get detailed file metadata."""
        try:
            stat = os.stat(file_path)
            with Path(file_path).open(encoding="utf-8", errors="ignore") as f:
                content = f.read()
                lines = content.split("\n")

            return {
                "size_bytes": stat.st_size,
                "size_kb": round(stat.st_size / 1024, 2),
                "lines": len(lines),
                "last_modified": datetime.fromtimestamp(stat.st_mtime).isoformat(),
                "encoding": "utf-8",
                "empty_lines": len([line for line in lines if not line.strip()]),
                "comment_lines": self._count_comment_lines(content, file_path),
            }
        except Exception as e:
            return {"error": str(e)}

    def _count_comment_lines(self, content: str, file_path: str) -> int:
        """Count comment lines based on file type."""
        ext = Path(file_path).suffix
        count = 0

        if ext in [".py"]:
            for line in content.split("\n"):
                stripped = line.strip()
                if stripped.startswith("#") or stripped.startswith('"""') or stripped.startswith("'''"):
                    count += 1
        elif ext in [".js", ".ts", ".tsx", ".jsx", ".java"]:
            for line in content.split("\n"):
                stripped = line.strip()
                if stripped.startswith("//") or stripped.startswith("/*") or stripped.startswith("*"):
                    count += 1
        elif ext in [".sh"]:
            for line in content.split("\n"):
                if line.strip().startswith("#"):
                    count += 1

        return count

    def extract_python_context_detailed(self, file_path: str, line_number: int) -> dict[str, Any]:
        """Extract comprehensive context for Python issues."""
        try:
            with Path(file_path).open(encoding="utf-8") as f:
                content = f.read()

            lines = content.split("\n")
            tree = ast.parse(content)

            # Initialize context
            context = {
                "class_name": None,
                "method_name": None,
                "function_type": None,  # 'method', 'function', 'property', 'staticmethod', 'classmethod'
                "decorators": [],
                "imports": [],
                "docstring": None,
                "complexity": 0,
                "line_content": lines[line_number - 1] if line_number <= len(lines) else "",
                "context_lines": {"before": [], "after": []},
                "indentation_level": 0,
                "in_test_file": "test" in file_path.lower(),
                "module_docstring": None,
            }

            # Get surrounding context (3 lines before and after)
            start_context = max(0, line_number - 4)
            end_context = min(len(lines), line_number + 3)
            context["context_lines"]["before"] = lines[start_context : line_number - 1]
            context["context_lines"]["after"] = lines[line_number:end_context]

            # Calculate indentation
            if context["line_content"]:
                context["indentation_level"] = len(context["line_content"]) - len(context["line_content"].lstrip())

            # Walk the AST to find context
            for node in ast.walk(tree):
                if hasattr(node, "lineno"):
                    # Module docstring
                    if isinstance(node, ast.Module) and ast.get_docstring(node):
                        context["module_docstring"] = ast.get_docstring(node)

                    # Imports
                    if isinstance(node, ast.Import | ast.ImportFrom):
                        if isinstance(node, ast.Import):
                            for alias in node.names:
                                context["imports"].append(alias.name)
                        else:
                            module = node.module or ""
                            for alias in node.names:
                                context["imports"].append(f"{module}.{alias.name}")

                    # Class context
                    if isinstance(node, ast.ClassDef) and node.lineno <= line_number:
                        end_line = getattr(node, "end_lineno", line_number + 1)
                        if line_number <= end_line:
                            context["class_name"] = node.name
                            context["decorators"] = [self._get_decorator_name(d) for d in node.decorator_list]
                            if ast.get_docstring(node):
                                context["docstring"] = ast.get_docstring(node)

                    # Function/Method context
                    if isinstance(node, ast.FunctionDef) and node.lineno <= line_number:
                        end_line = getattr(node, "end_lineno", line_number + 1)
                        if line_number <= end_line:
                            context["method_name"] = node.name

                            # Determine function type
                            decorators = [self._get_decorator_name(d) for d in node.decorator_list]
                            if "@staticmethod" in decorators:
                                context["function_type"] = "staticmethod"
                            elif "@classmethod" in decorators:
                                context["function_type"] = "classmethod"
                            elif "@property" in decorators:
                                context["function_type"] = "property"
                            elif context["class_name"]:
                                context["function_type"] = "method"
                            else:
                                context["function_type"] = "function"

                            context["decorators"] = decorators
                            if ast.get_docstring(node):
                                context["docstring"] = ast.get_docstring(node)

                            # Calculate cyclomatic complexity
                            context["complexity"] = self._calculate_complexity(node)

            return context

        except Exception as e:
            return {"error": str(e), "line_content": f"Error reading file: {e}"}

    def _get_decorator_name(self, decorator) -> str:
        """Extract decorator name from AST node."""
        if isinstance(decorator, ast.Name):
            return f"@{decorator.id}"
        elif isinstance(decorator, ast.Call) and isinstance(decorator.func, ast.Name):
            return f"@{decorator.func.id}"
        elif isinstance(decorator, ast.Attribute):
            return f"@{decorator.attr}"
        return "@unknown"

    def _calculate_complexity(self, node) -> int:
        """Calculate cyclomatic complexity of a function."""
        complexity = 1
        for child in ast.walk(node):
            if isinstance(
                child, ast.If | ast.While | ast.For | ast.ExceptHandler | ast.With | ast.Assert | ast.Raise | ast.Return
            ):
                complexity += 1
        return complexity

    def analyze_python_issue_patterns(self):
        """Analyze patterns in Python issues to suggest project-wide fixes."""
        patterns = defaultdict(lambda: {"count": 0, "files": set(), "examples": []})

        for issue in self.python_issues:
            pattern_key = issue["code"]
            patterns[pattern_key]["count"] += 1
            patterns[pattern_key]["files"].add(issue["file"])
            if len(patterns[pattern_key]["examples"]) < 3:
                patterns[pattern_key]["examples"].append(
                    {"file": issue["file"], "line": issue["line"], "content": issue["line_content"]}
                )

        return patterns

    def get_python_issues_detailed(self):
        """Get all Python issues with maximum detail."""

        # Get all Python files first
        python_files = list(Path().rglob("*.py"))
        self.stats["total_files_scanned"] += len(python_files)

        # Run ruff with JSON output
        result = subprocess.run(  # noqa: S603 (calling known development tool)
            ["ruff", "check", ".", "--output-format=json"], capture_output=True, text=True, check=False  # noqa: S607 (development tool path)
        )

        if result.stdout:
            issues = json.loads(result.stdout)

            # Also get security-specific issues
            security_result = subprocess.run(  # noqa: S603 (calling known development tool)
                ["ruff", "check", ".", "--select=S", "--output-format=json"],  # noqa: S607 (development tool path)
                capture_output=True,
                text=True,
                check=False,
            )

            security_codes = set()
            if security_result.stdout:
                security_issues = json.loads(security_result.stdout)
                security_codes = {issue["code"] for issue in security_issues}

            for issue in issues:
                # Get comprehensive context
                context = self.extract_python_context_detailed(issue["filename"], issue["location"]["row"])
                file_meta = self.get_file_metadata(issue["filename"])

                detailed_issue = {
                    "file": issue["filename"],
                    "line": issue["location"]["row"],
                    "column": issue["location"]["column"],
                    "end_line": issue["end_location"]["row"] if "end_location" in issue else issue["location"]["row"],
                    "end_column": issue["end_location"]["column"]
                    if "end_location" in issue
                    else issue["location"]["column"],
                    "code": issue["code"],
                    "message": issue["message"],
                    "url": issue.get("url", ""),
                    # Context
                    "class_name": context.get("class_name"),
                    "method_name": context.get("method_name"),
                    "function_type": context.get("function_type"),
                    "decorators": context.get("decorators", []),
                    "docstring": context.get("docstring"),
                    "complexity": context.get("complexity", 0),
                    "line_content": context.get("line_content", ""),
                    "context_before": context.get("context_lines", {}).get("before", []),
                    "context_after": context.get("context_lines", {}).get("after", []),
                    "indentation": context.get("indentation_level", 0),
                    "in_test": context.get("in_test_file", False),
                    "imports": context.get("imports", []),
                    # File metadata
                    "file_size_kb": file_meta.get("size_kb", 0),
                    "file_lines": file_meta.get("lines", 0),
                    "last_modified": file_meta.get("last_modified"),
                    # Issue metadata
                    "category": self._categorize_issue(issue["code"]),
                    "severity": self._determine_severity(issue["code"], context),
                    "is_security": issue["code"] in security_codes,
                    "fix_available": issue.get("fix") is not None,
                    "fix": issue.get("fix"),
                    "noqa_comment": f"  # noqa: {issue['code']}",
                }

                # Add suggested fixes after category is set
                detailed_issue.update(
                    {
                        "suggested_fix": self._suggest_fix(issue, context),
                        "fix_complexity": self._estimate_fix_complexity(issue, context),
                        "fix_priority": self._calculate_fix_priority(detailed_issue, context),
                    }
                )

                self.python_issues.append(detailed_issue)
                self.issues_by_file[issue["filename"]].append(detailed_issue)

                # Categorize for tracking
                if detailed_issue["is_security"]:
                    self.security_issues.append(detailed_issue)
                    if detailed_issue["severity"] == "critical":
                        self.stats["security_critical"] += 1

                if detailed_issue["category"] == "performance":
                    self.performance_issues.append(detailed_issue)
                    if detailed_issue["severity"] == "critical":
                        self.stats["performance_critical"] += 1

                if detailed_issue["fix_available"]:
                    self.auto_fixable.append(detailed_issue)
                    self.stats["auto_fixable_count"] += 1
                else:
                    self.manual_fixes_required.append(detailed_issue)

        return self.python_issues

    def _categorize_issue(self, code: str) -> str:
        """Categorize issue by code."""
        if not code:
            return "other"

        categories = {
            "E": "style",  # pycodestyle errors
            "W": "style",  # pycodestyle warnings
            "F": "error",  # pyflakes
            "C": "convention",  # mccabe
            "I": "import",  # isort
            "N": "naming",  # pep8-naming
            "D": "docstring",  # pydocstyle
            "UP": "upgrade",  # pyupgrade
            "YTT": "type",  # flake8-2020
            "ANN": "annotation",  # flake8-annotations
            "S": "security",  # bandit
            "BLE": "exception",  # flake8-blind-except
            "B": "bug",  # flake8-bugbear
            "A": "builtin",  # flake8-builtins
            "COM": "comma",  # flake8-commas
            "C4": "comprehension",  # flake8-comprehensions
            "DTZ": "datetime",  # flake8-datetimez
            "T10": "debug",  # flake8-debugger
            "ISC": "string",  # flake8-implicit-str-concat
            "ICN": "import",  # flake8-import-conventions
            "G": "logging",  # flake8-logging-format
            "INP": "namespace",  # flake8-no-pep420
            "PIE": "misc",  # flake8-pie
            "T20": "print",  # flake8-print
            "PT": "pytest",  # flake8-pytest-style
            "Q": "quotes",  # flake8-quotes
            "RSE": "raise",  # flake8-raise
            "RET": "return",  # flake8-return
            "SLF": "private",  # flake8-self
            "SIM": "simplify",  # flake8-simplify
            "TID": "import",  # flake8-tidy-imports
            "TCH": "type-checking",  # flake8-type-checking
            "ARG": "argument",  # flake8-unused-arguments
            "PTH": "pathlib",  # flake8-use-pathlib
            "ERA": "eradicate",  # eradicate
            "PD": "pandas",  # pandas-vet
            "PGH": "pygrep",  # pygrep-hooks
            "PL": "pylint",  # pylint
            "TRY": "tryceratops",  # tryceratops
            "FLY": "flynt",  # flynt
            "NPY": "numpy",  # numpy
            "PERF": "performance",  # perflint
            "RUF": "ruff",  # ruff-specific
        }

        for prefix, category in categories.items():
            if code.startswith(prefix):
                return category

        return "other"

    def _determine_severity(self, code: str, context: dict) -> str:
        """Determine issue severity based on code and context."""
        # Critical security issues
        critical_codes = [
            "S102",
            "S104",
            "S105",
            "S106",
            "S107",
            "S108",
            "S110",
            "S301",
            "S302",
            "S303",
            "S304",
            "S305",
            "S306",
            "S307",
            "S308",
            "S309",
            "S310",
            "S311",
            "S312",
            "S313",
            "S314",
            "S315",
            "S316",
            "S317",
            "S318",
            "S319",
            "S320",
            "S321",
            "S322",
            "S323",
            "S324",
            "S501",
            "S502",
            "S503",
            "S504",
            "S505",
            "S506",
            "S507",
            "S508",
            "S509",
            "S601",
            "S602",
            "S603",
            "S604",
            "S605",
            "S606",
            "S607",
            "S608",
            "S609",
            "S610",
            "S611",
            "S612",
            "S701",
            "S702",
        ]

        if code in critical_codes and not context.get("in_test_file", False):
            return "critical"

        # High severity
        high_codes = ["F401", "F402", "F403", "F404", "F405", "E999", "E902"]
        if code in high_codes:
            return "high"

        # Low severity for test files
        if context.get("in_test_file", False):
            return "low"

        # Medium by default
        return "medium"

    def _suggest_fix(self, issue: dict, context: dict) -> dict[str, Any]:
        """Suggest specific fix for the issue."""
        suggestions = {
            "S311": {
                "description": "Replace random with secrets for cryptographic use",
                "code_change": "import secrets\n# Use secrets.choice(), secrets.randbelow(), etc.",
                "command": 'sed -i "s/import random/import secrets/g" {file}',
            },
            "S113": {
                "description": "Add timeout to requests",
                "code_change": "requests.get(url, timeout=30)",
                "command": "Add timeout=30 parameter to all requests calls",
            },
            "PTH118": {
                "description": "Use pathlib instead of os.path",
                "code_change": 'from pathlib import Path\n# Use Path() / "subdir" instead of os.path.join()',
                "command": None,
            },
            "PLR2004": {
                "description": "Replace magic value with constant",
                "code_change": "MAX_ITEMS = 1000\nif len(items) > MAX_ITEMS:",
                "command": None,
            },
        }

        base_suggestion = suggestions.get(
            issue["code"], {"description": issue["message"], "code_change": None, "command": None}
        )

        # Add context-specific suggestions
        if context.get("class_name"):
            base_suggestion["context"] = f"In class {context['class_name']}"
        if context.get("method_name"):
            base_suggestion["context"] = base_suggestion.get("context", "") + f", method {context['method_name']}"

        return base_suggestion

    def _estimate_fix_complexity(self, issue: dict, context: dict) -> str:
        """Estimate complexity of fixing the issue."""
        if issue.get("fix"):
            return "trivial"  # Auto-fixable

        if issue["code"].startswith("S"):  # Security issues
            return "high"

        if context.get("complexity", 0) > 10:
            return "high"

        if issue["code"] in ["F401", "I001", "Q000"]:  # Simple import/quote fixes
            return "low"

        return "medium"

    def _calculate_fix_priority(self, issue: dict, context: dict) -> int:
        """Calculate fix priority (1-10, 10 being highest)."""
        priority = 5

        # Security issues are highest priority
        if issue.get("is_security"):
            priority = 10 if issue["severity"] == "critical" else 8

        # Syntax errors are high priority
        elif issue["code"] in ["E999", "F401"]:
            priority = 9

        # Performance issues
        elif issue["category"] == "performance":
            priority = 7

        # Style issues in test files are low priority
        elif context.get("in_test_file") and issue["category"] == "style":
            priority = 2

        # Auto-fixable issues can be higher priority
        if issue.get("fix_available"):
            priority += 1

        return min(priority, 10)

    def get_shell_issues_detailed(self):
        """Get detailed Shell script issues."""

        shell_files = []
        for pattern in ["*.sh", "*.bash"]:
            shell_files.extend(Path().rglob(pattern))

        self.stats["total_files_scanned"] += len(shell_files)

        for file_path in shell_files:
            if ".git" in str(file_path) or "node_modules" in str(file_path):
                continue

            # Run shellcheck with different formats for more info
            result = subprocess.run(  # noqa: S603 (calling known development tool)
                ["shellcheck", "--format=json", "--severity=style", "--external-sources", str(file_path)],  # noqa: S607 (development tool path)
                capture_output=True,
                text=True,
                check=False,
            )

            if result.stdout:
                try:
                    issues = json.loads(result.stdout)
                    file_meta = self.get_file_metadata(str(file_path))

                    with file_path.open(encoding="utf-8", errors="ignore") as f:
                        lines = f.readlines()

                    for issue in issues:
                        line_num = issue["line"]

                        # Get context
                        start_context = max(0, line_num - 4)
                        end_context = min(len(lines), line_num + 3)

                        detailed_issue = {
                            "file": str(file_path),
                            "line": line_num,
                            "endLine": issue.get("endLine", line_num),
                            "column": issue["column"],
                            "endColumn": issue.get("endColumn", issue["column"]),
                            "code": f"SC{issue['code']}",
                            "level": issue["level"],
                            "message": issue["message"],
                            "line_content": lines[line_num - 1].strip() if line_num <= len(lines) else "",
                            "context_before": [line.rstrip() for line in lines[start_context : line_num - 1]],
                            "context_after": [line.rstrip() for line in lines[line_num:end_context]],
                            # Additional context
                            "function_context": self._extract_shell_function_context(lines, line_num),
                            "in_conditional": self._is_in_shell_conditional(lines, line_num),
                            "in_loop": self._is_in_shell_loop(lines, line_num),
                            # File metadata
                            "file_size_kb": file_meta.get("size_kb", 0),
                            "file_lines": file_meta.get("lines", 0),
                            "shebang": lines[0].strip() if lines and lines[0].startswith("#!") else None,
                            # Fix information
                            "fix": issue.get("fix"),
                            "fix_available": "fix" in issue,
                            "wiki_url": f"https://www.shellcheck.net/wiki/SC{issue['code']}",
                            # Categorization
                            "category": self._categorize_shell_issue(issue["code"]),
                            "severity": self._determine_shell_severity(issue["level"], issue["code"]),
                            "fix_priority": self._calculate_shell_fix_priority(issue),
                        }

                        self.shell_issues.append(detailed_issue)
                        self.issues_by_file[str(file_path)].append(detailed_issue)

                        if detailed_issue["fix_available"]:
                            self.auto_fixable.append(detailed_issue)
                        else:
                            self.manual_fixes_required.append(detailed_issue)

                except json.JSONDecodeError:
                    pass

        return self.shell_issues

    def _extract_shell_function_context(self, lines: list[str], line_num: int) -> str | None:
        """Extract shell function context."""
        # Look backwards for function definition
        for i in range(line_num - 1, -1, -1):
            line = lines[i].strip()
            # Match function definitions
            if re.match(r"^(\w+)\s*\(\)\s*{", line) or re.match(r"^function\s+(\w+)", line):
                match = re.match(r"^(?:function\s+)?(\w+)", line)
                if match:
                    return match.group(1)
        return None

    def _is_in_shell_conditional(self, lines: list[str], line_num: int) -> bool:
        """Check if line is within a conditional block."""
        # Simple check - count if/fi statements
        if_count = 0
        fi_count = 0

        for i in range(0, min(line_num, len(lines))):
            line = lines[i].strip()
            if re.match(r"^if\s+", line):
                if_count += 1
            elif line == "fi":
                fi_count += 1

        return if_count > fi_count

    def _is_in_shell_loop(self, lines: list[str], line_num: int) -> bool:
        """Check if line is within a loop."""
        loop_count = 0
        done_count = 0

        for i in range(0, min(line_num, len(lines))):
            line = lines[i].strip()
            if re.match(r"^(for|while|until)\s+", line):
                loop_count += 1
            elif line == "done":
                done_count += 1

        return loop_count > done_count

    def _categorize_shell_issue(self, code: str) -> str:
        """Categorize ShellCheck issue."""
        code_num = int(code)

        if 1000 <= code_num < 2000:
            return "syntax"
        elif 2000 <= code_num < 2100:
            return "security"
        elif 2100 <= code_num < 2200:
            return "warning"
        elif 2200 <= code_num < 2300:
            return "info"
        else:
            return "style"

    def _determine_shell_severity(self, level: str, code: str) -> str:
        """Determine shell issue severity."""
        if level == "error":
            return "critical"
        elif level == "warning" and int(code) >= 2000 and int(code) < 2100:
            return "high"  # Security warnings
        elif level == "warning":
            return "medium"
        else:
            return "low"

    def _calculate_shell_fix_priority(self, issue: dict) -> int:
        """Calculate shell fix priority."""
        code_num = int(issue["code"])

        if issue["level"] == "error":
            return 10
        elif 2000 <= code_num < 2100:  # Security
            return 9
        elif issue["level"] == "warning":
            return 6
        else:
            return 3

    def get_typescript_issues_detailed(self):
        """Get detailed TypeScript/JavaScript issues."""

        debate_ui_path = Path("debate-ui")
        if not debate_ui_path.exists():
            return []

        # Count TS/JS files
        ts_files = list(debate_ui_path.rglob("*.ts")) + list(debate_ui_path.rglob("*.tsx"))
        js_files = list(debate_ui_path.rglob("*.js")) + list(debate_ui_path.rglob("*.jsx"))
        self.stats["total_files_scanned"] += len(ts_files) + len(js_files)

        # Run ESLint
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
                    if not file_result["messages"]:
                        continue

                    file_path = file_result["filePath"]
                    file_meta = self.get_file_metadata(file_path)

                    with Path(file_path).open(encoding="utf-8") as f:
                        lines = f.readlines()

                    for message in file_result["messages"]:
                        line_num = message["line"]

                        # Extract context
                        context = self._extract_typescript_context(lines, line_num)

                        detailed_issue = {
                            "file": file_path.replace(str(debate_ui_path.absolute()) + "/", "debate-ui/"),
                            "line": line_num,
                            "endLine": message.get("endLine", line_num),
                            "column": message["column"],
                            "endColumn": message.get("endColumn", message["column"]),
                            "severity": "error" if message["severity"] == 2 else "warning",
                            "message": message["message"],
                            "ruleId": message["ruleId"],
                            "line_content": lines[line_num - 1].strip() if line_num <= len(lines) else "",
                            # Context
                            "component_name": context.get("component_name"),
                            "function_name": context.get("function_name"),
                            "class_name": context.get("class_name"),
                            "hook_name": context.get("hook_name"),
                            "is_react_component": context.get("is_react_component", False),
                            "imports": context.get("imports", []),
                            "exports": context.get("exports", []),
                            "context_before": context.get("context_before", []),
                            "context_after": context.get("context_after", []),
                            # File metadata
                            "file_size_kb": file_meta.get("size_kb", 0),
                            "file_lines": file_meta.get("lines", 0),
                            "file_type": Path(file_path).suffix,
                            # Fix information
                            "fix": message.get("fix"),
                            "fix_available": "fix" in message,
                            "suggestions": message.get("suggestions", []),
                            # Categorization
                            "category": self._categorize_eslint_rule(message["ruleId"]),
                            "fix_priority": self._calculate_eslint_fix_priority(message),
                            # Additional metadata
                            "rule_docs": f"https://eslint.org/docs/rules/{message['ruleId']}",
                            "is_typescript": file_path.endswith((".ts", ".tsx")),
                            "is_test_file": "test" in file_path.lower() or "spec" in file_path.lower(),
                        }

                        self.typescript_issues.append(detailed_issue)
                        self.issues_by_file[file_path].append(detailed_issue)

                        if detailed_issue["fix_available"]:
                            self.auto_fixable.append(detailed_issue)
                        else:
                            self.manual_fixes_required.append(detailed_issue)

            except json.JSONDecodeError:
                pass

        return self.typescript_issues

    def _extract_typescript_context(self, lines: list[str], line_num: int) -> dict[str, Any]:
        """Extract TypeScript/React context."""
        context = {
            "component_name": None,
            "function_name": None,
            "class_name": None,
            "hook_name": None,
            "is_react_component": False,
            "imports": [],
            "exports": [],
            "context_before": [],
            "context_after": [],
        }

        # Get surrounding context
        start_context = max(0, line_num - 4)
        end_context = min(len(lines), line_num + 3)
        context["context_before"] = [line.rstrip() for line in lines[start_context : line_num - 1]]
        context["context_after"] = [line.rstrip() for line in lines[line_num:end_context]]

        # Extract imports and exports
        for i, line in enumerate(lines):
            if line.strip().startswith("import"):
                context["imports"].append(line.strip())
            elif line.strip().startswith("export"):
                context["exports"].append(line.strip())

        # Look for React component
        for i in range(max(0, line_num - 20), min(len(lines), line_num + 5)):
            line = lines[i].strip()

            # Function component
            comp_match = re.match(
                r"(?:export\s+)?(?:const|function)\s+(\w+).*(?::\s*(?:React\.)?FC|:\s*JSX\.Element|=.*=>.*JSX)", line
            )
            if comp_match:
                context["component_name"] = comp_match.group(1)
                context["is_react_component"] = True

            # Class component
            class_match = re.match(r"(?:export\s+)?class\s+(\w+).*extends.*(?:React\.)?Component", line)
            if class_match:
                context["component_name"] = class_match.group(1)
                context["class_name"] = class_match.group(1)
                context["is_react_component"] = True

            # Regular function
            func_match = re.match(r"(?:export\s+)?(?:async\s+)?function\s+(\w+)", line)
            if func_match and i <= line_num:
                context["function_name"] = func_match.group(1)

            # Arrow function
            arrow_match = re.match(r"(?:export\s+)?const\s+(\w+)\s*=.*=>(?!.*JSX)", line)
            if arrow_match and i <= line_num:
                context["function_name"] = arrow_match.group(1)

            # React hook
            hook_match = re.match(r"(?:const|let)\s+(use\w+)\s*=", line)
            if hook_match:
                context["hook_name"] = hook_match.group(1)

        return context

    def _categorize_eslint_rule(self, rule_id: str) -> str:
        """Categorize ESLint rule."""
        if not rule_id:
            return "other"

        categories = {
            "react/": "react",
            "react-hooks/": "react-hooks",
            "@typescript-eslint/": "typescript",
            "import/": "import",
            "jsx-a11y/": "accessibility",
            "security/": "security",
            "sonarjs/": "quality",
            "no-": "restriction",
            "prefer-": "suggestion",
        }

        for prefix, category in categories.items():
            if rule_id.startswith(prefix):
                return category

        return "eslint-core"

    def _calculate_eslint_fix_priority(self, message: dict) -> int:
        """Calculate ESLint fix priority."""
        if message["severity"] == 2:  # Error
            return 8

        if message["ruleId"] and "security" in message["ruleId"]:
            return 9

        if message["ruleId"] and "a11y" in message["ruleId"]:
            return 7

        if message.get("fix"):
            return 6

        return 4

    def get_java_issues_detailed(self):
        """Get detailed Java issues from multiple linters."""

        java_files = list(Path().rglob("*.java"))
        self.stats["total_files_scanned"] += len(java_files)

        # Run Maven checkstyle
        checkstyle_result = subprocess.run(  # noqa: S603 (calling known development tool)
            ["mvn", "checkstyle:check", "-Dcheckstyle.consoleOutput=true", "-Dcheckstyle.failOnViolation=false"],  # noqa: S607 (development tool path)
            capture_output=True,
            text=True,
            check=False,
        )

        # Parse checkstyle output
        if checkstyle_result.stdout:
            self._parse_checkstyle_output(checkstyle_result.stdout)

        return self.java_issues

    def _parse_checkstyle_output(self, output: str):
        """Parse checkstyle output for Java issues."""
        # Regex to match checkstyle violations
        pattern = r"\[(?:ERROR|WARN|INFO)\]\s+(.+?):(\d+)(?::(\d+))?\s+(.+?)\s+\[(.+?)\]"

        for match in re.finditer(pattern, output):
            file_path = match.group(1)
            line = int(match.group(2))
            column = int(match.group(3)) if match.group(3) else 0
            message = match.group(4)
            rule = match.group(5)

            # Get file context
            try:
                with Path(file_path).open(encoding="utf-8") as f:
                    lines = f.readlines()

                context = self._extract_java_context(lines, line)
                file_meta = self.get_file_metadata(file_path)

                detailed_issue = {
                    "file": file_path,
                    "line": line,
                    "column": column,
                    "message": message,
                    "rule": rule,
                    "tool": "checkstyle",
                    # Context
                    "class_name": context.get("class_name"),
                    "method_name": context.get("method_name"),
                    "line_content": lines[line - 1].strip() if line <= len(lines) else "",
                    "context_before": context.get("context_before", []),
                    "context_after": context.get("context_after", []),
                    # Metadata
                    "file_size_kb": file_meta.get("size_kb", 0),
                    "file_lines": file_meta.get("lines", 0),
                    "category": self._categorize_java_rule(rule),
                    "severity": "medium",
                    "fix_priority": 5,
                }

                self.java_issues.append(detailed_issue)
                self.issues_by_file[file_path].append(detailed_issue)

            except Exception:
                pass

    def _extract_java_context(self, lines: list[str], line_num: int) -> dict[str, Any]:
        """Extract Java context."""
        context = {"class_name": None, "method_name": None, "context_before": [], "context_after": []}

        # Get surrounding context
        start_context = max(0, line_num - 4)
        end_context = min(len(lines), line_num + 3)
        context["context_before"] = [line.rstrip() for line in lines[start_context : line_num - 1]]
        context["context_after"] = [line.rstrip() for line in lines[line_num:end_context]]

        # Look for class and method
        for i in range(0, min(line_num, len(lines))):
            line = lines[i].strip()

            # Class definition
            class_match = re.match(r"(?:public\s+)?(?:abstract\s+)?(?:final\s+)?class\s+(\w+)", line)
            if class_match:
                context["class_name"] = class_match.group(1)

            # Method definition
            method_match = re.match(
                r"(?:public\s+|private\s+|protected\s+)?(?:static\s+)?(?:final\s+)?(?:\w+(?:<[^>]+>)?)\s+(\w+)\s*\(",
                line,
            )
            if method_match and i <= line_num:
                context["method_name"] = method_match.group(1)

        return context

    def _categorize_java_rule(self, rule: str) -> str:
        """Categorize Java checkstyle rule."""
        categories = {
            "Naming": "naming",
            "Whitespace": "style",
            "Imports": "import",
            "Javadoc": "documentation",
            "Blocks": "structure",
            "Coding": "logic",
            "Design": "design",
        }

        for prefix, category in categories.items():
            if rule.startswith(prefix):
                return category

        return "other"

    def generate_fix_commands(self):
        """Generate specific fix commands for all issues."""

        # Python auto-fixes
        if self.python_issues:
            self.fix_commands.append(
                {
                    "description": "Auto-fix Python issues with Ruff",
                    "command": "ruff check . --fix --unsafe-fixes",
                    "issues_fixed": len([i for i in self.python_issues if i["fix_available"]]),
                }
            )

            # Security-specific fixes
            s311_files = {i["file"] for i in self.python_issues if i["code"] == "S311"}
            if s311_files:
                self.fix_commands.append(
                    {
                        "description": "Fix random usage for cryptographic purposes",
                        "command": "for f in "
                        + " ".join(s311_files)
                        + '; do sed -i "s/import random/import secrets/g" "$f"; done',
                        "issues_fixed": len(s311_files),
                    }
                )

        # Shell fixes
        shell_fixable = [i for i in self.shell_issues if i["fix_available"]]
        if shell_fixable:
            self.fix_commands.append(
                {
                    "description": "Apply ShellCheck fixes",
                    "command": 'for f in $(find . -name "*.sh"); do shellcheck -f diff "$f" | patch -p1; done',
                    "issues_fixed": len(shell_fixable),
                }
            )

        # TypeScript fixes
        if self.typescript_issues:
            self.fix_commands.append(
                {
                    "description": "Auto-fix TypeScript/JavaScript issues",
                    "command": "cd debate-ui && npx eslint src --ext .ts,.tsx,.js,.jsx --fix",
                    "issues_fixed": len([i for i in self.typescript_issues if i["fix_available"]]),
                }
            )

    def generate_comprehensive_report(self, output_file: str = "COMPREHENSIVE_LINTING_REPORT.md"):
        """Generate the comprehensive markdown report."""
        with Path(output_file).open("w", encoding="utf-8") as f:
            # Header
            f.write("# Comprehensive Linting Report\n\n")
            f.write(f"**Generated**: {self.stats['start_time'].isoformat()}\n")
            f.write(f"**Duration**: {(datetime.now() - self.stats['start_time']).total_seconds():.2f} seconds\n")
            f.write(f"**Project**: {Path.cwd()}\n\n")

            # Executive Summary
            f.write("## 📊 Executive Summary\n\n")
            f.write(f"- **Total Files Scanned**: {self.stats['total_files_scanned']}\n")
            f.write(f"- **Files with Issues**: {len(self.issues_by_file)}\n")
            f.write(
                f"- **Total Issues**: {len(self.python_issues) + len(self.shell_issues) + len(self.typescript_issues) + len(self.java_issues)}\n"
            )
            f.write(f"- **Auto-fixable Issues**: {self.stats['auto_fixable_count']}\n")
            f.write(f"- **Critical Security Issues**: {self.stats['security_critical']}\n")
            f.write(f"- **Critical Performance Issues**: {self.stats['performance_critical']}\n\n")

            # Quick Fix Commands
            f.write("## 🚀 Quick Fix Commands\n\n")
            f.write("```bash\n")
            for cmd in self.fix_commands:
                f.write(f"# {cmd['description']} ({cmd['issues_fixed']} issues)\n")
                f.write(f"{cmd['command']}\n\n")
            f.write("```\n\n")

            # Critical Issues
            f.write("## 🚨 Critical Issues Requiring Immediate Attention\n\n")

            critical_issues = [
                i
                for i in self.python_issues + self.shell_issues + self.typescript_issues
                if i.get("severity") == "critical"
            ]

            for issue in critical_issues[:10]:
                f.write(f"### {issue['file']} (Line {issue['line']})\n")
                f.write(f"- **Issue**: {issue.get('code', issue.get('ruleId', 'Unknown'))}\n")
                f.write(f"- **Message**: {issue['message']}\n")
                if issue.get("class_name"):
                    f.write(f"- **Class**: `{issue['class_name']}`\n")
                if issue.get("method_name"):
                    f.write(f"- **Method**: `{issue['method_name']}`\n")
                f.write(f"- **Code**: `{issue['line_content']}`\n")
                if issue.get("suggested_fix"):
                    f.write(f"- **Fix**: {issue['suggested_fix']['description']}\n")
                f.write("\n")

            # Detailed Python Issues
            f.write("## 🐍 Python Issues (Detailed)\n\n")
            self._write_python_issues_detailed(f)

            # Detailed Shell Issues
            f.write("\n## 🐚 Shell Script Issues (Detailed)\n\n")
            self._write_shell_issues_detailed(f)

            # Detailed TypeScript Issues
            f.write("\n## 📦 TypeScript/JavaScript Issues (Detailed)\n\n")
            self._write_typescript_issues_detailed(f)

            # Patterns and Recommendations
            f.write("\n## 📈 Patterns and Recommendations\n\n")
            patterns = self.analyze_python_issue_patterns()

            f.write("### Most Common Issues\n\n")
            sorted_patterns = sorted(patterns.items(), key=lambda x: x[1]["count"], reverse=True)

            for code, pattern_data in sorted_patterns[:10]:
                f.write(f"#### {code} ({pattern_data['count']} occurrences in {len(pattern_data['files'])} files)\n")
                f.write("Examples:\n")
                for example in pattern_data["examples"]:
                    f.write(f"- `{example['file']}:{example['line']}` - `{example['content']}`\n")
                f.write("\n")

            # File Rankings
            f.write("\n## 📁 Files with Most Issues\n\n")
            file_rankings = sorted(self.issues_by_file.items(), key=lambda x: len(x[1]), reverse=True)

            for file_path, issues in file_rankings[:20]:
                f.write(f"### {file_path} ({len(issues)} issues)\n")

                # Group by issue type
                issue_types = defaultdict(int)
                for issue in issues:
                    issue_types[issue.get("code", issue.get("ruleId", "Unknown"))] += 1

                f.write("Issue breakdown: ")
                f.write(
                    ", ".join(
                        [
                            f"{code} ({count})"
                            for code, count in sorted(issue_types.items(), key=lambda x: x[1], reverse=True)
                        ]
                    )
                )
                f.write("\n\n")

    def _write_python_issues_detailed(self, f):
        """Write detailed Python issues section."""
        # Group by category
        categories = defaultdict(list)
        for issue in self.python_issues:
            categories[issue["category"]].append(issue)

        for category, issues in sorted(categories.items()):
            f.write(f"### {category.title()} Issues ({len(issues)} total)\n\n")

            # Show first 5 issues in detail
            for issue in issues[:5]:
                f.write(f"#### `{issue['file']}:{issue['line']}:{issue['column']}`\n")
                f.write(f"- **Code**: {issue['code']}\n")
                f.write(f"- **Message**: {issue['message']}\n")
                f.write(f"- **Severity**: {issue['severity']}\n")
                f.write(f"- **Priority**: {issue['fix_priority']}/10\n")

                if issue["class_name"]:
                    f.write(f"- **Class**: `{issue['class_name']}`\n")
                if issue["method_name"]:
                    f.write(f"- **Method**: `{issue['method_name']}` ({issue['function_type']})\n")
                if issue["decorators"]:
                    f.write(f"- **Decorators**: {', '.join(issue['decorators'])}\n")
                if issue["complexity"] > 5:
                    f.write(f"- **Complexity**: {issue['complexity']} (high)\n")

                f.write("\n**Code Context**:\n```python\n")
                # Show context
                for line in issue["context_before"][-2:]:
                    f.write(f"{line}\n")
                f.write(f">>> {issue['line_content']}  # <- Issue here\n")
                for line in issue["context_after"][:2]:
                    f.write(f"{line}\n")
                f.write("```\n")

                if issue["fix_available"]:
                    f.write("\n**Auto-fix available**: ✅\n")
                elif issue["suggested_fix"].get("code_change"):
                    f.write(f"\n**Suggested Fix**:\n```python\n{issue['suggested_fix']['code_change']}\n```\n")

                f.write(f"\n**Documentation**: {issue.get('url', 'N/A')}\n")
                f.write("\n---\n\n")

            if len(issues) > 5:
                f.write(f"\n*... and {len(issues) - 5} more {category} issues*\n\n")

    def _write_shell_issues_detailed(self, f):
        """Write detailed Shell issues section."""
        # Group by severity
        severities = defaultdict(list)
        for issue in self.shell_issues:
            severities[issue["severity"]].append(issue)

        for severity, issues in [
            ("critical", severities["critical"]),
            ("high", severities["high"]),
            ("medium", severities["medium"]),
            ("low", severities["low"]),
        ]:
            if not issues:
                continue

            f.write(f"### {severity.title()} Severity ({len(issues)} issues)\n\n")

            for issue in issues[:5]:
                f.write(f"#### `{issue['file']}:{issue['line']}:{issue['column']}`\n")
                f.write(f"- **Code**: {issue['code']}\n")
                f.write(f"- **Level**: {issue['level']}\n")
                f.write(f"- **Message**: {issue['message']}\n")

                if issue["function_context"]:
                    f.write(f"- **Function**: `{issue['function_context']}`\n")
                if issue["in_conditional"]:
                    f.write("- **Context**: Inside conditional block\n")
                if issue["in_loop"]:
                    f.write("- **Context**: Inside loop\n")

                f.write("\n**Code Context**:\n```bash\n")
                for line in issue["context_before"][-2:]:
                    f.write(f"{line}\n")
                f.write(f">>> {issue['line_content']}  # <- Issue here\n")
                for line in issue["context_after"][:2]:
                    f.write(f"{line}\n")
                f.write("```\n")

                if issue["fix_available"]:
                    f.write("\n**Auto-fix available**: ✅\n")

                f.write(f"\n**Wiki**: {issue['wiki_url']}\n")
                f.write("\n---\n\n")

            if len(issues) > 5:
                f.write(f"\n*... and {len(issues) - 5} more {severity} issues*\n\n")

    def _write_typescript_issues_detailed(self, f):
        """Write detailed TypeScript issues section."""
        if not self.typescript_issues:
            f.write("No TypeScript/JavaScript issues found.\n")
            return

        # Group by file
        for issue in self.typescript_issues:
            f.write(f"#### `{issue['file']}:{issue['line']}:{issue['column']}`\n")
            f.write(f"- **Rule**: {issue['ruleId']}\n")
            f.write(f"- **Severity**: {issue['severity']}\n")
            f.write(f"- **Message**: {issue['message']}\n")
            f.write(f"- **Category**: {issue['category']}\n")

            if issue["component_name"]:
                f.write(f"- **Component**: `{issue['component_name']}`")
                if issue["is_react_component"]:
                    f.write(" (React Component)")
                f.write("\n")
            if issue["function_name"]:
                f.write(f"- **Function**: `{issue['function_name']}`\n")
            if issue["hook_name"]:
                f.write(f"- **Hook**: `{issue['hook_name']}`\n")

            f.write("\n**Code Context**:\n```typescript\n")
            for line in issue["context_before"][-2:]:
                f.write(f"{line}\n")
            f.write(f">>> {issue['line_content']}  # <- Issue here\n")
            for line in issue["context_after"][:2]:
                f.write(f"{line}\n")
            f.write("```\n")

            if issue["fix_available"]:
                f.write("\n**Auto-fix available**: ✅\n")

            if issue["suggestions"]:
                f.write("\n**Suggestions**:\n")
                for suggestion in issue["suggestions"]:
                    f.write(f"- {suggestion.get('desc', 'N/A')}\n")

            f.write(f"\n**Documentation**: {issue['rule_docs']}\n")
            f.write("\n---\n\n")

    def generate_json_report(self, output_file: str = "comprehensive_linting_issues.json"):
        """Generate comprehensive JSON report."""
        report = {
            "metadata": {
                "generated": self.stats["start_time"].isoformat(),
                "duration_seconds": (datetime.now() - self.stats["start_time"]).total_seconds(),
                "project_path": str(Path.cwd()),
                "statistics": self.stats,
            },
            "summary": {
                "total_issues": len(self.python_issues)
                + len(self.shell_issues)
                + len(self.typescript_issues)
                + len(self.java_issues),
                "by_language": {
                    "python": len(self.python_issues),
                    "shell": len(self.shell_issues),
                    "typescript": len(self.typescript_issues),
                    "java": len(self.java_issues),
                },
                "by_severity": {
                    "critical": len(
                        [i for i in self.python_issues + self.shell_issues if i.get("severity") == "critical"]
                    ),
                    "high": len([i for i in self.python_issues + self.shell_issues if i.get("severity") == "high"]),
                    "medium": len([i for i in self.python_issues + self.shell_issues if i.get("severity") == "medium"]),
                    "low": len([i for i in self.python_issues + self.shell_issues if i.get("severity") == "low"]),
                },
                "auto_fixable": self.stats["auto_fixable_count"],
                "security_issues": len(self.security_issues),
                "performance_issues": len(self.performance_issues),
            },
            "fix_commands": self.fix_commands,
            "issues": {
                "python": self.python_issues,
                "shell": self.shell_issues,
                "typescript": self.typescript_issues,
                "java": self.java_issues,
            },
            "by_file": dict(self.issues_by_file),
            "patterns": {"python": self.analyze_python_issue_patterns()},
        }

        with Path(output_file).open("w", encoding="utf-8") as f:
            json.dump(report, f, indent=2, default=str)

        return report


def main():
    reporter = ComprehensiveLintingReporter()

    # Collect all issues

    reporter.get_python_issues_detailed()
    reporter.get_shell_issues_detailed()
    reporter.get_typescript_issues_detailed()
    reporter.get_java_issues_detailed()

    # Generate fix commands
    reporter.generate_fix_commands()

    # Generate reports
    reporter.generate_comprehensive_report()
    reporter.generate_json_report()

    # Print summary

    # Show top priorities
    all_issues = reporter.python_issues + reporter.shell_issues + reporter.typescript_issues
    priority_issues = sorted(all_issues, key=lambda x: x.get("fix_priority", 0), reverse=True)

    for _issue in priority_issues[:5]:
        pass


if __name__ == "__main__":
    main()
