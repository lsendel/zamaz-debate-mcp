#!/usr/bin/env python3
"""
Unit tests for the notification service module.
"""

import pytest
import asyncio
from unittest.mock import Mock, AsyncMock, patch, MagicMock
from datetime import datetime, timedelta
import aiohttp
import sys
import os

# Add parent directory to path for imports
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'scripts'))

from notification_service import (
    NotificationService, NotificationType, NotificationChannel,
    NotificationRecipient, NotificationContent, notify_review_completed,
    notify_critical_issue
)


class TestNotificationService:
    """Test cases for NotificationService."""
    
    @pytest.fixture
    def mock_config(self):
        """Mock configuration for testing."""
        return {
            'slack_webhook_url': 'https://hooks.slack.com/test',
            'email': {
                'smtp_server': 'smtp.test.com',
                'smtp_port': 587,
                'smtp_username': 'test@example.com',
                'smtp_password': 'test_password',
                'from_address': 'noreply@test.com'
            },
            'github_token': 'test_github_token',
            'default_preferences': {
                'slack_enabled': True,
                'email_enabled': True,
                'github_enabled': True
            }
        }
    
    @pytest.fixture
    def notification_service(self, mock_config):
        """Create a NotificationService instance for testing."""
        return NotificationService(mock_config)
    
    @pytest.fixture
    def sample_recipient(self):
        """Sample recipient for testing."""
        return NotificationRecipient(
            user_id='user123',
            username='testuser',
            email='testuser@example.com',
            slack_id='U12345678',
            preferences={
                'slack_enabled': True,
                'email_enabled': True,
                'webhook_url': 'https://example.com/webhook'
            }
        )
    
    @pytest.fixture
    def sample_content(self):
        """Sample notification content for testing."""
        return NotificationContent(
            type=NotificationType.REVIEW_COMPLETED,
            title='Code Review Complete',
            message='Your code review has been completed.',
            details={'total_issues': 5, 'critical_issues': 1},
            repository='testorg/testrepo',
            pull_request=42,
            priority='normal',
            action_url='https://github.com/testorg/testrepo/pull/42'
        )
    
    @pytest.mark.asyncio
    async def test_send_notification_all_channels(
        self, notification_service, sample_recipient, sample_content
    ):
        """Test sending notification through all channels."""
        # Mock the individual channel methods
        notification_service._send_slack_notification = AsyncMock(return_value=True)
        notification_service._send_email_notification = AsyncMock(return_value=True)
        notification_service._send_github_notification = AsyncMock(return_value=True)
        notification_service._send_webhook_notification = AsyncMock(return_value=True)
        notification_service._log_notification_attempt = AsyncMock()
        
        channels = [
            NotificationChannel.SLACK,
            NotificationChannel.EMAIL,
            NotificationChannel.GITHUB,
            NotificationChannel.WEBHOOK
        ]
        
        results = await notification_service.send_notification(
            sample_recipient, sample_content, channels
        )
        
        assert results[NotificationChannel.SLACK.value] is True
        assert results[NotificationChannel.EMAIL.value] is True
        assert results[NotificationChannel.GITHUB.value] is True
        assert results[NotificationChannel.WEBHOOK.value] is True
        
        notification_service._send_slack_notification.assert_called_once()
        notification_service._send_email_notification.assert_called_once()
        notification_service._send_github_notification.assert_called_once()
        notification_service._send_webhook_notification.assert_called_once()
    
    @pytest.mark.asyncio
    async def test_send_slack_notification_success(
        self, notification_service, sample_recipient, sample_content
    ):
        """Test successful Slack notification."""
        mock_response = Mock()
        mock_response.status = 200
        
        with patch('aiohttp.ClientSession') as mock_session:
            mock_post = AsyncMock(return_value=mock_response)
            mock_session.return_value.__aenter__.return_value.post = mock_post
            
            result = await notification_service._send_slack_notification(
                sample_recipient, sample_content
            )
            
            assert result is True
            mock_post.assert_called_once()
            
            # Verify the Slack message format
            call_args = mock_post.call_args
            assert call_args[0][0] == notification_service.slack_webhook_url
            assert 'json' in call_args[1]
            slack_message = call_args[1]['json']
            assert 'blocks' in slack_message
    
    @pytest.mark.asyncio
    async def test_send_slack_notification_failure(
        self, notification_service, sample_recipient, sample_content
    ):
        """Test failed Slack notification."""
        mock_response = Mock()
        mock_response.status = 400
        
        with patch('aiohttp.ClientSession') as mock_session:
            mock_post = AsyncMock(return_value=mock_response)
            mock_session.return_value.__aenter__.return_value.post = mock_post
            
            result = await notification_service._send_slack_notification(
                sample_recipient, sample_content
            )
            
            assert result is False
    
    def test_format_slack_message(self, notification_service, sample_recipient, sample_content):
        """Test Slack message formatting."""
        message = notification_service._format_slack_message(sample_recipient, sample_content)
        
        assert 'blocks' in message
        assert len(message['blocks']) > 0
        
        # Check header block
        header_block = message['blocks'][0]
        assert header_block['type'] == 'header'
        assert sample_content.title in header_block['text']['text']
        
        # Check if action button is included
        action_blocks = [b for b in message['blocks'] if b.get('type') == 'actions']
        assert len(action_blocks) > 0
        assert action_blocks[0]['elements'][0]['url'] == sample_content.action_url
        
        # Check mention format
        assert f"<@{sample_recipient.slack_id}>" in message.get('text', '')
    
    @pytest.mark.asyncio
    async def test_send_email_notification_success(
        self, notification_service, sample_recipient, sample_content
    ):
        """Test successful email notification."""
        with patch('smtplib.SMTP') as mock_smtp:
            mock_server = MagicMock()
            mock_smtp.return_value.__enter__.return_value = mock_server
            
            result = await notification_service._send_email_notification(
                sample_recipient, sample_content
            )
            
            assert result is True
            mock_server.starttls.assert_called_once()
            mock_server.login.assert_called_once()
            mock_server.send_message.assert_called_once()
    
    def test_format_email_html(self, notification_service, sample_recipient, sample_content):
        """Test HTML email formatting."""
        html = notification_service._format_email_html(sample_recipient, sample_content)
        
        assert sample_content.title in html
        assert sample_content.message in html
        assert sample_content.repository in html
        assert str(sample_content.pull_request) in html
        assert sample_content.action_url in html
        assert sample_recipient.email in html
    
    @pytest.mark.asyncio
    async def test_send_github_notification_success(
        self, notification_service, sample_recipient, sample_content
    ):
        """Test successful GitHub notification."""
        mock_response = Mock()
        mock_response.status = 201
        
        with patch('aiohttp.ClientSession') as mock_session:
            mock_post = AsyncMock(return_value=mock_response)
            mock_session.return_value.__aenter__.return_value.post = mock_post
            
            result = await notification_service._send_github_notification(
                sample_recipient, sample_content
            )
            
            assert result is True
            mock_post.assert_called_once()
            
            # Verify GitHub API call
            call_args = mock_post.call_args
            expected_url = f"https://api.github.com/repos/{sample_content.repository}/issues/{sample_content.pull_request}/comments"
            assert call_args[0][0] == expected_url
    
    @pytest.mark.asyncio
    async def test_send_webhook_notification_success(
        self, notification_service, sample_recipient, sample_content
    ):
        """Test successful webhook notification."""
        mock_response = Mock()
        mock_response.status = 200
        
        with patch('aiohttp.ClientSession') as mock_session:
            mock_post = AsyncMock(return_value=mock_response)
            mock_session.return_value.__aenter__.return_value.post = mock_post
            
            result = await notification_service._send_webhook_notification(
                sample_recipient, sample_content
            )
            
            assert result is True
            mock_post.assert_called_once()
            
            # Verify webhook payload
            call_args = mock_post.call_args
            assert call_args[0][0] == sample_recipient.preferences['webhook_url']
            payload = call_args[1]['json']
            assert payload['type'] == sample_content.type.value
            assert payload['title'] == sample_content.title
    
    def test_get_preferred_channels_with_preferences(
        self, notification_service, sample_recipient
    ):
        """Test getting preferred channels based on user preferences."""
        channels = notification_service._get_preferred_channels(
            sample_recipient, NotificationType.REVIEW_COMPLETED
        )
        
        assert NotificationChannel.SLACK in channels
        assert NotificationChannel.EMAIL in channels
        assert NotificationChannel.WEBHOOK in channels
    
    def test_get_preferred_channels_type_specific(self, notification_service):
        """Test getting channels for specific notification types."""
        recipient = NotificationRecipient(
            user_id='user456',
            username='testuser2',
            preferences={
                'notification_types': {
                    'critical_issue': {
                        'slack': True,
                        'email': True,
                        'github': False
                    }
                }
            }
        )
        
        channels = notification_service._get_preferred_channels(
            recipient, NotificationType.CRITICAL_ISSUE
        )
        
        assert NotificationChannel.SLACK in channels
        assert NotificationChannel.EMAIL in channels
        assert NotificationChannel.GITHUB not in channels
    
    @pytest.mark.asyncio
    async def test_queue_notification(self, notification_service, sample_recipient, sample_content):
        """Test queuing notifications for batch processing."""
        await notification_service.queue_notification(sample_recipient, sample_content)
        
        # Check if notification was queued
        assert notification_service.notification_queue.qsize() == 1
        
        queued_item = await notification_service.notification_queue.get()
        assert queued_item['recipient'] == sample_recipient
        assert queued_item['content'] == sample_content
        assert 'timestamp' in queued_item
    
    def test_create_digest_notification(self, notification_service, sample_recipient):
        """Test creating digest notification from multiple notifications."""
        notifications = []
        for i in range(5):
            notifications.append({
                'recipient': sample_recipient,
                'content': NotificationContent(
                    type=NotificationType.REVIEW_COMPLETED,
                    title=f'Review {i+1} Complete',
                    message=f'Review {i+1} has been completed.',
                    repository=f'repo{i+1}',
                    pull_request=i+1
                )
            })
        
        digest = notification_service._create_digest_notification(
            NotificationType.REVIEW_COMPLETED,
            notifications
        )
        
        assert digest.type == NotificationType.REVIEW_COMPLETED
        assert '5' in digest.title
        assert len(digest.message.split('\n')) > 5  # Multiple lines for each notification
    
    @pytest.mark.asyncio
    async def test_notify_review_completed_helper(
        self, notification_service, sample_recipient
    ):
        """Test the notify_review_completed helper function."""
        notification_service.send_notification = AsyncMock(
            return_value={'slack': True, 'email': True}
        )
        
        summary = {
            'total_issues': 10,
            'critical_issues': 2,
            'suggestions_made': 15,
            'time_saved': '30 minutes'
        }
        
        result = await notify_review_completed(
            notification_service,
            sample_recipient,
            'testorg/testrepo',
            42,
            summary
        )
        
        assert result['slack'] is True
        assert result['email'] is True
        
        # Verify the notification content
        call_args = notification_service.send_notification.call_args
        content = call_args[0][1]
        assert content.type == NotificationType.REVIEW_COMPLETED
        assert content.repository == 'testorg/testrepo'
        assert content.pull_request == 42
        assert content.details == summary
    
    @pytest.mark.asyncio
    async def test_notify_critical_issue_helper(
        self, notification_service, sample_recipient
    ):
        """Test the notify_critical_issue helper function."""
        notification_service.send_notification = AsyncMock(
            return_value={'slack': True, 'email': True, 'github': True}
        )
        
        issue = {
            'type': 'SQL Injection',
            'message': 'Potential SQL injection vulnerability detected',
            'severity': 'critical',
            'file': 'api/users.py',
            'line': 42
        }
        
        result = await notify_critical_issue(
            notification_service,
            sample_recipient,
            'testorg/testrepo',
            100,
            issue
        )
        
        assert all(result.values())  # All channels should succeed
        
        # Verify the notification content
        call_args = notification_service.send_notification.call_args
        content = call_args[0][1]
        assert content.type == NotificationType.CRITICAL_ISSUE
        assert content.priority == 'critical'
        assert content.details == issue
    
    @pytest.mark.asyncio
    async def test_send_weekly_summary(self, notification_service, sample_recipient):
        """Test sending weekly summary notification."""
        notification_service.send_notification = AsyncMock(
            return_value={'email': True}
        )
        
        summary_data = {
            'total_reviews': 50,
            'issues_found': 150,
            'issues_fixed': 120,
            'top_repositories': [
                {'name': 'repo1', 'reviews': 20, 'issues': 60},
                {'name': 'repo2', 'reviews': 15, 'issues': 45}
            ]
        }
        
        result = await notification_service.send_weekly_summary(
            sample_recipient,
            summary_data
        )
        
        assert result['email'] is True
        
        # Verify only email channel was used for weekly summary
        call_args = notification_service.send_notification.call_args
        channels = call_args[0][2]
        assert channels == [NotificationChannel.EMAIL]


class TestNotificationClasses:
    """Test notification data classes."""
    
    def test_notification_type_enum(self):
        """Test NotificationType enum values."""
        assert NotificationType.REVIEW_COMPLETED.value == 'review_completed'
        assert NotificationType.CRITICAL_ISSUE.value == 'critical_issue'
        assert NotificationType.WEEKLY_SUMMARY.value == 'weekly_summary'
    
    def test_notification_channel_enum(self):
        """Test NotificationChannel enum values."""
        assert NotificationChannel.SLACK.value == 'slack'
        assert NotificationChannel.EMAIL.value == 'email'
        assert NotificationChannel.GITHUB.value == 'github'
        assert NotificationChannel.WEBHOOK.value == 'webhook'
    
    def test_notification_recipient_creation(self):
        """Test creating NotificationRecipient."""
        recipient = NotificationRecipient(
            user_id='123',
            username='testuser',
            email='test@example.com',
            slack_id='U123',
            preferences={'slack_enabled': True}
        )
        
        assert recipient.user_id == '123'
        assert recipient.username == 'testuser'
        assert recipient.email == 'test@example.com'
        assert recipient.slack_id == 'U123'
        assert recipient.preferences['slack_enabled'] is True
    
    def test_notification_content_creation(self):
        """Test creating NotificationContent."""
        content = NotificationContent(
            type=NotificationType.REVIEW_COMPLETED,
            title='Test Title',
            message='Test Message',
            details={'key': 'value'},
            repository='test/repo',
            pull_request=1,
            priority='high',
            action_url='https://example.com'
        )
        
        assert content.type == NotificationType.REVIEW_COMPLETED
        assert content.title == 'Test Title'
        assert content.message == 'Test Message'
        assert content.details == {'key': 'value'}
        assert content.repository == 'test/repo'
        assert content.pull_request == 1
        assert content.priority == 'high'
        assert content.action_url == 'https://example.com'


if __name__ == '__main__':
    pytest.main([__file__, '-v'])