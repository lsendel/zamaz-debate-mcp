#!/usr/bin/env python3
"""
Comment generator for Kiro GitHub integration.
This module generates intelligent comments for code issues found in pull requests.
"""

import json
import logging
import os
from typing import Any

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[logging.StreamHandler(), logging.FileHandler("kiro_comment_generator.log")],
)
logger = logging.getLogger("kiro_comment_generator")

# Constants
KIRO_API_URL = os.environ.get("KIRO_API_URL", "https://api.kiro.ai")


class CommentGenerator:
    """Generates intelligent comments for code issues."""

    def __init__(self, config: dict[str, Any] | None = None):
        """Initialize the comment generator."""
        self.config = config or {}
        self.comment_style = self.config.get("review", {}).get("comment_style", "educational")

    def generate_comments(self, issues: list[dict[str, Any]]) -> dict[str, Any]:
        """Generate comments for a list of issues."""
        logger.info(f"Generating comments for {len(issues)} issues")

        # Group issues by file and line
        issues_by_file = {}
        for issue in issues:
            file_path = issue.get("file_path", "")
            if file_path not in issues_by_file:
                issues_by_file[file_path] = []
            issues_by_file[file_path].append(issue)

        # Generate file comments
        file_comments = []
        for file_path, file_issues in issues_by_file.items():
            # Group issues by line
            issues_by_line = {}
            for issue in file_issues:
                line_start = issue.get("line_start", 1)
                if line_start not in issues_by_line:
                    issues_by_line[line_start] = []
                issues_by_line[line_start].append(issue)

            # Generate comments for each line
            for line_number, line_issues in issues_by_line.items():
                comment = self._generate_comment_for_line(file_path, line_number, line_issues)
                if comment:
                    file_comments.append({"path": file_path, "line": line_number, "body": comment})

        # Generate summary comment
        summary_comment = self._generate_summary_comment(issues)

        return {"summary_comment": summary_comment, "file_comments": file_comments}

    def _generate_comment_for_line(self, file_path: str, line_number: int, issues: list[dict[str, Any]]) -> str:
        """Generate a comment for a specific line with issues."""
        if not issues:
            return ""

        # Sort issues by severity
        issues = sorted(issues, key=lambda x: self._get_severity_order(x.get("severity", "suggestion")))

        # Get file extension
        _, file_ext = os.path.splitext(file_path)

        # Start with a header
        comment = ""

        # Add each issue
        for i, issue in enumerate(issues):
            severity = issue.get("severity", "suggestion").upper()
            issue.get("category", "best_practice").replace("_", " ").title()
            message = issue.get("message", "")
            issue.get("code_snippet", "")
            fix_suggestion = issue.get("fix_suggestion", "")
            issue.get("rule_id", "")
            references = issue.get("references", [])

            # Add separator if not the first issue
            if i > 0:
                comment += "\n\n---\n\n"

            # Add issue header with severity indicator
            severity_emoji = self._get_severity_emoji(severity.lower())
            comment += f"{severity_emoji} **{severity}**: {message}\n\n"

            # Add explanation based on comment style
            if self.comment_style == "educational":
                explanation = self._generate_educational_explanation(issue, file_ext)
                if explanation:
                    comment += f"{explanation}\n\n"

            # Add fix suggestion if available
            if fix_suggestion:
                # Format as GitHub suggestion block for easy application
                comment += f"**Suggested fix:**\n\n```suggestion\n{fix_suggestion}\n```\n\n"
                comment += (
                    "You can apply this suggestion directly by clicking the 'Add suggestion to batch' button above.\n\n"
                )
            elif "suggestion" in issue:
                # Use the suggestion from the issue if available
                suggestion = issue.get("suggestion", {})
                if suggestion.get("replacement_text"):
                    comment += f"**Suggested fix:**\n\n```suggestion\n{suggestion.get('replacement_text')}\n```\n\n"
                    comment += "You can apply this suggestion directly by clicking the 'Add suggestion to batch' button above.\n\n"

            # Add references if available
            if references:
                comment += "**References:**\n"
                for ref in references:
                    comment += f"- {ref}\n"

        return comment

    def _generate_educational_explanation(self, issue: dict[str, Any], file_ext: str) -> str:
        """Generate an educational explanation for an issue."""
        category = issue.get("category", "best_practice")
        rule_id = issue.get("rule_id", "")

        # Get explanation based on category and rule ID
        if category == "security":
            return self._get_security_explanation(rule_id, file_ext)
        elif category == "performance":
            return self._get_performance_explanation(rule_id, file_ext)
        elif category == "style":
            return self._get_style_explanation(rule_id, file_ext)
        elif category == "syntax":
            return self._get_syntax_explanation(rule_id, file_ext)
        else:
            return self._get_general_explanation(rule_id, file_ext)

    def _get_security_explanation(self, rule_id: str, file_ext: str) -> str:
        """Get an explanation for a security issue."""
        explanations = {
            "hardcoded-password": "Hardcoded passwords in source code are a security risk. If the code is ever exposed (e.g., in a public repository), credentials could be compromised. Instead, use environment variables, secure vaults, or configuration files that are not checked into version control.",
            "hardcoded-api-key": "API keys should not be hardcoded in source code. If the code is ever exposed, the API key could be compromised. Instead, use environment variables, secure vaults, or configuration files that are not checked into version control.",
            "hardcoded-secret": "Secrets should not be hardcoded in source code. If the code is ever exposed, the secret could be compromised. Instead, use environment variables, secure vaults, or configuration files that are not checked into version control.",
            "unsafe-eval": "The `eval()` function executes arbitrary code, which can be dangerous if the input is not properly sanitized. This can lead to code injection vulnerabilities. Consider using safer alternatives like JSON.parse() for JSON data or specific parsers for other formats.",
            "unsafe-exec": "Executing commands with user input can lead to command injection vulnerabilities. Always validate and sanitize user input before using it in command execution functions.",
            "sql-injection": "String concatenation in SQL queries can lead to SQL injection vulnerabilities. Use parameterized queries or prepared statements instead to ensure that user input is properly escaped.",
        }

        # Return explanation if available, otherwise a generic one
        return explanations.get(
            rule_id,
            "Security issues can lead to vulnerabilities in your application. Always follow security best practices to protect your application and user data.",
        )

    def _get_performance_explanation(self, rule_id: str, file_ext: str) -> str:
        """Get an explanation for a performance issue."""
        explanations = {
            "nested-loops": "Nested loops can lead to O(n²) time complexity or worse, which can cause performance issues with large datasets. Consider if there's a more efficient algorithm or data structure that could be used instead.",
            "multiple-filters": "Chaining multiple filter operations can be inefficient as each operation creates a new collection. Consider combining filters into a single operation to improve performance.",
            "select-all": "Using SELECT * in SQL queries retrieves all columns, which can be inefficient if you only need specific columns. This increases network traffic, memory usage, and processing time. Specify only the columns you need in your queries.",
            "large-timeout-function": "Large functions in setTimeout can delay the execution of other JavaScript code and potentially cause UI freezes. Consider breaking down the function into smaller parts or using requestAnimationFrame for UI updates.",
        }

        # Return explanation if available, otherwise a generic one
        return explanations.get(
            rule_id,
            "Performance issues can slow down your application and degrade user experience. Optimizing code for performance is important, especially for operations that are executed frequently or with large datasets.",
        )

    def _get_style_explanation(self, rule_id: str, file_ext: str) -> str:
        """Get an explanation for a style issue."""
        # Get language-specific explanations
        if file_ext == ".py":
            explanations = {
                "flake8-E101": "Indentation contains mixed spaces and tabs. PEP 8 recommends using spaces for indentation.",
                "flake8-E201": "Whitespace after '(' is not recommended in Python. PEP 8 suggests no whitespace after opening parentheses.",
                "flake8-E501": "Line too long. PEP 8 recommends limiting lines to 79 characters for code and 72 for comments and docstrings.",
            }
        elif file_ext in [".js", ".jsx", ".ts", ".tsx"]:
            explanations = {
                "prettier-format": "Code formatting doesn't match the project's style guide. Consistent formatting improves readability and maintainability."
            }
        elif file_ext == ".java":
            explanations = {
                "checkstyle": "Code doesn't follow Java style conventions. Consistent style improves readability and maintainability."
            }
        else:
            explanations = {}

        # Return explanation if available, otherwise a generic one
        return explanations.get(
            rule_id,
            "Consistent code style improves readability and maintainability. Following style conventions makes it easier for other developers to understand and work with your code.",
        )

    def _get_syntax_explanation(self, rule_id: str, file_ext: str) -> str:
        """Get an explanation for a syntax issue."""
        # Get language-specific explanations
        if file_ext == ".py":
            explanations = {
                "python-syntax": "Python syntax error. The code cannot be parsed by the Python interpreter.",
                "python-pyflakes": "Python issue detected by Pyflakes. This could be an undefined variable, unused import, or other issue.",
            }
        elif file_ext in [".js", ".jsx", ".ts", ".tsx"]:
            explanations = {
                "js-syntax": "JavaScript/TypeScript syntax error. The code cannot be parsed by the JavaScript/TypeScript interpreter."
            }
        elif file_ext == ".java":
            explanations = {
                "java-syntax": "Java syntax error. The code cannot be compiled by the Java compiler.",
                "java-lint": "Java warning detected by the compiler. This could be a potential issue that doesn't prevent compilation.",
            }
        else:
            explanations = {}

        # Return explanation if available, otherwise a generic one
        return explanations.get(
            rule_id,
            "Syntax errors prevent code from being parsed or compiled correctly. Fixing these issues is necessary for the code to run.",
        )

    def _get_general_explanation(self, rule_id: str, file_ext: str) -> str:
        """Get a general explanation for an issue."""
        explanations = {
            "best-practice": "Following best practices improves code quality, maintainability, and reduces the likelihood of bugs."
        }

        # Return explanation if available, otherwise a generic one
        return explanations.get(
            rule_id,
            "This issue could affect code quality, maintainability, or performance. Addressing it will improve your codebase.",
        )

    def _generate_summary_comment(self, issues: list[dict[str, Any]]) -> str:
        """Generate a summary comment for all issues."""
        if not issues:
            return self._generate_no_issues_comment()

        # Count issues by severity
        severity_counts = {}
        category_counts = {}
        for issue in issues:
            severity = issue.get("severity", "suggestion").lower()
            category = issue.get("category", "best_practice").lower()

            severity_counts[severity] = severity_counts.get(severity, 0) + 1
            category_counts[category] = category_counts.get(category, 0) + 1

        # Generate summary
        summary = "## Kiro AI Code Review\n\n"

        # Add overview
        summary += "### Overview\n\n"
        summary += f"I've reviewed this PR and found **{len(issues)}** issues to address:\n\n"

        # Add severity breakdown
        for severity in ["critical", "major", "minor", "suggestion"]:
            if severity in severity_counts:
                emoji = self._get_severity_emoji(severity)
                summary += f"- {emoji} **{severity_counts[severity]}** {severity.title()}\n"

        summary += "\n"

        # Add category breakdown
        summary += "### Issue Categories\n\n"
        for category, count in category_counts.items():
            category_display = category.replace("_", " ").title()
            summary += f"- **{count}** {category_display}\n"

        summary += "\n"

        # Add explanation based on comment style
        if self.comment_style == "educational":
            summary += "### Next Steps\n\n"
            summary += "I've added detailed comments to the specific lines with issues. Each comment includes:\n\n"
            summary += "- An explanation of the issue\n"
            summary += "- Educational context to help understand why it matters\n"
            summary += "- Suggested fixes where applicable\n\n"

            if "critical" in severity_counts or "major" in severity_counts:
                summary += "**Please address the Critical and Major issues before merging this PR.**\n\n"

        # Add footer
        summary += "Let me know if you have any questions or need further assistance!"

        return summary

    def _generate_no_issues_comment(self) -> str:
        """Generate a comment when no issues are found."""
        summary = "## Kiro AI Code Review\n\n"
        summary += "### Overview\n\n"
        summary += "I've reviewed this PR and found no issues to address. Great job! 🎉\n\n"

        # Add educational content based on comment style
        if self.comment_style == "educational":
            summary += "### Code Quality\n\n"
            summary += "Your code follows good practices in terms of:\n\n"
            summary += "- Syntax and style conventions\n"
            summary += "- Security considerations\n"
            summary += "- Performance optimizations\n"
            summary += "- Maintainability standards\n\n"

        # Add footer
        summary += "Let me know if you have any questions or need any assistance!"

        return summary

    def _get_severity_order(self, severity: str) -> int:
        """Get the order value for a severity level (for sorting)."""
        severity_order = {"critical": 0, "major": 1, "minor": 2, "suggestion": 3}
        return severity_order.get(severity.lower(), 4)

    def _get_severity_emoji(self, severity: str) -> str:
        """Get an emoji for a severity level."""
        severity_emoji = {"critical": "🔴", "major": "🟠", "minor": "🟡", "suggestion": "💡"}
        return severity_emoji.get(severity.lower(), "📝")


def generate_comments(issues: list[dict[str, Any]], config: dict[str, Any] | None = None) -> dict[str, Any]:
    """Generate comments for a list of issues."""
    generator = CommentGenerator(config)
    return generator.generate_comments(issues)


if __name__ == "__main__":
    # Example usage
    import sys

    if len(sys.argv) < 2:
        sys.exit(1)

    issues_file = sys.argv[1]

    try:
        with open(issues_file) as f:
            issues = json.load(f)

        # Generate comments
        comments = generate_comments(issues)

        # Print summary comment
        for _comment in comments["file_comments"]:
            pass

    except Exception:
        sys.exit(1)
