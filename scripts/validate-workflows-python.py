#!/usr/bin/env python3

import yaml
import os
import sys
from pathlib import Path

def validate_workflow(file_path):
    """Validate a single workflow file"""
    errors = []
    warnings = []
    
    try:
        with open(file_path, 'r') as f:
            workflow = yaml.safe_load(f)
    except Exception as e:
        return [(f"YAML parse error: {e}", 'error')]
    
    # Get all job names
    jobs = workflow.get('jobs', {})
    job_names = set(jobs.keys())
    
    # Check each job for dependencies
    for job_name, job_config in jobs.items():
        if 'needs' in job_config:
            needs = job_config['needs']
            # Handle both string and list formats
            if isinstance(needs, str):
                needs = [needs]
            
            for needed_job in needs:
                if needed_job not in job_names:
                    errors.append((f"Job '{job_name}' depends on unknown job '{needed_job}'", 'error'))
    
    # Check for deprecated actions
    for job_name, job_config in jobs.items():
        steps = job_config.get('steps', [])
        for step in steps:
            if 'uses' in step:
                action = step['uses']
                if 'actions/checkout@v' in action and not action.endswith('@v4'):
                    warnings.append((f"Job '{job_name}' uses outdated checkout action: {action}", 'warning'))
                if 'actions/setup-node@v' in action and not action.endswith('@v4'):
                    warnings.append((f"Job '{job_name}' uses outdated setup-node action: {action}", 'warning'))
    
    return errors + warnings

def main():
    workflow_dir = Path('.github/workflows')
    if not workflow_dir.exists():
        print("❌ .github/workflows directory not found!")
        return 1
    
    workflow_files = list(workflow_dir.glob('*.yml')) + list(workflow_dir.glob('*.yaml'))
    print(f"🔍 Validating {len(workflow_files)} workflow files...\n")
    
    total_errors = 0
    total_warnings = 0
    
    for workflow_file in sorted(workflow_files):
        print(f"Checking {workflow_file}...")
        issues = validate_workflow(workflow_file)
        
        if not issues:
            print("  ✅ Valid")
        else:
            for issue, issue_type in issues:
                if issue_type == 'error':
                    print(f"  ❌ ERROR: {issue}")
                    total_errors += 1
                else:
                    print(f"  ⚠️  WARNING: {issue}")
                    total_warnings += 1
        print()
    
    print("=" * 50)
    print(f"Summary:")
    print(f"Total files checked: {len(workflow_files)}")
    print(f"Errors found: {total_errors}")
    print(f"Warnings found: {total_warnings}")
    
    if total_errors == 0:
        print("\n✅ All workflows have valid job dependencies!")
        return 0
    else:
        print(f"\n❌ Found {total_errors} workflow errors that need fixing")
        return 1

if __name__ == '__main__':
    sys.exit(main())