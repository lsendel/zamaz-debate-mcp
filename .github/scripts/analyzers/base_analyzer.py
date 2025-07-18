"""
Base analyzer and strategy pattern implementation for code analysis.

This module provides the foundation for all code analyzers using
the strategy pattern for maximum flexibility and extensibility.
"""

import asyncio
import time
from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

from ..core.exceptions import AnalysisTimeoutError, FileNotSupportedError
from ..core.interfaces import AnalysisResult, AnalyzerInterface, CodeIssue, IssueLevel


@dataclass
class AnalyzerContext:
    """Context information for analysis."""

    file_path: str
    content: str
    language: str
    pr_context: dict[str, Any] | None = None
    config: dict[str, Any] = field(default_factory=dict)
    timeout_seconds: int = 30


class AnalysisStrategy(ABC):
    """Base strategy for code analysis."""

    @abstractmethod
    async def analyze(self, context: AnalyzerContext) -> list[CodeIssue]:
        """Perform analysis and return issues."""
        pass

    @abstractmethod
    def get_name(self) -> str:
        """Get strategy name."""
        pass

    def get_supported_languages(self) -> set[str]:
        """Get supported programming languages."""
        return set()


class BaseAnalyzer(AnalyzerInterface):
    """
    Base analyzer that orchestrates multiple analysis strategies.

    This class implements the strategy pattern, allowing different
    analysis techniques to be plugged in and combined.
    """

    def __init__(self, strategies: list[AnalysisStrategy]):
        self.strategies = strategies
        self.supported_extensions = self._build_extension_map()

    def _build_extension_map(self) -> dict[str, str]:
        """Build mapping of file extensions to languages."""
        return {
            ".py": "python",
            ".js": "javascript",
            ".jsx": "javascript",
            ".ts": "typescript",
            ".tsx": "typescript",
            ".java": "java",
            ".go": "golang",
            ".rs": "rust",
            ".rb": "ruby",
            ".php": "php",
            ".c": "c",
            ".cpp": "cpp",
            ".cs": "csharp",
            ".swift": "swift",
            ".kt": "kotlin",
            ".scala": "scala",
            ".r": "r",
            ".m": "objc",
            ".yml": "yaml",
            ".yaml": "yaml",
            ".json": "json",
            ".xml": "xml",
            ".sql": "sql",
            ".sh": "bash",
            ".ps1": "powershell",
            ".dockerfile": "dockerfile",
            "Dockerfile": "dockerfile",
            ".md": "markdown",
            ".rst": "restructuredtext",
        }

    async def analyze(self, file_path: str, content: str, context: dict[str, Any] | None = None) -> AnalysisResult:
        """
        Analyze code using all configured strategies.

        Args:
            file_path: Path to the file being analyzed
            content: File content to analyze
            context: Additional context (PR info, etc.)

        Returns:
            Analysis result with all found issues
        """
        start_time = time.time()

        # Determine language
        language = self._detect_language(file_path)
        if not language:
            raise FileNotSupportedError(file_path, Path(file_path).suffix)

        # Create analysis context
        analysis_context = AnalyzerContext(
            file_path=file_path,
            content=content,
            language=language,
            pr_context=context,
            config=self._get_config_for_language(language),
        )

        # Run all applicable strategies
        all_issues = []
        strategy_results = {}

        for strategy in self.strategies:
            if not self._should_run_strategy(strategy, analysis_context):
                continue

            try:
                # Run strategy with timeout
                issues = await asyncio.wait_for(
                    strategy.analyze(analysis_context), timeout=analysis_context.timeout_seconds
                )

                all_issues.extend(issues)
                strategy_results[strategy.get_name()] = len(issues)

            except TimeoutError:
                raise AnalysisTimeoutError(file_path, analysis_context.timeout_seconds)
            except Exception as e:
                # Log error but continue with other strategies
                strategy_results[strategy.get_name()] = f"error: {e!s}"

        # Sort issues by severity and line number
        all_issues.sort(key=lambda x: (self._severity_order(x.level), x.line_number or 0))

        duration_ms = (time.time() - start_time) * 1000

        return AnalysisResult(
            issues=all_issues,
            metrics={
                "total_issues": len(all_issues),
                "issues_by_level": self._count_by_level(all_issues),
                "issues_by_category": self._count_by_category(all_issues),
                "strategy_results": strategy_results,
                "language": language,
                "file_size": len(content),
                "line_count": len(content.splitlines()),
            },
            duration_ms=duration_ms,
            analyzer_version="2.0.0",
        )

    def supports_file(self, file_path: str) -> bool:
        """Check if analyzer supports the file type."""
        return self._detect_language(file_path) is not None

    def get_capabilities(self) -> list[str]:
        """Get list of analyzer capabilities."""
        capabilities = set()
        for strategy in self.strategies:
            capabilities.add(strategy.get_name())
        return sorted(capabilities)

    def _detect_language(self, file_path: str) -> str | None:
        """Detect programming language from file path."""
        path = Path(file_path)

        # Check full filename first (for Dockerfile, etc.)
        if path.name in self.supported_extensions:
            return self.supported_extensions[path.name]

        # Check extension
        return self.supported_extensions.get(path.suffix.lower())

    def _should_run_strategy(self, strategy: AnalysisStrategy, context: AnalyzerContext) -> bool:
        """Determine if strategy should run for this context."""
        supported_langs = strategy.get_supported_languages()

        # If strategy doesn't specify languages, run for all
        if not supported_langs:
            return True

        return context.language in supported_langs

    def _get_config_for_language(self, language: str) -> dict[str, Any]:
        """Get configuration specific to a language."""
        # This would load from config files in production
        return {
            "max_line_length": 120 if language == "python" else 100,
            "indent_size": 4 if language in ["python", "java"] else 2,
            "check_todos": True,
            "check_fixmes": True,
        }

    def _severity_order(self, level: IssueLevel) -> int:
        """Get numeric order for severity level."""
        order = {IssueLevel.CRITICAL: 0, IssueLevel.ERROR: 1, IssueLevel.WARNING: 2, IssueLevel.INFO: 3}
        return order.get(level, 999)

    def _count_by_level(self, issues: list[CodeIssue]) -> dict[str, int]:
        """Count issues by severity level."""
        counts = {}
        for issue in issues:
            level = issue.level.value
            counts[level] = counts.get(level, 0) + 1
        return counts

    def _count_by_category(self, issues: list[CodeIssue]) -> dict[str, int]:
        """Count issues by category."""
        counts = {}
        for issue in issues:
            counts[issue.category] = counts.get(issue.category, 0) + 1
        return counts


class CompositeAnalyzer(BaseAnalyzer):
    """
    Analyzer that combines multiple sub-analyzers.

    This allows for modular composition of analysis capabilities.
    """

    def __init__(self, analyzers: list[AnalyzerInterface]):
        # Extract strategies from all analyzers
        all_strategies = []
        for analyzer in analyzers:
            if isinstance(analyzer, BaseAnalyzer):
                all_strategies.extend(analyzer.strategies)

        super().__init__(all_strategies)
        self.analyzers = analyzers

    async def analyze(self, file_path: str, content: str, context: dict[str, Any] | None = None) -> AnalysisResult:
        """Run all analyzers and combine results."""
        # If we have specific analyzers, use them
        if self.analyzers:
            all_issues = []
            all_metrics = {}
            total_duration = 0

            for analyzer in self.analyzers:
                if analyzer.supports_file(file_path):
                    result = await analyzer.analyze(file_path, content, context)
                    all_issues.extend(result.issues)

                    # Merge metrics
                    for key, value in result.metrics.items():
                        if key not in all_metrics:
                            all_metrics[key] = value
                        elif isinstance(value, dict):
                            all_metrics[key].update(value)
                        elif isinstance(value, int | float):
                            all_metrics[key] = all_metrics.get(key, 0) + value

                    total_duration += result.duration_ms

            # Remove duplicates
            unique_issues = []
            seen = set()
            for issue in all_issues:
                key = (issue.file_path, issue.line_number, issue.message)
                if key not in seen:
                    seen.add(key)
                    unique_issues.append(issue)

            return AnalysisResult(
                issues=unique_issues,
                metrics=all_metrics,
                duration_ms=total_duration,
                analyzer_version="2.0.0-composite",
            )

        # Fall back to base implementation
        return await super().analyze(file_path, content, context)
