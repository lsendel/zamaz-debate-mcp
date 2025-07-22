#!/usr/bin/env python3
"""
Analyze top SonarCloud issues and create a fixing strategy
"""

import json
from pathlib import Path
from collections import Counter

# Map of rule IDs to descriptions and fix strategies
RULE_DESCRIPTIONS = {
    "javascript:S878": {
        "name": "Comma operator should not be used",
        "description": "The comma operator evaluates its operands and returns the value of the last. It's often misused or unclear.",
        "fix": "Replace comma operator with separate statements or proper syntax"
    },
    "javascript:S2681": {
        "name": "Multiline blocks should be enclosed in curly braces",
        "description": "While optional, curly braces improve code clarity",
        "fix": "Add curly braces to if/else/for/while statements"
    },
    "javascript:S905": {
        "name": "Non-empty statements should change control flow or have at least one side-effect",
        "description": "Statements that don't do anything should be removed",
        "fix": "Remove empty statements or add meaningful code"
    },
    "javascript:S1121": {
        "name": "Assignments should not be made from within sub-expressions",
        "description": "Assignments inside expressions can be confusing and error-prone",
        "fix": "Extract assignments to separate statements"
    },
    "javascript:S3358": {
        "name": "Ternary operators should not be nested",
        "description": "Nested ternary operators are hard to read",
        "fix": "Replace with if-else statements or extract to functions"
    },
    "javascript:S3504": {
        "name": "Variable, property and parameter names should comply with a naming convention",
        "description": "Consistent naming improves readability",
        "fix": "Rename to follow camelCase convention"
    },
    "javascript:S2392": {
        "name": "Variables should be defined in the proper scope",
        "description": "Variables should be declared in the narrowest scope possible",
        "fix": "Move variable declarations closer to where they're used"
    },
    "javascript:S3699": {
        "name": "Use of 'var' should be avoided",
        "description": "'let' or 'const' should be used instead of 'var'",
        "fix": "Replace 'var' with 'let' or 'const'"
    },
    "javascript:S3776": {
        "name": "Cognitive Complexity of functions should not be too high",
        "description": "Complex functions are hard to understand and maintain",
        "fix": "Break down complex functions into smaller ones"
    },
    "javascript:S2234": {
        "name": "Parameters should be passed in the correct order",
        "description": "Arguments don't match parameter order",
        "fix": "Reorder arguments to match function signature"
    }
}

def analyze_issues():
    """Analyze downloaded issues and create fix strategy"""
    
    # Load the latest issues report
    reports_dir = Path("sonar-reports")
    
    # Find the latest JSON report
    json_files = list(reports_dir.glob("sonar_full_report_*.json"))
    if not json_files:
        print("No issue reports found!")
        return
    
    latest_report = max(json_files, key=lambda p: p.stat().st_mtime)
    print(f"Analyzing report: {latest_report}")
    
    with open(latest_report, 'r') as f:
        data = json.load(f)
    
    issues = data.get('issues', [])
    
    # Group issues by rule for batch fixing
    issues_by_rule = {}
    for issue in issues:
        rule = issue.get('rule', 'unknown')
        if rule not in issues_by_rule:
            issues_by_rule[rule] = []
        issues_by_rule[rule].append(issue)
    
    # Sort rules by issue count
    sorted_rules = sorted(issues_by_rule.items(), key=lambda x: len(x[1]), reverse=True)
    
    print(f"\nTotal issues: {len(issues)}")
    print(f"Total unique rules: {len(sorted_rules)}")
    
    # Create fixing strategy
    strategy = {
        "total_issues": len(issues),
        "rules_to_fix": [],
        "estimated_fixes": 0
    }
    
    print("\n=== TOP ISSUES TO FIX ===")
    
    # Focus on top 10 rules that can be auto-fixed
    auto_fixable_rules = [
        "javascript:S878",   # Comma operator
        "javascript:S2681",  # Curly braces
        "javascript:S905",   # Empty statements
        "javascript:S3699",  # var to let/const
        "javascript:S3504",  # Naming convention
        "javascript:S2392"   # Variable scope
    ]
    
    total_fixable = 0
    for rule, issues_list in sorted_rules[:20]:
        count = len(issues_list)
        is_fixable = rule in auto_fixable_rules
        
        if rule in RULE_DESCRIPTIONS:
            desc = RULE_DESCRIPTIONS[rule]
            print(f"\n{rule}: {count} issues")
            print(f"  Description: {desc['name']}")
            print(f"  Fix: {desc['fix']}")
            print(f"  Auto-fixable: {'✅ YES' if is_fixable else '❌ NO'}")
            
            if is_fixable:
                total_fixable += count
                strategy["rules_to_fix"].append({
                    "rule": rule,
                    "count": count,
                    "description": desc["name"],
                    "fix_strategy": desc["fix"]
                })
    
    strategy["estimated_fixes"] = total_fixable
    strategy["fix_percentage"] = (total_fixable / len(issues)) * 100
    
    # Save strategy
    strategy_file = reports_dir / "fix_strategy.json"
    with open(strategy_file, 'w') as f:
        json.dump(strategy, f, indent=2)
    
    print(f"\n=== FIXING STRATEGY ===")
    print(f"Total issues: {len(issues)}")
    print(f"Auto-fixable issues: {total_fixable}")
    print(f"Fix percentage: {strategy['fix_percentage']:.1f}%")
    print(f"\nStrategy saved to: {strategy_file}")
    
    # Calculate what we need to reach 98%
    current_quality = 100 - (len(issues) / 74392 * 100)  # issues per lines of code
    print(f"\nCurrent quality score: {current_quality:.1f}%")
    
    issues_to_fix = len(issues) - (74392 * 0.02)  # 2% allowed issues
    print(f"Issues to fix to reach 98%: {int(issues_to_fix)}")
    
    return strategy

if __name__ == "__main__":
    analyze_issues()