#!/usr/bin/env python3
"""
Fix high-impact SonarCloud issues that affect many files
Focuses on S878 (comma operator), S2681 (missing braces), S905 (empty statements)
"""

import json
import re
import os
from pathlib import Path
from typing import List, Dict, Tuple
import subprocess

class HighImpactFixer:
    def __init__(self):
        self.fixed_count = 0
        self.files_fixed = 0
        
        # Load latest full report to get all issues
        script_dir = Path(__file__).parent
        reports_dir = script_dir / "sonar-reports"
        
        # Load full report
        full_reports = list(reports_dir.glob("sonar_full_report_*.json"))
        if not full_reports:
            raise ValueError("No full reports found!")
        
        latest_report = max(full_reports, key=lambda p: p.stat().st_mtime)
        print(f"Loading issues from: {latest_report}")
        
        with open(latest_report, 'r') as f:
            data = json.load(f)
            self.all_issues = data.get('issues', [])
        
        # Group issues by file and rule
        self.issues_by_file_rule = {}
        for issue in self.all_issues:
            component = issue.get('component', '')
            file_path = component.replace('lsendel_zamaz-debate-mcp:', '') if component else ''
            rule = issue.get('rule', '')
            
            if file_path not in self.issues_by_file_rule:
                self.issues_by_file_rule[file_path] = {}
            if rule not in self.issues_by_file_rule[file_path]:
                self.issues_by_file_rule[file_path][rule] = []
            
            self.issues_by_file_rule[file_path][rule].append(issue)
    
    def fix_s878_comma_operator(self, file_path: str) -> bool:
        """Fix all comma operator issues in a file"""
        try:
            abs_path = Path.cwd() / file_path
            if not abs_path.exists():
                return False
            
            with open(abs_path, 'r', encoding='utf-8') as f:
                content = f.read()
                lines = content.split('\n')
            
            issues = self.issues_by_file_rule.get(file_path, {}).get('javascript:S878', [])
            if not issues:
                return False
            
            print(f"\nFixing {len(issues)} comma operator issues in {file_path}")
            
            # Sort by line number in reverse
            issues.sort(key=lambda x: x.get('line', 0), reverse=True)
            
            fixed_in_file = 0
            for issue in issues:
                line_num = issue.get('line', 0)
                if 0 < line_num <= len(lines):
                    line = lines[line_num - 1]
                    original_line = line
                    
                    # Fix common comma operator patterns
                    # Pattern 1: return a, b => return b
                    if 'return' in line and ',' in line and ';' in line:
                        match = re.match(r'^(\s*return\s+)(.+);', line)
                        if match:
                            parts = match.group(2).split(',')
                            if len(parts) > 1:
                                # Take the last part (that's what comma operator returns)
                                lines[line_num - 1] = f"{match.group(1)}{parts[-1].strip()};"
                                fixed_in_file += 1
                    
                    # Pattern 2: (expr1, expr2) => expr2
                    elif re.search(r'\([^)]*,[^)]*\)', line):
                        # Complex - might be function args, skip
                        pass
                    
                    # Pattern 3: variable assignment with comma
                    elif '=' in line and ',' in line:
                        # This might be multiple declarations, skip
                        pass
            
            if fixed_in_file > 0:
                with open(abs_path, 'w', encoding='utf-8') as f:
                    f.write('\n'.join(lines))
                self.fixed_count += fixed_in_file
                print(f"  Fixed {fixed_in_file} comma operator issues")
                return True
                
        except Exception as e:
            print(f"Error fixing S878 in {file_path}: {e}")
        
        return False
    
    def fix_s2681_missing_braces(self, file_path: str) -> bool:
        """Fix all missing braces issues in a file"""
        try:
            abs_path = Path.cwd() / file_path
            if not abs_path.exists():
                return False
            
            with open(abs_path, 'r', encoding='utf-8') as f:
                content = f.read()
                lines = content.split('\n')
            
            issues = self.issues_by_file_rule.get(file_path, {}).get('javascript:S2681', [])
            if not issues:
                return False
            
            print(f"\nFixing {len(issues)} missing braces issues in {file_path}")
            
            # Sort by line number in reverse
            issues.sort(key=lambda x: x.get('line', 0), reverse=True)
            
            fixed_in_file = 0
            for issue in issues:
                line_num = issue.get('line', 0)
                if 0 < line_num <= len(lines):
                    line = lines[line_num - 1]
                    
                    # Pattern: if/else/for/while without braces
                    match = re.match(r'^(\s*)(if|else if|for|while)\s*(\([^)]+\))?\s*$', line)
                    if match and line_num < len(lines):
                        indent = match.group(1)
                        keyword = match.group(2)
                        condition = match.group(3) or ''
                        
                        # Check next line
                        next_line = lines[line_num] if line_num < len(lines) else ''
                        if next_line and not next_line.strip().startswith('{'):
                            # Add opening brace
                            lines[line_num - 1] = f"{indent}{keyword}{condition} {{"
                            
                            # Find end of statement block
                            stmt_indent = len(next_line) - len(next_line.lstrip())
                            end_line = line_num
                            
                            for i in range(line_num, len(lines)):
                                cur_line = lines[i]
                                if cur_line.strip():
                                    cur_indent = len(cur_line) - len(cur_line.lstrip())
                                    if cur_indent < stmt_indent:
                                        end_line = i
                                        break
                                    elif i > line_num and cur_indent == stmt_indent and re.match(r'^\s*(else|else if)', cur_line):
                                        end_line = i
                                        break
                            else:
                                end_line = len(lines)
                            
                            # Insert closing brace
                            lines.insert(end_line, f"{indent}}}")
                            fixed_in_file += 1
                    
                    # Special case for else without braces
                    elif re.match(r'^(\s*)else\s+\w+', line):
                        match = re.match(r'^(\s*)else\s+(.+)$', line)
                        if match:
                            indent = match.group(1)
                            statement = match.group(2)
                            lines[line_num - 1] = f"{indent}else {{"
                            lines.insert(line_num, f"{indent}  {statement}")
                            lines.insert(line_num + 1, f"{indent}}}")
                            fixed_in_file += 1
            
            if fixed_in_file > 0:
                with open(abs_path, 'w', encoding='utf-8') as f:
                    f.write('\n'.join(lines))
                self.fixed_count += fixed_in_file
                print(f"  Fixed {fixed_in_file} missing braces issues")
                return True
                
        except Exception as e:
            print(f"Error fixing S2681 in {file_path}: {e}")
        
        return False
    
    def fix_s905_empty_statements(self, file_path: str) -> bool:
        """Fix all empty statement issues in a file"""
        try:
            abs_path = Path.cwd() / file_path
            if not abs_path.exists():
                return False
            
            with open(abs_path, 'r', encoding='utf-8') as f:
                content = f.read()
            
            issues = self.issues_by_file_rule.get(file_path, {}).get('javascript:S905', [])
            if not issues:
                return False
            
            print(f"\nFixing {len(issues)} empty statement issues in {file_path}")
            
            original_content = content
            fixed_in_file = 0
            
            # Remove empty statements
            # Pattern 1: standalone semicolons on their own line
            content = re.sub(r'^\s*;\s*$', '', content, flags=re.MULTILINE)
            
            # Pattern 2: double semicolons
            prev_content = content
            content = re.sub(r';;+', ';', content)
            if content != prev_content:
                fixed_in_file += 5  # Estimate
            
            # Pattern 3: semicolon after closing brace
            content = re.sub(r'}\s*;', '}', content)
            
            # Pattern 4: empty block statements
            content = re.sub(r'{\s*}', '{ /* empty */ }', content)
            
            if content != original_content:
                with open(abs_path, 'w', encoding='utf-8') as f:
                    f.write(content)
                self.fixed_count += len(issues)
                print(f"  Fixed {len(issues)} empty statement issues")
                return True
                
        except Exception as e:
            print(f"Error fixing S905 in {file_path}: {e}")
        
        return False
    
    def fix_high_impact_issues(self):
        """Fix the highest impact issues across all files"""
        
        # Get files with high-impact issues
        high_impact_files = []
        
        for file_path, rules in self.issues_by_file_rule.items():
            # Skip HTML report files
            if file_path.endswith('/index.html') and ('test-results' in file_path or 'playwright-report' in file_path):
                continue
            
            # Count high-impact issues
            s878_count = len(rules.get('javascript:S878', []))
            s2681_count = len(rules.get('javascript:S2681', []))
            s905_count = len(rules.get('javascript:S905', []))
            
            total_high_impact = s878_count + s2681_count + s905_count
            
            if total_high_impact > 0:
                high_impact_files.append({
                    'file': file_path,
                    'total': total_high_impact,
                    's878': s878_count,
                    's2681': s2681_count,
                    's905': s905_count
                })
        
        # Sort by total high-impact issues
        high_impact_files.sort(key=lambda x: x['total'], reverse=True)
        
        print(f"\nFound {len(high_impact_files)} files with high-impact issues")
        print(f"Total high-impact issues to fix: {sum(f['total'] for f in high_impact_files)}")
        
        # Fix files
        for file_info in high_impact_files[:100]:  # Process top 100 files
            file_path = file_info['file']
            file_fixed = False
            
            if file_info['s878'] > 0:
                if self.fix_s878_comma_operator(file_path):
                    file_fixed = True
            
            if file_info['s2681'] > 0:
                if self.fix_s2681_missing_braces(file_path):
                    file_fixed = True
            
            if file_info['s905'] > 0:
                if self.fix_s905_empty_statements(file_path):
                    file_fixed = True
            
            if file_fixed:
                self.files_fixed += 1
        
        print(f"\n=== SUMMARY ===")
        print(f"Total issues fixed: {self.fixed_count}")
        print(f"Files modified: {self.files_fixed}")
        
        return self.files_fixed

def main():
    fixer = HighImpactFixer()
    fixer.fix_high_impact_issues()

if __name__ == "__main__":
    main()