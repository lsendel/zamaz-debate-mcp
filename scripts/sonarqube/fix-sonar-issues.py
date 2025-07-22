#!/usr/bin/env python3
"""
Automatically fix common SonarCloud issues
"""

import json
import re
import os
from pathlib import Path
from typing import List, Dict, Tuple
import subprocess

class SonarIssueFixer:
    def __init__(self, dry_run=False):
        self.dry_run = dry_run
        self.fixed_count = 0
        self.skipped_count = 0
        self.error_count = 0
        
        # Load latest issues
        script_dir = Path(__file__).parent
        reports_dir = script_dir / "sonar-reports"
        json_files = list(reports_dir.glob("issues_by_file_*.json"))
        if not json_files:
            raise ValueError("No issue reports found!")
        
        latest_report = max(json_files, key=lambda p: p.stat().st_mtime)
        print(f"Loading issues from: {latest_report}")
        
        with open(latest_report, 'r') as f:
            self.issues_by_file = json.load(f)
        
        # Define auto-fixable rules
        self.fixers = {
            "javascript:S878": self.fix_comma_operator,
            "javascript:S2681": self.fix_missing_braces,
            "javascript:S905": self.fix_empty_statements,
            "javascript:S3699": self.fix_var_to_let_const,
            "javascript:S3504": self.fix_naming_convention,
            "javascript:S2392": self.fix_variable_scope,
            "typescript:S1128": self.fix_unused_imports,
            "typescript:S3863": self.fix_duplicate_imports,
            "javascript:S4138": self.fix_for_of_loop,
            "typescript:S6749": self.fix_redundant_fragment,
            "typescript:S6571": self.fix_any_in_union,
            "typescript:S2486": self.fix_empty_catch,
            "javascript:S2486": self.fix_empty_catch
        }
    
    def fix_file(self, file_path: str, issues: List[Dict]) -> bool:
        """Fix issues in a single file"""
        
        # Skip HTML report files
        if file_path.endswith('/index.html') and ('test-results' in file_path or 'playwright-report' in file_path):
            print(f"Skipping HTML report file: {file_path}")
            self.skipped_count += len(issues)
            return False
        
        # Get absolute path
        project_root = Path.cwd()
        abs_path = project_root / file_path
        
        if not abs_path.exists():
            print(f"File not found: {abs_path}")
            self.error_count += len(issues)
            return False
        
        print(f"\nProcessing {file_path} ({len(issues)} issues)")
        
        try:
            with open(abs_path, 'r', encoding='utf-8') as f:
                content = f.read()
                original_content = content
            
            # Sort issues by line number in reverse order to avoid line number changes
            sorted_issues = sorted(issues, key=lambda x: x.get('line', 0), reverse=True)
            
            for issue in sorted_issues:
                rule = issue.get('rule', '')
                if rule in self.fixers:
                    try:
                        content = self.fixers[rule](content, issue, file_path)
                        if content != original_content:
                            self.fixed_count += 1
                            print(f"  Fixed {rule} at line {issue.get('line', '?')}")
                    except Exception as e:
                        print(f"  Error fixing {rule}: {e}")
                        self.error_count += 1
                else:
                    self.skipped_count += 1
            
            # Write back if changed
            if content != original_content and not self.dry_run:
                with open(abs_path, 'w', encoding='utf-8') as f:
                    f.write(content)
                print(f"  Saved changes to {file_path}")
                return True
            
        except Exception as e:
            print(f"Error processing file {file_path}: {e}")
            self.error_count += len(issues)
            return False
        
        return False
    
    def fix_comma_operator(self, content: str, issue: Dict, file_path: str) -> str:
        """Fix comma operator usage (S878)"""
        line_num = issue.get('line', 0)
        if not line_num:
            return content
        
        lines = content.split('\n')
        if 0 < line_num <= len(lines):
            line = lines[line_num - 1]
            
            # Common patterns with comma operator
            # In for loops: for(i=0, j=0; i<10; i++, j++)
            if 'for' in line and ',' in line:
                # This is complex, skip for now
                self.skipped_count += 1
                return content
            
            # In variable declarations: var a = 1, b = 2;
            if re.match(r'^\s*(var|let|const)\s+\w+\s*=.*,.*=', line):
                # Split into separate declarations
                match = re.match(r'^(\s*)(var|let|const)\s+(.+);?\s*$', line)
                if match:
                    indent = match.group(1)
                    keyword = match.group(2)
                    declarations = match.group(3)
                    
                    # Split by comma but not within parentheses
                    parts = self._smart_split(declarations, ',')
                    new_lines = [f"{indent}{keyword} {part.strip()};" for part in parts]
                    lines[line_num - 1] = new_lines[0]
                    for i, new_line in enumerate(new_lines[1:], 1):
                        lines.insert(line_num - 1 + i, new_line)
                    
                    return '\n'.join(lines)
        
        return content
    
    def fix_missing_braces(self, content: str, issue: Dict, file_path: str) -> str:
        """Add missing curly braces (S2681)"""
        line_num = issue.get('line', 0)
        if not line_num:
            return content
        
        lines = content.split('\n')
        if 0 < line_num <= len(lines):
            line = lines[line_num - 1]
            
            # Check for if/else/for/while without braces
            match = re.match(r'^(\s*)(if|else if|else|for|while|do)\s*(\(.*\))?\s*$', line)
            if match and line_num < len(lines):
                indent = match.group(1)
                keyword = match.group(2)
                condition = match.group(3) or ''
                
                # Check if next line is indented (indicating a block)
                next_line = lines[line_num] if line_num < len(lines) else ''
                if next_line and not next_line.strip().startswith('{'):
                    # Add braces
                    lines[line_num - 1] = f"{indent}{keyword}{condition} {{"
                    
                    # Find the end of the block
                    block_indent = len(next_line) - len(next_line.lstrip())
                    end_line = line_num
                    
                    for i in range(line_num, len(lines)):
                        current_line = lines[i]
                        if current_line.strip() and (len(current_line) - len(current_line.lstrip())) < block_indent:
                            end_line = i
                            break
                    else:
                        end_line = len(lines)
                    
                    # Insert closing brace
                    lines.insert(end_line, f"{indent}}}")
                    
                    return '\n'.join(lines)
        
        return content
    
    def fix_empty_statements(self, content: str, issue: Dict, file_path: str) -> str:
        """Remove empty statements (S905)"""
        line_num = issue.get('line', 0)
        if not line_num:
            return content
        
        lines = content.split('\n')
        if 0 < line_num <= len(lines):
            line = lines[line_num - 1]
            
            # Remove standalone semicolons
            if line.strip() == ';':
                lines.pop(line_num - 1)
                return '\n'.join(lines)
            
            # Remove double semicolons
            lines[line_num - 1] = re.sub(r';;+', ';', line)
        
        return '\n'.join(lines)
    
    def fix_var_to_let_const(self, content: str, issue: Dict, file_path: str) -> str:
        """Replace var with let or const (S3699)"""
        line_num = issue.get('line', 0)
        if not line_num:
            return content
        
        lines = content.split('\n')
        if 0 < line_num <= len(lines):
            line = lines[line_num - 1]
            
            # Check if variable is reassigned later
            var_match = re.match(r'^(\s*)var\s+(\w+)', line)
            if var_match:
                indent = var_match.group(1)
                var_name = var_match.group(2)
                
                # Simple heuristic: use const if not reassigned in next 20 lines
                is_reassigned = False
                for i in range(line_num, min(line_num + 20, len(lines))):
                    if re.search(rf'\b{var_name}\s*=(?!=)', lines[i]):
                        is_reassigned = True
                        break
                
                replacement = 'let' if is_reassigned else 'const'
                lines[line_num - 1] = re.sub(r'\bvar\b', replacement, line, 1)
        
        return '\n'.join(lines)
    
    def fix_naming_convention(self, content: str, issue: Dict, file_path: str) -> str:
        """Fix naming conventions (S3504)"""
        # This is complex as it requires understanding the context
        # Skip for now
        self.skipped_count += 1
        return content
    
    def fix_variable_scope(self, content: str, issue: Dict, file_path: str) -> str:
        """Fix variable scope issues (S2392)"""
        # This requires understanding code flow, skip for now
        self.skipped_count += 1
        return content
    
    def fix_unused_imports(self, content: str, issue: Dict, file_path: str) -> str:
        """Remove unused imports (S1128)"""
        line_num = issue.get('line', 0)
        message = issue.get('message', '')
        
        if not line_num or 'Remove this unused import' not in message:
            return content
        
        # Extract the import name from message
        import_match = re.search(r"import of '(.+?)'", message)
        if not import_match:
            return content
        
        import_name = import_match.group(1)
        
        lines = content.split('\n')
        if 0 < line_num <= len(lines):
            line = lines[line_num - 1]
            
            # Handle different import patterns
            # import { X } from 'module'
            if f"{{{import_name}}}" in line or f", {import_name}" in line or f"{import_name}," in line:
                # Remove just this import
                line = re.sub(rf',\s*{import_name}\b', '', line)
                line = re.sub(rf'\b{import_name}\s*,', '', line)
                line = re.sub(rf'{{\s*{import_name}\s*}}', '{}', line)
                
                # If empty import, remove the line
                if re.match(r'^import\s*{}\s*from', line) or re.match(r'^import\s+from', line):
                    lines.pop(line_num - 1)
                else:
                    lines[line_num - 1] = line
            
            # import X from 'module'
            elif f"import {import_name} from" in line:
                lines.pop(line_num - 1)
        
        return '\n'.join(lines)
    
    def fix_duplicate_imports(self, content: str, issue: Dict, file_path: str) -> str:
        """Fix duplicate imports (S3863)"""
        message = issue.get('message', '')
        
        # Extract module name
        module_match = re.search(r"'(.+?)' imported multiple times", message)
        if not module_match:
            return content
        
        module_name = module_match.group(1)
        
        lines = content.split('\n')
        import_lines = []
        imports = []
        
        # Find all imports from this module
        for i, line in enumerate(lines):
            if f"from '{module_name}'" in line or f'from "{module_name}"' in line:
                import_lines.append(i)
                # Extract imported items
                match = re.match(r'^import\s+{(.+?)}\s+from', line)
                if match:
                    items = [item.strip() for item in match.group(1).split(',')]
                    imports.extend(items)
        
        if len(import_lines) > 1:
            # Combine all imports into the first line
            combined_imports = ', '.join(sorted(set(imports)))
            lines[import_lines[0]] = f"import {{ {combined_imports} }} from '{module_name}';"
            
            # Remove other import lines (in reverse order)
            for i in reversed(import_lines[1:]):
                lines.pop(i)
        
        return '\n'.join(lines)
    
    def fix_for_of_loop(self, content: str, issue: Dict, file_path: str) -> str:
        """Convert for loop to for-of loop (S4138)"""
        line_num = issue.get('line', 0)
        if not line_num:
            return content
        
        lines = content.split('\n')
        if 0 < line_num <= len(lines):
            line = lines[line_num - 1]
            
            # Match simple for loops: for (let i = 0; i < array.length; i++)
            match = re.match(r'^(\s*)for\s*\(\s*(let|var|const)\s+(\w+)\s*=\s*0;\s*\3\s*<\s*(\w+)\.length;\s*\3\+\+\s*\)', line)
            if match:
                indent = match.group(1)
                keyword = match.group(2)
                index_var = match.group(3)
                array_name = match.group(4)
                
                # Check if the loop uses array[i]
                # This is a simple check, may need refinement
                lines[line_num - 1] = f"{indent}for ({keyword} item of {array_name})"
                
                # Try to replace array[index] with item in the loop body
                # This is risky, so we'll skip complex cases
                if line_num < len(lines) - 1:
                    next_line = lines[line_num]
                    if f"{array_name}[{index_var}]" in next_line:
                        lines[line_num] = next_line.replace(f"{array_name}[{index_var}]", "item")
        
        return '\n'.join(lines)
    
    def fix_redundant_fragment(self, content: str, issue: Dict, file_path: str) -> str:
        """Remove redundant React fragments (S6749)"""
        line_num = issue.get('line', 0)
        if not line_num:
            return content
        
        lines = content.split('\n')
        if 0 < line_num <= len(lines):
            line = lines[line_num - 1]
            
            # Simple case: <>single element</>
            if '<>' in line and '</>' in line:
                # Count elements between fragment tags
                content_between = re.search(r'<>(.*?)</>', line)
                if content_between:
                    inner = content_between.group(1)
                    # If there's only one element, remove fragments
                    if inner.count('<') == 1:
                        lines[line_num - 1] = line.replace('<>', '').replace('</>', '')
        
        return '\n'.join(lines)
    
    def fix_any_in_union(self, content: str, issue: Dict, file_path: str) -> str:
        """Fix 'any' in union types (S6571)"""
        # This is complex as it requires type analysis
        self.skipped_count += 1
        return content
    
    def fix_empty_catch(self, content: str, issue: Dict, file_path: str) -> str:
        """Add handling to empty catch blocks (S2486)"""
        line_num = issue.get('line', 0)
        if not line_num:
            return content
        
        lines = content.split('\n')
        if 0 < line_num <= len(lines):
            line = lines[line_num - 1]
            
            # Check if this is a catch line
            if 'catch' in line:
                # Look for empty catch block
                if line_num < len(lines) - 1:
                    next_line = lines[line_num]
                    if next_line.strip() == '}':
                        # Add a console.error
                        indent = len(line) - len(line.lstrip()) + 2
                        lines.insert(line_num, ' ' * indent + "console.error('Error caught:', error);")
        
        return '\n'.join(lines)
    
    def _smart_split(self, text: str, delimiter: str) -> List[str]:
        """Split text by delimiter, but not within parentheses or quotes"""
        parts = []
        current = []
        paren_depth = 0
        in_quotes = False
        quote_char = None
        
        i = 0
        while i < len(text):
            char = text[i]
            
            if char in '"\'':
                if not in_quotes:
                    in_quotes = True
                    quote_char = char
                elif char == quote_char and (i == 0 or text[i-1] != '\\'):
                    in_quotes = False
                    quote_char = None
            
            if not in_quotes:
                if char == '(':
                    paren_depth += 1
                elif char == ')':
                    paren_depth -= 1
                elif char == delimiter and paren_depth == 0:
                    parts.append(''.join(current))
                    current = []
                    i += 1
                    continue
            
            current.append(char)
            i += 1
        
        if current:
            parts.append(''.join(current))
        
        return parts
    
    def run(self, max_files=10):
        """Run the fixer on files with most issues"""
        
        # Sort files by issue count, excluding HTML reports
        file_counts = []
        for file_path, issues in self.issues_by_file.items():
            if not (file_path.endswith('/index.html') and ('test-results' in file_path or 'playwright-report' in file_path)):
                file_counts.append((file_path, issues))
        
        file_counts.sort(key=lambda x: len(x[1]), reverse=True)
        
        print(f"Processing top {max_files} files with most fixable issues...")
        
        fixed_files = []
        for i, (file_path, issues) in enumerate(file_counts[:max_files]):
            # Filter to only fixable issues
            fixable_issues = [issue for issue in issues if issue.get('rule') in self.fixers]
            if fixable_issues:
                if self.fix_file(file_path, fixable_issues):
                    fixed_files.append(file_path)
        
        print(f"\n=== SUMMARY ===")
        print(f"Fixed issues: {self.fixed_count}")
        print(f"Skipped issues: {self.skipped_count}")
        print(f"Errors: {self.error_count}")
        print(f"Files modified: {len(fixed_files)}")
        
        return fixed_files

def main():
    import argparse
    
    parser = argparse.ArgumentParser(description='Fix SonarCloud issues automatically')
    parser.add_argument('--dry-run', action='store_true', help='Show what would be fixed without changing files')
    parser.add_argument('--max-files', type=int, default=10, help='Maximum number of files to process')
    
    args = parser.parse_args()
    
    fixer = SonarIssueFixer(dry_run=args.dry_run)
    fixed_files = fixer.run(max_files=args.max_files)
    
    if fixed_files and not args.dry_run:
        print(f"\nFixed files:")
        for file in fixed_files:
            print(f"  - {file}")

if __name__ == "__main__":
    main()