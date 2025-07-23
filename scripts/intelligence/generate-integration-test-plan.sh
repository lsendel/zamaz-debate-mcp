#!/bin/bash

# Generate Integration Test Plan
# Creates a detailed test execution plan based on affected modules and risk assessment

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# Source utility functions
source "${SCRIPT_DIR}/utils.sh"
source "${SCRIPT_DIR}/test-planner.sh"

# Parse command line arguments
AFFECTED_MODULES=""
TEST_PLAN=""
RISK_LEVEL=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --modules=*)
            AFFECTED_MODULES="${1#*=}"
            shift
            ;;
        --plan=*)
            TEST_PLAN="${1#*=}"
            shift
            ;;
        --risk=*)
            RISK_LEVEL="${1#*=}"
            shift
            ;;
        *)
            echo "Unknown option $1"
            exit 1
            ;;
    esac
done

# Validate inputs
if [[ -z "${AFFECTED_MODULES}" ]] || [[ -z "${TEST_PLAN}" ]] || [[ -z "${RISK_LEVEL}" ]]; then
    echo "Usage: $0 --modules=<json> --plan=<plan> --risk=<level>"
    exit 1
fi

log_info "Generating integration test plan"
log_info "  Affected modules: ${AFFECTED_MODULES}"
log_info "  Test plan: ${TEST_PLAN}"
log_info "  Risk level: ${RISK_LEVEL}"

# Create output directory
OUTPUT_DIR="${PROJECT_ROOT}/.github/cache/test-plans"
mkdir -p "${OUTPUT_DIR}"

# Generate detailed test execution plan
EXECUTION_PLAN=$(generate_test_execution_plan "${AFFECTED_MODULES}" "${TEST_PLAN}" "")

# Generate parallelization strategy
PARALLELIZATION_STRATEGY=$(generate_test_parallelization_strategy "${EXECUTION_PLAN}")

# Estimate execution time
TIME_ESTIMATE=$(estimate_test_execution_time "${EXECUTION_PLAN}")

# Create comprehensive test plan
TEST_PLAN_FILE="${OUTPUT_DIR}/integration-test-plan-$(date +%Y%m%d-%H%M%S).json"

jq -n \
    --arg timestamp "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    --argjson affected_modules "${AFFECTED_MODULES}" \
    --arg test_plan "${TEST_PLAN}" \
    --arg risk_level "${RISK_LEVEL}" \
    --argjson execution_plan "${EXECUTION_PLAN}" \
    --argjson parallelization_strategy "${PARALLELIZATION_STRATEGY}" \
    --argjson time_estimate "${TIME_ESTIMATE}" \
    '{
        timestamp: $timestamp,
        affected_modules: $affected_modules,
        test_plan: $test_plan,
        risk_level: $risk_level,
        execution_plan: $execution_plan,
        parallelization_strategy: $parallelization_strategy,
        time_estimate: $time_estimate
    }' > "${TEST_PLAN_FILE}"

# Generate test execution scripts
generate_test_execution_scripts "${EXECUTION_PLAN}" "${OUTPUT_DIR}"

# Set output for GitHub Actions
echo "test-plan-file=${TEST_PLAN_FILE}" >> "${GITHUB_OUTPUT:-/dev/stdout}"

log_info "Integration test plan generated: ${TEST_PLAN_FILE}"

# Generate test execution scripts based on the plan
generate_test_execution_scripts() {
    local execution_plan="$1"
    local output_dir="$2"
    
    # Generate script for integration tests
    local integration_tests
    integration_tests=$(echo "${execution_plan}" | jq -r '.integration_tests[]' 2>/dev/null || echo "")
    
    if [[ -n "${integration_tests}" ]]; then
        cat > "${output_dir}/run-integration-tests.sh" << 'EOF'
#!/bin/bash

# Generated Integration Test Execution Script

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# Parse command line arguments
TEST_PLAN_FILE=""
while [[ $# -gt 0 ]]; do
    case $1 in
        --plan=*)
            TEST_PLAN_FILE="${1#*=}"
            shift
            ;;
        *)
            echo "Unknown option $1"
            exit 1
            ;;
    esac
done

if [[ -z "${TEST_PLAN_FILE}" ]] || [[ ! -f "${TEST_PLAN_FILE}" ]]; then
    echo "Error: Test plan file not found: ${TEST_PLAN_FILE}"
    exit 1
fi

echo "Executing integration tests based on plan: ${TEST_PLAN_FILE}"

# Read test plan
INTEGRATION_TESTS=$(jq -r '.execution_plan.integration_tests[]' "${TEST_PLAN_FILE}" 2>/dev/null || echo "")

# Execute integration tests
OVERALL_SUCCESS=true

while IFS= read -r test; do
    [[ -z "${test}" ]] && continue
    
    echo "Running integration test: ${test}"
    
    case "${test}" in
        organization-integration-tests)
            cd "${PROJECT_ROOT}/mcp-organization"
            if mvn verify -Pintegration-tests -Dtest="*IT"; then
                echo "✅ Organization integration tests passed"
            else
                echo "❌ Organization integration tests failed"
                OVERALL_SUCCESS=false
            fi
            ;;
        gateway-integration-tests)
            cd "${PROJECT_ROOT}/mcp-gateway"
            if mvn verify -Pintegration-tests -Dtest="*IT"; then
                echo "✅ Gateway integration tests passed"
            else
                echo "❌ Gateway integration tests failed"
                OVERALL_SUCCESS=false
            fi
            ;;
        debate-integration-tests)
            cd "${PROJECT_ROOT}/mcp-controller"
            if mvn verify -Pintegration-tests -Dtest="*IT"; then
                echo "✅ Debate integration tests passed"
            else
                echo "❌ Debate integration tests failed"
                OVERALL_SUCCESS=false
            fi
            ;;
        llm-integration-tests)
            cd "${PROJECT_ROOT}/mcp-llm"
            if mvn verify -Pintegration-tests -Dtest="*IT"; then
                echo "✅ LLM integration tests passed"
            else
                echo "❌ LLM integration tests failed"
                OVERALL_SUCCESS=false
            fi
            ;;
        rag-integration-tests)
            cd "${PROJECT_ROOT}/mcp-rag"
            if mvn verify -Pintegration-tests -Dtest="*IT"; then
                echo "✅ RAG integration tests passed"
            else
                echo "❌ RAG integration tests failed"
                OVERALL_SUCCESS=false
            fi
            ;;
        security-integration-tests)
            cd "${PROJECT_ROOT}/mcp-security"
            if mvn verify -Pintegration-tests -Dtest="*IT"; then
                echo "✅ Security integration tests passed"
            else
                echo "❌ Security integration tests failed"
                OVERALL_SUCCESS=false
            fi
            ;;
        frontend-integration-tests)
            cd "${PROJECT_ROOT}/debate-ui"
            if npm run test:integration; then
                echo "✅ Frontend integration tests passed"
            else
                echo "❌ Frontend integration tests failed"
                OVERALL_SUCCESS=false
            fi
            ;;
        docker-compose-tests)
            cd "${PROJECT_ROOT}"
            if ./scripts/testing/test-docker-compose.sh; then
                echo "✅ Docker compose tests passed"
            else
                echo "❌ Docker compose tests failed"
                OVERALL_SUCCESS=false
            fi
            ;;
        service-connectivity-tests)
            cd "${PROJECT_ROOT}"
            if ./scripts/testing/test-service-connectivity.sh; then
                echo "✅ Service connectivity tests passed"
            else
                echo "❌ Service connectivity tests failed"
                OVERALL_SUCCESS=false
            fi
            ;;
        *)
            echo "⚠️ Unknown integration test: ${test}"
            ;;
    esac
done <<< "${INTEGRATION_TESTS}"

if [[ "${OVERALL_SUCCESS}" == "true" ]]; then
    echo "✅ All integration tests passed"
    exit 0
else
    echo "❌ Some integration tests failed"
    exit 1
fi
EOF
        chmod +x "${output_dir}/run-integration-tests.sh"
    fi
    
    # Generate script for E2E tests
    local e2e_tests
    e2e_tests=$(echo "${execution_plan}" | jq -r '.e2e_tests[]' 2>/dev/null || echo "")
    
    if [[ -n "${e2e_tests}" ]]; then
        cat > "${output_dir}/run-e2e-tests.sh" << 'EOF'
#!/bin/bash

# Generated E2E Test Execution Script

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# Parse command line arguments
TEST_PLAN_FILE=""
while [[ $# -gt 0 ]]; do
    case $1 in
        --plan=*)
            TEST_PLAN_FILE="${1#*=}"
            shift
            ;;
        *)
            echo "Unknown option $1"
            exit 1
            ;;
    esac
done

if [[ -z "${TEST_PLAN_FILE}" ]] || [[ ! -f "${TEST_PLAN_FILE}" ]]; then
    echo "Error: Test plan file not found: ${TEST_PLAN_FILE}"
    exit 1
fi

echo "Executing E2E tests based on plan: ${TEST_PLAN_FILE}"

# Read test plan
E2E_TESTS=$(jq -r '.execution_plan.e2e_tests[]' "${TEST_PLAN_FILE}" 2>/dev/null || echo "")

# Execute E2E tests
OVERALL_SUCCESS=true

while IFS= read -r test; do
    [[ -z "${test}" ]] && continue
    
    echo "Running E2E test: ${test}"
    
    case "${test}" in
        user-authentication-flow)
            if ./scripts/testing/e2e/test-authentication-flow.sh; then
                echo "✅ User authentication flow test passed"
            else
                echo "❌ User authentication flow test failed"
                OVERALL_SUCCESS=false
            fi
            ;;
        organization-management-flow)
            if ./scripts/testing/e2e/test-organization-flow.sh; then
                echo "✅ Organization management flow test passed"
            else
                echo "❌ Organization management flow test failed"
                OVERALL_SUCCESS=false
            fi
            ;;
        debate-creation-flow)
            if ./scripts/testing/e2e/test-debate-creation-flow.sh; then
                echo "✅ Debate creation flow test passed"
            else
                echo "❌ Debate creation flow test failed"
                OVERALL_SUCCESS=false
            fi
            ;;
        debate-execution-flow)
            if ./scripts/testing/e2e/test-debate-execution-flow.sh; then
                echo "✅ Debate execution flow test passed"
            else
                echo "❌ Debate execution flow test failed"
                OVERALL_SUCCESS=false
            fi
            ;;
        ui-navigation-flow)
            if ./scripts/testing/e2e/test-ui-navigation-flow.sh; then
                echo "✅ UI navigation flow test passed"
            else
                echo "❌ UI navigation flow test failed"
                OVERALL_SUCCESS=false
            fi
            ;;
        user-interaction-flow)
            if ./scripts/testing/e2e/test-user-interaction-flow.sh; then
                echo "✅ User interaction flow test passed"
            else
                echo "❌ User interaction flow test failed"
                OVERALL_SUCCESS=false
            fi
            ;;
        api-gateway-flow)
            if ./scripts/testing/e2e/test-api-gateway-flow.sh; then
                echo "✅ API gateway flow test passed"
            else
                echo "❌ API gateway flow test failed"
                OVERALL_SUCCESS=false
            fi
            ;;
        *)
            echo "⚠️ Unknown E2E test: ${test}"
            ;;
    esac
done <<< "${E2E_TESTS}"

if [[ "${OVERALL_SUCCESS}" == "true" ]]; then
    echo "✅ All E2E tests passed"
    exit 0
else
    echo "❌ Some E2E tests failed"
    exit 1
fi
EOF
        chmod +x "${output_dir}/run-e2e-tests.sh"
    fi
}