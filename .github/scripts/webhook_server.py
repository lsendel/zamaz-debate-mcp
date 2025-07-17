#!/usr/bin/env python3
"""
GitHub webhook server for Kiro integration.
This script runs a Flask server that receives and processes GitHub webhook events.
"""

import os
import sys
import json
import hmac
import hashlib
import logging
import datetime
from flask import Flask, request, jsonify
from github_auth import GitHubCredentialManager
from github_event_handlers import (
    handle_pull_request_event,
    handle_issue_comment_event,
    handle_pull_request_review_event,
    handle_push_event
)

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler('kiro_github_webhook.log')
    ]
)
logger = logging.getLogger('kiro_github_webhook')

# Initialize Flask app
app = Flask(__name__)

# Initialize credential manager
credential_manager = GitHubCredentialManager()

def verify_webhook_signature(payload_body, signature_header):
    """Verify that the webhook payload was sent from GitHub by validating the signature."""
    credentials = credential_manager.get_credentials()
    if not credentials or 'webhook_secret' not in credentials:
        logger.warning("Webhook secret not found in credentials")
        return False
    
    webhook_secret = credentials['webhook_secret']
    
    signature = "sha1=" + hmac.new(
        webhook_secret.encode('utf-8'),
        payload_body,
        hashlib.sha1
    ).hexdigest()
    
    return hmac.compare_digest(signature, signature_header)

@app.route('/api/webhooks/github', methods=['POST'])
def github_webhook():
    """Handle GitHub webhook events."""
    # Verify signature
    signature_header = request.headers.get('X-Hub-Signature')
    if not signature_header:
        logger.warning("No signature header in request")
        return jsonify({'error': 'No signature header'}), 400
    
    payload_body = request.data
    if not verify_webhook_signature(payload_body, signature_header):
        logger.warning("Invalid signature")
        return jsonify({'error': 'Invalid signature'}), 401
    
    # Get event type
    event_type = request.headers.get('X-GitHub-Event')
    if not event_type:
        logger.warning("No event type header")
        return jsonify({'error': 'No event type header'}), 400
    
    # Parse payload
    try:
        payload = json.loads(payload_body)
    except json.JSONDecodeError:
        logger.warning("Invalid JSON payload")
        return jsonify({'error': 'Invalid JSON payload'}), 400
    
    # Process event
    logger.info(f"Received {event_type} event")
    
    try:
        if event_type == 'ping':
            return jsonify({'message': 'pong'}), 200
        elif event_type == 'pull_request':
            handle_pull_request_event(payload)
        elif event_type == 'issue_comment':
            handle_issue_comment_event(payload)
        elif event_type == 'pull_request_review':
            handle_pull_request_review_event(payload)
        elif event_type == 'push':
            handle_push_event(payload)
        else:
            logger.info(f"Event type {event_type} not handled")
        
        return jsonify({'message': 'Event received'}), 200
    except Exception as e:
        logger.error(f"Error processing event: {str(e)}")
        return jsonify({'error': 'Internal server error'}), 500

@app.route('/api/webhooks/status', methods=['GET'])
def webhook_status():
    """Check webhook server status."""
    return jsonify({
        'status': 'ok',
        'version': '0.1.0',
        'timestamp': datetime.datetime.now().isoformat()
    }), 200

def main():
    """Run the webhook server."""
    port = int(os.environ.get('PORT', 5000))
    app.run(host='0.0.0.0', port=port)

if __name__ == '__main__':
    main()