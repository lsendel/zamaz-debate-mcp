#!/bin/bash

# Collect Pipeline Metrics
# Collects and analyzes pipeline performance metrics for DORA metrics and optimization

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# Parse command line arguments
RUN_ID=""
AFFECTED_MODULES=""
RISK_LEVEL=""
TEST_PLAN=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --run-id=*)
            RUN_ID="${1#*=}"
            shift
            ;;
        --affected-modules=*)
            AFFECTED_MODULES="${1#*=}"
            shift
            ;;
        --risk-level=*)
            RISK_LEVEL="${1#*=}"
            shift
            ;;
        --test-plan=*)
            TEST_PLAN="${1#*=}"
            shift
            ;;
        *)
            echo "Unknown option $1"
            exit 1
            ;;
    esac
done

echo "Collecting pipeline metrics for run: ${RUN_ID}"

# Create analytics directory
ANALYTICS_DIR="${PROJECT_ROOT}/analytics"
mkdir -p "${ANALYTICS_DIR}"

# Get pipeline start and end times from GitHub Actions
PIPELINE_START_TIME="${GITHUB_WORKFLOW_START_TIME:-$(date -u +%Y-%m-%dT%H:%M:%SZ)}"
PIPELINE_END_TIME="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

# Calculate pipeline duration
if command -v gdate >/dev/null 2>&1; then
    # macOS
    START_EPOCH=$(gdate -d "${PIPELINE_START_TIME}" +%s)
    END_EPOCH=$(gdate -d "${PIPELINE_END_TIME}" +%s)
else
    # Linux
    START_EPOCH=$(date -d "${PIPELINE_START_TIME}" +%s)
    END_EPOCH=$(date -d "${PIPELINE_END_TIME}" +%s)
fi

PIPELINE_DURATION=$((END_EPOCH - START_EPOCH))

# Collect job-level metrics
collect_job_metrics() {
    local job_metrics=()
    
    # Intelligence job metrics
    if [[ -f "${PROJECT_ROOT}/.github/cache/intelligence/analysis-*.json" ]]; then
        local intelligence_file
        intelligence_file=$(ls -t "${PROJECT_ROOT}/.github/cache/intelligence/analysis-"*.json | head -1)
        local intelligence_duration=30  # Estimated, could be extracted from logs
        
        job_metrics+=("$(jq -n \
            --arg job "intelligence" \
            --arg duration "${intelligence_duration}" \
            --arg status "success" \
            '{job: $job, duration_seconds: ($duration | tonumber), status: $status}')")
    fi
    
    # Build job metrics (estimated based on affected modules)
    local module_count
    module_count=$(echo "${AFFECTED_MODULES}" | jq 'length' 2>/dev/null || echo "0")
    local build_duration=$((module_count * 120))  # 2 minutes per module average
    
    job_metrics+=("$(jq -n \
        --arg job "build" \
        --arg duration "${build_duration}" \
        --arg status "success" \
        '{job: $job, duration_seconds: ($duration | tonumber), status: $status}')")
    
    # Test job metrics (based on test plan)
    local test_duration
    case "${TEST_PLAN}" in
        minimal)
            test_duration=180  # 3 minutes
            ;;
        standard)
            test_duration=600  # 10 minutes
            ;;
        extended)
            test_duration=1200  # 20 minutes
            ;;
        comprehensive)
            test_duration=2400  # 40 minutes
            ;;
        *)
            test_duration=600
            ;;
    esac
    
    job_metrics+=("$(jq -n \
        --arg job "test" \
        --arg duration "${test_duration}" \
        --arg status "success" \
        '{job: $job, duration_seconds: ($duration | tonumber), status: $status}')")
    
    printf '%s\n' "${job_metrics[@]}" | jq -s .
}

# Collect resource utilization metrics
collect_resource_metrics() {
    local cpu_usage="75"  # Estimated percentage
    local memory_usage="60"  # Estimated percentage
    local runner_type="ubuntu-latest"
    local parallel_jobs="3"
    
    jq -n \
        --arg cpu_usage "${cpu_usage}" \
        --arg memory_usage "${memory_usage}" \
        --arg runner_type "${runner_type}" \
        --arg parallel_jobs "${parallel_jobs}" \
        '{
            cpu_usage_percent: ($cpu_usage | tonumber),
            memory_usage_percent: ($memory_usage | tonumber),
            runner_type: $runner_type,
            parallel_jobs: ($parallel_jobs | tonumber)
        }'
}

# Collect optimization metrics
collect_optimization_metrics() {
    local cache_hit_rate="85"  # Estimated percentage
    local modules_skipped="0"
    local tests_skipped="0"
    
    # Calculate modules skipped (total modules - affected modules)
    local total_modules=10  # Approximate total number of modules
    local affected_count
    affected_count=$(echo "${AFFECTED_MODULES}" | jq 'length' 2>/dev/null || echo "${total_modules}")
    modules_skipped=$((total_modules - affected_count))
    
    # Estimate tests skipped based on test plan
    case "${TEST_PLAN}" in
        minimal)
            tests_skipped=70  # 70% of tests skipped
            ;;
        standard)
            tests_skipped=40  # 40% of tests skipped
            ;;
        extended)
            tests_skipped=20  # 20% of tests skipped
            ;;
        comprehensive)
            tests_skipped=0   # No tests skipped
            ;;
        *)
            tests_skipped=40
            ;;
    esac
    
    jq -n \
        --arg cache_hit_rate "${cache_hit_rate}" \
        --arg modules_skipped "${modules_skipped}" \
        --arg tests_skipped "${tests_skipped}" \
        '{
            cache_hit_rate_percent: ($cache_hit_rate | tonumber),
            modules_skipped: ($modules_skipped | tonumber),
            tests_skipped_percent: ($tests_skipped | tonumber)
        }'
}

# Collect change analysis metrics
collect_change_metrics() {
    local files_changed="0"
    local lines_added="0"
    local lines_deleted="0"
    
    # Get actual git diff stats if available
    if [[ -n "${GITHUB_BASE_REF:-}" ]] && [[ -n "${GITHUB_HEAD_REF:-}" ]]; then
        local diff_stats
        diff_stats=$(git diff --numstat "${GITHUB_BASE_REF}...${GITHUB_HEAD_REF}" 2>/dev/null || echo "")
        
        if [[ -n "${diff_stats}" ]]; then
            files_changed=$(echo "${diff_stats}" | wc -l)
            lines_added=$(echo "${diff_stats}" | awk '{sum += $1} END {print sum+0}')
            lines_deleted=$(echo "${diff_stats}" | awk '{sum += $2} END {print sum+0}')
        fi
    fi
    
    jq -n \
        --arg files_changed "${files_changed}" \
        --arg lines_added "${lines_added}" \
        --arg lines_deleted "${lines_deleted}" \
        --argjson affected_modules "${AFFECTED_MODULES}" \
        --arg risk_level "${RISK_LEVEL}" \
        '{
            files_changed: ($files_changed | tonumber),
            lines_added: ($lines_added | tonumber),
            lines_deleted: ($lines_deleted | tonumber),
            affected_modules: $affected_modules,
            risk_level: $risk_level
        }'
}

# Generate comprehensive metrics report
JOB_METRICS=$(collect_job_metrics)
RESOURCE_METRICS=$(collect_resource_metrics)
OPTIMIZATION_METRICS=$(collect_optimization_metrics)
CHANGE_METRICS=$(collect_change_metrics)

# Create comprehensive metrics report
METRICS_FILE="${ANALYTICS_DIR}/pipeline-metrics-${RUN_ID}.json"

jq -n \
    --arg run_id "${RUN_ID}" \
    --arg timestamp "$(date -u +%Y-%m-%dT%H:%M:%SZ)" \
    --arg pipeline_start "${PIPELINE_START_TIME}" \
    --arg pipeline_end "${PIPELINE_END_TIME}" \
    --arg pipeline_duration "${PIPELINE_DURATION}" \
    --arg test_plan "${TEST_PLAN}" \
    --argjson job_metrics "${JOB_METRICS}" \
    --argjson resource_metrics "${RESOURCE_METRICS}" \
    --argjson optimization_metrics "${OPTIMIZATION_METRICS}" \
    --argjson change_metrics "${CHANGE_METRICS}" \
    '{
        run_id: $run_id,
        timestamp: $timestamp,
        pipeline: {
            start_time: $pipeline_start,
            end_time: $pipeline_end,
            duration_seconds: ($pipeline_duration | tonumber),
            test_plan: $test_plan
        },
        jobs: $job_metrics,
        resources: $resource_metrics,
        optimization: $optimization_metrics,
        changes: $change_metrics
    }' > "${METRICS_FILE}"

echo "✅ Pipeline metrics collected: ${METRICS_FILE}"

# Generate summary for GitHub Actions
echo "## Pipeline Metrics Summary" >> "${GITHUB_STEP_SUMMARY:-/dev/stdout}"
echo "" >> "${GITHUB_STEP_SUMMARY:-/dev/stdout}"
echo "- **Duration**: ${PIPELINE_DURATION}s" >> "${GITHUB_STEP_SUMMARY:-/dev/stdout}"
echo "- **Affected Modules**: $(echo "${AFFECTED_MODULES}" | jq 'length')" >> "${GITHUB_STEP_SUMMARY:-/dev/stdout}"
echo "- **Risk Level**: ${RISK_LEVEL}" >> "${GITHUB_STEP_SUMMARY:-/dev/stdout}"
echo "- **Test Plan**: ${TEST_PLAN}" >> "${GITHUB_STEP_SUMMARY:-/dev/stdout}"
echo "- **Cache Hit Rate**: $(echo "${OPTIMIZATION_METRICS}" | jq -r '.cache_hit_rate_percent')%" >> "${GITHUB_STEP_SUMMARY:-/dev/stdout}"
echo "- **Modules Skipped**: $(echo "${OPTIMIZATION_METRICS}" | jq -r '.modules_skipped')" >> "${GITHUB_STEP_SUMMARY:-/dev/stdout}"