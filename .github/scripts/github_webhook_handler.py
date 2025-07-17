#!/usr/bin/env python3
"""
GitHub webhook handler for Kiro integration.
This script processes GitHub webhook events and triggers appropriate actions.
"""

import os
import sys
import json
import hmac
import hashlib
import requests
from datetime import datetime
from pathlib import Path

# Configuration
WEBHOOK_SECRET = os.environ.get('GITHUB_WEBHOOK_SECRET')
GITHUB_APP_ID = os.environ.get('GITHUB_APP_ID')
GITHUB_PRIVATE_KEY_PATH = os.environ.get('GITHUB_PRIVATE_KEY_PATH')
KIRO_API_URL = os.environ.get('KIRO_API_URL', 'https://api.kiro.ai')

def verify_webhook_signature(payload_body, signature_header):
    """Verify that the webhook payload was sent from GitHub by validating the signature."""
    if not WEBHOOK_SECRET:
        print("Warning: GITHUB_WEBHOOK_SECRET is not set")
        return True  # Skip validation if secret is not set (not recommended for production)
    
    signature = "sha1=" + hmac.new(
        WEBHOOK_SECRET.encode('utf-8'),
        payload_body,
        hashlib.sha1
    ).hexdigest()
    
    return hmac.compare_digest(signature, signature_header)

def generate_jwt_token():
    """Generate a JWT token for GitHub App authentication."""
    import jwt
    import time
    
    # Read the private key
    if not GITHUB_PRIVATE_KEY_PATH:
        print("Error: GITHUB_PRIVATE_KEY_PATH is not set")
        return None
    
    try:
        with open(GITHUB_PRIVATE_KEY_PATH, 'r') as key_file:
            private_key = key_file.read()
    except Exception as e:
        print(f"Error reading private key: {str(e)}")
        return None
    
    # Generate the JWT
    now = int(time.time())
    payload = {
        'iat': now,  # Issued at time
        'exp': now + (10 * 60),  # JWT expiration time (10 minutes)
        'iss': GITHUB_APP_ID  # GitHub App's identifier
    }
    
    try:
        token = jwt.encode(payload, private_key, algorithm='RS256')
        return token
    except Exception as e:
        print(f"Error generating JWT: {str(e)}")
        return None

def get_installation_token(installation_id):
    """Get an installation access token for a specific installation."""
    jwt_token = generate_jwt_token()
    if not jwt_token:
        return None
    
    url = f"https://api.github.com/app/installations/{installation_id}/access_tokens"
    headers = {
        'Authorization': f'Bearer {jwt_token}',
        'Accept': 'application/vnd.github.v3+json'
    }
    
    try:
        response = requests.post(url, headers=headers)
        response.raise_for_status()
        return response.json().get('token')
    except Exception as e:
        print(f"Error getting installation token: {str(e)}")
        return None

def handle_pull_request_event(payload):
    """Handle pull request events."""
    action = payload.get('action')
    pr = payload.get('pull_request', {})
    repository = payload.get('repository', {})
    installation_id = payload.get('installation', {}).get('id')
    
    print(f"Processing pull request #{pr.get('number')} - {action}")
    
    # Check if Kiro is assigned as a reviewer
    assignees = pr.get('assignees', [])
    requested_reviewers = pr.get('requested_reviewers', [])
    
    is_assigned_to_kiro = any(
        assignee.get('login') == 'kiro-ai' 
        for assignee in assignees
    )
    
    is_requested_reviewer = any(
        reviewer.get('login') == 'kiro-ai' 
        for reviewer in requested_reviewers
    )
    
    if action in ['assigned', 'review_requested'] and (is_assigned_to_kiro or is_requested_reviewer):
        # Kiro was assigned or requested as reviewer
        process_pr_for_review(pr, repository, installation_id)
    elif action == 'synchronize' and (is_assigned_to_kiro or is_requested_reviewer):
        # New commits were pushed to a PR that Kiro is reviewing
        process_pr_for_review(pr, repository, installation_id, is_update=True)

def process_pr_for_review(pr, repository, installation_id, is_update=False):
    """Process a pull request for review by Kiro."""
    # Get installation token
    token = get_installation_token(installation_id)
    if not token:
        print("Failed to get installation token")
        return
    
    # Get PR details
    pr_number = pr.get('number')
    repo_full_name = repository.get('full_name')
    
    print(f"{'Updating' if is_update else 'Starting'} review for PR #{pr_number} in {repo_full_name}")
    
    # TODO: Call Kiro API to start or update the review
    # This would be implemented in a production version

def handle_issue_comment_event(payload):
    """Handle issue comment events."""
    action = payload.get('action')
    comment = payload.get('comment', {})
    issue = payload.get('issue', {})
    repository = payload.get('repository', {})
    installation_id = payload.get('installation', {}).get('id')
    
    # Check if this is a PR comment (issues with pull_request property are actually PRs)
    is_pr = 'pull_request' in issue
    
    if not is_pr:
        return
    
    print(f"Processing comment on PR #{issue.get('number')}")
    
    # Check if comment mentions Kiro
    comment_body = comment.get('body', '').lower()
    if '@kiro-ai' in comment_body or 'kiro' in comment_body:
        # Comment mentions Kiro, process it
        process_pr_comment(comment, issue, repository, installation_id)

def process_pr_comment(comment, issue, repository, installation_id):
    """Process a comment that mentions Kiro on a PR."""
    # Get installation token
    token = get_installation_token(installation_id)
    if not token:
        print("Failed to get installation token")
        return
    
    # Get PR details
    pr_number = issue.get('number')
    repo_full_name = repository.get('full_name')
    comment_body = comment.get('body', '')
    
    print(f"Processing mention in comment on PR #{pr_number} in {repo_full_name}")
    
    # TODO: Parse comment for commands and call Kiro API
    # This would be implemented in a production version

def main(event_name, payload_path):
    """Main entry point for the webhook handler."""
    try:
        with open(payload_path, 'r') as f:
            payload = json.load(f)
        
        print(f"Processing {event_name} event")
        
        if event_name == 'pull_request':
            handle_pull_request_event(payload)
        elif event_name == 'issue_comment':
            handle_issue_comment_event(payload)
        else:
            print(f"Event {event_name} is not supported yet")
        
    except Exception as e:
        print(f"Error processing webhook: {str(e)}")
        sys.exit(1)

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: github_webhook_handler.py <event_name> <payload_path>")
        sys.exit(1)
    
    event_name = sys.argv[1]
    payload_path = sys.argv[2]
    main(event_name, payload_path)