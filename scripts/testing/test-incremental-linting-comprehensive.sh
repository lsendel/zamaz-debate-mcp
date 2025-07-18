#!/bin/bash

# Comprehensive Test Suite for Incremental Linting System
# This script validates all aspects of the incremental linting functionality

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test configuration
TEST_DIR="/tmp/incremental-linting-test-$(date +%s)"
ORIGINAL_DIR=$(pwd)
LINT_CLI=""""$ORIGINAL_DIR"""/mcp-common/target/linting-cli.jar"
TEST_RESULTS_DIR=""""$TEST_DIR"""/test-results"
LOG_FILE=""""$TEST_RESULTS_DIR"""/test.log"

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Helper functions
log() {
    echo -e "${BLUE}[$(date '+%H:%M:%S')]${NC} $1" | tee -a """"$LOG_FILE""""
}

success() {
    echo -e "${GREEN}✓${NC} $1" | tee -a """"$LOG_FILE""""
    ((PASSED_TESTS++))
}

failure() {
    echo -e "${RED}✗${NC} $1" | tee -a """"$LOG_FILE""""
    ((FAILED_TESTS++))
}

warning() {
    echo -e "${YELLOW}⚠${NC} $1" | tee -a """"$LOG_FILE""""
}

run_test() {
    local test_name="$1"
    local test_function="$2"
    
    ((TOTAL_TESTS++))
    log "Running test: """$test_name""""
    
    if """$test_function"""; then
        success """"$test_name""""
    else
        failure """"$test_name""""
    fi
    echo ""
}

# Setup test environment
setup_test_environment() {
    log "Setting up test environment..."
    
    # Create test directory
    mkdir -p """"$TEST_DIR""""
    mkdir -p """"$TEST_RESULTS_DIR""""
    cd """"$TEST_DIR""""
    
    # Initialize git repository
    git init
    git config user.name "Test User"
    git config user.email "test@example.com"
    
    # Copy linting configuration
    cp -r """"$ORIGINAL_DIR"""/.linting" ./ 2>/dev/null || {
        mkdir -p .linting
        cat > .linting/global.yml << EOF
thresholds:
  maxErrors: 0
  maxWarnings: 10
  maxInfo: 50

performance:
  parallelExecution: true
  maxThreads: 2
  cacheEnabled: true

files:
  excludePatterns:
    - "**/test/**"
    - "**/*.tmp"
EOF
    }
    
    # Create sample files for testing
    create_sample_files
    
    success "Test environment setup complete"
}

create_sample_files() {
    log "Creating sample files..."
    
    # Java files
    mkdir -p src/main/java/com/example
    cat > src/main/java/com/example/GoodCode.java << 'EOF'
package com.example;

public class GoodCode {
    private String message;
    
    public GoodCode(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
}
EOF

    cat > src/main/java/com/example/BadCode.java << 'EOF'
package com.example;

public class BadCode {
    public String message;  // Should be private
    
    public BadCode(String message) {
        this.message=message; // Missing space
    }
    
    public String getMessage(){  // Missing space before brace
        return message;
    }
}
EOF

    # TypeScript files
    mkdir -p src/frontend
    cat > src/frontend/good.ts << 'EOF'
export interface User {
    id: number;
    name: string;
    email: string;
}

export class UserService {
    constructor(private apiUrl: string) {}
    
    async getUser(id: number): Promise<User> {
        const response = await fetch($(${this.apiUrl}/users/${id}));
        return response.json();
    }
}
EOF

    cat > src/frontend/bad.ts << 'EOF'
export interface User {
    id: number;
    name: string;
    email: string;
}

export class UserService {
    constructor(private apiUrl: string) {}
    
    async getUser(id: number): Promise<User> {
        const response = await fetch($(${this.apiUrl}/users/${id}));
        return response.json();
    }
    
    // Unused variable
    private unusedVar = "test";
}
EOF

    # YAML files
    cat > config.yml << 'EOF'
app:
  name: "Test App"
  version: "1.0.0"
  database:
    host: "localhost"
    port: 5432
    name: "testdb"
EOF

    cat > bad-config.yml << 'EOF'
app:
  name: "Test App"
  version: "1.0.0"
  database:
    host: "localhost"
    port: 5432
    name: "testdb"
      extra_indent: "bad"  # Incorrect indentation
EOF

    # Markdown files
    cat > README.md << 'EOF'
# Test Project

This is a test project for incremental linting.

## Features

- Java linting
- TypeScript linting
- YAML linting
- Markdown linting
EOF

    cat > BAD-README.md << 'EOF'
# Test Project

This is a test project for incremental linting.

## Features

- Java linting
- TypeScript linting  
- YAML linting
- Markdown linting

[broken link](http://nonexistent-url-12345.com)
EOF

    # Initial commit
    git add .
    git commit -m "Initial commit with sample files"
    
    success "Sample files created"
}

# Test functions

test_basic_incremental_functionality() {
    log "Testing basic incremental functionality..."
    
    # Make a change to a Java file
    echo "    // Added comment" >> src/main/java/com/example/GoodCode.java
    
    # Run incremental linting
    if java -jar """"$LINT_CLI"""" --incremental --working-dir --format json --output incremental-result.json > /dev/null 2>&1; then
        if [ -f incremental-result.json ]; then
            local files_processed=$(jq -r '.filesProcessed // 0' incremental-result.json)
            if [ """"$files_processed"""" -eq 1 ]; then
                return 0
            else
                log "Expected 1 file processed, got """$files_processed""""
                return 1
            fi
        else
            log "No result file generated"
            return 1
        fi
    else
        log "Incremental linting command failed"
        return 1
    fi
}

test_git_diff_integration() {
    log "Testing git diff integration..."
    
    # Create a new commit
    git add .
    git commit -m "Added comment to GoodCode.java"
    
    # Make more changes
    echo "    // Another comment" >> src/main/java/com/example/BadCode.java
    echo "    console.log('debug');" >> src/frontend/good.ts
    
    # Test commit range linting
    if java -jar """"$LINT_CLI"""" --incremental --from-commit HEAD~1 --to-commit HEAD --format json --output git-diff-result.json > /dev/null 2>&1; then
        if [ -f git-diff-result.json ]; then
            return 0
        else
            log "No git diff result file generated"
            return 1
        fi
    else
        log "Git diff linting command failed"
        return 1
    fi
}

test_caching_functionality() {
    log "Testing caching functionality..."
    
    # Clear cache first
    java -jar """"$LINT_CLI"""" --clear-cache > /dev/null 2>&1
    
    # First run (cold cache)
    start_time=$(date +%s%N)
    java -jar """"$LINT_CLI"""" --incremental --working-dir --format json --output cache-cold.json > /dev/null 2>&1
    cold_time=$(($(date +%s%N) - start_time))
    
    # Second run (warm cache) - no file changes
    start_time=$(date +%s%N)
    java -jar """"$LINT_CLI"""" --incremental --working-dir --format json --output cache-warm.json > /dev/null 2>&1
    warm_time=$(($(date +%s%N) - start_time))
    
    # Warm cache should be significantly faster
    if [ """"$warm_time"""" -lt "$((cold_time / 2))" ]; then
        return 0
    else
        log "Cache performance issue: cold="""$cold_time""" ns, warm="""$warm_time""" ns"
        return 1
    fi
}

test_cache_statistics() {
    log "Testing cache statistics..."
    
    # Run cache statistics command
    if java -jar """"$LINT_CLI"""" --cache-stats --format json --output cache-stats.json > /dev/null 2>&1; then
        if [ -f cache-stats.json ]; then
            local total_requests=$(jq -r '.totalRequests // 0' cache-stats.json)
            if [ """"$total_requests"""" -gt 0 ]; then
                return 0
            else
                log "No cache requests recorded"
                return 1
            fi
        else
            log "No cache stats file generated"
            return 1
        fi
    else
        log "Cache statistics command failed"
        return 1
    fi
}

test_parallel_processing() {
    log "Testing parallel processing..."
    
    # Create multiple files to test parallel processing
    for i in {1..10}; do
        cat > "src/main/java/com/example/TestClass"""$i""".java" << EOF
package com.example;

public class TestClass"""$i""" {
    private String value"""$i""";
    
    public TestClass"""$i"""(String value) {
        this.value"""$i""" = value;
    }
    
    public String getValue() {
        return value"""$i""";
    }
}
EOF
    done
    
    # Test with parallel processing enabled
    start_time=$(date +%s%N)
    java -jar """"$LINT_CLI"""" --incremental --working-dir --parallel --threads 4 --format json --output parallel-result.json > /dev/null 2>&1
    parallel_time=$(($(date +%s%N) - start_time))
    
    # Test with parallel processing disabled
    start_time=$(date +%s%N)
    java -jar """"$LINT_CLI"""" --incremental --working-dir --no-parallel --format json --output sequential-result.json > /dev/null 2>&1
    sequential_time=$(($(date +%s%N) - start_time))
    
    if [ -f parallel-result.json ] && [ -f sequential-result.json ]; then
        local parallel_files=$(jq -r '.filesProcessed // 0' parallel-result.json)
        local sequential_files=$(jq -r '.filesProcessed // 0' sequential-result.json)
        
        if [ """"$parallel_files"""" -eq """"$sequential_files"""" ]; then
            return 0
        else
            log "File count mismatch: parallel="""$parallel_files""", sequential="""$sequential_files""""
            return 1
        fi
    else
        log "Missing result files for parallel processing test"
        return 1
    fi
}

test_file_type_detection() {
    log "Testing file type detection..."
    
    # Run linting on specific file types
    java -jar """"$LINT_CLI"""" --incremental --working-dir --files "*.java" --format json --output java-only.json > /dev/null 2>&1
    java -jar """"$LINT_CLI"""" --incremental --working-dir --files "*.ts" --format json --output ts-only.json > /dev/null 2>&1
    java -jar """"$LINT_CLI"""" --incremental --working-dir --files "*.yml" --format json --output yaml-only.json > /dev/null 2>&1
    
    if [ -f java-only.json ] && [ -f ts-only.json ] && [ -f yaml-only.json ]; then
        return 0
    else
        log "File type detection test failed - missing result files"
        return 1
    fi
}

test_quality_thresholds() {
    log "Testing quality thresholds..."
    
    # This should fail due to linting issues in BadCode.java
    if java -jar """"$LINT_CLI"""" --incremental --working-dir --files "src/main/java/com/example/BadCode.java" --format json --output threshold-test.json > /dev/null 2>&1; then
        log "Expected linting to fail due to quality issues"
        return 1
    else
        # Check if the result file shows errors
        if [ -f threshold-test.json ]; then
            local errors=$(jq -r '.errors // 0' threshold-test.json)
            if [ """"$errors"""" -gt 0 ]; then
                return 0
            else
                log "Expected errors but found none"
                return 1
            fi
        else
            log "No threshold test result file"
            return 1
        fi
    fi
}

test_exclude_patterns() {
    log "Testing exclude patterns..."
    
    # Create test files that should be excluded
    mkdir -p test/java/com/example
    cat > test/java/com/example/TestFile.java << 'EOF'
package com.example;

public class TestFile {
    // This is intentionally bad code that should be excluded
    public String badVariable;
}
EOF

    # Create a temporary file that should be excluded
    echo "temporary content" > temp.tmp
    
    # Run linting - excluded files should not be processed
    java -jar """"$LINT_CLI"""" --incremental --working-dir --format json --output exclude-test.json > /dev/null 2>&1
    
    if [ -f exclude-test.json ]; then
        # Check that test files and .tmp files were not processed
        local issues=$(jq -r '.issues[] | select(.file | contains("test/") or endswith(".tmp"))' exclude-test.json)
        if [ -z """"$issues"""" ]; then
            return 0
        else
            log "Excluded files were processed: """$issues""""
            return 1
        fi
    else
        log "No exclude test result file"
        return 1
    fi
}

test_auto_fix_functionality() {
    log "Testing auto-fix functionality..."
    
    # Create a file with fixable issues
    cat > src/main/java/com/example/FixableCode.java << 'EOF'
package com.example;

public class FixableCode {
    private String message;
    
    public FixableCode(String message){  // Missing space before brace
        this.message=message;  // Missing spaces around assignment
    }
    
    public String getMessage(){  // Missing space before brace
        return message;
    }
}
EOF
    
    # Make a backup to compare
    cp src/main/java/com/example/FixableCode.java src/main/java/com/example/FixableCode.java.backup
    
    # Run auto-fix
    java -jar """"$LINT_CLI"""" --incremental --working-dir --auto-fix --files "src/main/java/com/example/FixableCode.java" > /dev/null 2>&1
    
    # Check if file was modified
    if ! diff -q src/main/java/com/example/FixableCode.java src/main/java/com/example/FixableCode.java.backup > /dev/null; then
        return 0
    else
        log "Auto-fix did not modify the file"
        return 1
    fi
}

test_commit_range_validation() {
    log "Testing commit range validation..."
    
    # Test with invalid commit range
    if java -jar """"$LINT_CLI"""" --incremental --from-commit invalid-commit --to-commit HEAD --format json --output invalid-range.json > /dev/null 2>&1; then
        log "Expected failure with invalid commit range"
        return 1
    else
        return 0
    fi
}

test_large_file_handling() {
    log "Testing large file handling..."
    
    # Create a large file
    mkdir -p src/main/java/com/example/large
    {
        echo "package com.example.large;"
        echo "public class LargeFile {"
        for i in {1..1000}; do
            echo "    private String field"""$i""" = \"value"""$i"""\";"
            echo "    public String getField"""$i"""() { return field"""$i"""; }"
            echo "    public void setField"""$i"""(String value) { this.field"""$i""" = value; }"
        done
        echo "}"
    } > src/main/java/com/example/large/LargeFile.java
    
    # Test linting the large file
    start_time=$(date +%s)
    java -jar """"$LINT_CLI"""" --incremental --working-dir --files "src/main/java/com/example/large/LargeFile.java" --format json --output large-file-test.json > /dev/null 2>&1
    end_time=$(date +%s)
    duration=$((end_time - start_time))
    
    if [ -f large-file-test.json ] && [ """"$duration"""" -lt 30 ]; then
        return 0
    else
        log "Large file handling failed or took too long: ${duration}s"
        return 1
    fi
}

test_error_handling() {
    log "Testing error handling..."
    
    # Test with non-existent project directory
    if java -jar """"$LINT_CLI"""" --incremental --project /nonexistent/directory > /dev/null 2>&1; then
        log "Expected failure with non-existent directory"
        return 1
    fi
    
    # Test with corrupted git repository
    rm -rf .git/objects
    if java -jar """"$LINT_CLI"""" --incremental --working-dir > /dev/null 2>&1; then
        log "Expected failure with corrupted git repository"
        return 1
    fi
    
    # Restore git repository for other tests
    cd """"$ORIGINAL_DIR""""
    rm -rf """"$TEST_DIR""""
    setup_test_environment > /dev/null 2>&1
    
    return 0
}

test_performance_metrics() {
    log "Testing performance metrics..."
    
    # Run linting with performance metrics
    java -jar """"$LINT_CLI"""" --incremental --working-dir --format json --output performance-test.json > /dev/null 2>&1
    
    if [ -f performance-test.json ]; then
        local duration=$(jq -r '.durationMs // 0' performance-test.json)
        local files_processed=$(jq -r '.filesProcessed // 0' performance-test.json)
        
        if [ """"$duration"""" -gt 0 ] && [ """"$files_processed"""" -gt 0 ]; then
            return 0
        else
            log "Invalid performance metrics: duration="""$duration""", files="""$files_processed""""
            return 1
        fi
    else
        log "No performance test result file"
        return 1
    fi
}

# CI/CD specific tests
test_ci_cd_integration() {
    log "Testing CI/CD integration..."
    
    # Test with CI environment variables
    export CI=true
    export GITHUB_SHA="$(git rev-parse HEAD)"
    export GITHUB_BASE_REF="main"
    
    # Create a simple CI script
    cat > ci-lint-test.sh << 'EOF'
#!/bin/bash
set -e

# Simulate CI environment
if [ -n """"$CI"""" ]; then
    echo "Running in CI environment"
    # Use fewer threads in CI
    THREADS=2
else
    THREADS=4
fi

# Run incremental linting
java -jar "$1" --incremental --working-dir --parallel --threads """$THREADS""" --format json --output ci-result.json
EOF
    
    chmod +x ci-lint-test.sh
    
    if ./ci-lint-test.sh """"$LINT_CLI"""" > /dev/null 2>&1; then
        if [ -f ci-result.json ]; then
            return 0
        else
            log "No CI result file generated"
            return 1
        fi
    else
        log "CI integration test failed"
        return 1
    fi
}

# Generate comprehensive report
generate_test_report() {
    log "Generating comprehensive test report..."
    
    cat > """"$TEST_RESULTS_DIR"""/test-report.md" << EOF
# Incremental Linting Test Report

**Generated**: $(date)
**Test Environment**: $TEST_DIR
**Total Tests**: $TOTAL_TESTS
**Passed**: $PASSED_TESTS
**Failed**: $FAILED_TESTS
**Success Rate**: $(echo "scale=2; """$PASSED_TESTS""" * 100 / """$TOTAL_TESTS"""" | bc -l)%

## Test Results Summary

EOF

    if [ """$FAILED_TESTS""" -eq 0 ]; then
        echo "✅ **All tests passed!**" >> """"$TEST_RESULTS_DIR"""/test-report.md"
    else
        echo "❌ **Some tests failed**" >> """"$TEST_RESULTS_DIR"""/test-report.md"
    fi
    
    cat >> """"$TEST_RESULTS_DIR"""/test-report.md" << EOF

## Detailed Results

$(cat """"$LOG_FILE"""")

## Performance Analysis

EOF

    # Add performance analysis if available
    if [ -f performance-test.json ]; then
        local duration=$(jq -r '.durationMs // 0' performance-test.json)
        local files=$(jq -r '.filesProcessed // 0' performance-test.json)
        echo "- Average processing time: ${duration}ms for """$files""" files" >> """"$TEST_RESULTS_DIR"""/test-report.md"
        echo "- Processing rate: $(echo "scale=2; """$files""" * 1000 / """$duration"""" | bc -l) files/second" >> """"$TEST_RESULTS_DIR"""/test-report.md"
    fi
    
    cat >> """"$TEST_RESULTS_DIR"""/test-report.md" << EOF

## Test Environment Details

- **Java Version**: $(java -version 2>&1 | head -1)
- **Git Version**: $(git --version)
- **OS**: $(uname -a)
- **Test Directory**: $TEST_DIR
- **Linting CLI**: $LINT_CLI

## Recommendations

EOF

    if [ """$FAILED_TESTS""" -gt 0 ]; then
        cat >> """"$TEST_RESULTS_DIR"""/test-report.md" << EOF
⚠️ **Action Required**: """$FAILED_TESTS""" test(s) failed. Please review the detailed results above and address the issues.

EOF
    fi
    
    echo "✅ Test report generated: """$TEST_RESULTS_DIR"""/test-report.md"
}

# Cleanup function
cleanup() {
    log "Cleaning up test environment..."
    cd """"$ORIGINAL_DIR""""
    # Keep test results but clean up the test environment
    # rm -rf """"$TEST_DIR""""
    warning "Test environment preserved at: """$TEST_DIR""""
    warning "Test results available at: """$TEST_RESULTS_DIR""""
}

# Main execution
main() {
    echo "=========================================="
    echo "Incremental Linting Comprehensive Test Suite"
    echo "=========================================="
    echo ""
    
    # Check prerequisites
    if [ ! -f """"$LINT_CLI"""" ]; then
        failure "Linting CLI not found at """$LINT_CLI""""
        echo "Please build the project first: mvn clean install"
        exit 1
    fi
    
    if ! command -v jq > /dev/null; then
        failure "jq is required for JSON processing"
        echo "Please install jq: apt-get install jq / brew install jq"
        exit 1
    fi
    
    if ! command -v bc > /dev/null; then
        warning "bc not available, some calculations may be skipped"
    fi
    
    # Setup
    setup_test_environment
    
    # Run tests
    echo "Running comprehensive test suite..."
    echo ""
    
    run_test "Basic Incremental Functionality" test_basic_incremental_functionality
    run_test "Git Diff Integration" test_git_diff_integration
    run_test "Caching Functionality" test_caching_functionality
    run_test "Cache Statistics" test_cache_statistics
    run_test "Parallel Processing" test_parallel_processing
    run_test "File Type Detection" test_file_type_detection
    run_test "Quality Thresholds" test_quality_thresholds
    run_test "Exclude Patterns" test_exclude_patterns
    run_test "Auto-fix Functionality" test_auto_fix_functionality
    run_test "Commit Range Validation" test_commit_range_validation
    run_test "Large File Handling" test_large_file_handling
    run_test "Error Handling" test_error_handling
    run_test "Performance Metrics" test_performance_metrics
    run_test "CI/CD Integration" test_ci_cd_integration
    
    # Generate report
    generate_test_report
    
    # Summary
    echo ""
    echo "=========================================="
    echo "Test Suite Summary"
    echo "=========================================="
    echo "Total Tests: """$TOTAL_TESTS""""
    echo "Passed: """$PASSED_TESTS""""
    echo "Failed: """$FAILED_TESTS""""
    echo "Success Rate: $(echo "scale=1; """$PASSED_TESTS""" * 100 / """$TOTAL_TESTS"""" | bc -l 2>/dev/null || echo "N/A")%"
    echo ""
    
    if [ """$FAILED_TESTS""" -eq 0 ]; then
        echo -e "${GREEN}🎉 All tests passed! Incremental linting is working correctly.${NC}"
        echo ""
        echo "Report: """$TEST_RESULTS_DIR"""/test-report.md"
        exit 0
    else
        echo -e "${RED}❌ """$FAILED_TESTS""" test(s) failed. Please review the results.${NC}"
        echo ""
        echo "Report: """$TEST_RESULTS_DIR"""/test-report.md"
        echo "Logs: """$LOG_FILE""""
        exit 1
    fi
}

# Set trap for cleanup
trap cleanup EXIT

# Run main function
main "$@"