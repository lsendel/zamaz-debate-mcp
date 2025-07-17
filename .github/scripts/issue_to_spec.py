#!/usr/bin/env python3
"""
Script to convert GitHub issues to Kiro specs.
This script is triggered by the issue-to-spec.yml GitHub Action workflow.
"""

import os
import re
import json
import requests
import yaml
from pathlib import Path

# Get environment variables
GITHUB_TOKEN = os.environ.get('GITHUB_TOKEN')
ISSUE_NUMBER = os.environ.get('ISSUE_NUMBER')
ISSUE_TITLE = os.environ.get('ISSUE_TITLE')
ISSUE_BODY = os.environ.get('ISSUE_BODY')
REPO_NAME = os.environ.get('REPO_NAME')
KIRO_API_KEY = os.environ.get('KIRO_API_KEY')

# Constants
GITHUB_API_URL = "https://api.github.com"
KIRO_API_URL = "https://api.kiro.ai"  # Replace with actual Kiro API URL if available

def create_feature_name(title):
    """Convert issue title to kebab-case feature name."""
    # Remove special characters and convert to lowercase
    name = re.sub(r'[^a-zA-Z0-9\s]', '', title).lower()
    # Replace spaces with hyphens
    name = re.sub(r'\s+', '-', name)
    return name

def get_issue_details():
    """Get detailed information about the issue from GitHub API."""
    url = f"{GITHUB_API_URL}/repos/{REPO_NAME}/issues/{ISSUE_NUMBER}"
    headers = {
        "Authorization": f"token {GITHUB_TOKEN}",
        "Accept": "application/vnd.github.v3+json"
    }
    response = requests.get(url, headers=headers)
    response.raise_for_status()
    return response.json()

def get_linked_issues(issue_body):
    """Extract references to other issues from the issue body."""
    # Look for patterns like #123 or owner/repo#123
    issue_refs = re.findall(r'(?:^|\s)(?:#(\d+)|([a-zA-Z0-9_-]+/[a-zA-Z0-9_-]+)#(\d+))', issue_body)
    linked_issues = []
    
    for ref in issue_refs:
        if ref[0]:  # Format: #123
            linked_issues.append({
                "repo": REPO_NAME,
                "number": ref[0]
            })
        elif ref[1] and ref[2]:  # Format: owner/repo#123
            linked_issues.append({
                "repo": ref[1],
                "number": ref[2]
            })
    
    return linked_issues

def extract_capabilities(issue_body):
    """Extract capabilities or key points from the issue body."""
    capabilities = []
    
    # Look for bullet points or numbered lists
    bullet_matches = re.findall(r'(?:^|\n)[\s]*[-*][\s]+(.*?)(?:\n|$)', issue_body)
    numbered_matches = re.findall(r'(?:^|\n)[\s]*\d+\.[\s]+(.*?)(?:\n|$)', issue_body)
    
    capabilities.extend(bullet_matches)
    capabilities.extend(numbered_matches)
    
    return capabilities

def generate_spec_with_kiro_api(feature_name, issue_details, capabilities):
    """
    Generate spec documents using Kiro API.
    This is a placeholder - in a real implementation, you would call the Kiro API.
    """
    if KIRO_API_KEY:
        # This would be the actual API call to Kiro
        # For now, we'll just create placeholder documents
        print("Would call Kiro API here if it was available")
    
    # Create placeholder documents
    requirements_md = generate_requirements_doc(feature_name, issue_details, capabilities)
    design_md = generate_design_doc(feature_name)
    tasks_md = generate_tasks_doc(feature_name)
    
    return {
        "requirements.md": requirements_md,
        "design.md": design_md,
        "tasks.md": tasks_md
    }

def generate_requirements_doc(feature_name, issue_details, capabilities):
    """Generate a basic requirements document based on the issue."""
    title = issue_details.get('title', 'Unknown Feature')
    body = issue_details.get('body', '')
    
    # Extract the first paragraph as an introduction
    intro = body.split('\n\n')[0] if '\n\n' in body else body
    
    requirements = []
    for i, capability in enumerate(capabilities, 1):
        requirements.append(f"""
### Requirement {i}

**User Story:** As a user, I want {capability}, so that I can be more productive.

#### Acceptance Criteria

1. WHEN [event] THEN the system SHALL [response]
2. IF [precondition] THEN the system SHALL [response]
3. WHEN [event] THEN the system SHALL [response]
""")
    
    return f"""# Requirements Document

## Introduction

This document outlines the requirements for implementing {title}. {intro}

## Requirements
{''.join(requirements)}
"""

def generate_design_doc(feature_name):
    """Generate a placeholder design document."""
    return f"""# Design Document for {feature_name}

## Overview

This document outlines the design for implementing the {feature_name} feature.

## Architecture

(Add architecture details here)

## Components and Interfaces

(Add component details here)

## Data Models

(Add data model details here)

## Error Handling

(Add error handling details here)

## Testing Strategy

(Add testing strategy details here)
"""

def generate_tasks_doc(feature_name):
    """Generate a placeholder tasks document."""
    return f"""# Implementation Plan

- [ ] 1. Set up project structure
  - Create directory structure
  - Set up configuration files
  - _Requirements: 1.1_

- [ ] 2. Implement core functionality
  - Create main components
  - Implement business logic
  - _Requirements: 1.2, 1.3_

- [ ] 3. Add user interface
  - Design UI components
  - Implement user interactions
  - _Requirements: 2.1_

- [ ] 4. Set up testing
  - Create unit tests
  - Implement integration tests
  - _Requirements: 3.1_

- [ ] 5. Documentation
  - Create user documentation
  - Add developer guides
  - _Requirements: 3.2_
"""

def create_spec_files(feature_name, spec_content):
    """Create the spec files in the repository."""
    # Create the spec directory
    spec_dir = Path(f".kiro/specs/{feature_name}")
    spec_dir.mkdir(parents=True, exist_ok=True)
    
    # Write the spec files
    for filename, content in spec_content.items():
        with open(spec_dir / filename, 'w') as f:
            f.write(content)
    
    print(f"Created spec files in {spec_dir}")

def add_comment_to_issue(issue_number, comment):
    """Add a comment to the GitHub issue."""
    url = f"{GITHUB_API_URL}/repos/{REPO_NAME}/issues/{issue_number}/comments"
    headers = {
        "Authorization": f"token {GITHUB_TOKEN}",
        "Accept": "application/vnd.github.v3+json"
    }
    data = {
        "body": comment
    }
    response = requests.post(url, headers=headers, json=data)
    response.raise_for_status()
    print(f"Added comment to issue #{issue_number}")

def main():
    try:
        print(f"Processing issue #{ISSUE_NUMBER}: {ISSUE_TITLE}")
        
        # Get detailed issue information
        issue_details = get_issue_details()
        
        # Create feature name from issue title
        feature_name = create_feature_name(ISSUE_TITLE)
        print(f"Feature name: {feature_name}")
        
        # Extract capabilities from issue body
        capabilities = extract_capabilities(ISSUE_BODY)
        print(f"Extracted {len(capabilities)} capabilities")
        
        # Generate spec content
        spec_content = generate_spec_with_kiro_api(feature_name, issue_details, capabilities)
        
        # Create spec files
        create_spec_files(feature_name, spec_content)
        
        # Add comment to the issue
        comment = f"""
I've created a spec for this feature:

- Feature name: `{feature_name}`
- Spec location: `.kiro/specs/{feature_name}/`

The spec includes:
- Requirements document
- Design document
- Tasks document

A pull request has been created with these changes.
"""
        add_comment_to_issue(ISSUE_NUMBER, comment)
        
        print("Successfully created spec from issue")
        
    except Exception as e:
        print(f"Error: {str(e)}")
        # Add error comment to issue
        error_comment = f"""
I encountered an error while trying to create a spec for this issue:

```
{str(e)}
```

Please check the GitHub Action logs for more details.
"""
        try:
            add_comment_to_issue(ISSUE_NUMBER, error_comment)
        except:
            print("Failed to add error comment to issue")
        
        raise

if __name__ == "__main__":
    main()