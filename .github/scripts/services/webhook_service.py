"""
Refactored webhook service with proper separation of concerns.

This service handles GitHub webhook processing with clean architecture,
dependency injection, and proper error handling.
"""

import hashlib
import hmac
import json
import logging
from dataclasses import dataclass
from datetime import datetime
from typing import Any, Dict, Optional

from ..core.interfaces import (
    CacheInterface,
    GitHubClientInterface,
    MetricsInterface,
    QueueInterface
)
from ..core.exceptions import (
    WebhookValidationError,
    WebhookProcessingError,
    ConfigurationError
)


logger = logging.getLogger(__name__)


@dataclass
class WebhookEvent:
    """Represents a GitHub webhook event."""
    event_type: str
    action: Optional[str]
    delivery_id: str
    signature: str
    payload: Dict[str, Any]
    received_at: datetime


class WebhookValidator:
    """Handles webhook signature validation."""
    
    def __init__(self, secret: str):
        if not secret:
            raise ConfigurationError("Webhook secret is required for security")
        self.secret = secret.encode('utf-8')
    
    def validate_signature(self, payload: bytes, signature: str) -> bool:
        """
        Validate webhook signature using HMAC-SHA256.
        
        Args:
            payload: Raw request body
            signature: Signature from X-Hub-Signature-256 header
            
        Returns:
            True if signature is valid
            
        Raises:
            WebhookValidationError: If signature format is invalid
        """
        if not signature.startswith('sha256='):
            raise WebhookValidationError("Invalid signature format")
        
        expected_signature = 'sha256=' + hmac.new(
            self.secret,
            payload,
            hashlib.sha256
        ).hexdigest()
        
        return hmac.compare_digest(signature, expected_signature)


class WebhookService:
    """
    Service for processing GitHub webhooks.
    
    This service follows single responsibility principle by focusing only
    on webhook reception and validation. Processing is delegated to
    appropriate handlers via the queue.
    """
    
    def __init__(
        self,
        validator: WebhookValidator,
        queue: QueueInterface,
        cache: CacheInterface,
        metrics: MetricsInterface,
        github_client: GitHubClientInterface
    ):
        self.validator = validator
        self.queue = queue
        self.cache = cache
        self.metrics = metrics
        self.github_client = github_client
        
        # Event handlers mapping
        self.event_handlers = {
            'pull_request': self._handle_pull_request,
            'issue_comment': self._handle_issue_comment,
            'pull_request_review': self._handle_pull_request_review,
            'push': self._handle_push,
            'release': self._handle_release
        }
    
    async def process_webhook(
        self,
        headers: Dict[str, str],
        body: bytes
    ) -> Dict[str, Any]:
        """
        Process incoming webhook.
        
        Args:
            headers: Request headers
            body: Raw request body
            
        Returns:
            Processing result
            
        Raises:
            WebhookValidationError: If validation fails
            WebhookProcessingError: If processing fails
        """
        # Extract required headers
        event_type = headers.get('X-GitHub-Event')
        delivery_id = headers.get('X-GitHub-Delivery')
        signature = headers.get('X-Hub-Signature-256')
        
        if not all([event_type, delivery_id, signature]):
            raise WebhookValidationError("Missing required headers")
        
        # Validate signature
        if not self.validator.validate_signature(body, signature):
            self.metrics.increment('webhook.validation.failed')
            raise WebhookValidationError("Invalid signature")
        
        # Check for duplicate delivery
        if await self._is_duplicate(delivery_id):
            logger.info(f"Duplicate webhook delivery: {delivery_id}")
            return {"status": "duplicate", "delivery_id": delivery_id}
        
        # Parse payload
        try:
            payload = json.loads(body.decode('utf-8'))
        except json.JSONDecodeError as e:
            raise WebhookValidationError(f"Invalid JSON payload: {e}")
        
        # Create event object
        event = WebhookEvent(
            event_type=event_type,
            action=payload.get('action'),
            delivery_id=delivery_id,
            signature=signature,
            payload=payload,
            received_at=datetime.utcnow()
        )
        
        # Record metrics
        self.metrics.increment(
            'webhook.received',
            tags={
                'event': event_type,
                'action': event.action or 'none'
            }
        )
        
        # Process event
        return await self._process_event(event)
    
    async def _is_duplicate(self, delivery_id: str) -> bool:
        """Check if webhook has already been processed."""
        cache_key = f"webhook:delivery:{delivery_id}"
        
        if await self.cache.exists(cache_key):
            return True
        
        # Mark as processed (with 1 hour TTL)
        await self.cache.set(cache_key, True, ttl_seconds=3600)
        return False
    
    async def _process_event(self, event: WebhookEvent) -> Dict[str, Any]:
        """Process webhook event based on type."""
        handler = self.event_handlers.get(event.event_type)
        
        if not handler:
            logger.info(f"No handler for event type: {event.event_type}")
            return {
                "status": "ignored",
                "reason": "unsupported_event",
                "event_type": event.event_type
            }
        
        try:
            result = await handler(event)
            
            self.metrics.increment(
                'webhook.processed',
                tags={'event': event.event_type, 'status': 'success'}
            )
            
            return {
                "status": "processed",
                "event_type": event.event_type,
                "result": result
            }
            
        except Exception as e:
            logger.error(f"Error processing webhook: {e}", exc_info=True)
            
            self.metrics.increment(
                'webhook.processed',
                tags={'event': event.event_type, 'status': 'error'}
            )
            
            raise WebhookProcessingError(f"Failed to process {event.event_type}: {e}")
    
    async def _handle_pull_request(self, event: WebhookEvent) -> Dict[str, Any]:
        """Handle pull request events."""
        payload = event.payload
        action = event.action
        
        # Extract PR details
        pr = payload['pull_request']
        repo = payload['repository']
        
        pr_data = {
            'pr_number': pr['number'],
            'repo_owner': repo['owner']['login'],
            'repo_name': repo['name'],
            'action': action,
            'pr_data': pr,
            'sender': payload['sender']['login']
        }
        
        # Determine if we should process this PR
        if not await self._should_process_pr(pr_data):
            return {"processed": False, "reason": "not_assigned_to_kiro"}
        
        # Queue for processing
        message_id = await self.queue.publish(
            'pr_processing',
            pr_data,
            priority=self._get_priority(pr_data)
        )
        
        return {
            "processed": True,
            "message_id": message_id,
            "pr_number": pr['number']
        }
    
    async def _handle_issue_comment(self, event: WebhookEvent) -> Dict[str, Any]:
        """Handle issue comment events."""
        payload = event.payload
        
        # Check if it's a PR comment
        if 'pull_request' not in payload['issue']:
            return {"processed": False, "reason": "not_pr_comment"}
        
        comment = payload['comment']
        
        # Check for mentions or commands
        if '@kiro' in comment['body'].lower():
            comment_data = {
                'comment_id': comment['id'],
                'pr_number': payload['issue']['number'],
                'repo_owner': payload['repository']['owner']['login'],
                'repo_name': payload['repository']['name'],
                'comment_body': comment['body'],
                'author': comment['user']['login']
            }
            
            message_id = await self.queue.publish(
                'comment_processing',
                comment_data
            )
            
            return {
                "processed": True,
                "message_id": message_id,
                "comment_id": comment['id']
            }
        
        return {"processed": False, "reason": "no_kiro_mention"}
    
    async def _handle_pull_request_review(self, event: WebhookEvent) -> Dict[str, Any]:
        """Handle pull request review events."""
        # Implementation for review events
        return {"processed": True, "action": "review_recorded"}
    
    async def _handle_push(self, event: WebhookEvent) -> Dict[str, Any]:
        """Handle push events."""
        # Implementation for push events
        return {"processed": False, "reason": "push_events_not_supported"}
    
    async def _handle_release(self, event: WebhookEvent) -> Dict[str, Any]:
        """Handle release events."""
        # Implementation for release events
        return {"processed": False, "reason": "release_events_not_supported"}
    
    async def _should_process_pr(self, pr_data: Dict[str, Any]) -> bool:
        """Determine if PR should be processed by Kiro."""
        pr = pr_data['pr_data']
        
        # Check if Kiro is assigned
        assignees = pr.get('assignees', [])
        if any(assignee['login'] == 'kiro-ai' for assignee in assignees):
            return True
        
        # Check if Kiro is requested reviewer
        requested_reviewers = pr.get('requested_reviewers', [])
        if any(reviewer['login'] == 'kiro-ai' for reviewer in requested_reviewers):
            return True
        
        # Check for labels
        labels = pr.get('labels', [])
        if any(label['name'] in ['kiro-review', 'needs-kiro'] for label in labels):
            return True
        
        # Check PR title
        if '[kiro]' in pr.get('title', '').lower():
            return True
        
        return False
    
    def _get_priority(self, pr_data: Dict[str, Any]) -> int:
        """Calculate priority for PR processing."""
        priority = 0
        
        # Higher priority for smaller PRs
        pr = pr_data['pr_data']
        if pr.get('additions', 0) + pr.get('deletions', 0) < 100:
            priority += 2
        
        # Higher priority for PRs with certain labels
        labels = pr.get('labels', [])
        for label in labels:
            if label['name'] in ['urgent', 'high-priority', 'security']:
                priority += 3
                break
        
        # Higher priority for certain actions
        if pr_data['action'] in ['opened', 'synchronize']:
            priority += 1
        
        return min(priority, 5)  # Max priority is 5