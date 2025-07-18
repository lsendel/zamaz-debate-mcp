#!/usr/bin/env python3
"""
Analytics collector for Kiro GitHub integration.
This module collects and stores analytics data for code reviews and feedback.
"""

import logging
import sqlite3
import uuid
from datetime import datetime
from pathlib import Path
from typing import Any

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
    handlers=[logging.StreamHandler(), logging.FileHandler("kiro_analytics.log")],
)
logger = logging.getLogger("kiro_analytics")

# Constants
DEFAULT_DB_PATH = Path(__file__).parent / ".." / ".." / ".kiro" / "data" / "analytics.db"


class AnalyticsCollector:
    """Collects and stores analytics data for code reviews and feedback."""

    def __init__(self, db_path: str | None = None):
        """Initialize the analytics collector."""
        self.db_path = db_path or DEFAULT_DB_PATH

        # Ensure directory exists
        Path(self.db_path).parent.mkdir(parents=True, exist_ok=True)

        # Initialize database
        self._init_db()

    def _init_db(self):
        """Initialize the database."""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()

            # Create reviews table
            cursor.execute("""
            CREATE TABLE IF NOT EXISTS reviews (
                id TEXT PRIMARY KEY,
                repo_owner TEXT NOT NULL,
                repo_name TEXT NOT NULL,
                pr_number INTEGER NOT NULL,
                start_time TIMESTAMP NOT NULL,
                end_time TIMESTAMP,
                status TEXT NOT NULL,
                issue_count INTEGER,
                file_count INTEGER,
                comment_count INTEGER,
                suggestion_count INTEGER,
                applied_suggestion_count INTEGER
            )
            """)

            # Create issues table
            cursor.execute("""
            CREATE TABLE IF NOT EXISTS issues (
                id TEXT PRIMARY KEY,
                review_id TEXT NOT NULL,
                file_path TEXT NOT NULL,
                line_start INTEGER NOT NULL,
                line_end INTEGER,
                severity TEXT NOT NULL,
                category TEXT NOT NULL,
                rule_id TEXT,
                has_suggestion BOOLEAN NOT NULL,
                suggestion_applied BOOLEAN,
                feedback_score INTEGER,
                FOREIGN KEY (review_id) REFERENCES reviews (id)
            )
            """)

            # Create feedback table
            cursor.execute("""
            CREATE TABLE IF NOT EXISTS feedback (
                id TEXT PRIMARY KEY,
                review_id TEXT NOT NULL,
                issue_id TEXT,
                feedback_type TEXT NOT NULL,
                score INTEGER,
                comment TEXT,
                timestamp TIMESTAMP NOT NULL,
                FOREIGN KEY (review_id) REFERENCES reviews (id),
                FOREIGN KEY (issue_id) REFERENCES issues (id)
            )
            """)

            # Create metrics table
            cursor.execute("""
            CREATE TABLE IF NOT EXISTS metrics (
                id TEXT PRIMARY KEY,
                repo_owner TEXT NOT NULL,
                repo_name TEXT NOT NULL,
                metric_name TEXT NOT NULL,
                metric_value REAL NOT NULL,
                timestamp TIMESTAMP NOT NULL
            )
            """)

            conn.commit()
            conn.close()

            logger.info("Database initialized")

        except Exception as e:
            logger.error(f"Error initializing database: {e!s}")

    def start_review(self, repo_owner: str, repo_name: str, pr_number: int) -> str:
        """Start tracking a new review."""
        try:
            review_id = str(uuid.uuid4())
            start_time = datetime.now().isoformat()

            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()

            cursor.execute(
                """
            INSERT INTO reviews (id, repo_owner, repo_name, pr_number, start_time, status)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
                (review_id, repo_owner, repo_name, pr_number, start_time, "in_progress"),
            )

            conn.commit()
            conn.close()

            logger.info(f"Started review {review_id} for PR #{pr_number} in {repo_owner}/{repo_name}")
            return review_id

        except Exception as e:
            logger.error(f"Error starting review: {e!s}")
            return ""

    def complete_review(
        self, review_id: str, issues: list[dict[str, Any]], file_count: int, comment_count: int, suggestion_count: int
    ) -> bool:
        """Complete a review and store issue data."""
        try:
            end_time = datetime.now().isoformat()
            issue_count = len(issues)

            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()

            # Update review
            cursor.execute(
                """
            UPDATE reviews
            SET end_time = ?, status = ?, issue_count = ?, file_count = ?, comment_count = ?, suggestion_count = ?
            WHERE id = ?
            """,
                (end_time, "completed", issue_count, file_count, comment_count, suggestion_count, review_id),
            )

            # Insert issues
            for issue in issues:
                issue_id = str(uuid.uuid4())
                file_path = issue.get("file_path", "")
                line_start = issue.get("line_start", 1)
                line_end = issue.get("line_end", line_start)
                severity = issue.get("severity", "suggestion")
                category = issue.get("category", "best_practice")
                rule_id = issue.get("rule_id", "")
                has_suggestion = "suggestion" in issue or issue.get("fix_suggestion") is not None

                cursor.execute(
                    """
                INSERT INTO issues (id, review_id, file_path, line_start, line_end, severity, category, rule_id, has_suggestion, suggestion_applied)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                    (
                        issue_id,
                        review_id,
                        file_path,
                        line_start,
                        line_end,
                        severity,
                        category,
                        rule_id,
                        has_suggestion,
                        False,
                    ),
                )

            conn.commit()
            conn.close()

            logger.info(f"Completed review {review_id} with {issue_count} issues")
            return True

        except Exception as e:
            logger.error(f"Error completing review: {e!s}")
            return False

    def record_suggestion_applied(self, review_id: str, file_path: str, line_number: int) -> bool:
        """Record that a suggestion was applied."""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()

            # Find the issue
            cursor.execute(
                """
            SELECT id FROM issues
            WHERE review_id = ? AND file_path = ? AND line_start = ? AND has_suggestion = 1
            """,
                (review_id, file_path, line_number),
            )

            result = cursor.fetchone()
            if not result:
                logger.warning(
                    f"No matching issue found for suggestion in review {review_id}, file {file_path}, line {line_number}"
                )
                conn.close()
                return False

            issue_id = result[0]

            # Update the issue
            cursor.execute(
                """
            UPDATE issues
            SET suggestion_applied = 1
            WHERE id = ?
            """,
                (issue_id,),
            )

            # Update the review
            cursor.execute(
                """
            UPDATE reviews
            SET applied_suggestion_count = COALESCE(applied_suggestion_count, 0) + 1
            WHERE id = ?
            """,
                (review_id,),
            )

            conn.commit()
            conn.close()

            logger.info(f"Recorded suggestion applied for issue {issue_id} in review {review_id}")
            return True

        except Exception as e:
            logger.error(f"Error recording suggestion applied: {e!s}")
            return False

    def record_feedback(
        self,
        review_id: str,
        feedback_type: str,
        score: int | None = None,
        comment: str | None = None,
        issue_id: str | None = None,
    ) -> bool:
        """Record feedback for a review or issue."""
        try:
            feedback_id = str(uuid.uuid4())
            timestamp = datetime.now().isoformat()

            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()

            cursor.execute(
                """
            INSERT INTO feedback (id, review_id, issue_id, feedback_type, score, comment, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
                (feedback_id, review_id, issue_id, feedback_type, score, comment, timestamp),
            )

            # If issue_id is provided, update the issue's feedback score
            if issue_id and score is not None:
                cursor.execute(
                    """
                UPDATE issues
                SET feedback_score = ?
                WHERE id = ?
                """,
                    (score, issue_id),
                )

            conn.commit()
            conn.close()

            logger.info(f"Recorded {feedback_type} feedback for review {review_id}")
            return True

        except Exception as e:
            logger.error(f"Error recording feedback: {e!s}")
            return False

    def record_metric(self, repo_owner: str, repo_name: str, metric_name: str, metric_value: float) -> bool:
        """Record a metric."""
        try:
            metric_id = str(uuid.uuid4())
            timestamp = datetime.now().isoformat()

            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()

            cursor.execute(
                """
            INSERT INTO metrics (id, repo_owner, repo_name, metric_name, metric_value, timestamp)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
                (metric_id, repo_owner, repo_name, metric_name, metric_value, timestamp),
            )

            conn.commit()
            conn.close()

            logger.info(f"Recorded metric {metric_name} = {metric_value} for {repo_owner}/{repo_name}")
            return True

        except Exception as e:
            logger.error(f"Error recording metric: {e!s}")
            return False

    def get_review_stats(
        self, repo_owner: str | None = None, repo_name: str | None = None, days: int = 30
    ) -> dict[str, Any]:
        """Get review statistics."""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()

            # Calculate date cutoff
            cutoff_date = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
            cutoff_date = cutoff_date.replace(day=cutoff_date.day - days).isoformat()

            # Build query
            query = """
            SELECT
                COUNT(*) as total_reviews,
                AVG(julianday(end_time) - julianday(start_time)) * 24 * 60 * 60 as avg_review_time_seconds,
                SUM(issue_count) as total_issues,
                AVG(issue_count) as avg_issues_per_review,
                SUM(suggestion_count) as total_suggestions,
                SUM(applied_suggestion_count) as total_applied_suggestions,
                CASE WHEN SUM(suggestion_count) > 0 THEN CAST(SUM(applied_suggestion_count) AS REAL) / SUM(suggestion_count) ELSE 0 END as suggestion_acceptance_rate
            FROM reviews
            WHERE end_time IS NOT NULL AND start_time >= ?
            """

            params = [cutoff_date]

            if repo_owner:
                query += " AND repo_owner = ?"
                params.append(repo_owner)

            if repo_name:
                query += " AND repo_name = ?"
                params.append(repo_name)

            cursor.execute(query, params)
            result = cursor.fetchone()

            # Get issue breakdown by severity
            severity_query = """
            SELECT severity, COUNT(*) as count
            FROM issues
            JOIN reviews ON issues.review_id = reviews.id
            WHERE reviews.end_time IS NOT NULL AND reviews.start_time >= ?
            """

            severity_params = [cutoff_date]

            if repo_owner:
                severity_query += " AND reviews.repo_owner = ?"
                severity_params.append(repo_owner)

            if repo_name:
                severity_query += " AND reviews.repo_name = ?"
                severity_params.append(repo_name)

            severity_query += " GROUP BY severity"

            cursor.execute(severity_query, severity_params)
            severity_results = cursor.fetchall()

            # Get issue breakdown by category
            category_query = """
            SELECT category, COUNT(*) as count
            FROM issues
            JOIN reviews ON issues.review_id = reviews.id
            WHERE reviews.end_time IS NOT NULL AND reviews.start_time >= ?
            """

            category_params = [cutoff_date]

            if repo_owner:
                category_query += " AND reviews.repo_owner = ?"
                category_params.append(repo_owner)

            if repo_name:
                category_query += " AND reviews.repo_name = ?"
                category_params.append(repo_name)

            category_query += " GROUP BY category"

            cursor.execute(category_query, category_params)
            category_results = cursor.fetchall()

            conn.close()

            # Format results
            stats = {
                "total_reviews": result[0],
                "avg_review_time_seconds": result[1],
                "total_issues": result[2],
                "avg_issues_per_review": result[3],
                "total_suggestions": result[4],
                "total_applied_suggestions": result[5],
                "suggestion_acceptance_rate": result[6],
                "issues_by_severity": dict(severity_results),
                "issues_by_category": dict(category_results),
            }

            return stats

        except Exception as e:
            logger.error(f"Error getting review stats: {e!s}")
            return {}

    def get_feedback_stats(
        self, repo_owner: str | None = None, repo_name: str | None = None, days: int = 30
    ) -> dict[str, Any]:
        """Get feedback statistics."""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()

            # Calculate date cutoff
            cutoff_date = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
            cutoff_date = cutoff_date.replace(day=cutoff_date.day - days).isoformat()

            # Build query
            query = """
            SELECT
                feedback_type,
                COUNT(*) as count,
                AVG(score) as avg_score
            FROM feedback
            JOIN reviews ON feedback.review_id = reviews.id
            WHERE reviews.start_time >= ?
            """

            params = [cutoff_date]

            if repo_owner:
                query += " AND reviews.repo_owner = ?"
                params.append(repo_owner)

            if repo_name:
                query += " AND reviews.repo_name = ?"
                params.append(repo_name)

            query += " GROUP BY feedback_type"

            cursor.execute(query, params)
            results = cursor.fetchall()

            conn.close()

            # Format results
            stats = {
                "feedback_by_type": {
                    feedback_type: {"count": count, "avg_score": avg_score}
                    for feedback_type, count, avg_score in results
                }
            }

            return stats

        except Exception as e:
            logger.error(f"Error getting feedback stats: {e!s}")
            return {}

    def get_metrics(
        self,
        repo_owner: str | None = None,
        repo_name: str | None = None,
        metric_name: str | None = None,
        days: int = 30,
    ) -> list[dict[str, Any]]:
        """Get metrics."""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()

            # Calculate date cutoff
            cutoff_date = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
            cutoff_date = cutoff_date.replace(day=cutoff_date.day - days).isoformat()

            # Build query
            query = """
            SELECT
                repo_owner,
                repo_name,
                metric_name,
                metric_value,
                timestamp
            FROM metrics
            WHERE timestamp >= ?
            """

            params = [cutoff_date]

            if repo_owner:
                query += " AND repo_owner = ?"
                params.append(repo_owner)

            if repo_name:
                query += " AND repo_name = ?"
                params.append(repo_name)

            if metric_name:
                query += " AND metric_name = ?"
                params.append(metric_name)

            query += " ORDER BY timestamp DESC"

            cursor.execute(query, params)
            results = cursor.fetchall()

            conn.close()

            # Format results
            metrics = [
                {
                    "repo_owner": row[0],
                    "repo_name": row[1],
                    "metric_name": row[2],
                    "metric_value": row[3],
                    "timestamp": row[4],
                }
                for row in results
            ]

            return metrics

        except Exception as e:
            logger.error(f"Error getting metrics: {e!s}")
            return []

    def get_top_issues(
        self, repo_owner: str | None = None, repo_name: str | None = None, days: int = 30, limit: int = 10
    ) -> list[dict[str, Any]]:
        """Get top issues by frequency."""
        try:
            conn = sqlite3.connect(self.db_path)
            cursor = conn.cursor()

            # Calculate date cutoff
            cutoff_date = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
            cutoff_date = cutoff_date.replace(day=cutoff_date.day - days).isoformat()

            # Build query
            query = """
            SELECT
                category,
                rule_id,
                COUNT(*) as count
            FROM issues
            JOIN reviews ON issues.review_id = reviews.id
            WHERE reviews.start_time >= ?
            """

            params = [cutoff_date]

            if repo_owner:
                query += " AND reviews.repo_owner = ?"
                params.append(repo_owner)

            if repo_name:
                query += " AND reviews.repo_name = ?"
                params.append(repo_name)

            query += " GROUP BY category, rule_id ORDER BY count DESC LIMIT ?"
            params.append(limit)

            cursor.execute(query, params)
            results = cursor.fetchall()

            conn.close()

            # Format results
            top_issues = [{"category": row[0], "rule_id": row[1], "count": row[2]} for row in results]

            return top_issues

        except Exception as e:
            logger.error(f"Error getting top issues: {e!s}")
            return []


def start_review(repo_owner: str, repo_name: str, pr_number: int) -> str:
    """Start tracking a new review."""
    collector = AnalyticsCollector()
    return collector.start_review(repo_owner, repo_name, pr_number)


def complete_review(
    review_id: str, issues: list[dict[str, Any]], file_count: int, comment_count: int, suggestion_count: int
) -> bool:
    """Complete a review and store issue data."""
    collector = AnalyticsCollector()
    return collector.complete_review(review_id, issues, file_count, comment_count, suggestion_count)


def record_suggestion_applied(review_id: str, file_path: str, line_number: int) -> bool:
    """Record that a suggestion was applied."""
    collector = AnalyticsCollector()
    return collector.record_suggestion_applied(review_id, file_path, line_number)


def record_feedback(
    review_id: str,
    feedback_type: str,
    score: int | None = None,
    comment: str | None = None,
    issue_id: str | None = None,
) -> bool:
    """Record feedback for a review or issue."""
    collector = AnalyticsCollector()
    return collector.record_feedback(review_id, feedback_type, score, comment, issue_id)


def record_metric(repo_owner: str, repo_name: str, metric_name: str, metric_value: float) -> bool:
    """Record a metric."""
    collector = AnalyticsCollector()
    return collector.record_metric(repo_owner, repo_name, metric_name, metric_value)


def get_review_stats(repo_owner: str | None = None, repo_name: str | None = None, days: int = 30) -> dict[str, Any]:
    """Get review statistics."""
    collector = AnalyticsCollector()
    return collector.get_review_stats(repo_owner, repo_name, days)


def get_feedback_stats(repo_owner: str | None = None, repo_name: str | None = None, days: int = 30) -> dict[str, Any]:
    """Get feedback statistics."""
    collector = AnalyticsCollector()
    return collector.get_feedback_stats(repo_owner, repo_name, days)


def get_metrics(
    repo_owner: str | None = None, repo_name: str | None = None, metric_name: str | None = None, days: int = 30
) -> list[dict[str, Any]]:
    """Get metrics."""
    collector = AnalyticsCollector()
    return collector.get_metrics(repo_owner, repo_name, metric_name, days)


def get_top_issues(
    repo_owner: str | None = None, repo_name: str | None = None, days: int = 30, limit: int = 10
) -> list[dict[str, Any]]:
    """Get top issues by frequency."""
    collector = AnalyticsCollector()
    return collector.get_top_issues(repo_owner, repo_name, days, limit)


if __name__ == "__main__":
    # Example usage
    collector = AnalyticsCollector()

    # Start a review
    review_id = collector.start_review("example", "repo", 123)

    # Complete the review
    issues = [
        {
            "file_path": "src/main.py",
            "line_start": 10,
            "line_end": 12,
            "severity": "major",
            "category": "security",
            "rule_id": "security-vulnerability",
            "suggestion": {"replacement_text": "Fixed code"},
        },
        {
            "file_path": "src/utils.py",
            "line_start": 25,
            "severity": "minor",
            "category": "style",
            "rule_id": "style-convention",
        },
    ]

    collector.complete_review(review_id, issues, 2, 2, 1)

    # Record a suggestion being applied
    collector.record_suggestion_applied(review_id, "src/main.py", 10)

    # Record feedback
    collector.record_feedback(review_id, "helpful", 5, "Great suggestions!")

    # Record a metric
    collector.record_metric("example", "repo", "review_time_seconds", 120.5)

    # Get statistics
    stats = collector.get_review_stats("example", "repo")
