#!/bin/bash

# System Cleanup and Maintenance Script
# Performs routine maintenance tasks for the MCP Debate System

set -euo pipefail

# Configuration
LOG_RETENTION_DAYS=${LOG_RETENTION_DAYS:-30}
BACKUP_RETENTION_DAYS=${BACKUP_RETENTION_DAYS:-90}
DOCKER_CLEANUP=${DOCKER_CLEANUP:-true}
DATABASE_MAINTENANCE=${DATABASE_MAINTENANCE:-true}
REDIS_OPTIMIZATION=${REDIS_OPTIMIZATION:-true}
DRY_RUN=${DRY_RUN:-false}

# Paths
LOG_DIRS=(
    "/var/log/mcp"
    "/var/log/docker"
    "/opt/mcp/logs"
)

TEMP_DIRS=(
    "/tmp/mcp-*"
    "/var/tmp/mcp-*"
    "/opt/mcp/temp"
)

# Database settings
DB_HOST=${DB_HOST:-"localhost"}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-"debate_db"}
DB_USER=${DB_USER:-"postgres"}
DB_PASSWORD=${DB_PASSWORD:?"Database password must be provided"}

REDIS_HOST=${REDIS_HOST:-"localhost"}
REDIS_PORT=${REDIS_PORT:-6379}

# Logging
LOG_FILE="/var/log/mcp/cleanup-$(date +%Y%m%d_%H%M%S).log"
mkdir -p "$(dirname """"$LOG_FILE"""")"
exec 1> >(tee -a """"$LOG_FILE"""")
exec 2>&1

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

error() {
    log "ERROR: $1"
    exit 1
}

warning() {
    log "WARNING: $1"
}

# Execute command with dry run support
execute() {
    if [ """"$DRY_RUN"""" = "true" ]; then
        log "DRY RUN: $*"
    else
        log "EXECUTING: $*"
        eval "$*"
    fi
}

# Clean up old log files
cleanup_logs() {
    log "Starting log cleanup (retention: """$LOG_RETENTION_DAYS""" days)..."
    
    local total_freed=0
    
    for log_dir in "${LOG_DIRS[@]}"; do
        if [ ! -d """"$log_dir"""" ]; then
            continue
        fi
        
        log "Cleaning logs in: """$log_dir""""
        
        # Calculate size before cleanup
        local size_before=$(du -sb """"$log_dir"""" 2>/dev/null | cut -f1 || echo "0")
        
        # Find and remove old log files
        local old_logs=$(find """"$log_dir"""" -name "*.log*" -type f -mtime +"""$LOG_RETENTION_DAYS""" 2>/dev/null || echo "")
        
        if [ -n """"$old_logs"""" ]; then
            echo """"$old_logs"""" | while read -r logfile; do
                local file_size=$(du -b """"$logfile"""" 2>/dev/null | cut -f1 || echo "0")
                execute "rm -f '"""$logfile"""'"
                total_freed=$((total_freed + file_size))
            done
        fi
        
        # Compress large current log files (> 100MB)
        find """"$log_dir"""" -name "*.log" -type f -size +100M -not -name "*.gz" 2>/dev/null | while read -r logfile; do
            log "Compressing large log file: """$logfile""""
            execute "gzip '"""$logfile"""'"
        done
        
        # Clean up empty directories
        find """"$log_dir"""" -type d -empty -delete 2>/dev/null || true
    done
    
    if [ """"$total_freed"""" -gt 0 ]; then
        log "Log cleanup completed. Freed: $(numfmt --to=iec """$total_freed""")"
    else
        log "Log cleanup completed. No old logs found."
    fi
}

# Clean up temporary files
cleanup_temp_files() {
    log "Starting temporary files cleanup..."
    
    local total_freed=0
    
    for temp_pattern in "${TEMP_DIRS[@]}"; do
        # Find matching directories/files
        local temp_items=$(find /tmp /var/tmp -name "$(basename """"$temp_pattern"""")" -mtime +1 2>/dev/null || echo "")
        
        if [ -n """"$temp_items"""" ]; then
            echo """"$temp_items"""" | while read -r item; do
                if [ -e """"$item"""" ]; then
                    local item_size=$(du -sb """"$item"""" 2>/dev/null | cut -f1 || echo "0")
                    execute "rm -rf '"""$item"""'"
                    total_freed=$((total_freed + item_size))
                fi
            done
        fi
    done
    
    # Clean up core dumps
    find /tmp /var/tmp -name "core.*" -mtime +7 -delete 2>/dev/null || true
    
    # Clean up old download files
    find /tmp -name "*.tmp" -name "*.download" -mtime +1 -delete 2>/dev/null || true
    
    if [ """"$total_freed"""" -gt 0 ]; then
        log "Temp cleanup completed. Freed: $(numfmt --to=iec """$total_freed""")"
    else
        log "Temp cleanup completed. No temp files found."
    fi
}

# Docker system cleanup
cleanup_docker() {
    if [ """"$DOCKER_CLEANUP"""" != "true" ] || ! command -v docker >/dev/null 2>&1; then
        log "Skipping Docker cleanup"
        return
    fi
    
    log "Starting Docker cleanup..."
    
    # Show current Docker usage
    log "Docker disk usage before cleanup:"
    docker system df || true
    
    # Remove stopped containers
    local stopped_containers=$(docker ps -aq --filter status=exited 2>/dev/null || echo "")
    if [ -n """"$stopped_containers"""" ]; then
        log "Removing stopped containers..."
        echo """"$stopped_containers"""" | while read -r container; do
            execute "docker rm '"""$container"""'"
        done
    fi
    
    # Remove dangling images
    local dangling_images=$(docker images -f "dangling=true" -q 2>/dev/null || echo "")
    if [ -n """"$dangling_images"""" ]; then
        log "Removing dangling images..."
        echo """"$dangling_images"""" | while read -r image; do
            execute "docker rmi '"""$image"""'"
        done
    fi
    
    # Remove unused networks
    execute "docker network prune -f"
    
    # Remove unused volumes
    execute "docker volume prune -f"
    
    # Remove build cache
    execute "docker builder prune -f"
    
    # Show usage after cleanup
    log "Docker disk usage after cleanup:"
    docker system df || true
    
    log "Docker cleanup completed"
}

# Database maintenance
maintain_database() {
    if [ """"$DATABASE_MAINTENANCE"""" != "true" ]; then
        log "Skipping database maintenance"
        return
    fi
    
    log "Starting database maintenance..."
    
    # Check if PostgreSQL is accessible
    if ! pg_isready -h """"$DB_HOST"""" -p """"$DB_PORT"""" -U """"$DB_USER"""" >/dev/null 2>&1; then
        warning "PostgreSQL not accessible, skipping database maintenance"
        return
    fi
    
    # Vacuum and analyze
    log "Running VACUUM ANALYZE..."
    execute "PGPASSWORD='"""$DB_PASSWORD"""' psql -h '"""$DB_HOST"""' -p '"""$DB_PORT"""' -U '"""$DB_USER"""' -d '"""$DB_NAME"""' -c 'VACUUM ANALYZE;'"
    
    # Update table statistics
    log "Updating table statistics..."
    execute "PGPASSWORD='"""$DB_PASSWORD"""' psql -h '"""$DB_HOST"""' -p '"""$DB_PORT"""' -U '"""$DB_USER"""' -d '"""$DB_NAME"""' -c 'ANALYZE;'"
    
    # Reindex if needed (check for bloat first)
    log "Checking for index bloat..."
    local bloat_query="
    SELECT schemaname, tablename, 
           pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size,
           pg_size_pretty(pg_relation_size(schemaname||'.'||tablename)) as table_size
    FROM pg_tables 
    WHERE schemaname = 'public'
    ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;"
    
    PGPASSWORD=""""$DB_PASSWORD"""" psql -h """"$DB_HOST"""" -p """"$DB_PORT"""" -U """"$DB_USER"""" -d """"$DB_NAME"""" -c """"$bloat_query"""" || true
    
    # Clean up old debate data (older than 1 year)
    log "Cleaning up old debate data..."
    local cleanup_query="
    DELETE FROM debate_messages 
    WHERE debate_id IN (
        SELECT id FROM debates 
        WHERE created_at < NOW() - INTERVAL '1 year'
        AND status = 'COMPLETED'
    );
    
    DELETE FROM debates 
    WHERE created_at < NOW() - INTERVAL '1 year'
    AND status = 'COMPLETED';"
    
    if [ """"$DRY_RUN"""" = "true" ]; then
        log "DRY RUN: Would clean up old debate data"
    else
        local deleted_count=$(PGPASSWORD=""""$DB_PASSWORD"""" psql -h """"$DB_HOST"""" -p """"$DB_PORT"""" -U """"$DB_USER"""" -d """"$DB_NAME"""" -t -c "
        WITH deleted_messages AS (
            DELETE FROM debate_messages 
            WHERE debate_id IN (
                SELECT id FROM debates 
                WHERE created_at < NOW() - INTERVAL '1 year'
                AND status = 'COMPLETED'
            ) RETURNING 1
        ),
        deleted_debates AS (
            DELETE FROM debates 
            WHERE created_at < NOW() - INTERVAL '1 year'
            AND status = 'COMPLETED'
            RETURNING 1
        )
        SELECT 
            (SELECT COUNT(*) FROM deleted_messages) as messages,
            (SELECT COUNT(*) FROM deleted_debates) as debates;" | xargs)
        
        log "Cleaned up old data: """$deleted_count""""
    fi
    
    log "Database maintenance completed"
}

# Redis optimization
optimize_redis() {
    if [ """"$REDIS_OPTIMIZATION"""" != "true" ]; then
        log "Skipping Redis optimization"
        return
    fi
    
    log "Starting Redis optimization..."
    
    # Check if Redis is accessible
    if ! redis-cli -h """"$REDIS_HOST"""" -p """"$REDIS_PORT"""" ping >/dev/null 2>&1; then
        warning "Redis not accessible, skipping optimization"
        return
    fi
    
    # Get Redis info
    local redis_info=$(redis-cli -h """"$REDIS_HOST"""" -p """"$REDIS_PORT"""" INFO memory | grep -E "(used_memory_human|used_memory_peak_human|mem_fragmentation_ratio)")
    log "Redis memory info: """$redis_info""""
    
    # Clean up expired keys
    log "Cleaning up expired keys..."
    execute "redis-cli -h '"""$REDIS_HOST"""' -p '"""$REDIS_PORT"""' --eval 'return redis.call(\"SCAN\", 0, \"COUNT\", 1000)' 0"
    
    # Defragment if fragmentation ratio is high
    local frag_ratio=$(redis-cli -h """"$REDIS_HOST"""" -p """"$REDIS_PORT"""" INFO memory | grep mem_fragmentation_ratio | cut -d: -f2 | tr -d '\r')
    if (( $(echo """"$frag_ratio""" > 1.5" | bc -l) )); then
        log "High fragmentation detected ("""$frag_ratio"""), starting defragmentation..."
        execute "redis-cli -h '"""$REDIS_HOST"""' -p '"""$REDIS_PORT"""' MEMORY DOCTOR"
        
        # Active defragmentation (Redis 4.0+)
        execute "redis-cli -h '"""$REDIS_HOST"""' -p '"""$REDIS_PORT"""' CONFIG SET activedefrag yes"
    fi
    
    # Clean up old cache entries (older than 24 hours)
    log "Cleaning up old cache entries..."
    local cache_keys=$(redis-cli -h """"$REDIS_HOST"""" -p """"$REDIS_PORT"""" --scan --pattern "*cache*" | head -1000)
    if [ -n """"$cache_keys"""" ]; then
        echo """"$cache_keys"""" | while read -r key; do
            local ttl=$(redis-cli -h """"$REDIS_HOST"""" -p """"$REDIS_PORT"""" TTL """"$key"""")
            if [ """"$ttl"""" -eq -1 ]; then
                # Key without expiration, set a reasonable TTL
                execute "redis-cli -h '"""$REDIS_HOST"""' -p '"""$REDIS_PORT"""' EXPIRE '"""$key"""' 86400"
            fi
        done
    fi
    
    log "Redis optimization completed"
}

# Clean up old backups
cleanup_backups() {
    log "Starting backup cleanup (retention: """$BACKUP_RETENTION_DAYS""" days)..."
    
    local backup_dirs=(
        "/backups"
        "/var/backups/mcp"
        "/opt/mcp/backups"
    )
    
    local total_freed=0
    
    for backup_dir in "${backup_dirs[@]}"; do
        if [ ! -d """"$backup_dir"""" ]; then
            continue
        fi
        
        log "Cleaning backups in: """$backup_dir""""
        
        # Find old backup files
        local old_backups=$(find """"$backup_dir"""" -name "mcp-debate_*" -type f -mtime +"""$BACKUP_RETENTION_DAYS""" 2>/dev/null || echo "")
        
        if [ -n """"$old_backups"""" ]; then
            echo """"$old_backups"""" | while read -r backup; do
                local file_size=$(du -b """"$backup"""" 2>/dev/null | cut -f1 || echo "0")
                execute "rm -f '"""$backup"""'"
                total_freed=$((total_freed + file_size))
            done
        fi
    done
    
    if [ """"$total_freed"""" -gt 0 ]; then
        log "Backup cleanup completed. Freed: $(numfmt --to=iec """$total_freed""")"
    else
        log "Backup cleanup completed. No old backups found."
    fi
}

# System health check
perform_health_check() {
    log "Performing post-cleanup health check..."
    
    # Check disk space
    local disk_usage=$(df / | awk 'NR==2 {print $5}' | sed 's/%//')
    if [ """"$disk_usage"""" -gt 90 ]; then
        warning "Disk usage still high after cleanup: ${disk_usage}%"
    else
        log "Disk usage OK: ${disk_usage}%"
    fi
    
    # Check memory usage
    local mem_usage=$(free | awk 'NR==2{printf "%.0f", $3*100/$2}')
    if [ """"$mem_usage"""" -gt 90 ]; then
        warning "Memory usage high: ${mem_usage}%"
    else
        log "Memory usage OK: ${mem_usage}%"
    fi
    
    # Check system load
    local load_avg=$(uptime | awk -F'load average:' '{print $2}' | cut -d',' -f1 | xargs)
    log "System load average: """$load_avg""""
    
    # Check key services
    local services=("docker" "postgresql" "redis-server")
    for service in "${services[@]}"; do
        if systemctl is-active --quiet """"$service"""" 2>/dev/null; then
            log "Service """$service""" is running"
        else
            warning "Service """$service""" is not running"
        fi
    done
}

# Generate cleanup report
generate_report() {
    local report_file="/var/log/mcp/cleanup-report-$(date +%Y%m%d_%H%M%S).txt"
    
    cat > """"$report_file"""" <<EOF
MCP System Cleanup Report
========================
Generated: $(date)
Cleanup Duration: $(($(date +%s) - start_time)) seconds

Settings:
- Log Retention: """$LOG_RETENTION_DAYS""" days
- Backup Retention: """$BACKUP_RETENTION_DAYS""" days
- Docker Cleanup: $DOCKER_CLEANUP
- Database Maintenance: $DATABASE_MAINTENANCE
- Redis Optimization: $REDIS_OPTIMIZATION
- Dry Run: $DRY_RUN

System Status After Cleanup:
- Disk Usage: $(df / | awk 'NR==2 {print $5}')
- Memory Usage: $(free | awk 'NR==2{printf "%.0f%%", $3*100/$2}')
- Load Average: $(uptime | awk -F'load average:' '{print $2}')

Cleanup Log: $LOG_FILE
EOF
    
    log "Cleanup report generated: """$report_file""""
    cat """"$report_file""""
}

# Send cleanup notification
send_notification() {
    local status="$1"
    local message="$2"
    
    # Slack notification
    if [ -n "${SLACK_WEBHOOK:-}" ]; then
        curl -X POST -H 'Content-type: application/json' \
            --data "{\"text\":\"🧹 MCP Cleanup """$status""": """$message"""\"}" \
            """"$SLACK_WEBHOOK"""" >/dev/null 2>&1 || true
    fi
    
    # Email notification
    if [ -n "${EMAIL_TO:-}" ] && command -v mail >/dev/null 2>&1; then
        echo """"$message"""" | mail -s "MCP Cleanup """$status"""" """"$EMAIL_TO"""" || true
    fi
}

# Main cleanup function
main() {
    local start_time=$(date +%s)
    
    log "Starting MCP system cleanup..."
    log "Mode: $([ """"$DRY_RUN"""" = "true" ] && echo "DRY RUN" || echo "LIVE")"
    
    # Trap for cleanup on exit
    trap 'log "Cleanup process interrupted"' INT TERM
    
    # Perform cleanup tasks
    cleanup_logs
    cleanup_temp_files
    cleanup_docker
    maintain_database
    optimize_redis
    cleanup_backups
    
    # Post-cleanup checks
    perform_health_check
    
    # Generate report
    generate_report
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    log "Cleanup process completed successfully in ${duration} seconds"
    send_notification "SUCCESS" "System cleanup completed in ${duration} seconds"
}

# Parse command line arguments
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --no-docker)
            DOCKER_CLEANUP=false
            shift
            ;;
        --no-database)
            DATABASE_MAINTENANCE=false
            shift
            ;;
        --no-redis)
            REDIS_OPTIMIZATION=false
            shift
            ;;
        --log-retention)
            LOG_RETENTION_DAYS="$2"
            shift 2
            ;;
        --backup-retention)
            BACKUP_RETENTION_DAYS="$2"
            shift 2
            ;;
        --help)
            cat <<EOF
Usage: $0 [OPTIONS]

MCP System Cleanup and Maintenance Script

Options:
    --dry-run              Show what would be done without executing
    --no-docker            Skip Docker cleanup
    --no-database          Skip database maintenance
    --no-redis             Skip Redis optimization
    --log-retention DAYS   Log retention period (default: 30)
    --backup-retention DAYS Backup retention period (default: 90)
    --help                 Show this help

Environment Variables:
    LOG_RETENTION_DAYS     Log retention period
    BACKUP_RETENTION_DAYS  Backup retention period
    DOCKER_CLEANUP         Enable/disable Docker cleanup
    DATABASE_MAINTENANCE   Enable/disable database maintenance
    REDIS_OPTIMIZATION     Enable/disable Redis optimization
    DRY_RUN               Enable dry run mode
    SLACK_WEBHOOK         Slack webhook for notifications
    EMAIL_TO              Email address for notifications

Examples:
    $0                     # Full cleanup
    $0 --dry-run          # Preview what would be cleaned
    $0 --no-docker        # Skip Docker cleanup
    $0 --log-retention 7  # Keep logs for 7 days only

EOF
            exit 0
            ;;
        *)
            error "Unknown option: $1"
            ;;
    esac
done

# Run main function
main "$@"