#!/usr/bin/env python3
"""
Slack integration for Kiro GitHub notifications.
Provides rich Slack app functionality with interactive components.
"""

import os
import json
import asyncio
import aiohttp
import logging
from typing import Dict, List, Any, Optional
from datetime import datetime
from slack_sdk import WebClient
from slack_sdk.errors import SlackApiError
from slack_bolt.async_app import AsyncApp
from slack_bolt.adapter.socket_mode.async_handler import AsyncSocketModeHandler

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger('kiro_slack')

class SlackIntegration:
    """Manages Slack integration for Kiro notifications."""
    
    def __init__(self, config: Dict[str, Any]):
        """Initialize Slack integration.
        
        Args:
            config: Configuration including tokens and settings
        """
        self.config = config
        self.bot_token = config.get('bot_token')
        self.app_token = config.get('app_token')
        self.signing_secret = config.get('signing_secret')
        
        # Initialize Slack client
        self.client = WebClient(token=self.bot_token)
        
        # Initialize Bolt app
        self.app = AsyncApp(
            token=self.bot_token,
            signing_secret=self.signing_secret
        )
        
        # Register handlers
        self._register_handlers()
        
        # Channel mappings
        self.channel_mappings = config.get('channel_mappings', {})
        self.default_channel = config.get('default_channel', '#code-reviews')
    
    def _register_handlers(self):
        """Register Slack event and command handlers."""
        # Command handlers
        self.app.command("/kiro")(self.handle_kiro_command)
        self.app.command("/kiro-review")(self.handle_review_command)
        self.app.command("/kiro-stats")(self.handle_stats_command)
        
        # Action handlers
        self.app.action("approve_suggestion")(self.handle_approve_suggestion)
        self.app.action("reject_suggestion")(self.handle_reject_suggestion)
        self.app.action("view_details")(self.handle_view_details)
        self.app.action("configure_notifications")(self.handle_configure_notifications)
        
        # View submission handlers
        self.app.view("configure_modal")(self.handle_configuration_submission)
        
        # Event handlers
        self.app.event("app_mention")(self.handle_app_mention)
        self.app.event("message")(self.handle_message)
    
    async def send_review_notification(
        self,
        channel: str,
        repository: str,
        pull_request: int,
        review_summary: Dict[str, Any],
        author: str,
        pr_title: str
    ) -> bool:
        """Send a code review notification to Slack."""
        try:
            # Build review blocks
            blocks = self._build_review_blocks(
                repository, pull_request, review_summary, author, pr_title
            )
            
            # Send message
            response = await self.client.chat_postMessage(
                channel=channel,
                text=f"Code review completed for {repository} PR #{pull_request}",
                blocks=blocks
            )
            
            return response["ok"]
            
        except SlackApiError as e:
            logger.error(f"Slack API error: {e.response['error']}")
            return False
    
    def _build_review_blocks(
        self,
        repository: str,
        pull_request: int,
        review_summary: Dict[str, Any],
        author: str,
        pr_title: str
    ) -> List[Dict[str, Any]]:
        """Build Slack blocks for review notification."""
        total_issues = review_summary.get('total_issues', 0)
        critical_issues = review_summary.get('critical_issues', 0)
        suggestions = review_summary.get('suggestions_made', 0)
        
        # Determine status emoji and color
        if critical_issues > 0:
            status_emoji = "🔴"
            status_text = "Critical Issues Found"
        elif total_issues > 5:
            status_emoji = "🟡"
            status_text = "Multiple Issues Found"
        elif total_issues > 0:
            status_emoji = "🟢"
            status_text = "Minor Issues Found"
        else:
            status_emoji = "✅"
            status_text = "Looks Good!"
        
        blocks = [
            {
                "type": "header",
                "text": {
                    "type": "plain_text",
                    "text": f"{status_emoji} Code Review Complete"
                }
            },
            {
                "type": "section",
                "fields": [
                    {
                        "type": "mrkdwn",
                        "text": f"*Repository:*\n`{repository}`"
                    },
                    {
                        "type": "mrkdwn",
                        "text": f"*Pull Request:*\n<https://github.com/{repository}/pull/{pull_request}|#{pull_request}>"
                    },
                    {
                        "type": "mrkdwn",
                        "text": f"*Author:*\n{author}"
                    },
                    {
                        "type": "mrkdwn",
                        "text": f"*Title:*\n{pr_title}"
                    }
                ]
            },
            {
                "type": "divider"
            },
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": f"*Review Summary:* {status_text}"
                }
            },
            {
                "type": "section",
                "fields": [
                    {
                        "type": "mrkdwn",
                        "text": f"*Total Issues:*\n{total_issues}"
                    },
                    {
                        "type": "mrkdwn",
                        "text": f"*Critical Issues:*\n{critical_issues}"
                    },
                    {
                        "type": "mrkdwn",
                        "text": f"*Suggestions:*\n{suggestions}"
                    },
                    {
                        "type": "mrkdwn",
                        "text": f"*Est. Time Saved:*\n{review_summary.get('time_saved', 'N/A')}"
                    }
                ]
            }
        ]
        
        # Add top issues if present
        if review_summary.get('top_issues'):
            issues_text = "*Top Issues Found:*\n"
            for i, issue in enumerate(review_summary['top_issues'][:3], 1):
                severity_emoji = {
                    'critical': '🔴',
                    'high': '🟠',
                    'medium': '🟡',
                    'low': '🟢'
                }.get(issue.get('severity', 'medium'), '🟡')
                
                issues_text += f"{i}. {severity_emoji} {issue['message']}\n"
            
            blocks.append({
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": issues_text
                }
            })
        
        # Add actions
        blocks.append({
            "type": "actions",
            "elements": [
                {
                    "type": "button",
                    "text": {
                        "type": "plain_text",
                        "text": "View on GitHub"
                    },
                    "url": f"https://github.com/{repository}/pull/{pull_request}",
                    "style": "primary"
                },
                {
                    "type": "button",
                    "text": {
                        "type": "plain_text",
                        "text": "View Details"
                    },
                    "action_id": "view_details",
                    "value": json.dumps({
                        "repository": repository,
                        "pull_request": pull_request
                    })
                },
                {
                    "type": "button",
                    "text": {
                        "type": "plain_text",
                        "text": "Configure"
                    },
                    "action_id": "configure_notifications",
                    "value": repository
                }
            ]
        })
        
        return blocks
    
    async def send_suggestion_card(
        self,
        channel: str,
        suggestion: Dict[str, Any],
        context: Dict[str, Any]
    ) -> bool:
        """Send an interactive suggestion card."""
        try:
            blocks = [
                {
                    "type": "header",
                    "text": {
                        "type": "plain_text",
                        "text": "💡 Kiro Suggestion"
                    }
                },
                {
                    "type": "section",
                    "text": {
                        "type": "mrkdwn",
                        "text": f"*File:* `{context['file_path']}`\n*Line:* {context['line_number']}"
                    }
                },
                {
                    "type": "section",
                    "text": {
                        "type": "mrkdwn",
                        "text": f"```{context['language']}\n{context['code_snippet']}\n```"
                    }
                },
                {
                    "type": "section",
                    "text": {
                        "type": "mrkdwn",
                        "text": f"*Suggestion:*\n{suggestion['message']}"
                    }
                },
                {
                    "type": "section",
                    "text": {
                        "type": "mrkdwn",
                        "text": f"*Proposed Fix:*\n```{context['language']}\n{suggestion['fix_code']}\n```"
                    }
                },
                {
                    "type": "actions",
                    "elements": [
                        {
                            "type": "button",
                            "text": {
                                "type": "plain_text",
                                "text": "✅ Apply Fix"
                            },
                            "style": "primary",
                            "action_id": "approve_suggestion",
                            "value": json.dumps(suggestion)
                        },
                        {
                            "type": "button",
                            "text": {
                                "type": "plain_text",
                                "text": "❌ Dismiss"
                            },
                            "action_id": "reject_suggestion",
                            "value": json.dumps(suggestion)
                        }
                    ]
                }
            ]
            
            response = await self.client.chat_postMessage(
                channel=channel,
                text="New code suggestion from Kiro",
                blocks=blocks
            )
            
            return response["ok"]
            
        except SlackApiError as e:
            logger.error(f"Slack API error: {e.response['error']}")
            return False
    
    async def handle_kiro_command(self, ack, command, client):
        """Handle the /kiro command."""
        await ack()
        
        user_id = command['user_id']
        text = command['text'].strip()
        
        if not text or text == 'help':
            await self._send_help_message(client, user_id)
        elif text == 'status':
            await self._send_status_message(client, user_id)
        elif text.startswith('review '):
            await self._trigger_review(client, user_id, text[7:])
        else:
            await client.chat_postEphemeral(
                channel=command['channel_id'],
                user=user_id,
                text=f"Unknown command: `{text}`. Use `/kiro help` for available commands."
            )
    
    async def _send_help_message(self, client, user_id):
        """Send help message for /kiro command."""
        help_text = """
*Kiro Commands:*

• `/kiro help` - Show this help message
• `/kiro status` - Check Kiro's status and configuration
• `/kiro review <PR_URL>` - Trigger a manual review of a PR
• `/kiro-review <repository> <PR_number>` - Quick review trigger
• `/kiro-stats [repository]` - View code review statistics

*Interactive Features:*
• Mention @Kiro in a PR link to trigger review
• React with 👀 to request re-review
• Use thread replies for follow-up questions

*Configuration:*
• Click "Configure" on any review notification
• Set per-repository preferences
• Customize notification settings
        """
        
        await client.chat_postEphemeral(
            channel=user_id,
            user=user_id,
            text=help_text
        )
    
    async def handle_review_command(self, ack, command, client):
        """Handle the /kiro-review command."""
        await ack()
        
        parts = command['text'].strip().split()
        if len(parts) != 2:
            await client.chat_postEphemeral(
                channel=command['channel_id'],
                user=command['user_id'],
                text="Usage: `/kiro-review <repository> <PR_number>`\nExample: `/kiro-review myorg/myrepo 123`"
            )
            return
        
        repository, pr_number = parts
        
        # Trigger review
        await client.chat_postMessage(
            channel=command['channel_id'],
            text=f"🔍 Initiating review for {repository} PR #{pr_number}..."
        )
        
        # Here you would trigger the actual review process
        # For now, we'll simulate it
        await asyncio.sleep(2)
        
        await self.send_review_notification(
            command['channel_id'],
            repository,
            int(pr_number),
            {
                'total_issues': 3,
                'critical_issues': 0,
                'suggestions_made': 5,
                'time_saved': '15 minutes'
            },
            command['user_name'],
            "Sample PR Title"
        )
    
    async def handle_stats_command(self, ack, command, client):
        """Handle the /kiro-stats command."""
        await ack()
        
        repository = command['text'].strip() or None
        
        # Generate stats (this would fetch real data)
        stats = self._generate_sample_stats(repository)
        
        blocks = [
            {
                "type": "header",
                "text": {
                    "type": "plain_text",
                    "text": "📊 Kiro Statistics"
                }
            },
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": f"*Period:* Last 30 days\n*Scope:* {repository or 'All repositories'}"
                }
            },
            {
                "type": "divider"
            },
            {
                "type": "section",
                "fields": [
                    {
                        "type": "mrkdwn",
                        "text": f"*PRs Reviewed:*\n{stats['prs_reviewed']}"
                    },
                    {
                        "type": "mrkdwn",
                        "text": f"*Issues Found:*\n{stats['issues_found']}"
                    },
                    {
                        "type": "mrkdwn",
                        "text": f"*Issues Fixed:*\n{stats['issues_fixed']}"
                    },
                    {
                        "type": "mrkdwn",
                        "text": f"*Fix Rate:*\n{stats['fix_rate']}%"
                    },
                    {
                        "type": "mrkdwn",
                        "text": f"*Avg Review Time:*\n{stats['avg_review_time']}"
                    },
                    {
                        "type": "mrkdwn",
                        "text": f"*Time Saved:*\n{stats['time_saved']}"
                    }
                ]
            }
        ]
        
        # Add top issues
        if stats.get('top_issues'):
            issues_text = "*Top Issue Types:*\n"
            for issue in stats['top_issues']:
                issues_text += f"• {issue['type']}: {issue['count']} occurrences\n"
            
            blocks.append({
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": issues_text
                }
            })
        
        await client.chat_postMessage(
            channel=command['channel_id'],
            text="Kiro statistics",
            blocks=blocks
        )
    
    def _generate_sample_stats(self, repository: Optional[str]) -> Dict[str, Any]:
        """Generate sample statistics (would be replaced with real data)."""
        return {
            'prs_reviewed': 156,
            'issues_found': 423,
            'issues_fixed': 387,
            'fix_rate': 91,
            'avg_review_time': '3.5 minutes',
            'time_saved': '12.5 hours',
            'top_issues': [
                {'type': 'Unused imports', 'count': 89},
                {'type': 'Missing error handling', 'count': 67},
                {'type': 'Code formatting', 'count': 54}
            ]
        }
    
    async def handle_approve_suggestion(self, ack, body, client):
        """Handle approval of a code suggestion."""
        await ack()
        
        suggestion = json.loads(body['actions'][0]['value'])
        user = body['user']['username']
        
        # Update the message to show it was approved
        blocks = body['message']['blocks']
        blocks[-1] = {
            "type": "section",
            "text": {
                "type": "mrkdwn",
                "text": f"✅ *Suggestion approved by {user}*\nFix has been applied to the PR."
            }
        }
        
        await client.chat_update(
            channel=body['channel']['id'],
            ts=body['message']['ts'],
            blocks=blocks
        )
        
        # Here you would trigger the actual fix application
        logger.info(f"Suggestion approved: {suggestion['id']} by {user}")
    
    async def handle_reject_suggestion(self, ack, body, client):
        """Handle rejection of a code suggestion."""
        await ack()
        
        suggestion = json.loads(body['actions'][0]['value'])
        user = body['user']['username']
        
        # Update the message to show it was rejected
        blocks = body['message']['blocks']
        blocks[-1] = {
            "type": "section",
            "text": {
                "type": "mrkdwn",
                "text": f"❌ *Suggestion dismissed by {user}*"
            }
        }
        
        await client.chat_update(
            channel=body['channel']['id'],
            ts=body['message']['ts'],
            blocks=blocks
        )
        
        logger.info(f"Suggestion rejected: {suggestion['id']} by {user}")
    
    async def handle_view_details(self, ack, body, client):
        """Handle view details action."""
        await ack()
        
        data = json.loads(body['actions'][0]['value'])
        
        # Open a modal with detailed information
        await client.views_open(
            trigger_id=body['trigger_id'],
            view={
                "type": "modal",
                "title": {
                    "type": "plain_text",
                    "text": "Review Details"
                },
                "blocks": [
                    {
                        "type": "section",
                        "text": {
                            "type": "mrkdwn",
                            "text": f"*Repository:* {data['repository']}\n*Pull Request:* #{data['pull_request']}"
                        }
                    },
                    {
                        "type": "section",
                        "text": {
                            "type": "mrkdwn",
                            "text": "Detailed review information would be displayed here..."
                        }
                    }
                ]
            }
        )
    
    async def handle_configure_notifications(self, ack, body, client):
        """Handle configuration request."""
        await ack()
        
        repository = body['actions'][0]['value']
        
        # Open configuration modal
        await client.views_open(
            trigger_id=body['trigger_id'],
            view={
                "type": "modal",
                "callback_id": "configure_modal",
                "title": {
                    "type": "plain_text",
                    "text": "Configure Kiro"
                },
                "submit": {
                    "type": "plain_text",
                    "text": "Save"
                },
                "blocks": [
                    {
                        "type": "section",
                        "text": {
                            "type": "mrkdwn",
                            "text": f"Configure notifications for *{repository}*"
                        }
                    },
                    {
                        "type": "input",
                        "block_id": "notification_level",
                        "label": {
                            "type": "plain_text",
                            "text": "Notification Level"
                        },
                        "element": {
                            "type": "static_select",
                            "action_id": "level_select",
                            "placeholder": {
                                "type": "plain_text",
                                "text": "Select level"
                            },
                            "options": [
                                {
                                    "text": {"type": "plain_text", "text": "All Issues"},
                                    "value": "all"
                                },
                                {
                                    "text": {"type": "plain_text", "text": "Critical Only"},
                                    "value": "critical"
                                },
                                {
                                    "text": {"type": "plain_text", "text": "Summary Only"},
                                    "value": "summary"
                                }
                            ]
                        }
                    },
                    {
                        "type": "input",
                        "block_id": "auto_fix",
                        "label": {
                            "type": "plain_text",
                            "text": "Auto-apply Safe Fixes"
                        },
                        "element": {
                            "type": "checkboxes",
                            "action_id": "auto_fix_check",
                            "options": [
                                {
                                    "text": {"type": "plain_text", "text": "Enable auto-fix"},
                                    "value": "enabled"
                                }
                            ]
                        }
                    }
                ],
                "private_metadata": repository
            }
        )
    
    async def handle_configuration_submission(self, ack, body, view, client):
        """Handle configuration form submission."""
        await ack()
        
        repository = view['private_metadata']
        values = view['state']['values']
        
        notification_level = values['notification_level']['level_select']['selected_option']['value']
        auto_fix = len(values['auto_fix']['auto_fix_check']['selected_options']) > 0
        
        # Save configuration (this would persist to database)
        logger.info(f"Configuration saved for {repository}: level={notification_level}, auto_fix={auto_fix}")
        
        # Send confirmation
        await client.chat_postMessage(
            channel=body['user']['id'],
            text=f"✅ Configuration saved for {repository}"
        )
    
    async def handle_app_mention(self, event, client):
        """Handle @Kiro mentions."""
        text = event['text']
        channel = event['channel']
        user = event['user']
        
        # Extract PR URLs from the message
        pr_urls = self._extract_pr_urls(text)
        
        if pr_urls:
            for url in pr_urls:
                await client.chat_postMessage(
                    channel=channel,
                    text=f"<@{user}> I'll review that PR for you!",
                    thread_ts=event.get('thread_ts', event['ts'])
                )
                # Trigger review for the PR
        else:
            await client.chat_postMessage(
                channel=channel,
                text=f"<@{user}> I'm here to help! Mention me with a PR link to trigger a review, or use `/kiro help` for more options.",
                thread_ts=event.get('thread_ts', event['ts'])
            )
    
    def _extract_pr_urls(self, text: str) -> List[str]:
        """Extract GitHub PR URLs from text."""
        import re
        pattern = r'https://github\.com/[\w-]+/[\w-]+/pull/\d+'
        return re.findall(pattern, text)
    
    async def start(self):
        """Start the Slack app."""
        handler = AsyncSocketModeHandler(self.app, self.app_token)
        await handler.start_async()

# Example usage
if __name__ == "__main__":
    config = {
        'bot_token': os.getenv('SLACK_BOT_TOKEN'),
        'app_token': os.getenv('SLACK_APP_TOKEN'),
        'signing_secret': os.getenv('SLACK_SIGNING_SECRET'),
        'default_channel': '#code-reviews'
    }
    
    integration = SlackIntegration(config)
    
    # Run the Slack app
    asyncio.run(integration.start())