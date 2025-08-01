#!/usr/bin/env python3
"""
GitHub webhook handler for Kiro integration.
This script processes GitHub webhook events and triggers appropriate actions.
"""

import hashlib
import hmac
import json
import os
import sys

import requests

# Configuration
WEBHOOK_SECRET = os.environ.get("GITHUB_WEBHOOK_SECRET")
GITHUB_APP_ID = os.environ.get("GITHUB_APP_ID")
GITHUB_PRIVATE_KEY_PATH = os.environ.get("GITHUB_PRIVATE_KEY_PATH")
KIRO_API_URL = os.environ.get("KIRO_API_URL", "https://api.kiro.ai")


def verify_webhook_signature(payload_body, signature_header):
    """Verify that the webhook payload was sent from GitHub by validating the signature."""
    if not WEBHOOK_SECRET:
        return True  # Skip validation if secret is not set (not recommended for production)

    signature = "sha1=" + hmac.new(WEBHOOK_SECRET.encode("utf-8"), payload_body, hashlib.sha1).hexdigest()

    return hmac.compare_digest(signature, signature_header)


def generate_jwt_token():
    """Generate a JWT token for GitHub App authentication."""
    import time

    import jwt

    # Read the private key
    if not GITHUB_PRIVATE_KEY_PATH:
        return None

    try:
        with open(GITHUB_PRIVATE_KEY_PATH) as key_file:
            private_key = key_file.read()
    except Exception:
        return None

    # Generate the JWT
    now = int(time.time())
    payload = {
        "iat": now,  # Issued at time
        "exp": now + (10 * 60),  # JWT expiration time (10 minutes)
        "iss": GITHUB_APP_ID,  # GitHub App's identifier
    }

    try:
        token = jwt.encode(payload, private_key, algorithm="RS256")
        return token
    except Exception:
        return None


def get_installation_token(installation_id):
    """Get an installation access token for a specific installation."""
    jwt_token = generate_jwt_token()
    if not jwt_token:
        return None

    url = f"https://api.github.com/app/installations/{installation_id}/access_tokens"
    headers = {"Authorization": f"Bearer {jwt_token}", "Accept": "application/vnd.github.v3+json"}

    try:
        response = requests.post(url, headers=headers, timeout=30)
        response.raise_for_status()
        return response.json().get("token")
    except Exception:
        return None


def handle_pull_request_event(payload):
    """Handle pull request events."""
    action = payload.get("action")
    pr = payload.get("pull_request", {})
    repository = payload.get("repository", {})
    installation_id = payload.get("installation", {}).get("id")

    # Check if Kiro is assigned as a reviewer
    assignees = pr.get("assignees", [])
    requested_reviewers = pr.get("requested_reviewers", [])

    is_assigned_to_kiro = any(assignee.get("login") == "kiro-ai" for assignee in assignees)

    is_requested_reviewer = any(reviewer.get("login") == "kiro-ai" for reviewer in requested_reviewers)

    if action in ["assigned", "review_requested"] and (is_assigned_to_kiro or is_requested_reviewer):
        # Kiro was assigned or requested as reviewer
        process_pr_for_review(pr, repository, installation_id)
    elif action == "synchronize" and (is_assigned_to_kiro or is_requested_reviewer):
        # New commits were pushed to a PR that Kiro is reviewing
        process_pr_for_review(pr, repository, installation_id, is_update=True)


def process_pr_for_review(pr, repository, installation_id, is_update=False):
    """Process a pull request for review by Kiro."""
    # Get installation token
    token = get_installation_token(installation_id)
    if not token:
        return

    # Get PR details
    pr.get("number")
    repository.get("full_name")

    # TODO: Call Kiro API to start or update the review
    # This would be implemented in a production version


def handle_issue_comment_event(payload):
    """Handle issue comment events."""
    payload.get("action")
    comment = payload.get("comment", {})
    issue = payload.get("issue", {})
    repository = payload.get("repository", {})
    installation_id = payload.get("installation", {}).get("id")

    # Check if this is a PR comment (issues with pull_request property are actually PRs)
    is_pr = "pull_request" in issue

    if not is_pr:
        return

    # Check if comment mentions Kiro
    comment_body = comment.get("body", "").lower()
    if "@kiro-ai" in comment_body or "kiro" in comment_body:
        # Comment mentions Kiro, process it
        process_pr_comment(comment, issue, repository, installation_id)


def process_pr_comment(comment, issue, repository, installation_id):
    """Process a comment that mentions Kiro on a PR."""
    # Get installation token
    token = get_installation_token(installation_id)
    if not token:
        return

    # Get PR details
    issue.get("number")
    repository.get("full_name")
    comment.get("body", "")

    # TODO: Parse comment for commands and call Kiro API
    # This would be implemented in a production version


def main(event_name, payload_path):
    """Main entry point for the webhook handler."""
    try:
        with open(payload_path) as f:
            payload = json.load(f)

        if event_name == "pull_request":
            handle_pull_request_event(payload)
        elif event_name == "issue_comment":
            handle_issue_comment_event(payload)
        else:
            pass

    except Exception:
        sys.exit(1)


if __name__ == "__main__":
    if len(sys.argv) < 3:
        sys.exit(1)

    event_name = sys.argv[1]
    payload_path = sys.argv[2]
    main(event_name, payload_path)
