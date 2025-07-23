#!/bin/bash

# Update DORA Metrics
# Updates DORA (DevOps Research and Assessment) metrics based on pipeline execution

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# Parse command line arguments
DEPLOYMENT_SUCCESS=""
LEAD_TIME_START=""
COMMIT_SHA=""

while [[ $# -gt 0 ]]; do
    case $1 in
        --deployment-success=*)
            DEPLOYMENT_SUCCESS="${1#*=}"
            shift
            ;;
        --lead-time=*)
            LEAD_TIME_START="${1#*=}"
            shift
            ;;
        --commit-sha=*)
            COMMIT_SHA="${1#*=}"
            shift
            ;;
        *)
            echo "Unknown option $1"
            exit 1
            ;;
    esac
done

echo "Updating DORA metrics"

# Create analytics directory
ANALYTICS_DIR="${PROJECT_ROOT}/analytics"
DORA_DIR="${ANALYTICS_DIR}/dora"
mkdir -p "${DORA_DIR}"

# Current timestamp
CURRENT_TIME="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

# Initialize DORA metrics file if it doesn't exist
DORA_METRICS_FILE="${DORA_DIR}/dora-metrics.json"
if [[ ! -f "${DORA_METRICS_FILE}" ]]; then
    jq -n '{
        deployment_frequency: [],
        lead_time: [],
        change_failure_rate: [],
        mttr: []
    }' > "${DORA_METRICS_FILE}"
fi

# Update Deployment Frequency
update_deployment_frequency() {
    local success="$1"
    
    if [[ "${success}" == "success" ]]; then
        local deployment_record
        deployment_record=$(jq -n \
            --arg timestamp "${CURRENT_TIME}" \
            --arg commit "${COMMIT_SHA}" \
            --arg environment "${GITHUB_REF_NAME:-unknown}" \
            --arg success "true" \
            '{
                timestamp: $timestamp,
                commit: $commit,
                environment: $environment,
                success: ($success == "true")
            }')
        
        # Add to deployment frequency array
        jq --argjson record "${deployment_record}" \
            '.deployment_frequency += [$record]' \
            "${DORA_METRICS_FILE}" > "${DORA_METRICS_FILE}.tmp" && \
            mv "${DORA_METRICS_FILE}.tmp" "${DORA_METRICS_FILE}"
        
        echo "✅ Deployment frequency updated"
    fi
}

# Update Lead Time
update_lead_time() {
    local start_time="$1"
    local commit="$2"
    
    if [[ -n "${start_time}" ]] && [[ -n "${commit}" ]]; then
        # Calculate lead time in seconds
        local lead_time_seconds
        if command -v gdate >/dev/null 2>&1; then
            # macOS
            local start_epoch
            start_epoch=$(gdate -d "${start_time}" +%s 2>/dev/null || echo "0")
            local end_epoch
            end_epoch=$(gdate -d "${CURRENT_TIME}" +%s)
        else
            # Linux
            local start_epoch
            start_epoch=$(date -d "${start_time}" +%s 2>/dev/null || echo "0")
            local end_epoch
            end_epoch=$(date -d "${CURRENT_TIME}" +%s)
        fi
        
        lead_time_seconds=$((end_epoch - start_epoch))
        
        # Only record if we have a valid lead time
        if [[ "${lead_time_seconds}" -gt 0 ]]; then
            local lead_time_record
            lead_time_record=$(jq -n \
                --arg timestamp "${CURRENT_TIME}" \
                --arg commit "${commit}" \
                --arg start_time "${start_time}" \
                --arg lead_time_seconds "${lead_time_seconds}" \
                --arg lead_time_hours "$(echo "scale=2; ${lead_time_seconds} / 3600" | bc -l 2>/dev/null || echo "0")" \
                '{
                    timestamp: $timestamp,
                    commit: $commit,
                    start_time: $start_time,
                    lead_time_seconds: ($lead_time_seconds | tonumber),
                    lead_time_hours: ($lead_time_hours | tonumber)
                }')
            
            # Add to lead time array
            jq --argjson record "${lead_time_record}" \
                '.lead_time += [$record]' \
                "${DORA_METRICS_FILE}" > "${DORA_METRICS_FILE}.tmp" && \
                mv "${DORA_METRICS_FILE}.tmp" "${DORA_METRICS_FILE}"
            
            echo "✅ Lead time updated: ${lead_time_seconds}s ($(echo "scale=2; ${lead_time_seconds} / 3600" | bc -l 2>/dev/null || echo "0")h)"
        fi
    fi
}

# Update Change Failure Rate
update_change_failure_rate() {
    local success="$1"
    local commit="$2"
    
    local failure_record
    failure_record=$(jq -n \
        --arg timestamp "${CURRENT_TIME}" \
        --arg commit "${commit}" \
        --arg failed "$(if [[ "${success}" != "success" ]]; then echo "true"; else echo "false"; fi)" \
        '{
            timestamp: $timestamp,
            commit: $commit,
            failed: ($failed == "true")
        }')
    
    # Add to change failure rate array
    jq --argjson record "${failure_record}" \
        '.change_failure_rate += [$record]' \
        "${DORA_METRICS_FILE}" > "${DORA_METRICS_FILE}.tmp" && \
        mv "${DORA_METRICS_FILE}.tmp" "${DORA_METRICS_FILE}"
    
    echo "✅ Change failure rate updated"
}

# Calculate current DORA metrics
calculate_current_metrics() {
    local metrics_file="$1"
    
    # Calculate deployment frequency (deployments per day over last 30 days)
    local deployment_frequency
    deployment_frequency=$(jq -r --arg cutoff "$(date -u -d '30 days ago' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-30d +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || echo '1970-01-01T00:00:00Z')" '
        [.deployment_frequency[] | select(.timestamp > $cutoff and .success == true)] | 
        length / 30
    ' "${metrics_file}" 2>/dev/null || echo "0")
    
    # Calculate average lead time (hours over last 30 days)
    local avg_lead_time
    avg_lead_time=$(jq -r --arg cutoff "$(date -u -d '30 days ago' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-30d +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || echo '1970-01-01T00:00:00Z')" '
        [.lead_time[] | select(.timestamp > $cutoff) | .lead_time_hours] | 
        if length > 0 then (add / length) else 0 end
    ' "${metrics_file}" 2>/dev/null || echo "0")
    
    # Calculate change failure rate (percentage over last 30 days)
    local change_failure_rate
    change_failure_rate=$(jq -r --arg cutoff "$(date -u -d '30 days ago' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-30d +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || echo '1970-01-01T00:00:00Z')" '
        [.change_failure_rate[] | select(.timestamp > $cutoff)] as $recent |
        if ($recent | length) > 0 then 
            ([$recent[] | select(.failed == true)] | length) / ($recent | length) * 100
        else 0 end
    ' "${metrics_file}" 2>/dev/null || echo "0")
    
    # Calculate MTTR (average hours over last 30 days)
    local avg_mttr
    avg_mttr=$(jq -r --arg cutoff "$(date -u -d '30 days ago' +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-30d +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || echo '1970-01-01T00:00:00Z')" '
        [.mttr[] | select(.timestamp > $cutoff) | .recovery_time_hours] | 
        if length > 0 then (add / length) else 0 end
    ' "${metrics_file}" 2>/dev/null || echo "0")
    
    # Generate current metrics summary
    jq -n \
        --arg timestamp "${CURRENT_TIME}" \
        --arg deployment_frequency "${deployment_frequency}" \
        --arg avg_lead_time "${avg_lead_time}" \
        --arg change_failure_rate "${change_failure_rate}" \
        --arg avg_mttr "${avg_mttr}" \
        '{
            timestamp: $timestamp,
            deployment_frequency_per_day: ($deployment_frequency | tonumber),
            lead_time_hours: ($avg_lead_time | tonumber),
            change_failure_rate_percent: ($change_failure_rate | tonumber),
            mttr_hours: ($avg_mttr | tonumber)
        }'
}

# Update metrics based on provided data
if [[ -n "${DEPLOYMENT_SUCCESS}" ]]; then
    update_deployment_frequency "${DEPLOYMENT_SUCCESS}"
    update_change_failure_rate "${DEPLOYMENT_SUCCESS}" "${COMMIT_SHA}"
fi

if [[ -n "${LEAD_TIME_START}" ]] && [[ -n "${COMMIT_SHA}" ]]; then
    update_lead_time "${LEAD_TIME_START}" "${COMMIT_SHA}"
fi

# Calculate and save current metrics
CURRENT_METRICS=$(calculate_current_metrics "${DORA_METRICS_FILE}")
echo "${CURRENT_METRICS}" > "${DORA_DIR}/current-metrics.json"

# Generate DORA metrics summary for GitHub Actions
echo "## DORA Metrics Summary" >> "${GITHUB_STEP_SUMMARY:-/dev/stdout}"
echo "" >> "${GITHUB_STEP_SUMMARY:-/dev/stdout}"
echo "- **Deployment Frequency**: $(echo "${CURRENT_METRICS}" | jq -r '.deployment_frequency_per_day') deployments/day" >> "${GITHUB_STEP_SUMMARY:-/dev/stdout}"
echo "- **Lead Time**: $(echo "${CURRENT_METRICS}" | jq -r '.lead_time_hours') hours" >> "${GITHUB_STEP_SUMMARY:-/dev/stdout}"
echo "- **Change Failure Rate**: $(echo "${CURRENT_METRICS}" | jq -r '.change_failure_rate_percent')%" >> "${GITHUB_STEP_SUMMARY:-/dev/stdout}"
echo "- **MTTR**: $(echo "${CURRENT_METRICS}" | jq -r '.mttr_hours') hours" >> "${GITHUB_STEP_SUMMARY:-/dev/stdout}"

# Classify DORA performance level
DEPLOYMENT_FREQ=$(echo "${CURRENT_METRICS}" | jq -r '.deployment_frequency_per_day')
LEAD_TIME=$(echo "${CURRENT_METRICS}" | jq -r '.lead_time_hours')
FAILURE_RATE=$(echo "${CURRENT_METRICS}" | jq -r '.change_failure_rate_percent')
MTTR=$(echo "${CURRENT_METRICS}" | jq -r '.mttr_hours')

PERFORMANCE_LEVEL="Low"
if (( $(echo "${DEPLOYMENT_FREQ} >= 1" | bc -l 2>/dev/null || echo "0") )) && \
   (( $(echo "${LEAD_TIME} <= 24" | bc -l 2>/dev/null || echo "0") )) && \
   (( $(echo "${FAILURE_RATE} <= 15" | bc -l 2>/dev/null || echo "0") )) && \
   (( $(echo "${MTTR} <= 24" | bc -l 2>/dev/null || echo "0") )); then
    PERFORMANCE_LEVEL="High"
elif (( $(echo "${DEPLOYMENT_FREQ} >= 0.5" | bc -l 2>/dev/null || echo "0") )) && \
     (( $(echo "${LEAD_TIME} <= 168" | bc -l 2>/dev/null || echo "0") )) && \
     (( $(echo "${FAILURE_RATE} <= 20" | bc -l 2>/dev/null || echo "0") )) && \
     (( $(echo "${MTTR} <= 168" | bc -l 2>/dev/null || echo "0") )); then
    PERFORMANCE_LEVEL="Medium"
fi

echo "" >> "${GITHUB_STEP_SUMMARY:-/dev/stdout}"
echo "**Overall DORA Performance Level**: ${PERFORMANCE_LEVEL}" >> "${GITHUB_STEP_SUMMARY:-/dev/stdout}"

echo "✅ DORA metrics updated successfully"