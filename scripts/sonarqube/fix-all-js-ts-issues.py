#!/usr/bin/env python3
"""
Aggressively fix all JavaScript/TypeScript issues
"""

import json
import re
import os
from pathlib import Path
from typing import List, Dict

class AggressiveJSTSFixer:
    def __init__(self):
        self.fixed_count = 0
        self.files_fixed = set()
        
        # Load latest report
        script_dir = Path(__file__).parent
        reports_dir = script_dir / "sonar-reports"
        
        full_reports = list(reports_dir.glob("sonar_full_report_*.json"))
        if not full_reports:
            raise ValueError("No full reports found!")
        
        latest_report = max(full_reports, key=lambda p: p.stat().st_mtime)
        print(f"Loading issues from: {latest_report}")
        
        with open(latest_report, 'r') as f:
            data = json.load(f)
            all_issues = data.get('issues', [])
        
        # Filter JS/TS issues only (excluding HTML)
        self.issues = []
        for issue in all_issues:
            component = issue.get('component', '')
            if not ('index.html' in component) and any(ext in component for ext in ['.js', '.ts', '.tsx', '.jsx']):
                self.issues.append(issue)
        
        print(f"Found {len(self.issues)} JavaScript/TypeScript issues")
        
        # Group by file
        self.issues_by_file = {}
        for issue in self.issues:
            component = issue.get('component', '')
            file_path = component.replace('lsendel_zamaz-debate-mcp:', '') if component else ''
            
            if file_path not in self.issues_by_file:
                self.issues_by_file[file_path] = []
            self.issues_by_file[file_path].append(issue)
    
    def fix_file(self, file_path: str, issues: List[Dict]) -> bool:
        """Fix all issues in a file"""
        try:
            abs_path = Path.cwd() / file_path
            if not abs_path.exists():
                return False
            
            with open(abs_path, 'r', encoding='utf-8') as f:
                content = f.read()
                original_content = content
                lines = content.split('\n')
            
            # Sort issues by line number in reverse
            sorted_issues = sorted(issues, key=lambda x: x.get('line', 0), reverse=True)
            
            fixed_in_file = 0
            
            for issue in sorted_issues:
                rule = issue.get('rule', '')
                line_num = issue.get('line', 0)
                message = issue.get('message', '')
                
                if 0 < line_num <= len(lines):
                    line = lines[line_num - 1]
                    
                    # S3504: Naming convention (already handled)
                    if rule in ['javascript:S3504', 'typescript:S3504']:
                        continue
                    
                    # S1128: Unused imports (already handled)
                    elif rule == 'typescript:S1128':
                        continue
                    
                    # S3863: Duplicate imports (already handled)
                    elif rule == 'typescript:S3863':
                        continue
                    
                    # S1119: Remove unnecessary labels
                    elif rule == 'javascript:S1119':
                        if ':' in line:
                            lines[line_num - 1] = re.sub(r'^\s*\w+:\s*', '', line)
                            fixed_in_file += 1
                    
                    # S128: Break or return in switch cases
                    elif rule == 'javascript:S128':
                        # Add break statement
                        indent = len(line) - len(line.lstrip())
                        if 'case' in line and line_num < len(lines) - 1:
                            # Find end of case block
                            for i in range(line_num, len(lines)):
                                next_line = lines[i]
                                if 'case' in next_line or 'default' in next_line or '}' in next_line:
                                    if not any(kw in lines[i-1] for kw in ['break', 'return', 'throw']):
                                        lines.insert(i, ' ' * (indent + 2) + 'break;')
                                        fixed_in_file += 1
                                    break
                    
                    # S101: Rename classes to PascalCase
                    elif rule == 'javascript:S101':
                        class_match = re.search(r'class\s+(\w+)', line)
                        if class_match:
                            old_name = class_match.group(1)
                            new_name = self._to_pascal_case(old_name)
                            if old_name != new_name:
                                # Replace in current line
                                lines[line_num - 1] = line.replace(f'class {old_name}', f'class {new_name}')
                                # Replace in next 50 lines (constructor, references)
                                for i in range(line_num, min(line_num + 50, len(lines))):
                                    lines[i] = re.sub(rf'\b{old_name}\b', new_name, lines[i])
                                fixed_in_file += 1
                    
                    # S1264: Remove unnecessary increment in for loop
                    elif rule == 'javascript:S1264':
                        if 'for' in line and '++' in line:
                            # This is complex, skip
                            pass
                    
                    # S2430: Remove unnecessary 'new' with Symbol
                    elif rule == 'javascript:S2430':
                        if 'new Symbol' in line:
                            lines[line_num - 1] = line.replace('new Symbol', 'Symbol')
                            fixed_in_file += 1
                    
                    # S6661: Remove unnecessary template literals
                    elif rule == 'javascript:S6661':
                        # Convert `string` to 'string' when no interpolation
                        if '`' in line and not '${' in line:
                            # Extract template literal content
                            template_match = re.search(r'`([^`]*)`', line)
                            if template_match:
                                content_str = template_match.group(1)
                                if '"' not in content_str:
                                    lines[line_num - 1] = line.replace(f'`{content_str}`', f'"{content_str}"')
                                else:
                                    lines[line_num - 1] = line.replace(f'`{content_str}`', f"'{content_str}'")
                                fixed_in_file += 1
                    
                    # S1439: Remove labels that are only used once
                    elif rule == 'javascript:S1439':
                        label_match = re.search(r"'(\w+)' is only used once", message)
                        if label_match:
                            label = label_match.group(1)
                            # Remove label declaration
                            if f'{label}:' in line:
                                lines[line_num - 1] = line.replace(f'{label}:', '').strip()
                                # Remove break label usage
                                for i in range(line_num, min(line_num + 20, len(lines))):
                                    if f'break {label}' in lines[i]:
                                        lines[i] = lines[i].replace(f'break {label}', 'break')
                                fixed_in_file += 1
                    
                    # S2234: Parameters in wrong order
                    elif rule in ['javascript:S2234', 'typescript:S2234']:
                        # This requires understanding function signatures, skip
                        pass
                    
                    # S6853: Correct imperative verbs in JSDoc
                    elif rule == 'typescript:S6853':
                        if '@' in line and any(word in line for word in ['Gets', 'Sets', 'Returns', 'Checks']):
                            lines[line_num - 1] = line.replace('Gets', 'Get').replace('Sets', 'Set').replace('Returns', 'Return').replace('Checks', 'Check')
                            fixed_in_file += 1
                    
                    # S2933: Make readonly
                    elif rule == 'typescript:S2933':
                        if 'private' in line and not 'readonly' in line:
                            lines[line_num - 1] = line.replace('private', 'private readonly')
                            fixed_in_file += 1
                    
                    # S6836: Remove unnecessary 'await'
                    elif rule == 'typescript:S6836':
                        if 'return await' in line:
                            lines[line_num - 1] = line.replace('return await', 'return')
                            fixed_in_file += 1
            
            # Write back if changed
            new_content = '\n'.join(lines)
            if new_content != original_content:
                with open(abs_path, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                self.fixed_count += fixed_in_file
                return True
            
        except Exception as e:
            print(f"Error processing {file_path}: {e}")
        
        return False
    
    def _to_pascal_case(self, name: str) -> str:
        """Convert name to PascalCase"""
        # Handle snake_case
        if '_' in name:
            parts = name.split('_')
            return ''.join(part.capitalize() for part in parts)
        # Handle camelCase
        elif name and name[0].islower():
            return name[0].upper() + name[1:]
        return name
    
    def run(self):
        """Fix all JS/TS issues"""
        
        # Sort files by issue count
        file_counts = [(f, len(issues)) for f, issues in self.issues_by_file.items()]
        file_counts.sort(key=lambda x: x[1], reverse=True)
        
        print(f"\nProcessing {len(file_counts)} files...")
        
        for file_path, count in file_counts:
            if count == 0:
                continue
            
            issues = self.issues_by_file[file_path]
            
            if self.fix_file(file_path, issues):
                self.files_fixed.add(file_path)
                print(f"Fixed issues in {file_path}")
        
        print(f"\n=== SUMMARY ===")
        print(f"Total issues fixed: {self.fixed_count}")
        print(f"Files modified: {len(self.files_fixed)}")
        
        return self.fixed_count

def main():
    fixer = AggressiveJSTSFixer()
    fixer.run()

if __name__ == "__main__":
    main()