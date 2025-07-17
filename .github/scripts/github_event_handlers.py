#!/usr/bin/env python3
"""
GitHub event handlers for Kiro integration.
This module contains handlers for different GitHub webhook events.
"""

import os
import re
import json
import logging
import requests
from github_auth import GitHubCredentialManager

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler('kiro_github_events.log')
    ]
)
logger = logging.getLogger('kiro_github_events')

# Initialize credential manager
credential_manager = GitHubCredentialManager()

# Constants
GITHUB_API_URL = "https://api.github.com"
KIRO_API_URL = os.environ.get('KIRO_API_URL', 'https://api.kiro.ai')
KIRO_BOT_USERNAME = os.environ.get('KIRO_BOT_USERNAME', 'kiro-ai')

def get_github_client(installation_id):
    """Get a GitHub API client for a specific installation."""
    # Get installation token
    token = credential_manager.get_installation_token(installation_id)
    if not token:
        # Token not found or expired, generate a new one
        credentials = credential_manager.get_credentials()
        if not credentials:
            logger.error("No GitHub App credentials found")
            return None
        
        # Generate JWT
        import jwt
        import time
        
        now = int(time.time())
        payload = {
            'iat': now,
            'exp': now + (10 * 60),
            'iss': credentials['app_id']
        }
        
        try:
            jwt_token = jwt.encode(payload, credentials['private_key'], algorithm='RS256')
        except Exception as e:
            logger.error(f"Error generating JWT: {str(e)}")
            return None
        
        # Get installation token
        url = f"{GITHUB_API_URL}/app/installations/{installation_id}/access_tokens"
        headers = {
            'Authorization': f'Bearer {jwt_token}',
            'Accept': 'application/vnd.github.v3+json'
        }
        
        try:
            response = requests.post(url, headers=headers)
            response.raise_for_status()
            result = response.json()
            token = result.get('token')
            expires_at = result.get('expires_at')
            
            # Store token
            credential_manager.store_installation_token(installation_id, token, expires_at)
        except Exception as e:
            logger.error(f"Error getting installation token: {str(e)}")
            return None
    
    # Create client
    class GitHubClient:
        def __init__(self, token):
            self.token = token
            self.headers = {
                'Authorization': f'token {token}',
                'Accept': 'application/vnd.github.v3+json'
            }
            self.api_base = GITHUB_API_URL
        
        def get(self, url, params=None):
            response = requests.get(url, headers=self.headers, params=params)
            response.raise_for_status()
            return response.json()
        
        def post(self, url, data=None, json=None):
            response = requests.post(url, headers=self.headers, data=data, json=json)
            response.raise_for_status()
            return response.json()
        
        def patch(self, url, data=None, json=None):
            response = requests.patch(url, headers=self.headers, data=data, json=json)
            response.raise_for_status()
            return response.json()
    
    return GitHubClient(token)

def handle_pull_request_event(payload):
    """Handle pull request events."""
    action = payload.get('action')
    pr = payload.get('pull_request', {})
    repository = payload.get('repository', {})
    installation_id = payload.get('installation', {}).get('id')
    
    logger.info(f"Processing pull request #{pr.get('number')} - {action}")
    
    # Check if this event should trigger a review
    should_review = False
    
    if action == 'assigned':
        # Check if Kiro is assigned
        assignee = payload.get('assignee', {})
        if assignee.get('login') == KIRO_BOT_USERNAME:
            logger.info(f"PR #{pr.get('number')} assigned to {KIRO_BOT_USERNAME}")
            should_review = True
    
    elif action == 'review_requested':
        # Check if Kiro is requested as reviewer
        requested_reviewer = payload.get('requested_reviewer', {})
        if requested_reviewer.get('login') == KIRO_BOT_USERNAME:
            logger.info(f"Review requested from {KIRO_BOT_USERNAME} for PR #{pr.get('number')}")
            should_review = True
    
    elif action == 'synchronize':
        # Check if Kiro is already assigned or requested as reviewer
        assignees = pr.get('assignees', [])
        requested_reviewers = pr.get('requested_reviewers', [])
        
        is_assigned = any(assignee.get('login') == KIRO_BOT_USERNAME for assignee in assignees)
        is_reviewer = any(reviewer.get('login') == KIRO_BOT_USERNAME for reviewer in requested_reviewers)
        
        if is_assigned or is_reviewer:
            logger.info(f"New commits pushed to PR #{pr.get('number')} that {KIRO_BOT_USERNAME} is reviewing")
            should_review = True
    
    # Process the PR if needed
    if should_review:
        process_pr_for_review(pr, repository, installation_id, is_update=(action == 'synchronize'))

def process_pr_for_review(pr, repository, installation_id, is_update=False):
    """Process a pull request for review by Kiro."""
    # Get GitHub client
    github = get_github_client(installation_id)
    if not github:
        logger.error("Failed to get GitHub client")
        return
    
    # Get PR details
    pr_number = pr.get('number')
    repo_full_name = repository.get('full_name')
    repo_owner, repo_name = repo_full_name.split('/')
    
    logger.info(f"{'Updating' if is_update else 'Starting'} review for PR #{pr_number} in {repo_full_name}")
    
    try:
        # Get repository configuration
        config = get_repository_config(github, repo_owner, repo_name)
        
        # Add a comment to the PR
        if not is_update:
            comment_url = f"{GITHUB_API_URL}/repos/{repo_full_name}/issues/{pr_number}/comments"
            comment_data = {
                'body': f"👋 Hi there! I'm {KIRO_BOT_USERNAME} and I'll be reviewing this PR. I'll add comments with suggestions shortly."
            }
            github.post(comment_url, json=comment_data)
            logger.info(f"Added initial comment to PR #{pr_number}")
        
        # Import PR processor here to avoid circular imports
        from pr_processor import queue_pr_for_processing
        
        # Set priority based on update status (updates have higher priority)
        priority = 0 if is_update else 1
        
        # Queue the PR for processing
        queue_pr_for_processing(github, repo_full_name, pr_number, priority, config)
        logger.info(f"Queued PR #{pr_number} for processing with priority {priority}")
    
    except Exception as e:
        logger.error(f"Error processing PR for review: {str(e)}")

def handle_issue_comment_event(payload):
    """Handle issue comment events."""
    action = payload.get('action')
    if action != 'created':
        return
    
    comment = payload.get('comment', {})
    issue = payload.get('issue', {})
    repository = payload.get('repository', {})
    installation_id = payload.get('installation', {}).get('id')
    
    # Check if this is a PR comment (issues with pull_request property are actually PRs)
    is_pr = 'pull_request' in issue
    
    if not is_pr:
        return
    
    logger.info(f"Processing comment on PR #{issue.get('number')}")
    
    # Check for apply suggestion command
    comment_body = comment.get('body', '')
    if '/apply-suggestion' in comment_body.lower():
        process_suggestion_command(comment, issue, repository, installation_id)
    # Check if comment mentions Kiro
    elif mentions_kiro(comment_body):
        process_pr_comment(comment, issue, repository, installation_id)

def process_suggestion_command(comment, issue, repository, installation_id):
    """Process a comment with an apply suggestion command."""
    # Get GitHub client
    github = get_github_client(installation_id)
    if not github:
        logger.error("Failed to get GitHub client")
        return
    
    # Get PR details
    pr_number = issue.get('number')
    repo_full_name = repository.get('full_name')
    comment_id = comment.get('id')
    comment_body = comment.get('body', '')
    
    logger.info(f"Processing suggestion command in comment #{comment_id} on PR #{pr_number}")
    
    try:
        # Import fix suggester
        from fix_suggester import handle_suggestion_comment
        
        # Get repository configuration
        repo_owner, repo_name = repo_full_name.split('/')
        config = get_repository_config(github, repo_owner, repo_name)
        
        # Handle the suggestion command
        success = handle_suggestion_comment(
            github,
            repo_full_name,
            pr_number,
            comment_id,
            comment_body,
            config
        )
        
        # Add a reply to the comment
        reply_url = f"{GITHUB_API_URL}/repos/{repo_full_name}/issues/{pr_number}/comments"
        
        if success:
            reply_body = f"✅ Successfully applied the suggestion! The changes have been committed to the PR branch."
        else:
            reply_body = f"❌ Failed to apply the suggestion. Please check the syntax or try applying it manually."
        
        reply_data = {
            'body': reply_body,
            'in_reply_to': comment_id
        }
        
        github.post(reply_url, json=reply_data)
        logger.info(f"Added reply to suggestion command in comment #{comment_id}")
    
    except Exception as e:
        logger.error(f"Error processing suggestion command: {str(e)}")
        
        # Add error reply
        try:
            reply_url = f"{GITHUB_API_URL}/repos/{repo_full_name}/issues/{pr_number}/comments"
            reply_data = {
                'body': f"❌ Error applying suggestion: {str(e)}",
                'in_reply_to': comment_id
            }
            github.post(reply_url, json=reply_data)
        except:
            logger.error("Failed to add error reply")

def mentions_kiro(text):
    """Check if text mentions Kiro."""
    # Check for @kiro-ai mention
    if f"@{KIRO_BOT_USERNAME}" in text:
        return True
    
    # Check for commands
    kiro_commands = [
        r'/kiro\s+review',
        r'/kiro\s+help',
        r'/kiro\s+explain',
        r'/kiro\s+fix',
        r'/kiro\s+ignore'
    ]
    
    for command in kiro_commands:
        if re.search(command, text, re.IGNORECASE):
            return True
    
    return False

def process_pr_comment(comment, issue, repository, installation_id):
    """Process a comment that mentions Kiro on a PR."""
    # Get GitHub client
    github = get_github_client(installation_id)
    if not github:
        logger.error("Failed to get GitHub client")
        return
    
    # Get PR details
    pr_number = issue.get('number')
    repo_full_name = repository.get('full_name')
    comment_body = comment.get('body', '')
    
    logger.info(f"Processing mention in comment on PR #{pr_number} in {repo_full_name}")
    
    # Parse commands
    if '/kiro review' in comment_body.lower():
        # Get PR details
        pr_url = f"{GITHUB_API_URL}/repos/{repo_full_name}/pulls/{pr_number}"
        pr = github.get(pr_url)
        
        # Process PR for review
        process_pr_for_review(pr, repository, installation_id)
    
    elif '/kiro help' in comment_body.lower():
        # Reply with help information
        comment_url = f"{GITHUB_API_URL}/repos/{repo_full_name}/issues/{pr_number}/comments"
        help_text = f"""
## {KIRO_BOT_USERNAME} Commands

- `/kiro review` - Request a code review for this PR
- `/kiro explain [file:line]` - Ask for an explanation of specific code
- `/kiro fix [file:line]` - Request an automated fix for an issue
- `/kiro ignore [file:line]` - Ignore a specific issue
- `/apply-suggestion [id]` - Apply a suggested fix (can be used on any suggestion)
- `/kiro help` - Show this help message

For more information, see the [documentation](https://docs.example.com/kiro).
"""
        comment_data = {
            'body': help_text
        }
        github.post(comment_url, json=comment_data)
        logger.info(f"Added help comment to PR #{pr_number}")
    
    elif '/kiro explain' in comment_body.lower():
        # Extract file and line reference
        match = re.search(r'/kiro\s+explain\s+([^\s:]+):(\d+)', comment_body, re.IGNORECASE)
        if match:
            file_path = match.group(1)
            line_number = match.group(2)
            
            # In a real implementation, this would call the Kiro API
            # For now, we'll just add a placeholder comment
            comment_url = f"{GITHUB_API_URL}/repos/{repo_full_name}/issues/{pr_number}/comments"
            explanation = f"""
## Explanation for `{file_path}:{line_number}`

This code appears to be [explanation would go here].

The purpose of this section is to [purpose would go here].
"""
            comment_data = {
                'body': explanation
            }
            github.post(comment_url, json=comment_data)
            logger.info(f"Added explanation comment for {file_path}:{line_number} to PR #{pr_number}")
    
    elif '/kiro fix' in comment_body.lower():
        # Extract file and line reference
        match = re.search(r'/kiro\s+fix\s+([^\s:]+):(\d+)', comment_body, re.IGNORECASE)
        if match:
            file_path = match.group(1)
            line_number = int(match.group(2))
            
            # Get PR details
            pr_url = f"{GITHUB_API_URL}/repos/{repo_full_name}/pulls/{pr_number}"
            pr = github.get(pr_url)
            
            # Get file content
            contents_url = f"{GITHUB_API_URL}/repos/{repo_full_name}/contents/{file_path}"
            response = github.get(contents_url, params={'ref': pr.get('head', {}).get('ref')})
            
            if 'content' in response:
                import base64
                content = base64.b64decode(response['content']).decode('utf-8')
                lines = content.splitlines()
                
                if 0 <= line_number - 1 < len(lines):
                    # Generate a fix suggestion
                    comment_url = f"{GITHUB_API_URL}/repos/{repo_full_name}/issues/{pr_number}/comments"
                    suggestion = f"""
## Fix Suggestion for `{file_path}:{line_number}`

I've analyzed this code and here's a suggested fix:

```suggestion
{lines[line_number - 1]}
```

You can apply this suggestion by clicking the "Add suggestion to batch" button above.
"""
                    comment_data = {
                        'body': suggestion
                    }
                    github.post(comment_url, json=comment_data)
                    logger.info(f"Added fix suggestion for {file_path}:{line_number} to PR #{pr_number}")

def handle_pull_request_review_event(payload):
    """Handle pull request review events."""
    action = payload.get('action')
    review = payload.get('review', {})
    pr = payload.get('pull_request', {})
    repository = payload.get('repository', {})
    installation_id = payload.get('installation', {}).get('id')
    
    logger.info(f"Processing review {action} on PR #{pr.get('number')}")
    
    # Check if this is a review of Kiro's comments
    review_body = review.get('body', '')
    if mentions_kiro(review_body):
        process_review_feedback(review, pr, repository, installation_id)

def process_review_feedback(review, pr, repository, installation_id):
    """Process feedback on Kiro's review."""
    # Get GitHub client
    github = get_github_client(installation_id)
    if not github:
        logger.error("Failed to get GitHub client")
        return
    
    # Get PR details
    pr_number = pr.get('number')
    repo_full_name = repository.get('full_name')
    repo_owner, repo_name = repo_full_name.split('/')
    review_body = review.get('body', '')
    reviewer = review.get('user', {}).get('login', '')
    
    logger.info(f"Processing feedback in review on PR #{pr_number} in {repo_full_name}")
    
    try:
        # Import analytics and learning systems
        from analytics_collector import record_feedback
        from learning_system import update_developer_preferences, update_rule_effectiveness
        
        # Analyze feedback sentiment
        feedback_type, score = analyze_feedback_sentiment(review_body)
        
        # Record feedback in analytics
        # Note: In a real implementation, we would need to track review_id
        # For now, we'll create a mock review_id based on PR
        review_id = f"pr-{pr_number}-feedback"
        record_feedback(review_id, feedback_type, score, review_body)
        
        # Update learning system based on feedback
        if feedback_type in ['positive', 'negative']:
            # Extract mentioned rules from the review body
            mentioned_rules = extract_mentioned_rules(review_body)
            
            for rule_id, category in mentioned_rules:
                # Update rule effectiveness
                update_rule_effectiveness(rule_id, category, feedback_type)
                
                # Update developer preferences
                preference_score = 0.8 if feedback_type == 'positive' else 0.2
                update_developer_preferences(reviewer, repo_owner, repo_name, rule_id, preference_score)
        
        logger.info(f"Processed {feedback_type} feedback from {reviewer}")
    
    except Exception as e:
        logger.error(f"Error processing feedback: {str(e)}")

def analyze_feedback_sentiment(text: str) -> Tuple[str, int]:
    """Analyze the sentiment of feedback text."""
    text_lower = text.lower()
    
    # Positive indicators
    positive_words = ['good', 'great', 'helpful', 'useful', 'correct', 'right', 'thanks', 'thank you', 'excellent', 'perfect']
    negative_words = ['bad', 'wrong', 'incorrect', 'unhelpful', 'useless', 'disagree', 'no', 'not helpful', 'false']
    
    positive_count = sum(1 for word in positive_words if word in text_lower)
    negative_count = sum(1 for word in negative_words if word in text_lower)
    
    if positive_count > negative_count:
        return 'positive', min(5, 3 + positive_count)
    elif negative_count > positive_count:
        return 'negative', max(1, 3 - negative_count)
    else:
        return 'neutral', 3

def extract_mentioned_rules(text: str) -> List[Tuple[str, str]]:
    """Extract mentioned rules from feedback text."""
    # Look for rule patterns in the text
    rule_patterns = [
        (r'security[_-](\w+)', 'security'),
        (r'style[_-](\w+)', 'style'),
        (r'performance[_-](\w+)', 'performance'),
        (r'(\w+)[_-]security', 'security'),
        (r'(\w+)[_-]style', 'style'),
        (r'(\w+)[_-]performance', 'performance'),
    ]
    
    mentioned_rules = []
    text_lower = text.lower()
    
    for pattern, category in rule_patterns:
        matches = re.findall(pattern, text_lower)
        for match in matches:
            rule_id = f"{category}-{match}" if isinstance(match, str) else f"{category}-rule"
            mentioned_rules.append((rule_id, category))
    
    return mentioned_rules

def handle_push_event(payload):
    """Handle push events."""
    ref = payload.get('ref')
    repository = payload.get('repository', {})
    installation_id = payload.get('installation', {}).get('id')
    
    # Only process pushes to main branches
    if ref not in ['refs/heads/main', 'refs/heads/master', 'refs/heads/develop']:
        return
    
    logger.info(f"Processing push to {ref} in {repository.get('full_name')}")
    
    # In a real implementation, this might trigger scans or other actions
    # For now, we'll just log it
    logger.info(f"Push event received for {ref}")

def get_repository_config(github, repo_owner, repo_name):
    """Get Kiro configuration for a repository."""
    try:
        # Import config manager
        from config_manager import get_repository_config as get_config
        
        # Get configuration
        config = get_config(github, repo_owner, repo_name)
        
        return config
    except Exception as e:
        logger.warning(f"Error getting repository config: {str(e)}")
        # Return default configuration
        return {
            'review': {
                'depth': 'standard',
                'focus_areas': ['security', 'performance', 'style', 'documentation'],
                'auto_fix': True,
                'comment_style': 'educational'
            }
        }