#!/usr/bin/env python3
"""
Unit tests for the code analyzer module.
"""

import os
import sys
from unittest.mock import AsyncMock, Mock, patch

import pytest

# Add parent directory to path for imports
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "scripts"))

from code_analyzer import AnalysisResult, CodeAnalyzer, IssueSeverity, IssueType


class TestCodeAnalyzer:
    """Test cases for CodeAnalyzer."""

    @pytest.fixture
    def mock_config(self):
        """Mock configuration for testing."""
        return {
            "rules_config": {
                "style_checks": True,
                "security_checks": True,
                "performance_checks": True,
                "best_practices": True
            },
            "language_configs": {
                "python": {
                    "max_line_length": 88,
                    "max_complexity": 10
                },
                "javascript": {
                    "max_line_length": 100,
                    "max_complexity": 15
                }
            },
            "github_token": "test-token"
        }

    @pytest.fixture
    def code_analyzer(self, mock_config):
        """Create a CodeAnalyzer instance for testing."""
        return CodeAnalyzer(mock_config)

    @pytest.fixture
    def sample_python_code(self):
        """Sample Python code for testing."""
        return """
def calculate_total(items):
    total = 0
    for item in items:
        if item.price > 0:
            total += item.price * item.quantity
    return total

def process_user_input(input_string):
    # Potential security issue: using eval
    result = eval(input_string)
    return result

def inefficient_search(large_list, target):
    # Performance issue: O(n) search when sorted list could use binary search
    for item in large_list:
        if item == target:
            return True
    return False
"""

    @pytest.fixture
    def sample_javascript_code(self):
        """Sample JavaScript code for testing."""
        return """
function fetchUserData(userId) {
    // Missing error handling
    return fetch('/api/users/' + userId)
        .then(response => response.json());
}

function processPayment(amount, cardNumber) {
    // Security issue: logging sensitive data
    console.log('Processing payment for card: ' + cardNumber);

    // SQL injection vulnerability
    const query = "SELECT * FROM payments WHERE card = '" + cardNumber + "'";
    return db.query(query);
}

// Unused variable
const unusedConfig = {
    apiKey: 'hardcoded-api-key'  // Security issue: hardcoded credentials
};
"""

    @pytest.mark.asyncio
    async def test_analyze_code_python(self, code_analyzer, sample_python_code):
        """Test analyzing Python code."""
        result = await code_analyzer.analyze_code(
            sample_python_code,
            "example.py",
            "python"
        )

        assert isinstance(result, AnalysisResult)
        assert result.file_path == "example.py"
        assert result.language == "python"
        assert len(result.issues) > 0

        # Check for security issue (eval usage)
        security_issues = [i for i in result.issues if i.type == IssueType.SECURITY]
        assert len(security_issues) > 0
        assert any("eval" in issue.message.lower() for issue in security_issues)

    @pytest.mark.asyncio
    async def test_analyze_code_javascript(self, code_analyzer, sample_javascript_code):
        """Test analyzing JavaScript code."""
        result = await code_analyzer.analyze_code(
            sample_javascript_code,
            "example.js",
            "javascript"
        )

        assert isinstance(result, AnalysisResult)
        assert result.file_path == "example.js"
        assert result.language == "javascript"
        assert len(result.issues) > 0

        # Check for various issue types
        issue_types = {issue.type for issue in result.issues}
        assert IssueType.SECURITY in issue_types  # SQL injection, hardcoded credentials
        assert IssueType.BUG in issue_types  # Missing error handling

    @pytest.mark.asyncio
    async def test_analyze_diff_added_lines(self, code_analyzer):
        """Test analyzing diff with added lines."""
        diff_content = """
@@ -10,6 +10,10 @@ def process_data(data):
     result = []
     for item in data:
         result.append(item.upper())
+
+    # New code with issue
+    password = "hardcoded123"  # Security issue
+    eval(user_input)  # Another security issue

     return result
"""

        with patch.object(code_analyzer, "_get_file_content", return_value="mock content"):
            result = await code_analyzer.analyze_diff(
                diff_content,
                "test.py",
                "python"
            )

        assert len(result.issues) > 0

        # Check that issues are only for added lines
        added_line_numbers = {15, 16}  # Based on the diff
        for issue in result.issues:
            if issue.line:
                assert issue.line in added_line_numbers

    def test_detect_style_issues_long_line(self, code_analyzer):
        """Test detection of long lines."""
        code_lines = [
            "short line",
            "x" * 150,  # Very long line
            "another short line"
        ]

        issues = code_analyzer._detect_style_issues(code_lines, "python")

        long_line_issues = [i for i in issues if "long" in i.message.lower()]
        assert len(long_line_issues) > 0
        assert long_line_issues[0].line == 2

    def test_detect_style_issues_trailing_whitespace(self, code_analyzer):
        """Test detection of trailing whitespace."""
        code_lines = [
            "clean line",
            "line with trailing space ",
            "line with trailing tab\t"
        ]

        issues = code_analyzer._detect_style_issues(code_lines, "python")

        whitespace_issues = [i for i in issues if "whitespace" in i.message.lower()]
        assert len(whitespace_issues) >= 2

    def test_detect_security_issues_eval(self, code_analyzer):
        """Test detection of eval usage."""
        code = """
result = eval(user_input)
safe_result = int(user_input)
"""

        issues = code_analyzer._detect_security_issues(code, "python")

        eval_issues = [i for i in issues if "eval" in i.message]
        assert len(eval_issues) > 0
        assert eval_issues[0].severity == IssueSeverity.CRITICAL

    def test_detect_security_issues_sql_injection(self, code_analyzer):
        """Test detection of SQL injection vulnerabilities."""
        code = """
query = "SELECT * FROM users WHERE id = '" + user_id + "'"
safe_query = "SELECT * FROM users WHERE id = ?"
"""

        issues = code_analyzer._detect_security_issues(code, "javascript")

        sql_issues = [i for i in issues if "SQL" in i.message]
        assert len(sql_issues) > 0
        assert sql_issues[0].severity == IssueSeverity.CRITICAL

    def test_detect_security_issues_hardcoded_secrets(self, code_analyzer):
        """Test detection of hardcoded secrets."""
        code = """
api_key = "sk-1234567890abcdef"
password = "admin123"
db_password = os.environ.get('DB_PASSWORD')
"""

        issues = code_analyzer._detect_security_issues(code, "python")

        secret_issues = [i for i in issues if "hardcoded" in i.message.lower() or "password" in i.message.lower()]
        assert len(secret_issues) >= 2

    def test_detect_bug_patterns_missing_error_handling(self, code_analyzer):
        """Test detection of missing error handling."""
        code = """
def risky_function():
    file = open('data.txt', 'r')
    data = file.read()
    file.close()
    return data

def safe_function():
    try:
        with open('data.txt', 'r') as file:
            return file.read()
    except IOError:
        return None
"""

        issues = code_analyzer._detect_bug_patterns(code, "python")

        error_handling_issues = [i for i in issues if "error handling" in i.message.lower() or "exception" in i.message.lower()]
        assert len(error_handling_issues) > 0

    def test_detect_performance_issues_inefficient_loops(self, code_analyzer):
        """Test detection of inefficient loops."""
        code = """
# Inefficient: repeated list concatenation
result = []
for item in large_list:
    result = result + [process(item)]

# Inefficient: multiple iterations
total = sum([x for x in numbers])
count = len([x for x in numbers])
average = total / count
"""

        issues = code_analyzer._detect_performance_issues(code, "python")

        assert len(issues) > 0
        performance_keywords = ["inefficient", "performance", "optimize"]
        assert any(
            any(keyword in issue.message.lower() for keyword in performance_keywords)
            for issue in issues
        )

    def test_calculate_complexity_simple_function(self, code_analyzer):
        """Test complexity calculation for simple function."""
        code = """
def simple_function(x):
    return x * 2
"""

        complexity = code_analyzer._calculate_complexity(code, "python")
        assert complexity == 1  # Simple function has complexity 1

    def test_calculate_complexity_complex_function(self, code_analyzer):
        """Test complexity calculation for complex function."""
        code = """
def complex_function(data):
    result = []
    for item in data:  # +1
        if item > 0:  # +1
            if item % 2 == 0:  # +1
                result.append(item)
            elif item % 3 == 0:  # +1
                result.append(item * 2)
            else:  # +0 (else doesn't add complexity)
                result.append(item * 3)
        else:  # +0
            for i in range(item):  # +1
                if i > 5:  # +1
                    break
    return result
"""

        complexity = code_analyzer._calculate_complexity(code, "python")
        assert complexity >= 6  # Multiple branches and loops

    def test_suggest_fix_long_line(self, code_analyzer):
        """Test fix suggestion for long lines."""
        issue = type("Issue", (), {
            "type": IssueType.STYLE,
            "message": "Line too long (150 > 88 characters)",
            "line": 1
        })()

        original_line = "very_long_function_call(parameter1, parameter2, parameter3, parameter4, parameter5, parameter6)"

        fix = code_analyzer._suggest_fix(issue, original_line, "python")

        assert fix is not None
        assert "very_long_function_call(\n" in fix
        assert len(fix.split("\n")) > 1  # Should be multi-line

    def test_suggest_fix_eval_usage(self, code_analyzer):
        """Test fix suggestion for eval usage."""
        issue = type("Issue", (), {
            "type": IssueType.SECURITY,
            "message": "Use of eval() is a security risk",
            "line": 1
        })()

        original_line = "result = eval(user_input)"

        fix = code_analyzer._suggest_fix(issue, original_line, "python")

        assert fix is not None
        assert "eval" not in fix
        assert "ast.literal_eval" in fix or "json.loads" in fix

    @pytest.mark.asyncio
    async def test_get_file_content_from_github(self, code_analyzer):
        """Test fetching file content from GitHub."""
        mock_response = Mock()
        mock_response.status = 200
        mock_response.json = AsyncMock(return_value={
            "content": "ZGVmIGZvbygpOgogICAgcGFzcw==",  # Base64 encoded "def foo():\n    pass"
            "encoding": "base64"
        })

        with patch("aiohttp.ClientSession.get") as mock_get:
            mock_get.return_value.__aenter__.return_value = mock_response

            result = await code_analyzer.get_file_content_from_github(
                "owner", "repo", "path/to/file.py"
            )

            assert result == "def foo():\n    pass"
