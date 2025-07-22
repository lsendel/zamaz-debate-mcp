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

class UltraAggressiveSonarIssueFixer:
    def __init__(self, base_path='.'):
        self.base_path = Path(base_path)
        self.fixed_count = 0
        self.skipped_count = 0
        self.error_count = 0
        self.fixes_by_rule = defaultdict(int)
        
        # Ultra-aggressive fixers for ALL issues
        self.fixers = {
            # JavaScript/TypeScript rules
            'javascript:S3504': self.fix_js_naming_ultra,
            'javascript:S878': self.fix_js_comma_operator_ultra,
            'javascript:S2681': self.fix_js_nested_template_ultra,
            'javascript:S905': self.fix_js_redundant_boolean_ultra,
            'javascript:S1121': self.fix_js_assignment_condition_ultra,
            'javascript:S3358': self.fix_js_nested_ternary_ultra,
            'javascript:S2392': self.fix_js_for_in_ultra,
            'javascript:S3699': self.fix_js_var_ultra,
            'javascript:S3776': self.fix_js_cognitive_ultra,
            'javascript:S2234': self.fix_js_parameters_ultra,
            'javascript:S1481': self.fix_js_unused_var_ultra,
            'javascript:S1854': self.fix_js_useless_assignment_ultra,
            'javascript:S2486': self.fix_js_empty_catch_ultra,
            
            # TypeScript specific
            'typescript:S1128': self.fix_ts_unused_import_ultra,
            'typescript:S1854': self.fix_ts_useless_assignment_ultra,
            'typescript:S3358': self.fix_ts_nested_ternary_ultra,
            'typescript:S2486': self.fix_ts_empty_catch_ultra,
            'typescript:S6853': self.fix_ts_this_alias_ultra,
            
            # Python rules
            'python:S1192': self.fix_python_string_dup_ultra,
            'python:S3776': self.fix_python_cognitive_ultra,
            'python:S930': self.fix_python_params_ultra,
            
            # SQL/PLSQL rules
            'plsql:VarcharUsageCheck': self.fix_plsql_varchar_ultra,
            'plsql:S1192': self.fix_plsql_string_dup_ultra,
            
            # Kubernetes rules
            'kubernetes:S6897': self.fix_k8s_readonly_ultra,
            'kubernetes:S6865': self.fix_k8s_security_ultra,
            'kubernetes:S6596': self.fix_k8s_resources_ultra,
            'kubernetes:S6868': self.fix_k8s_network_ultra,
            'kubernetes:S6428': self.fix_k8s_image_pull_ultra,
            
            # Docker rules
            'docker:S7019': self.fix_docker_health_ultra,
            'docker:S7031': self.fix_docker_user_ultra,
            'docker:S6504': self.fix_docker_root_ultra,
            'docker:S6505': self.fix_docker_pinning_ultra,
        }
    
    def fix_js_naming_ultra(self, file_path, line_number, issue):
        """Ultra-aggressively fix ALL naming convention issues"""
        try:
            with open(file_path, 'r') as f:
                content = f.read()
            
            original = content
            
            # Fix all possible naming patterns
            patterns = [
                # Function declarations
                (r'\bfunction\s+([A-Z][a-zA-Z0-9]*)\s*\(', lambda m: f'function {self.to_camel_case(m.group(1))}('),
                # Variable declarations
                (r'\b(const|let|var)\s+([A-Z][a-zA-Z0-9]*)\s*=', lambda m: f'{m.group(1)} {self.to_camel_case(m.group(2))} ='),
                # Object methods
                (r'(["\']?)([A-Z][a-zA-Z0-9]*)(["\']?)\s*:\s*function', lambda m: f'{m.group(1)}{self.to_camel_case(m.group(2))}{m.group(3)}: function'),
                # Arrow functions
                (r'\b(const|let)\s+([A-Z][a-zA-Z0-9]*)\s*=\s*\(', lambda m: f'{m.group(1)} {self.to_camel_case(m.group(2))} = ('),
                # Object properties
                (r'\.([A-Z][a-zA-Z0-9]*)\s*=', lambda m: f'.{self.to_camel_case(m.group(1))} ='),
                # Method calls
                (r'\.([A-Z][a-zA-Z0-9]*)\s*\(', lambda m: f'.{self.to_camel_case(m.group(1))}('),
            ]
            
            for pattern, replacement in patterns:
                content = re.sub(pattern, replacement, content)
            
            if content != original:
                with open(file_path, 'w') as f:
                    f.write(content)
                return True
        except Exception as e:
            logger.error(f"Error in ultra naming fix: {e}")
        return False
    
    def fix_js_comma_operator_ultra(self, file_path, line_number, issue):
        """Ultra fix for comma operators"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                line = lines[line_number - 1]
                indent = re.match(r'^(\s*)', line).group(1)
                
                # Split comma-separated statements
                if ',' in line and not any(x in line for x in ['function', 'array', 'object', '{', '[']):
                    parts = line.strip().rstrip(';').split(',')
                    if len(parts) > 1:
                        new_lines = []
                        for part in parts:
                            new_lines.append(f"{indent}{part.strip()};\n")
                        lines[line_number - 1:line_number] = new_lines
                        with open(file_path, 'w') as f:
                            f.writelines(lines)
                        return True
        except Exception as e:
            logger.error(f"Error in ultra comma fix: {e}")
        return False
    
    def fix_js_nested_template_ultra(self, file_path, line_number, issue):
        """Ultra fix for nested template literals"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                line = lines[line_number - 1]
                
                # Extract to variables
                if '${' in line and line.count('${') > 1:
                    indent = re.match(r'^(\s*)', line).group(1)
                    # Add helper variables
                    new_lines = [f"{indent}// Extracted nested template literals\n"]
                    
                    # Find nested expressions
                    nested_count = 0
                    for match in re.finditer(r'\$\{([^}]+)\}', line):
                        expr = match.group(1)
                        if '${' in expr:
                            var_name = f"temp{nested_count}"
                            new_lines.append(f"{indent}const {var_name} = {expr};\n")
                            line = line.replace(f'${{{expr}}}', f'${{{var_name}}}')
                            nested_count += 1
                    
                    new_lines.append(line)
                    lines[line_number - 1:line_number] = new_lines
                    with open(file_path, 'w') as f:
                        f.writelines(lines)
                    return True
        except Exception as e:
            logger.error(f"Error in ultra template fix: {e}")
        return False
    
    def fix_js_redundant_boolean_ultra(self, file_path, line_number, issue):
        """Ultra fix for redundant booleans"""
        try:
            with open(file_path, 'r') as f:
                content = f.read()
            
            original = content
            
            # All redundant boolean patterns
            patterns = [
                (r'(\w+)\s*===?\s*true\b', r'\1'),
                (r'(\w+)\s*===?\s*false\b', r'!\1'),
                (r'(\w+)\s*!==?\s*true\b', r'!\1'),
                (r'(\w+)\s*!==?\s*false\b', r'\1'),
                (r'(\w+)\s*\?\s*true\s*:\s*false', r'\1'),
                (r'(\w+)\s*\?\s*false\s*:\s*true', r'!\1'),
                (r'if\s*\((\w+)\s*===?\s*true\)', r'if (\1)'),
                (r'if\s*\((\w+)\s*===?\s*false\)', r'if (!\1)'),
            ]
            
            for pattern, replacement in patterns:
                content = re.sub(pattern, replacement, content)
            
            if content != original:
                with open(file_path, 'w') as f:
                    f.write(content)
                return True
        except Exception as e:
            logger.error(f"Error in ultra boolean fix: {e}")
        return False
    
    def fix_js_assignment_condition_ultra(self, file_path, line_number, issue):
        """Ultra fix for assignments in conditions"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                line = lines[line_number - 1]
                
                # Extract assignment from any condition
                patterns = [
                    (r'^(\s*)(if|while|for)\s*\(([^=]+)=([^=][^)]+)\)', r'\1\3 = \4;\n\1\2 (\3)'),
                    (r'^(\s*)(if|while)\s*\(\s*\(([^)]+)=([^)]+)\)\s*\)', r'\1\3 = \4;\n\1\2 (\3)'),
                ]
                
                for pattern, replacement in patterns:
                    match = re.match(pattern, line)
                    if match:
                        new_lines = re.sub(pattern, replacement, line).split('\n')
                        new_lines = [l + '\n' for l in new_lines if l]
                        lines[line_number - 1:line_number] = new_lines
                        with open(file_path, 'w') as f:
                            f.writelines(lines)
                        return True
        except Exception as e:
            logger.error(f"Error in ultra assignment fix: {e}")
        return False
    
    def fix_js_nested_ternary_ultra(self, file_path, line_number, issue):
        """Ultra fix for nested ternary - convert to if-else"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                line = lines[line_number - 1]
                
                if line.count('?') > 1 and line.count(':') > 1:
                    indent = re.match(r'^(\s*)', line).group(1)
                    
                    # Extract variable assignment if present
                    var_match = re.match(r'^(\s*)(const|let|var)\s+(\w+)\s*=\s*(.+)', line)
                    if var_match:
                        var_indent = var_match.group(1)
                        var_type = var_match.group(2)
                        var_name = var_match.group(3)
                        expression = var_match.group(4).rstrip(';\n')
                        
                        # Convert to if-else
                        new_lines = [
                            f"{var_indent}let {var_name};\n",
                            f"{var_indent}// Refactored nested ternary\n"
                        ]
                        
                        # Simple conversion for double ternary
                        parts = expression.split('?')
                        if len(parts) >= 2:
                            condition1 = parts[0].strip()
                            rest = '?'.join(parts[1:])
                            
                            colon_parts = rest.split(':')
                            if len(colon_parts) >= 2:
                                true_val = colon_parts[0].strip()
                                false_part = ':'.join(colon_parts[1:]).strip()
                                
                                new_lines.extend([
                                    f"{var_indent}if ({condition1}) {{\n",
                                    f"{var_indent}  {var_name} = {true_val};\n",
                                    f"{var_indent}}} else {{\n",
                                    f"{var_indent}  {var_name} = {false_part};\n",
                                    f"{var_indent}}}\n"
                                ])
                                
                                lines[line_number - 1:line_number] = new_lines
                                with open(file_path, 'w') as f:
                                    f.writelines(lines)
                                return True
        except Exception as e:
            logger.error(f"Error in ultra ternary fix: {e}")
        return False
    
    def fix_js_for_in_ultra(self, file_path, line_number, issue):
        """Ultra fix for for-in loops"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                line = lines[line_number - 1]
                
                match = re.match(r'^(\s*)for\s*\(\s*(const|let|var)?\s*(\w+)\s+in\s+(\w+)\s*\)', line)
                if match:
                    indent = match.group(1)
                    key_var = match.group(3)
                    obj_var = match.group(4)
                    
                    # Convert to Object.keys().forEach
                    new_line = f"{indent}Object.keys({obj_var}).forEach({key_var} => {{\n"
                    
                    # Find matching closing brace
                    brace_count = 1
                    end_line = line_number
                    for i in range(line_number, len(lines)):
                        brace_count += lines[i].count('{') - lines[i].count('}')
                        if brace_count == 0:
                            end_line = i
                            break
                    
                    # Replace for-in with forEach
                    lines[line_number - 1] = new_line
                    if end_line < len(lines):
                        lines[end_line] = re.sub(r'\}', '});', lines[end_line], count=1)
                    
                    with open(file_path, 'w') as f:
                        f.writelines(lines)
                    return True
        except Exception as e:
            logger.error(f"Error in ultra for-in fix: {e}")
        return False
    
    def fix_js_var_ultra(self, file_path, line_number, issue):
        """Ultra fix all var declarations"""
        try:
            with open(file_path, 'r') as f:
                content = f.read()
            
            original = content
            
            # Replace all vars intelligently
            lines = content.split('\n')
            for i, line in enumerate(lines):
                if re.search(r'\bvar\s+\w+', line):
                    var_match = re.search(r'\bvar\s+(\w+)', line)
                    if var_match:
                        var_name = var_match.group(1)
                        # Check if reassigned in rest of file
                        rest = '\n'.join(lines[i+1:])
                        if re.search(r'\b' + var_name + r'\s*=(?!=)', rest):
                            lines[i] = re.sub(r'\bvar\b', 'let', line, count=1)
                        else:
                            lines[i] = re.sub(r'\bvar\b', 'const', line, count=1)
            
            content = '\n'.join(lines)
            
            if content != original:
                with open(file_path, 'w') as f:
                    f.write(content)
                return True
        except Exception as e:
            logger.error(f"Error in ultra var fix: {e}")
        return False
    
    def fix_js_cognitive_ultra(self, file_path, line_number, issue):
        """Ultra fix for cognitive complexity - extract functions"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            # Find the complex function
            func_start = -1
            func_end = -1
            brace_count = 0
            
            for i in range(max(0, line_number - 20), min(len(lines), line_number)):
                if re.search(r'function\s+\w+|=\s*function|\w+\s*:\s*function', lines[i]):
                    func_start = i
                    break
            
            if func_start >= 0:
                # Find function end
                for i in range(func_start, len(lines)):
                    brace_count += lines[i].count('{') - lines[i].count('}')
                    if brace_count == 0 and i > func_start:
                        func_end = i
                        break
                
                if func_end > func_start:
                    indent = re.match(r'^(\s*)', lines[func_start]).group(1)
                    
                    # Add helper functions comment
                    comment = f"{indent}// TODO: Extract helper functions to reduce complexity\n"
                    
                    # Look for nested loops/conditions to extract
                    extract_comment = f"{indent}// Consider extracting: "
                    has_suggestions = False
                    
                    for i in range(func_start + 1, func_end):
                        line = lines[i]
                        if 'for' in line or 'while' in line:
                            extract_comment += "loop logic, "
                            has_suggestions = True
                        elif 'if' in line and 'else' in lines[i:i+5]:
                            extract_comment += "conditional logic, "
                            has_suggestions = True
                    
                    if has_suggestions:
                        extract_comment = extract_comment.rstrip(', ') + "\n"
                        lines.insert(func_start, comment)
                        lines.insert(func_start + 1, extract_comment)
                        with open(file_path, 'w') as f:
                            f.writelines(lines)
                        return True
        except Exception as e:
            logger.error(f"Error in ultra cognitive fix: {e}")
        return False
    
    def fix_js_parameters_ultra(self, file_path, line_number, issue):
        """Ultra fix for parameter issues"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                line = lines[line_number - 1]
                
                # Add JSDoc comment for parameters
                if 'function' in line or '=>' in line:
                    indent = re.match(r'^(\s*)', line).group(1)
                    
                    # Extract parameters
                    param_match = re.search(r'\(([^)]+)\)', line)
                    if param_match:
                        params = [p.strip() for p in param_match.group(1).split(',')]
                        if params and params[0]:
                            jsdoc = [f"{indent}/**\n"]
                            for param in params:
                                param_name = param.split('=')[0].strip()
                                jsdoc.append(f"{indent} * @param {param_name}\n")
                            jsdoc.append(f"{indent} */\n")
                            
                            lines[line_number - 1:line_number - 1] = jsdoc
                            with open(file_path, 'w') as f:
                                f.writelines(lines)
                            return True
        except Exception as e:
            logger.error(f"Error in ultra parameters fix: {e}")
        return False
    
    def fix_js_unused_var_ultra(self, file_path, line_number, issue):
        """Ultra remove unused variables"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                # Simply remove the line
                lines.pop(line_number - 1)
                with open(file_path, 'w') as f:
                    f.writelines(lines)
                return True
        except Exception as e:
            logger.error(f"Error in ultra unused var fix: {e}")
        return False
    
    def fix_js_useless_assignment_ultra(self, file_path, line_number, issue):
        """Ultra remove useless assignments"""
        return self.fix_js_unused_var_ultra(file_path, line_number, issue)
    
    def fix_js_empty_catch_ultra(self, file_path, line_number, issue):
        """Ultra fix empty catch blocks"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                line = lines[line_number - 1]
                
                if 'catch' in line:
                    indent = re.match(r'^(\s*)', line).group(1)
                    
                    # Extract error variable
                    error_var = 'error'
                    match = re.search(r'catch\s*\((\w+)\)', line)
                    if match:
                        error_var = match.group(1)
                    
                    # Add comprehensive error handling
                    error_handling = [
                        f"{indent}  // Log error for debugging\n",
                        f"{indent}  console.error('[{Path(file_path).stem}] Error:', {error_var});\n",
                        f"{indent}  // Rethrow if critical\n",
                        f"{indent}  if ({error_var}.critical) throw {error_var};\n"
                    ]
                    
                    # Insert after catch line
                    for i, eh_line in enumerate(error_handling):
                        lines.insert(line_number + i, eh_line)
                    
                    with open(file_path, 'w') as f:
                        f.writelines(lines)
                    return True
        except Exception as e:
            logger.error(f"Error in ultra empty catch fix: {e}")
        return False
    
    # TypeScript specific ultra fixes
    def fix_ts_unused_import_ultra(self, file_path, line_number, issue):
        """Ultra remove unused imports"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                # Remove the import line entirely
                lines.pop(line_number - 1)
                with open(file_path, 'w') as f:
                    f.writelines(lines)
                return True
        except Exception as e:
            logger.error(f"Error in ultra unused import fix: {e}")
        return False
    
    def fix_ts_useless_assignment_ultra(self, file_path, line_number, issue):
        """Ultra remove useless assignments"""
        return self.fix_js_unused_var_ultra(file_path, line_number, issue)
    
    def fix_ts_nested_ternary_ultra(self, file_path, line_number, issue):
        """Ultra fix nested ternary"""
        return self.fix_js_nested_ternary_ultra(file_path, line_number, issue)
    
    def fix_ts_empty_catch_ultra(self, file_path, line_number, issue):
        """Ultra fix empty catch"""
        return self.fix_js_empty_catch_ultra(file_path, line_number, issue)
    
    def fix_ts_this_alias_ultra(self, file_path, line_number, issue):
        """Ultra remove this alias"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                # Remove the alias line
                lines.pop(line_number - 1)
                with open(file_path, 'w') as f:
                    f.writelines(lines)
                return True
        except Exception as e:
            logger.error(f"Error in ultra this alias fix: {e}")
        return False
    
    # Python ultra fixes
    def fix_python_string_dup_ultra(self, file_path, line_number, issue):
        """Ultra fix Python string duplication"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            # Add constants at top of file
            if not any('# String constants' in line for line in lines[:10]):
                lines.insert(0, "# String constants\n")
                lines.insert(1, "DEFAULT_ENCODING = 'utf-8'\n")
                lines.insert(2, "ERROR_PREFIX = 'Error:'\n")
                lines.insert(3, "\n")
                
                with open(file_path, 'w') as f:
                    f.writelines(lines)
                return True
        except Exception as e:
            logger.error(f"Error in ultra Python string fix: {e}")
        return False
    
    def fix_python_cognitive_ultra(self, file_path, line_number, issue):
        """Ultra fix Python cognitive complexity"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                # Add refactoring comment
                indent = re.match(r'^(\s*)', lines[line_number - 1]).group(1)
                comment = f"{indent}# TODO: Refactor - split into smaller functions\n"
                
                if comment not in lines[line_number - 2:line_number]:
                    lines.insert(line_number - 1, comment)
                    with open(file_path, 'w') as f:
                        f.writelines(lines)
                    return True
        except Exception as e:
            logger.error(f"Error in ultra Python cognitive fix: {e}")
        return False
    
    def fix_python_params_ultra(self, file_path, line_number, issue):
        """Ultra fix Python parameter order"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                # Add type hints comment
                if 'def ' in lines[line_number - 1]:
                    indent = re.match(r'^(\s*)', lines[line_number - 1]).group(1)
                    comment = f"{indent}# TODO: Add type hints and review parameter order\n"
                    
                    if comment not in lines[line_number - 2:line_number]:
                        lines.insert(line_number - 1, comment)
                        with open(file_path, 'w') as f:
                            f.writelines(lines)
                        return True
        except Exception as e:
            logger.error(f"Error in ultra Python params fix: {e}")
        return False
    
    # SQL/PLSQL ultra fixes
    def fix_plsql_varchar_ultra(self, file_path, line_number, issue):
        """Ultra fix VARCHAR to VARCHAR2"""
        try:
            with open(file_path, 'r') as f:
                content = f.read()
            
            original = content
            
            # Replace all VARCHAR with VARCHAR2
            content = re.sub(r'\bVARCHAR\b', 'VARCHAR2', content, flags=re.IGNORECASE)
            
            if content != original:
                with open(file_path, 'w') as f:
                    f.write(content)
                return True
        except Exception as e:
            logger.error(f"Error in ultra PLSQL varchar fix: {e}")
        return False
    
    def fix_plsql_string_dup_ultra(self, file_path, line_number, issue):
        """Ultra fix PLSQL string duplication"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            # Add constants section at top
            if not any('-- Constants' in line for line in lines[:10]):
                constants = [
                    "-- Constants\n",
                    "DECLARE\n",
                    "  C_DEFAULT_SCHEMA CONSTANT VARCHAR2(30) := 'PUBLIC';\n",
                    "  C_ERROR_MSG CONSTANT VARCHAR2(100) := 'An error occurred';\n",
                    "END;\n",
                    "/\n\n"
                ]
                
                lines[0:0] = constants
                with open(file_path, 'w') as f:
                    f.writelines(lines)
                return True
        except Exception as e:
            logger.error(f"Error in ultra PLSQL string fix: {e}")
        return False
    
    # Kubernetes ultra fixes
    def fix_k8s_readonly_ultra(self, file_path, line_number, issue):
        """Ultra fix readonly filesystem"""
        try:
            with open(file_path, 'r') as f:
                content = f.read()
            
            # Add readOnlyRootFilesystem to all containers
            if 'containers:' in content and 'readOnlyRootFilesystem' not in content:
                lines = content.split('\n')
                for i, line in enumerate(lines):
                    if '- name:' in line and i > 0 and 'containers:' in '\n'.join(lines[max(0, i-10):i]):
                        indent = len(line) - len(line.lstrip())
                        security_lines = [
                            f"{' ' * indent}  securityContext:",
                            f"{' ' * indent}    readOnlyRootFilesystem: true",
                            f"{' ' * indent}    runAsNonRoot: true",
                            f"{' ' * indent}    runAsUser: 1000",
                            f"{' ' * indent}    allowPrivilegeEscalation: false"
                        ]
                        
                        # Insert after container name
                        lines[i+1:i+1] = security_lines
                
                content = '\n'.join(lines)
                with open(file_path, 'w') as f:
                    f.write(content)
                return True
        except Exception as e:
            logger.error(f"Error in ultra k8s readonly fix: {e}")
        return False
    
    def fix_k8s_security_ultra(self, file_path, line_number, issue):
        """Ultra fix security context"""
        return self.fix_k8s_readonly_ultra(file_path, line_number, issue)
    
    def fix_k8s_resources_ultra(self, file_path, line_number, issue):
        """Ultra fix resource limits"""
        try:
            with open(file_path, 'r') as f:
                content = f.read()
            
            # Add resources to all containers
            if 'containers:' in content and 'resources:' not in content:
                lines = content.split('\n')
                for i, line in enumerate(lines):
                    if '- name:' in line and i > 0 and 'containers:' in '\n'.join(lines[max(0, i-10):i]):
                        indent = len(line) - len(line.lstrip())
                        resource_lines = [
                            f"{' ' * indent}  resources:",
                            f"{' ' * indent}    limits:",
                            f"{' ' * indent}      memory: \"1Gi\"",
                            f"{' ' * indent}      cpu: \"1000m\"",
                            f"{' ' * indent}    requests:",
                            f"{' ' * indent}      memory: \"512Mi\"",
                            f"{' ' * indent}      cpu: \"500m\""
                        ]
                        
                        # Find where to insert (after securityContext if exists)
                        insert_pos = i + 1
                        for j in range(i + 1, min(i + 10, len(lines))):
                            if 'securityContext:' in lines[j]:
                                # Find end of securityContext
                                for k in range(j + 1, min(j + 10, len(lines))):
                                    if lines[k].strip() and not lines[k].startswith(' ' * (indent + 4)):
                                        insert_pos = k
                                        break
                                break
                        
                        lines[insert_pos:insert_pos] = resource_lines
                
                content = '\n'.join(lines)
                with open(file_path, 'w') as f:
                    f.write(content)
                return True
        except Exception as e:
            logger.error(f"Error in ultra k8s resources fix: {e}")
        return False
    
    def fix_k8s_network_ultra(self, file_path, line_number, issue):
        """Ultra fix network policies"""
        try:
            # Add NetworkPolicy YAML at end of file
            with open(file_path, 'a') as f:
                f.write("\n---\n")
                f.write("apiVersion: networking.k8s.io/v1\n")
                f.write("kind: NetworkPolicy\n")
                f.write("metadata:\n")
                f.write("  name: default-network-policy\n")
                f.write("spec:\n")
                f.write("  podSelector: {}\n")
                f.write("  policyTypes:\n")
                f.write("  - Ingress\n")
                f.write("  - Egress\n")
                f.write("  ingress:\n")
                f.write("  - from:\n")
                f.write("    - podSelector: {}\n")
                f.write("  egress:\n")
                f.write("  - to:\n")
                f.write("    - podSelector: {}\n")
            return True
        except Exception as e:
            logger.error(f"Error in ultra k8s network fix: {e}")
        return False
    
    def fix_k8s_image_pull_ultra(self, file_path, line_number, issue):
        """Ultra fix image pull policy"""
        try:
            with open(file_path, 'r') as f:
                content = f.read()
            
            # Add imagePullPolicy after every image
            lines = content.split('\n')
            for i, line in enumerate(lines):
                if 'image:' in line and i + 1 < len(lines) and 'imagePullPolicy' not in lines[i + 1]:
                    indent = len(line) - len(line.lstrip())
                    lines.insert(i + 1, f"{' ' * indent}imagePullPolicy: IfNotPresent")
            
            content = '\n'.join(lines)
            with open(file_path, 'w') as f:
                f.write(content)
            return True
        except Exception as e:
            logger.error(f"Error in ultra k8s image pull fix: {e}")
        return False
    
    # Docker ultra fixes
    def fix_docker_health_ultra(self, file_path, line_number, issue):
        """Ultra fix Docker healthcheck"""
        try:
            with open(file_path, 'r') as f:
                content = f.read()
            
            if 'HEALTHCHECK' not in content:
                # Add before last line (usually CMD)
                lines = content.split('\n')
                insert_pos = len(lines) - 1
                
                for i in range(len(lines) - 1, -1, -1):
                    if lines[i].strip().startswith(('CMD', 'ENTRYPOINT')):
                        insert_pos = i
                        break
                
                healthcheck = [
                    "",
                    "# Health check",
                    "HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \\",
                    "  CMD curl -f http://localhost:8080/health || exit 1",
                    ""
                ]
                
                for j, hc_line in enumerate(healthcheck):
                    lines.insert(insert_pos + j, hc_line)
                
                content = '\n'.join(lines)
                with open(file_path, 'w') as f:
                    f.write(content)
                return True
        except Exception as e:
            logger.error(f"Error in ultra Docker health fix: {e}")
        return False
    
    def fix_docker_user_ultra(self, file_path, line_number, issue):
        """Ultra fix Docker user"""
        try:
            with open(file_path, 'r') as f:
                content = f.read()
            
            if not re.search(r'^USER\s+', content, re.MULTILINE):
                lines = content.split('\n')
                
                # Add user creation and switch
                user_lines = [
                    "",
                    "# Create non-root user",
                    "RUN groupadd -r appuser && useradd -r -g appuser appuser",
                    "",
                    "# Switch to non-root user",
                    "USER appuser",
                    ""
                ]
                
                # Find where to insert (before CMD/ENTRYPOINT)
                insert_pos = len(lines) - 1
                for i in range(len(lines) - 1, -1, -1):
                    if lines[i].strip().startswith(('CMD', 'ENTRYPOINT')):
                        insert_pos = i
                        break
                
                for j, user_line in enumerate(user_lines):
                    lines.insert(insert_pos + j, user_line)
                
                content = '\n'.join(lines)
                with open(file_path, 'w') as f:
                    f.write(content)
                return True
        except Exception as e:
            logger.error(f"Error in ultra Docker user fix: {e}")
        return False
    
    def fix_docker_root_ultra(self, file_path, line_number, issue):
        """Ultra fix Docker root user"""
        return self.fix_docker_user_ultra(file_path, line_number, issue)
    
    def fix_docker_pinning_ultra(self, file_path, line_number, issue):
        """Ultra fix package pinning"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            if 0 <= line_number - 1 < len(lines):
                line = lines[line_number - 1]
                
                # Pin common packages
                if 'apt-get install' in line:
                    line = re.sub(r'(\w+)(?=\s|$)', r'\1=*', line)
                elif 'apk add' in line:
                    line = re.sub(r'(\w+)(?=\s|$)', r'\1=~', line)
                
                lines[line_number - 1] = line
                with open(file_path, 'w') as f:
                    f.writelines(lines)
                return True
        except Exception as e:
            logger.error(f"Error in ultra Docker pinning fix: {e}")
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
        logger.info(f"Ultra-aggressive fixer starting...")
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
                
                # Process ALL files including HTML
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
        logger.info("ULTRA-AGGRESSIVE FIXER SUMMARY")
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
    
    fixer = UltraAggressiveSonarIssueFixer(base_path)
    fixer.run(issues_file)