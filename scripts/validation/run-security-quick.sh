#!/bin/bash

# Quick security validation script for affected modules

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

echo "Running quick security validation for modules: ${AFFECTED_MODULES}"

# Create reports directory
mkdir -p "${PROJECT_ROOT}/target/validation-reports/security"

# Parse affected modules
MODULES=$(echo "${AFFECTED_MODULES}" | jq -r '.[]' 2>/dev/null || echo "")

# Track overall success
OVERALL_SUCCESS=true

# Quick dependency vulnerability check
echo "Running dependency vulnerability check..."
if command -v npm >/dev/null 2>&1; then
    # Check for npm audit if frontend is affected
    if echo "${AFFECTED_MODULES}" | jq -e 'index("debate-ui") != null' >/dev/null 2>&1; then
        cd "${PROJECT_ROOT}/debate-ui"
        if npm audit --audit-level=high --json > "${PROJECT_ROOT}/target/validation-reports/security/npm-audit.json" 2>/dev/null; then
            echo "✅ NPM audit passed for frontend"
        else
            echo "⚠️ NPM audit found vulnerabilities in frontend"
            # Don't fail on npm audit for quick validation
        fi
    fi
fi

# Quick Maven security check for Java modules
while IFS= read -r module; do
    [[ -z "${module}" ]] && continue
    
    if [[ "${module}" == mcp-* ]] && [[ -f "${PROJECT_ROOT}/${module}/pom.xml" ]]; then
        echo "Running quick security check for Java module: ${module}"
        
        cd "${PROJECT_ROOT}/${module}"
        
        # Check for known vulnerable dependencies (quick check)
        if mvn org.owasp:dependency-check-maven:check -DfailBuildOnCVSS=9 -DskipTestScope=true -Dformats=JSON 2>/dev/null; then
            echo "✅ Quick dependency check passed for ${module}"
        else
            echo "⚠️ Quick dependency check found potential issues in ${module}"
            # Don't fail on quick security check
        fi
        
        # Copy security reports if available
        if [[ -d "target/dependency-check-report.json" ]]; then
            cp target/dependency-check-report.json "${PROJECT_ROOT}/target/validation-reports/security/${module}-dependency-check.json" || true
        fi
    fi
done <<< "${MODULES}"

# Quick secrets scan using git-secrets or similar
echo "Running quick secrets scan..."
if command -v git-secrets >/dev/null 2>&1; then
    cd "${PROJECT_ROOT}"
    if git-secrets --scan; then
        echo "✅ No secrets detected"
    else
        echo "❌ Potential secrets detected"
        OVERALL_SUCCESS=false
    fi
else
    # Fallback: simple grep for common secret patterns
    echo "Running basic secret pattern check..."
    SECRET_PATTERNS=(
        "password\s*=\s*['\"][^'\"]{8,}"
        "api[_-]?key\s*=\s*['\"][^'\"]{16,}"
        "secret\s*=\s*['\"][^'\"]{16,}"
        "token\s*=\s*['\"][^'\"]{16,}"
        "-----BEGIN.*PRIVATE KEY-----"
    )
    
    SECRETS_FOUND=false
    for pattern in "${SECRET_PATTERNS[@]}"; do
        if git grep -i -E "${pattern}" -- '*.java' '*.js' '*.ts' '*.tsx' '*.yml' '*.yaml' '*.properties' '*.json' 2>/dev/null; then
            SECRETS_FOUND=true
        fi
    done
    
    if [[ "${SECRETS_FOUND}" == "true" ]]; then
        echo "⚠️ Potential secrets detected - manual review required"
        # Don't fail on quick validation
    else
        echo "✅ No obvious secret patterns detected"
    fi
fi

# Quick container security check if Docker files are affected
if echo "${AFFECTED_MODULES}" | jq -e 'index("infrastructure") != null' >/dev/null 2>&1; then
    echo "Running quick container security check..."
    
    # Check for common Dockerfile security issues
    DOCKERFILE_ISSUES=false
    find "${PROJECT_ROOT}" -name "Dockerfile*" -type f | while read -r dockerfile; do
        echo "Checking ${dockerfile}..."
        
        # Check for running as root
        if ! grep -q "USER " "${dockerfile}"; then
            echo "⚠️ ${dockerfile}: No USER instruction found (may run as root)"
        fi
        
        # Check for COPY with broad permissions
        if grep -q "COPY \. " "${dockerfile}"; then
            echo "⚠️ ${dockerfile}: Copying entire context (potential security risk)"
        fi
        
        # Check for hardcoded secrets
        if grep -i -E "(password|secret|key|token)" "${dockerfile}"; then
            echo "⚠️ ${dockerfile}: Potential hardcoded secrets"
        fi
    done
fi

# Generate summary report
cat > "${PROJECT_ROOT}/target/validation-reports/security/summary.json" << EOF
{
    "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
    "affected_modules": ${AFFECTED_MODULES},
    "overall_success": ${OVERALL_SUCCESS},
    "validation_type": "security-quick",
    "scan_types": ["dependency-check", "secrets-scan", "container-security"]
}
EOF

if [[ "${OVERALL_SUCCESS}" == "true" ]]; then
    echo "✅ Quick security validation completed successfully"
    exit 0
else
    echo "❌ Quick security validation found issues"
    exit 1
fi