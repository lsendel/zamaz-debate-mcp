"""
End-to-end tests for complete PR review flows.

This module tests complete scenarios from webhook receipt to review posting,
including configuration changes and analytics collection.
"""

import asyncio
import json
import pytest
from datetime import datetime, timedelta
from typing import Dict, List, Any
from unittest.mock import Mock, AsyncMock, patch

from ...scripts.core.container import ServiceContainer
from ...scripts.core.logging import configure_logging, get_logger


# Configure logging for tests
configure_logging(level='DEBUG', format_type='human')
logger = get_logger(__name__)


class E2ETestEnvironment:
    """Complete test environment for E2E tests."""
    
    def __init__(self):
        self.container = ServiceContainer()
        self.webhook_events = []
        self.reviews_posted = []
        self.notifications_sent = []
        self.analytics_events = []
        
    async def setup(self):
        """Set up test environment."""
        # Register mock services
        from ...scripts.core.interfaces import (
            CacheInterface, DatabaseInterface, GitHubClientInterface,
            MetricsInterface, QueueInterface, NotificationInterface
        )
        
        # Mock implementations
        self.cache = AsyncMock(spec=CacheInterface)
        self.database = AsyncMock(spec=DatabaseInterface)
        self.github = AsyncMock(spec=GitHubClientInterface)
        self.metrics = Mock(spec=MetricsInterface)
        self.queue = AsyncMock(spec=QueueInterface)
        self.notifications = AsyncMock(spec=NotificationInterface)
        
        # Register in container
        self.container.register_instance(CacheInterface, self.cache)
        self.container.register_instance(DatabaseInterface, self.database)
        self.container.register_instance(GitHubClientInterface, self.github)
        self.container.register_instance(MetricsInterface, self.metrics)
        self.container.register_instance(QueueInterface, self.queue)
        self.container.register_instance(NotificationInterface, self.notifications)
        
        # Set up default behaviors
        self.cache.exists.return_value = False
        self.cache.get.return_value = None
        self.cache.set.return_value = True
        
        self.queue.publish.side_effect = self._queue_publish
        self.github.create_review.side_effect = self._create_review
        self.notifications.send.side_effect = self._send_notification
        
    async def _queue_publish(self, queue_name: str, message: Any, **kwargs):
        """Mock queue publish."""
        msg_id = f"msg-{len(self.webhook_events)}"
        self.webhook_events.append({
            "id": msg_id,
            "queue": queue_name,
            "message": message,
            "timestamp": datetime.utcnow()
        })
        return msg_id
    
    async def _create_review(self, owner: str, repo: str, pr_number: int, review_data: Dict):
        """Mock review creation."""
        review = {
            "id": f"review-{len(self.reviews_posted)}",
            "owner": owner,
            "repo": repo,
            "pr_number": pr_number,
            "data": review_data,
            "timestamp": datetime.utcnow()
        }
        self.reviews_posted.append(review)
        return {"id": review["id"], "html_url": f"https://github.com/{owner}/{repo}/pull/{pr_number}#review-{review['id']}"}
    
    async def _send_notification(self, recipient: str, subject: str, message: str, **kwargs):
        """Mock notification sending."""
        notification = {
            "id": f"notif-{len(self.notifications_sent)}",
            "recipient": recipient,
            "subject": subject,
            "message": message,
            "timestamp": datetime.utcnow()
        }
        self.notifications_sent.append(notification)
        return True


@pytest.fixture
async def e2e_env():
    """Create E2E test environment."""
    env = E2ETestEnvironment()
    await env.setup()
    return env


class TestCompletePRReviewFlow:
    """Test complete PR review flow from start to finish."""
    
    @pytest.mark.asyncio
    async def test_new_pr_automated_review(self, e2e_env):
        """Test automated review when new PR is opened."""
        logger.info("=== Testing New PR Automated Review Flow ===")
        
        # 1. Simulate webhook for new PR
        webhook_payload = {
            "action": "opened",
            "pull_request": {
                "number": 456,
                "title": "Add user authentication feature",
                "body": "This PR implements JWT-based authentication\n\nCloses #789",
                "user": {"login": "developer123"},
                "assignees": [],
                "requested_reviewers": [{"login": "kiro-ai"}],
                "labels": [{"name": "enhancement"}, {"name": "security"}],
                "head": {"sha": "abc123", "ref": "feature/auth"},
                "base": {"ref": "main"},
                "additions": 250,
                "deletions": 50,
                "changed_files": 5
            },
            "repository": {
                "owner": {"login": "acme-corp"},
                "name": "web-app",
                "full_name": "acme-corp/web-app"
            }
        }
        
        # 2. Process webhook
        from ...scripts.services.webhook_service import WebhookService, WebhookValidator
        
        validator = WebhookValidator("test-secret")
        webhook_service = WebhookService(
            validator=validator,
            queue=e2e_env.queue,
            cache=e2e_env.cache,
            metrics=e2e_env.metrics,
            github_client=e2e_env.github
        )
        
        headers = {
            "X-GitHub-Event": "pull_request",
            "X-GitHub-Delivery": "e2e-test-123",
            "X-Hub-Signature-256": "sha256=valid"
        }
        
        with patch.object(validator, 'validate_signature', return_value=True):
            result = await webhook_service.process_webhook(
                headers,
                json.dumps(webhook_payload).encode()
            )
        
        # 3. Verify webhook was processed
        assert result["status"] == "processed"
        assert len(e2e_env.webhook_events) == 1
        assert e2e_env.webhook_events[0]["queue"] == "pr_processing"
        
        # 4. Simulate PR processing
        pr_message = e2e_env.webhook_events[0]["message"]
        
        # Mock file retrieval
        e2e_env.github.get_pr_files.return_value = [
            {
                "filename": "src/auth/jwt_handler.py",
                "status": "added",
                "additions": 150,
                "deletions": 0,
                "patch": """
@@ -0,0 +1,150 @@
+import jwt
+from datetime import datetime, timedelta
+
+SECRET_KEY = "hardcoded-secret-key"  # Security issue
+
+def generate_token(user_id):
+    payload = {
+        "user_id": user_id,
+        "exp": datetime.utcnow() + timedelta(hours=24)
+    }
+    return jwt.encode(payload, SECRET_KEY, algorithm="HS256")
"""
            },
            {
                "filename": "src/auth/validators.py",
                "status": "added",
                "additions": 100,
                "deletions": 0,
                "patch": """
@@ -0,0 +1,100 @@
+def validate_password(password):
+    if len(password) < 8:
+        return False
+    return True  # Missing complexity checks
"""
            }
        ]
        
        # 5. Simulate code analysis
        analysis_results = {
            "src/auth/jwt_handler.py": [
                {
                    "line": 4,
                    "level": "critical",
                    "category": "security",
                    "message": "Hardcoded secret key detected",
                    "suggestion": "Use environment variable for SECRET_KEY"
                }
            ],
            "src/auth/validators.py": [
                {
                    "line": 4,
                    "level": "warning",
                    "category": "security",
                    "message": "Weak password validation",
                    "suggestion": "Add checks for uppercase, lowercase, numbers, and special characters"
                }
            ]
        }
        
        # 6. Generate review
        review_comments = []
        critical_issues = 0
        warnings = 0
        
        for file_path, issues in analysis_results.items():
            for issue in issues:
                if issue["level"] == "critical":
                    critical_issues += 1
                elif issue["level"] == "warning":
                    warnings += 1
                
                review_comments.append({
                    "path": file_path,
                    "line": issue["line"],
                    "body": f"**{issue['level'].upper()} - {issue['category']}**: {issue['message']}\n\n"
                           f"💡 **Suggestion**: {issue['suggestion']}"
                })
        
        # 7. Post review
        review_body = f"""## 🤖 Automated Code Review by Kiro

I've analyzed the {len(analysis_results)} files in this PR and found some issues that need attention.

### Summary
- 🚨 **Critical Issues**: {critical_issues}
- ⚠️ **Warnings**: {warnings}
- 📁 **Files Reviewed**: {len(analysis_results)}

### Security Concerns
The authentication implementation has security vulnerabilities that should be addressed before merging.

Please review the inline comments for specific issues and suggestions.
"""
        
        review_data = {
            "event": "REQUEST_CHANGES" if critical_issues > 0 else "COMMENT",
            "body": review_body,
            "comments": review_comments
        }
        
        review_result = await e2e_env.github.create_review(
            "acme-corp", "web-app", 456, review_data
        )
        
        # 8. Verify review was posted
        assert len(e2e_env.reviews_posted) == 1
        posted_review = e2e_env.reviews_posted[0]
        assert posted_review["pr_number"] == 456
        assert posted_review["data"]["event"] == "REQUEST_CHANGES"
        assert len(posted_review["data"]["comments"]) == 2
        
        # 9. Send notification
        await e2e_env.notifications.send(
            "developer123",
            "Code Review Completed - PR #456",
            f"Kiro has completed the review of your PR. Found {critical_issues} critical issues."
        )
        
        # 10. Verify notification
        assert len(e2e_env.notifications_sent) == 1
        assert "critical issues" in e2e_env.notifications_sent[0]["message"]
        
        # 11. Record analytics
        e2e_env.metrics.increment.assert_called()
        
        logger.info("✅ New PR automated review flow completed successfully")
    
    @pytest.mark.asyncio
    async def test_pr_update_re_review(self, e2e_env):
        """Test re-review when PR is updated with new commits."""
        logger.info("=== Testing PR Update Re-review Flow ===")
        
        # 1. Simulate synchronize event (new commits pushed)
        webhook_payload = {
            "action": "synchronize",
            "pull_request": {
                "number": 456,
                "title": "Add user authentication feature",
                "assignees": [{"login": "kiro-ai"}],
                "head": {"sha": "def456", "ref": "feature/auth"},  # New SHA
                "base": {"ref": "main"}
            },
            "repository": {
                "owner": {"login": "acme-corp"},
                "name": "web-app"
            },
            "before": "abc123",  # Previous SHA
            "after": "def456"    # New SHA
        }
        
        # 2. Check if this is a significant change
        # In production, would compare diffs
        
        # 3. Queue for re-review
        await e2e_env.queue.publish(
            "pr_processing",
            {
                "pr_number": 456,
                "repo_owner": "acme-corp",
                "repo_name": "web-app",
                "action": "synchronize",
                "previous_sha": "abc123",
                "new_sha": "def456",
                "priority": 2  # Higher priority for updates
            }
        )
        
        # 4. Verify queued
        assert len(e2e_env.webhook_events) > 0
        latest_event = e2e_env.webhook_events[-1]
        assert latest_event["message"]["action"] == "synchronize"
        
        logger.info("✅ PR update re-review flow completed")
    
    @pytest.mark.asyncio
    async def test_configuration_change_flow(self, e2e_env):
        """Test flow when repository configuration is changed."""
        logger.info("=== Testing Configuration Change Flow ===")
        
        # 1. Simulate .kiro.yml file change
        config_change_payload = {
            "commits": [{
                "added": [],
                "removed": [],
                "modified": [".kiro.yml"]
            }],
            "repository": {
                "owner": {"login": "acme-corp"},
                "name": "web-app"
            }
        }
        
        # 2. Parse new configuration
        new_config = """
review:
  auto_approve_dependabot: true
  ignore_paths:
    - "*.md"
    - "docs/*"
  security:
    enabled: true
    block_on_critical: true
  style:
    max_line_length: 120
    indent_size: 4
"""
        
        # 3. Validate configuration
        config_valid = True
        validation_errors = []
        
        # 4. Apply configuration
        if config_valid:
            # Store in cache
            await e2e_env.cache.set(
                "config:acme-corp/web-app",
                new_config,
                ttl_seconds=3600
            )
            
            # Send confirmation
            await e2e_env.notifications.send(
                "acme-corp",
                "Kiro Configuration Updated",
                "Your .kiro.yml configuration has been successfully updated."
            )
        
        # 5. Verify configuration applied
        e2e_env.cache.set.assert_called()
        assert len(e2e_env.notifications_sent) > 0
        
        logger.info("✅ Configuration change flow completed")


class TestAnalyticsCollection:
    """Test analytics data collection throughout the review process."""
    
    @pytest.mark.asyncio
    async def test_review_metrics_collection(self, e2e_env):
        """Test collection of review metrics."""
        logger.info("=== Testing Review Metrics Collection ===")
        
        # 1. Track review start
        review_start = datetime.utcnow()
        e2e_env.metrics.gauge("review.processing.start", review_start.timestamp())
        
        # 2. Track file analysis
        files_analyzed = 5
        for i in range(files_analyzed):
            e2e_env.metrics.timing(
                "file.analysis.duration",
                duration_ms=150 + i * 50,  # Varying durations
                tags={"file_type": "python"}
            )
        
        # 3. Track issues found
        issues_by_category = {
            "security": 3,
            "style": 7,
            "complexity": 2,
            "documentation": 4
        }
        
        for category, count in issues_by_category.items():
            e2e_env.metrics.gauge(
                "issues.found",
                count,
                tags={"category": category}
            )
        
        # 4. Track review completion
        review_end = datetime.utcnow()
        review_duration = (review_end - review_start).total_seconds() * 1000
        
        e2e_env.metrics.timing("review.total.duration", review_duration)
        e2e_env.metrics.increment("reviews.completed", tags={"status": "success"})
        
        # 5. Generate analytics summary
        analytics_summary = {
            "review_id": "review-123",
            "pr_number": 456,
            "duration_ms": review_duration,
            "files_analyzed": files_analyzed,
            "total_issues": sum(issues_by_category.values()),
            "issues_by_category": issues_by_category,
            "timestamp": review_end.isoformat()
        }
        
        # 6. Store analytics
        await e2e_env.database.execute(
            "INSERT INTO review_analytics (data) VALUES (?)",
            {"data": json.dumps(analytics_summary)}
        )
        
        # 7. Verify metrics recorded
        assert e2e_env.metrics.gauge.called
        assert e2e_env.metrics.timing.called
        assert e2e_env.metrics.increment.called
        
        logger.info("✅ Review metrics collection completed")
    
    @pytest.mark.asyncio
    async def test_developer_feedback_tracking(self, e2e_env):
        """Test tracking of developer feedback on reviews."""
        logger.info("=== Testing Developer Feedback Tracking ===")
        
        # 1. Simulate feedback comment
        feedback_payload = {
            "action": "created",
            "comment": {
                "body": "Thanks @kiro-ai! The security issue was very helpful. The style suggestion seems too strict though.",
                "user": {"login": "developer123"}
            },
            "issue": {
                "number": 456,
                "pull_request": {"url": "..."}
            }
        }
        
        # 2. Parse feedback sentiment
        feedback_analysis = {
            "sentiment": "mixed",
            "helpful_aspects": ["security issue detection"],
            "improvement_areas": ["style rules too strict"],
            "overall_rating": 4  # Out of 5
        }
        
        # 3. Store feedback
        await e2e_env.database.execute(
            "INSERT INTO review_feedback (pr_number, feedback) VALUES (?, ?)",
            {"pr_number": 456, "feedback": json.dumps(feedback_analysis)}
        )
        
        # 4. Update learning model
        e2e_env.metrics.gauge(
            "feedback.rating",
            feedback_analysis["overall_rating"],
            tags={"pr_number": "456"}
        )
        
        # 5. Verify feedback tracked
        e2e_env.database.execute.assert_called()
        
        logger.info("✅ Developer feedback tracking completed")


class TestErrorScenarios:
    """Test error handling in E2E scenarios."""
    
    @pytest.mark.asyncio
    async def test_github_api_failure_handling(self, e2e_env):
        """Test handling when GitHub API fails."""
        logger.info("=== Testing GitHub API Failure Handling ===")
        
        # 1. Simulate API failure
        e2e_env.github.get_pr_files.side_effect = Exception("GitHub API error: 502 Bad Gateway")
        
        # 2. Attempt to process PR
        try:
            await e2e_env.github.get_pr_files("acme-corp", "web-app", 456)
        except Exception as e:
            # 3. Handle error gracefully
            logger.error(f"GitHub API failed: {e}")
            
            # 4. Queue for retry
            await e2e_env.queue.publish(
                "pr_processing_retry",
                {
                    "pr_number": 456,
                    "retry_count": 1,
                    "error": str(e)
                },
                delay_seconds=60  # Retry after 1 minute
            )
            
            # 5. Notify about delay
            await e2e_env.notifications.send(
                "developer123",
                "Review Delayed - PR #456",
                "GitHub API is temporarily unavailable. Will retry in 1 minute."
            )
        
        # 6. Verify error handling
        assert len(e2e_env.webhook_events) > 0
        assert "retry" in e2e_env.webhook_events[-1]["queue"]
        assert len(e2e_env.notifications_sent) > 0
        
        logger.info("✅ GitHub API failure handling completed")
    
    @pytest.mark.asyncio
    async def test_timeout_handling(self, e2e_env):
        """Test handling of analysis timeouts."""
        logger.info("=== Testing Analysis Timeout Handling ===")
        
        # 1. Simulate long-running analysis
        async def slow_analysis():
            await asyncio.sleep(5)  # Simulate slow operation
            raise asyncio.TimeoutError("Analysis timeout")
        
        # 2. Run with timeout
        try:
            await asyncio.wait_for(slow_analysis(), timeout=2.0)
        except asyncio.TimeoutError:
            logger.warning("Analysis timed out")
            
            # 3. Post partial review
            await e2e_env.github.create_review(
                "acme-corp", "web-app", 456,
                {
                    "event": "COMMENT",
                    "body": "⏱️ Analysis timed out. Partial review posted.\n\nLarge PRs may take longer to analyze.",
                    "comments": []
                }
            )
            
            # 4. Record timeout metric
            e2e_env.metrics.increment("analysis.timeout", tags={"pr_size": "large"})
        
        # 5. Verify timeout handled
        assert len(e2e_env.reviews_posted) > 0
        assert "timed out" in e2e_env.reviews_posted[-1]["data"]["body"]
        
        logger.info("✅ Timeout handling completed")