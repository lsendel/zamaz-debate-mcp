#!/bin/bash

# Automated Database Backup Script
# Supports PostgreSQL, Redis, and application state backups

set -euo pipefail

# Configuration
BACKUP_DIR=${BACKUP_DIR:-"/backups"}
RETENTION_DAYS=${RETENTION_DAYS:-30}
COMPRESSION_LEVEL=${COMPRESSION_LEVEL:-6}
BACKUP_PREFIX="mcp-debate"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Database configurations
DB_HOST=${DB_HOST:-"localhost"}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-"debate_db"}
DB_USER=${DB_USER:-"postgres"}
DB_PASSWORD=${DB_PASSWORD:?"Database password must be provided"}

REDIS_HOST=${REDIS_HOST:-"localhost"}
REDIS_PORT=${REDIS_PORT:-6379}
REDIS_PASSWORD=${REDIS_PASSWORD:-""}

# S3 Configuration (optional)
S3_BUCKET=${S3_BUCKET:-""}
S3_PREFIX=${S3_PREFIX:-"backups"}

# Logging
LOG_FILE="${BACKUP_DIR}/backup.log"
exec 1> >(tee -a """"$LOG_FILE"""")
exec 2>&1

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

error() {
    log "ERROR: $1"
    exit 1
}

# Create backup directory
mkdir -p """"$BACKUP_DIR""""
cd """"$BACKUP_DIR""""

log "Starting backup process for MCP Debate System"

# PostgreSQL Backup
backup_postgresql() {
    log "Starting PostgreSQL backup..."
    
    local backup_file="${BACKUP_PREFIX}_postgres_${TIMESTAMP}.sql"
    local compressed_file="${backup_file}.gz"
    
    # Check if PostgreSQL is accessible
    if ! pg_isready -h """"$DB_HOST"""" -p """"$DB_PORT"""" -U """"$DB_USER"""" >/dev/null 2>&1; then
        error "PostgreSQL is not accessible at ${DB_HOST}:${DB_PORT}"
    fi
    
    # Create database dump
    log "Creating PostgreSQL dump..."
    PGPASSWORD=""""$DB_PASSWORD"""" pg_dump \
        -h """"$DB_HOST"""" \
        -p """"$DB_PORT"""" \
        -U """"$DB_USER"""" \
        -d """"$DB_NAME"""" \
        --verbose \
        --no-password \
        --format=custom \
        --compress=""""$COMPRESSION_LEVEL"""" \
        --file=""""$backup_file""""
    
    if [ "$?" -eq 0 ]; then
        log "PostgreSQL backup completed: """$backup_file""""
        
        # Get backup size
        local backup_size=$(du -h """"$backup_file"""" | cut -f1)
        log "Backup size: """$backup_size""""
        
        # Verify backup integrity
        PGPASSWORD=""""$DB_PASSWORD"""" pg_restore \
            --list \
            """"$backup_file"""" >/dev/null 2>&1
        
        if [ "$?" -eq 0 ]; then
            log "Backup integrity verified"
        else
            error "Backup integrity check failed"
        fi
        
        echo """"$backup_file""""
    else
        error "PostgreSQL backup failed"
    fi
}

# Redis Backup
backup_redis() {
    log "Starting Redis backup..."
    
    local backup_file="${BACKUP_PREFIX}_redis_${TIMESTAMP}.rdb"
    
    # Check if Redis is accessible
    if [ -n """"$REDIS_PASSWORD"""" ]; then
        redis_cmd="redis-cli -h """$REDIS_HOST""" -p """$REDIS_PORT""" -a """$REDIS_PASSWORD""""
    else
        redis_cmd="redis-cli -h """$REDIS_HOST""" -p """$REDIS_PORT""""
    fi
    
    if ! """$redis_cmd""" ping >/dev/null 2>&1; then
        error "Redis is not accessible at ${REDIS_HOST}:${REDIS_PORT}"
    fi
    
    # Trigger Redis background save
    log "Triggering Redis BGSAVE..."
    """$redis_cmd""" BGSAVE >/dev/null
    
    # Wait for background save to complete
    while [ "$("""$redis_cmd""" LASTSAVE)" = "$("""$redis_cmd""" LASTSAVE)" ]; do
        sleep 1
    done
    
    # Copy Redis dump file
    local redis_data_dir="/var/lib/redis"
    if [ -f """"$redis_data_dir"""/dump.rdb" ]; then
        cp """"$redis_data_dir"""/dump.rdb" """"$backup_file""""
        gzip """"$backup_file""""
        log "Redis backup completed: ${backup_file}.gz"
        echo "${backup_file}.gz"
    else
        error "Redis dump file not found"
    fi
}

# Application State Backup
backup_application_state() {
    log "Starting application state backup..."
    
    local backup_file="${BACKUP_PREFIX}_config_${TIMESTAMP}.tar.gz"
    
    # Backup configuration files, secrets, and other state
    tar -czf """"$backup_file"""" \
        -C / \
        --exclude='*/node_modules/*' \
        --exclude='*/target/*' \
        --exclude='*/build/*' \
        --exclude='*/.git/*' \
        opt/mcp/ \
        etc/mcp/ \
        var/log/mcp/ \
        2>/dev/null || true
    
    if [ -f """"$backup_file"""" ]; then
        log "Application state backup completed: """$backup_file""""
        echo """"$backup_file""""
    else
        error "Application state backup failed"
    fi
}

# Kubernetes Resources Backup
backup_kubernetes() {
    log "Starting Kubernetes resources backup..."
    
    local backup_file="${BACKUP_PREFIX}_k8s_${TIMESTAMP}.yaml"
    
    # Check if kubectl is available
    if ! command -v kubectl >/dev/null 2>&1; then
        log "kubectl not found, skipping Kubernetes backup"
        return
    fi
    
    # Backup all MCP-related resources
    kubectl get all,configmap,secret,pvc,ingress \
        -n production \
        -o yaml > """"$backup_file"""" 2>/dev/null || {
        log "Kubernetes backup failed or no access"
        return
    }
    
    gzip """"$backup_file""""
    log "Kubernetes backup completed: ${backup_file}.gz"
    echo "${backup_file}.gz"
}

# Upload to S3 (if configured)
upload_to_s3() {
    local file="$1"
    
    if [ -z """"$S3_BUCKET"""" ]; then
        return
    fi
    
    if ! command -v aws >/dev/null 2>&1; then
        log "AWS CLI not found, skipping S3 upload"
        return
    fi
    
    log "Uploading """$file""" to S3..."
    aws s3 cp """"$file"""" "s3://${S3_BUCKET}/${S3_PREFIX}/" --quiet
    
    if [ "$?" -eq 0 ]; then
        log "Successfully uploaded """$file""" to S3"
    else
        log "Failed to upload """$file""" to S3"
    fi
}

# Cleanup old backups
cleanup_old_backups() {
    log "Cleaning up backups older than """$RETENTION_DAYS""" days..."
    
    find """"$BACKUP_DIR"""" -name "${BACKUP_PREFIX}_*" -mtime +"""$RETENTION_DAYS""" -delete
    
    # Also cleanup S3 if configured
    if [ -n """"$S3_BUCKET"""" ] && command -v aws >/dev/null 2>&1; then
        local cutoff_date=$(date -d """"$RETENTION_DAYS""" days ago" +%Y-%m-%d)
        aws s3 ls "s3://${S3_BUCKET}/${S3_PREFIX}/" --recursive | \
        awk -v cutoff=""""$cutoff_date"""" '$1 < cutoff {print $4}' | \
        while read -r file; do
            aws s3 rm "s3://${S3_BUCKET}/"""$file"""" --quiet
            log "Deleted old S3 backup: """$file""""
        done
    fi
    
    log "Cleanup completed"
}

# Health check before backup
health_check() {
    log "Performing health check before backup..."
    
    # Check system resources
    local disk_usage=$(df """"$BACKUP_DIR"""" | awk 'NR==2 {print $5}' | sed 's/%//')
    if [ """"$disk_usage"""" -gt 90 ]; then
        error "Insufficient disk space: ${disk_usage}% used"
    fi
    
    # Check memory usage
    local mem_usage=$(free | awk 'NR==2{printf "%.0f", $3*100/$2}')
    if [ """"$mem_usage"""" -gt 95 ]; then
        log "WARNING: High memory usage: ${mem_usage}%"
    fi
    
    log "Health check passed"
}

# Generate backup report
generate_report() {
    local postgres_backup="$1"
    local redis_backup="$2"
    local config_backup="$3"
    local k8s_backup="$4"
    
    local report_file="${BACKUP_PREFIX}_report_${TIMESTAMP}.txt"
    
    cat > """"$report_file"""" <<EOF
MCP Debate System Backup Report
===============================
Timestamp: $(date)
Backup Directory: $BACKUP_DIR
Retention Period: """$RETENTION_DAYS""" days

Backup Files:
- PostgreSQL: """$postgres_backup""" ($(du -h """"$postgres_backup"""" 2>/dev/null | cut -f1 || echo "N/A"))
- Redis: """$redis_backup""" ($(du -h """"$redis_backup"""" 2>/dev/null | cut -f1 || echo "N/A"))
- Configuration: """$config_backup""" ($(du -h """"$config_backup"""" 2>/dev/null | cut -f1 || echo "N/A"))
- Kubernetes: """$k8s_backup""" ($(du -h """"$k8s_backup"""" 2>/dev/null | cut -f1 || echo "N/A"))

System Status:
- Disk Usage: $(df """"$BACKUP_DIR"""" | awk 'NR==2 {print $5}')
- Memory Usage: $(free | awk 'NR==2{printf "%.0f%%", $3*100/$2}')
- Load Average: $(uptime | awk -F'load average:' '{print $2}')

S3 Upload: $([ -n """"$S3_BUCKET"""" ] && echo "Enabled" || echo "Disabled")

Backup Status: SUCCESS
EOF
    
    log "Backup report generated: """$report_file""""
    cat """"$report_file""""
}

# Send notification (if configured)
send_notification() {
    local status="$1"
    local message="$2"
    
    # Slack notification
    if [ -n "${SLACK_WEBHOOK:-}" ]; then
        curl -X POST -H 'Content-type: application/json' \
            --data "{\"text\":\"🗄️ MCP Backup """$status""": """$message"""\"}" \
            """"$SLACK_WEBHOOK"""" >/dev/null 2>&1 || true
    fi
    
    # Email notification
    if [ -n "${EMAIL_TO:-}" ] && command -v mail >/dev/null 2>&1; then
        echo """"$message"""" | mail -s "MCP Backup """$status"""" """"$EMAIL_TO"""" || true
    fi
}

# Main backup process
main() {
    local start_time=$(date +%s)
    
    # Trap for cleanup on exit
    trap 'log "Backup process interrupted"' INT TERM
    
    health_check
    
    # Perform backups
    local postgres_backup=$(backup_postgresql)
    local redis_backup=$(backup_redis)
    local config_backup=$(backup_application_state)
    local k8s_backup=$(backup_kubernetes)
    
    # Upload to S3 if configured
    for file in """"$postgres_backup"""" """"$redis_backup"""" """"$config_backup"""" """"$k8s_backup""""; do
        if [ -n """"$file"""" ] && [ -f """"$file"""" ]; then
            upload_to_s3 """"$file""""
        fi
    done
    
    # Cleanup old backups
    cleanup_old_backups
    
    # Generate report
    generate_report """"$postgres_backup"""" """"$redis_backup"""" """"$config_backup"""" """"$k8s_backup""""
    
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    log "Backup process completed successfully in ${duration} seconds"
    send_notification "SUCCESS" "All backups completed in ${duration} seconds"
}

# Run main function
main "$@"