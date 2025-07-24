#!/usr/bin/env python3

import yaml
import os
import sys
from pathlib import Path
import json

def analyze_workflow_permissions(file_path):
    """Analyze a single workflow file for permissions"""
    with open(file_path, 'r') as f:
        workflow = yaml.safe_load(f)
    
    result = {
        'file': file_path.name,
        'workflow_permissions': None,
        'job_permissions': {},
        'uses_reusable_workflows': [],
        'needs_permissions': set(),
        'issues': []
    }
    
    # Check workflow-level permissions
    if 'permissions' in workflow:
        result['workflow_permissions'] = workflow['permissions']
    
    # Check each job
    jobs = workflow.get('jobs', {})
    for job_name, job_config in jobs.items():
        # Check job-level permissions
        if 'permissions' in job_config:
            result['job_permissions'][job_name] = job_config['permissions']
        
        # Check if job uses reusable workflow
        if 'uses' in job_config:
            result['uses_reusable_workflows'].append({
                'job': job_name,
                'workflow': job_config['uses']
            })
        
        # Analyze what permissions the job might need
        steps = job_config.get('steps', [])
        for step in steps:
            if 'uses' in step:
                action = step['uses']
                # Common actions that need specific permissions
                if 'github/codeql-action/upload-sarif' in action:
                    result['needs_permissions'].add('security-events: write')
                if 'docker/build-push-action' in action or 'docker/login-action' in action:
                    result['needs_permissions'].add('packages: write')
                if 'actions/create-release' in action:
                    result['needs_permissions'].add('contents: write')
                if 'peter-evans/create-pull-request' in action:
                    result['needs_permissions'].add('pull-requests: write')
                    result['needs_permissions'].add('contents: write')
                if 'github-actions-x/commit' in action:
                    result['needs_permissions'].add('contents: write')
            
            # Check for gh CLI usage that might need permissions
            if 'run' in step:
                run_script = step['run']
                if 'gh issue' in run_script:
                    result['needs_permissions'].add('issues: write')
                if 'gh pr' in run_script:
                    result['needs_permissions'].add('pull-requests: write')
                if 'gh release' in run_script:
                    result['needs_permissions'].add('contents: write')
    
    # Check for permission issues
    if result['uses_reusable_workflows'] and not result['workflow_permissions']:
        result['issues'].append('Uses reusable workflows but has no workflow-level permissions defined')
    
    if result['needs_permissions'] and not result['workflow_permissions'] and not result['job_permissions']:
        result['issues'].append(f'Likely needs permissions: {", ".join(result["needs_permissions"])} but none defined')
    
    return result

def main():
    workflow_dir = Path('.github/workflows')
    if not workflow_dir.exists():
        print("❌ .github/workflows directory not found!")
        return 1
    
    workflow_files = list(workflow_dir.glob('*.yml')) + list(workflow_dir.glob('*.yaml'))
    print(f"🔍 Analyzing {len(workflow_files)} workflow files...\n")
    
    all_results = []
    workflows_with_issues = []
    
    for workflow_file in sorted(workflow_files):
        try:
            result = analyze_workflow_permissions(workflow_file)
            all_results.append(result)
            
            if result['issues']:
                workflows_with_issues.append(result)
            
            # Print summary for each workflow
            print(f"📄 {result['file']}:")
            if result['workflow_permissions']:
                print(f"   ✓ Has workflow-level permissions")
            else:
                print(f"   ✗ No workflow-level permissions")
            
            if result['job_permissions']:
                print(f"   ✓ Has job-level permissions for: {', '.join(result['job_permissions'].keys())}")
            
            if result['uses_reusable_workflows']:
                print(f"   📦 Uses reusable workflows:")
                for use in result['uses_reusable_workflows']:
                    print(f"      - {use['job']}: {use['workflow']}")
            
            if result['needs_permissions']:
                print(f"   🔑 Likely needs: {', '.join(result['needs_permissions'])}")
            
            if result['issues']:
                print(f"   ⚠️  Issues:")
                for issue in result['issues']:
                    print(f"      - {issue}")
            
            print()
            
        except Exception as e:
            print(f"❌ Error analyzing {workflow_file}: {e}")
            print()
    
    # Generate recommendations
    print("=" * 60)
    print("📊 ANALYSIS SUMMARY")
    print("=" * 60)
    print(f"Total workflows analyzed: {len(all_results)}")
    print(f"Workflows with potential issues: {len(workflows_with_issues)}")
    
    if workflows_with_issues:
        print("\n🔧 RECOMMENDED FIXES:\n")
        for result in workflows_with_issues:
            print(f"File: {result['file']}")
            if result['needs_permissions'] and not result['workflow_permissions']:
                print(f"Add these permissions at workflow level:")
                print("```yaml")
                print("permissions:")
                for perm in sorted(result['needs_permissions']):
                    key, value = perm.split(': ')
                    print(f"  {key}: {value}")
                print("```")
            print()
    
    # Save detailed report
    report_path = 'workflow-permissions-report.json'
    with open(report_path, 'w') as f:
        json.dump(all_results, f, indent=2, default=list)
    print(f"\n📄 Detailed report saved to: {report_path}")
    
    return 0 if not workflows_with_issues else 1

if __name__ == '__main__':
    sys.exit(main())