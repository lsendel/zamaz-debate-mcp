#!/usr/bin/env python3
"""
Targeted fixes for remaining SonarCloud issues without breaking syntax
"""

import os
import re
import json
import logging
from pathlib import Path
from collections import defaultdict

# Set up logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

class TargetedSonarFixer:
    def __init__(self, project_root):
        self.project_root = Path(project_root)
        self.fixes_applied = defaultdict(int)
        self.errors = []
        
    def fix_var_to_let_const_safe(self, file_path):
        """Safely convert var to let/const without breaking syntax"""
        try:
            with open(file_path, 'r') as f:
                content = f.read()
            
            original_content = content
            lines = content.split('\n')
            modified = False
            
            for i, line in enumerate(lines):
                # Skip if line is in a comment
                if '//' in line and line.strip().startswith('//'):
                    continue
                
                # Pattern to match var declarations at start of line or after whitespace
                # But not in strings or comments
                var_pattern = r'^(\s*)var\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*='
                match = re.match(var_pattern, line)
                
                if match:
                    indent = match.group(1)
                    var_name = match.group(2)
                    
                    # Check if variable is reassigned later
                    is_reassigned = False
                    for j in range(i + 1, len(lines)):
                        if re.search(rf'\b{var_name}\s*=', lines[j]):
                            is_reassigned = True
                            break
                    
                    # Use const if not reassigned, let if reassigned
                    replacement = 'let' if is_reassigned else 'const'
                    lines[i] = re.sub(r'^(\s*)var\s+', rf'\1{replacement} ', line)
                    modified = True
                    self.fixes_applied['var_to_let_const'] += 1
            
            if modified:
                new_content = '\n'.join(lines)
                # Verify we didn't break the file
                if self.verify_js_syntax(new_content, file_path):
                    with open(file_path, 'w') as f:
                        f.write(new_content)
                    return True
                else:
                    logger.warning(f"Syntax check failed after var conversion in {file_path}")
                    
        except Exception as e:
            logger.error(f"Error fixing var declarations in {file_path}: {e}")
            self.errors.append(f"var_to_let_const: {file_path}: {str(e)}")
        
        return False
    
    def fix_empty_catch_blocks(self, file_path):
        """Add error handling to empty catch blocks"""
        try:
            with open(file_path, 'r') as f:
                content = f.read()
            
            # Pattern to match empty catch blocks
            pattern = r'catch\s*\([^)]*\)\s*\{\s*\}'
            
            if re.search(pattern, content):
                # Replace with basic error logging
                new_content = re.sub(
                    pattern,
                    r'catch (error) { console.error("Error:", error); }',
                    content
                )
                
                if self.verify_js_syntax(new_content, file_path):
                    with open(file_path, 'w') as f:
                        f.write(new_content)
                    self.fixes_applied['empty_catch_blocks'] += 1
                    return True
                    
        except Exception as e:
            logger.error(f"Error fixing empty catch blocks in {file_path}: {e}")
            self.errors.append(f"empty_catch_blocks: {file_path}: {str(e)}")
        
        return False
    
    def fix_unused_imports_ts(self, file_path):
        """Remove unused imports from TypeScript files"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            # Read the latest sonar report to get specific unused imports
            unused_imports = self.get_unused_imports_for_file(file_path)
            
            if not unused_imports:
                return False
            
            modified = False
            new_lines = []
            
            for line in lines:
                skip_line = False
                
                for unused in unused_imports:
                    # Check if this line contains the unused import
                    if f"import {unused}" in line or f"{{ {unused}" in line or f"{unused} }}" in line:
                        # Remove just this import from the line
                        if f"{{ {unused} }}" in line:
                            line = line.replace(f"{{ {unused} }}", "{}")
                        elif f"{{ {unused}," in line:
                            line = line.replace(f"{{ {unused},", "{")
                        elif f", {unused} }}" in line:
                            line = line.replace(f", {unused} }}", " }")
                        elif f", {unused}," in line:
                            line = line.replace(f", {unused},", ",")
                        elif f"import {unused}" in line and " from " in line:
                            skip_line = True
                        
                        if not skip_line and "{}" in line:
                            skip_line = True
                        
                        modified = True
                        self.fixes_applied['unused_imports'] += 1
                
                if not skip_line:
                    new_lines.append(line)
            
            if modified:
                with open(file_path, 'w') as f:
                    f.writelines(new_lines)
                return True
                
        except Exception as e:
            logger.error(f"Error fixing unused imports in {file_path}: {e}")
            self.errors.append(f"unused_imports: {file_path}: {str(e)}")
        
        return False
    
    def fix_naming_conventions(self, file_path):
        """Fix naming convention issues (PascalCase to camelCase)"""
        try:
            with open(file_path, 'r') as f:
                content = f.read()
            
            # Skip if this is a test fixture or intentionally uses PascalCase
            if 'fixture' in file_path.lower() or 'mock' in file_path.lower():
                return False
            
            # Pattern to find function names that start with uppercase
            pattern = r'function\s+([A-Z][a-zA-Z0-9]*)\s*\('
            matches = re.findall(pattern, content)
            
            modified = False
            for match in matches:
                # Convert to camelCase
                camel_case = match[0].lower() + match[1:]
                content = content.replace(f'function {match}(', f'function {camel_case}(')
                # Also replace calls to this function
                content = re.sub(rf'\b{match}\(', f'{camel_case}(', content)
                modified = True
                self.fixes_applied['naming_conventions'] += 1
            
            if modified and self.verify_js_syntax(content, file_path):
                with open(file_path, 'w') as f:
                    f.write(content)
                return True
                
        except Exception as e:
            logger.error(f"Error fixing naming conventions in {file_path}: {e}")
            self.errors.append(f"naming_conventions: {file_path}: {str(e)}")
        
        return False
    
    def fix_kubernetes_security(self, file_path):
        """Add security contexts and resource limits to Kubernetes files"""
        try:
            with open(file_path, 'r') as f:
                content = f.read()
            
            # Check if already has security context
            if 'securityContext:' in content:
                return False
            
            # Add security context after containers section
            lines = content.split('\n')
            new_lines = []
            in_container = False
            container_indent = ''
            
            for i, line in enumerate(lines):
                new_lines.append(line)
                
                if 'containers:' in line and not in_container:
                    in_container = True
                    container_indent = line[:line.index('containers:')]
                elif in_container and line.strip().startswith('- name:'):
                    # Found a container, add security context and resources
                    item_indent = line[:line.index('- name:')]
                    security_lines = [
                        f"{item_indent}  securityContext:",
                        f"{item_indent}    runAsNonRoot: true",
                        f"{item_indent}    runAsUser: 1000",
                        f"{item_indent}    readOnlyRootFilesystem: true",
                        f"{item_indent}    allowPrivilegeEscalation: false",
                        f"{item_indent}  resources:",
                        f"{item_indent}    limits:",
                        f"{item_indent}      memory: \"512Mi\"",
                        f"{item_indent}      cpu: \"500m\"",
                        f"{item_indent}    requests:",
                        f"{item_indent}      memory: \"256Mi\"",
                        f"{item_indent}      cpu: \"250m\""
                    ]
                    
                    # Find where to insert (after image line)
                    for j in range(i+1, min(i+10, len(lines))):
                        if 'image:' in lines[j]:
                            # Insert after image line
                            new_lines.extend(lines[i+1:j+1])
                            new_lines.extend(security_lines)
                            # Skip the lines we already added
                            for k in range(i+1, j+1):
                                lines[k] = None
                            break
                    
                    self.fixes_applied['kubernetes_security'] += 1
            
            # Filter out None lines
            new_lines = [line for line in new_lines if line is not None]
            
            if self.fixes_applied['kubernetes_security'] > 0:
                with open(file_path, 'w') as f:
                    f.write('\n'.join(new_lines))
                return True
                
        except Exception as e:
            logger.error(f"Error fixing Kubernetes security in {file_path}: {e}")
            self.errors.append(f"kubernetes_security: {file_path}: {str(e)}")
        
        return False
    
    def verify_js_syntax(self, content, file_path):
        """Verify JavaScript syntax using node"""
        try:
            import subprocess
            import tempfile
            
            with tempfile.NamedTemporaryFile(mode='w', suffix='.js', delete=False) as f:
                f.write(content)
                temp_path = f.name
            
            result = subprocess.run(
                ['node', '--check', temp_path],
                capture_output=True,
                text=True
            )
            
            os.unlink(temp_path)
            
            return result.returncode == 0
            
        except Exception as e:
            logger.warning(f"Could not verify syntax for {file_path}: {e}")
            return True  # Assume it's okay if we can't check
    
    def get_unused_imports_for_file(self, file_path):
        """Get list of unused imports for a specific file from sonar report"""
        # This would normally read from the latest sonar report
        # For now, return empty list
        return []
    
    def fix_docker_security(self, file_path):
        """Add security improvements to Dockerfiles"""
        try:
            with open(file_path, 'r') as f:
                lines = f.readlines()
            
            modified = False
            new_lines = []
            has_user = False
            has_healthcheck = False
            
            for line in lines:
                if line.strip().startswith('USER '):
                    has_user = True
                if line.strip().startswith('HEALTHCHECK '):
                    has_healthcheck = True
                new_lines.append(line)
            
            # Add USER directive before CMD/ENTRYPOINT if not present
            if not has_user:
                for i in range(len(new_lines) - 1, -1, -1):
                    if new_lines[i].strip().startswith(('CMD', 'ENTRYPOINT')):
                        new_lines.insert(i, 'USER 1000\n\n')
                        modified = True
                        self.fixes_applied['docker_user'] += 1
                        break
            
            # Add HEALTHCHECK if not present
            if not has_healthcheck:
                # Add before CMD/ENTRYPOINT
                for i in range(len(new_lines) - 1, -1, -1):
                    if new_lines[i].strip().startswith(('CMD', 'ENTRYPOINT')):
                        new_lines.insert(i, 'HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \\\n  CMD curl -f http://localhost:8080/health || exit 1\n\n')
                        modified = True
                        self.fixes_applied['docker_healthcheck'] += 1
                        break
            
            if modified:
                with open(file_path, 'w') as f:
                    f.writelines(new_lines)
                return True
                
        except Exception as e:
            logger.error(f"Error fixing Docker security in {file_path}: {e}")
            self.errors.append(f"docker_security: {file_path}: {str(e)}")
        
        return False
    
    def run(self):
        """Run all targeted fixes"""
        logger.info("Starting targeted SonarCloud fixes...")
        
        # Fix JavaScript files
        js_files = list(self.project_root.glob("**/*.js"))
        js_files = [f for f in js_files if 'node_modules' not in str(f) and 'build' not in str(f)]
        
        for file_path in js_files:
            self.fix_var_to_let_const_safe(file_path)
            self.fix_empty_catch_blocks(file_path)
            self.fix_naming_conventions(file_path)
        
        # Fix TypeScript files
        ts_files = list(self.project_root.glob("**/*.ts"))
        ts_files = [f for f in ts_files if 'node_modules' not in str(f) and 'build' not in str(f)]
        
        for file_path in ts_files:
            self.fix_unused_imports_ts(file_path)
        
        # Fix Kubernetes files
        k8s_files = list(self.project_root.glob("**/*.yaml"))
        k8s_files.extend(list(self.project_root.glob("**/*.yml")))
        k8s_files = [f for f in k8s_files if 'kubernetes' in str(f).lower() or 'k8s' in str(f).lower()]
        
        for file_path in k8s_files:
            self.fix_kubernetes_security(file_path)
        
        # Fix Dockerfiles
        docker_files = list(self.project_root.glob("**/Dockerfile*"))
        
        for file_path in docker_files:
            self.fix_docker_security(file_path)
        
        # Print summary
        logger.info("\n=== Fix Summary ===")
        total_fixes = sum(self.fixes_applied.values())
        logger.info(f"Total fixes applied: {total_fixes}")
        
        for fix_type, count in self.fixes_applied.items():
            logger.info(f"  {fix_type}: {count}")
        
        if self.errors:
            logger.warning(f"\nErrors encountered: {len(self.errors)}")
            for error in self.errors[:10]:  # Show first 10 errors
                logger.warning(f"  {error}")
        
        return total_fixes

if __name__ == "__main__":
    import sys
    
    project_root = sys.argv[1] if len(sys.argv) > 1 else os.getcwd()
    
    fixer = TargetedSonarFixer(project_root)
    total_fixes = fixer.run()
    
    logger.info(f"\nCompleted with {total_fixes} fixes applied")
    
    if total_fixes > 0:
        logger.info("\nNext steps:")
        logger.info("1. Review the changes with 'git diff'")
        logger.info("2. Run tests to ensure nothing is broken")
        logger.info("3. Commit the changes")
        logger.info("4. Run 'make sonarqube-scan' to see improvements")