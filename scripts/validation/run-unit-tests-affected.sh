#!/bin/bash

# Unit tests validation script for affected modules only

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# Parse command line arguments
AFFECTED_MODULES=""
while [[ $# -gt 0 ]]; do
    case $1 in
        --modules=*)
            AFFECTED_MODULES="${1#*=}"
            shift
            ;;
        *)
            echo "Unknown option $1"
            exit 1
            ;;
    esac
done

echo "Running unit tests for affected modules: ${AFFECTED_MODULES}"

# Create reports directory
mkdir -p "${PROJECT_ROOT}/target/validation-reports/unit-tests"

# Parse affected modules
MODULES=$(echo "${AFFECTED_MODULES}" | jq -r '.[]' 2>/dev/null || echo "")

# Track overall success
OVERALL_SUCCESS=true
TESTS_RUN=0

# Run unit tests for Java modules
while IFS= read -r module; do
    [[ -z "${module}" ]] && continue
    
    if [[ "${module}" == mcp-* ]] && [[ -f "${PROJECT_ROOT}/${module}/pom.xml" ]]; then
        echo "Running unit tests for Java module: ${module}"
        
        cd "${PROJECT_ROOT}/${module}"
        
        # Run unit tests with optimized settings for fast feedback
        if mvn test \
            -Dtest="*Test" \
            -DfailIfNoTests=false \
            -Dmaven.test.failure.ignore=false \
            -Dparallel=methods \
            -DthreadCount=2 \
            -DforkCount=1 \
            -DreuseForks=true \
            -DargLine="-Xmx512m -XX:MaxMetaspaceSize=128m"; then
            echo "✅ Unit tests passed for ${module}"
            TESTS_RUN=$((TESTS_RUN + 1))
        else
            echo "❌ Unit tests failed for ${module}"
            OVERALL_SUCCESS=false
        fi
        
        # Copy test reports
        if [[ -d "target/surefire-reports" ]]; then
            cp -r target/surefire-reports "${PROJECT_ROOT}/target/validation-reports/unit-tests/${module}-surefire" || true
        fi
        
        # Copy coverage reports if available
        if [[ -d "target/site/jacoco" ]]; then
            cp -r target/site/jacoco "${PROJECT_ROOT}/target/validation-reports/unit-tests/${module}-coverage" || true
        fi
    fi
done <<< "${MODULES}"

# Run unit tests for frontend if affected
if echo "${AFFECTED_MODULES}" | jq -e 'index("debate-ui") != null' >/dev/null 2>&1; then
    echo "Running unit tests for frontend module: debate-ui"
    
    cd "${PROJECT_ROOT}/debate-ui"
    
    # Run Jest tests with optimized settings
    if npm test -- \
        --coverage \
        --watchAll=false \
        --testPathPattern=".*\.(test|spec)\.(js|ts|tsx)$" \
        --maxWorkers=2 \
        --silent \
        --reporters=default \
        --reporters=jest-junit; then
        echo "✅ Unit tests passed for debate-ui"
        TESTS_RUN=$((TESTS_RUN + 1))
    else
        echo "❌ Unit tests failed for debate-ui"
        OVERALL_SUCCESS=false
    fi
    
    # Copy test reports
    if [[ -f "junit.xml" ]]; then
        cp junit.xml "${PROJECT_ROOT}/target/validation-reports/unit-tests/frontend-junit.xml" || true
    fi
    
    if [[ -d "coverage" ]]; then
        cp -r coverage "${PROJECT_ROOT}/target/validation-reports/unit-tests/frontend-coverage" || true
    fi
fi

# Generate summary report
cat > "${PROJECT_ROOT}/target/validation-reports/unit-tests/summary.json" << EOF
{
    "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
    "affected_modules": ${AFFECTED_MODULES},
    "overall_success": ${OVERALL_SUCCESS},
    "tests_run": ${TESTS_RUN},
    "validation_type": "unit-tests-affected"
}
EOF

if [[ "${TESTS_RUN}" -eq 0 ]]; then
    echo "⚠️ No unit tests were run (no affected modules with tests found)"
    exit 0
elif [[ "${OVERALL_SUCCESS}" == "true" ]]; then
    echo "✅ All unit tests passed for affected modules (${TESTS_RUN} modules tested)"
    exit 0
else
    echo "❌ Some unit tests failed for affected modules"
    exit 1
fi