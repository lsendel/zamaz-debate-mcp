#!/usr/bin/env python3

import os
import re
from pathlib import Path

def add_issues_permission(file_path):
    """Add issues: write permission to a workflow file"""
    with open(file_path, 'r') as f:
        content = f.read()
    
    # Check if workflow calls workflow-failure-handler.yml
    if 'workflow-failure-handler.yml' not in content:
        return False, "Doesn't use workflow-failure-handler"
    
    # Check if already has issues: write
    if 'issues: write' in content:
        return False, "Already has issues: write permission"
    
    # Find permissions block
    permissions_match = re.search(r'^permissions:\s*\n((?:  \w+: \w+\s*\n)*)', content, re.MULTILINE)
    
    if permissions_match:
        # Add issues: write to existing permissions
        permissions_block = permissions_match.group(0)
        new_permissions = permissions_block.rstrip() + '\n  issues: write\n'
        content = content.replace(permissions_block, new_permissions)
        
        with open(file_path, 'w') as f:
            f.write(content)
        
        return True, "Added issues: write permission"
    else:
        return False, "No permissions block found"

def main():
    workflow_dir = Path('.github/workflows')
    workflows_updated = 0
    
    for workflow_file in workflow_dir.glob('*.yml'):
        updated, message = add_issues_permission(workflow_file)
        
        if updated:
            print(f"✅ Updated {workflow_file.name}: {message}")
            workflows_updated += 1
        else:
            print(f"⏭️  Skipped {workflow_file.name}: {message}")
    
    print(f"\n✨ Updated {workflows_updated} workflow files")

if __name__ == '__main__':
    main()