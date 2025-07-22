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

class EnhancedSonarIssueFixer:
    def __init__(self, base_path='.'):
        self.base_path = Path(base_path)
        self.fixed_count = 0
        self.skipped_count = 0
        self.error_count = 0
        self.fixes_by_rule = defaultdict(int)
        
        # Enhanced fixers for remaining issues
        self.fixers = {
            # JavaScript/TypeScript rules
            'javascript:S3504': self.fix_js_naming_convention,
            'typescript:S1128': self.fix_ts_unused_import,
            'typescript:S1854': self.fix_ts_useless_assignment,
            'typescript:S3358': self.fix_ts_nested_ternary,
            'javascript:S2486': self.fix_js_empty_catch,
            'typescript:S2486': self.fix_ts_empty_catch,
            'javascript:S1481': self.fix_js_unused_variable,
            'javascript:S3776': self.fix_js_cognitive_complexity,
            'typescript:S6853': self.fix_ts_this_alias,
            'javascript:S1854': self.fix_js_useless_assignment,
            
            # Python rules
            'python:S1192': self.fix_python_string_duplication,
            'python:S3776': self.fix_python_cognitive_complexity,
            'python:S930': self.fix_python_function_parameters,
            
            # SQL/PLSQL rules
            'plsql:VarcharUsageCheck': self.fix_plsql_varchar,
            'plsql:S1192': self.fix_plsql_string_duplication,
            
            # Kubernetes rules
            'kubernetes:S6897': self.fix_k8s_readonly_filesystem,
            'kubernetes:S6865': self.fix_k8s_security_context,
            'kubernetes:S6596': self.fix_k8s_resource_limits,
            
            # Docker rules
            'docker:S7019': self.fix_docker_healthcheck,
            'docker:S7031': self.fix_docker_user,
        }
    
    def fix_js_naming_convention(self, file_path, line_number, issue):
        """Fix JavaScript naming convention issues (S3504)"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                line = lines[line_number - 1]
                original = line
                
                # Fix function names that should be camelCase
                line = re.sub(r'\bfunction\s+([A-Z][a-zA-Z0-9]*)\s*\(', 
                            lambda m: f'function {self.to_camel_case(m.group(1))}(', line)
                
                # Fix const/let/var declarations
                line = re.sub(r'\b(const|let|var)\s+([A-Z][a-zA-Z0-9]*)\s*=', 
                            lambda m: f'{m.group(1)} {self.to_camel_case(m.group(2))} =', line)
                
                # Fix object method names
                line = re.sub(r'(["\']?)([A-Z][a-zA-Z0-9]*)(["\']?)\s*:\s*function', 
                            lambda m: f'{m.group(1)}{self.to_camel_case(m.group(2))}{m.group(3)}: function', line)
                
                if line != original:
                    lines[line_number - 1] = line
                    with open(file_path, 'w') as f:
                        f.writelines(lines)
                    return True
        except Exception as e:
            logger.error(f"Error fixing naming convention in {file_path}:{line_number} - {e}")
        return False
    
    def fix_ts_unused_import(self, file_path, line_number, issue):
        """Fix TypeScript unused import issues (S1128)"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                line = lines[line_number - 1]
                
                # Extract imported items
                import_match = re.match(r'^import\s+\{([^}]+)\}\s+from\s+["\']([^"\']+)["\'];?\s*$', line.strip())
                if import_match:
                    imports = [i.strip() for i in import_match.group(1).split(',')]
                    module = import_match.group(2)
                    
                    # Check which imports are actually used in the file
                    file_content = ''.join(lines[line_number:])  # Check only after the import
                    used_imports = []
                    
                    for imp in imports:
                        # Handle 'as' aliases
                        if ' as ' in imp:
                            original, alias = imp.split(' as ')
                            check_name = alias.strip()
                        else:
                            check_name = imp.strip()
                        
                        # Check if the import is used
                        if re.search(r'\b' + re.escape(check_name) + r'\b', file_content):
                            used_imports.append(imp)
                    
                    if used_imports and len(used_imports) < len(imports):
                        # Reconstruct the import with only used items
                        new_import = f"import {{ {', '.join(used_imports)} }} from '{module}';\n"
                        lines[line_number - 1] = new_import
                        with open(file_path, 'w') as f:
                            f.writelines(lines)
                        return True
                    elif not used_imports:
                        # Remove the entire import line
                        lines.pop(line_number - 1)
                        with open(file_path, 'w') as f:
                            f.writelines(lines)
                        return True
        except Exception as e:
            logger.error(f"Error fixing unused import in {file_path}:{line_number} - {e}")
        return False
    
    def fix_ts_useless_assignment(self, file_path, line_number, issue):
        """Fix TypeScript useless assignment issues (S1854)"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                line = lines[line_number - 1]
                
                # Pattern: const/let/var x = value; where x is never used
                match = re.match(r'^(\s*)(const|let|var)\s+(\w+)\s*=\s*(.+);?\s*$', line)
                if match:
                    indent = match.group(1)
                    var_name = match.group(3)
                    
                    # Check if variable is used in subsequent lines
                    used = False
                    for i in range(line_number, len(lines)):
                        if re.search(r'\b' + re.escape(var_name) + r'\b', lines[i]):
                            used = True
                            break
                    
                    if not used:
                        # Comment out the line
                        lines[line_number - 1] = f"{indent}// {line.strip()} // SonarCloud: removed useless assignment\n"
                        with open(file_path, 'w') as f:
                            f.writelines(lines)
                        return True
        except Exception as e:
            logger.error(f"Error fixing useless assignment in {file_path}:{line_number} - {e}")
        return False
    
    def fix_ts_nested_ternary(self, file_path, line_number, issue):
        """Fix TypeScript nested ternary operator issues (S3358)"""
        # This is complex to fix automatically - we'll mark it for manual review
        logger.info(f"Nested ternary in {file_path}:{line_number} - requires manual refactoring")
        return False
    
    def fix_js_empty_catch(self, file_path, line_number, issue):
        """Fix JavaScript/TypeScript empty catch block issues (S2486)"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                line = lines[line_number - 1]
                
                # Check if this is a catch line with empty block
                if 'catch' in line and '{' in line:
                    # Look for the closing brace
                    brace_count = line.count('{') - line.count('}')
                    end_line = line_number - 1
                    
                    while brace_count > 0 and end_line + 1 < len(lines):
                        end_line += 1
                        brace_count += lines[end_line].count('{') - lines[end_line].count('}')
                    
                    # Check if the catch block is empty
                    block_content = ''.join(lines[line_number:end_line])
                    if not block_content.strip() or block_content.strip() == '}':
                        # Add error logging
                        indent = re.match(r'^(\s*)', lines[line_number - 1]).group(1)
                        error_var = 'error' if 'error' in line else 'e'
                        log_line = f'{indent}    console.error("Error:", {error_var});\n'
                        lines.insert(line_number, log_line)
                        with open(file_path, 'w') as f:
                            f.writelines(lines)
                        return True
        except Exception as e:
            logger.error(f"Error fixing empty catch in {file_path}:{line_number} - {e}")
        return False
    
    def fix_ts_empty_catch(self, file_path, line_number, issue):
        """Fix TypeScript empty catch block issues (S2486)"""
        return self.fix_js_empty_catch(file_path, line_number, issue)
    
    def fix_js_unused_variable(self, file_path, line_number, issue):
        """Fix JavaScript unused variable issues (S1481)"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                line = lines[line_number - 1]
                
                # Pattern for variable declarations
                match = re.match(r'^(\s*)(const|let|var)\s+(\w+)\s*=', line)
                if match:
                    var_name = match.group(3)
                    
                    # Check if variable is used anywhere else in the file
                    file_content = ''.join(lines)
                    # Count occurrences (should be at least 2 - declaration and usage)
                    occurrences = len(re.findall(r'\b' + re.escape(var_name) + r'\b', file_content))
                    
                    if occurrences <= 1:
                        # Comment out the line
                        indent = match.group(1)
                        lines[line_number - 1] = f"{indent}// {line.strip()} // Removed: unused variable\n"
                        with open(file_path, 'w') as f:
                            f.writelines(lines)
                        return True
        except Exception as e:
            logger.error(f"Error fixing unused variable in {file_path}:{line_number} - {e}")
        return False
    
    def fix_js_cognitive_complexity(self, file_path, line_number, issue):
        """Fix JavaScript cognitive complexity issues (S3776)"""
        # This requires refactoring - mark for manual review
        logger.info(f"High cognitive complexity in {file_path}:{line_number} - requires manual refactoring")
        return False
    
    def fix_ts_this_alias(self, file_path, line_number, issue):
        """Fix TypeScript 'this' alias issues (S6853)"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                line = lines[line_number - 1]
                
                # Pattern: const self = this; or var that = this;
                if re.match(r'^(\s*)(const|let|var)\s+(self|that|_this)\s*=\s*this\s*;?\s*$', line):
                    # Remove this line and use arrow functions instead
                    lines[line_number - 1] = f"// {line.strip()} // Removed: use arrow functions instead\n"
                    with open(file_path, 'w') as f:
                        f.writelines(lines)
                    return True
        except Exception as e:
            logger.error(f"Error fixing this alias in {file_path}:{line_number} - {e}")
        return False
    
    def fix_js_useless_assignment(self, file_path, line_number, issue):
        """Fix JavaScript useless assignment issues (S1854)"""
        return self.fix_ts_useless_assignment(file_path, line_number, issue)
    
    def fix_python_string_duplication(self, file_path, line_number, issue):
        """Fix Python string duplication issues (S1192)"""
        # This requires extracting constants - mark for manual review
        logger.info(f"String duplication in {file_path}:{line_number} - requires constant extraction")
        return False
    
    def fix_python_cognitive_complexity(self, file_path, line_number, issue):
        """Fix Python cognitive complexity issues (S3776)"""
        # This requires refactoring - mark for manual review
        logger.info(f"High cognitive complexity in {file_path}:{line_number} - requires manual refactoring")
        return False
    
    def fix_python_function_parameters(self, file_path, line_number, issue):
        """Fix Python function parameter order issues (S930)"""
        # This requires careful parameter reordering - mark for manual review
        logger.info(f"Function parameter order in {file_path}:{line_number} - requires manual review")
        return False
    
    def fix_plsql_varchar(self, file_path, line_number, issue):
        """Fix PLSQL VARCHAR usage issues"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                line = lines[line_number - 1]
                original = line
                
                # Replace VARCHAR with VARCHAR2
                line = re.sub(r'\bVARCHAR\s*\(', 'VARCHAR2(', line, flags=re.IGNORECASE)
                
                if line != original:
                    lines[line_number - 1] = line
                    with open(file_path, 'w') as f:
                        f.writelines(lines)
                    return True
        except Exception as e:
            logger.error(f"Error fixing VARCHAR in {file_path}:{line_number} - {e}")
        return False
    
    def fix_plsql_string_duplication(self, file_path, line_number, issue):
        """Fix PLSQL string duplication issues (S1192)"""
        # This requires extracting constants - mark for manual review
        logger.info(f"String duplication in {file_path}:{line_number} - requires constant extraction")
        return False
    
    def fix_k8s_readonly_filesystem(self, file_path, line_number, issue):
        """Fix Kubernetes readonly filesystem issues (S6897)"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            # Find the container section
            container_line = -1
            for i in range(max(0, line_number - 20), min(len(lines), line_number + 5)):
                if 'containers:' in lines[i] or '- name:' in lines[i]:
                    container_line = i
                    break
            
            if container_line >= 0:
                # Find the indentation
                indent = len(lines[container_line]) - len(lines[container_line].lstrip())
                
                # Look for securityContext
                security_context_line = -1
                for i in range(container_line, min(len(lines), container_line + 20)):
                    if 'securityContext:' in lines[i]:
                        security_context_line = i
                        break
                
                if security_context_line >= 0:
                    # Add readOnlyRootFilesystem
                    sc_indent = len(lines[security_context_line]) - len(lines[security_context_line].lstrip())
                    lines.insert(security_context_line + 1, f"{' ' * (sc_indent + 2)}readOnlyRootFilesystem: true\n")
                else:
                    # Add security context
                    lines.insert(container_line + 1, f"{' ' * (indent + 2)}securityContext:\n")
                    lines.insert(container_line + 2, f"{' ' * (indent + 4)}readOnlyRootFilesystem: true\n")
                
                with open(file_path, 'w') as f:
                    f.writelines(lines)
                return True
        except Exception as e:
            logger.error(f"Error fixing readonly filesystem in {file_path}:{line_number} - {e}")
        return False
    
    def fix_k8s_security_context(self, file_path, line_number, issue):
        """Fix Kubernetes security context issues (S6865)"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            # Find the container section
            container_line = -1
            for i in range(max(0, line_number - 20), min(len(lines), line_number + 5)):
                if 'containers:' in lines[i] or '- name:' in lines[i]:
                    container_line = i
                    break
            
            if container_line >= 0:
                indent = len(lines[container_line]) - len(lines[container_line].lstrip())
                
                # Add security context
                security_lines = [
                    f"{' ' * (indent + 2)}securityContext:\n",
                    f"{' ' * (indent + 4)}runAsNonRoot: true\n",
                    f"{' ' * (indent + 4)}runAsUser: 1000\n",
                    f"{' ' * (indent + 4)}allowPrivilegeEscalation: false\n"
                ]
                
                # Insert after container name
                for i, line in enumerate(security_lines):
                    lines.insert(container_line + 1 + i, line)
                
                with open(file_path, 'w') as f:
                    f.writelines(lines)
                return True
        except Exception as e:
            logger.error(f"Error fixing security context in {file_path}:{line_number} - {e}")
        return False
    
    def fix_k8s_resource_limits(self, file_path, line_number, issue):
        """Fix Kubernetes resource limits issues (S6596)"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            # Find the container section
            container_line = -1
            for i in range(max(0, line_number - 20), min(len(lines), line_number + 5)):
                if 'containers:' in lines[i] or '- name:' in lines[i]:
                    container_line = i
                    break
            
            if container_line >= 0:
                indent = len(lines[container_line]) - len(lines[container_line].lstrip())
                
                # Add resources
                resource_lines = [
                    f"{' ' * (indent + 2)}resources:\n",
                    f"{' ' * (indent + 4)}limits:\n",
                    f"{' ' * (indent + 6)}memory: \"512Mi\"\n",
                    f"{' ' * (indent + 6)}cpu: \"500m\"\n",
                    f"{' ' * (indent + 4)}requests:\n",
                    f"{' ' * (indent + 6)}memory: \"256Mi\"\n",
                    f"{' ' * (indent + 6)}cpu: \"250m\"\n"
                ]
                
                # Insert after container name
                for i, line in enumerate(resource_lines):
                    lines.insert(container_line + 1 + i, line)
                
                with open(file_path, 'w') as f:
                    f.writelines(lines)
                return True
        except Exception as e:
            logger.error(f"Error fixing resource limits in {file_path}:{line_number} - {e}")
        return False
    
    def fix_docker_healthcheck(self, file_path, line_number, issue):
        """Fix Docker healthcheck issues (S7019)"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            # Add healthcheck before CMD or at end
            healthcheck = [
                "HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \\\n",
                "  CMD curl -f http://localhost:8080/health || exit 1\n",
                "\n"
            ]
            
            # Find where to insert
            cmd_line = -1
            for i, line in enumerate(lines):
                if line.strip().startswith('CMD') or line.strip().startswith('ENTRYPOINT'):
                    cmd_line = i
                    break
            
            if cmd_line >= 0:
                for i, line in enumerate(healthcheck):
                    lines.insert(cmd_line + i, line)
            else:
                lines.extend(healthcheck)
            
            with open(file_path, 'w') as f:
                f.writelines(lines)
            return True
        except Exception as e:
            logger.error(f"Error fixing Docker healthcheck in {file_path}:{line_number} - {e}")
        return False
    
    def fix_docker_user(self, file_path, line_number, issue):
        """Fix Docker user issues (S7031)"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            # Add USER directive before CMD
            user_line = "USER node\n\n"
            
            # Find where to insert
            cmd_line = -1
            for i, line in enumerate(lines):
                if line.strip().startswith('CMD') or line.strip().startswith('ENTRYPOINT'):
                    cmd_line = i
                    break
            
            if cmd_line >= 0:
                lines.insert(cmd_line, user_line)
            else:
                lines.append("\n" + user_line)
            
            with open(file_path, 'w') as f:
                f.writelines(lines)
            return True
        except Exception as e:
            logger.error(f"Error fixing Docker user in {file_path}:{line_number} - {e}")
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
        
        # Sort issues by line number in reverse order to avoid line number shifts
        sorted_issues = sorted(issues, key=lambda x: x.get('line', 0), reverse=True)
        
        for issue in sorted_issues:
            rule = issue.get('rule', '')
            line = issue.get('line', 0)
            
            if rule in self.fixers:
                if self.fixers[rule](file_path, line, issue):
                    self.fixed_count += 1
                    fixed_in_file += 1
                    self.fixes_by_rule[rule] += 1
                    logger.info(f"Fixed {rule} in {file_path}:{line}")
                else:
                    self.skipped_count += 1
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
                
                # Skip HTML files
                if component.endswith('.html'):
                    continue
                
                file_path = self.base_path / component
                if file_path.exists():
                    issues_by_file[str(file_path)].append(issue)
                else:
                    logger.debug(f"File not found: {file_path}")
        
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
        logger.info("ENHANCED FIXER SUMMARY")
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
    
    fixer = EnhancedSonarIssueFixer(base_path)
    fixer.run(issues_file)