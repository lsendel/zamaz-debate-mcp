#!/bin/bash

# Intelligent Change Detection and Impact Analysis System
# This script analyzes Git changes and determines affected modules, test scope, and risk level

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
INTELLIGENCE_DIR="${SCRIPT_DIR}"
CACHE_DIR="${PROJECT_ROOT}/.github/cache/intelligence"

# Create cache directory if it doesn't exist
mkdir -p "${CACHE_DIR}"

# Source utility functions
source "${INTELLIGENCE_DIR}/utils.sh"
source "${INTELLIGENCE_DIR}/dependency-analyzer.sh"
source "${INTELLIGENCE_DIR}/impact-analyzer.sh"
source "${INTELLIGENCE_DIR}/test-planner.sh"

# Main function
main() {
    log_info "Starting intelligent change detection and impact analysis..."
    
    # Get Git changes
    local base_ref="${GITHUB_BASE_REF:-main}"
    local head_ref="${GITHUB_HEAD_REF:-HEAD}"
    
    log_info "Analyzing changes between ${base_ref} and ${head_ref}"
    
    # Detect changed files
    local changed_files
    changed_files=$(detect_changed_files "${base_ref}" "${head_ref}")
    
    if [[ -z "${changed_files}" ]]; then
        log_info "No changes detected"
        set_output "affected-modules" "[]"
        set_output "test-plan" "minimal"
        set_output "risk-level" "low"
        exit 0
    fi
    
    log_info "Changed files:"
    echo "${changed_files}" | while read -r file; do
        log_info "  - ${file}"
    done
    
    # Analyze affected modules
    local affected_modules
    affected_modules=$(analyze_affected_modules "${changed_files}")
    
    # Perform dependency analysis
    local dependency_impact
    dependency_impact=$(analyze_dependency_impact "${affected_modules}")
    
    # Assess risk level
    local risk_level
    risk_level=$(assess_risk_level "${changed_files}" "${affected_modules}" "${dependency_impact}")
    
    # Generate test plan
    local test_plan
    test_plan=$(generate_test_plan "${affected_modules}" "${risk_level}" "${changed_files}")
    
    # Output results
    log_info "Analysis complete:"
    log_info "  Affected modules: ${affected_modules}"
    log_info "  Risk level: ${risk_level}"
    log_info "  Test plan: ${test_plan}"
    
    # Set GitHub Actions outputs
    set_output "affected-modules" "${affected_modules}"
    set_output "test-plan" "${test_plan}"
    set_output "risk-level" "${risk_level}"
    
    # Cache results for future use
    cache_analysis_results "${affected_modules}" "${risk_level}" "${test_plan}" "${changed_files}"
    
    log_info "Intelligence analysis completed successfully"
}

# Detect changed files between two refs
detect_changed_files() {
    local base_ref="$1"
    local head_ref="$2"
    
    # Get changed files with change type
    git diff --name-status "${base_ref}...${head_ref}" 2>/dev/null || {
        log_warn "Failed to get git diff, falling back to current changes"
        git diff --name-status HEAD~1 2>/dev/null || echo ""
    }
}

# Analyze which modules are affected by the changes
analyze_affected_modules() {
    local changed_files="$1"
    local modules=()
    
    # Java services
    local java_services=(
        "mcp-common"
        "mcp-security" 
        "mcp-auth-server"
        "mcp-config-server"
        "mcp-organization"
        "mcp-debate-engine"
        "mcp-llm"
        "mcp-rag"
        "mcp-docs"
        "mcp-gateway"
        "mcp-testing"
        "mcp-pattern-recognition"
        "github-integration"
        "workflow-editor"
        "mcp-context"
        "mcp-controller"
        "mcp-debate"
        "mcp-template"
        "mcp-context-client"
        "mcp-modulith"
    )
    
    # Check each changed file against module patterns
    while IFS=$'\t' read -r status file; do
        [[ -z "${file}" ]] && continue
        
        # Check Java services
        for service in "${java_services[@]}"; do
            if [[ "${file}" == "${service}/"* ]]; then
                if ! array_contains "${service}" "${modules[@]}"; then
                    modules+=("${service}")
                fi
            fi
        done
        
        # Check frontend
        if [[ "${file}" == "debate-ui/"* ]]; then
            if ! array_contains "debate-ui" "${modules[@]}"; then
                modules+=("debate-ui")
            fi
        fi
        
        # Check infrastructure
        if [[ "${file}" == "docker-compose"* ]] || [[ "${file}" == "Dockerfile"* ]] || [[ "${file}" == "k8s/"* ]] || [[ "${file}" == "helm/"* ]]; then
            if ! array_contains "infrastructure" "${modules[@]}"; then
                modules+=("infrastructure")
            fi
        fi
        
        # Check CI/CD
        if [[ "${file}" == ".github/"* ]] || [[ "${file}" == "scripts/"* ]] || [[ "${file}" == "Makefile" ]]; then
            if ! array_contains "ci-cd" "${modules[@]}"; then
                modules+=("ci-cd")
            fi
        fi
        
        # Check root configuration
        if [[ "${file}" == "pom.xml" ]] || [[ "${file}" == "*.properties" ]] || [[ "${file}" == "*.yml" ]] || [[ "${file}" == "*.yaml" ]]; then
            if ! array_contains "config" "${modules[@]}"; then
                modules+=("config")
            fi
        fi
        
    done <<< "${changed_files}"
    
    # Convert array to JSON
    printf '%s\n' "${modules[@]}" | jq -R . | jq -s .
}

# Cache analysis results for future reference
cache_analysis_results() {
    local affected_modules="$1"
    local risk_level="$2"
    local test_plan="$3"
    local changed_files="$4"
    
    local cache_file="${CACHE_DIR}/analysis-$(date +%Y%m%d-%H%M%S).json"
    
    jq -n \
        --arg timestamp "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
        --arg commit "${GITHUB_SHA:-$(git rev-parse HEAD)}" \
        --argjson affected_modules "${affected_modules}" \
        --arg risk_level "${risk_level}" \
        --arg test_plan "${test_plan}" \
        --arg changed_files "${changed_files}" \
        '{
            timestamp: $timestamp,
            commit: $commit,
            affected_modules: $affected_modules,
            risk_level: $risk_level,
            test_plan: $test_plan,
            changed_files: $changed_files
        }' > "${cache_file}"
    
    log_info "Analysis results cached to ${cache_file}"
}

# Set GitHub Actions output
set_output() {
    local name="$1"
    local value="$2"
    
    if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
        echo "${name}=${value}" >> "${GITHUB_OUTPUT}"
    else
        echo "::set-output name=${name}::${value}"
    fi
}

# Run main function
main "$@"