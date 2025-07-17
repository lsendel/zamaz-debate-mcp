#!/usr/bin/env python3
"""
Unit tests for the webhook handler module.
"""

import pytest
import json
import asyncio
from unittest.mock import Mock, AsyncMock, patch, MagicMock
from datetime import datetime
import sys
import os

# Add parent directory to path for imports
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'scripts'))

from webhook_handler import WebhookHandler, WebhookEvent, EventType


class TestWebhookHandler:
    """Test cases for WebhookHandler."""
    
    @pytest.fixture
    def mock_config(self):
        """Mock configuration for testing."""
        return {
            'github_app_id': '12345',
            'github_private_key': 'fake-private-key',
            'webhook_secret': 'test-secret',
            'redis_url': 'redis://localhost:6379',
            'processing_queue': 'test-queue'
        }
    
    @pytest.fixture
    def webhook_handler(self, mock_config):
        """Create a WebhookHandler instance for testing."""
        with patch('webhook_handler.Redis'):
            return WebhookHandler(mock_config)
    
    @pytest.fixture
    def sample_pr_event(self):
        """Sample pull request event payload."""
        return {
            'action': 'opened',
            'pull_request': {
                'id': 1,
                'number': 42,
                'state': 'open',
                'title': 'Add new feature',
                'body': 'This PR adds a new feature',
                'user': {
                    'login': 'testuser',
                    'id': 123
                },
                'head': {
                    'sha': 'abc123',
                    'ref': 'feature-branch'
                },
                'base': {
                    'sha': 'def456',
                    'ref': 'main'
                },
                'created_at': '2024-01-01T00:00:00Z',
                'updated_at': '2024-01-01T00:00:00Z'
            },
            'repository': {
                'id': 456,
                'name': 'test-repo',
                'full_name': 'testorg/test-repo',
                'owner': {
                    'login': 'testorg',
                    'id': 789
                }
            },
            'sender': {
                'login': 'testuser',
                'id': 123
            }
        }
    
    def test_validate_signature_valid(self, webhook_handler):
        """Test signature validation with valid signature."""
        payload = b'{"test": "data"}'
        secret = webhook_handler.config['webhook_secret']
        
        # Generate valid signature
        import hmac
        import hashlib
        signature = 'sha256=' + hmac.new(
            secret.encode('utf-8'),
            payload,
            hashlib.sha256
        ).hexdigest()
        
        assert webhook_handler.validate_signature(payload, signature) is True
    
    def test_validate_signature_invalid(self, webhook_handler):
        """Test signature validation with invalid signature."""
        payload = b'{"test": "data"}'
        signature = 'sha256=invalid_signature'
        
        assert webhook_handler.validate_signature(payload, signature) is False
    
    def test_validate_signature_wrong_algorithm(self, webhook_handler):
        """Test signature validation with wrong algorithm."""
        payload = b'{"test": "data"}'
        signature = 'sha1=some_signature'
        
        assert webhook_handler.validate_signature(payload, signature) is False
    
    @pytest.mark.asyncio
    async def test_handle_pull_request_opened(self, webhook_handler, sample_pr_event):
        """Test handling of pull request opened event."""
        webhook_handler.redis_client.rpush = AsyncMock()
        
        event = WebhookEvent(
            event_type=EventType.PULL_REQUEST,
            action='opened',
            payload=sample_pr_event
        )
        
        result = await webhook_handler.handle_pull_request(event)
        
        assert result is True
        webhook_handler.redis_client.rpush.assert_called_once()
        
        # Check the queued data
        call_args = webhook_handler.redis_client.rpush.call_args
        queue_name = call_args[0][0]
        queued_data = json.loads(call_args[0][1])
        
        assert queue_name == webhook_handler.config['processing_queue']
        assert queued_data['event_type'] == 'pull_request_review'
        assert queued_data['repository'] == 'testorg/test-repo'
        assert queued_data['pull_request_number'] == 42
    
    @pytest.mark.asyncio
    async def test_handle_pull_request_synchronize(self, webhook_handler, sample_pr_event):
        """Test handling of pull request synchronize event."""
        sample_pr_event['action'] = 'synchronize'
        webhook_handler.redis_client.rpush = AsyncMock()
        
        event = WebhookEvent(
            event_type=EventType.PULL_REQUEST,
            action='synchronize',
            payload=sample_pr_event
        )
        
        result = await webhook_handler.handle_pull_request(event)
        
        assert result is True
        webhook_handler.redis_client.rpush.assert_called_once()
    
    @pytest.mark.asyncio
    async def test_handle_pull_request_closed(self, webhook_handler, sample_pr_event):
        """Test handling of pull request closed event (should be ignored)."""
        sample_pr_event['action'] = 'closed'
        webhook_handler.redis_client.rpush = AsyncMock()
        
        event = WebhookEvent(
            event_type=EventType.PULL_REQUEST,
            action='closed',
            payload=sample_pr_event
        )
        
        result = await webhook_handler.handle_pull_request(event)
        
        assert result is False
        webhook_handler.redis_client.rpush.assert_not_called()
    
    @pytest.mark.asyncio
    async def test_handle_pull_request_review_requested(self, webhook_handler, sample_pr_event):
        """Test handling of review requested event."""
        sample_pr_event['action'] = 'review_requested'
        sample_pr_event['requested_reviewer'] = {
            'login': 'reviewer1',
            'id': 999
        }
        webhook_handler.redis_client.rpush = AsyncMock()
        
        event = WebhookEvent(
            event_type=EventType.PULL_REQUEST,
            action='review_requested',
            payload=sample_pr_event
        )
        
        result = await webhook_handler.handle_pull_request(event)
        
        assert result is True
        webhook_handler.redis_client.rpush.assert_called_once()
        
        # Check priority is set to high
        queued_data = json.loads(webhook_handler.redis_client.rpush.call_args[0][1])
        assert queued_data['priority'] == 'high'
    
    @pytest.mark.asyncio
    async def test_handle_issue_comment_with_mention(self, webhook_handler):
        """Test handling of issue comment with Kiro mention."""
        webhook_handler.redis_client.rpush = AsyncMock()
        
        payload = {
            'action': 'created',
            'issue': {
                'number': 42,
                'pull_request': {'url': 'https://api.github.com/repos/testorg/test-repo/pulls/42'}
            },
            'comment': {
                'body': '@kiro please review this',
                'user': {'login': 'testuser'}
            },
            'repository': {
                'full_name': 'testorg/test-repo'
            }
        }
        
        event = WebhookEvent(
            event_type=EventType.ISSUE_COMMENT,
            action='created',
            payload=payload
        )
        
        result = await webhook_handler.handle_issue_comment(event)
        
        assert result is True
        webhook_handler.redis_client.rpush.assert_called_once()
        
        # Check priority is urgent for mentions
        queued_data = json.loads(webhook_handler.redis_client.rpush.call_args[0][1])
        assert queued_data['priority'] == 'urgent'
    
    @pytest.mark.asyncio
    async def test_handle_issue_comment_without_mention(self, webhook_handler):
        """Test handling of issue comment without Kiro mention."""
        webhook_handler.redis_client.rpush = AsyncMock()
        
        payload = {
            'action': 'created',
            'issue': {
                'number': 42,
                'pull_request': None
            },
            'comment': {
                'body': 'Just a regular comment',
                'user': {'login': 'testuser'}
            },
            'repository': {
                'full_name': 'testorg/test-repo'
            }
        }
        
        event = WebhookEvent(
            event_type=EventType.ISSUE_COMMENT,
            action='created',
            payload=payload
        )
        
        result = await webhook_handler.handle_issue_comment(event)
        
        assert result is False
        webhook_handler.redis_client.rpush.assert_not_called()
    
    @pytest.mark.asyncio
    async def test_handle_installation_created(self, webhook_handler):
        """Test handling of app installation event."""
        webhook_handler.redis_client.rpush = AsyncMock()
        
        payload = {
            'action': 'created',
            'installation': {
                'id': 12345,
                'account': {
                    'login': 'testorg',
                    'type': 'Organization'
                }
            },
            'repositories': [
                {'name': 'repo1', 'full_name': 'testorg/repo1'},
                {'name': 'repo2', 'full_name': 'testorg/repo2'}
            ]
        }
        
        event = WebhookEvent(
            event_type=EventType.INSTALLATION,
            action='created',
            payload=payload
        )
        
        result = await webhook_handler.handle_installation(event)
        
        assert result is True
        # Should log installation but not queue for processing
        webhook_handler.redis_client.rpush.assert_not_called()
    
    @pytest.mark.asyncio
    async def test_parse_webhook_valid(self, webhook_handler):
        """Test parsing valid webhook data."""
        headers = {
            'X-GitHub-Event': 'pull_request',
            'X-GitHub-Delivery': 'test-delivery-id'
        }
        
        body = {
            'action': 'opened',
            'pull_request': {'id': 1},
            'repository': {'name': 'test'}
        }
        
        event = await webhook_handler.parse_webhook(headers, body)
        
        assert event is not None
        assert event.event_type == EventType.PULL_REQUEST
        assert event.action == 'opened'
        assert event.delivery_id == 'test-delivery-id'
    
    @pytest.mark.asyncio
    async def test_parse_webhook_unknown_event(self, webhook_handler):
        """Test parsing webhook with unknown event type."""
        headers = {
            'X-GitHub-Event': 'unknown_event',
            'X-GitHub-Delivery': 'test-delivery-id'
        }
        
        body = {'test': 'data'}
        
        event = await webhook_handler.parse_webhook(headers, body)
        
        assert event is not None
        assert event.event_type == EventType.UNKNOWN
    
    @pytest.mark.asyncio
    async def test_process_webhook_pull_request(self, webhook_handler, sample_pr_event):
        """Test processing a complete pull request webhook."""
        webhook_handler.handle_pull_request = AsyncMock(return_value=True)
        
        headers = {
            'X-GitHub-Event': 'pull_request',
            'X-GitHub-Delivery': 'test-delivery-id'
        }
        
        result = await webhook_handler.process_webhook(headers, sample_pr_event)
        
        assert result is True
        webhook_handler.handle_pull_request.assert_called_once()
    
    @pytest.mark.asyncio
    async def test_process_webhook_error_handling(self, webhook_handler, sample_pr_event):
        """Test error handling in webhook processing."""
        webhook_handler.handle_pull_request = AsyncMock(
            side_effect=Exception("Processing error")
        )
        
        headers = {
            'X-GitHub-Event': 'pull_request',
            'X-GitHub-Delivery': 'test-delivery-id'
        }
        
        result = await webhook_handler.process_webhook(headers, sample_pr_event)
        
        assert result is False
    
    def test_extract_pull_request_data(self, webhook_handler, sample_pr_event):
        """Test extraction of pull request data."""
        data = webhook_handler._extract_pull_request_data(sample_pr_event)
        
        assert data['pull_request_id'] == 1
        assert data['pull_request_number'] == 42
        assert data['repository'] == 'testorg/test-repo'
        assert data['author'] == 'testuser'
        assert data['title'] == 'Add new feature'
        assert data['base_branch'] == 'main'
        assert data['head_branch'] == 'feature-branch'
        assert data['head_sha'] == 'abc123'
    
    @pytest.mark.asyncio
    async def test_queue_for_processing_success(self, webhook_handler):
        """Test successful queuing of events."""
        webhook_handler.redis_client.rpush = AsyncMock(return_value=1)
        
        data = {
            'event_type': 'test',
            'repository': 'test/repo',
            'pull_request_number': 1
        }
        
        result = await webhook_handler._queue_for_processing(data, priority='normal')
        
        assert result is True
        webhook_handler.redis_client.rpush.assert_called_once()
    
    @pytest.mark.asyncio
    async def test_queue_for_processing_failure(self, webhook_handler):
        """Test failed queuing of events."""
        webhook_handler.redis_client.rpush = AsyncMock(
            side_effect=Exception("Redis error")
        )
        
        data = {'event_type': 'test'}
        
        result = await webhook_handler._queue_for_processing(data, priority='normal')
        
        assert result is False


class TestWebhookEventClass:
    """Test cases for WebhookEvent class."""
    
    def test_webhook_event_creation(self):
        """Test creating a WebhookEvent instance."""
        event = WebhookEvent(
            event_type=EventType.PULL_REQUEST,
            action='opened',
            payload={'test': 'data'},
            delivery_id='test-123'
        )
        
        assert event.event_type == EventType.PULL_REQUEST
        assert event.action == 'opened'
        assert event.payload == {'test': 'data'}
        assert event.delivery_id == 'test-123'
        assert isinstance(event.timestamp, datetime)
    
    def test_webhook_event_to_dict(self):
        """Test converting WebhookEvent to dictionary."""
        event = WebhookEvent(
            event_type=EventType.ISSUE_COMMENT,
            action='created',
            payload={'comment': 'test'}
        )
        
        event_dict = event.to_dict()
        
        assert event_dict['event_type'] == 'issue_comment'
        assert event_dict['action'] == 'created'
        assert event_dict['payload'] == {'comment': 'test'}
        assert 'timestamp' in event_dict
        assert 'delivery_id' in event_dict


class TestEventTypeEnum:
    """Test cases for EventType enum."""
    
    def test_event_type_values(self):
        """Test EventType enum values."""
        assert EventType.PULL_REQUEST.value == 'pull_request'
        assert EventType.PULL_REQUEST_REVIEW.value == 'pull_request_review'
        assert EventType.ISSUE_COMMENT.value == 'issue_comment'
        assert EventType.INSTALLATION.value == 'installation'
        assert EventType.PUSH.value == 'push'
        assert EventType.UNKNOWN.value == 'unknown'
    
    def test_event_type_from_string(self):
        """Test creating EventType from string."""
        # This would need to be implemented in the actual code
        # For now, test that the enum works as expected
        assert EventType('pull_request') == EventType.PULL_REQUEST
        assert EventType('issue_comment') == EventType.ISSUE_COMMENT


if __name__ == '__main__':
    pytest.main([__file__, '-v'])