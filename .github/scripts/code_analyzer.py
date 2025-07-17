#!/usr/bin/env python3
"""
Code analyzer for Kiro GitHub integration.
This module analyzes code changes in pull requests to identify issues and suggest improvements.
"""

import os
import re
import json
import logging
import subprocess
import tempfile
from pathlib import Path
from enum import Enum
from typing import Dict, List, Any, Optional, Tuple

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler('kiro_code_analyzer.log')
    ]
)
logger = logging.getLogger('kiro_code_analyzer')

# Constants
KIRO_API_URL = os.environ.get('KIRO_API_URL', 'https://api.kiro.ai')

class IssueSeverity(Enum):
    """Enum for issue severity levels."""
    CRITICAL = "critical"
    MAJOR = "major"
    MINOR = "minor"
    SUGGESTION = "suggestion"

class IssueCategory(Enum):
    """Enum for issue categories."""
    SYNTAX = "syntax"
    STYLE = "style"
    SECURITY = "security"
    PERFORMANCE = "performance"
    MAINTAINABILITY = "maintainability"
    DOCUMENTATION = "documentation"
    BEST_PRACTICE = "best_practice"

class CodeIssue:
    """Represents an issue found in code."""
    
    def __init__(
        self,
        file_path: str,
        line_start: int,
        line_end: Optional[int] = None,
        message: str = "",
        severity: IssueSeverity = IssueSeverity.SUGGESTION,
        category: IssueCategory = IssueCategory.BEST_PRACTICE,
        code_snippet: str = "",
        fix_suggestion: Optional[str] = None,
        rule_id: Optional[str] = None,
        references: Optional[List[str]] = None
    ):
        """Initialize a code issue."""
        self.file_path = file_path
        self.line_start = line_start
        self.line_end = line_end or line_start
        self.message = message
        self.severity = severity
        self.category = category
        self.code_snippet = code_snippet
        self.fix_suggestion = fix_suggestion
        self.rule_id = rule_id
        self.references = references or []
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert the issue to a dictionary."""
        return {
            'file_path': self.file_path,
            'line_start': self.line_start,
            'line_end': self.line_end,
            'message': self.message,
            'severity': self.severity.value,
            'category': self.category.value,
            'code_snippet': self.code_snippet,
            'fix_suggestion': self.fix_suggestion,
            'rule_id': self.rule_id,
            'references': self.references
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'CodeIssue':
        """Create an issue from a dictionary."""
        return cls(
            file_path=data['file_path'],
            line_start=data['line_start'],
            line_end=data.get('line_end', data['line_start']),
            message=data['message'],
            severity=IssueSeverity(data['severity']),
            category=IssueCategory(data['category']),
            code_snippet=data.get('code_snippet', ''),
            fix_suggestion=data.get('fix_suggestion'),
            rule_id=data.get('rule_id'),
            references=data.get('references', [])
        )

class CodeAnalyzer:
    """Analyzes code changes in pull requests."""
    
    def __init__(self, config: Optional[Dict[str, Any]] = None):
        """Initialize the code analyzer."""
        self.config = config or {}
        self.issues = []
        self.temp_dir = None
    
    def analyze_pr(self, pr_data: Dict[str, Any], files: List[Dict[str, Any]], diff: str) -> List[CodeIssue]:
        """Analyze a pull request and return a list of issues."""
        logger.info(f"Analyzing PR #{pr_data['number']} in {pr_data['repository']['full_name']}")
        
        # Reset issues
        self.issues = []
        
        # Create a temporary directory for analysis
        with tempfile.TemporaryDirectory() as temp_dir:
            self.temp_dir = temp_dir
            
            # Write files to temporary directory
            self._write_files_to_temp_dir(files)
            
            # Run different types of analysis
            self._run_syntax_analysis(files)
            self._run_style_analysis(files)
            self._run_security_analysis(files)
            self._run_performance_analysis(files)
            
            # Clean up
            self.temp_dir = None
        
        logger.info(f"Found {len(self.issues)} issues in PR #{pr_data['number']}")
        return self.issues
    
    def _write_files_to_temp_dir(self, files: List[Dict[str, Any]]) -> None:
        """Write files to a temporary directory for analysis."""
        for file in files:
            file_path = file.get('filename')
            if not file_path:
                continue
            
            # Skip deleted files
            if file.get('status') == 'removed':
                continue
            
            # Create directory structure
            full_path = os.path.join(self.temp_dir, file_path)
            os.makedirs(os.path.dirname(full_path), exist_ok=True)
            
            # Write file content
            content = file.get('content', '')
            if not content and 'raw_url' in file:
                try:
                    import requests
                    response = requests.get(file['raw_url'])
                    content = response.text
                except Exception as e:
                    logger.warning(f"Error fetching file content: {str(e)}")
            
            with open(full_path, 'w') as f:
                f.write(content)
    
    def _run_syntax_analysis(self, files: List[Dict[str, Any]]) -> None:
        """Run syntax analysis on files."""
        logger.info("Running syntax analysis")
        
        for file in files:
            file_path = file.get('filename')
            if not file_path:
                continue
            
            # Skip deleted files
            if file.get('status') == 'removed':
                continue
            
            # Determine file type
            file_ext = os.path.splitext(file_path)[1].lower()
            
            # Run appropriate syntax checker based on file type
            if file_ext in ['.py']:
                self._check_python_syntax(file_path)
            elif file_ext in ['.js', '.jsx', '.ts', '.tsx']:
                self._check_javascript_syntax(file_path)
            elif file_ext in ['.java']:
                self._check_java_syntax(file_path)
            elif file_ext in ['.go']:
                self._check_go_syntax(file_path)
            elif file_ext in ['.rb']:
                self._check_ruby_syntax(file_path)
    
    def _check_python_syntax(self, file_path: str) -> None:
        """Check Python syntax using pyflakes."""
        full_path = os.path.join(self.temp_dir, file_path)
        
        try:
            # Use Python's built-in compile function to check syntax
            with open(full_path, 'r') as f:
                source = f.read()
            
            try:
                compile(source, file_path, 'exec')
            except SyntaxError as e:
                # Add syntax error to issues
                self.issues.append(CodeIssue(
                    file_path=file_path,
                    line_start=e.lineno,
                    message=f"Python syntax error: {e.msg}",
                    severity=IssueSeverity.CRITICAL,
                    category=IssueCategory.SYNTAX,
                    code_snippet=e.text.strip() if e.text else "",
                    rule_id="python-syntax"
                ))
            
            # Try to run pyflakes if available
            try:
                result = subprocess.run(
                    ['pyflakes', full_path],
                    capture_output=True,
                    text=True
                )
                
                if result.returncode != 0:
                    # Parse pyflakes output
                    for line in result.stdout.splitlines():
                        match = re.match(r'(.+):(\d+):(\d+): (.+)', line)
                        if match:
                            _, lineno, _, message = match.groups()
                            self.issues.append(CodeIssue(
                                file_path=file_path,
                                line_start=int(lineno),
                                message=f"Python issue: {message}",
                                severity=IssueSeverity.MAJOR,
                                category=IssueCategory.SYNTAX,
                                rule_id="python-pyflakes"
                            ))
            except FileNotFoundError:
                # pyflakes not installed, skip
                pass
        
        except Exception as e:
            logger.warning(f"Error checking Python syntax for {file_path}: {str(e)}")
    
    def _check_javascript_syntax(self, file_path: str) -> None:
        """Check JavaScript/TypeScript syntax using ESLint if available."""
        full_path = os.path.join(self.temp_dir, file_path)
        
        try:
            # Try to run ESLint if available
            try:
                result = subprocess.run(
                    ['eslint', '--format', 'json', full_path],
                    capture_output=True,
                    text=True
                )
                
                # Parse ESLint JSON output
                try:
                    eslint_results = json.loads(result.stdout)
                    for file_result in eslint_results:
                        for message in file_result.get('messages', []):
                            severity_map = {
                                2: IssueSeverity.MAJOR,  # Error
                                1: IssueSeverity.MINOR,  # Warning
                                0: IssueSeverity.SUGGESTION  # Info
                            }
                            
                            self.issues.append(CodeIssue(
                                file_path=file_path,
                                line_start=message.get('line', 1),
                                line_end=message.get('endLine', message.get('line', 1)),
                                message=f"JavaScript/TypeScript issue: {message.get('message')}",
                                severity=severity_map.get(message.get('severity', 1), IssueSeverity.MINOR),
                                category=IssueCategory.SYNTAX,
                                rule_id=message.get('ruleId', 'js-syntax')
                            ))
                except json.JSONDecodeError:
                    # ESLint didn't output valid JSON
                    pass
            
            except FileNotFoundError:
                # ESLint not installed, skip
                pass
        
        except Exception as e:
            logger.warning(f"Error checking JavaScript/TypeScript syntax for {file_path}: {str(e)}")
    
    def _check_java_syntax(self, file_path: str) -> None:
        """Check Java syntax using javac if available."""
        full_path = os.path.join(self.temp_dir, file_path)
        
        try:
            # Try to run javac if available
            try:
                result = subprocess.run(
                    ['javac', '-Xlint:all', full_path],
                    capture_output=True,
                    text=True
                )
                
                if result.returncode != 0:
                    # Parse javac error output
                    for line in result.stderr.splitlines():
                        match = re.match(r'(.+):(\d+): error: (.+)', line)
                        if match:
                            _, lineno, message = match.groups()
                            self.issues.append(CodeIssue(
                                file_path=file_path,
                                line_start=int(lineno),
                                message=f"Java syntax error: {message}",
                                severity=IssueSeverity.CRITICAL,
                                category=IssueCategory.SYNTAX,
                                rule_id="java-syntax"
                            ))
                        
                        match = re.match(r'(.+):(\d+): warning: (.+)', line)
                        if match:
                            _, lineno, message = match.groups()
                            self.issues.append(CodeIssue(
                                file_path=file_path,
                                line_start=int(lineno),
                                message=f"Java warning: {message}",
                                severity=IssueSeverity.MINOR,
                                category=IssueCategory.SYNTAX,
                                rule_id="java-lint"
                            ))
            
            except FileNotFoundError:
                # javac not installed, skip
                pass
        
        except Exception as e:
            logger.warning(f"Error checking Java syntax for {file_path}: {str(e)}")
    
    def _check_go_syntax(self, file_path: str) -> None:
        """Check Go syntax using go vet if available."""
        full_path = os.path.join(self.temp_dir, file_path)
        
        try:
            # Try to run go vet if available
            try:
                result = subprocess.run(
                    ['go', 'vet', full_path],
                    capture_output=True,
                    text=True
                )
                
                if result.returncode != 0:
                    # Parse go vet output
                    for line in result.stderr.splitlines():
                        match = re.match(r'(.+):(\d+):(\d+): (.+)', line)
                        if match:
                            _, lineno, _, message = match.groups()
                            self.issues.append(CodeIssue(
                                file_path=file_path,
                                line_start=int(lineno),
                                message=f"Go issue: {message}",
                                severity=IssueSeverity.MAJOR,
                                category=IssueCategory.SYNTAX,
                                rule_id="go-vet"
                            ))
            
            except FileNotFoundError:
                # go not installed, skip
                pass
        
        except Exception as e:
            logger.warning(f"Error checking Go syntax for {file_path}: {str(e)}")
    
    def _check_ruby_syntax(self, file_path: str) -> None:
        """Check Ruby syntax using ruby -c if available."""
        full_path = os.path.join(self.temp_dir, file_path)
        
        try:
            # Try to run ruby -c if available
            try:
                result = subprocess.run(
                    ['ruby', '-c', full_path],
                    capture_output=True,
                    text=True
                )
                
                if result.returncode != 0:
                    # Parse ruby -c output
                    for line in result.stderr.splitlines():
                        match = re.match(r'(.+):(\d+): (.+)', line)
                        if match:
                            _, lineno, message = match.groups()
                            self.issues.append(CodeIssue(
                                file_path=file_path,
                                line_start=int(lineno),
                                message=f"Ruby syntax error: {message}",
                                severity=IssueSeverity.CRITICAL,
                                category=IssueCategory.SYNTAX,
                                rule_id="ruby-syntax"
                            ))
            
            except FileNotFoundError:
                # ruby not installed, skip
                pass
        
        except Exception as e:
            logger.warning(f"Error checking Ruby syntax for {file_path}: {str(e)}")
    
    def _run_style_analysis(self, files: List[Dict[str, Any]]) -> None:
        """Run style analysis on files."""
        logger.info("Running style analysis")
        
        for file in files:
            file_path = file.get('filename')
            if not file_path:
                continue
            
            # Skip deleted files
            if file.get('status') == 'removed':
                continue
            
            # Determine file type
            file_ext = os.path.splitext(file_path)[1].lower()
            
            # Run appropriate style checker based on file type
            if file_ext in ['.py']:
                self._check_python_style(file_path)
            elif file_ext in ['.js', '.jsx', '.ts', '.tsx']:
                self._check_javascript_style(file_path)
            elif file_ext in ['.java']:
                self._check_java_style(file_path)
    
    def _check_python_style(self, file_path: str) -> None:
        """Check Python style using flake8 if available."""
        full_path = os.path.join(self.temp_dir, file_path)
        
        try:
            # Try to run flake8 if available
            try:
                result = subprocess.run(
                    ['flake8', '--format', 'default', full_path],
                    capture_output=True,
                    text=True
                )
                
                # Parse flake8 output
                for line in result.stdout.splitlines():
                    match = re.match(r'(.+):(\d+):(\d+): ([A-Z]\d+) (.+)', line)
                    if match:
                        _, lineno, _, code, message = match.groups()
                        
                        # Determine severity based on code
                        severity = IssueSeverity.SUGGESTION
                        if code.startswith('E'):
                            severity = IssueSeverity.MINOR
                        elif code.startswith('F'):
                            severity = IssueSeverity.MAJOR
                        
                        self.issues.append(CodeIssue(
                            file_path=file_path,
                            line_start=int(lineno),
                            message=f"Python style issue: {message}",
                            severity=severity,
                            category=IssueCategory.STYLE,
                            rule_id=f"flake8-{code}"
                        ))
            
            except FileNotFoundError:
                # flake8 not installed, skip
                pass
        
        except Exception as e:
            logger.warning(f"Error checking Python style for {file_path}: {str(e)}")
    
    def _check_javascript_style(self, file_path: str) -> None:
        """Check JavaScript/TypeScript style using prettier if available."""
        full_path = os.path.join(self.temp_dir, file_path)
        
        try:
            # Try to run prettier if available
            try:
                result = subprocess.run(
                    ['prettier', '--check', full_path],
                    capture_output=True,
                    text=True
                )
                
                if result.returncode != 0:
                    # Prettier found style issues
                    self.issues.append(CodeIssue(
                        file_path=file_path,
                        line_start=1,
                        message="JavaScript/TypeScript style issues: Code doesn't match prettier formatting",
                        severity=IssueSeverity.MINOR,
                        category=IssueCategory.STYLE,
                        rule_id="prettier-format"
                    ))
            
            except FileNotFoundError:
                # prettier not installed, skip
                pass
        
        except Exception as e:
            logger.warning(f"Error checking JavaScript/TypeScript style for {file_path}: {str(e)}")
    
    def _check_java_style(self, file_path: str) -> None:
        """Check Java style using checkstyle if available."""
        full_path = os.path.join(self.temp_dir, file_path)
        
        try:
            # Try to run checkstyle if available
            try:
                result = subprocess.run(
                    ['checkstyle', '-f', 'plain', full_path],
                    capture_output=True,
                    text=True
                )
                
                # Parse checkstyle output
                for line in result.stdout.splitlines():
                    match = re.match(r'\[(\w+)\] (.+):(\d+):(\d+): (.+)', line)
                    if match:
                        level, _, lineno, _, message = match.groups()
                        
                        # Determine severity based on level
                        severity = IssueSeverity.SUGGESTION
                        if level == 'ERROR':
                            severity = IssueSeverity.MINOR
                        elif level == 'WARNING':
                            severity = IssueSeverity.SUGGESTION
                        
                        self.issues.append(CodeIssue(
                            file_path=file_path,
                            line_start=int(lineno),
                            message=f"Java style issue: {message}",
                            severity=severity,
                            category=IssueCategory.STYLE,
                            rule_id="checkstyle"
                        ))
            
            except FileNotFoundError:
                # checkstyle not installed, skip
                pass
        
        except Exception as e:
            logger.warning(f"Error checking Java style for {file_path}: {str(e)}")
    
    def _run_security_analysis(self, files: List[Dict[str, Any]]) -> None:
        """Run security analysis on files."""
        logger.info("Running security analysis")
        
        # Try to run bandit for Python security if available
        try:
            python_files = [
                os.path.join(self.temp_dir, file.get('filename'))
                for file in files
                if file.get('filename', '').endswith('.py') and file.get('status') != 'removed'
            ]
            
            if python_files:
                try:
                    result = subprocess.run(
                        ['bandit', '-f', 'json', '-r'] + python_files,
                        capture_output=True,
                        text=True
                    )
                    
                    # Parse bandit JSON output
                    try:
                        bandit_results = json.loads(result.stdout)
                        for result in bandit_results.get('results', []):
                            file_path = result.get('filename', '')
                            if file_path.startswith(self.temp_dir):
                                file_path = file_path[len(self.temp_dir) + 1:]
                            
                            severity_map = {
                                'HIGH': IssueSeverity.CRITICAL,
                                'MEDIUM': IssueSeverity.MAJOR,
                                'LOW': IssueSeverity.MINOR
                            }
                            
                            self.issues.append(CodeIssue(
                                file_path=file_path,
                                line_start=result.get('line_number', 1),
                                message=f"Security issue: {result.get('issue_text')}",
                                severity=severity_map.get(result.get('issue_severity', 'LOW'), IssueSeverity.MINOR),
                                category=IssueCategory.SECURITY,
                                code_snippet=result.get('code', ''),
                                rule_id=f"bandit-{result.get('test_id', 'security')}"
                            ))
                    except json.JSONDecodeError:
                        # Bandit didn't output valid JSON
                        pass
                
                except FileNotFoundError:
                    # bandit not installed, skip
                    pass
        
        except Exception as e:
            logger.warning(f"Error running security analysis: {str(e)}")
        
        # Check for common security issues using regex patterns
        self._check_security_patterns(files)
    
    def _check_security_patterns(self, files: List[Dict[str, Any]]) -> None:
        """Check for common security issues using regex patterns."""
        # Define security patterns to check
        security_patterns = [
            {
                'pattern': r'(?i)password\s*=\s*[\'"][^\'"]+[\'"]',
                'message': "Hardcoded password detected",
                'severity': IssueSeverity.CRITICAL,
                'rule_id': "hardcoded-password"
            },
            {
                'pattern': r'(?i)api[_-]?key\s*=\s*[\'"][^\'"]+[\'"]',
                'message': "Hardcoded API key detected",
                'severity': IssueSeverity.CRITICAL,
                'rule_id': "hardcoded-api-key"
            },
            {
                'pattern': r'(?i)secret\s*=\s*[\'"][^\'"]+[\'"]',
                'message': "Hardcoded secret detected",
                'severity': IssueSeverity.CRITICAL,
                'rule_id': "hardcoded-secret"
            },
            {
                'pattern': r'eval\s*\(',
                'message': "Potentially unsafe eval() usage",
                'severity': IssueSeverity.MAJOR,
                'rule_id': "unsafe-eval"
            },
            {
                'pattern': r'(?i)exec\s*\(',
                'message': "Potentially unsafe command execution",
                'severity': IssueSeverity.MAJOR,
                'rule_id': "unsafe-exec"
            },
            {
                'pattern': r'(?i)sql\s*=\s*[\'"][^\'"]*(SELECT|INSERT|UPDATE|DELETE)[^\'"]*(\'|")[^\'"]*(\'|")[^\'"]*[\'"]',
                'message': "Potential SQL injection vulnerability",
                'severity': IssueSeverity.CRITICAL,
                'rule_id': "sql-injection"
            }
        ]
        
        for file in files:
            file_path = file.get('filename')
            if not file_path:
                continue
            
            # Skip deleted files
            if file.get('status') == 'removed':
                continue
            
            # Get file content
            full_path = os.path.join(self.temp_dir, file_path)
            try:
                with open(full_path, 'r') as f:
                    content = f.read()
                
                # Check each pattern
                for pattern_info in security_patterns:
                    pattern = pattern_info['pattern']
                    for match in re.finditer(pattern, content):
                        # Get line number
                        line_number = content[:match.start()].count('\n') + 1
                        
                        # Get code snippet
                        lines = content.splitlines()
                        start_line = max(0, line_number - 2)
                        end_line = min(len(lines), line_number + 1)
                        code_snippet = '\n'.join(lines[start_line:end_line])
                        
                        self.issues.append(CodeIssue(
                            file_path=file_path,
                            line_start=line_number,
                            message=f"Security issue: {pattern_info['message']}",
                            severity=pattern_info['severity'],
                            category=IssueCategory.SECURITY,
                            code_snippet=code_snippet,
                            rule_id=pattern_info['rule_id']
                        ))
            
            except Exception as e:
                logger.warning(f"Error checking security patterns for {file_path}: {str(e)}")
    
    def _run_performance_analysis(self, files: List[Dict[str, Any]]) -> None:
        """Run performance analysis on files."""
        logger.info("Running performance analysis")
        
        # Check for common performance issues using regex patterns
        self._check_performance_patterns(files)
    
    def _check_performance_patterns(self, files: List[Dict[str, Any]]) -> None:
        """Check for common performance issues using regex patterns."""
        # Define performance patterns to check
        performance_patterns = [
            {
                'pattern': r'for\s+\w+\s+in\s+range\([^)]+\):\s*\n\s*for\s+\w+\s+in\s+range\([^)]+\):',
                'message': "Nested loops detected, consider optimizing for better performance",
                'severity': IssueSeverity.MINOR,
                'rule_id': "nested-loops",
                'file_types': ['.py']
            },
            {
                'pattern': r'\.filter\([^)]+\)\s*\.\s*filter\([^)]+\)',
                'message': "Multiple filters chained, consider combining filters for better performance",
                'severity': IssueSeverity.SUGGESTION,
                'rule_id': "multiple-filters",
                'file_types': ['.py', '.js', '.ts']
            },
            {
                'pattern': r'(?i)select\s+\*\s+from',
                'message': "SELECT * query detected, consider specifying only needed columns for better performance",
                'severity': IssueSeverity.MINOR,
                'rule_id': "select-all",
                'file_types': ['.sql', '.py', '.js', '.ts', '.java']
            },
            {
                'pattern': r'setTimeout\(\s*function\s*\(\)\s*{\s*[^}]{100,}\s*}\s*,',
                'message': "Large function in setTimeout, consider optimizing for better performance",
                'severity': IssueSeverity.SUGGESTION,
                'rule_id': "large-timeout-function",
                'file_types': ['.js', '.ts']
            }
        ]
        
        for file in files:
            file_path = file.get('filename')
            if not file_path:
                continue
            
            # Skip deleted files
            if file.get('status') == 'removed':
                continue
            
            # Get file extension
            file_ext = os.path.splitext(file_path)[1].lower()
            
            # Get file content
            full_path = os.path.join(self.temp_dir, file_path)
            try:
                with open(full_path, 'r') as f:
                    content = f.read()
                
                # Check each pattern
                for pattern_info in performance_patterns:
                    # Skip if pattern is not applicable to this file type
                    if 'file_types' in pattern_info and file_ext not in pattern_info['file_types']:
                        continue
                    
                    pattern = pattern_info['pattern']
                    for match in re.finditer(pattern, content, re.MULTILINE):
                        # Get line number
                        line_number = content[:match.start()].count('\n') + 1
                        
                        # Get code snippet
                        lines = content.splitlines()
                        start_line = max(0, line_number - 2)
                        end_line = min(len(lines), line_number + 3)
                        code_snippet = '\n'.join(lines[start_line:end_line])
                        
                        self.issues.append(CodeIssue(
                            file_path=file_path,
                            line_start=line_number,
                            line_end=line_number + code_snippet.count('\n'),
                            message=f"Performance issue: {pattern_info['message']}",
                            severity=pattern_info['severity'],
                            category=IssueCategory.PERFORMANCE,
                            code_snippet=code_snippet,
                            rule_id=pattern_info['rule_id']
                        ))
            
            except Exception as e:
                logger.warning(f"Error checking performance patterns for {file_path}: {str(e)}")

def analyze_pr(pr_data: Dict[str, Any], files: List[Dict[str, Any]], diff: str, config: Optional[Dict[str, Any]] = None) -> List[Dict[str, Any]]:
    """Analyze a pull request and return a list of issues."""
    analyzer = CodeAnalyzer(config)
    issues = analyzer.analyze_pr(pr_data, files, diff)
    return [issue.to_dict() for issue in issues]

if __name__ == "__main__":
    # Example usage
    import sys
    
    if len(sys.argv) < 2:
        print("Usage: code_analyzer.py <file_path>")
        sys.exit(1)
    
    file_path = sys.argv[1]
    
    # Create a simple analyzer
    analyzer = CodeAnalyzer()
    
    # Create a mock PR data structure
    mock_pr = {
        "number": 123,
        "repository": {
            "full_name": "example/repo"
        }
    }
    
    # Create a mock file structure
    mock_files = [{
        "filename": os.path.basename(file_path),
        "status": "modified",
        "raw_url": f"file://{file_path}"
    }]
    
    # Analyze the file
    issues = analyzer.analyze_pr(mock_pr, mock_files, "")
    
    # Print issues
    for issue in issues:
        print(f"{issue.severity.value.upper()}: {issue.message}")
        print(f"  File: {issue.file_path}, Line: {issue.line_start}")
        if issue.code_snippet:
            print(f"  Code: {issue.code_snippet.strip()}")
        print()