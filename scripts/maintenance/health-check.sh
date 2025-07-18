#!/bin/bash

# Comprehensive Health Check Script
# Monitors all components of the MCP Debate System

set -euo pipefail

# Configuration
HEALTH_CHECK_TIMEOUT=30
CRITICAL_THRESHOLD=90
WARNING_THRESHOLD=80
REPORT_FILE="/var/log/mcp/health-check-$(date +%Y%m%d_%H%M%S).json"
SLACK_WEBHOOK="${SLACK_WEBHOOK:-}"
EMAIL_TO="${EMAIL_TO:-}"

# Service endpoints
SERVICES=(
    "gateway:http://localhost:8080/actuator/health"
    "organization:http://localhost:5005/actuator/health"
    "debate-engine:http://localhost:5013/actuator/health"
    "llm:http://localhost:5002/actuator/health"
    "rag:http://localhost:5004/actuator/health"
)

# Database endpoints
DATABASES=(
    "postgres:localhost:5432"
    "redis:localhost:6379"
    "qdrant:localhost:6333"
)

# External dependencies
EXTERNAL_DEPS=(
    "openai:https://api.openai.com/v1/models"
    "anthropic:https://api.anthropic.com/v1/messages"
    "google:https://generativelanguage.googleapis.com/v1/models"
)

# Initialize results
HEALTH_RESULTS='{"timestamp":"'$(date -Iseconds)'","status":"UNKNOWN","checks":{}}'

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" >&2
}

error() {
    log "ERROR: $1"
}

warning() {
    log "WARNING: $1"
}

# Update health results
update_result() {
    local component="$1"
    local status="$2"
    local message="$3"
    local response_time="${4:-0}"
    local details="${5:-{}}"
    
    HEALTH_RESULTS=$(echo "$HEALTH_RESULTS" | jq \
        --arg comp "$component" \
        --arg stat "$status" \
        --arg msg "$message" \
        --argjson time "$response_time" \
        --argjson det "$details" \
        '.checks[$comp] = {
            "status": $stat,
            "message": $msg,
            "response_time_ms": $time,
            "details": $det,
            "timestamp": now
        }')
}

# Check HTTP endpoint
check_http_endpoint() {
    local name="$1"
    local url="$2"
    local expected_status="${3:-200}"
    
    log "Checking $name at $url"
    
    local start_time=$(date +%s%3N)
    local response=$(curl -s -w "%{http_code},%{time_total}" \
        --max-time "$HEALTH_CHECK_TIMEOUT" \
        --connect-timeout 10 \
        -H "Accept: application/json" \
        "$url" 2>/dev/null || echo "000,0")
    
    local end_time=$(date +%s%3N)
    local response_time=$((end_time - start_time))
    
    local http_code=$(echo "$response" | tail -n1 | cut -d',' -f1)
    local curl_time=$(echo "$response" | tail -n1 | cut -d',' -f2)
    local body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" = "$expected_status" ]; then
        local status_detail="{}"
        if echo "$body" | jq . >/dev/null 2>&1; then
            status_detail="$body"
        fi
        
        if [ "$response_time" -gt 5000 ]; then
            update_result "$name" "DEGRADED" "Slow response time" "$response_time" "$status_detail"
            warning "$name responding slowly: ${response_time}ms"
        else
            update_result "$name" "UP" "Service is healthy" "$response_time" "$status_detail"
            log "$name is healthy (${response_time}ms)"
        fi
    else
        update_result "$name" "DOWN" "HTTP $http_code" "$response_time" "{\"http_code\":$http_code}"
        error "$name is down: HTTP $http_code"
    fi
}

# Check database connectivity
check_database() {
    local name="$1"
    local host="$2"
    local port="$3"
    
    log "Checking $name database at $host:$port"
    
    local start_time=$(date +%s%3N)
    local result=""
    
    case "$name" in
        postgres)
            result=$(pg_isready -h "$host" -p "$port" -U postgres 2>&1 || echo "FAILED")
            ;;
        redis)
            result=$(redis-cli -h "$host" -p "$port" ping 2>&1 || echo "FAILED")
            ;;
        qdrant)
            result=$(curl -s --max-time 10 "http://$host:$port/health" || echo "FAILED")
            ;;
    esac
    
    local end_time=$(date +%s%3N)
    local response_time=$((end_time - start_time))
    
    if [[ "$result" == *"FAILED"* ]] || [[ "$result" == *"refused"* ]]; then
        update_result "$name" "DOWN" "Database unreachable" "$response_time" "{\"error\":\"$result\"}"
        error "$name database is down: $result"
    else
        update_result "$name" "UP" "Database is accessible" "$response_time" "{}"
        log "$name database is healthy (${response_time}ms)"
    fi
}

# Check system resources
check_system_resources() {
    log "Checking system resources"
    
    # CPU usage
    local cpu_usage=$(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | sed 's/%us,//')
    cpu_usage=${cpu_usage%.*}  # Remove decimal part
    
    # Memory usage
    local mem_info=$(free | grep Mem)
    local mem_total=$(echo "$mem_info" | awk '{print $2}')
    local mem_used=$(echo "$mem_info" | awk '{print $3}')
    local mem_usage=$((mem_used * 100 / mem_total))
    
    # Disk usage
    local disk_usage=$(df / | awk 'NR==2 {print $5}' | sed 's/%//')
    
    # Load average
    local load_avg=$(uptime | awk -F'load average:' '{print $2}' | cut -d',' -f1 | xargs)
    
    local resource_details=$(jq -n \
        --argjson cpu "$cpu_usage" \
        --argjson mem "$mem_usage" \
        --argjson disk "$disk_usage" \
        --arg load "$load_avg" \
        '{
            "cpu_usage_percent": $cpu,
            "memory_usage_percent": $mem,
            "disk_usage_percent": $disk,
            "load_average": $load
        }')
    
    local status="UP"
    local message="System resources normal"
    
    if [ "$cpu_usage" -gt "$CRITICAL_THRESHOLD" ] || 
       [ "$mem_usage" -gt "$CRITICAL_THRESHOLD" ] || 
       [ "$disk_usage" -gt "$CRITICAL_THRESHOLD" ]; then
        status="CRITICAL"
        message="Critical resource usage detected"
        error "Critical resource usage: CPU:${cpu_usage}% MEM:${mem_usage}% DISK:${disk_usage}%"
    elif [ "$cpu_usage" -gt "$WARNING_THRESHOLD" ] || 
         [ "$mem_usage" -gt "$WARNING_THRESHOLD" ] || 
         [ "$disk_usage" -gt "$WARNING_THRESHOLD" ]; then
        status="WARNING"
        message="High resource usage detected"
        warning "High resource usage: CPU:${cpu_usage}% MEM:${mem_usage}% DISK:${disk_usage}%"
    else
        log "System resources OK: CPU:${cpu_usage}% MEM:${mem_usage}% DISK:${disk_usage}%"
    fi
    
    update_result "system_resources" "$status" "$message" 0 "$resource_details"
}

# Check Docker containers
check_docker_containers() {
    log "Checking Docker containers"
    
    if ! command -v docker >/dev/null 2>&1; then
        update_result "docker" "UNKNOWN" "Docker not available" 0 "{}"
        return
    fi
    
    local containers=$(docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "(mcp-|postgres|redis)" || echo "")
    local unhealthy_containers=()
    local total_containers=0
    local running_containers=0
    
    if [ -n "$containers" ]; then
        while IFS= read -r line; do
            if [[ "$line" =~ ^(mcp-|postgres|redis) ]]; then
                total_containers=$((total_containers + 1))
                local name=$(echo "$line" | awk '{print $1}')
                local status=$(echo "$line" | awk '{print $2}')
                
                if [[ "$status" =~ ^Up ]]; then
                    running_containers=$((running_containers + 1))
                else
                    unhealthy_containers+=("$name:$status")
                fi
            fi
        done <<< "$containers"
    fi
    
    local container_details=$(jq -n \
        --argjson total "$total_containers" \
        --argjson running "$running_containers" \
        --argjson unhealthy "$(printf '%s\n' "${unhealthy_containers[@]}" | jq -R . | jq -s .)" \
        '{
            "total_containers": $total,
            "running_containers": $running,
            "unhealthy_containers": $unhealthy
        }')
    
    if [ ${#unhealthy_containers[@]} -gt 0 ]; then
        update_result "docker" "DEGRADED" "Some containers are not running" 0 "$container_details"
        warning "Unhealthy containers: ${unhealthy_containers[*]}"
    elif [ "$total_containers" -eq 0 ]; then
        update_result "docker" "DOWN" "No MCP containers found" 0 "$container_details"
        error "No MCP containers are running"
    else
        update_result "docker" "UP" "All containers running" 0 "$container_details"
        log "All $running_containers containers are running"
    fi
}

# Check external dependencies
check_external_dependencies() {
    for dep in "${EXTERNAL_DEPS[@]}"; do
        local name=$(echo "$dep" | cut -d':' -f1)
        local url=$(echo "$dep" | cut -d':' -f2-)
        
        log "Checking external dependency: $name"
        
        local start_time=$(date +%s%3N)
        local http_code=$(curl -s -o /dev/null -w "%{http_code}" \
            --max-time 10 \
            --connect-timeout 5 \
            -H "Authorization: Bearer dummy" \
            "$url" 2>/dev/null || echo "000")
        local end_time=$(date +%s%3N)
        local response_time=$((end_time - start_time))
        
        # For external APIs, we accept various success codes
        if [[ "$http_code" =~ ^(200|401|403)$ ]]; then
            update_result "external_$name" "UP" "External service reachable" "$response_time" "{\"http_code\":$http_code}"
            log "External $name is reachable (HTTP $http_code)"
        else
            update_result "external_$name" "DOWN" "External service unreachable" "$response_time" "{\"http_code\":$http_code}"
            warning "External $name may be down: HTTP $http_code"
        fi
    done
}

# Check application-specific metrics
check_application_metrics() {
    log "Checking application metrics"
    
    # Check if Prometheus is available
    if curl -s --max-time 5 "http://localhost:9090/-/healthy" >/dev/null 2>&1; then
        # Query some key metrics
        local metrics_data=$(curl -s --max-time 10 \
            "http://localhost:9090/api/v1/query?query=up" | \
            jq -r '.data.result[] | select(.metric.job == "gateway") | .value[1]' 2>/dev/null || echo "0")
        
        local active_debates=$(curl -s --max-time 10 \
            "http://localhost:9090/api/v1/query?query=active_debates_total" | \
            jq -r '.data.result[0].value[1]' 2>/dev/null || echo "0")
        
        local error_rate=$(curl -s --max-time 10 \
            "http://localhost:9090/api/v1/query?query=rate(http_requests_total{status=~\"5..\"}[5m])" | \
            jq -r '.data.result[0].value[1]' 2>/dev/null || echo "0")
        
        local app_details=$(jq -n \
            --arg gateway "$metrics_data" \
            --arg debates "$active_debates" \
            --arg errors "$error_rate" \
            '{
                "gateway_up": $gateway,
                "active_debates": $debates,
                "error_rate": $errors
            }')
        
        update_result "application_metrics" "UP" "Metrics available" 0 "$app_details"
        log "Application metrics collected successfully"
    else
        update_result "application_metrics" "UNKNOWN" "Prometheus not available" 0 "{}"
        warning "Cannot collect application metrics - Prometheus unavailable"
    fi
}

# Determine overall health status
determine_overall_status() {
    local down_count=$(echo "$HEALTH_RESULTS" | jq '[.checks[] | select(.status == "DOWN")] | length')
    local critical_count=$(echo "$HEALTH_RESULTS" | jq '[.checks[] | select(.status == "CRITICAL")] | length')
    local degraded_count=$(echo "$HEALTH_RESULTS" | jq '[.checks[] | select(.status == "DEGRADED")] | length')
    local warning_count=$(echo "$HEALTH_RESULTS" | jq '[.checks[] | select(.status == "WARNING")] | length')
    
    local overall_status="UP"
    local status_message="All systems operational"
    
    if [ "$down_count" -gt 0 ] || [ "$critical_count" -gt 0 ]; then
        overall_status="DOWN"
        status_message="Critical issues detected"
    elif [ "$degraded_count" -gt 0 ] || [ "$warning_count" -gt 0 ]; then
        overall_status="DEGRADED"
        status_message="Performance issues detected"
    fi
    
    HEALTH_RESULTS=$(echo "$HEALTH_RESULTS" | jq \
        --arg status "$overall_status" \
        --arg message "$status_message" \
        --argjson down "$down_count" \
        --argjson critical "$critical_count" \
        --argjson degraded "$degraded_count" \
        --argjson warning "$warning_count" \
        '.status = $status | 
         .message = $message |
         .summary = {
             "down": $down,
             "critical": $critical, 
             "degraded": $degraded,
             "warning": $warning
         }')
}

# Send notifications
send_notifications() {
    local status=$(echo "$HEALTH_RESULTS" | jq -r '.status')
    local message=$(echo "$HEALTH_RESULTS" | jq -r '.message')
    
    # Only send notifications for non-UP status
    if [ "$status" = "UP" ]; then
        return
    fi
    
    local notification_text="🏥 MCP Health Check - Status: $status\n$message"
    
    # Slack notification
    if [ -n "$SLACK_WEBHOOK" ]; then
        local color="good"
        case "$status" in
            "DOWN") color="danger" ;;
            "DEGRADED") color="warning" ;;
        esac
        
        curl -X POST -H 'Content-type: application/json' \
            --data "{
                \"attachments\": [{
                    \"color\": \"$color\",
                    \"title\": \"MCP System Health Check\",
                    \"text\": \"$notification_text\",
                    \"ts\": $(date +%s)
                }]
            }" \
            "$SLACK_WEBHOOK" >/dev/null 2>&1 || true
    fi
    
    # Email notification
    if [ -n "$EMAIL_TO" ] && command -v mail >/dev/null 2>&1; then
        echo -e "$notification_text\n\nFull report: $REPORT_FILE" | \
            mail -s "MCP Health Check - $status" "$EMAIL_TO" || true
    fi
}

# Save health report
save_report() {
    # Create directory if it doesn't exist
    mkdir -p "$(dirname "$REPORT_FILE")"
    
    # Save JSON report
    echo "$HEALTH_RESULTS" | jq '.' > "$REPORT_FILE"
    
    # Create human-readable report
    local text_report="${REPORT_FILE%.json}.txt"
    cat > "$text_report" <<EOF
MCP Debate System Health Check Report
====================================
Generated: $(date)
Overall Status: $(echo "$HEALTH_RESULTS" | jq -r '.status')
Message: $(echo "$HEALTH_RESULTS" | jq -r '.message')

Component Status Summary:
EOF
    
    echo "$HEALTH_RESULTS" | jq -r '.checks | to_entries[] | 
        "\(.key): \(.value.status) (\(.value.response_time_ms)ms) - \(.value.message)"' >> "$text_report"
    
    echo -e "\nDetailed Report: $REPORT_FILE" >> "$text_report"
    
    log "Health report saved to: $REPORT_FILE"
    log "Text report saved to: $text_report"
}

# Main health check function
main() {
    log "Starting comprehensive health check..."
    
    # Check all services
    for service in "${SERVICES[@]}"; do
        local name=$(echo "$service" | cut -d':' -f1)
        local url=$(echo "$service" | cut -d':' -f2-)
        check_http_endpoint "$name" "$url"
    done
    
    # Check databases
    for database in "${DATABASES[@]}"; do
        local name=$(echo "$database" | cut -d':' -f1)
        local host=$(echo "$database" | cut -d':' -f2)
        local port=$(echo "$database" | cut -d':' -f3)
        check_database "$name" "$host" "$port"
    done
    
    # Check system resources
    check_system_resources
    
    # Check Docker containers
    check_docker_containers
    
    # Check external dependencies
    check_external_dependencies
    
    # Check application metrics
    check_application_metrics
    
    # Determine overall status
    determine_overall_status
    
    # Save report
    save_report
    
    # Send notifications if needed
    send_notifications
    
    # Output results
    local status=$(echo "$HEALTH_RESULTS" | jq -r '.status')
    echo "$HEALTH_RESULTS" | jq '.'
    
    log "Health check completed. Overall status: $status"
    
    # Exit with appropriate code
    case "$status" in
        "UP") exit 0 ;;
        "DEGRADED") exit 1 ;;
        "DOWN") exit 2 ;;
        *) exit 3 ;;
    esac
}

# Run main function
main "$@"