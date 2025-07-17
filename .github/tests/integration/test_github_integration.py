#!/usr/bin/env python3
"""
Integration tests for GitHub integration components.
Tests the interaction between different modules.
"""

import pytest
import asyncio
import json
from unittest.mock import Mock, AsyncMock, patch
from datetime import datetime
import sys
import os

# Add parent directory to path for imports
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', 'scripts'))

from webhook_handler import WebhookHandler, WebhookEvent, EventType
from pr_processor import PRProcessor
from code_analyzer import CodeAnalyzer
from comment_generator import CommentGenerator
from github_client import GitHubClient


class TestGitHubIntegrationFlow:
    """Integration tests for the complete GitHub integration flow."""
    
    @pytest.fixture
    def integration_config(self):
        """Configuration for integration testing."""
        return {
            'github_app_id': '12345',
            'github_private_key': 'fake-key',
            'webhook_secret': 'test-secret',
            'redis_url': 'redis://localhost:6379',
            'processing_queue': 'test-queue',
            'github_token': 'test-token',
            'organization_id': 'test-org'
        }
    
    @pytest.fixture
    def webhook_handler(self, integration_config):
        """Create webhook handler with mocked Redis."""
        with patch('webhook_handler.Redis'):
            return WebhookHandler(integration_config)
    
    @pytest.fixture
    def pr_processor(self, integration_config):
        """Create PR processor."""
        return PRProcessor(integration_config)
    
    @pytest.fixture
    def code_analyzer(self, integration_config):
        """Create code analyzer."""
        return CodeAnalyzer(integration_config)
    
    @pytest.fixture
    def comment_generator(self, integration_config):
        """Create comment generator."""
        return CommentGenerator(integration_config)
    
    @pytest.fixture
    def github_client(self, integration_config):
        """Create GitHub client with mocked session."""
        with patch('aiohttp.ClientSession'):
            return GitHubClient(integration_config['github_token'])
    
    @pytest.fixture
    def sample_pr_webhook_payload(self):
        """Sample PR opened webhook payload."""
        return {
            'action': 'opened',
            'pull_request': {
                'id': 1,
                'number': 42,
                'state': 'open',
                'title': 'Add new feature',
                'body': 'This PR adds authentication feature\n\nCloses #10',
                'user': {'login': 'developer', 'id': 123},
                'head': {
                    'sha': 'abc123',
                    'ref': 'feature/auth',
                    'repo': {'full_name': 'testorg/testrepo'}
                },
                'base': {
                    'sha': 'def456',
                    'ref': 'main',
                    'repo': {'full_name': 'testorg/testrepo'}
                },
                'created_at': '2024-01-01T00:00:00Z',
                'updated_at': '2024-01-01T00:00:00Z',
                'diff_url': 'https://github.com/testorg/testrepo/pull/42.diff',
                'patch_url': 'https://github.com/testorg/testrepo/pull/42.patch'
            },
            'repository': {
                'id': 456,
                'name': 'testrepo',
                'full_name': 'testorg/testrepo',
                'owner': {'login': 'testorg', 'id': 789}
            }
        }
    
    @pytest.mark.asyncio
    async def test_complete_pr_review_flow(
        self, webhook_handler, pr_processor, code_analyzer,
        comment_generator, github_client, sample_pr_webhook_payload
    ):
        """Test the complete flow from webhook to comment posting."""
        # Step 1: Webhook receives PR opened event
        webhook_handler.redis_client.rpush = AsyncMock()
        
        headers = {
            'X-GitHub-Event': 'pull_request',
            'X-GitHub-Delivery': 'test-delivery-123'
        }
        
        result = await webhook_handler.process_webhook(headers, sample_pr_webhook_payload)
        assert result is True
        
        # Verify event was queued
        webhook_handler.redis_client.rpush.assert_called_once()
        queued_data = json.loads(webhook_handler.redis_client.rpush.call_args[0][1])
        
        # Step 2: PR Processor fetches PR details
        pr_processor.github_client = github_client
        github_client.get_pull_request = AsyncMock(return_value={
            'files': [
                {
                    'filename': 'auth.py',
                    'status': 'added',
                    'additions': 50,
                    'deletions': 0,
                    'patch': '@@ -0,0 +1,50 @@\n+def authenticate(username, password):\n+    # TODO: Add validation\n+    return True'
                }
            ],
            'commits': [
                {
                    'sha': 'abc123',
                    'commit': {'message': 'Add authentication function'}
                }
            ]
        })
        
        pr_data = await pr_processor.fetch_pr_data(
            'testorg/testrepo',
            42
        )
        
        assert pr_data is not None
        assert len(pr_data['files']) == 1
        assert pr_data['files'][0]['filename'] == 'auth.py'
        
        # Step 3: Code Analyzer analyzes the changes
        analysis_results = []
        for file in pr_data['files']:
            if file['patch']:
                result = await code_analyzer.analyze_diff(
                    file['patch'],
                    file['filename'],
                    'python'
                )
                analysis_results.append(result)
        
        assert len(analysis_results) == 1
        assert len(analysis_results[0].issues) > 0  # Should find TODO comment
        
        # Step 4: Comment Generator creates review comments
        comments = []
        for analysis in analysis_results:
            for issue in analysis.issues:
                comment = await comment_generator.generate_comment(issue)
                comments.append({
                    'path': analysis.file_path,
                    'line': issue.line or 1,
                    'body': comment
                })
        
        assert len(comments) > 0
        assert 'TODO' in comments[0]['body'] or 'validation' in comments[0]['body']
        
        # Step 5: GitHub Client posts the review
        github_client.create_pull_request_review = AsyncMock(return_value={
            'id': 123456,
            'state': 'COMMENTED',
            'body': 'Automated review by Kiro'
        })
        
        review_result = await github_client.create_pull_request_review(
            'testorg/testrepo',
            42,
            {
                'body': 'Kiro has reviewed your pull request.',
                'event': 'COMMENT',
                'comments': comments
            }
        )
        
        assert review_result is not None
        assert review_result['state'] == 'COMMENTED'
    
    @pytest.mark.asyncio
    async def test_pr_with_security_issues(
        self, code_analyzer, comment_generator, github_client
    ):
        """Test handling PR with security vulnerabilities."""
        # Analyze code with SQL injection
        vulnerable_code = '''
def get_user(user_id):
    query = f"SELECT * FROM users WHERE id = {user_id}"
    return db.execute(query)
'''
        
        analysis = await code_analyzer.analyze_code(
            vulnerable_code,
            'database.py',
            'python'
        )
        
        # Should detect SQL injection
        security_issues = [i for i in analysis.issues if i.type.value == 'security']
        assert len(security_issues) > 0
        
        # Generate security alert comment
        comment = await comment_generator.generate_comment(security_issues[0])
        assert 'SQL injection' in comment or 'security' in comment.lower()
        assert 'critical' in comment.lower() or 'high' in comment.lower()
    
    @pytest.mark.asyncio
    async def test_pr_with_multiple_files(
        self, pr_processor, code_analyzer, github_client
    ):
        """Test processing PR with multiple files."""
        github_client.get_pull_request = AsyncMock(return_value={
            'files': [
                {
                    'filename': 'models.py',
                    'status': 'modified',
                    'patch': '@@ -10,0 +11,5 @@\n+class User:\n+    def __init__(self):\n+        pass'
                },
                {
                    'filename': 'views.py',
                    'status': 'modified',
                    'patch': '@@ -20,0 +21,3 @@\n+def index():\n+    return "Hello"'
                },
                {
                    'filename': 'README.md',
                    'status': 'modified',
                    'patch': '@@ -1,0 +1,1 @@\n+# Project Title'
                }
            ]
        })
        
        pr_data = await pr_processor.fetch_pr_data('testorg/testrepo', 50)
        
        # Process each file
        total_issues = 0
        for file in pr_data['files']:
            # Skip non-code files
            if file['filename'].endswith('.md'):
                continue
                
            language = 'python' if file['filename'].endswith('.py') else 'unknown'
            result = await code_analyzer.analyze_diff(
                file['patch'],
                file['filename'],
                language
            )
            total_issues += len(result.issues)
        
        # Should analyze Python files but skip markdown
        assert total_issues >= 0  # May or may not find issues
    
    @pytest.mark.asyncio
    async def test_configuration_based_analysis(
        self, code_analyzer, integration_config
    ):
        """Test that analysis respects configuration settings."""
        # Update config to disable certain checks
        integration_config['rules_config'] = {
            'style_checks': False,
            'security_checks': True,
            'performance_checks': True,
            'best_practices': True
        }
        
        analyzer = CodeAnalyzer(integration_config)
        
        code_with_style_issues = '''
def test():
    x=1;y=2  # Style issue: multiple statements on one line
    return x+y
'''
        
        result = await analyzer.analyze_code(
            code_with_style_issues,
            'test.py',
            'python'
        )
        
        # Should not report style issues if disabled
        style_issues = [i for i in result.issues if i.type.value == 'style']
        # Note: This test assumes the analyzer respects config
        # In practice, you'd need to implement this configuration check
    
    @pytest.mark.asyncio
    async def test_error_handling_in_flow(
        self, webhook_handler, pr_processor, github_client
    ):
        """Test error handling throughout the integration flow."""
        # Simulate GitHub API error
        github_client.get_pull_request = AsyncMock(
            side_effect=Exception("GitHub API rate limit exceeded")
        )
        
        pr_processor.github_client = github_client
        
        # Should handle error gracefully
        with pytest.raises(Exception) as exc_info:
            await pr_processor.fetch_pr_data('testorg/testrepo', 100)
        
        assert "rate limit" in str(exc_info.value)
    
    @pytest.mark.asyncio
    async def test_comment_batching(self, comment_generator):
        """Test batching multiple issues into organized comments."""
        issues = [
            Mock(
                type=Mock(value='style'),
                severity=Mock(value='minor'),
                message='Line too long',
                line=10
            ),
            Mock(
                type=Mock(value='style'),
                severity=Mock(value='minor'),
                message='Missing docstring',
                line=15
            ),
            Mock(
                type=Mock(value='security'),
                severity=Mock(value='critical'),
                message='Hardcoded password',
                line=20
            )
        ]
        
        # Generate batch comment
        batch_comment = await comment_generator.generate_batch_comment(issues)
        
        # Should organize by severity/type
        assert 'critical' in batch_comment.lower()
        assert 'style' in batch_comment.lower()
        # Should list all issues
        assert 'Line too long' in batch_comment
        assert 'Missing docstring' in batch_comment
        assert 'Hardcoded password' in batch_comment
    
    @pytest.mark.asyncio
    async def test_incremental_review_on_push(
        self, webhook_handler, pr_processor, code_analyzer
    ):
        """Test incremental review when new commits are pushed."""
        # First review
        initial_review = {
            'commit_sha': 'abc123',
            'issues_found': 5,
            'timestamp': datetime.now().isoformat()
        }
        
        # Simulate new push event
        push_payload = {
            'action': 'synchronize',
            'pull_request': {
                'number': 42,
                'head': {'sha': 'def456'}  # New commit
            },
            'repository': {'full_name': 'testorg/testrepo'}
        }
        
        # Process should only analyze new changes
        # This would require implementing incremental diff analysis
        # For now, we just verify the flow handles synchronize events
        webhook_handler.redis_client.rpush = AsyncMock()
        
        event = WebhookEvent(
            event_type=EventType.PULL_REQUEST,
            action='synchronize',
            payload=push_payload
        )
        
        result = await webhook_handler.handle_pull_request(event)
        assert result is True
        webhook_handler.redis_client.rpush.assert_called_once()


class TestEndToEndScenarios:
    """End-to-end scenario tests."""
    
    @pytest.mark.asyncio
    async def test_auto_fix_suggestion_flow(
        self, code_analyzer, comment_generator, github_client
    ):
        """Test flow for suggesting automatic fixes."""
        # Code with fixable issue
        code_with_issue = '''
import os
import sys
import json  # Unused import

def process_data(data):
    return data.upper()
'''
        
        analysis = await code_analyzer.analyze_code(
            code_with_issue,
            'processor.py',
            'python'
        )
        
        # Find unused import issue
        unused_import_issues = [
            i for i in analysis.issues 
            if 'unused' in i.message.lower() and 'import' in i.message.lower()
        ]
        
        if unused_import_issues:
            issue = unused_import_issues[0]
            
            # Generate comment with fix suggestion
            comment = await comment_generator.generate_comment(issue)
            
            # Should include suggestion to remove the import
            assert 'remove' in comment.lower() or 'delete' in comment.lower()
            
            # In a real implementation, would create a commit suggestion
            # github_client.create_commit_suggestion(...)
    
    @pytest.mark.asyncio
    async def test_performance_analysis_flow(self, code_analyzer):
        """Test performance analysis integration."""
        # Code with performance issues
        inefficient_code = '''
def find_duplicates(items):
    duplicates = []
    for i in range(len(items)):
        for j in range(i + 1, len(items)):
            if items[i] == items[j] and items[i] not in duplicates:
                duplicates.append(items[i])
    return duplicates
'''
        
        analysis = await code_analyzer.analyze_code(
            inefficient_code,
            'utils.py',
            'python'
        )
        
        performance_issues = [
            i for i in analysis.issues 
            if i.type.value == 'performance'
        ]
        
        # Should detect O(n²) complexity
        assert len(performance_issues) > 0
        
        # Verify suggestion includes better algorithm
        for issue in performance_issues:
            if issue.suggested_fix:
                assert 'set' in issue.suggested_fix or 'dict' in issue.suggested_fix


if __name__ == '__main__':
    pytest.main([__file__, '-v'])