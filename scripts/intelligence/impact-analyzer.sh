#!/bin/bash

# Impact Analysis System
# Categorizes changes by risk level based on multiple factors

# Assess risk level of changes
assess_risk_level() {
    local changed_files="$1"
    local affected_modules="$2"
    local dependency_impact="$3"
    
    log_info "Assessing risk level for changes"
    
    local risk_factors=()
    local risk_score=0
    
    # Analyze file-level risk factors
    while IFS=$'\t' read -r status file; do
        [[ -z "${file}" ]] && continue
        
        local file_risk
        file_risk=$(assess_file_risk "${status}" "${file}")
        risk_score=$((risk_score + file_risk))
        
        # Track specific risk factors
        if [[ "${file_risk}" -gt 5 ]]; then
            risk_factors+=("High-risk file: ${file}")
        fi
        
    done <<< "${changed_files}"
    
    # Analyze module-level risk factors
    local module_count
    module_count=$(echo "${affected_modules}" | jq 'length')
    
    if [[ "${module_count}" -gt 3 ]]; then
        risk_score=$((risk_score + 10))
        risk_factors+=("Multiple modules affected: ${module_count}")
    fi
    
    # Analyze dependency impact risk
    local dependency_score
    dependency_score=$(calculate_dependency_impact_score "${dependency_impact}")
    risk_score=$((risk_score + dependency_score))
    
    if [[ "${dependency_score}" -gt 5 ]]; then
        risk_factors+=("High dependency impact: ${dependency_score}")
    fi
    
    # Check for critical module changes
    local critical_modules=("mcp-security" "mcp-gateway" "mcp-organization" "infrastructure" "ci-cd")
    for critical in "${critical_modules[@]}"; do
        if echo "${affected_modules}" | jq -e --arg mod "${critical}" 'index($mod) != null' >/dev/null 2>&1; then
            risk_score=$((risk_score + 15))
            risk_factors+=("Critical module affected: ${critical}")
        fi
    done
    
    # Determine risk level based on score
    local risk_level
    if [[ "${risk_score}" -lt 10 ]]; then
        risk_level="low"
    elif [[ "${risk_score}" -lt 25 ]]; then
        risk_level="medium"
    elif [[ "${risk_score}" -lt 50 ]]; then
        risk_level="high"
    else
        risk_level="critical"
    fi
    
    log_info "Risk assessment complete:"
    log_info "  Risk score: ${risk_score}"
    log_info "  Risk level: ${risk_level}"
    for factor in "${risk_factors[@]}"; do
        log_info "  Risk factor: ${factor}"
    done
    
    echo "${risk_level}"
}

# Assess risk level for individual file changes
assess_file_risk() {
    local status="$1"
    local file="$2"
    local risk=0
    
    # Base risk by change type
    case "${status}" in
        A)  # Added
            risk=2
            ;;
        M)  # Modified
            risk=1
            ;;
        D)  # Deleted
            risk=3
            ;;
        R*)  # Renamed
            risk=2
            ;;
        C*)  # Copied
            risk=1
            ;;
        *)
            risk=1
            ;;
    esac
    
    # Adjust risk based on file type and location
    case "${file}" in
        # Critical configuration files
        pom.xml|*/pom.xml)
            risk=$((risk * 5))
            ;;
        docker-compose*.yml|Dockerfile*|*/Dockerfile*)
            risk=$((risk * 4))
            ;;
        .github/workflows/*)
            risk=$((risk * 4))
            ;;
        */src/main/resources/application*.yml|*/src/main/resources/application*.properties)
            risk=$((risk * 4))
            ;;
        
        # Security-related files
        */security/*|*/auth/*|*/config/Security*)
            risk=$((risk * 6))
            ;;
        
        # Core business logic
        */controller/*|*/service/*|*/repository/*)
            risk=$((risk * 3))
            ;;
        
        # Database migrations
        */db/migration/*|*/flyway/*)
            risk=$((risk * 4))
            ;;
        
        # Frontend critical files
        */src/App.tsx|*/src/index.tsx|*/package.json)
            risk=$((risk * 3))
            ;;
        
        # Infrastructure as code
        k8s/*|helm/*|terraform/*)
            risk=$((risk * 4))
            ;;
        
        # Test files (lower risk)
        */test/*|*/tests/*|*Test.java|*Tests.java|*.test.ts|*.test.js)
            risk=$((risk / 2))
            ;;
        
        # Documentation (lowest risk)
        *.md|*.txt|docs/*)
            risk=$((risk / 3))
            ;;
    esac
    
    # Calculate change complexity
    local complexity
    complexity=$(calculate_change_complexity "${file}" "${GITHUB_BASE_REF:-main}" "${GITHUB_HEAD_REF:-HEAD}")
    
    # Adjust risk based on complexity
    if [[ "${complexity}" -gt 100 ]]; then
        risk=$((risk * 2))
    elif [[ "${complexity}" -gt 50 ]]; then
        risk=$((risk * 3 / 2))
    fi
    
    # Check if it's a critical path
    if is_critical_path "${file}"; then
        risk=$((risk * 2))
    fi
    
    echo "${risk}"
}

# Analyze blast radius of changes
analyze_blast_radius() {
    local affected_modules="$1"
    local dependency_impact="$2"
    
    log_info "Analyzing blast radius"
    
    local directly_affected
    directly_affected=$(echo "${affected_modules}" | jq 'length')
    
    local upstream_affected
    upstream_affected=$(echo "${dependency_impact}" | jq '.upstream | length')
    
    local downstream_affected
    downstream_affected=$(echo "${dependency_impact}" | jq '.downstream | length')
    
    local transitive_affected
    transitive_affected=$(echo "${dependency_impact}" | jq '.transitive | length')
    
    local total_affected=$((directly_affected + upstream_affected + downstream_affected + transitive_affected))
    
    # Categorize blast radius
    local blast_radius
    if [[ "${total_affected}" -lt 3 ]]; then
        blast_radius="small"
    elif [[ "${total_affected}" -lt 6 ]]; then
        blast_radius="medium"
    elif [[ "${total_affected}" -lt 10 ]]; then
        blast_radius="large"
    else
        blast_radius="extensive"
    fi
    
    jq -n \
        --arg radius "${blast_radius}" \
        --arg directly_affected "${directly_affected}" \
        --arg upstream_affected "${upstream_affected}" \
        --arg downstream_affected "${downstream_affected}" \
        --arg transitive_affected "${transitive_affected}" \
        --arg total_affected "${total_affected}" \
        '{
            radius: $radius,
            directly_affected: ($directly_affected | tonumber),
            upstream_affected: ($upstream_affected | tonumber),
            downstream_affected: ($downstream_affected | tonumber),
            transitive_affected: ($transitive_affected | tonumber),
            total_affected: ($total_affected | tonumber)
        }'
}

# Identify change patterns that indicate specific risks
identify_change_patterns() {
    local changed_files="$1"
    local patterns=()
    
    local has_security_changes=false
    local has_config_changes=false
    local has_database_changes=false
    local has_api_changes=false
    local has_infrastructure_changes=false
    local has_dependency_changes=false
    
    # Analyze files for patterns
    while IFS=$'\t' read -r status file; do
        [[ -z "${file}" ]] && continue
        
        case "${file}" in
            */security/*|*/auth/*|*/config/Security*)
                has_security_changes=true
                ;;
            */application*.yml|*/application*.properties|pom.xml|*/pom.xml|package.json)
                has_config_changes=true
                ;;
            */db/migration/*|*/flyway/*|*schema*.sql)
                has_database_changes=true
                ;;
            */controller/*|*/api/*|*/rest/*)
                has_api_changes=true
                ;;
            docker-compose*.yml|Dockerfile*|k8s/*|helm/*|.github/workflows/*)
                has_infrastructure_changes=true
                ;;
            pom.xml|package.json|package-lock.json|yarn.lock)
                has_dependency_changes=true
                ;;
        esac
    done <<< "${changed_files}"
    
    # Build pattern analysis
    [[ "${has_security_changes}" == true ]] && patterns+=("security_changes")
    [[ "${has_config_changes}" == true ]] && patterns+=("configuration_changes")
    [[ "${has_database_changes}" == true ]] && patterns+=("database_changes")
    [[ "${has_api_changes}" == true ]] && patterns+=("api_changes")
    [[ "${has_infrastructure_changes}" == true ]] && patterns+=("infrastructure_changes")
    [[ "${has_dependency_changes}" == true ]] && patterns+=("dependency_changes")
    
    printf '%s\n' "${patterns[@]}" | jq -R . | jq -s .
}

# Generate risk mitigation recommendations
generate_risk_mitigation() {
    local risk_level="$1"
    local change_patterns="$2"
    local blast_radius="$3"
    
    local recommendations=()
    
    # Base recommendations by risk level
    case "${risk_level}" in
        critical)
            recommendations+=("Require manual approval from senior engineer")
            recommendations+=("Run full test suite including performance tests")
            recommendations+=("Deploy to staging environment first")
            recommendations+=("Monitor deployment closely with rollback plan ready")
            ;;
        high)
            recommendations+=("Require code review from team lead")
            recommendations+=("Run comprehensive test suite")
            recommendations+=("Deploy during low-traffic hours")
            ;;
        medium)
            recommendations+=("Require standard code review")
            recommendations+=("Run affected module tests plus integration tests")
            ;;
        low)
            recommendations+=("Run affected module tests")
            recommendations+=("Standard automated deployment")
            ;;
    esac
    
    # Pattern-specific recommendations
    while IFS= read -r pattern; do
        [[ -z "${pattern}" ]] && continue
        
        case "${pattern}" in
            security_changes)
                recommendations+=("Run security scan and penetration tests")
                recommendations+=("Review authentication and authorization flows")
                ;;
            database_changes)
                recommendations+=("Verify database migration rollback procedures")
                recommendations+=("Test with production-like data volume")
                ;;
            api_changes)
                recommendations+=("Verify API backward compatibility")
                recommendations+=("Update API documentation")
                ;;
            infrastructure_changes)
                recommendations+=("Test infrastructure changes in staging")
                recommendations+=("Verify monitoring and alerting")
                ;;
            dependency_changes)
                recommendations+=("Run security vulnerability scan")
                recommendations+=("Verify license compatibility")
                ;;
        esac
    done <<< "$(echo "${change_patterns}" | jq -r '.[]' 2>/dev/null || echo "")"
    
    # Blast radius specific recommendations
    local radius
    radius=$(echo "${blast_radius}" | jq -r '.radius')
    
    case "${radius}" in
        extensive)
            recommendations+=("Consider breaking changes into smaller increments")
            recommendations+=("Coordinate with all affected teams")
            ;;
        large)
            recommendations+=("Notify affected teams before deployment")
            ;;
    esac
    
    printf '%s\n' "${recommendations[@]}" | jq -R . | jq -s . | jq 'unique'
}