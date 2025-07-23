#!/usr/bin/env python3
"""Test GitHub Actions workflow expressions for syntax errors."""

import re
import sys

def test_expression(expr, context=None):
    """Test if a GitHub Actions expression is valid."""
    # Remove ${{ and }}
    expr = expr.strip()
    if expr.startswith('${{') and expr.endswith('}}'):
        expr = expr[3:-2].strip()
    
    # Common issues to check
    issues = []
    
    # Check for failure() used incorrectly with &&
    if re.search(r'failure\(\)\s*&&\s*[\'"]', expr):
        issues.append("failure() returns boolean, cannot use && with string")
    
    # Check for unbalanced parentheses
    paren_count = expr.count('(') - expr.count(')')
    if paren_count != 0:
        issues.append(f"Unbalanced parentheses: {paren_count} extra {'(' if paren_count > 0 else ')'}")
    
    # Check for unbalanced quotes
    single_quotes = expr.count("'")
    if single_quotes % 2 != 0:
        issues.append("Unbalanced single quotes")
    
    double_quotes = expr.count('"')
    if double_quotes % 2 != 0:
        issues.append("Unbalanced double quotes")
    
    return issues

# Test expressions from security.yml
expressions = [
    {
        'line': 147,
        'expr': "${{ (needs.secrets-scan.result == 'failure' && 'critical') || (github.event_name == 'schedule' && 'high') || (github.ref == 'refs/heads/main' && 'high') || 'medium' }}",
        'desc': 'severity'
    },
    {
        'line': 151,
        'expr': "${{ format('workflow-failure,security,compliance,{0}{1}{2}{3}{4}', (needs.secrets-scan.result == 'failure' && 'secrets-exposed,critical,' || ''), (needs.java-security.result == 'failure' && 'java-vulnerability,' || ''), (needs.frontend-security.result == 'failure' && 'frontend-vulnerability,npm,' || ''), (needs.semgrep.result == 'failure' && 'code-vulnerability,' || ''), (github.event_name == 'schedule' && 'scheduled-scan' || github.ref_name)) }}",
        'desc': 'labels'
    }
]

print("Testing GitHub Actions expressions...")
print("=" * 50)

has_errors = False
for expr_info in expressions:
    print(f"\nLine {expr_info['line']} ({expr_info['desc']}):")
    print(f"Expression: {expr_info['expr'][:100]}...")
    
    issues = test_expression(expr_info['expr'])
    if issues:
        has_errors = True
        print("  ❌ Issues found:")
        for issue in issues:
            print(f"     - {issue}")
    else:
        print("  ✅ No obvious syntax issues")

if has_errors:
    print("\n❌ Errors found in expressions")
    sys.exit(1)
else:
    print("\n✅ All expressions appear valid")
    sys.exit(0)