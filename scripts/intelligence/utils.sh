#!/bin/bash

# Utility functions for the intelligence system

# Logging functions
log_info() {
    echo "[INFO] $(date '+%Y-%m-%d %H:%M:%S') - $*" >&2
}

log_warn() {
    echo "[WARN] $(date '+%Y-%m-%d %H:%M:%S') - $*" >&2
}

log_error() {
    echo "[ERROR] $(date '+%Y-%m-%d %H:%M:%S') - $*" >&2
}

# Check if array contains element
array_contains() {
    local element="$1"
    shift
    local array=("$@")
    
    for item in "${array[@]}"; do
        [[ "${item}" == "${element}" ]] && return 0
    done
    return 1
}

# Get file extension
get_file_extension() {
    local file="$1"
    echo "${file##*.}"
}

# Check if file exists and is readable
file_readable() {
    local file="$1"
    [[ -f "${file}" && -r "${file}" ]]
}

# Parse Maven modules from pom.xml
parse_maven_modules() {
    local pom_file="${1:-pom.xml}"
    
    if ! file_readable "${pom_file}"; then
        echo "[]"
        return
    fi
    
    # Extract module names from pom.xml
    grep -o '<module>[^<]*</module>' "${pom_file}" 2>/dev/null | \
        sed 's/<module>//g; s/<\/module>//g' | \
        jq -R . | jq -s . 2>/dev/null || echo "[]"
}

# Get service dependencies from Maven
get_maven_dependencies() {
    local module="$1"
    local module_dir="${PROJECT_ROOT}/${module}"
    
    if [[ ! -f "${module_dir}/pom.xml" ]]; then
        echo "[]"
        return
    fi
    
    # Use Maven to get dependency tree
    (cd "${module_dir}" && mvn dependency:tree -DoutputType=text -DoutputFile=/dev/stdout 2>/dev/null | \
        grep "com.zamaz.mcp" | \
        sed 's/.*com\.zamaz\.mcp:\([^:]*\):.*/\1/' | \
        sort -u | \
        jq -R . | jq -s .) 2>/dev/null || echo "[]"
}

# Get package.json dependencies
get_npm_dependencies() {
    local package_json="$1"
    
    if ! file_readable "${package_json}"; then
        echo "[]"
        return
    fi
    
    jq -r '.dependencies // {} | keys[]' "${package_json}" 2>/dev/null | \
        jq -R . | jq -s . 2>/dev/null || echo "[]"
}

# Calculate file change complexity
calculate_change_complexity() {
    local file="$1"
    local base_ref="$2"
    local head_ref="$3"
    
    # Get diff stats
    local stats
    stats=$(git diff --numstat "${base_ref}...${head_ref}" -- "${file}" 2>/dev/null || echo "0	0	${file}")
    
    local added=$(echo "${stats}" | cut -f1)
    local deleted=$(echo "${stats}" | cut -f2)
    
    # Ensure numeric values
    [[ "${added}" =~ ^[0-9]+$ ]] || added=0
    [[ "${deleted}" =~ ^[0-9]+$ ]] || deleted=0
    
    # Simple complexity calculation
    local complexity=$((added + deleted))
    
    # Adjust based on file type
    case "${file}" in
        *.java|*.ts|*.tsx|*.js|*.jsx)
            # Code files have higher impact
            complexity=$((complexity * 2))
            ;;
        *.yml|*.yaml|*.properties|*.json)
            # Configuration files have medium impact
            complexity=$((complexity * 3))
            ;;
        *.md|*.txt|*.html)
            # Documentation has lower impact
            complexity=$((complexity / 2))
            ;;
    esac
    
    echo "${complexity}"
}

# Check if change affects critical paths
is_critical_path() {
    local file="$1"
    
    # Critical paths that require full testing
    local critical_patterns=(
        "*/src/main/java/*/security/*"
        "*/src/main/java/*/config/*"
        "*/src/main/java/*/controller/*"
        "*/Application.java"
        "pom.xml"
        "docker-compose*.yml"
        ".github/workflows/*"
        "*/package.json"
        "*/src/main/resources/application*.yml"
    )
    
    for pattern in "${critical_patterns[@]}"; do
        if [[ "${file}" == ${pattern} ]]; then
            return 0
        fi
    done
    
    return 1
}

# Get test files for a given source file
get_related_test_files() {
    local source_file="$1"
    local test_files=()
    
    # Java test patterns
    if [[ "${source_file}" == *.java ]]; then
        local class_name
        class_name=$(basename "${source_file}" .java)
        local package_path
        package_path=$(dirname "${source_file}" | sed 's|src/main/java|src/test/java|')
        
        # Common test patterns
        local test_patterns=(
            "${package_path}/${class_name}Test.java"
            "${package_path}/${class_name}Tests.java"
            "${package_path}/${class_name}IT.java"
            "${package_path}/${class_name}IntegrationTest.java"
        )
        
        for pattern in "${test_patterns[@]}"; do
            if [[ -f "${pattern}" ]]; then
                test_files+=("${pattern}")
            fi
        done
    fi
    
    # TypeScript/JavaScript test patterns
    if [[ "${source_file}" == *.ts ]] || [[ "${source_file}" == *.tsx ]] || [[ "${source_file}" == *.js ]] || [[ "${source_file}" == *.jsx ]]; then
        local base_name
        base_name=$(basename "${source_file}" | sed 's/\.[^.]*$//')
        local dir_path
        dir_path=$(dirname "${source_file}")
        
        local test_patterns=(
            "${dir_path}/${base_name}.test.ts"
            "${dir_path}/${base_name}.test.tsx"
            "${dir_path}/${base_name}.test.js"
            "${dir_path}/${base_name}.test.jsx"
            "${dir_path}/__tests__/${base_name}.test.ts"
            "${dir_path}/__tests__/${base_name}.test.tsx"
            "${dir_path}/__tests__/${base_name}.test.js"
            "${dir_path}/__tests__/${base_name}.test.jsx"
        )
        
        for pattern in "${test_patterns[@]}"; do
            if [[ -f "${pattern}" ]]; then
                test_files+=("${pattern}")
            fi
        done
    fi
    
    printf '%s\n' "${test_files[@]}" | jq -R . | jq -s .
}

# Validate JSON output
validate_json() {
    local json="$1"
    echo "${json}" | jq . >/dev/null 2>&1
}

# Merge JSON arrays
merge_json_arrays() {
    local arrays=("$@")
    local merged="[]"
    
    for array in "${arrays[@]}"; do
        if validate_json "${array}"; then
            merged=$(echo "${merged}" "${array}" | jq -s 'add | unique')
        fi
    done
    
    echo "${merged}"
}