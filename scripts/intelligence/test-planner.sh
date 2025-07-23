#!/bin/bash

# Test Plan Generator
# Selects optimal tests based on changes and risk assessment

# Generate test plan based on affected modules and risk level
generate_test_plan() {
    local affected_modules="$1"
    local risk_level="$2"
    local changed_files="$3"
    
    log_info "Generating test plan for risk level: ${risk_level}"
    
    local test_plan
    
    case "${risk_level}" in
        critical)
            test_plan="comprehensive"
            ;;
        high)
            test_plan="extended"
            ;;
        medium)
            test_plan="standard"
            ;;
        low)
            test_plan="minimal"
            ;;
        *)
            test_plan="standard"
            ;;
    esac
    
    # Adjust based on specific patterns
    local change_patterns
    change_patterns=$(identify_change_patterns "${changed_files}")
    
    # Security changes always require comprehensive testing
    if echo "${change_patterns}" | jq -e 'index("security_changes") != null' >/dev/null 2>&1; then
        test_plan="comprehensive"
    fi
    
    # Database changes require extended testing
    if echo "${change_patterns}" | jq -e 'index("database_changes") != null' >/dev/null 2>&1; then
        if [[ "${test_plan}" == "minimal" ]]; then
            test_plan="standard"
        fi
    fi
    
    log_info "Generated test plan: ${test_plan}"
    echo "${test_plan}"
}

# Generate detailed test execution plan
generate_test_execution_plan() {
    local affected_modules="$1"
    local test_plan="$2"
    local changed_files="$3"
    
    log_info "Generating detailed test execution plan"
    
    local unit_tests=()
    local integration_tests=()
    local e2e_tests=()
    local performance_tests=()
    local security_tests=()
    
    # Determine test scope based on test plan
    case "${test_plan}" in
        comprehensive)
            unit_tests+=("all")
            integration_tests+=("all")
            e2e_tests+=("all")
            performance_tests+=("all")
            security_tests+=("all")
            ;;
        extended)
            unit_tests+=($(get_affected_unit_tests "${affected_modules}" "${changed_files}"))
            integration_tests+=($(get_affected_integration_tests "${affected_modules}"))
            e2e_tests+=($(get_critical_e2e_tests "${affected_modules}"))
            security_tests+=($(get_security_tests "${changed_files}"))
            ;;
        standard)
            unit_tests+=($(get_affected_unit_tests "${affected_modules}" "${changed_files}"))
            integration_tests+=($(get_affected_integration_tests "${affected_modules}"))
            ;;
        minimal)
            unit_tests+=($(get_affected_unit_tests "${affected_modules}" "${changed_files}"))
            ;;
    esac
    
    # Build execution plan
    jq -n \
        --arg plan "${test_plan}" \
        --argjson unit_tests "$(printf '%s\n' "${unit_tests[@]}" | jq -R . | jq -s . | jq 'unique')" \
        --argjson integration_tests "$(printf '%s\n' "${integration_tests[@]}" | jq -R . | jq -s . | jq 'unique')" \
        --argjson e2e_tests "$(printf '%s\n' "${e2e_tests[@]}" | jq -R . | jq -s . | jq 'unique')" \
        --argjson performance_tests "$(printf '%s\n' "${performance_tests[@]}" | jq -R . | jq -s . | jq 'unique')" \
        --argjson security_tests "$(printf '%s\n' "${security_tests[@]}" | jq -R . | jq -s . | jq 'unique')" \
        '{
            plan: $plan,
            unit_tests: $unit_tests,
            integration_tests: $integration_tests,
            e2e_tests: $e2e_tests,
            performance_tests: $performance_tests,
            security_tests: $security_tests
        }'
}

# Get affected unit tests
get_affected_unit_tests() {
    local affected_modules="$1"
    local changed_files="$2"
    local tests=()
    
    # Get tests for each affected module
    while IFS= read -r module; do
        [[ -z "${module}" ]] && continue
        
        case "${module}" in
            mcp-*)
                # Java module tests
                tests+=("${module}:test")
                ;;
            debate-ui)
                # Frontend tests
                tests+=("frontend:test")
                ;;
        esac
    done <<< "$(echo "${affected_modules}" | jq -r '.[]' 2>/dev/null || echo "")"
    
    # Get specific test files for changed source files
    while IFS=$'\t' read -r status file; do
        [[ -z "${file}" ]] && continue
        
        local related_tests
        related_tests=$(get_related_test_files "${file}")
        
        while IFS= read -r test_file; do
            [[ -z "${test_file}" ]] && continue
            tests+=("${test_file}")
        done <<< "$(echo "${related_tests}" | jq -r '.[]' 2>/dev/null || echo "")"
        
    done <<< "${changed_files}"
    
    printf '%s\n' "${tests[@]}" | sort -u
}

# Get affected integration tests
get_affected_integration_tests() {
    local affected_modules="$1"
    local tests=()
    
    while IFS= read -r module; do
        [[ -z "${module}" ]] && continue
        
        case "${module}" in
            mcp-organization)
                tests+=("organization-integration-tests")
                tests+=("auth-integration-tests")
                ;;
            mcp-gateway)
                tests+=("gateway-integration-tests")
                tests+=("routing-integration-tests")
                ;;
            mcp-debate-engine)
                tests+=("debate-integration-tests")
                tests+=("workflow-integration-tests")
                ;;
            mcp-llm)
                tests+=("llm-integration-tests")
                tests+=("provider-integration-tests")
                ;;
            mcp-rag)
                tests+=("rag-integration-tests")
                tests+=("vector-db-integration-tests")
                ;;
            mcp-security)
                tests+=("security-integration-tests")
                tests+=("jwt-integration-tests")
                ;;
            debate-ui)
                tests+=("frontend-integration-tests")
                ;;
            infrastructure)
                tests+=("docker-compose-tests")
                tests+=("service-connectivity-tests")
                ;;
        esac
    done <<< "$(echo "${affected_modules}" | jq -r '.[]' 2>/dev/null || echo "")"
    
    printf '%s\n' "${tests[@]}" | sort -u
}

# Get critical end-to-end tests
get_critical_e2e_tests() {
    local affected_modules="$1"
    local tests=()
    
    # Always run core user flows if certain modules are affected
    while IFS= read -r module; do
        [[ -z "${module}" ]] && continue
        
        case "${module}" in
            mcp-organization|mcp-gateway|mcp-security)
                tests+=("user-authentication-flow")
                tests+=("organization-management-flow")
                ;;
            mcp-debate-engine|mcp-llm)
                tests+=("debate-creation-flow")
                tests+=("debate-execution-flow")
                ;;
            debate-ui)
                tests+=("ui-navigation-flow")
                tests+=("user-interaction-flow")
                ;;
            mcp-gateway)
                tests+=("api-gateway-flow")
                ;;
        esac
    done <<< "$(echo "${affected_modules}" | jq -r '.[]' 2>/dev/null || echo "")"
    
    printf '%s\n' "${tests[@]}" | sort -u
}

# Get security tests based on changed files
get_security_tests() {
    local changed_files="$1"
    local tests=()
    
    # Check if security-related files are changed
    while IFS=$'\t' read -r status file; do
        [[ -z "${file}" ]] && continue
        
        case "${file}" in
            */security/*|*/auth/*|*/config/Security*)
                tests+=("authentication-security-tests")
                tests+=("authorization-security-tests")
                tests+=("jwt-security-tests")
                ;;
            */controller/*|*/api/*|*/rest/*)
                tests+=("api-security-tests")
                tests+=("input-validation-tests")
                ;;
            docker-compose*.yml|Dockerfile*)
                tests+=("container-security-tests")
                ;;
            .github/workflows/*)
                tests+=("pipeline-security-tests")
                ;;
        esac
    done <<< "${changed_files}"
    
    printf '%s\n' "${tests[@]}" | sort -u
}

# Generate test parallelization strategy
generate_test_parallelization_strategy() {
    local test_execution_plan="$1"
    
    local parallel_groups=()
    
    # Group 1: Fast unit tests (can run in parallel)
    local unit_tests
    unit_tests=$(echo "${test_execution_plan}" | jq '.unit_tests')
    if [[ "$(echo "${unit_tests}" | jq 'length')" -gt 0 ]]; then
        parallel_groups+=("$(jq -n --argjson tests "${unit_tests}" '{group: "unit", parallel: true, tests: $tests, max_time: "5m"}')")
    fi
    
    # Group 2: Integration tests (limited parallelism due to resource constraints)
    local integration_tests
    integration_tests=$(echo "${test_execution_plan}" | jq '.integration_tests')
    if [[ "$(echo "${integration_tests}" | jq 'length')" -gt 0 ]]; then
        parallel_groups+=("$(jq -n --argjson tests "${integration_tests}" '{group: "integration", parallel: true, max_parallel: 2, tests: $tests, max_time: "15m"}')")
    fi
    
    # Group 3: E2E tests (sequential due to shared resources)
    local e2e_tests
    e2e_tests=$(echo "${test_execution_plan}" | jq '.e2e_tests')
    if [[ "$(echo "${e2e_tests}" | jq 'length')" -gt 0 ]]; then
        parallel_groups+=("$(jq -n --argjson tests "${e2e_tests}" '{group: "e2e", parallel: false, tests: $tests, max_time: "30m"}')")
    fi
    
    # Group 4: Performance tests (sequential, resource intensive)
    local performance_tests
    performance_tests=$(echo "${test_execution_plan}" | jq '.performance_tests')
    if [[ "$(echo "${performance_tests}" | jq 'length')" -gt 0 ]]; then
        parallel_groups+=("$(jq -n --argjson tests "${performance_tests}" '{group: "performance", parallel: false, tests: $tests, max_time: "45m"}')")
    fi
    
    # Group 5: Security tests (can run in parallel with unit tests)
    local security_tests
    security_tests=$(echo "${test_execution_plan}" | jq '.security_tests')
    if [[ "$(echo "${security_tests}" | jq 'length')" -gt 0 ]]; then
        parallel_groups+=("$(jq -n --argjson tests "${security_tests}" '{group: "security", parallel: true, tests: $tests, max_time: "10m"}')")
    fi
    
    printf '%s\n' "${parallel_groups[@]}" | jq -s .
}

# Estimate test execution time
estimate_test_execution_time() {
    local test_execution_plan="$1"
    
    local unit_time=0
    local integration_time=0
    local e2e_time=0
    local performance_time=0
    local security_time=0
    
    # Estimate based on test counts and historical data
    local unit_count
    unit_count=$(echo "${test_execution_plan}" | jq '.unit_tests | length')
    unit_time=$((unit_count * 30))  # 30 seconds per unit test on average
    
    local integration_count
    integration_count=$(echo "${test_execution_plan}" | jq '.integration_tests | length')
    integration_time=$((integration_count * 120))  # 2 minutes per integration test
    
    local e2e_count
    e2e_count=$(echo "${test_execution_plan}" | jq '.e2e_tests | length')
    e2e_time=$((e2e_count * 300))  # 5 minutes per e2e test
    
    local performance_count
    performance_count=$(echo "${test_execution_plan}" | jq '.performance_tests | length')
    performance_time=$((performance_count * 600))  # 10 minutes per performance test
    
    local security_count
    security_count=$(echo "${test_execution_plan}" | jq '.security_tests | length')
    security_time=$((security_count * 180))  # 3 minutes per security test
    
    # Calculate total time considering parallelization
    local parallel_time=$((unit_time > security_time ? unit_time : security_time))
    local sequential_time=$((integration_time + e2e_time + performance_time))
    local total_time=$((parallel_time + sequential_time))
    
    jq -n \
        --arg unit_time "${unit_time}s" \
        --arg integration_time "${integration_time}s" \
        --arg e2e_time "${e2e_time}s" \
        --arg performance_time "${performance_time}s" \
        --arg security_time "${security_time}s" \
        --arg total_time "${total_time}s" \
        '{
            unit_time: $unit_time,
            integration_time: $integration_time,
            e2e_time: $e2e_time,
            performance_time: $performance_time,
            security_time: $security_time,
            total_estimated_time: $total_time
        }'
}