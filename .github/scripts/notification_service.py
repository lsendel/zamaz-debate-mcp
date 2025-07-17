#!/usr/bin/env python3
"""
Notification service for Kiro GitHub integration.
Handles Slack, email, and GitHub notifications for code review events.
"""

import os
import json
import asyncio
import aiohttp
import smtplib
import logging
from typing import Dict, List, Any, Optional, Union
from datetime import datetime, timedelta
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from dataclasses import dataclass, asdict
from enum import Enum

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('kiro_notifications')

class NotificationType(Enum):
    """Types of notifications."""
    REVIEW_COMPLETED = "review_completed"
    CRITICAL_ISSUE = "critical_issue"
    REVIEW_REQUEST = "review_request"
    SUGGESTION_ACCEPTED = "suggestion_accepted"
    WEEKLY_SUMMARY = "weekly_summary"
    DAILY_DIGEST = "daily_digest"
    MENTION = "mention"
    ERROR_ALERT = "error_alert"

class NotificationChannel(Enum):
    """Notification delivery channels."""
    SLACK = "slack"
    EMAIL = "email"
    GITHUB = "github"
    WEBHOOK = "webhook"

@dataclass
class NotificationRecipient:
    """Recipient information for notifications."""
    user_id: str
    username: str
    email: Optional[str] = None
    slack_id: Optional[str] = None
    preferences: Optional[Dict[str, Any]] = None

@dataclass
class NotificationContent:
    """Content for a notification."""
    type: NotificationType
    title: str
    message: str
    details: Optional[Dict[str, Any]] = None
    repository: Optional[str] = None
    pull_request: Optional[int] = None
    priority: str = "normal"
    action_url: Optional[str] = None

class NotificationService:
    """Manages notification delivery across multiple channels."""
    
    def __init__(self, config: Dict[str, Any]):
        """Initialize the notification service.
        
        Args:
            config: Configuration including API keys and preferences
        """
        self.config = config
        self.slack_webhook_url = config.get('slack_webhook_url')
        self.email_config = config.get('email', {})
        self.github_token = config.get('github_token')
        self.default_preferences = config.get('default_preferences', {})
        
        # Queue for batching notifications
        self.notification_queue = asyncio.Queue()
        self.batch_interval = config.get('batch_interval', 300)  # 5 minutes
        
    async def send_notification(
        self,
        recipient: NotificationRecipient,
        content: NotificationContent,
        channels: Optional[List[NotificationChannel]] = None
    ) -> Dict[str, bool]:
        """Send a notification through specified channels.
        
        Args:
            recipient: The recipient of the notification
            content: The notification content
            channels: List of channels to use (defaults to user preferences)
            
        Returns:
            Dictionary indicating success/failure for each channel
        """
        if channels is None:
            channels = self._get_preferred_channels(recipient, content.type)
        
        results = {}
        
        for channel in channels:
            try:
                if channel == NotificationChannel.SLACK:
                    results[channel.value] = await self._send_slack_notification(
                        recipient, content
                    )
                elif channel == NotificationChannel.EMAIL:
                    results[channel.value] = await self._send_email_notification(
                        recipient, content
                    )
                elif channel == NotificationChannel.GITHUB:
                    results[channel.value] = await self._send_github_notification(
                        recipient, content
                    )
                elif channel == NotificationChannel.WEBHOOK:
                    results[channel.value] = await self._send_webhook_notification(
                        recipient, content
                    )
            except Exception as e:
                logger.error(f"Error sending {channel.value} notification: {str(e)}")
                results[channel.value] = False
        
        # Log notification attempt
        await self._log_notification_attempt(recipient, content, results)
        
        return results
    
    async def _send_slack_notification(
        self,
        recipient: NotificationRecipient,
        content: NotificationContent
    ) -> bool:
        """Send a notification via Slack."""
        if not self.slack_webhook_url:
            logger.warning("Slack webhook URL not configured")
            return False
        
        # Format message for Slack
        slack_message = self._format_slack_message(recipient, content)
        
        try:
            async with aiohttp.ClientSession() as session:
                async with session.post(
                    self.slack_webhook_url,
                    json=slack_message,
                    headers={'Content-Type': 'application/json'}
                ) as response:
                    return response.status == 200
        except Exception as e:
            logger.error(f"Slack notification error: {str(e)}")
            return False
    
    def _format_slack_message(
        self,
        recipient: NotificationRecipient,
        content: NotificationContent
    ) -> Dict[str, Any]:
        """Format notification content for Slack."""
        # Color based on priority
        color_map = {
            "critical": "#dc3545",
            "high": "#fd7e14",
            "normal": "#0366d6",
            "low": "#28a745"
        }
        color = color_map.get(content.priority, "#0366d6")
        
        # Build message blocks
        blocks = [
            {
                "type": "header",
                "text": {
                    "type": "plain_text",
                    "text": content.title
                }
            },
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": content.message
                }
            }
        ]
        
        # Add context if available
        if content.repository and content.pull_request:
            blocks.append({
                "type": "context",
                "elements": [
                    {
                        "type": "mrkdwn",
                        "text": f"*Repository:* {content.repository} | *PR:* #{content.pull_request}"
                    }
                ]
            })
        
        # Add action button if URL provided
        if content.action_url:
            blocks.append({
                "type": "actions",
                "elements": [
                    {
                        "type": "button",
                        "text": {
                            "type": "plain_text",
                            "text": "View Details"
                        },
                        "url": content.action_url,
                        "style": "primary"
                    }
                ]
            })
        
        # Build attachments for details
        attachments = []
        if content.details:
            fields = []
            for key, value in content.details.items():
                if isinstance(value, (str, int, float)):
                    fields.append({
                        "title": key.replace('_', ' ').title(),
                        "value": str(value),
                        "short": True
                    })
            
            if fields:
                attachments.append({
                    "color": color,
                    "fields": fields[:10]  # Limit to 10 fields
                })
        
        message = {
            "blocks": blocks,
            "attachments": attachments
        }
        
        # Add mention if recipient has Slack ID
        if recipient.slack_id:
            message["text"] = f"<@{recipient.slack_id}> {content.title}"
        
        return message
    
    async def _send_email_notification(
        self,
        recipient: NotificationRecipient,
        content: NotificationContent
    ) -> bool:
        """Send a notification via email."""
        if not recipient.email or not self.email_config.get('smtp_server'):
            logger.warning("Email not configured for recipient")
            return False
        
        try:
            # Create email message
            msg = MIMEMultipart('alternative')
            msg['Subject'] = f"[Kiro] {content.title}"
            msg['From'] = self.email_config.get('from_address', 'noreply@kiro.ai')
            msg['To'] = recipient.email
            
            # Create HTML and plain text parts
            html_body = self._format_email_html(recipient, content)
            text_body = self._format_email_text(recipient, content)
            
            msg.attach(MIMEText(text_body, 'plain'))
            msg.attach(MIMEText(html_body, 'html'))
            
            # Send email
            with smtplib.SMTP(
                self.email_config['smtp_server'],
                self.email_config.get('smtp_port', 587)
            ) as server:
                if self.email_config.get('use_tls', True):
                    server.starttls()
                
                if self.email_config.get('smtp_username'):
                    server.login(
                        self.email_config['smtp_username'],
                        self.email_config['smtp_password']
                    )
                
                server.send_message(msg)
                return True
                
        except Exception as e:
            logger.error(f"Email notification error: {str(e)}")
            return False
    
    def _format_email_html(
        self,
        recipient: NotificationRecipient,
        content: NotificationContent
    ) -> str:
        """Format notification content as HTML email."""
        priority_colors = {
            "critical": "#dc3545",
            "high": "#fd7e14",
            "normal": "#0366d6",
            "low": "#28a745"
        }
        
        html = f"""
        <html>
        <body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #24292e;">
            <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background: linear-gradient(135deg, #0366d6, #0969da); padding: 20px; border-radius: 8px 8px 0 0;">
                    <h1 style="color: white; margin: 0; font-size: 24px;">Kiro Notification</h1>
                </div>
                
                <div style="background: white; padding: 30px; border: 1px solid #d1d9e0; border-radius: 0 0 8px 8px;">
                    <h2 style="color: #1f2328; margin-top: 0;">{content.title}</h2>
                    
                    <div style="background: #f6f8fa; padding: 15px; border-radius: 6px; margin: 20px 0;">
                        <p style="margin: 0;">{content.message}</p>
                    </div>
        """
        
        # Add repository and PR info
        if content.repository:
            html += f"""
                    <p style="color: #656d76; font-size: 14px;">
                        <strong>Repository:</strong> {content.repository}
                        {f' | <strong>Pull Request:</strong> #{content.pull_request}' if content.pull_request else ''}
                    </p>
            """
        
        # Add details if present
        if content.details:
            html += """
                    <div style="margin-top: 20px;">
                        <h3 style="color: #1f2328; font-size: 16px;">Details:</h3>
                        <table style="width: 100%; border-collapse: collapse;">
            """
            
            for key, value in content.details.items():
                if isinstance(value, (str, int, float)):
                    html += f"""
                            <tr>
                                <td style="padding: 8px; border-bottom: 1px solid #f1f8ff; font-weight: 500;">
                                    {key.replace('_', ' ').title()}
                                </td>
                                <td style="padding: 8px; border-bottom: 1px solid #f1f8ff;">
                                    {value}
                                </td>
                            </tr>
                    """
            
            html += """
                        </table>
                    </div>
            """
        
        # Add action button
        if content.action_url:
            html += f"""
                    <div style="margin-top: 30px; text-align: center;">
                        <a href="{content.action_url}" 
                           style="display: inline-block; background: #238636; color: white; 
                                  padding: 12px 24px; text-decoration: none; border-radius: 6px; 
                                  font-weight: 500;">
                            View Details
                        </a>
                    </div>
            """
        
        # Add footer
        html += f"""
                    <div style="margin-top: 30px; padding-top: 20px; border-top: 1px solid #d1d9e0; 
                                font-size: 12px; color: #656d76;">
                        <p>This notification was sent to {recipient.email} based on your preferences.</p>
                        <p>
                            <a href="#" style="color: #0366d6;">Manage notification preferences</a> | 
                            <a href="#" style="color: #0366d6;">Unsubscribe</a>
                        </p>
                    </div>
                </div>
            </div>
        </body>
        </html>
        """
        
        return html
    
    def _format_email_text(
        self,
        recipient: NotificationRecipient,
        content: NotificationContent
    ) -> str:
        """Format notification content as plain text email."""
        text = f"""
Kiro Notification

{content.title}
{'=' * len(content.title)}

{content.message}
"""
        
        if content.repository:
            text += f"\nRepository: {content.repository}"
            if content.pull_request:
                text += f"\nPull Request: #{content.pull_request}"
        
        if content.details:
            text += "\n\nDetails:\n"
            for key, value in content.details.items():
                if isinstance(value, (str, int, float)):
                    text += f"  {key.replace('_', ' ').title()}: {value}\n"
        
        if content.action_url:
            text += f"\n\nView details: {content.action_url}\n"
        
        text += f"""

--
This notification was sent to {recipient.email} based on your preferences.
Manage preferences: https://github.com/apps/kiro/preferences
"""
        
        return text
    
    async def _send_github_notification(
        self,
        recipient: NotificationRecipient,
        content: NotificationContent
    ) -> bool:
        """Send a notification via GitHub (comment or status)."""
        if not self.github_token or not content.repository:
            logger.warning("GitHub notification requires token and repository")
            return False
        
        try:
            headers = {
                'Authorization': f'token {self.github_token}',
                'Accept': 'application/vnd.github.v3+json'
            }
            
            # If this is for a PR, add a comment
            if content.pull_request:
                url = f"https://api.github.com/repos/{content.repository}/issues/{content.pull_request}/comments"
                
                comment_body = self._format_github_comment(content)
                
                async with aiohttp.ClientSession() as session:
                    async with session.post(
                        url,
                        headers=headers,
                        json={'body': comment_body}
                    ) as response:
                        return response.status == 201
            
            return False
            
        except Exception as e:
            logger.error(f"GitHub notification error: {str(e)}")
            return False
    
    def _format_github_comment(self, content: NotificationContent) -> str:
        """Format notification as a GitHub comment."""
        comment = f"### {content.title}\n\n{content.message}\n"
        
        if content.details:
            comment += "\n#### Details\n"
            for key, value in content.details.items():
                if isinstance(value, (str, int, float)):
                    comment += f"- **{key.replace('_', ' ').title()}**: {value}\n"
        
        return comment
    
    async def _send_webhook_notification(
        self,
        recipient: NotificationRecipient,
        content: NotificationContent
    ) -> bool:
        """Send a notification via custom webhook."""
        webhook_url = recipient.preferences.get('webhook_url') if recipient.preferences else None
        
        if not webhook_url:
            logger.warning("No webhook URL configured for recipient")
            return False
        
        try:
            payload = {
                'type': content.type.value,
                'title': content.title,
                'message': content.message,
                'priority': content.priority,
                'repository': content.repository,
                'pull_request': content.pull_request,
                'details': content.details,
                'action_url': content.action_url,
                'recipient': {
                    'user_id': recipient.user_id,
                    'username': recipient.username
                },
                'timestamp': datetime.now().isoformat()
            }
            
            async with aiohttp.ClientSession() as session:
                async with session.post(
                    webhook_url,
                    json=payload,
                    headers={'Content-Type': 'application/json'}
                ) as response:
                    return response.status in [200, 201, 202, 204]
                    
        except Exception as e:
            logger.error(f"Webhook notification error: {str(e)}")
            return False
    
    def _get_preferred_channels(
        self,
        recipient: NotificationRecipient,
        notification_type: NotificationType
    ) -> List[NotificationChannel]:
        """Get preferred notification channels for a recipient and type."""
        preferences = recipient.preferences or self.default_preferences
        
        # Check if user has type-specific preferences
        type_prefs = preferences.get('notification_types', {}).get(
            notification_type.value, {}
        )
        
        # Get enabled channels
        channels = []
        
        if type_prefs.get('slack', preferences.get('slack_enabled', True)):
            channels.append(NotificationChannel.SLACK)
        
        if type_prefs.get('email', preferences.get('email_enabled', True)):
            channels.append(NotificationChannel.EMAIL)
        
        if type_prefs.get('github', preferences.get('github_enabled', True)):
            channels.append(NotificationChannel.GITHUB)
        
        if type_prefs.get('webhook', preferences.get('webhook_enabled', False)):
            channels.append(NotificationChannel.WEBHOOK)
        
        return channels
    
    async def queue_notification(
        self,
        recipient: NotificationRecipient,
        content: NotificationContent
    ):
        """Queue a notification for batch processing."""
        await self.notification_queue.put({
            'recipient': recipient,
            'content': content,
            'timestamp': datetime.now()
        })
    
    async def process_notification_queue(self):
        """Process queued notifications in batches."""
        while True:
            notifications = []
            deadline = datetime.now() + timedelta(seconds=self.batch_interval)
            
            # Collect notifications for batch
            while datetime.now() < deadline:
                try:
                    timeout = (deadline - datetime.now()).total_seconds()
                    notification = await asyncio.wait_for(
                        self.notification_queue.get(),
                        timeout=max(timeout, 0.1)
                    )
                    notifications.append(notification)
                except asyncio.TimeoutError:
                    break
            
            if notifications:
                await self._process_notification_batch(notifications)
    
    async def _process_notification_batch(
        self,
        notifications: List[Dict[str, Any]]
    ):
        """Process a batch of notifications."""
        # Group by recipient and type for digest
        grouped = {}
        
        for notif in notifications:
            recipient_id = notif['recipient'].user_id
            notif_type = notif['content'].type
            
            key = (recipient_id, notif_type)
            if key not in grouped:
                grouped[key] = []
            grouped[key].append(notif)
        
        # Send individual or digest notifications
        for (recipient_id, notif_type), group in grouped.items():
            if len(group) == 1:
                # Send individual notification
                await self.send_notification(
                    group[0]['recipient'],
                    group[0]['content']
                )
            else:
                # Create and send digest
                digest_content = self._create_digest_notification(
                    notif_type,
                    group
                )
                await self.send_notification(
                    group[0]['recipient'],
                    digest_content
                )
    
    def _create_digest_notification(
        self,
        notification_type: NotificationType,
        notifications: List[Dict[str, Any]]
    ) -> NotificationContent:
        """Create a digest notification from multiple notifications."""
        count = len(notifications)
        
        # Create summary title
        title = f"{count} {notification_type.value.replace('_', ' ').title()} Notifications"
        
        # Build message
        message_parts = [f"You have {count} new notifications:"]
        
        for i, notif in enumerate(notifications[:5]):  # Show first 5
            content = notif['content']
            message_parts.append(f"\n{i+1}. {content.title}")
            if content.repository:
                message_parts.append(f"   Repository: {content.repository}")
        
        if count > 5:
            message_parts.append(f"\n... and {count - 5} more")
        
        # Combine details
        combined_details = {}
        for notif in notifications:
            if notif['content'].details:
                for key, value in notif['content'].details.items():
                    if key not in combined_details:
                        combined_details[key] = []
                    combined_details[key].append(value)
        
        return NotificationContent(
            type=notification_type,
            title=title,
            message='\n'.join(message_parts),
            details=combined_details,
            priority="normal"
        )
    
    async def _log_notification_attempt(
        self,
        recipient: NotificationRecipient,
        content: NotificationContent,
        results: Dict[str, bool]
    ):
        """Log notification attempt for analytics."""
        log_entry = {
            'timestamp': datetime.now().isoformat(),
            'recipient_id': recipient.user_id,
            'notification_type': content.type.value,
            'channels': results,
            'success': any(results.values()),
            'repository': content.repository,
            'pull_request': content.pull_request
        }
        
        logger.info(f"Notification attempt: {log_entry}")
    
    async def send_weekly_summary(
        self,
        recipient: NotificationRecipient,
        summary_data: Dict[str, Any]
    ) -> Dict[str, bool]:
        """Send a weekly summary notification."""
        # Build summary content
        total_reviews = summary_data.get('total_reviews', 0)
        issues_found = summary_data.get('issues_found', 0)
        issues_fixed = summary_data.get('issues_fixed', 0)
        top_repositories = summary_data.get('top_repositories', [])
        
        message = f"""
Weekly Kiro Summary for {recipient.username}

📊 Activity Overview:
• {total_reviews} pull requests reviewed
• {issues_found} issues identified
• {issues_fixed} issues fixed ({(issues_fixed/issues_found*100):.1f}% fix rate)

🏆 Top Repositories:
"""
        
        for repo in top_repositories[:5]:
            message += f"• {repo['name']}: {repo['reviews']} reviews, {repo['issues']} issues\n"
        
        content = NotificationContent(
            type=NotificationType.WEEKLY_SUMMARY,
            title="Your Weekly Kiro Summary",
            message=message,
            details=summary_data,
            priority="low"
        )
        
        return await self.send_notification(
            recipient,
            content,
            [NotificationChannel.EMAIL]  # Weekly summaries via email only
        )

# Helper functions for external use
async def notify_review_completed(
    service: NotificationService,
    recipient: NotificationRecipient,
    repository: str,
    pull_request: int,
    summary: Dict[str, Any]
) -> Dict[str, bool]:
    """Send a review completed notification."""
    content = NotificationContent(
        type=NotificationType.REVIEW_COMPLETED,
        title=f"Code Review Completed for PR #{pull_request}",
        message=f"Kiro has completed reviewing your pull request in {repository}.",
        details=summary,
        repository=repository,
        pull_request=pull_request,
        action_url=f"https://github.com/{repository}/pull/{pull_request}"
    )
    
    return await service.send_notification(recipient, content)

async def notify_critical_issue(
    service: NotificationService,
    recipient: NotificationRecipient,
    repository: str,
    pull_request: int,
    issue: Dict[str, Any]
) -> Dict[str, bool]:
    """Send a critical issue notification."""
    content = NotificationContent(
        type=NotificationType.CRITICAL_ISSUE,
        title="Critical Issue Found",
        message=f"Kiro identified a critical issue in PR #{pull_request}: {issue['message']}",
        details=issue,
        repository=repository,
        pull_request=pull_request,
        priority="critical",
        action_url=f"https://github.com/{repository}/pull/{pull_request}"
    )
    
    return await service.send_notification(recipient, content)

if __name__ == "__main__":
    # Example usage
    config = {
        'slack_webhook_url': os.getenv('SLACK_WEBHOOK_URL'),
        'email': {
            'smtp_server': os.getenv('SMTP_SERVER'),
            'smtp_port': 587,
            'smtp_username': os.getenv('SMTP_USERNAME'),
            'smtp_password': os.getenv('SMTP_PASSWORD'),
            'from_address': 'noreply@kiro.ai'
        },
        'github_token': os.getenv('GITHUB_TOKEN')
    }
    
    service = NotificationService(config)
    
    # Example recipient
    recipient = NotificationRecipient(
        user_id='user123',
        username='johndoe',
        email='john@example.com',
        slack_id='U12345678'
    )
    
    # Send a test notification
    async def test():
        result = await notify_review_completed(
            service,
            recipient,
            'myorg/myrepo',
            42,
            {
                'total_issues': 5,
                'critical_issues': 0,
                'suggestions_made': 12,
                'estimated_time_saved': '30 minutes'
            }
        )
        print(f"Notification results: {result}")
    
    asyncio.run(test())