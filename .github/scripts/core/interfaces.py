"""
Core interfaces for the Kiro GitHub Integration system.

This module defines the abstract base classes that all implementations
must follow, ensuring loose coupling and testability.
"""

from abc import ABC, abstractmethod
from dataclasses import dataclass
from enum import Enum
from typing import Any


class IssueLevel(Enum):
    """Severity levels for code issues."""

    INFO = "info"
    WARNING = "warning"
    ERROR = "error"
    CRITICAL = "critical"


@dataclass
class CodeIssue:
    """Represents a code issue found during analysis."""

    level: IssueLevel
    category: str
    message: str
    file_path: str
    line_number: int | None = None
    column_number: int | None = None
    suggestion: str | None = None
    rule_id: str | None = None


@dataclass
class AnalysisResult:
    """Result of code analysis."""

    issues: list[CodeIssue]
    metrics: dict[str, Any]
    duration_ms: float
    analyzer_version: str


class DatabaseInterface(ABC):
    """Interface for database operations."""

    @abstractmethod
    async def connect(self) -> None:
        """Establish database connection."""
        pass

    @abstractmethod
    async def disconnect(self) -> None:
        """Close database connection."""
        pass

    @abstractmethod
    async def execute(self, query: str, params: dict[str, Any] | None = None) -> Any:
        """Execute a database query."""
        pass

    @abstractmethod
    async def transaction(self) -> "TransactionInterface":
        """Start a database transaction."""
        pass


class TransactionInterface(ABC):
    """Interface for database transactions."""

    @abstractmethod
    async def commit(self) -> None:
        """Commit the transaction."""
        pass

    @abstractmethod
    async def rollback(self) -> None:
        """Rollback the transaction."""
        pass

    @abstractmethod
    async def __aenter__(self):
        """Enter transaction context."""
        pass

    @abstractmethod
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        """Exit transaction context."""
        pass


class RepositoryInterface(ABC):
    """Base interface for repository pattern."""

    @abstractmethod
    async def find_by_id(self, entity_id: str) -> Any | None:
        """Find entity by ID."""
        pass

    @abstractmethod
    async def find_all(self, filters: dict[str, Any] | None = None) -> list[Any]:
        """Find all entities matching filters."""
        pass

    @abstractmethod
    async def save(self, entity: Any) -> str:
        """Save entity and return ID."""
        pass

    @abstractmethod
    async def update(self, entity_id: str, updates: dict[str, Any]) -> bool:
        """Update entity."""
        pass

    @abstractmethod
    async def delete(self, entity_id: str) -> bool:
        """Delete entity."""
        pass


class GitHubClientInterface(ABC):
    """Interface for GitHub API operations."""

    @abstractmethod
    async def get_pull_request(self, owner: str, repo: str, pr_number: int) -> dict[str, Any]:
        """Get pull request details."""
        pass

    @abstractmethod
    async def get_pr_files(self, owner: str, repo: str, pr_number: int) -> list[dict[str, Any]]:
        """Get files changed in a pull request."""
        pass

    @abstractmethod
    async def create_review(self, owner: str, repo: str, pr_number: int, review_data: dict[str, Any]) -> dict[str, Any]:
        """Create a pull request review."""
        pass

    @abstractmethod
    async def create_issue_comment(self, owner: str, repo: str, issue_number: int, body: str) -> dict[str, Any]:
        """Create an issue comment."""
        pass

    @abstractmethod
    async def get_rate_limit(self) -> dict[str, Any]:
        """Get current rate limit status."""
        pass


class AnalyzerInterface(ABC):
    """Interface for code analyzers."""

    @abstractmethod
    async def analyze(self, file_path: str, content: str, context: dict[str, Any] | None = None) -> AnalysisResult:
        """Analyze code and return results."""
        pass

    @abstractmethod
    def supports_file(self, file_path: str) -> bool:
        """Check if analyzer supports the file type."""
        pass

    @abstractmethod
    def get_capabilities(self) -> list[str]:
        """Get list of analyzer capabilities."""
        pass


class NotificationInterface(ABC):
    """Interface for notification services."""

    @abstractmethod
    async def send(self, recipient: str, subject: str, message: str, metadata: dict[str, Any] | None = None) -> bool:
        """Send a notification."""
        pass

    @abstractmethod
    async def send_batch(self, notifications: list[dict[str, Any]]) -> dict[str, bool]:
        """Send multiple notifications."""
        pass

    @abstractmethod
    def get_channel_type(self) -> str:
        """Get the notification channel type."""
        pass


class CacheInterface(ABC):
    """Interface for caching operations."""

    @abstractmethod
    async def get(self, key: str) -> Any | None:
        """Get value from cache."""
        pass

    @abstractmethod
    async def set(self, key: str, value: Any, ttl_seconds: int | None = None) -> bool:
        """Set value in cache with optional TTL."""
        pass

    @abstractmethod
    async def delete(self, key: str) -> bool:
        """Delete value from cache."""
        pass

    @abstractmethod
    async def exists(self, key: str) -> bool:
        """Check if key exists in cache."""
        pass

    @abstractmethod
    async def clear(self, pattern: str | None = None) -> int:
        """Clear cache entries matching pattern."""
        pass


class MetricsInterface(ABC):
    """Interface for metrics collection."""

    @abstractmethod
    def increment(self, metric: str, value: int = 1, tags: dict[str, str] | None = None) -> None:
        """Increment a counter metric."""
        pass

    @abstractmethod
    def gauge(self, metric: str, value: float, tags: dict[str, str] | None = None) -> None:
        """Set a gauge metric."""
        pass

    @abstractmethod
    def histogram(self, metric: str, value: float, tags: dict[str, str] | None = None) -> None:
        """Record a histogram metric."""
        pass

    @abstractmethod
    def timing(self, metric: str, duration_ms: float, tags: dict[str, str] | None = None) -> None:
        """Record a timing metric."""
        pass


class EncryptionInterface(ABC):
    """Interface for encryption operations."""

    @abstractmethod
    def encrypt(self, data: str | bytes) -> bytes:
        """Encrypt data."""
        pass

    @abstractmethod
    def decrypt(self, encrypted_data: bytes) -> str | bytes:
        """Decrypt data."""
        pass

    @abstractmethod
    def generate_key(self) -> bytes:
        """Generate a new encryption key."""
        pass


class AuthenticationInterface(ABC):
    """Interface for authentication operations."""

    @abstractmethod
    async def authenticate(self, credentials: dict[str, Any]) -> str | None:
        """Authenticate and return token."""
        pass

    @abstractmethod
    async def validate_token(self, token: str) -> dict[str, Any] | None:
        """Validate token and return claims."""
        pass

    @abstractmethod
    async def refresh_token(self, refresh_token: str) -> tuple[str, str] | None:
        """Refresh access token."""
        pass

    @abstractmethod
    async def revoke_token(self, token: str) -> bool:
        """Revoke a token."""
        pass


class AuthorizationInterface(ABC):
    """Interface for authorization operations."""

    @abstractmethod
    async def check_permission(self, user_id: str, resource: str, action: str) -> bool:
        """Check if user has permission for action on resource."""
        pass

    @abstractmethod
    async def grant_permission(self, user_id: str, resource: str, action: str) -> bool:
        """Grant permission to user."""
        pass

    @abstractmethod
    async def revoke_permission(self, user_id: str, resource: str, action: str) -> bool:
        """Revoke permission from user."""
        pass

    @abstractmethod
    async def get_user_permissions(self, user_id: str) -> list[dict[str, str]]:
        """Get all permissions for a user."""
        pass


class QueueInterface(ABC):
    """Interface for message queue operations."""

    @abstractmethod
    async def publish(self, queue_name: str, message: Any, priority: int = 0, delay_seconds: int = 0) -> str:
        """Publish message to queue."""
        pass

    @abstractmethod
    async def consume(self, queue_name: str, callback: callable, max_messages: int = 1) -> None:
        """Consume messages from queue."""
        pass

    @abstractmethod
    async def acknowledge(self, queue_name: str, message_id: str) -> bool:
        """Acknowledge message processing."""
        pass

    @abstractmethod
    async def get_queue_size(self, queue_name: str) -> int:
        """Get number of messages in queue."""
        pass


class ConfigurationInterface(ABC):
    """Interface for configuration management."""

    @abstractmethod
    def get(self, key: str, default: Any = None) -> Any:
        """Get configuration value."""
        pass

    @abstractmethod
    def set(self, key: str, value: Any) -> None:
        """Set configuration value."""
        pass

    @abstractmethod
    def reload(self) -> None:
        """Reload configuration from source."""
        pass

    @abstractmethod
    def validate(self) -> list[str]:
        """Validate configuration and return errors."""
        pass
