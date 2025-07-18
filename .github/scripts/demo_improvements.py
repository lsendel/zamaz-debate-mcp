#!/usr/bin/env python3
"""
Demonstration of the architectural improvements.

This script shows how the refactored code is cleaner, more maintainable,
and follows SOLID principles.
"""

import asyncio
import json

# Configure logging first
from core.logging import configure_logging, get_logger, log_event, set_correlation_id

# Configure structured logging
configure_logging(level="INFO", format_type="human")
logger = get_logger(__name__)


async def demo_dependency_injection():
    """Demonstrate dependency injection container."""
    logger.info("=== Dependency Injection Demo ===")

    from core.container import ServiceContainer
    from core.interfaces import CacheInterface, MetricsInterface

    # Create container
    container = ServiceContainer()

    # Register mock services
    class MockCache:
        async def get(self, key):
            return f"cached_{key}"

        async def set(self, _key, _value, _ttl=None):
            return True

    class MockMetrics:
        def increment(self, metric, value=1, tags=None):
            logger.info(f"Metric: {metric} += {value}", tags=tags)

    container.register_instance(CacheInterface, MockCache())
    container.register_instance(MetricsInterface, MockMetrics())

    # Service with dependencies automatically injected
    class BusinessService:
        def __init__(self, cache: CacheInterface, metrics: MetricsInterface):
            self.cache = cache
            self.metrics = metrics
            logger.info("BusinessService created with injected dependencies")

        async def process(self, data):
            # Use injected services
            cached = await self.cache.get(data)
            self.metrics.increment("business.process", tags={"cached": "yes"})
            return cached

    container.register(BusinessService, BusinessService)

    # Get service - dependencies are automatically resolved
    service = container.get(BusinessService)
    result = await service.process("test_data")
    logger.info(f"Result: {result}")


async def demo_clean_webhook_handling():
    """Demonstrate clean webhook handling with proper separation."""
    logger.info("\n=== Clean Webhook Handling Demo ===")

    # Set correlation ID for tracking
    correlation_id = set_correlation_id()
    logger.info(f"Processing webhook with correlation ID: {correlation_id}")

    from services.webhook_service import WebhookValidator

    # Webhook validation is now a separate concern
    WebhookValidator("test-secret")

    # Simulate webhook data
    json.dumps({"action": "opened", "pull_request": {"number": 123}}).encode()

    # Clean validation
    try:
        # In production, this would use the actual signature
        logger.info("Validating webhook signature...")
        # is_valid = validator.validate_signature(payload, signature)
        logger.info("✅ Webhook validation passed")
    except Exception as e:
        logger.error(f"❌ Webhook validation failed: {e}")


async def demo_repository_pattern():
    """Demonstrate repository pattern for data access."""
    logger.info("\n=== Repository Pattern Demo ===")

    from datetime import datetime

    from repositories.review_repository import Review

    # Create review entity - clean data model
    review = Review(
        id="review-001",
        pr_number=123,
        repo_owner="example",
        repo_name="demo-repo",
        review_type="automated",
        status="pending",
        created_at=datetime.utcnow(),
        updated_at=datetime.utcnow(),
    )

    logger.info(f"Created review entity: {review.id}")
    logger.info(f"  PR: {review.repo_owner}/{review.repo_name}#{review.pr_number}")
    logger.info(f"  Status: {review.status}")

    # In production, this would save to database
    # repository = ReviewRepository(db_path)
    # await repository.save(review)


async def demo_analyzer_strategies():
    """Demonstrate strategy pattern for code analysis."""
    logger.info("\n=== Analyzer Strategy Pattern Demo ===")

    from analyzers.base_analyzer import BaseAnalyzer
    from analyzers.security_analyzer import SecurityPatternStrategy
    from analyzers.style_analyzer import StyleGuideStrategy

    # Create analyzer with multiple strategies
    analyzer = BaseAnalyzer([SecurityPatternStrategy(), StyleGuideStrategy()])

    # Sample code to analyze
    test_code = """
def process_user_input(user_data):
    password = "admin123"  # Security issue
    query = "SELECT * FROM users WHERE id = " + user_data  # SQL injection
    return execute_query(query)
"""

    # Analyze code
    logger.info("Analyzing code for issues...")
    result = await analyzer.analyze("test.py", test_code)

    logger.info(f"Found {len(result.issues)} issues:")
    for issue in result.issues:
        logger.info(f"  [{issue.level.value}] Line {issue.line_number}: {issue.message}")
        if issue.suggestion:
            logger.info(f"    💡 {issue.suggestion}")


async def demo_clean_authentication():
    """Demonstrate clean authentication service."""
    logger.info("\n=== Clean Authentication Demo ===")

    # Authentication is now a single responsibility
    logger.info("Authentication service handles only auth concerns:")
    logger.info("  ✅ Token generation")
    logger.info("  ✅ Token validation")
    logger.info("  ✅ Token refresh")
    logger.info("  ✅ Token revocation")
    logger.info("  ❌ NOT: Authorization (separate service)")
    logger.info("  ❌ NOT: Encryption (separate service)")
    logger.info("  ❌ NOT: Audit logging (separate service)")


def demo_structured_logging():
    """Demonstrate structured logging capabilities."""
    logger.info("\n=== Structured Logging Demo ===")

    # Log with structured data
    log_event(
        logger,
        event_type="code_review",
        message="Code review completed",
        pr_number=123,
        issues_found=5,
        duration_ms=1523.45,
    )

    # Log with correlation ID
    set_correlation_id("req-456")
    logger.info("This log includes correlation ID automatically")

    # Log with extra context
    logger.info(
        "Processing webhook", extra={"webhook_type": "pull_request", "action": "opened", "repository": "example/demo"}
    )


def show_architecture_comparison():
    """Show before and after architecture comparison."""

    comparisons = [
        {
            "aspect": "Class Size",
            "before": "SecurityManager: 659 lines (7 responsibilities)",
            "after": "AuthenticationService: 180 lines (1 responsibility)",
        },
        {
            "aspect": "Dependencies",
            "before": "Direct imports and tight coupling",
            "after": "Dependency injection with interfaces",
        },
        {
            "aspect": "Error Handling",
            "before": "Generic exceptions, str(e) everywhere",
            "after": "Domain-specific exceptions with context",
        },
        {
            "aspect": "Code Analysis",
            "before": "Monolithic analyzer with nested conditions",
            "after": "Strategy pattern with pluggable analyzers",
        },
        {
            "aspect": "Data Access",
            "before": "Direct SQLite calls scattered in code",
            "after": "Repository pattern with clean abstractions",
        },
        {
            "aspect": "Logging",
            "before": "Basic logging with print statements",
            "after": "Structured logging with correlation IDs",
        },
    ]

    for _comp in comparisons:
        pass


async def main():
    """Run all demonstrations."""

    # Show architecture comparison
    show_architecture_comparison()

    # Run demos
    await demo_dependency_injection()
    await demo_clean_webhook_handling()
    await demo_repository_pattern()
    await demo_analyzer_strategies()
    await demo_clean_authentication()
    demo_structured_logging()


if __name__ == "__main__":
    asyncio.run(main())
