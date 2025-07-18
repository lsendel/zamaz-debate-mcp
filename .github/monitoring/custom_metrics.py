"""
Custom metrics for PR processing monitoring.

This module implements custom Prometheus metrics for tracking
PR processing performance, SLOs, and critical failures.
"""

from datetime import datetime
from typing import Any

from prometheus_client import CollectorRegistry, Counter, Gauge, Histogram, Info, Summary, generate_latest

from ..scripts.core.logging import get_logger

logger = get_logger(__name__)

# Constants for alert thresholds
MAX_PROCESSING_HISTORY = 1000
CRITICAL_ERROR_RATE = 0.05
WARNING_RATE_LIMIT_PERCENT = 10
SLOW_PROCESSING_SECONDS = 300
HIGH_QUEUE_SIZE = 100
HIGH_MEMORY_MB = 1024


class PRProcessingMetrics:
    """Custom metrics for PR processing."""

    def __init__(self, registry: CollectorRegistry | None = None):
        self.registry = registry or CollectorRegistry()

        # PR Processing Metrics
        self.pr_received = Counter(
            "kiro_pr_received_total",
            "Total number of PRs received for processing",
            ["action", "repo_owner", "repo_name"],
            registry=self.registry,
        )

        self.pr_processed = Counter(
            "kiro_pr_processed_total",
            "Total number of PRs processed",
            ["status", "repo_owner", "repo_name"],
            registry=self.registry,
        )

        self.pr_processing_duration = Histogram(
            "kiro_pr_processing_duration_seconds",
            "Time spent processing a PR",
            ["repo_owner", "repo_name", "pr_size"],
            buckets=(0.5, 1.0, 2.5, 5.0, 10.0, 30.0, 60.0, 120.0, 300.0),
            registry=self.registry,
        )

        self.pr_queue_size = Gauge(
            "kiro_pr_queue_size", "Current number of PRs in processing queue", ["priority"], registry=self.registry
        )

        # Analysis Metrics
        self.files_analyzed = Counter(
            "kiro_files_analyzed_total",
            "Total number of files analyzed",
            ["language", "repo_owner"],
            registry=self.registry,
        )

        self.issues_found = Counter(
            "kiro_issues_found_total",
            "Total number of issues found",
            ["category", "severity", "repo_owner"],
            registry=self.registry,
        )

        self.analysis_duration = Histogram(
            "kiro_analysis_duration_seconds",
            "Time spent analyzing code",
            ["analyzer", "language"],
            buckets=(0.01, 0.05, 0.1, 0.5, 1.0, 2.5, 5.0, 10.0),
            registry=self.registry,
        )

        # Review Metrics
        self.reviews_posted = Counter(
            "kiro_reviews_posted_total",
            "Total number of reviews posted",
            ["review_type", "repo_owner"],
            registry=self.registry,
        )

        self.review_comments = Histogram(
            "kiro_review_comments_count",
            "Number of comments per review",
            ["repo_owner"],
            buckets=(0, 1, 5, 10, 20, 50, 100),
            registry=self.registry,
        )

        # Error Metrics
        self.errors = Counter(
            "kiro_errors_total", "Total number of errors", ["error_type", "component"], registry=self.registry
        )

        self.github_api_calls = Counter(
            "kiro_github_api_calls_total", "Total GitHub API calls", ["endpoint", "status_code"], registry=self.registry
        )

        self.github_rate_limit_remaining = Gauge(
            "kiro_github_rate_limit_remaining", "Remaining GitHub API rate limit", registry=self.registry
        )

        # SLO Metrics
        self.slo_pr_processing_time = Summary(
            "kiro_slo_pr_processing_seconds", "SLO: PR processing time", ["percentile"], registry=self.registry
        )

        self.slo_availability = Gauge(
            "kiro_slo_availability_ratio", "SLO: Service availability ratio", registry=self.registry
        )

        # System Metrics
        self.active_connections = Gauge(
            "kiro_active_connections", "Number of active connections", ["connection_type"], registry=self.registry
        )

        self.memory_usage_bytes = Gauge(
            "kiro_memory_usage_bytes", "Current memory usage in bytes", ["component"], registry=self.registry
        )

        # Business Metrics
        self.developer_satisfaction = Gauge(
            "kiro_developer_satisfaction_score",
            "Developer satisfaction score (1-5)",
            ["repo_owner"],
            registry=self.registry,
        )

        self.pr_turnaround_time = Histogram(
            "kiro_pr_turnaround_seconds",
            "Time from PR open to first review",
            ["repo_owner"],
            buckets=(60, 300, 900, 1800, 3600, 7200, 14400),  # 1m to 4h
            registry=self.registry,
        )

        # Info metrics
        self.build_info = Info("kiro_build_info", "Build information", registry=self.registry)

    def record_pr_received(self, action: str, repo_owner: str, repo_name: str):
        """Record PR received for processing."""
        self.pr_received.labels(action=action, repo_owner=repo_owner, repo_name=repo_name).inc()

    def record_pr_processed(self, status: str, repo_owner: str, repo_name: str, duration: float, pr_size: str):
        """Record PR processing completion."""
        self.pr_processed.labels(status=status, repo_owner=repo_owner, repo_name=repo_name).inc()

        self.pr_processing_duration.labels(repo_owner=repo_owner, repo_name=repo_name, pr_size=pr_size).observe(
            duration
        )

        # Update SLO metric
        self.slo_pr_processing_time.labels(percentile="p95").observe(duration)

    def update_queue_size(self, priority: str, size: int):
        """Update PR queue size."""
        self.pr_queue_size.labels(priority=priority).set(size)

    def record_file_analyzed(self, language: str, repo_owner: str):
        """Record file analysis."""
        self.files_analyzed.labels(language=language, repo_owner=repo_owner).inc()

    def record_issue_found(self, category: str, severity: str, repo_owner: str):
        """Record issue found during analysis."""
        self.issues_found.labels(category=category, severity=severity, repo_owner=repo_owner).inc()

    def record_analysis_duration(self, analyzer: str, language: str, duration: float):
        """Record code analysis duration."""
        self.analysis_duration.labels(analyzer=analyzer, language=language).observe(duration)

    def record_review_posted(self, review_type: str, repo_owner: str, comment_count: int):
        """Record review posting."""
        self.reviews_posted.labels(review_type=review_type, repo_owner=repo_owner).inc()

        self.review_comments.labels(repo_owner=repo_owner).observe(comment_count)

    def record_error(self, error_type: str, component: str):
        """Record error occurrence."""
        self.errors.labels(error_type=error_type, component=component).inc()

    def record_github_api_call(self, endpoint: str, status_code: int):
        """Record GitHub API call."""
        self.github_api_calls.labels(endpoint=endpoint, status_code=str(status_code)).inc()

    def update_github_rate_limit(self, remaining: int):
        """Update GitHub rate limit."""
        self.github_rate_limit_remaining.set(remaining)

    def update_availability(self, ratio: float):
        """Update service availability ratio."""
        self.slo_availability.set(ratio)

    def update_active_connections(self, connection_type: str, count: int):
        """Update active connections count."""
        self.active_connections.labels(connection_type=connection_type).set(count)

    def update_memory_usage(self, component: str, bytes_used: int):
        """Update memory usage."""
        self.memory_usage_bytes.labels(component=component).set(bytes_used)

    def record_developer_feedback(self, repo_owner: str, score: float):
        """Record developer satisfaction score."""
        self.developer_satisfaction.labels(repo_owner=repo_owner).set(score)

    def record_pr_turnaround(self, repo_owner: str, seconds: float):
        """Record PR turnaround time."""
        self.pr_turnaround_time.labels(repo_owner=repo_owner).observe(seconds)

    def set_build_info(self, version: str, commit: str, build_time: str):
        """Set build information."""
        self.build_info.info({"version": version, "commit": commit, "build_time": build_time})

    def get_metrics(self) -> bytes:
        """Get metrics in Prometheus format."""
        return generate_latest(self.registry)


class SLOMonitor:
    """Monitor Service Level Objectives."""

    def __init__(self, metrics: PRProcessingMetrics):
        self.metrics = metrics
        self.slo_targets = {
            "pr_processing_p95": 300,  # 95% of PRs processed in < 5 minutes
            "availability": 0.999,  # 99.9% availability
            "error_rate": 0.001,  # < 0.1% error rate
            "review_time_p90": 180,  # 90% reviewed in < 3 minutes
        }

        # Tracking data
        self.window_start = datetime.utcnow()
        self.requests_total = 0
        self.requests_failed = 0
        self.processing_times: list[float] = []

    def record_request(self, success: bool, duration: float):
        """Record a request for SLO calculation."""
        self.requests_total += 1
        if not success:
            self.requests_failed += 1
        self.processing_times.append(duration)

        # Maintain rolling window (last 1000 requests)
        if len(self.processing_times) > MAX_PROCESSING_HISTORY:
            self.processing_times.pop(0)

    def check_slos(self) -> dict[str, dict[str, Any]]:
        """Check current SLO status."""
        results = {}

        # Processing time SLO
        if self.processing_times:
            sorted_times = sorted(self.processing_times)
            p95_index = int(len(sorted_times) * 0.95)
            p95_time = sorted_times[p95_index] if p95_index < len(sorted_times) else 0

            results["pr_processing_p95"] = {
                "current": p95_time,
                "target": self.slo_targets["pr_processing_p95"],
                "met": p95_time <= self.slo_targets["pr_processing_p95"],
                "margin": (self.slo_targets["pr_processing_p95"] - p95_time) / self.slo_targets["pr_processing_p95"],
            }

        # Availability SLO
        if self.requests_total > 0:
            availability = (self.requests_total - self.requests_failed) / self.requests_total
            results["availability"] = {
                "current": availability,
                "target": self.slo_targets["availability"],
                "met": availability >= self.slo_targets["availability"],
                "margin": availability - self.slo_targets["availability"],
            }

            # Update metric
            self.metrics.update_availability(availability)

        # Error rate SLO
        if self.requests_total > 0:
            error_rate = self.requests_failed / self.requests_total
            results["error_rate"] = {
                "current": error_rate,
                "target": self.slo_targets["error_rate"],
                "met": error_rate <= self.slo_targets["error_rate"],
                "margin": (self.slo_targets["error_rate"] - error_rate) / self.slo_targets["error_rate"],
            }

        return results

    def get_error_budget(self) -> dict[str, float]:
        """Calculate remaining error budget."""
        budget = {}

        # Availability error budget
        if self.requests_total > 0:
            (self.requests_total - self.requests_failed) / self.requests_total
            allowed_failures = self.requests_total * (1 - self.slo_targets["availability"])
            budget["availability"] = {
                "allowed_failures": allowed_failures,
                "actual_failures": self.requests_failed,
                "remaining_budget": max(0, allowed_failures - self.requests_failed),
                "budget_consumed_percent": (self.requests_failed / allowed_failures * 100)
                if allowed_failures > 0
                else 0,
            }

        return budget


class AlertManager:
    """Manage alerts based on metrics."""

    def __init__(self, metrics: PRProcessingMetrics):
        self.metrics = metrics
        self.alerts: list[dict[str, Any]] = []
        self.alert_rules = self._define_alert_rules()

    def _define_alert_rules(self) -> list[dict[str, Any]]:
        """Define alert rules."""
        return [
            {
                "name": "HighErrorRate",
                "condition": lambda: self._get_error_rate() > CRITICAL_ERROR_RATE,
                "severity": "critical",
                "message": "Error rate exceeds 5%",
            },
            {
                "name": "LowGitHubRateLimit",
                "condition": lambda: self._get_rate_limit_percent() < WARNING_RATE_LIMIT_PERCENT,
                "severity": "warning",
                "message": "GitHub rate limit below 10%",
            },
            {
                "name": "SlowPRProcessing",
                "condition": lambda: self._get_processing_p95() > SLOW_PROCESSING_SECONDS,
                "severity": "warning",
                "message": "PR processing P95 exceeds 5 minutes",
            },
            {
                "name": "HighQueueSize",
                "condition": lambda: self._get_max_queue_size() > HIGH_QUEUE_SIZE,
                "severity": "warning",
                "message": "PR queue size exceeds 100",
            },
            {
                "name": "MemoryUsageHigh",
                "condition": lambda: self._get_memory_usage_mb() > HIGH_MEMORY_MB,
                "severity": "warning",
                "message": "Memory usage exceeds 1GB",
            },
        ]

    def check_alerts(self):
        """Check all alert conditions."""
        current_alerts = []

        for rule in self.alert_rules:
            try:
                if rule["condition"]():
                    alert = {
                        "name": rule["name"],
                        "severity": rule["severity"],
                        "message": rule["message"],
                        "timestamp": datetime.utcnow(),
                    }
                    current_alerts.append(alert)
                    logger.warning(f"Alert triggered: {rule['name']} - {rule['message']}")
            except Exception as e:
                logger.error(f"Error checking alert {rule['name']}: {e}")

        self.alerts = current_alerts
        return current_alerts

    def _get_error_rate(self) -> float:
        """Calculate current error rate."""
        # This would query actual metrics
        return 0.01  # Placeholder

    def _get_rate_limit_percent(self) -> float:
        """Get GitHub rate limit percentage."""
        # This would query actual metrics
        return 50.0  # Placeholder

    def _get_processing_p95(self) -> float:
        """Get PR processing P95 time."""
        # This would query actual metrics
        return 120.0  # Placeholder

    def _get_max_queue_size(self) -> int:
        """Get maximum queue size."""
        # This would query actual metrics
        return 10  # Placeholder

    def _get_memory_usage_mb(self) -> float:
        """Get memory usage in MB."""
        # This would query actual metrics
        return 256.0  # Placeholder
