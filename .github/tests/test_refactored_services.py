"""
Comprehensive tests for refactored services.

This module tests the new architecture including dependency injection,
repository pattern, and strategy pattern implementations.
"""

from datetime import datetime
from unittest.mock import AsyncMock, Mock, patch

import pytest

from ..scripts.analyzers.base_analyzer import AnalyzerContext, BaseAnalyzer
from ..scripts.analyzers.security_analyzer import DependencySecurityStrategy, SecurityPatternStrategy
from ..scripts.core.container import ServiceContainer
from ..scripts.core.exceptions import AuthenticationError, WebhookValidationError
from ..scripts.core.interfaces import (
    CacheInterface,
    DatabaseInterface,
    GitHubClientInterface,
    IssueLevel,
    MetricsInterface,
    QueueInterface,
)
from ..scripts.repositories.review_repository import Review, ReviewRepository
from ..scripts.services.authentication_service import AuthenticationService, JWTConfiguration
from ..scripts.services.webhook_service import WebhookService, WebhookValidator


class TestServiceContainer:
    """Test dependency injection container."""

    def test_register_and_resolve(self):
        """Test service registration and resolution."""
        container = ServiceContainer()

        # Register a mock service
        mock_cache = Mock(spec=CacheInterface)
        container.register_instance(CacheInterface, mock_cache)

        # Resolve service
        resolved = container.get(CacheInterface)
        assert resolved is mock_cache

    def test_singleton_behavior(self):
        """Test singleton service behavior."""
        container = ServiceContainer()

        # Create a simple service class
        class TestService:
            def __init__(self):
                self.id = id(self)

        # Register as singleton
        container.register(TestService, TestService, singleton=True)

        # Get multiple instances
        instance1 = container.get(TestService)
        instance2 = container.get(TestService)

        assert instance1 is instance2

    def test_transient_behavior(self):
        """Test transient service behavior."""
        container = ServiceContainer()

        class TestService:
            def __init__(self):
                self.id = id(self)

        # Register as transient
        container.register(TestService, TestService, singleton=False)

        # Get multiple instances
        instance1 = container.get(TestService)
        instance2 = container.get(TestService)

        assert instance1 is not instance2

    def test_dependency_injection(self):
        """Test automatic dependency injection."""
        container = ServiceContainer()

        # Create mock dependencies
        mock_cache = Mock(spec=CacheInterface)
        mock_db = Mock(spec=DatabaseInterface)

        container.register_instance(CacheInterface, mock_cache)
        container.register_instance(DatabaseInterface, mock_db)

        # Create service with dependencies
        class TestService:
            def __init__(self, cache: CacheInterface, database: DatabaseInterface):
                self.cache = cache
                self.database = database

        container.register(TestService, TestService)

        # Resolve service
        service = container.get(TestService)

        assert service.cache is mock_cache
        assert service.database is mock_db


class TestWebhookService:
    """Test refactored webhook service."""

    @pytest.fixture
    def mock_dependencies(self):
        """Create mock dependencies."""
        return {
            "validator": WebhookValidator("test-secret"),
            "queue": AsyncMock(spec=QueueInterface),
            "cache": AsyncMock(spec=CacheInterface),
            "metrics": Mock(spec=MetricsInterface),
            "github_client": AsyncMock(spec=GitHubClientInterface),
        }

    @pytest.fixture
    def webhook_service(self, mock_dependencies):
        """Create webhook service with mocks."""
        return WebhookService(**mock_dependencies)

    @pytest.mark.asyncio
    async def test_valid_webhook_processing(self, webhook_service, mock_dependencies):
        """Test processing valid webhook."""
        # Prepare test data
        headers = {
            "X-GitHub-Event": "pull_request",
            "X-GitHub-Delivery": "test-delivery-123",
            "X-Hub-Signature-256": "sha256=valid_signature",
        }

        payload = {
            "action": "opened",
            "pull_request": {"number": 123, "assignees": [{"login": "kiro-ai"}], "labels": []},
            "repository": {"owner": {"login": "test-org"}, "name": "test-repo"},
            "sender": {"login": "test-user"},
        }

        body = json.dumps(payload).encode("utf-8")

        # Mock validator
        with patch.object(webhook_service.validator, "validate_signature", return_value=True):
            # Mock cache
            mock_dependencies["cache"].exists.return_value = False
            mock_dependencies["queue"].publish.return_value = "msg-123"

            # Process webhook
            result = await webhook_service.process_webhook(headers, body)

            # Verify result
            assert result["status"] == "processed"
            assert result["event_type"] == "pull_request"
            assert result["result"]["processed"] is True
            assert result["result"]["message_id"] == "msg-123"

            # Verify metrics were recorded
            mock_dependencies["metrics"].increment.assert_called()

            # Verify message was queued
            mock_dependencies["queue"].publish.assert_called_once()

    @pytest.mark.asyncio
    async def test_invalid_signature(self, webhook_service):
        """Test webhook with invalid signature."""
        headers = {
            "X-GitHub-Event": "pull_request",
            "X-GitHub-Delivery": "test-delivery-123",
            "X-Hub-Signature-256": "sha256=invalid_signature",
        }

        body = b'{"test": "data"}'

        with pytest.raises(WebhookValidationError) as exc_info:
            await webhook_service.process_webhook(headers, body)

        assert "Invalid signature" in str(exc_info.value)

    @pytest.mark.asyncio
    async def test_duplicate_delivery(self, webhook_service, mock_dependencies):
        """Test duplicate webhook delivery handling."""
        headers = {
            "X-GitHub-Event": "pull_request",
            "X-GitHub-Delivery": "duplicate-123",
            "X-Hub-Signature-256": "sha256=valid_signature",
        }

        body = b'{"test": "data"}'

        # Mock duplicate detection
        mock_dependencies["cache"].exists.return_value = True

        with patch.object(webhook_service.validator, "validate_signature", return_value=True):
            result = await webhook_service.process_webhook(headers, body)

            assert result["status"] == "duplicate"
            assert result["delivery_id"] == "duplicate-123"


class TestAuthenticationService:
    """Test refactored authentication service."""

    @pytest.fixture
    def jwt_config(self):
        """Create JWT configuration."""
        return JWTConfiguration(
            secret_key="test-secret-key-that-is-long-enough-32chars",
            access_token_expire_minutes=30,
            refresh_token_expire_days=30,
        )

    @pytest.fixture
    def mock_dependencies(self):
        """Create mock dependencies."""
        return {"database": AsyncMock(spec=DatabaseInterface), "cache": AsyncMock(spec=CacheInterface)}

    @pytest.fixture
    def auth_service(self, jwt_config, mock_dependencies):
        """Create authentication service."""
        return AuthenticationService(jwt_config, **mock_dependencies)

    @pytest.mark.asyncio
    async def test_api_key_authentication(self, auth_service, mock_dependencies):
        """Test API key authentication."""
        # Mock database response
        mock_dependencies["database"].execute.return_value = [
            ("user-123", None)  # user_id, expires_at
        ]

        # Authenticate
        token = await auth_service.authenticate({"api_key": "test-api-key"})

        assert token is not None
        assert isinstance(token, str)

        # Verify token
        claims = await auth_service.validate_token(token)
        assert claims is not None
        assert claims["sub"] == "user-123"
        assert claims["type"] == "access"

    @pytest.mark.asyncio
    async def test_invalid_api_key(self, auth_service, mock_dependencies):
        """Test invalid API key authentication."""
        # Mock empty database response
        mock_dependencies["database"].execute.return_value = []

        with pytest.raises(AuthenticationError) as exc_info:
            await auth_service.authenticate({"api_key": "invalid-key"})

        assert "Invalid API key" in str(exc_info.value)

    @pytest.mark.asyncio
    async def test_token_refresh(self, auth_service, mock_dependencies):
        """Test token refresh functionality."""
        # Create initial tokens
        mock_dependencies["database"].execute.return_value = [("user-123", None)]
        initial_token = await auth_service.authenticate({"api_key": "test-key"})

        # Get refresh token (would normally be returned with access token)
        refresh_token = await auth_service._generate_refresh_token("user-123")

        # Mock cache for revocation check
        mock_dependencies["cache"].exists.return_value = False

        # Refresh tokens
        new_access, new_refresh = await auth_service.refresh_token(refresh_token)

        assert new_access is not None
        assert new_refresh is not None
        assert new_access != initial_token
        assert new_refresh != refresh_token

    @pytest.mark.asyncio
    async def test_token_revocation(self, auth_service, mock_dependencies):
        """Test token revocation."""
        # Create token
        token = await auth_service._generate_access_token("user-123")

        # Revoke token
        result = await auth_service.revoke_token(token)
        assert result is True

        # Verify cache was called
        mock_dependencies["cache"].set.assert_called()

        # Check if token is revoked
        mock_dependencies["cache"].exists.return_value = True
        claims = await auth_service.validate_token(token)
        assert claims is None


class TestRepositoryPattern:
    """Test repository pattern implementation."""

    @pytest.fixture
    def mock_database(self):
        """Create mock database."""
        return AsyncMock(spec=DatabaseInterface)

    @pytest.fixture
    def review_repository(self, mock_database):
        """Create review repository with mock database."""
        repo = ReviewRepository(":memory:")
        repo.database = mock_database
        return repo

    @pytest.mark.asyncio
    async def test_save_review(self, review_repository, mock_database):
        """Test saving review entity."""
        # Create review
        review = Review(
            id="review-123",
            pr_number=456,
            repo_owner="test-org",
            repo_name="test-repo",
            review_type="automated",
            status="completed",
            created_at=datetime.utcnow(),
            updated_at=datetime.utcnow(),
        )

        # Mock database execution
        mock_database.execute.return_value = None

        # Save review
        review_id = await review_repository.save(review)

        assert review_id == "review-123"
        mock_database.execute.assert_called_once()

    @pytest.mark.asyncio
    async def test_find_by_pr(self, review_repository, mock_database):
        """Test finding reviews by PR."""
        # Mock database response
        mock_database.execute.return_value = [
            {
                "id": "review-123",
                "pr_number": 456,
                "repo_owner": "test-org",
                "repo_name": "test-repo",
                "review_type": "automated",
                "status": "completed",
                "created_at": "2024-01-01T00:00:00",
                "updated_at": "2024-01-01T00:00:00",
                "completed_at": None,
                "reviewer": "kiro-ai",
                "commit_sha": None,
                "review_depth": "standard",
                "issues_found": 5,
                "suggestions_made": 3,
                "files_reviewed": 10,
                "processing_time_ms": 1500,
                "metadata": "{}",
            }
        ]

        # Find reviews
        reviews = await review_repository.find_by_pr("test-org", "test-repo", 456)

        assert len(reviews) == 1
        assert reviews[0].pr_number == 456
        assert reviews[0].issues_found == 5


class TestAnalyzerStrategies:
    """Test analyzer strategy pattern."""

    @pytest.fixture
    def security_strategy(self):
        """Create security pattern strategy."""
        return SecurityPatternStrategy()

    @pytest.fixture
    def analyzer_context(self):
        """Create analyzer context."""
        return AnalyzerContext(
            file_path="test.py", content='password = "hardcoded123"\nprint(password)', language="python"
        )

    @pytest.mark.asyncio
    async def test_security_pattern_detection(self, security_strategy, analyzer_context):
        """Test security pattern detection."""
        issues = await security_strategy.analyze(analyzer_context)

        assert len(issues) > 0

        # Check for hardcoded password issue
        password_issues = [i for i in issues if "hardcoded" in i.message.lower()]
        assert len(password_issues) == 1
        assert password_issues[0].level == IssueLevel.CRITICAL
        assert password_issues[0].line_number == 1

    @pytest.mark.asyncio
    async def test_composite_analyzer(self):
        """Test composite analyzer with multiple strategies."""
        # Create analyzer with multiple strategies
        analyzer = BaseAnalyzer([SecurityPatternStrategy(), DependencySecurityStrategy()])

        # Test Python code
        result = await analyzer.analyze("test.py", "import pickle\ndata = pickle.loads(user_input)", {})

        assert len(result.issues) > 0
        assert result.analyzer_version == "2.0.0"
        assert "security-pattern" in result.metrics["strategy_results"]


# Import json for test data
import json
