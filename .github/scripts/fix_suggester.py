#!/usr/bin/env python3
"""
Fix suggester for Kiro GitHub integration.
This module generates automated fix suggestions for code issues and handles applying them.
"""

import os
import re
import json
import logging
import difflib
import base64
from typing import Dict, List, Any, Optional, Tuple, Union

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler('kiro_fix_suggester.log')
    ]
)
logger = logging.getLogger('kiro_fix_suggester')

class FixSuggester:
    """Generates and applies fix suggestions for code issues."""
    
    def __init__(self, github_client, repo_full_name: str, pr_number: int, config: Optional[Dict[str, Any]] = None):
        """Initialize the fix suggester."""
        self.github = github_client
        self.repo_full_name = repo_full_name
        self.pr_number = pr_number
        self.config = config or {}
        self.auto_fix_enabled = self.config.get('review', {}).get('auto_fix', True)
    
    def generate_fix_suggestions(self, issues: List[Dict[str, Any]], files: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """Generate fix suggestions for a list of issues."""
        logger.info(f"Generating fix suggestions for {len(issues)} issues")
        
        # Skip if auto-fix is disabled
        if not self.auto_fix_enabled:
            logger.info("Auto-fix is disabled, skipping fix suggestion generation")
            return []
        
        # Create a map of files for quick lookup
        file_map = {file.get('filename'): file for file in files}
        
        # Generate fix suggestions
        suggestions = []
        for issue in issues:
            file_path = issue.get('file_path')
            if not file_path or file_path not in file_map:
                continue
            
            # Get file content
            file_content = self._get_file_content(file_map[file_path])
            if not file_content:
                continue
            
            # Generate fix suggestion
            suggestion = self._generate_fix_suggestion(issue, file_content)
            if suggestion:
                suggestions.append({
                    'issue': issue,
                    'suggestion': suggestion
                })
        
        logger.info(f"Generated {len(suggestions)} fix suggestions")
        return suggestions
    
    def _get_file_content(self, file: Dict[str, Any]) -> Optional[str]:
        """Get the content of a file."""
        # Check if content is already available
        if 'content' in file:
            return file['content']
        
        # Try to get content from raw_url
        if 'raw_url' in file:
            try:
                import requests
                response = requests.get(file['raw_url'])
                return response.text
            except Exception as e:
                logger.warning(f"Error fetching file content from raw_url: {str(e)}")
        
        # Try to get content from GitHub API
        try:
            contents_url = file.get('contents_url')
            if contents_url:
                response = self.github.get(contents_url)
                if 'content' in response:
                    content = base64.b64decode(response['content']).decode('utf-8')
                    return content
        except Exception as e:
            logger.warning(f"Error fetching file content from GitHub API: {str(e)}")
        
        return None
    
    def _generate_fix_suggestion(self, issue: Dict[str, Any], file_content: str) -> Optional[Dict[str, Any]]:
        """Generate a fix suggestion for an issue."""
        # Check if issue already has a fix suggestion
        if issue.get('fix_suggestion'):
            suggestion = self._create_suggestion_from_existing(issue, file_content)
        else:
            # Generate fix based on issue type
            category = issue.get('category')
            rule_id = issue.get('rule_id')
            
            if category == 'style':
                suggestion = self._generate_style_fix(issue, file_content, rule_id)
            elif category == 'security':
                suggestion = self._generate_security_fix(issue, file_content, rule_id)
            elif category == 'performance':
                suggestion = self._generate_performance_fix(issue, file_content, rule_id)
            else:
                suggestion = None
        
        # Improve suggestion using learning system
        if suggestion:
            try:
                from learning_system import improve_suggestion
                
                context = {
                    'file_path': issue.get('file_path', ''),
                    'category': issue.get('category', ''),
                    'rule_id': issue.get('rule_id', '')
                }
                
                original_text = suggestion.get('replacement_text', '')
                improved_text = improve_suggestion(original_text, context)
                
                if improved_text != original_text:
                    suggestion['replacement_text'] = improved_text
                    suggestion['description'] += ' (improved by learning system)'
            
            except Exception as e:
                logger.warning(f"Error improving suggestion: {str(e)}")
        
        return suggestion
    
    def _create_suggestion_from_existing(self, issue: Dict[str, Any], file_content: str) -> Dict[str, Any]:
        """Create a suggestion from an existing fix suggestion."""
        line_start = issue.get('line_start', 1)
        line_end = issue.get('line_end', line_start)
        fix_suggestion = issue.get('fix_suggestion', '')
        
        # Get the lines to replace
        lines = file_content.splitlines()
        original_lines = lines[line_start - 1:line_end]
        original_text = '\n'.join(original_lines)
        
        return {
            'type': 'replace',
            'line_start': line_start,
            'line_end': line_end,
            'original_text': original_text,
            'replacement_text': fix_suggestion,
            'description': issue.get('message', 'Fix issue')
        }
    
    def _generate_style_fix(self, issue: Dict[str, Any], file_content: str, rule_id: str) -> Optional[Dict[str, Any]]:
        """Generate a fix for a style issue."""
        line_start = issue.get('line_start', 1)
        line_end = issue.get('line_end', line_start)
        
        # Get the lines to fix
        lines = file_content.splitlines()
        original_lines = lines[line_start - 1:line_end]
        original_text = '\n'.join(original_lines)
        
        # Generate fix based on rule ID
        if rule_id == 'flake8-E201':
            # Fix whitespace after '('
            replacement_text = re.sub(r'\(\s+', '(', original_text)
            if replacement_text != original_text:
                return {
                    'type': 'replace',
                    'line_start': line_start,
                    'line_end': line_end,
                    'original_text': original_text,
                    'replacement_text': replacement_text,
                    'description': 'Remove whitespace after opening parenthesis'
                }
        
        elif rule_id == 'flake8-E501':
            # Fix long lines by breaking at appropriate points
            if len(original_text) > 79 and ',' in original_text:
                # Try to break at commas
                parts = original_text.split(',')
                indentation = len(original_text) - len(original_text.lstrip())
                indent_str = ' ' * indentation
                
                # Rebuild with line breaks
                result = parts[0] + ','
                current_line_length = len(result)
                
                for part in parts[1:]:
                    if current_line_length + len(part) > 75:  # Leave some margin
                        result += f'\n{indent_str}{part.lstrip()},'
                        current_line_length = indentation + len(part.lstrip()) + 1
                    else:
                        result += part + ','
                        current_line_length += len(part) + 1
                
                # Remove trailing comma
                if result.endswith(','):
                    result = result[:-1]
                
                return {
                    'type': 'replace',
                    'line_start': line_start,
                    'line_end': line_end,
                    'original_text': original_text,
                    'replacement_text': result,
                    'description': 'Break long line into multiple lines'
                }
        
        # No specific fix available
        return None
    
    def _generate_security_fix(self, issue: Dict[str, Any], file_content: str, rule_id: str) -> Optional[Dict[str, Any]]:
        """Generate a fix for a security issue."""
        line_start = issue.get('line_start', 1)
        line_end = issue.get('line_end', line_start)
        
        # Get the lines to fix
        lines = file_content.splitlines()
        original_lines = lines[line_start - 1:line_end]
        original_text = '\n'.join(original_lines)
        
        # Generate fix based on rule ID
        if rule_id == 'hardcoded-password':
            # Replace hardcoded password with environment variable
            match = re.search(r'(password\s*=\s*[\'"]).+([\'"])', original_text, re.IGNORECASE)
            if match:
                prefix = match.group(1)
                suffix = match.group(2)
                replacement_text = original_text.replace(
                    match.group(0),
                    f'{prefix}" + os.environ.get("PASSWORD", "") + "{suffix}'
                )
                
                return {
                    'type': 'replace',
                    'line_start': line_start,
                    'line_end': line_end,
                    'original_text': original_text,
                    'replacement_text': replacement_text,
                    'description': 'Replace hardcoded password with environment variable'
                }
        
        elif rule_id == 'hardcoded-api-key':
            # Replace hardcoded API key with environment variable
            match = re.search(r'(api[_-]?key\s*=\s*[\'"]).+([\'"])', original_text, re.IGNORECASE)
            if match:
                prefix = match.group(1)
                suffix = match.group(2)
                replacement_text = original_text.replace(
                    match.group(0),
                    f'{prefix}" + os.environ.get("API_KEY", "") + "{suffix}'
                )
                
                return {
                    'type': 'replace',
                    'line_start': line_start,
                    'line_end': line_end,
                    'original_text': original_text,
                    'replacement_text': replacement_text,
                    'description': 'Replace hardcoded API key with environment variable'
                }
        
        elif rule_id == 'sql-injection':
            # Replace string concatenation in SQL with parameterized query
            if 'SELECT' in original_text or 'INSERT' in original_text or 'UPDATE' in original_text or 'DELETE' in original_text:
                # This is a simplified example - real SQL parsing would be more complex
                if '+' in original_text and "'" in original_text:
                    # Replace string concatenation with parameter placeholder
                    replacement_text = re.sub(r"'([^']*)'\s*\+\s*([^']+)\s*\+\s*'([^']*)'", r"'\1?\3'", original_text)
                    
                    # Add comment explaining the change
                    replacement_text += "\n# Use parameterized query to prevent SQL injection"
                    
                    return {
                        'type': 'replace',
                        'line_start': line_start,
                        'line_end': line_end,
                        'original_text': original_text,
                        'replacement_text': replacement_text,
                        'description': 'Replace string concatenation with parameterized query'
                    }
        
        # No specific fix available
        return None
    
    def _generate_performance_fix(self, issue: Dict[str, Any], file_content: str, rule_id: str) -> Optional[Dict[str, Any]]:
        """Generate a fix for a performance issue."""
        line_start = issue.get('line_start', 1)
        line_end = issue.get('line_end', line_start)
        
        # Get the lines to fix
        lines = file_content.splitlines()
        original_lines = lines[line_start - 1:line_end]
        original_text = '\n'.join(original_lines)
        
        # Generate fix based on rule ID
        if rule_id == 'multiple-filters':
            # Combine multiple filters
            if '.filter(' in original_text:
                # This is a simplified example - real parsing would be more complex
                match = re.search(r'(\.filter\([^)]+\))\s*\.\s*(filter\([^)]+\))', original_text)
                if match:
                    filter1 = match.group(1)
                    filter2 = match.group(2)
                    
                    # Extract filter conditions
                    condition1 = re.search(r'filter\(([^)]+)\)', filter1)
                    condition2 = re.search(r'filter\(([^)]+)\)', filter2)
                    
                    if condition1 and condition2:
                        # Combine conditions
                        combined = f".filter({condition1.group(1)} & {condition2.group(1)})"
                        replacement_text = original_text.replace(f"{filter1}.{filter2}", combined)
                        
                        return {
                            'type': 'replace',
                            'line_start': line_start,
                            'line_end': line_end,
                            'original_text': original_text,
                            'replacement_text': replacement_text,
                            'description': 'Combine multiple filters for better performance'
                        }
        
        elif rule_id == 'select-all':
            # Replace SELECT * with specific columns
            if 'SELECT *' in original_text:
                # This is a placeholder - in a real implementation, we would need to analyze the schema
                replacement_text = original_text.replace('SELECT *', 'SELECT id, name, created_at')
                
                return {
                    'type': 'replace',
                    'line_start': line_start,
                    'line_end': line_end,
                    'original_text': original_text,
                    'replacement_text': replacement_text,
                    'description': 'Specify columns instead of using SELECT *'
                }
        
        # No specific fix available
        return None
    
    def format_suggestion_as_comment(self, suggestion: Dict[str, Any]) -> str:
        """Format a suggestion as a GitHub comment with suggestion block."""
        original_text = suggestion.get('original_text', '')
        replacement_text = suggestion.get('replacement_text', '')
        description = suggestion.get('description', 'Fix issue')
        
        # Create GitHub suggestion block
        comment = f"{description}:\n\n"
        comment += "```suggestion\n"
        comment += f"{replacement_text}\n"
        comment += "```\n"
        
        return comment
    
    def apply_suggestion(self, suggestion_id: str, commit_message: str = None) -> bool:
        """Apply a suggestion to create a commit."""
        try:
            # Get suggestion details from comment
            comment_url = f"{self.github.api_base}/repos/{self.repo_full_name}/pulls/comments/{suggestion_id}"
            comment = self.github.get(comment_url)
            
            # Extract suggestion content
            body = comment.get('body', '')
            match = re.search(r'```suggestion\n(.*?)```', body, re.DOTALL)
            if not match:
                logger.warning(f"No suggestion block found in comment {suggestion_id}")
                return False
            
            suggestion_content = match.group(1)
            
            # Get the file path and position
            path = comment.get('path')
            position = comment.get('position')
            
            if not path or position is None:
                logger.warning(f"Missing path or position in comment {suggestion_id}")
                return False
            
            # Get the file content
            contents_url = f"{self.github.api_base}/repos/{self.repo_full_name}/contents/{path}"
            response = self.github.get(contents_url, params={'ref': comment.get('pull_request_review_id')})
            
            if 'content' not in response:
                logger.warning(f"Could not get content for file {path}")
                return False
            
            content = base64.b64decode(response['content']).decode('utf-8')
            lines = content.splitlines()
            
            # Apply the suggestion
            original_line = lines[position - 1]
            lines[position - 1] = suggestion_content.strip()
            new_content = '\n'.join(lines)
            
            # Create a commit
            if not commit_message:
                commit_message = f"Apply suggestion from comment {suggestion_id}"
            
            # Create blob
            blob_url = f"{self.github.api_base}/repos/{self.repo_full_name}/git/blobs"
            blob_data = {
                'content': new_content,
                'encoding': 'utf-8'
            }
            blob = self.github.post(blob_url, json=blob_data)
            
            # Get the current commit SHA
            ref_url = f"{self.github.api_base}/repos/{self.repo_full_name}/git/refs/heads/{comment.get('head_ref')}"
            ref = self.github.get(ref_url)
            commit_sha = ref.get('object', {}).get('sha')
            
            # Get the current tree
            commit_url = f"{self.github.api_base}/repos/{self.repo_full_name}/git/commits/{commit_sha}"
            commit = self.github.get(commit_url)
            tree_sha = commit.get('tree', {}).get('sha')
            
            # Create a new tree
            tree_url = f"{self.github.api_base}/repos/{self.repo_full_name}/git/trees"
            tree_data = {
                'base_tree': tree_sha,
                'tree': [
                    {
                        'path': path,
                        'mode': '100644',
                        'type': 'blob',
                        'sha': blob.get('sha')
                    }
                ]
            }
            tree = self.github.post(tree_url, json=tree_data)
            
            # Create a new commit
            new_commit_url = f"{self.github.api_base}/repos/{self.repo_full_name}/git/commits"
            new_commit_data = {
                'message': commit_message,
                'tree': tree.get('sha'),
                'parents': [commit_sha]
            }
            new_commit = self.github.post(new_commit_url, json=new_commit_data)
            
            # Update the reference
            update_ref_url = f"{self.github.api_base}/repos/{self.repo_full_name}/git/refs/heads/{comment.get('head_ref')}"
            update_ref_data = {
                'sha': new_commit.get('sha'),
                'force': False
            }
            self.github.patch(update_ref_url, json=update_ref_data)
            
            logger.info(f"Applied suggestion {suggestion_id} to file {path}")
            return True
        
        except Exception as e:
            logger.error(f"Error applying suggestion {suggestion_id}: {str(e)}")
            return False
    
    def handle_suggestion_comment(self, comment_id: str, comment_body: str) -> bool:
        """Handle a comment that might contain a command to apply a suggestion."""
        # Check for apply suggestion command
        match = re.search(r'/apply-suggestion(?:\s+(\d+))?', comment_body)
        if not match:
            return False
        
        # Get suggestion ID
        suggestion_id = match.group(1)
        if not suggestion_id:
            # If no specific ID, look for the parent comment
            try:
                comment_url = f"{self.github.api_base}/repos/{self.repo_full_name}/issues/comments/{comment_id}"
                comment = self.github.get(comment_url)
                
                # Check if this is a reply to another comment
                in_reply_to = comment.get('in_reply_to_id')
                if in_reply_to:
                    suggestion_id = in_reply_to
                else:
                    logger.warning(f"No suggestion ID specified and comment {comment_id} is not a reply")
                    return False
            
            except Exception as e:
                logger.error(f"Error getting comment {comment_id}: {str(e)}")
                return False
        
        # Extract commit message if provided
        commit_message = None
        message_match = re.search(r'/apply-suggestion(?:\s+\d+)?\s+"([^"]+)"', comment_body)
        if message_match:
            commit_message = message_match.group(1)
        
        # Apply the suggestion
        return self.apply_suggestion(suggestion_id, commit_message)

def generate_fix_suggestions(github_client, repo_full_name: str, pr_number: int, issues: List[Dict[str, Any]], files: List[Dict[str, Any]], config: Optional[Dict[str, Any]] = None) -> List[Dict[str, Any]]:
    """Generate fix suggestions for a list of issues."""
    suggester = FixSuggester(github_client, repo_full_name, pr_number, config)
    return suggester.generate_fix_suggestions(issues, files)

def format_suggestion_as_comment(suggestion: Dict[str, Any]) -> str:
    """Format a suggestion as a GitHub comment with suggestion block."""
    suggester = FixSuggester(None, "", 0)
    return suggester.format_suggestion_as_comment(suggestion)

def handle_suggestion_comment(github_client, repo_full_name: str, pr_number: int, comment_id: str, comment_body: str, config: Optional[Dict[str, Any]] = None) -> bool:
    """Handle a comment that might contain a command to apply a suggestion."""
    suggester = FixSuggester(github_client, repo_full_name, pr_number, config)
    return suggester.handle_suggestion_comment(comment_id, comment_body)

if __name__ == "__main__":
    # Example usage
    import sys
    
    if len(sys.argv) < 3:
        print("Usage: fix_suggester.py <issue_file> <file_path>")
        sys.exit(1)
    
    issue_file = sys.argv[1]
    file_path = sys.argv[2]
    
    try:
        with open(issue_file, 'r') as f:
            issues = json.load(f)
        
        with open(file_path, 'r') as f:
            file_content = f.read()
        
        # Create a mock file structure
        files = [{
            'filename': os.path.basename(file_path),
            'content': file_content
        }]
        
        # Create a mock GitHub client
        class MockGitHubClient:
            def get(self, url, params=None):
                return {}
            
            def post(self, url, json=None):
                return {}
            
            def patch(self, url, json=None):
                return {}
        
        # Generate fix suggestions
        suggester = FixSuggester(MockGitHubClient(), "example/repo", 123)
        suggestions = suggester.generate_fix_suggestions(issues, files)
        
        # Print suggestions
        for suggestion in suggestions:
            print(f"Issue: {suggestion['issue'].get('message')}")
            print(f"Original: {suggestion['suggestion'].get('original_text')}")
            print(f"Replacement: {suggestion['suggestion'].get('replacement_text')}")
            print(f"Description: {suggestion['suggestion'].get('description')}")
            print()
            print("Comment format:")
            print(suggester.format_suggestion_as_comment(suggestion['suggestion']))
            print("-" * 40)
    
    except Exception as e:
        print(f"Error: {str(e)}")
        sys.exit(1)