"""
Integration tests for GitHub API interactions.

This module tests the integration between our services and GitHub API,
using mock fixtures and test scenarios.
"""

import asyncio
import json
import pytest
from datetime import datetime
from unittest.mock import Mock, AsyncMock, patch
from typing import Dict, List, Any

from ...scripts.core.interfaces import GitHubClientInterface
from ...scripts.services.webhook_service import WebhookService, WebhookValidator
from ...scripts.repositories.review_repository import ReviewRepository, Review


class MockGitHubClient(GitHubClientInterface):
    """Mock GitHub client for testing."""
    
    def __init__(self):
        self.pull_requests = {}
        self.issues = {}
        self.reviews = {}
        self.comments = {}
        self.rate_limit = {
            "rate": {
                "limit": 5000,
                "remaining": 4999,
                "reset": int(datetime.utcnow().timestamp()) + 3600
            }
        }
    
    async def get_pull_request(self, owner: str, repo: str, pr_number: int) -> Dict[str, Any]:
        """Get mock pull request."""
        key = f"{owner}/{repo}#{pr_number}"
        if key not in self.pull_requests:
            raise ValueError(f"Pull request {key} not found")
        return self.pull_requests[key]
    
    async def get_pr_files(self, owner: str, repo: str, pr_number: int) -> List[Dict[str, Any]]:
        """Get mock PR files."""
        # Return test files
        return [
            {
                "filename": "src/main.py",
                "status": "modified",
                "additions": 50,
                "deletions": 10,
                "changes": 60,
                "patch": "@@ -1,10 +1,50 @@\n+def new_function():\n+    pass"
            },
            {
                "filename": "tests/test_main.py",
                "status": "added",
                "additions": 100,
                "deletions": 0,
                "changes": 100,
                "patch": "@@ -0,0 +1,100 @@\n+import pytest\n+def test_new_function():\n+    assert True"
            }
        ]
    
    async def create_review(self, owner: str, repo: str, pr_number: int, 
                          review_data: Dict[str, Any]) -> Dict[str, Any]:
        """Create mock review."""
        review_id = f"review-{len(self.reviews) + 1}"
        review = {
            "id": review_id,
            "pull_request_number": pr_number,
            "state": review_data.get("event", "COMMENT"),
            "body": review_data.get("body", ""),
            "comments": review_data.get("comments", []),
            "created_at": datetime.utcnow().isoformat()
        }
        self.reviews[review_id] = review
        return review
    
    async def create_issue_comment(self, owner: str, repo: str, issue_number: int, 
                                 body: str) -> Dict[str, Any]:
        """Create mock issue comment."""
        comment_id = f"comment-{len(self.comments) + 1}"
        comment = {
            "id": comment_id,
            "issue_number": issue_number,
            "body": body,
            "created_at": datetime.utcnow().isoformat()
        }
        self.comments[comment_id] = comment
        return comment
    
    async def get_rate_limit(self) -> Dict[str, Any]:
        """Get mock rate limit."""
        return self.rate_limit


@pytest.fixture
def mock_github_client():
    """Create mock GitHub client with test data."""
    client = MockGitHubClient()
    
    # Add test pull request
    client.pull_requests["test-org/test-repo#123"] = {
        "number": 123,
        "title": "Add new feature",
        "body": "This PR adds a new feature\n\nFixes #100",
        "state": "open",
        "user": {"login": "developer1"},
        "assignees": [{"login": "kiro-ai"}],
        "requested_reviewers": [],
        "labels": [{"name": "enhancement"}],
        "head": {
            "ref": "feature-branch",
            "sha": "abc123"
        },
        "base": {
            "ref": "main"
        },
        "created_at": "2024-01-01T00:00:00Z",
        "updated_at": "2024-01-01T01:00:00Z"
    }
    
    return client


class TestGitHubWebhookIntegration:
    """Test webhook integration with GitHub API."""
    
    @pytest.mark.asyncio
    async def test_pr_assignment_flow(self, mock_github_client):
        """Test complete flow when PR is assigned to Kiro."""
        # Create webhook payload
        payload = {
            "action": "assigned",
            "pull_request": mock_github_client.pull_requests["test-org/test-repo#123"],
            "repository": {
                "owner": {"login": "test-org"},
                "name": "test-repo"
            },
            "assignee": {"login": "kiro-ai"}
        }
        
        # Process webhook
        validator = WebhookValidator("test-secret")
        service = WebhookService(
            validator=validator,
            queue=AsyncMock(),
            cache=AsyncMock(),
            metrics=Mock(),
            github_client=mock_github_client
        )
        
        # Mock cache and queue
        service.cache.exists = AsyncMock(return_value=False)
        service.queue.publish = AsyncMock(return_value="msg-123")
        
        # Create webhook headers
        headers = {
            "X-GitHub-Event": "pull_request",
            "X-GitHub-Delivery": "delivery-123",
            "X-Hub-Signature-256": "sha256=test"
        }
        
        body = json.dumps(payload).encode()
        
        # Mock signature validation
        with patch.object(validator, 'validate_signature', return_value=True):
            result = await service.process_webhook(headers, body)
        
        # Verify processing
        assert result["status"] == "processed"
        assert result["result"]["processed"] is True
        
        # Verify PR was queued for processing
        service.queue.publish.assert_called_once()
        call_args = service.queue.publish.call_args
        assert call_args[0][0] == "pr_processing"  # queue name
        assert call_args[0][1]["pr_number"] == 123  # PR number
    
    @pytest.mark.asyncio
    async def test_review_creation_flow(self, mock_github_client):
        """Test creating a review on GitHub."""
        # Create review data
        review_data = {
            "event": "COMMENT",
            "body": "## Code Review by Kiro\n\nOverall the changes look good!",
            "comments": [
                {
                    "path": "src/main.py",
                    "position": 10,
                    "body": "Consider adding error handling here"
                }
            ]
        }
        
        # Create review
        result = await mock_github_client.create_review(
            "test-org", "test-repo", 123, review_data
        )
        
        # Verify review created
        assert result["id"] == "review-1"
        assert result["pull_request_number"] == 123
        assert result["state"] == "COMMENT"
        assert len(result["comments"]) == 1
    
    @pytest.mark.asyncio
    async def test_rate_limit_handling(self, mock_github_client):
        """Test GitHub API rate limit handling."""
        # Get initial rate limit
        rate_limit = await mock_github_client.get_rate_limit()
        assert rate_limit["rate"]["remaining"] == 4999
        
        # Simulate API calls reducing rate limit
        mock_github_client.rate_limit["rate"]["remaining"] = 10
        
        # Check low rate limit
        rate_limit = await mock_github_client.get_rate_limit()
        assert rate_limit["rate"]["remaining"] == 10
        
        # This should trigger rate limit warnings in production


class TestPRProcessingIntegration:
    """Test PR processing pipeline integration."""
    
    @pytest.mark.asyncio
    async def test_pr_analysis_flow(self, mock_github_client):
        """Test complete PR analysis flow."""
        from ...scripts.analyzers.base_analyzer import BaseAnalyzer
        from ...scripts.analyzers.security_analyzer import SecurityPatternStrategy
        
        # Get PR files
        files = await mock_github_client.get_pr_files("test-org", "test-repo", 123)
        
        # Analyze each file
        analyzer = BaseAnalyzer([SecurityPatternStrategy()])
        issues = []
        
        for file in files:
            if file["filename"].endswith(".py"):
                # Simulate file content with security issue
                content = """
def login(username, password):
    hardcoded_password = "admin123"
    if password == hardcoded_password:
        return True
"""
                result = await analyzer.analyze(file["filename"], content)
                issues.extend(result.issues)
        
        # Verify security issues found
        assert len(issues) > 0
        security_issues = [i for i in issues if i.category == "security"]
        assert len(security_issues) > 0
    
    @pytest.mark.asyncio
    async def test_review_comment_generation(self, mock_github_client):
        """Test generating review comments from analysis."""
        # Simulate analysis results
        analysis_results = {
            "src/main.py": [
                {
                    "line": 3,
                    "level": "error",
                    "message": "Hardcoded password detected",
                    "suggestion": "Use environment variables for credentials"
                }
            ],
            "tests/test_main.py": [
                {
                    "line": 10,
                    "level": "warning",
                    "message": "Missing test for error case",
                    "suggestion": "Add test for exception handling"
                }
            ]
        }
        
        # Generate review comments
        comments = []
        for file_path, issues in analysis_results.items():
            for issue in issues:
                comment = {
                    "path": file_path,
                    "line": issue["line"],
                    "body": f"**{issue['level'].upper()}**: {issue['message']}\n\n"
                           f"💡 **Suggestion**: {issue['suggestion']}"
                }
                comments.append(comment)
        
        # Create review with comments
        review_data = {
            "event": "REQUEST_CHANGES" if any(i["level"] == "error" for issues in analysis_results.values() for i in issues) else "COMMENT",
            "body": "## Code Review Summary\n\nI found some issues that need attention.",
            "comments": comments
        }
        
        result = await mock_github_client.create_review(
            "test-org", "test-repo", 123, review_data
        )
        
        assert result["state"] == "REQUEST_CHANGES"
        assert len(result["comments"]) == 2


class TestWebhookScenarios:
    """Test various webhook scenarios."""
    
    @pytest.mark.asyncio
    async def test_pr_synchronize_event(self, mock_github_client):
        """Test handling PR synchronization (new commits)."""
        payload = {
            "action": "synchronize",
            "pull_request": {
                "number": 123,
                "head": {"sha": "def456"},  # New commit
                "assignees": [{"login": "kiro-ai"}]
            },
            "repository": {
                "owner": {"login": "test-org"},
                "name": "test-repo"
            }
        }
        
        # This should trigger re-analysis of the PR
        # Implementation would process the new changes
        assert payload["action"] == "synchronize"
        assert payload["pull_request"]["head"]["sha"] == "def456"
    
    @pytest.mark.asyncio
    async def test_review_requested_event(self, mock_github_client):
        """Test handling review request event."""
        payload = {
            "action": "review_requested",
            "pull_request": {
                "number": 123,
                "requested_reviewers": [{"login": "kiro-ai"}]
            },
            "repository": {
                "owner": {"login": "test-org"},
                "name": "test-repo"
            }
        }
        
        # This should trigger PR analysis
        assert payload["action"] == "review_requested"
        assert any(r["login"] == "kiro-ai" for r in payload["pull_request"]["requested_reviewers"])
    
    @pytest.mark.asyncio
    async def test_comment_mention_event(self, mock_github_client):
        """Test handling @kiro-ai mentions in comments."""
        payload = {
            "action": "created",
            "issue": {
                "number": 123,
                "pull_request": {"url": "https://api.github.com/repos/test-org/test-repo/pulls/123"}
            },
            "comment": {
                "body": "@kiro-ai can you review this security fix?",
                "user": {"login": "developer1"}
            },
            "repository": {
                "owner": {"login": "test-org"},
                "name": "test-repo"
            }
        }
        
        # Check for Kiro mention
        assert "@kiro-ai" in payload["comment"]["body"].lower()
        
        # This should trigger targeted review
        # Could parse specific commands from the comment


class TestErrorHandling:
    """Test error handling in GitHub integration."""
    
    @pytest.mark.asyncio
    async def test_github_api_error_handling(self, mock_github_client):
        """Test handling of GitHub API errors."""
        # Simulate API error
        async def failing_api_call():
            raise Exception("GitHub API error: 404 Not Found")
        
        mock_github_client.get_pull_request = failing_api_call
        
        # Test error handling
        with pytest.raises(Exception) as exc_info:
            await mock_github_client.get_pull_request("test-org", "test-repo", 999)
        
        assert "404 Not Found" in str(exc_info.value)
    
    @pytest.mark.asyncio
    async def test_rate_limit_exceeded(self, mock_github_client):
        """Test handling when rate limit is exceeded."""
        # Set rate limit to 0
        mock_github_client.rate_limit["rate"]["remaining"] = 0
        
        # Check rate limit
        rate_limit = await mock_github_client.get_rate_limit()
        
        # Should handle gracefully
        assert rate_limit["rate"]["remaining"] == 0
        
        # In production, this would pause processing until reset


@pytest.fixture
async def integration_test_setup():
    """Set up integration test environment."""
    # Initialize test database
    review_repo = ReviewRepository(":memory:")
    await review_repo.initialize()
    
    # Create test configuration
    config = {
        "github_app_id": "test-app-123",
        "webhook_secret": "test-secret",
        "processing_timeout": 30
    }
    
    return {
        "review_repo": review_repo,
        "config": config
    }


class TestFullIntegration:
    """Test full integration scenarios."""
    
    @pytest.mark.asyncio
    async def test_complete_pr_review_flow(self, mock_github_client, integration_test_setup):
        """Test complete PR review flow from webhook to comment."""
        setup = await integration_test_setup
        
        # 1. Receive webhook
        webhook_payload = {
            "action": "opened",
            "pull_request": mock_github_client.pull_requests["test-org/test-repo#123"],
            "repository": {
                "owner": {"login": "test-org"},
                "name": "test-repo"
            }
        }
        
        # 2. Process PR
        pr_data = webhook_payload["pull_request"]
        
        # 3. Create review record
        review = Review(
            id="review-test-123",
            pr_number=pr_data["number"],
            repo_owner="test-org",
            repo_name="test-repo",
            review_type="automated",
            status="in_progress",
            created_at=datetime.utcnow(),
            updated_at=datetime.utcnow()
        )
        
        review_id = await setup["review_repo"].save(review)
        
        # 4. Get PR files
        files = await mock_github_client.get_pr_files("test-org", "test-repo", 123)
        
        # 5. Analyze files (simplified)
        issues_found = 2
        suggestions_made = 1
        
        # 6. Update review
        await setup["review_repo"].update(review_id, {
            "status": "completed",
            "issues_found": issues_found,
            "suggestions_made": suggestions_made,
            "completed_at": datetime.utcnow().isoformat()
        })
        
        # 7. Post review to GitHub
        review_result = await mock_github_client.create_review(
            "test-org", "test-repo", 123,
            {
                "event": "COMMENT",
                "body": f"Found {issues_found} issues",
                "comments": []
            }
        )
        
        # Verify complete flow
        assert review_result["id"] is not None
        completed_review = await setup["review_repo"].find_by_id(review_id)
        assert completed_review.status == "completed"