"""
Security-focused analyzer strategies.

This module implements various security analysis strategies
for detecting vulnerabilities and security issues in code.
"""

import re

from ..core.interfaces import CodeIssue, IssueLevel
from .base_analyzer import AnalysisStrategy, AnalyzerContext


class SecurityPatternStrategy(AnalysisStrategy):
    """Strategy for detecting security issues using pattern matching."""

    def __init__(self):
        self.patterns = self._build_security_patterns()

    def _build_security_patterns(self) -> dict[str, dict]:
        """Build security patterns for different issues."""
        return {
            "hardcoded_password": {
                "pattern": re.compile(
                    r'(?i)(password|passwd|pwd|secret|api_key|apikey|token|auth)\s*=\s*["\'][^"\']+["\']', re.IGNORECASE
                ),
                "level": IssueLevel.CRITICAL,
                "message": "Hardcoded credential detected",
                "suggestion": "Use environment variables or secure credential storage",
            },
            "sql_injection": {
                "pattern": re.compile(
                    r'(?i)(query|execute)\s*\(\s*["\'].*?\+.*?["\']|'
                    r'f["\'].*?{.*?}.*?(?:SELECT|INSERT|UPDATE|DELETE)',
                    re.IGNORECASE | re.DOTALL,
                ),
                "level": IssueLevel.CRITICAL,
                "message": "Potential SQL injection vulnerability",
                "suggestion": "Use parameterized queries or prepared statements",
            },
            "command_injection": {
                "pattern": re.compile(
                    r"(?:os\.system|subprocess\.call|subprocess\.run|exec|eval)\s*\([^)]*\+[^)]*\)", re.IGNORECASE
                ),
                "level": IssueLevel.CRITICAL,
                "message": "Potential command injection vulnerability",
                "suggestion": "Validate and sanitize user input, use subprocess with arrays",
            },
            "weak_crypto": {
                "pattern": re.compile(r"(?i)(?:md5|sha1|des|rc4)\s*\(", re.IGNORECASE),
                "level": IssueLevel.ERROR,
                "message": "Weak cryptographic algorithm detected",
                "suggestion": "Use SHA-256 or stronger algorithms",
            },
            "insecure_random": {
                "pattern": re.compile(r"(?:random\.random|random\.randint|Math\.random)", re.IGNORECASE),
                "level": IssueLevel.WARNING,
                "message": "Insecure random number generation",
                "suggestion": "Use cryptographically secure random functions",
            },
            "debug_enabled": {
                "pattern": re.compile(r"(?i)(?:debug\s*=\s*true|DEBUG\s*=\s*True)", re.IGNORECASE),
                "level": IssueLevel.WARNING,
                "message": "Debug mode enabled",
                "suggestion": "Disable debug mode in production",
            },
            "unsafe_yaml": {
                "pattern": re.compile(r"yaml\.load\s*\([^,)]+\)", re.IGNORECASE),
                "level": IssueLevel.ERROR,
                "message": "Unsafe YAML loading",
                "suggestion": "Use yaml.safe_load() instead",
            },
            "jwt_none_algorithm": {
                "pattern": re.compile(r'jwt\.decode\s*\([^)]+algorithms\s*=\s*\[[^\]]*["\']none["\']', re.IGNORECASE),
                "level": IssueLevel.CRITICAL,
                "message": "JWT none algorithm vulnerability",
                "suggestion": 'Never allow "none" algorithm in JWT validation',
            },
        }

    async def analyze(self, context: AnalyzerContext) -> list[CodeIssue]:
        """Analyze code for security patterns."""
        issues = []
        lines = context.content.splitlines()

        for line_num, line in enumerate(lines, 1):
            # Skip comments (basic check)
            stripped = line.strip()
            if self._is_comment(stripped, context.language):
                continue

            # Check each security pattern
            for pattern_name, pattern_info in self.patterns.items():
                if pattern_info["pattern"].search(line):
                    # Additional validation to reduce false positives
                    if self._is_false_positive(pattern_name, line, context):
                        continue

                    issues.append(
                        CodeIssue(
                            level=pattern_info["level"],
                            category="security",
                            message=pattern_info["message"],
                            file_path=context.file_path,
                            line_number=line_num,
                            column_number=line.index(line.strip()) + 1,
                            suggestion=pattern_info["suggestion"],
                            rule_id=f"security-{pattern_name}",
                        )
                    )

        return issues

    def _is_comment(self, line: str, language: str) -> bool:
        """Check if line is a comment."""
        comment_prefixes = {
            "python": ["#"],
            "javascript": ["//", "/*", "*"],
            "java": ["//", "/*", "*"],
            "c": ["//", "/*", "*"],
            "cpp": ["//", "/*", "*"],
            "go": ["//", "/*", "*"],
            "rust": ["//", "/*", "*"],
            "ruby": ["#"],
            "bash": ["#"],
            "yaml": ["#"],
            "sql": ["--", "/*"],
        }

        prefixes = comment_prefixes.get(language, [])
        return any(line.startswith(prefix) for prefix in prefixes)

    def _is_false_positive(self, pattern_name: str, line: str, context: AnalyzerContext) -> bool:
        """Check if match is likely a false positive."""
        # Example: Skip test files for certain patterns
        if "test" in context.file_path.lower() and pattern_name == "hardcoded_password":
            return True

        # Example: Skip example/demo code
        return bool(any(x in line.lower() for x in ["example", "demo", "sample"]))

    def get_name(self) -> str:
        return "security-pattern"

    def get_supported_languages(self) -> set[str]:
        return {"python", "javascript", "java", "go", "ruby", "php"}


class DependencySecurityStrategy(AnalysisStrategy):
    """Strategy for checking dependency security issues."""

    def __init__(self):
        self.vulnerable_packages = self._load_vulnerable_packages()

    def _load_vulnerable_packages(self) -> dict[str, list[dict]]:
        """Load known vulnerable packages."""
        # In production, this would fetch from a vulnerability database
        return {
            "python": [
                {
                    "package": "requests",
                    "vulnerable_versions": ["<2.20.0"],
                    "severity": "high",
                    "cve": "CVE-2018-18074",
                },
                {
                    "package": "django",
                    "vulnerable_versions": ["<2.2.24", ">=3.0,<3.1.12", ">=3.2,<3.2.4"],
                    "severity": "critical",
                    "cve": "CVE-2021-33203",
                },
            ],
            "javascript": [
                {"package": "lodash", "vulnerable_versions": ["<4.17.21"], "severity": "high", "cve": "CVE-2021-23337"},
                {"package": "axios", "vulnerable_versions": ["<0.21.1"], "severity": "medium", "cve": "CVE-2020-28168"},
            ],
        }

    async def analyze(self, context: AnalyzerContext) -> list[CodeIssue]:
        """Check for vulnerable dependencies."""
        issues = []

        # Check different dependency files
        dependency_files = {
            "requirements.txt": self._check_python_requirements,
            "package.json": self._check_npm_packages,
            "Gemfile": self._check_ruby_gems,
            "go.mod": self._check_go_modules,
            "pom.xml": self._check_maven_dependencies,
            "build.gradle": self._check_gradle_dependencies,
        }

        file_name = context.file_path.split("/")[-1]

        if file_name in dependency_files:
            checker = dependency_files[file_name]
            issues = await checker(context)

        return issues

    async def _check_python_requirements(self, context: AnalyzerContext) -> list[CodeIssue]:
        """Check Python requirements.txt for vulnerabilities."""
        issues = []
        lines = context.content.splitlines()

        for line_num, line in enumerate(lines, 1):
            line = line.strip()
            if not line or line.startswith("#"):
                continue

            # Parse package and version
            match = re.match(r"([a-zA-Z0-9-_]+)\s*([=<>]+)\s*([0-9.]+)", line)
            if match:
                package = match.group(1).lower()
                operator = match.group(2)
                version = match.group(3)

                # Check against vulnerable packages
                for vuln in self.vulnerable_packages.get("python", []):
                    if vuln["package"] == package:
                        # Simplified version check - in production use proper version comparison
                        issues.append(
                            CodeIssue(
                                level=self._severity_to_level(vuln["severity"]),
                                category="security",
                                message=f"Vulnerable package: {package} {operator} {version} ({vuln['cve']})",
                                file_path=context.file_path,
                                line_number=line_num,
                                suggestion=f"Update {package} to latest secure version",
                                rule_id=f"dependency-{vuln['cve']}",
                            )
                        )

        return issues

    async def _check_npm_packages(self, context: AnalyzerContext) -> list[CodeIssue]:
        """Check package.json for vulnerabilities."""
        # Implementation would parse JSON and check dependencies
        return []

    async def _check_ruby_gems(self, context: AnalyzerContext) -> list[CodeIssue]:
        """Check Gemfile for vulnerabilities."""
        return []

    async def _check_go_modules(self, context: AnalyzerContext) -> list[CodeIssue]:
        """Check go.mod for vulnerabilities."""
        return []

    async def _check_maven_dependencies(self, context: AnalyzerContext) -> list[CodeIssue]:
        """Check pom.xml for vulnerabilities."""
        return []

    async def _check_gradle_dependencies(self, context: AnalyzerContext) -> list[CodeIssue]:
        """Check build.gradle for vulnerabilities."""
        return []

    def _severity_to_level(self, severity: str) -> IssueLevel:
        """Convert severity string to IssueLevel."""
        mapping = {
            "critical": IssueLevel.CRITICAL,
            "high": IssueLevel.ERROR,
            "medium": IssueLevel.WARNING,
            "low": IssueLevel.INFO,
        }
        return mapping.get(severity.lower(), IssueLevel.WARNING)

    def get_name(self) -> str:
        return "dependency-security"

    def get_supported_languages(self) -> set[str]:
        # This strategy works on dependency files, not languages
        return set()


class InputValidationStrategy(AnalysisStrategy):
    """Strategy for checking input validation issues."""

    async def analyze(self, context: AnalyzerContext) -> list[CodeIssue]:
        """Check for input validation issues."""
        issues = []

        # Language-specific checks
        if context.language == "python":
            issues.extend(await self._check_python_input_validation(context))
        elif context.language in ["javascript", "typescript"]:
            issues.extend(await self._check_javascript_input_validation(context))

        return issues

    async def _check_python_input_validation(self, context: AnalyzerContext) -> list[CodeIssue]:
        """Check Python code for input validation issues."""
        issues = []
        lines = context.content.splitlines()

        patterns = [
            {
                "pattern": re.compile(r"input\s*\([^)]*\)"),
                "message": "User input without validation",
                "suggestion": "Validate and sanitize user input",
            },
            {
                "pattern": re.compile(r"request\.(?:args|form|json)\s*\.\s*get\s*\([^)]+\)(?!\s*\.)"),
                "message": "Web request parameter without validation",
                "suggestion": "Validate request parameters before use",
            },
            {
                "pattern": re.compile(r"pickle\.loads?\s*\("),
                "message": "Unsafe deserialization with pickle",
                "suggestion": "Avoid pickle for untrusted data, use JSON instead",
            },
        ]

        for line_num, line in enumerate(lines, 1):
            for pattern_info in patterns:
                if pattern_info["pattern"].search(line):
                    issues.append(
                        CodeIssue(
                            level=IssueLevel.WARNING,
                            category="security",
                            message=pattern_info["message"],
                            file_path=context.file_path,
                            line_number=line_num,
                            suggestion=pattern_info["suggestion"],
                            rule_id="input-validation",
                        )
                    )

        return issues

    async def _check_javascript_input_validation(self, context: AnalyzerContext) -> list[CodeIssue]:
        """Check JavaScript code for input validation issues."""
        # Implementation for JavaScript
        return []

    def get_name(self) -> str:
        return "input-validation"

    def get_supported_languages(self) -> set[str]:
        return {"python", "javascript", "typescript", "java", "php"}
