#!/usr/bin/env python3
"""
Pull Request processor for Kiro GitHub integration.
This module handles the processing of pull requests for code review.
"""

import os
import re
import json
import time
import logging
import requests
import threading
import queue
from collections import defaultdict
from github_auth import GitHubCredentialManager

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler('kiro_pr_processor.log')
    ]
)
logger = logging.getLogger('kiro_pr_processor')

# Initialize credential manager
credential_manager = GitHubCredentialManager()

# Constants
GITHUB_API_URL = "https://api.github.com"
KIRO_API_URL = os.environ.get('KIRO_API_URL', 'https://api.kiro.ai')
KIRO_BOT_USERNAME = os.environ.get('KIRO_BOT_USERNAME', 'kiro-ai')

# Processing queue
pr_queue = queue.PriorityQueue()
processing_threads = []
MAX_THREADS = 5
is_running = True

class PullRequestProcessor:
    """Processes pull requests for code review."""
    
    def __init__(self, github_client, repo_full_name, pr_number, config=None):
        """Initialize the PR processor."""
        self.github = github_client
        self.repo_full_name = repo_full_name
        self.pr_number = pr_number
        self.config = config or {}
        self.repo_owner, self.repo_name = repo_full_name.split('/')
        self.pr_data = None
        self.files = []
        self.diff = ""
        self.linked_issues = []
        self.context = {}
    
    def process(self):
        """Process the pull request."""
        try:
            logger.info(f"Processing PR #{self.pr_number} in {self.repo_full_name}")
            
            # Start analytics tracking
            from analytics_collector import start_review
            self.review_id = start_review(self.repo_owner, self.repo_name, self.pr_number)
            
            # Extract PR metadata
            self.extract_pr_metadata()
            
            # Retrieve and parse code diffs
            self.retrieve_code_diffs()
            
            # Gather context from linked issues
            self.gather_context_from_linked_issues()
            
            # Analyze code changes
            self.analyze_code_changes()
            
            # Generate review comments
            self.generate_review_comments()
            
            # Complete analytics tracking
            if hasattr(self, 'review_id') and self.review_id:
                from analytics_collector import complete_review
                complete_review(
                    self.review_id,
                    getattr(self, 'issues', []),
                    len(self.files),
                    len(getattr(self, 'review_data', {}).get('comments', [])),
                    sum(1 for issue in getattr(self, 'issues', []) if 'suggestion' in issue)
                )
            
            logger.info(f"Completed processing PR #{self.pr_number}")
            return True
        
        except Exception as e:
            logger.error(f"Error processing PR #{self.pr_number}: {str(e)}")
            return False
    
    def extract_pr_metadata(self):
        """Extract metadata from the pull request."""
        logger.info(f"Extracting metadata for PR #{self.pr_number}")
        
        # Get PR data
        pr_url = f"{GITHUB_API_URL}/repos/{self.repo_full_name}/pulls/{self.pr_number}"
        self.pr_data = self.github.get(pr_url)
        
        # Extract basic metadata
        self.title = self.pr_data.get('title', '')
        self.description = self.pr_data.get('body', '')
        self.author = self.pr_data.get('user', {}).get('login', '')
        self.base_branch = self.pr_data.get('base', {}).get('ref', '')
        self.head_branch = self.pr_data.get('head', {}).get('ref', '')
        self.created_at = self.pr_data.get('created_at', '')
        self.updated_at = self.pr_data.get('updated_at', '')
        
        # Extract linked issues from PR description
        issue_refs = re.findall(r'(?:close[sd]?|fix(?:e[sd])?|resolve[sd]?)\s+(?:#(\d+)|([a-zA-Z0-9_-]+/[a-zA-Z0-9_-]+)#(\d+))', 
                               self.description, 
                               re.IGNORECASE)
        
        for ref in issue_refs:
            if ref[0]:  # Format: #123
                self.linked_issues.append({
                    'repo': self.repo_full_name,
                    'number': ref[0]
                })
            elif ref[1] and ref[2]:  # Format: owner/repo#123
                self.linked_issues.append({
                    'repo': ref[1],
                    'number': ref[2]
                })
        
        logger.info(f"Extracted metadata for PR #{self.pr_number}: {self.title}")
        logger.info(f"Found {len(self.linked_issues)} linked issues")
    
    def retrieve_code_diffs(self):
        """Retrieve and parse code diffs."""
        logger.info(f"Retrieving code diffs for PR #{self.pr_number}")
        
        # Get PR files
        files_url = f"{GITHUB_API_URL}/repos/{self.repo_full_name}/pulls/{self.pr_number}/files"
        self.files = self.github.get(files_url)
        
        # Get PR diff
        diff_url = self.pr_data.get('diff_url')
        diff_response = requests.get(diff_url)
        diff_response.raise_for_status()
        self.diff = diff_response.text
        
        # Parse diff to extract changes by file
        self.changes_by_file = self.parse_diff(self.diff)
        
        logger.info(f"Retrieved {len(self.files)} changed files for PR #{self.pr_number}")
    
    def parse_diff(self, diff_text):
        """Parse a git diff and extract changes by file."""
        changes_by_file = defaultdict(list)
        current_file = None
        current_old_line = 0
        current_new_line = 0
        
        # Split diff into lines
        lines = diff_text.split('\n')
        
        for line in lines:
            # Check for file header
            if line.startswith('diff --git'):
                # Extract filename
                match = re.search(r'diff --git a/(.*) b/(.*)', line)
                if match:
                    current_file = match.group(2)
            
            # Check for hunk header
            elif line.startswith('@@'):
                # Extract line numbers
                match = re.search(r'@@ -(\d+)(?:,\d+)? \+(\d+)(?:,\d+)? @@', line)
                if match:
                    current_old_line = int(match.group(1))
                    current_new_line = int(match.group(2))
            
            # Process content lines
            elif current_file is not None:
                if line.startswith('+'):
                    # Added line
                    changes_by_file[current_file].append({
                        'type': 'add',
                        'line_number': current_new_line,
                        'content': line[1:]
                    })
                    current_new_line += 1
                elif line.startswith('-'):
                    # Removed line
                    changes_by_file[current_file].append({
                        'type': 'remove',
                        'line_number': current_old_line,
                        'content': line[1:]
                    })
                    current_old_line += 1
                elif not line.startswith('\\'):  # Ignore "\ No newline at end of file"
                    # Context line
                    changes_by_file[current_file].append({
                        'type': 'context',
                        'old_line_number': current_old_line,
                        'new_line_number': current_new_line,
                        'content': line[1:] if line.startswith(' ') else line
                    })
                    current_old_line += 1
                    current_new_line += 1
        
        return changes_by_file
    
    def gather_context_from_linked_issues(self):
        """Gather context from linked issues."""
        logger.info(f"Gathering context from linked issues for PR #{self.pr_number}")
        
        for issue in self.linked_issues:
            try:
                repo = issue['repo']
                number = issue['number']
                
                # Get issue data
                issue_url = f"{GITHUB_API_URL}/repos/{repo}/issues/{number}"
                issue_data = self.github.get(issue_url)
                
                # Extract relevant information
                self.context[f"issue_{number}"] = {
                    'title': issue_data.get('title', ''),
                    'body': issue_data.get('body', ''),
                    'labels': [label.get('name') for label in issue_data.get('labels', [])],
                    'url': issue_data.get('html_url', '')
                }
                
                logger.info(f"Added context from issue #{number}: {issue_data.get('title', '')}")
            
            except Exception as e:
                logger.warning(f"Error gathering context from issue #{issue['number']}: {str(e)}")
    
    def analyze_code_changes(self):
        """Analyze code changes in the PR."""
        logger.info(f"Analyzing code changes for PR #{self.pr_number}")
        
        # Prepare file data for analysis
        file_data = []
        for file in self.files:
            file_data.append({
                'filename': file.get('filename'),
                'status': file.get('status'),
                'additions': file.get('additions'),
                'deletions': file.get('deletions'),
                'changes': file.get('changes'),
                'patch': file.get('patch'),
                'content_url': file.get('contents_url'),
                'raw_url': file.get('raw_url')
            })
        
        # Prepare analysis request
        analysis_request = {
            'pr': {
                'number': self.pr_number,
                'title': self.title,
                'description': self.description,
                'author': self.author,
                'base_branch': self.base_branch,
                'head_branch': self.head_branch
            },
            'repository': {
                'full_name': self.repo_full_name,
                'owner': self.repo_owner,
                'name': self.repo_name
            },
            'files': file_data,
            'context': self.context,
            'config': self.config
        }
        
        # Store the analysis request for later use
        self.analysis_request = analysis_request
        
        # Run code analysis
        try:
            from code_analyzer import analyze_pr
            
            # Run the analysis
            self.issues = analyze_pr(
                pr_data={
                    'number': self.pr_number,
                    'repository': {
                        'full_name': self.repo_full_name
                    }
                },
                files=file_data,
                diff=self.diff,
                config=self.config
            )
            
            logger.info(f"Found {len(self.issues)} issues in PR #{self.pr_number}")
            
            # Generate fix suggestions
            try:
                from fix_suggester import generate_fix_suggestions
                
                # Generate suggestions
                suggestions = generate_fix_suggestions(
                    self.github,
                    self.repo_full_name,
                    self.pr_number,
                    self.issues,
                    file_data,
                    self.config
                )
                
                # Add suggestions to issues
                for suggestion in suggestions:
                    issue = suggestion.get('issue')
                    issue_suggestion = suggestion.get('suggestion')
                    
                    # Find the corresponding issue in self.issues
                    for i, existing_issue in enumerate(self.issues):
                        if (existing_issue.get('file_path') == issue.get('file_path') and
                            existing_issue.get('line_start') == issue.get('line_start') and
                            existing_issue.get('message') == issue.get('message')):
                            # Add suggestion to the issue
                            self.issues[i]['suggestion'] = issue_suggestion
                            break
                
                logger.info(f"Generated {len(suggestions)} fix suggestions for PR #{self.pr_number}")
            
            except Exception as e:
                logger.error(f"Error generating fix suggestions: {str(e)}")
        
        except Exception as e:
            logger.error(f"Error analyzing code changes: {str(e)}")
            self.issues = []
    
    def generate_review_comments(self):
        """Generate review comments based on code analysis."""
        logger.info(f"Generating review comments for PR #{self.pr_number}")
        
        try:
            # Import comment generator
            from comment_generator import generate_comments
            
            # Generate comments based on issues
            if hasattr(self, 'issues') and self.issues:
                comments_data = generate_comments(self.issues, self.config)
                
                # Extract comments
                summary_comment = comments_data.get('summary_comment', '')
                file_comments = comments_data.get('file_comments', [])
                
                # Create review data
                review_data = {
                    'commit_id': self.pr_data.get('head', {}).get('sha'),
                    'body': summary_comment,
                    'event': 'COMMENT',
                    'comments': file_comments
                }
                
                # Store the review data for later use
                self.review_data = review_data
                
                logger.info(f"Generated review with {len(file_comments)} comments for PR #{self.pr_number}")
                
                # Submit the review to GitHub
                review_url = f"{GITHUB_API_URL}/repos/{self.repo_full_name}/pulls/{self.pr_number}/reviews"
                self.github.post(review_url, json=review_data)
                
                logger.info(f"Submitted review for PR #{self.pr_number}")
            else:
                logger.warning(f"No issues found for PR #{self.pr_number}, skipping review generation")
        
        except Exception as e:
            logger.error(f"Error generating review comments: {str(e)}")
            
            # Create a fallback review
            body = f"""
## Kiro AI Review

I've reviewed this PR but encountered an error while generating detailed comments.

### Summary
Thank you for submitting this PR! I recommend a manual review by a team member.

### Error Details
An error occurred during the automated review process. This might be due to:
- Complex code structures
- Unsupported file formats
- System limitations

Please let me know if you'd like me to try again or if you need any other assistance.
"""
            
            # Create fallback review data
            self.review_data = {
                'commit_id': self.pr_data.get('head', {}).get('sha'),
                'body': body,
                'event': 'COMMENT',
                'comments': []
            }
            
            logger.info(f"Generated fallback review for PR #{self.pr_number}")

def queue_pr_for_processing(github_client, repo_full_name, pr_number, priority=1, config=None):
    """Queue a pull request for processing."""
    logger.info(f"Queueing PR #{pr_number} in {repo_full_name} with priority {priority}")
    
    # Add to queue with priority (lower number = higher priority)
    pr_queue.put((priority, {
        'github_client': github_client,
        'repo_full_name': repo_full_name,
        'pr_number': pr_number,
        'config': config
    }))

def process_queue_item():
    """Process a single item from the queue."""
    while is_running:
        try:
            # Get item from queue with 1 second timeout
            priority, item = pr_queue.get(timeout=1)
            
            # Process the PR
            processor = PullRequestProcessor(
                item['github_client'],
                item['repo_full_name'],
                item['pr_number'],
                item['config']
            )
            
            success = processor.process()
            
            # Mark task as done
            pr_queue.task_done()
            
            logger.info(f"Processed PR #{item['pr_number']} with {'success' if success else 'failure'}")
        
        except queue.Empty:
            # Queue is empty, just continue
            pass
        
        except Exception as e:
            logger.error(f"Error in queue processing thread: {str(e)}")
            # Mark task as done if we got an item
            if 'item' in locals():
                pr_queue.task_done()

def start_processing_threads():
    """Start processing threads for the queue."""
    global processing_threads
    global is_running
    
    is_running = True
    
    # Create and start threads
    for i in range(MAX_THREADS):
        thread = threading.Thread(target=process_queue_item, daemon=True)
        thread.start()
        processing_threads.append(thread)
    
    logger.info(f"Started {len(processing_threads)} processing threads")

def stop_processing_threads():
    """Stop processing threads."""
    global is_running
    
    is_running = False
    
    # Wait for threads to finish
    for thread in processing_threads:
        thread.join(timeout=2)
    
    logger.info("Stopped processing threads")

# Start processing threads when module is imported
start_processing_threads()

# Register shutdown function
import atexit
atexit.register(stop_processing_threads)