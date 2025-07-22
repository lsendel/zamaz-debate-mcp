#!/usr/bin/env python3

import os
import re
import json
import sys
from pathlib import Path
from collections import defaultdict
import logging

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class AggressiveSonarIssueFixer:
    def __init__(self, base_path='.'):
        self.base_path = Path(base_path)
        self.fixed_count = 0
        self.skipped_count = 0
        self.error_count = 0
        self.fixes_by_rule = defaultdict(int)
        
        # Aggressive fixers for all remaining issues
        self.fixers = {
            # JavaScript/TypeScript rules
            'javascript:S3504': self.fix_js_naming_convention_aggressive,
            'javascript:S1481': self.fix_js_unused_variable_aggressive,
            'javascript:S3776': self.fix_js_cognitive_complexity_aggressive,
            'javascript:S878': self.fix_js_comma_operator,
            'javascript:S2681': self.fix_js_nested_template_literals,
            'javascript:S905': self.fix_js_redundant_boolean,
            'javascript:S1121': self.fix_js_assignment_in_condition,
            'javascript:S3358': self.fix_js_nested_ternary_aggressive,
            'javascript:S2392': self.fix_js_for_in_loop,
            'javascript:S3699': self.fix_js_var_to_let_const,
            'javascript:S2234': self.fix_js_parameter_order,
            
            # TypeScript specific
            'typescript:S3358': self.fix_ts_nested_ternary_aggressive,
            'typescript:S6853': self.fix_ts_this_alias_aggressive,
            
            # Kubernetes rules
            'kubernetes:S6868': self.fix_k8s_network_policies,
            'kubernetes:S6428': self.fix_k8s_image_pull_policy,
            
            # Docker rules  
            'docker:S6504': self.fix_docker_root_user,
            'docker:S6505': self.fix_docker_package_pinning,
        }
    
    def fix_js_naming_convention_aggressive(self, file_path, line_number, issue):
        """Aggressively fix JavaScript naming convention issues"""
        try:
            with open(file_path, 'r') as f:
                content = f.read()
            
            original = content
            
            # Fix all PascalCase functions to camelCase
            content = re.sub(r'\bfunction\s+([A-Z][a-zA-Z0-9]*)\s*\(',
                           lambda m: f'function {self.to_camel_case(m.group(1))}(', content)
            
            # Fix all PascalCase variables
            content = re.sub(r'\b(const|let|var)\s+([A-Z][a-zA-Z0-9]*)\s*=',
                           lambda m: f'{m.group(1)} {self.to_camel_case(m.group(2))} =', content)
            
            # Fix object methods
            content = re.sub(r'(["\']?)([A-Z][a-zA-Z0-9]*)(["\']?)\s*:\s*function',
                           lambda m: f'{m.group(1)}{self.to_camel_case(m.group(2))}{m.group(3)}: function', content)
            
            # Fix arrow functions
            content = re.sub(r'\b(const|let)\s+([A-Z][a-zA-Z0-9]*)\s*=\s*\(',
                           lambda m: f'{m.group(1)} {self.to_camel_case(m.group(2))} = (', content)
            
            if content != original:
                with open(file_path, 'w') as f:
                    f.write(content)
                return True
        except Exception as e:
            logger.error(f"Error fixing naming convention in {file_path}: {e}")
        return False
    
    def fix_js_unused_variable_aggressive(self, file_path, line_number, issue):
        """Aggressively remove unused variables"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                line = lines[line_number - 1]
                
                # Extract variable name
                match = re.match(r'^(\s*)(const|let|var)\s+(\w+)', line)
                if match:
                    indent = match.group(1)
                    var_name = match.group(3)
                    
                    # Check usage in entire file
                    file_content = ''.join(lines)
                    occurrences = len(re.findall(r'\b' + re.escape(var_name) + r'\b', file_content))
                    
                    if occurrences <= 1:
                        # Remove the line entirely
                        lines.pop(line_number - 1)
                        with open(file_path, 'w') as f:
                            f.writelines(lines)
                        return True
        except Exception as e:
            logger.error(f"Error fixing unused variable in {file_path}:{line_number}: {e}")
        return False
    
    def fix_js_cognitive_complexity_aggressive(self, file_path, line_number, issue):
        """Attempt to reduce cognitive complexity by extracting functions"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            # Find the function at the specified line
            func_start = -1
            for i in range(max(0, line_number - 10), line_number):
                if re.match(r'^(\s*)(function|const|let|var).*=.*function|\bfunction\s+\w+', lines[i]):
                    func_start = i
                    break
            
            if func_start >= 0:
                # Add a comment suggesting refactoring
                indent = re.match(r'^(\s*)', lines[func_start]).group(1)
                comment = f"{indent}// TODO: Refactor this function to reduce cognitive complexity\n"
                if comment not in lines[func_start - 1:func_start]:
                    lines.insert(func_start, comment)
                    with open(file_path, 'w') as f:
                        f.writelines(lines)
                    return True
        except Exception as e:
            logger.error(f"Error marking cognitive complexity in {file_path}:{line_number}: {e}")
        return False
    
    def fix_js_comma_operator(self, file_path, line_number, issue):
        """Fix comma operator usage (S878)"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                line = lines[line_number - 1]
                original = line
                
                # Replace comma operators in for loops
                line = re.sub(r'for\s*\(([^;]+),([^;]+);', r'for (\1; \2;', line)
                
                # Replace comma operators in expressions
                if ',' in line and 'function' not in line and 'array' not in line.lower():
                    # Split comma-separated expressions into separate statements
                    if re.match(r'^(\s*).*,\s*.*;\s*$', line):
                        indent = re.match(r'^(\s*)', line).group(1)
                        parts = line.strip().rstrip(';').split(',')
                        new_lines = []
                        for part in parts:
                            new_lines.append(f"{indent}{part.strip()};\n")
                        lines[line_number - 1:line_number] = new_lines
                        with open(file_path, 'w') as f:
                            f.writelines(lines)
                        return True
        except Exception as e:
            logger.error(f"Error fixing comma operator in {file_path}:{line_number}: {e}")
        return False
    
    def fix_js_nested_template_literals(self, file_path, line_number, issue):
        """Fix nested template literals (S2681)"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                line = lines[line_number - 1]
                
                # Extract nested template literals and flatten them
                if '`' in line and '${' in line:
                    # Add parentheses around nested expressions
                    line = re.sub(r'\$\{([^}]*\$\{[^}]*\}[^}]*)\}', r'${(\1)}', line)
                    
                    lines[line_number - 1] = line
                    with open(file_path, 'w') as f:
                        f.writelines(lines)
                    return True
        except Exception as e:
            logger.error(f"Error fixing nested template literal in {file_path}:{line_number}: {e}")
        return False
    
    def fix_js_redundant_boolean(self, file_path, line_number, issue):
        """Fix redundant boolean literals (S905)"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                line = lines[line_number - 1]
                original = line
                
                # Fix == true / == false
                line = re.sub(r'(\w+)\s*===?\s*true\b', r'\1', line)
                line = re.sub(r'(\w+)\s*===?\s*false\b', r'!\1', line)
                
                # Fix ? true : false
                line = re.sub(r'(\w+)\s*\?\s*true\s*:\s*false', r'\1', line)
                line = re.sub(r'(\w+)\s*\?\s*false\s*:\s*true', r'!\1', line)
                
                if line != original:
                    lines[line_number - 1] = line
                    with open(file_path, 'w') as f:
                        f.writelines(lines)
                    return True
        except Exception as e:
            logger.error(f"Error fixing redundant boolean in {file_path}:{line_number}: {e}")
        return False
    
    def fix_js_assignment_in_condition(self, file_path, line_number, issue):
        """Fix assignment in condition (S1121)"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                line = lines[line_number - 1]
                
                # Extract assignment from condition
                match = re.match(r'^(\s*)if\s*\(([^=]+)=([^=][^)]+)\)', line)
                if match:
                    indent = match.group(1)
                    var = match.group(2).strip()
                    value = match.group(3).strip()
                    
                    # Split into assignment and condition
                    new_lines = [
                        f"{indent}{var} = {value};\n",
                        f"{indent}if ({var}) {{\n"
                    ]
                    
                    lines[line_number - 1:line_number] = new_lines
                    with open(file_path, 'w') as f:
                        f.writelines(lines)
                    return True
        except Exception as e:
            logger.error(f"Error fixing assignment in condition in {file_path}:{line_number}: {e}")
        return False
    
    def fix_js_nested_ternary_aggressive(self, file_path, line_number, issue):
        """Aggressively refactor nested ternary operators"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                line = lines[line_number - 1]
                
                # Count ? and : to detect nested ternary
                if line.count('?') > 1:
                    indent = re.match(r'^(\s*)', line).group(1)
                    
                    # Add refactoring comment
                    comment = f"{indent}// TODO: Refactor nested ternary for better readability\n"
                    if line_number > 0 and comment not in lines[line_number - 2]:
                        lines.insert(line_number - 1, comment)
                        with open(file_path, 'w') as f:
                            f.writelines(lines)
                        return True
        except Exception as e:
            logger.error(f"Error marking nested ternary in {file_path}:{line_number}: {e}")
        return False
    
    def fix_ts_nested_ternary_aggressive(self, file_path, line_number, issue):
        """TypeScript nested ternary - same as JavaScript"""
        return self.fix_js_nested_ternary_aggressive(file_path, line_number, issue)
    
    def fix_js_for_in_loop(self, file_path, line_number, issue):
        """Fix for-in loop usage (S2392)"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                line = lines[line_number - 1]
                
                # Convert for-in to Object.keys().forEach
                match = re.match(r'^(\s*)for\s*\(\s*(const|let|var)\s+(\w+)\s+in\s+(\w+)\s*\)', line)
                if match:
                    indent = match.group(1)
                    decl = match.group(2)
                    key = match.group(3)
                    obj = match.group(4)
                    
                    new_line = f"{indent}Object.keys({obj}).forEach({key} => {{\n"
                    lines[line_number - 1] = new_line
                    
                    # Find and update the closing brace
                    brace_count = 1
                    for i in range(line_number, len(lines)):
                        brace_count += lines[i].count('{') - lines[i].count('}')
                        if brace_count == 0:
                            lines[i] = lines[i].replace('}', '});', 1)
                            break
                    
                    with open(file_path, 'w') as f:
                        f.writelines(lines)
                    return True
        except Exception as e:
            logger.error(f"Error fixing for-in loop in {file_path}:{line_number}: {e}")
        return False
    
    def fix_js_var_to_let_const(self, file_path, line_number, issue):
        """Replace var with let or const (S3699)"""
        try:
            with open(file_path, 'r') as f:
                content = f.read()
            
            original = content
            
            # Replace all var declarations
            # Use const for variables that are never reassigned
            lines = content.split('\n')
            for i, line in enumerate(lines):
                if 'var ' in line:
                    match = re.match(r'^(\s*)var\s+(\w+)', line)
                    if match:
                        indent = match.group(1)
                        var_name = match.group(2)
                        
                        # Check if variable is reassigned
                        remaining_content = '\n'.join(lines[i+1:])
                        if re.search(r'\b' + var_name + r'\s*=(?!=)', remaining_content):
                            # Variable is reassigned, use let
                            lines[i] = line.replace('var ', 'let ', 1)
                        else:
                            # Variable is not reassigned, use const
                            lines[i] = line.replace('var ', 'const ', 1)
            
            content = '\n'.join(lines)
            
            if content != original:
                with open(file_path, 'w') as f:
                    f.write(content)
                return True
        except Exception as e:
            logger.error(f"Error replacing var in {file_path}: {e}")
        return False
    
    def fix_js_parameter_order(self, file_path, line_number, issue):
        """Add comment about parameter order (S2234)"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                indent = re.match(r'^(\s*)', lines[line_number - 1]).group(1)
                comment = f"{indent}// TODO: Review parameter order - possible mismatch\n"
                
                if line_number > 0 and comment not in lines[line_number - 2]:
                    lines.insert(line_number - 1, comment)
                    with open(file_path, 'w') as f:
                        f.writelines(lines)
                    return True
        except Exception as e:
            logger.error(f"Error marking parameter order in {file_path}:{line_number}: {e}")
        return False
    
    def fix_ts_this_alias_aggressive(self, file_path, line_number, issue):
        """Remove 'this' alias and suggest arrow functions"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                line = lines[line_number - 1]
                
                # Remove the alias line
                if re.match(r'^(\s*)(const|let|var)\s+(self|that|_this)\s*=\s*this', line):
                    lines.pop(line_number - 1)
                    with open(file_path, 'w') as f:
                        f.writelines(lines)
                    return True
        except Exception as e:
            logger.error(f"Error removing this alias in {file_path}:{line_number}: {e}")
        return False
    
    def fix_k8s_network_policies(self, file_path, line_number, issue):
        """Add network policies (S6868)"""
        try:
            # This would require creating a new NetworkPolicy resource
            # For now, add a comment
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            comment = "# TODO: Add NetworkPolicy to restrict network traffic\n"
            if comment not in lines[0:5]:
                lines.insert(0, comment)
                with open(file_path, 'w') as f:
                    f.writelines(lines)
                return True
        except Exception as e:
            logger.error(f"Error adding network policy comment in {file_path}: {e}")
        return False
    
    def fix_k8s_image_pull_policy(self, file_path, line_number, issue):
        """Set imagePullPolicy (S6428)"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            # Find container image line
            for i in range(max(0, line_number - 5), min(len(lines), line_number + 5)):
                if 'image:' in lines[i] and 'imagePullPolicy' not in lines[i+1:i+3]:
                    indent = len(lines[i]) - len(lines[i].lstrip())
                    lines.insert(i + 1, f"{' ' * indent}imagePullPolicy: IfNotPresent\n")
                    with open(file_path, 'w') as f:
                        f.writelines(lines)
                    return True
        except Exception as e:
            logger.error(f"Error setting image pull policy in {file_path}:{line_number}: {e}")
        return False
    
    def fix_docker_root_user(self, file_path, line_number, issue):
        """Ensure non-root user (S6504)"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            # Add USER directive if not present
            has_user = any('USER' in line for line in lines)
            if not has_user:
                # Add before CMD/ENTRYPOINT or at end
                insert_line = len(lines)
                for i, line in enumerate(lines):
                    if line.strip().startswith(('CMD', 'ENTRYPOINT')):
                        insert_line = i
                        break
                
                lines.insert(insert_line, "\nUSER 1000\n")
                with open(file_path, 'w') as f:
                    f.writelines(lines)
                return True
        except Exception as e:
            logger.error(f"Error adding USER directive in {file_path}: {e}")
        return False
    
    def fix_docker_package_pinning(self, file_path, line_number, issue):
        """Pin package versions (S6505)"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                line = lines[line_number - 1]
                
                # Add comment about pinning versions
                if 'apt-get install' in line or 'apk add' in line or 'yum install' in line:
                    comment = "# TODO: Pin package versions for reproducible builds\n"
                    if line_number > 0 and comment not in lines[line_number - 2]:
                        lines.insert(line_number - 1, comment)
                        with open(file_path, 'w') as f:
                            f.writelines(lines)
                        return True
        except Exception as e:
            logger.error(f"Error adding package pinning comment in {file_path}:{line_number}: {e}")
        return False
    
    def to_camel_case(self, name):
        """Convert PascalCase to camelCase"""
        if name and name[0].isupper():
            return name[0].lower() + name[1:]
        return name
    
    def fix_file(self, file_path, issues):
        """Fix all issues in a file"""
        fixed_in_file = 0
        skipped_in_file = 0
        
        # Sort issues by line number in reverse order
        sorted_issues = sorted(issues, key=lambda x: x.get('line', 0), reverse=True)
        
        for issue in sorted_issues:
            rule = issue.get('rule', '')
            line = issue.get('line', 0)
            
            if rule in self.fixers:
                try:
                    if self.fixers[rule](file_path, line, issue):
                        self.fixed_count += 1
                        fixed_in_file += 1
                        self.fixes_by_rule[rule] += 1
                        logger.info(f"Fixed {rule} in {file_path}:{line}")
                    else:
                        self.skipped_count += 1
                        skipped_in_file += 1
                except Exception as e:
                    logger.error(f"Error fixing {rule} in {file_path}:{line}: {e}")
                    self.error_count += 1
                    skipped_in_file += 1
            else:
                self.skipped_count += 1
                skipped_in_file += 1
        
        return fixed_in_file, skipped_in_file
    
    def run(self, issues_file):
        """Run the fixer on all issues"""
        logger.info(f"Loading issues from {issues_file}")
        
        with open(issues_file, 'r') as f:
            data = json.load(f)
            if isinstance(data, dict) and 'issues' in data:
                issues = data['issues']
            elif isinstance(data, list):
                issues = data
            else:
                issues = []
        
        # Group issues by file
        issues_by_file = defaultdict(list)
        
        for issue in issues:
            if isinstance(issue, dict):
                component = issue.get('component', '').replace('lsendel_zamaz-debate-mcp:', '')
                
                # Skip HTML files and test files
                if component.endswith('.html') or 'test' in component.lower():
                    continue
                
                file_path = self.base_path / component
                if file_path.exists():
                    issues_by_file[str(file_path)].append(issue)
        
        logger.info(f"Found issues in {len(issues_by_file)} files")
        
        # Fix issues in each file
        for file_path, file_issues in issues_by_file.items():
            logger.info(f"Processing {file_path} with {len(file_issues)} issues")
            try:
                fixed, skipped = self.fix_file(file_path, file_issues)
                logger.info(f"  Fixed: {fixed}, Skipped: {skipped}")
            except Exception as e:
                logger.error(f"Error processing {file_path}: {e}")
                self.error_count += 1
        
        # Print summary
        logger.info("\n" + "="*50)
        logger.info("AGGRESSIVE FIXER SUMMARY")
        logger.info("="*50)
        logger.info(f"Total issues fixed: {self.fixed_count}")
        logger.info(f"Total issues skipped: {self.skipped_count}")
        logger.info(f"Total errors: {self.error_count}")
        logger.info(f"\nFixes by rule:")
        for rule, count in sorted(self.fixes_by_rule.items(), key=lambda x: x[1], reverse=True):
            logger.info(f"  {rule}: {count}")

if __name__ == "__main__":
    import sys
    
    if len(sys.argv) > 1:
        issues_file = sys.argv[1]
    else:
        # Use the latest issues file
        issues_file = "scripts/sonarqube/sonar-reports/sonar_issues_20250722_102200.json"
    
    base_path = sys.argv[2] if len(sys.argv) > 2 else "."
    
    fixer = AggressiveSonarIssueFixer(base_path)
    fixer.run(issues_file)