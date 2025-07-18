"""
Code style and quality analyzer strategies.

This module implements strategies for checking code style,
formatting, and general quality issues.
"""

import re
from typing import Optional

from ..core.interfaces import CodeIssue, IssueLevel
from .base_analyzer import AnalysisStrategy, AnalyzerContext


class StyleGuideStrategy(AnalysisStrategy):
    """Strategy for enforcing coding style guidelines."""

    def __init__(self):
        self.style_rules = self._build_style_rules()

    def _build_style_rules(self) -> dict[str, list[dict]]:
        """Build style rules for different languages."""
        return {
            "python": [
                {
                    "name": "line_length",
                    "check": lambda line: len(line) > 120,
                    "message": "Line too long ({length} > 120 characters)",
                    "level": IssueLevel.INFO,
                },
                {
                    "name": "class_naming",
                    "pattern": re.compile(r"^class\s+([a-z_][a-zA-Z0-9_]*)\s*[\(:]"),
                    "message": "Class name should use CapWords convention",
                    "level": IssueLevel.WARNING,
                },
                {
                    "name": "function_naming",
                    "pattern": re.compile(r"^def\s+([A-Z][a-zA-Z0-9_]*)\s*\("),
                    "message": "Function name should use snake_case",
                    "level": IssueLevel.WARNING,
                },
                {
                    "name": "constant_naming",
                    "pattern": re.compile(r"^([A-Z_]+)\s*=\s*[^=]"),
                    "check": lambda match: not match.group(1).isupper(),
                    "message": "Constants should be UPPER_CASE",
                    "level": IssueLevel.INFO,
                },
            ],
            "javascript": [
                {
                    "name": "line_length",
                    "check": lambda line: len(line) > 100,
                    "message": "Line too long ({length} > 100 characters)",
                    "level": IssueLevel.INFO,
                },
                {
                    "name": "var_usage",
                    "pattern": re.compile(r"\bvar\s+\w+\s*="),
                    "message": "Use const or let instead of var",
                    "level": IssueLevel.WARNING,
                },
                {
                    "name": "semicolon",
                    "pattern": re.compile(r"[^;]\s*$"),
                    "check": lambda line: not any(line.strip().endswith(x) for x in ["{", "}", ";", ","]),
                    "message": "Missing semicolon",
                    "level": IssueLevel.INFO,
                },
            ],
        }

    async def analyze(self, context: AnalyzerContext) -> list[CodeIssue]:
        """Check code style issues."""
        issues = []
        lines = context.content.splitlines()
        rules = self.style_rules.get(context.language, [])

        for line_num, line in enumerate(lines, 1):
            # Skip empty lines and comments
            if not line.strip() or self._is_comment(line.strip(), context.language):
                continue

            for rule in rules:
                issue = self._check_rule(rule, line, line_num, context)
                if issue:
                    issues.append(issue)

        # File-level checks
        issues.extend(self._check_file_level_style(context))

        return issues

    def _check_rule(self, rule: dict, line: str, line_num: int, context: AnalyzerContext) -> Optional[CodeIssue]:
        """Check a single style rule."""
        # Simple check function
        if "check" in rule and rule["check"](line):
            message = rule["message"]
            if "{length}" in message:
                message = message.format(length=len(line))

            return CodeIssue(
                level=rule["level"],
                category="style",
                message=message,
                file_path=context.file_path,
                line_number=line_num,
                rule_id=f"style-{rule['name']}",
            )

        # Pattern matching
        if "pattern" in rule:
            match = rule["pattern"].search(line)
            if match:
                # Additional check if provided
                if "check" in rule and not rule["check"](match):
                    return None

                return CodeIssue(
                    level=rule["level"],
                    category="style",
                    message=rule["message"],
                    file_path=context.file_path,
                    line_number=line_num,
                    rule_id=f"style-{rule['name']}",
                )

        return None

    def _check_file_level_style(self, context: AnalyzerContext) -> list[CodeIssue]:
        """Check file-level style issues."""
        issues = []

        # Check file has newline at end
        if not context.content.endswith("\n"):
            issues.append(
                CodeIssue(
                    level=IssueLevel.INFO,
                    category="style",
                    message="File should end with a newline",
                    file_path=context.file_path,
                    rule_id="style-final-newline",
                )
            )

        # Check for trailing whitespace
        lines = context.content.splitlines()
        for line_num, line in enumerate(lines, 1):
            if line.endswith((" ", "\t")):
                issues.append(
                    CodeIssue(
                        level=IssueLevel.INFO,
                        category="style",
                        message="Trailing whitespace",
                        file_path=context.file_path,
                        line_number=line_num,
                        rule_id="style-trailing-whitespace",
                    )
                )

        return issues

    def _is_comment(self, line: str, language: str) -> bool:
        """Check if line is a comment."""
        comment_patterns = {
            "python": r"^\s*#",
            "javascript": r"^\s*(/\*|//)",
            "java": r"^\s*(/\*|//)",
            "c": r"^\s*(/\*|//)",
            "cpp": r"^\s*(/\*|//)",
        }

        pattern = comment_patterns.get(language)
        if pattern:
            return bool(re.match(pattern, line))
        return False

    def get_name(self) -> str:
        return "style-guide"

    def get_supported_languages(self) -> set[str]:
        return set(self.style_rules.keys())


class ComplexityStrategy(AnalysisStrategy):
    """Strategy for analyzing code complexity."""

    async def analyze(self, context: AnalyzerContext) -> list[CodeIssue]:
        """Analyze code complexity."""
        issues = []

        # Calculate cyclomatic complexity for functions
        if context.language in ["python", "javascript", "java"]:
            functions = self._extract_functions(context)

            for func in functions:
                complexity = self._calculate_complexity(func)

                if complexity > 10:
                    issues.append(
                        CodeIssue(
                            level=IssueLevel.WARNING,
                            category="complexity",
                            message=f"Function '{func['name']}' has high cyclomatic complexity ({complexity})",
                            file_path=context.file_path,
                            line_number=func["start_line"],
                            suggestion="Consider breaking this function into smaller functions",
                            rule_id="complexity-cyclomatic",
                        )
                    )

        # Check function length
        issues.extend(self._check_function_length(context))

        # Check class size
        issues.extend(self._check_class_size(context))

        return issues

    def _extract_functions(self, context: AnalyzerContext) -> list[dict]:
        """Extract function definitions from code."""
        functions = []
        lines = context.content.splitlines()

        if context.language == "python":
            pattern = re.compile(r"^(\s*)def\s+(\w+)\s*\(")
            current_func = None

            for line_num, line in enumerate(lines, 1):
                match = pattern.match(line)
                if match:
                    indent = len(match.group(1))
                    name = match.group(2)

                    if current_func:
                        current_func["end_line"] = line_num - 1
                        current_func["content"] = "\n".join(
                            lines[current_func["start_line"] - 1 : current_func["end_line"]]
                        )
                        functions.append(current_func)

                    current_func = {"name": name, "start_line": line_num, "indent": indent}

            # Handle last function
            if current_func:
                current_func["end_line"] = len(lines)
                current_func["content"] = "\n".join(lines[current_func["start_line"] - 1 : current_func["end_line"]])
                functions.append(current_func)

        return functions

    def _calculate_complexity(self, func: dict) -> int:
        """Calculate cyclomatic complexity of a function."""
        complexity = 1  # Base complexity

        # Count decision points
        decision_keywords = [
            r"\bif\b",
            r"\belif\b",
            r"\belse\b",
            r"\bfor\b",
            r"\bwhile\b",
            r"\btry\b",
            r"\bexcept\b",
            r"\band\b",
            r"\bor\b",
            r"\?",
            r"\bcase\b",
            r"\bcatch\b",
        ]

        content = func.get("content", "")
        for keyword in decision_keywords:
            complexity += len(re.findall(keyword, content))

        return complexity

    def _check_function_length(self, context: AnalyzerContext) -> list[CodeIssue]:
        """Check for overly long functions."""
        issues = []
        functions = self._extract_functions(context)

        for func in functions:
            length = func["end_line"] - func["start_line"] + 1

            if length > 50:
                issues.append(
                    CodeIssue(
                        level=IssueLevel.WARNING,
                        category="complexity",
                        message=f"Function '{func['name']}' is too long ({length} lines)",
                        file_path=context.file_path,
                        line_number=func["start_line"],
                        suggestion="Consider breaking this function into smaller functions",
                        rule_id="complexity-function-length",
                    )
                )

        return issues

    def _check_class_size(self, context: AnalyzerContext) -> list[CodeIssue]:
        """Check for overly large classes."""
        issues = []

        if context.language == "python":
            pattern = re.compile(r"^class\s+(\w+)")
            lines = context.content.splitlines()

            current_class = None
            for line_num, line in enumerate(lines, 1):
                match = pattern.match(line)
                if match:
                    if current_class:
                        size = line_num - current_class["start_line"]
                        if size > 300:
                            issues.append(
                                CodeIssue(
                                    level=IssueLevel.WARNING,
                                    category="complexity",
                                    message=f"Class '{current_class['name']}' is too large ({size} lines)",
                                    file_path=context.file_path,
                                    line_number=current_class["start_line"],
                                    suggestion="Consider splitting this class into smaller classes",
                                    rule_id="complexity-class-size",
                                )
                            )

                    current_class = {"name": match.group(1), "start_line": line_num}

            # Check last class
            if current_class:
                size = len(lines) - current_class["start_line"] + 1
                if size > 300:
                    issues.append(
                        CodeIssue(
                            level=IssueLevel.WARNING,
                            category="complexity",
                            message=f"Class '{current_class['name']}' is too large ({size} lines)",
                            file_path=context.file_path,
                            line_number=current_class["start_line"],
                            suggestion="Consider splitting this class into smaller classes",
                            rule_id="complexity-class-size",
                        )
                    )

        return issues

    def get_name(self) -> str:
        return "complexity"

    def get_supported_languages(self) -> set[str]:
        return {"python", "javascript", "java", "go", "csharp"}


class DocumentationStrategy(AnalysisStrategy):
    """Strategy for checking documentation quality."""

    async def analyze(self, context: AnalyzerContext) -> list[CodeIssue]:
        """Check documentation issues."""
        issues = []

        # Check for missing docstrings/comments
        if context.language == "python":
            issues.extend(self._check_python_docstrings(context))
        elif context.language in ["javascript", "typescript"]:
            issues.extend(self._check_jsdoc(context))

        # Check for TODO/FIXME comments
        issues.extend(self._check_todo_comments(context))

        return issues

    def _check_python_docstrings(self, context: AnalyzerContext) -> list[CodeIssue]:
        """Check Python docstring coverage."""
        issues = []
        lines = context.content.splitlines()

        # Patterns for Python constructs that should have docstrings
        patterns = {
            "class": re.compile(r"^class\s+(\w+)"),
            "function": re.compile(r"^def\s+(\w+)"),
            "method": re.compile(r"^\s+def\s+(\w+)"),
        }

        for line_num, line in enumerate(lines, 1):
            for construct_type, pattern in patterns.items():
                match = pattern.match(line)
                if match:
                    name = match.group(1)

                    # Skip private methods
                    if name.startswith("_") and not name.startswith("__"):
                        continue

                    # Check next few lines for docstring
                    has_docstring = False
                    for i in range(1, 5):
                        if line_num + i <= len(lines):
                            next_line = lines[line_num + i - 1].strip()
                            if '"""' in next_line or "'''" in next_line:
                                has_docstring = True
                                break
                            elif next_line and not next_line.startswith(("def", "class", "@")):
                                break

                    if not has_docstring:
                        issues.append(
                            CodeIssue(
                                level=IssueLevel.INFO,
                                category="documentation",
                                message=f"{construct_type.capitalize()} '{name}' is missing docstring",
                                file_path=context.file_path,
                                line_number=line_num,
                                suggestion="Add a docstring to document purpose and parameters",
                                rule_id="doc-missing-docstring",
                            )
                        )

        return issues

    def _check_jsdoc(self, context: AnalyzerContext) -> list[CodeIssue]:
        """Check JSDoc documentation."""
        # Implementation for JavaScript/TypeScript
        return []

    def _check_todo_comments(self, context: AnalyzerContext) -> list[CodeIssue]:
        """Check for TODO/FIXME comments."""
        issues = []
        lines = context.content.splitlines()

        todo_pattern = re.compile(r"(?i)(TODO|FIXME|HACK|XXX|BUG):\s*(.+)")

        for line_num, line in enumerate(lines, 1):
            match = todo_pattern.search(line)
            if match:
                tag = match.group(1).upper()
                message = match.group(2).strip()

                level = IssueLevel.WARNING if tag in ["FIXME", "BUG"] else IssueLevel.INFO

                issues.append(
                    CodeIssue(
                        level=level,
                        category="documentation",
                        message=f"{tag} comment: {message}",
                        file_path=context.file_path,
                        line_number=line_num,
                        suggestion="Address this technical debt",
                        rule_id=f"doc-{tag.lower()}",
                    )
                )

        return issues

    def get_name(self) -> str:
        return "documentation"

    def get_supported_languages(self) -> set[str]:
        return {"python", "javascript", "typescript", "java", "go"}
