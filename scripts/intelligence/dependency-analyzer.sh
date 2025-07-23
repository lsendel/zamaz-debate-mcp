#!/bin/bash

# Dependency Analysis System
# Analyzes module dependencies to determine test and build scope

# Analyze dependency impact of changed modules
analyze_dependency_impact() {
    local affected_modules="$1"
    
    log_info "Analyzing dependency impact for modules: ${affected_modules}"
    
    # Parse affected modules from JSON
    local modules
    modules=$(echo "${affected_modules}" | jq -r '.[]' 2>/dev/null || echo "")
    
    if [[ -z "${modules}" ]]; then
        echo "{\"upstream\": [], \"downstream\": [], \"transitive\": []}"
        return
    fi
    
    local upstream_deps=()
    local downstream_deps=()
    local transitive_deps=()
    
    # Analyze each affected module
    while IFS= read -r module; do
        [[ -z "${module}" ]] && continue
        
        log_info "Analyzing dependencies for module: ${module}"
        
        # Get upstream dependencies (what this module depends on)
        local module_upstream
        module_upstream=$(get_upstream_dependencies "${module}")
        
        # Get downstream dependencies (what depends on this module)
        local module_downstream
        module_downstream=$(get_downstream_dependencies "${module}")
        
        # Get transitive dependencies
        local module_transitive
        module_transitive=$(get_transitive_dependencies "${module}")
        
        # Merge results
        upstream_deps=($(merge_json_arrays "$(printf '%s\n' "${upstream_deps[@]}" | jq -R . | jq -s .)" "${module_upstream}"))
        downstream_deps=($(merge_json_arrays "$(printf '%s\n' "${downstream_deps[@]}" | jq -R . | jq -s .)" "${module_downstream}"))
        transitive_deps=($(merge_json_arrays "$(printf '%s\n' "${transitive_deps[@]}" | jq -R . | jq -s .)" "${module_transitive}"))
        
    done <<< "${modules}"
    
    # Build dependency impact result
    jq -n \
        --argjson upstream "$(printf '%s\n' "${upstream_deps[@]}" | jq -R . | jq -s . | jq 'unique')" \
        --argjson downstream "$(printf '%s\n' "${downstream_deps[@]}" | jq -R . | jq -s . | jq 'unique')" \
        --argjson transitive "$(printf '%s\n' "${transitive_deps[@]}" | jq -R . | jq -s . | jq 'unique')" \
        '{
            upstream: $upstream,
            downstream: $downstream,
            transitive: $transitive
        }'
}

# Get upstream dependencies (what this module depends on)
get_upstream_dependencies() {
    local module="$1"
    local deps=()
    
    case "${module}" in
        mcp-*)
            # Java module - check Maven dependencies
            deps+=($(get_maven_upstream_deps "${module}"))
            ;;
        debate-ui)
            # Frontend module - check package.json
            deps+=($(get_npm_upstream_deps "${module}"))
            ;;
        infrastructure)
            # Infrastructure affects all services
            deps+=("mcp-organization" "mcp-gateway" "mcp-debate-engine" "mcp-llm" "mcp-rag" "debate-ui")
            ;;
        ci-cd)
            # CI/CD affects all modules
            deps+=("mcp-organization" "mcp-gateway" "mcp-debate-engine" "mcp-llm" "mcp-rag" "debate-ui" "infrastructure")
            ;;
        config)
            # Configuration affects all services
            deps+=("mcp-organization" "mcp-gateway" "mcp-debate-engine" "mcp-llm" "mcp-rag")
            ;;
    esac
    
    printf '%s\n' "${deps[@]}" | jq -R . | jq -s . | jq 'unique'
}

# Get downstream dependencies (what depends on this module)
get_downstream_dependencies() {
    local module="$1"
    local deps=()
    
    case "${module}" in
        mcp-common)
            # Common module is used by all services
            deps+=("mcp-organization" "mcp-gateway" "mcp-debate-engine" "mcp-llm" "mcp-rag" "mcp-security")
            ;;
        mcp-security)
            # Security module is used by services that need authentication
            deps+=("mcp-organization" "mcp-gateway" "mcp-debate-engine")
            ;;
        mcp-organization)
            # Organization service is used by other services for tenant info
            deps+=("mcp-gateway" "mcp-debate-engine" "debate-ui")
            ;;
        mcp-llm)
            # LLM service is used by debate engine
            deps+=("mcp-debate-engine" "mcp-rag")
            ;;
        mcp-rag)
            # RAG service is used by debate engine
            deps+=("mcp-debate-engine")
            ;;
        mcp-gateway)
            # Gateway is used by frontend
            deps+=("debate-ui")
            ;;
        *)
            # Check Maven dependencies for other modules
            deps+=($(get_maven_downstream_deps "${module}"))
            ;;
    esac
    
    printf '%s\n' "${deps[@]}" | jq -R . | jq -s . | jq 'unique'
}

# Get transitive dependencies
get_transitive_dependencies() {
    local module="$1"
    local deps=()
    
    # Get direct upstream dependencies
    local direct_upstream
    direct_upstream=$(get_upstream_dependencies "${module}")
    
    # For each direct dependency, get its dependencies
    while IFS= read -r dep; do
        [[ -z "${dep}" ]] && continue
        local transitive
        transitive=$(get_upstream_dependencies "${dep}")
        deps+=($(echo "${transitive}" | jq -r '.[]' 2>/dev/null || echo ""))
    done <<< "$(echo "${direct_upstream}" | jq -r '.[]' 2>/dev/null || echo "")"
    
    printf '%s\n' "${deps[@]}" | jq -R . | jq -s . | jq 'unique'
}

# Get Maven upstream dependencies
get_maven_upstream_deps() {
    local module="$1"
    local module_dir="${PROJECT_ROOT}/${module}"
    
    if [[ ! -f "${module_dir}/pom.xml" ]]; then
        echo "[]"
        return
    fi
    
    # Extract internal dependencies from pom.xml
    (cd "${module_dir}" && {
        # Get compile and runtime dependencies that are internal
        mvn dependency:list -DoutputFile=/dev/stdout -Dsilent=true 2>/dev/null | \
            grep "com.zamaz.mcp:" | \
            sed 's/.*com\.zamaz\.mcp:\([^:]*\):.*/\1/' | \
            grep -v "^${module}$" | \
            sort -u | \
            jq -R . | jq -s .
    }) 2>/dev/null || echo "[]"
}

# Get Maven downstream dependencies
get_maven_downstream_deps() {
    local module="$1"
    local deps=()
    
    # Search all pom.xml files for dependencies on this module
    find "${PROJECT_ROOT}" -name "pom.xml" -type f | while read -r pom; do
        if grep -q "com.zamaz.mcp:${module}" "${pom}" 2>/dev/null; then
            local dependent_module
            dependent_module=$(dirname "${pom}" | sed "s|${PROJECT_ROOT}/||" | sed 's|/.*||')
            if [[ "${dependent_module}" != "${module}" ]] && [[ -n "${dependent_module}" ]]; then
                echo "${dependent_module}"
            fi
        fi
    done | sort -u | jq -R . | jq -s .
}

# Get NPM upstream dependencies
get_npm_upstream_deps() {
    local module="$1"
    local package_json="${PROJECT_ROOT}/${module}/package.json"
    
    if [[ ! -f "${package_json}" ]]; then
        echo "[]"
        return
    fi
    
    # Get internal dependencies (if any)
    jq -r '.dependencies // {} | to_entries[] | select(.key | startswith("@zamaz/")) | .key' "${package_json}" 2>/dev/null | \
        jq -R . | jq -s . 2>/dev/null || echo "[]"
}

# Build dependency graph for visualization
build_dependency_graph() {
    local affected_modules="$1"
    
    log_info "Building dependency graph"
    
    local graph_nodes=()
    local graph_edges=()
    
    # Get all modules
    local all_modules
    all_modules=$(parse_maven_modules)
    all_modules=$(echo "${all_modules}" '["debate-ui", "infrastructure", "ci-cd", "config"]' | jq -s 'add | unique')
    
    # Build nodes
    while IFS= read -r module; do
        [[ -z "${module}" ]] && continue
        
        local is_affected="false"
        if echo "${affected_modules}" | jq -e --arg mod "${module}" 'index($mod) != null' >/dev/null 2>&1; then
            is_affected="true"
        fi
        
        graph_nodes+=("$(jq -n --arg id "${module}" --arg affected "${is_affected}" '{id: $id, affected: ($affected == "true")}')")
    done <<< "$(echo "${all_modules}" | jq -r '.[]')"
    
    # Build edges
    while IFS= read -r module; do
        [[ -z "${module}" ]] && continue
        
        local upstream
        upstream=$(get_upstream_dependencies "${module}")
        
        while IFS= read -r dep; do
            [[ -z "${dep}" ]] && continue
            graph_edges+=("$(jq -n --arg from "${dep}" --arg to "${module}" '{from: $from, to: $to}')")
        done <<< "$(echo "${upstream}" | jq -r '.[]' 2>/dev/null || echo "")"
        
    done <<< "$(echo "${all_modules}" | jq -r '.[]')"
    
    # Return graph structure
    jq -n \
        --argjson nodes "$(printf '%s\n' "${graph_nodes[@]}" | jq -s .)" \
        --argjson edges "$(printf '%s\n' "${graph_edges[@]}" | jq -s .)" \
        '{
            nodes: $nodes,
            edges: $edges
        }'
}

# Calculate dependency impact score
calculate_dependency_impact_score() {
    local dependency_impact="$1"
    
    local upstream_count
    upstream_count=$(echo "${dependency_impact}" | jq '.upstream | length')
    
    local downstream_count
    downstream_count=$(echo "${dependency_impact}" | jq '.downstream | length')
    
    local transitive_count
    transitive_count=$(echo "${dependency_impact}" | jq '.transitive | length')
    
    # Calculate weighted score
    local score=$((upstream_count + (downstream_count * 2) + transitive_count))
    
    echo "${score}"
}