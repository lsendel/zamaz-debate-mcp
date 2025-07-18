"""
Custom exceptions for the Kiro GitHub Integration system.

This module defines a hierarchy of exceptions for better error handling
and debugging.
"""

from typing import Any


class KiroException(Exception):
    """Base exception for all Kiro-related errors."""

    def __init__(self, message: str, details: dict[str, Any] | None = None):
        super().__init__(message)
        self.message = message
        self.details = details or {}

    def to_dict(self) -> dict[str, Any]:
        """Convert exception to dictionary for API responses."""
        return {"error": self.__class__.__name__, "message": self.message, "details": self.details}


# Configuration Exceptions
class ConfigurationError(KiroException):
    """Raised when configuration is invalid or missing."""

    pass


class ValidationError(ConfigurationError):
    """Raised when validation fails."""

    def __init__(self, field: str, value: Any, message: str):
        super().__init__(f"Validation failed for {field}: {message}", {"field": field, "value": value})


# GitHub API Exceptions
class GitHubError(KiroException):
    """Base exception for GitHub-related errors."""

    pass


class GitHubAPIError(GitHubError):
    """Raised when GitHub API returns an error."""

    def __init__(self, status_code: int, message: str, response_data: dict | None = None):
        super().__init__(
            f"GitHub API error {status_code}: {message}", {"status_code": status_code, "response": response_data}
        )
        self.status_code = status_code


class GitHubRateLimitError(GitHubAPIError):
    """Raised when GitHub rate limit is exceeded."""

    def __init__(self, reset_time: int, limit: int, remaining: int):
        super().__init__(
            403,
            f"Rate limit exceeded. Resets at {reset_time}",
            {"limit": limit, "remaining": remaining, "reset": reset_time},
        )


class GitHubAuthenticationError(GitHubAPIError):
    """Raised when GitHub authentication fails."""

    def __init__(self, message: str = "Authentication failed"):
        super().__init__(401, message)


# Analysis Exceptions
class AnalysisError(KiroException):
    """Base exception for code analysis errors."""

    pass


class FileNotSupportedError(AnalysisError):
    """Raised when file type is not supported for analysis."""

    def __init__(self, file_path: str, file_type: str):
        super().__init__(
            f"File type '{file_type}' not supported for {file_path}", {"file_path": file_path, "file_type": file_type}
        )


class AnalysisTimeoutError(AnalysisError):
    """Raised when analysis takes too long."""

    def __init__(self, file_path: str, timeout_seconds: int):
        super().__init__(
            f"Analysis of {file_path} timed out after {timeout_seconds}s",
            {"file_path": file_path, "timeout": timeout_seconds},
        )


# Database Exceptions
class DatabaseError(KiroException):
    """Base exception for database-related errors."""

    pass


class ConnectionError(DatabaseError):
    """Raised when database connection fails."""

    pass


class TransactionError(DatabaseError):
    """Raised when database transaction fails."""

    pass


class EntityNotFoundError(DatabaseError):
    """Raised when entity is not found in database."""

    def __init__(self, entity_type: str, entity_id: str):
        super().__init__(
            f"{entity_type} with id '{entity_id}' not found", {"entity_type": entity_type, "entity_id": entity_id}
        )


# Security Exceptions
class SecurityError(KiroException):
    """Base exception for security-related errors."""

    pass


class AuthenticationError(SecurityError):
    """Raised when authentication fails."""

    pass


class AuthorizationError(SecurityError):
    """Raised when authorization fails."""

    def __init__(self, user_id: str, resource: str, action: str):
        super().__init__(
            f"User '{user_id}' not authorized to {action} on {resource}",
            {"user_id": user_id, "resource": resource, "action": action},
        )


class TokenError(SecurityError):
    """Raised when token validation fails."""

    pass


class EncryptionError(SecurityError):
    """Raised when encryption/decryption fails."""

    pass


# Service Exceptions
class ServiceError(KiroException):
    """Base exception for service-related errors."""

    pass


class ServiceUnavailableError(ServiceError):
    """Raised when a required service is unavailable."""

    def __init__(self, service_name: str, reason: str | None = None):
        message = f"Service '{service_name}' is unavailable"
        if reason:
            message += f": {reason}"
        super().__init__(message, {"service": service_name})


class ExternalServiceError(ServiceError):
    """Raised when an external service call fails."""

    pass


# Webhook Exceptions
class WebhookError(KiroException):
    """Base exception for webhook-related errors."""

    pass


class WebhookValidationError(WebhookError):
    """Raised when webhook validation fails."""

    def __init__(self, reason: str):
        super().__init__(f"Webhook validation failed: {reason}")


class WebhookProcessingError(WebhookError):
    """Raised when webhook processing fails."""

    pass


# Queue Exceptions
class QueueError(KiroException):
    """Base exception for queue-related errors."""

    pass


class MessagePublishError(QueueError):
    """Raised when message publishing fails."""

    pass


class MessageConsumeError(QueueError):
    """Raised when message consumption fails."""

    pass


# Notification Exceptions
class NotificationError(KiroException):
    """Base exception for notification-related errors."""

    pass


class NotificationDeliveryError(NotificationError):
    """Raised when notification delivery fails."""

    def __init__(self, channel: str, recipient: str, reason: str):
        super().__init__(
            f"Failed to deliver notification via {channel} to {recipient}: {reason}",
            {"channel": channel, "recipient": recipient},
        )
