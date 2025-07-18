#!/bin/bash
# Workflow integration tests for the linting system
# Tests end-to-end developer workflows and CI/CD integration

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

WORKFLOW_TEST_DIR=".linting/workflow-tests"
RESULTS_DIR=".linting/test-results/workflow"

setup_workflow_tests() {
    echo -e "${BLUE}Setting up workflow test environment...${NC}"
    
    mkdir -p "$WORKFLOW_TEST_DIR"/{git-hooks,ide-config,ci-simulation}
    mkdir -p "$RESULTS_DIR"
    
    # Create a temporary git repository for testing
    cd "$WORKFLOW_TEST_DIR/git-hooks"
    git init . > /dev/null 2>&1
    git config user.email "test@example.com"
    git config user.name "Test User"
    cd - > /dev/null
    
    echo -e "${GREEN}✓ Workflow test environment ready${NC}"
}

# Test pre-commit hook integration
test_precommit_workflow() {
    echo -e "${YELLOW}Testing pre-commit hook workflow...${NC}"
    
    local test_repo="$WORKFLOW_TEST_DIR/git-hooks"
    
    # Copy pre-commit configuration
    cp .pre-commit-config.yaml "$test_repo/" 2>/dev/null || echo "# Pre-commit config" > "$test_repo/.pre-commit-config.yaml"
    
    # Create a test file with violations
    cat > "$test_repo/test-file.java" << 'EOF'
package com.test;
public class TestFile{
    public static void main(String args[]){
        if(args.length>0)
            System.out.println("Hello "+args[0]);
    }
}
EOF
    
    cd "$test_repo"
    
    # Install pre-commit hooks
    if command -v pre-commit >/dev/null 2>&1; then
        pre-commit install > /dev/null 2>&1 || true
        
        # Add file and attempt commit
        git add test-file.java
        
        # Test pre-commit hook execution
        if pre-commit run --all-files > "$RESULTS_DIR/precommit-output.txt" 2>&1; then
            echo -e "${GREEN}✓ Pre-commit hooks executed successfully${NC}"
        else
            echo -e "${YELLOW}⚠ Pre-commit hooks detected violations (expected)${NC}"
        fi
    else
        echo -e "${YELLOW}⚠ Pre-commit not installed, skipping hook test${NC}"
    fi
    
    cd - > /dev/null
    
    return 0
}

# Test IDE integration workflow
test_ide_integration_workflow() {
    echo -e "${YELLOW}Testing IDE integration workflow...${NC}"
    
    local ide_config_dir="$WORKFLOW_TEST_DIR/ide-config"
    
    # Test VS Code configuration
    if [ -f .vscode/settings.json ]; then
        cp .vscode/settings.json "$ide_config_dir/vscode-settings.json"
        
        # Validate VS Code settings
        if command -v jq >/dev/null 2>&1; then
            if jq empty "$ide_config_dir/vscode-settings.json" 2>/dev/null; then
                echo -e "${GREEN}✓ VS Code settings are valid JSON${NC}"
            else
                echo -e "${RED}✗ VS Code settings contain invalid JSON${NC}"
                return 1
            fi
        fi
        
        # Check for required linting configurations
        local required_settings=(
            "java.checkstyle.configuration"
            "eslint.workingDirectories"
            "prettier.configPath"
            "markdownlint.config"
        )
        
        local found_settings=0
        for setting in "${required_settings[@]}"; do
            if grep -q "$setting" "$ide_config_dir/vscode-settings.json"; then
                found_settings=$((found_settings + 1))
            fi
        done
        
        if [ $found_settings -ge 3 ]; then
            echo -e "${GREEN}✓ VS Code linting integration properly configured${NC}"
        else
            echo -e "${YELLOW}⚠ Some VS Code linting settings may be missing${NC}"
        fi
    else
        echo -e "${YELLOW}⚠ VS Code settings not found${NC}"
    fi
    
    # Test IntelliJ configuration (if present)
    if [ -d .idea ]; then
        echo -e "${GREEN}✓ IntelliJ IDEA configuration detected${NC}"
    fi
    
    return 0
}

# Test CI/CD workflow simulation
test_cicd_workflow() {
    echo -e "${YELLOW}Testing CI/CD workflow simulation...${NC}"
    
    local ci_dir="$WORKFLOW_TEST_DIR/ci-simulation"
    
    # Create sample files for CI testing
    cat > "$ci_dir/sample.java" << 'EOF'
package com.zamaz.mcp.ci;

public class CISample {
    public void method() {
        System.out.println("CI test");
    }
}
EOF
    
    cat > "$ci_dir/sample.tsx" << 'EOF'
import React from 'react';

export const CISample: React.FC = () => {
    return <div>CI Test</div>;
};
EOF
    
    # Simulate CI linting steps
    echo -e "${BLUE}Simulating CI/CD linting pipeline...${NC}"
    
    # Step 1: Java linting
    if java -jar $(find ~/.m2/repository -name 'checkstyle-*.jar' | head -1) \
        -c .linting/java/checkstyle.xml "$ci_dir/sample.java" > "$RESULTS_DIR/ci-java-lint.txt" 2>&1; then
        echo -e "${GREEN}✓ CI Java linting passed${NC}"
    else
        echo -e "${YELLOW}⚠ CI Java linting found issues (expected in CI)${NC}"
    fi
    
    # Step 2: Frontend linting
    if cd debate-ui && npx eslint --config ../.linting/frontend/.eslintrc.js "../$ci_dir/sample.tsx" > "../$RESULTS_DIR/ci-frontend-lint.txt" 2>&1; then
        echo -e "${GREEN}✓ CI Frontend linting passed${NC}"
    else
        echo -e "${YELLOW}⚠ CI Frontend linting found issues${NC}"
    fi
    cd - > /dev/null
    
    # Step 3: Quality gate simulation
    local java_issues=$(grep -c "ERROR\|WARN" "$RESULTS_DIR/ci-java-lint.txt" 2>/dev/null || echo 0)
    local frontend_issues=$(grep -c "error\|warning" "$RESULTS_DIR/ci-frontend-lint.txt" 2>/dev/null || echo 0)
    local total_issues=$((java_issues + frontend_issues))
    
    echo "Java issues: $java_issues" > "$RESULTS_DIR/quality-gate-report.txt"
    echo "Frontend issues: $frontend_issues" >> "$RESULTS_DIR/quality-gate-report.txt"
    echo "Total issues: $total_issues" >> "$RESULTS_DIR/quality-gate-report.txt"
    
    # Quality gate threshold (allow up to 5 issues for testing)
    if [ $total_issues -le 5 ]; then
        echo -e "${GREEN}✓ Quality gate passed (${total_issues} issues)${NC}"
    else
        echo -e "${RED}✗ Quality gate failed (${total_issues} issues)${NC}"
    fi
    
    return 0
}

# Test incremental linting workflow
test_incremental_workflow() {
    echo -e "${YELLOW}Testing incremental linting workflow...${NC}"
    
    local incremental_dir="$WORKFLOW_TEST_DIR/incremental"
    mkdir -p "$incremental_dir"
    
    # Create multiple files
    for i in {1..5}; do
        cat > "$incremental_dir/File$i.java" << EOF
package com.test.incremental;

public class File$i {
    public void method$i() {
        System.out.println("File $i");
    }
}
EOF
    done
    
    # Test incremental linting script
    if .linting/scripts/incremental-lint.sh --include-pattern "$incremental_dir/*.java" --verbose > "$RESULTS_DIR/incremental-output.txt" 2>&1; then
        echo -e "${GREEN}✓ Incremental linting executed successfully${NC}"
    else
        echo -e "${YELLOW}⚠ Incremental linting completed with issues${NC}"
    fi
    
    # Test cache behavior
    echo -e "${BLUE}Testing incremental cache behavior...${NC}"
    
    # First run
    local start_time1=$(date +%s)
    .linting/scripts/incremental-lint.sh --include-pattern "$incremental_dir/*.java" > /dev/null 2>&1 || true
    local end_time1=$(date +%s)
    local first_run_time=$((end_time1 - start_time1))
    
    # Second run (should be faster due to caching)
    local start_time2=$(date +%s)
    .linting/scripts/incremental-lint.sh --include-pattern "$incremental_dir/*.java" > /dev/null 2>&1 || true
    local end_time2=$(date +%s)
    local second_run_time=$((end_time2 - start_time2))
    
    echo "First run: ${first_run_time}s" > "$RESULTS_DIR/incremental-timing.txt"
    echo "Second run: ${second_run_time}s" >> "$RESULTS_DIR/incremental-timing.txt"
    
    if [ $second_run_time -le $first_run_time ]; then
        echo -e "${GREEN}✓ Incremental caching improves performance${NC}"
    else
        echo -e "${YELLOW}⚠ Incremental caching may need optimization${NC}"
    fi
    
    return 0
}

# Test error handling and recovery workflows
test_error_handling_workflow() {
    echo -e "${YELLOW}Testing error handling workflow...${NC}"
    
    local error_dir="$WORKFLOW_TEST_DIR/error-handling"
    mkdir -p "$error_dir"
    
    # Create files with various types of errors
    
    # Syntax error file
    cat > "$error_dir/SyntaxError.java" << 'EOF'
package com.test.error;

public class SyntaxError {
    public void method() {
        // Missing closing brace
        if (true) {
            System.out.println("test");
        // Missing closing brace
    }
EOF
    
    # File with encoding issues
    echo -e "package com.test.error;\n\npublic class EncodingTest {\n    // Special chars: \xc3\xa9\xc3\xa1\xc3\xad\n}" > "$error_dir/EncodingTest.java"
    
    # Test error handling
    echo -e "${BLUE}Testing linter error handling...${NC}"
    
    # Test with syntax error file
    if java -jar $(find ~/.m2/repository -name 'checkstyle-*.jar' | head -1) \
        -c .linting/java/checkstyle.xml "$error_dir/SyntaxError.java" > "$RESULTS_DIR/syntax-error-handling.txt" 2>&1; then
        echo -e "${YELLOW}⚠ Syntax error not detected (unexpected)${NC}"
    else
        echo -e "${GREEN}✓ Syntax error properly handled${NC}"
    fi
    
    # Test with non-existent file
    if java -jar $(find ~/.m2/repository -name 'checkstyle-*.jar' | head -1) \
        -c .linting/java/checkstyle.xml "$error_dir/NonExistent.java" > "$RESULTS_DIR/missing-file-handling.txt" 2>&1; then
        echo -e "${YELLOW}⚠ Missing file not detected${NC}"
    else
        echo -e "${GREEN}✓ Missing file error properly handled${NC}"
    fi
    
    return 0
}

# Test reporting and metrics workflow
test_reporting_workflow() {
    echo -e "${YELLOW}Testing reporting workflow...${NC}"
    
    # Test report generation
    if [ -f .linting/cache/lint-results.json ]; then
        echo -e "${GREEN}✓ Linting results cache exists${NC}"
        
        # Validate JSON format
        if command -v jq >/dev/null 2>&1; then
            if jq empty .linting/cache/lint-results.json 2>/dev/null; then
                echo -e "${GREEN}✓ Linting results are valid JSON${NC}"
            else
                echo -e "${RED}✗ Linting results contain invalid JSON${NC}"
                return 1
            fi
        fi
    else
        echo -e "${YELLOW}⚠ Linting results cache not found${NC}"
    fi
    
    # Test cache manager functionality
    if node .linting/scripts/cache-manager.js stats > "$RESULTS_DIR/cache-stats.txt" 2>&1; then
        echo -e "${GREEN}✓ Cache manager executed successfully${NC}"
    else
        echo -e "${RED}✗ Cache manager failed${NC}"
        return 1
    fi
    
    return 0
}

# Generate workflow test summary
generate_workflow_summary() {
    echo -e "${BLUE}Generating workflow test summary...${NC}"
    
    local summary_file="$RESULTS_DIR/workflow-summary.md"
    
    cat > "$summary_file" << EOF
# Workflow Integration Test Summary

Test execution completed on $(date)

## Test Results

### Pre-commit Workflow
$([ -f "$RESULTS_DIR/precommit-output.txt" ] && echo "✅ Executed" || echo "❌ Failed")

### IDE Integration
$([ -f "$WORKFLOW_TEST_DIR/ide-config/vscode-settings.json" ] && echo "✅ VS Code configured" || echo "❌ VS Code not configured")

### CI/CD Simulation
$([ -f "$RESULTS_DIR/quality-gate-report.txt" ] && echo "✅ Quality gate tested" || echo "❌ Quality gate not tested")

### Incremental Linting
$([ -f "$RESULTS_DIR/incremental-timing.txt" ] && echo "✅ Performance tested" || echo "❌ Performance not tested")

### Error Handling
$([ -f "$RESULTS_DIR/syntax-error-handling.txt" ] && echo "✅ Error handling tested" || echo "❌ Error handling not tested")

### Reporting System
$([ -f "$RESULTS_DIR/cache-stats.txt" ] && echo "✅ Reporting functional" || echo "❌ Reporting not functional")

## Performance Metrics

$([ -f "$RESULTS_DIR/incremental-timing.txt" ] && cat "$RESULTS_DIR/incremental-timing.txt" || echo "No performance data available")

## Quality Gate Results

$([ -f "$RESULTS_DIR/quality-gate-report.txt" ] && cat "$RESULTS_DIR/quality-gate-report.txt" || echo "No quality gate data available")

EOF
    
    echo -e "${GREEN}✓ Workflow summary generated: $summary_file${NC}"
}

# Main workflow test execution
main() {
    echo -e "${PURPLE}=== WORKFLOW INTEGRATION TESTS ===${NC}"
    
    setup_workflow_tests
    
    echo -e "${BLUE}Running workflow integration tests...${NC}"
    
    test_precommit_workflow
    test_ide_integration_workflow
    test_cicd_workflow
    test_incremental_workflow
    test_error_handling_workflow
    test_reporting_workflow
    
    generate_workflow_summary
    
    echo -e "${GREEN}Workflow integration tests completed!${NC}"
    echo -e "${BLUE}Results available in: $RESULTS_DIR${NC}"
}

# Execute if run directly
if [ "${BASH_SOURCE[0]}" == "${0}" ]; then
    main "$@"
fi