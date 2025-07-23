#!/bin/bash

# Lint validation script for affected modules

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

echo "Running lint validation for modules: ${AFFECTED_MODULES}"

# Create reports directory
mkdir -p "${PROJECT_ROOT}/target/validation-reports/lint"

# Parse affected modules
MODULES=$(echo "${AFFECTED_MODULES}" | jq -r '.[]' 2>/dev/null || echo "")

# Track overall success
OVERALL_SUCCESS=true

# Lint Java modules
while IFS= read -r module; do
    [[ -z "${module}" ]] && continue
    
    if [[ "${module}" == mcp-* ]] && [[ -f "${PROJECT_ROOT}/${module}/pom.xml" ]]; then
        echo "Linting Java module: ${module}"
        
        cd "${PROJECT_ROOT}/${module}"
        
        # Run Checkstyle
        if mvn checkstyle:check -Dcheckstyle.failOnViolation=false; then
            echo "✅ Checkstyle passed for ${module}"
        else
            echo "❌ Checkstyle failed for ${module}"
            OVERALL_SUCCESS=false
        fi
        
        # Run SpotBugs
        if mvn spotbugs:check -Dspotbugs.failOnError=false; then
            echo "✅ SpotBugs passed for ${module}"
        else
            echo "❌ SpotBugs failed for ${module}"
            OVERALL_SUCCESS=false
        fi
        
        # Run PMD
        if mvn pmd:check -Dpmd.failOnViolation=false; then
            echo "✅ PMD passed for ${module}"
        else
            echo "❌ PMD failed for ${module}"
            OVERALL_SUCCESS=false
        fi
        
        # Copy reports
        if [[ -d "target/checkstyle-reports" ]]; then
            cp -r target/checkstyle-reports "${PROJECT_ROOT}/target/validation-reports/lint/${module}-checkstyle" || true
        fi
        if [[ -d "target/spotbugs" ]]; then
            cp -r target/spotbugs "${PROJECT_ROOT}/target/validation-reports/lint/${module}-spotbugs" || true
        fi
        if [[ -d "target/pmd" ]]; then
            cp -r target/pmd "${PROJECT_ROOT}/target/validation-reports/lint/${module}-pmd" || true
        fi
    fi
done <<< "${MODULES}"

# Lint frontend if affected
if echo "${AFFECTED_MODULES}" | jq -e 'index("debate-ui") != null' >/dev/null 2>&1; then
    echo "Linting frontend module: debate-ui"
    
    cd "${PROJECT_ROOT}/debate-ui"
    
    if npm run lint; then
        echo "✅ ESLint passed for debate-ui"
    else
        echo "❌ ESLint failed for debate-ui"
        OVERALL_SUCCESS=false
    fi
    
    # Copy lint results if available
    if [[ -f "eslint-report.json" ]]; then
        cp eslint-report.json "${PROJECT_ROOT}/target/validation-reports/lint/frontend-eslint.json" || true
    fi
fi

# Generate summary report
cat > "${PROJECT_ROOT}/target/validation-reports/lint/summary.json" << EOF
{
    "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
    "affected_modules": ${AFFECTED_MODULES},
    "overall_success": ${OVERALL_SUCCESS},
    "validation_type": "lint"
}
EOF

if [[ "${OVERALL_SUCCESS}" == "true" ]]; then
    echo "✅ All lint validations passed"
    exit 0
else
    echo "❌ Some lint validations failed"
    exit 1
fi