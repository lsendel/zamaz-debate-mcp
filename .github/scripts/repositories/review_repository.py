"""
Repository for managing code review data.

This repository handles persistence of code reviews, comments,
and related analytics data.
"""

from dataclasses import dataclass, field
from datetime import datetime
from typing import Any

from .base_repository import BaseRepository, Entity


@dataclass
class Review(Entity):
    """Code review entity."""

    id: str
    pr_number: int
    repo_owner: str
    repo_name: str
    review_type: str
    status: str
    created_at: datetime
    updated_at: datetime
    completed_at: datetime | None = None
    reviewer: str = "kiro-ai"
    commit_sha: str | None = None
    review_depth: str = "standard"
    issues_found: int = 0
    suggestions_made: int = 0
    files_reviewed: int = 0
    processing_time_ms: int | None = None
    metadata: dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> dict[str, Any]:
        """Convert to dictionary for storage."""
        return {
            "id": self.id,
            "pr_number": self.pr_number,
            "repo_owner": self.repo_owner,
            "repo_name": self.repo_name,
            "review_type": self.review_type,
            "status": self.status,
            "created_at": self.created_at.isoformat(),
            "updated_at": self.updated_at.isoformat(),
            "completed_at": self.completed_at.isoformat() if self.completed_at else None,
            "reviewer": self.reviewer,
            "commit_sha": self.commit_sha,
            "review_depth": self.review_depth,
            "issues_found": self.issues_found,
            "suggestions_made": self.suggestions_made,
            "files_reviewed": self.files_reviewed,
            "processing_time_ms": self.processing_time_ms,
            "metadata": json.dumps(self.metadata) if self.metadata else "{}",
        }

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> "Review":
        """Create from dictionary."""
        # Parse dates
        data["created_at"] = datetime.fromisoformat(data["created_at"])
        data["updated_at"] = datetime.fromisoformat(data["updated_at"])
        if data.get("completed_at"):
            data["completed_at"] = datetime.fromisoformat(data["completed_at"])

        # Parse metadata
        if isinstance(data.get("metadata"), str):
            data["metadata"] = json.loads(data["metadata"])

        return cls(**data)


@dataclass
class ReviewComment(Entity):
    """Review comment entity."""

    id: str
    review_id: str
    file_path: str
    line_number: int
    comment_type: str  # issue, suggestion, praise
    severity: str  # info, warning, error, critical
    message: str
    suggestion: str | None = None
    rule_id: str | None = None
    created_at: datetime | None = None

    def to_dict(self) -> dict[str, Any]:
        """Convert to dictionary."""
        return {
            "id": self.id,
            "review_id": self.review_id,
            "file_path": self.file_path,
            "line_number": self.line_number,
            "comment_type": self.comment_type,
            "severity": self.severity,
            "message": self.message,
            "suggestion": self.suggestion,
            "rule_id": self.rule_id,
            "created_at": self.created_at.isoformat() if self.created_at else datetime.utcnow().isoformat(),
        }

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> "ReviewComment":
        """Create from dictionary."""
        if data.get("created_at") and isinstance(data["created_at"], str):
            data["created_at"] = datetime.fromisoformat(data["created_at"])
        return cls(**data)


class ReviewRepository(BaseRepository):
    """Repository for review entities."""

    def __init__(self, database_path: str):
        super().__init__(database_path, "reviews")
        self.set_entity_class(Review)

    async def initialize(self) -> None:
        """Create tables if they don't exist."""
        # Reviews table
        await self.create_table("""
            CREATE TABLE IF NOT EXISTS reviews (
                id TEXT PRIMARY KEY,
                pr_number INTEGER NOT NULL,
                repo_owner TEXT NOT NULL,
                repo_name TEXT NOT NULL,
                review_type TEXT NOT NULL,
                status TEXT NOT NULL,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                completed_at TEXT,
                reviewer TEXT NOT NULL,
                commit_sha TEXT,
                review_depth TEXT NOT NULL,
                issues_found INTEGER DEFAULT 0,
                suggestions_made INTEGER DEFAULT 0,
                files_reviewed INTEGER DEFAULT 0,
                processing_time_ms INTEGER,
                metadata TEXT DEFAULT '{}'
            )
        """)

        # Create indexes
        await self.create_index("idx_reviews_pr", ["repo_owner", "repo_name", "pr_number"])
        await self.create_index("idx_reviews_status", ["status"])
        await self.create_index("idx_reviews_created", ["created_at"])

    async def find_by_pr(self, repo_owner: str, repo_name: str, pr_number: int) -> list[Review]:
        """Find all reviews for a pull request."""
        return await self.find_all(
            {"repo_owner": repo_owner, "repo_name": repo_name, "pr_number": pr_number}, order_by="created_at DESC"
        )

    async def find_latest_by_pr(self, repo_owner: str, repo_name: str, pr_number: int) -> Review | None:
        """Find the latest review for a pull request."""
        reviews = await self.find_by_pr(repo_owner, repo_name, pr_number)
        return reviews[0] if reviews else None

    async def find_pending(self, limit: int = 10) -> list[Review]:
        """Find pending reviews."""
        return await self.find_all({"status": "pending"}, order_by="created_at ASC", limit=limit)

    async def get_statistics(
        self, repo_owner: str | None = None, repo_name: str | None = None, days: int = 30
    ) -> dict[str, Any]:
        """Get review statistics."""
        # Build query
        query_parts = [
            """
            SELECT
                COUNT(*) as total_reviews,
                AVG(issues_found) as avg_issues,
                AVG(suggestions_made) as avg_suggestions,
                AVG(files_reviewed) as avg_files,
                AVG(processing_time_ms) as avg_processing_time,
                SUM(issues_found) as total_issues,
                SUM(suggestions_made) as total_suggestions
            FROM reviews
            WHERE created_at > datetime('now', '-' || ? || ' days')
        """
        ]

        params = {"days": days}

        if repo_owner and repo_name:
            query_parts.append("AND repo_owner = :owner AND repo_name = :name")
            params["owner"] = repo_owner
            params["name"] = repo_name

        query = " ".join(query_parts)
        result = await self.database.execute(query, params)

        if result and result[0]:
            row = result[0]
            return {
                "total_reviews": row[0] or 0,
                "avg_issues_per_review": round(row[1] or 0, 2),
                "avg_suggestions_per_review": round(row[2] or 0, 2),
                "avg_files_per_review": round(row[3] or 0, 2),
                "avg_processing_time_ms": round(row[4] or 0),
                "total_issues": row[5] or 0,
                "total_suggestions": row[6] or 0,
            }

        return {
            "total_reviews": 0,
            "avg_issues_per_review": 0,
            "avg_suggestions_per_review": 0,
            "avg_files_per_review": 0,
            "avg_processing_time_ms": 0,
            "total_issues": 0,
            "total_suggestions": 0,
        }


class ReviewCommentRepository(BaseRepository):
    """Repository for review comment entities."""

    def __init__(self, database_path: str):
        super().__init__(database_path, "review_comments")
        self.set_entity_class(ReviewComment)

    async def initialize(self) -> None:
        """Create tables if they don't exist."""
        await self.create_table("""
            CREATE TABLE IF NOT EXISTS review_comments (
                id TEXT PRIMARY KEY,
                review_id TEXT NOT NULL,
                file_path TEXT NOT NULL,
                line_number INTEGER NOT NULL,
                comment_type TEXT NOT NULL,
                severity TEXT NOT NULL,
                message TEXT NOT NULL,
                suggestion TEXT,
                rule_id TEXT,
                created_at TEXT NOT NULL,
                FOREIGN KEY (review_id) REFERENCES reviews(id)
            )
        """)

        # Create indexes
        await self.create_index("idx_comments_review", ["review_id"])
        await self.create_index("idx_comments_type", ["comment_type"])
        await self.create_index("idx_comments_severity", ["severity"])

    async def find_by_review(self, review_id: str) -> list[ReviewComment]:
        """Find all comments for a review."""
        return await self.find_all({"review_id": review_id}, order_by="file_path, line_number")

    async def save_batch(self, comments: list[ReviewComment]) -> list[str]:
        """Save multiple comments in a transaction."""
        ids = []
        async with self.transaction():
            for comment in comments:
                comment_id = await self.save(comment)
                ids.append(comment_id)
        return ids

    async def get_comment_distribution(self, review_id: str) -> dict[str, int]:
        """Get distribution of comment types and severities."""
        query = """
            SELECT
                comment_type,
                severity,
                COUNT(*) as count
            FROM review_comments
            WHERE review_id = ?
            GROUP BY comment_type, severity
        """

        result = await self.database.execute(query, {"review_id": review_id})

        distribution = {"by_type": {}, "by_severity": {}}

        for row in result:
            comment_type, severity, count = row
            distribution["by_type"][comment_type] = distribution["by_type"].get(comment_type, 0) + count
            distribution["by_severity"][severity] = distribution["by_severity"].get(severity, 0) + count

        return distribution


# Import json at the top of file
import json
