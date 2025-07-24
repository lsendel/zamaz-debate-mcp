#!/usr/bin/env python3

import yaml
import os
from pathlib import Path

# Define permissions needed for various actions
PERMISSIONS_MAP = {
    'security-events: write': [
        'github/codeql-action/upload-sarif',
        'github/codeql-action/analyze',
        'snyk/actions',
        'aquasecurity/trivy-action'
    ],
    'packages: write': [
        'docker/build-push-action',
        'docker/login-action',
        'docker/metadata-action'
    ],
    'contents: write': [
        'actions/create-release',
        'peter-evans/create-pull-request',
        'github-actions-x/commit',
        'softprops/action-gh-release'
    ],
    'pull-requests: write': [
        'peter-evans/create-pull-request',
        'actions/labeler'
    ],
    'issues: write': [
        'actions/create-issue',
        'peter-evans/create-issue-from-file'
    ]
}

# Workflows that should have specific base permissions
WORKFLOW_BASE_PERMISSIONS = {
    # Workflows that call reusable workflows need base permissions
    'workflow-failure-handler.yml': {
        'contents': 'read',
        'issues': 'write',
        'actions': 'read'
    }
}

def fix_workflow_permissions(file_path):
    """Fix permissions for a single workflow file"""
    with open(file_path, 'r') as f:
        content = f.read()
        workflow = yaml.safe_load(content)
    
    if not workflow:
        return False, "Empty workflow file"
    
    needs_update = False
    needed_permissions = {}
    
    # Check if it already has workflow-level permissions
    has_workflow_permissions = 'permissions' in workflow
    
    # Check if it uses reusable workflows
    uses_reusable_workflows = False
    jobs = workflow.get('jobs', {})
    
    for job_name, job_config in jobs.items():
        # Check if job uses reusable workflow
        if 'uses' in job_config:
            uses_reusable_workflows = True
            # Reusable workflows need at least read permissions
            needed_permissions['contents'] = 'read'
            needed_permissions['actions'] = 'read'
        
        # Check job permissions
        if 'permissions' in job_config:
            # If job has permissions, we might not need workflow-level ones
            continue
            
        # Analyze steps for needed permissions
        steps = job_config.get('steps', [])
        for step in steps:
            if 'uses' in step:
                action = step['uses']
                # Check which permissions this action needs
                for perm, actions in PERMISSIONS_MAP.items():
                    for action_pattern in actions:
                        if action_pattern in action:
                            key, value = perm.split(': ')
                            needed_permissions[key] = value
            
            # Check for gh CLI usage
            if 'run' in step:
                run_script = step['run']
                if 'gh issue' in run_script:
                    needed_permissions['issues'] = 'write'
                if 'gh pr' in run_script:
                    needed_permissions['pull-requests'] = 'write'
                if 'gh release' in run_script:
                    needed_permissions['contents'] = 'write'
                if 'git push' in run_script or 'git commit' in run_script:
                    needed_permissions['contents'] = 'write'
    
    # Check if this workflow has specific base permissions defined
    workflow_name = os.path.basename(file_path)
    if workflow_name in WORKFLOW_BASE_PERMISSIONS:
        for key, value in WORKFLOW_BASE_PERMISSIONS[workflow_name].items():
            if key not in needed_permissions or needed_permissions[key] == 'read':
                needed_permissions[key] = value
    
    # Add permissions if needed
    if (needed_permissions or uses_reusable_workflows) and not has_workflow_permissions:
        # Always include contents: read as base permission
        if 'contents' not in needed_permissions:
            needed_permissions['contents'] = 'read'
        
        workflow['permissions'] = needed_permissions
        needs_update = True
    
    if needs_update:
        # Write updated workflow
        # First, find where to insert permissions (after 'on' section)
        lines = content.split('\n')
        insert_index = -1
        
        # Find the 'on:' section
        for i, line in enumerate(lines):
            if line.strip().startswith('on:'):
                # Find the end of the on section
                indent_level = len(line) - len(line.lstrip())
                for j in range(i + 1, len(lines)):
                    next_line = lines[j]
                    if next_line.strip() and not next_line.startswith(' ' * (indent_level + 2)):
                        insert_index = j
                        break
                break
        
        if insert_index > 0:
            # Build permissions section
            permissions_lines = ['\npermissions:']
            for key, value in sorted(needed_permissions.items()):
                permissions_lines.append(f'  {key}: {value}')
            
            # Insert permissions
            lines.insert(insert_index, '\n'.join(permissions_lines))
            
            # Write back
            with open(file_path, 'w') as f:
                f.write('\n'.join(lines))
            
            return True, f"Added permissions: {needed_permissions}"
    
    return False, "No changes needed"

def main():
    workflow_dir = Path('.github/workflows')
    if not workflow_dir.exists():
        print("❌ .github/workflows directory not found!")
        return 1
    
    workflow_files = list(workflow_dir.glob('*.yml')) + list(workflow_dir.glob('*.yaml'))
    print(f"🔍 Processing {len(workflow_files)} workflow files...\n")
    
    updated_count = 0
    
    for workflow_file in sorted(workflow_files):
        try:
            updated, message = fix_workflow_permissions(workflow_file)
            
            if updated:
                print(f"✅ Updated {workflow_file.name}: {message}")
                updated_count += 1
            else:
                print(f"⏭️  Skipped {workflow_file.name}: {message}")
                
        except Exception as e:
            print(f"❌ Error processing {workflow_file}: {e}")
    
    print(f"\n✨ Updated {updated_count} workflow files")
    
    # Also update workflow-failure-handler.yml if it needs specific permissions
    failure_handler = workflow_dir / 'workflow-failure-handler.yml'
    if failure_handler.exists():
        print("\n🔧 Checking workflow-failure-handler.yml for special permissions...")
        with open(failure_handler, 'r') as f:
            content = f.read()
            workflow = yaml.safe_load(content)
        
        # This is a reusable workflow, it should define its own permissions
        if 'on' in workflow and 'workflow_call' in workflow['on']:
            print("✓ workflow-failure-handler.yml is a reusable workflow")
    
    return 0

if __name__ == '__main__':
    exit(main())