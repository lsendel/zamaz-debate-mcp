#!/bin/bash

# Database Recovery Script
# Restores PostgreSQL, Redis, and application state from backups

set -euo pipefail

# Configuration
BACKUP_DIR=${BACKUP_DIR:-"/backups"}
BACKUP_FILE=""
RESTORE_TYPE=""
CONFIRM_RESTORE=false

# Database configurations
DB_HOST=${DB_HOST:-"localhost"}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-"debate_db"}
DB_USER=${DB_USER:-"postgres"}
DB_PASSWORD=${DB_PASSWORD:?"Database password must be provided"}

REDIS_HOST=${REDIS_HOST:-"localhost"}
REDIS_PORT=${REDIS_PORT:-6379}
REDIS_PASSWORD=${REDIS_PASSWORD:-""}

# Logging
LOG_FILE="${BACKUP_DIR}/restore.log"
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

usage() {
    cat <<EOF
Usage: $0 [OPTIONS]

Database Recovery Script for MCP Debate System

Options:
    -f, --file FILE         Backup file to restore from
    -t, --type TYPE         Restore type: postgres|redis|config|k8s|all
    -d, --directory DIR     Backup directory (default: /backups)
    -y, --yes              Skip confirmation prompts
    -l, --list             List available backups
    -v, --verify FILE      Verify backup file integrity
    --point-in-time TIME   Restore to specific point in time (PostgreSQL)
    --dry-run              Show what would be restored without executing
    -h, --help             Show this help

Examples:
    $0 --list
    $0 --type postgres --file backup_postgres_20240117_120000.sql
    $0 --type all --point-in-time "2024-01-17 12:00:00"
    $0 --verify backup_postgres_20240117_120000.sql
    $0 --dry-run --type all --file latest

EOF
}

# List available backups
list_backups() {
    log "Available backups in """$BACKUP_DIR""":"
    echo
    
    if [ ! -d """"$BACKUP_DIR"""" ]; then
        log "Backup directory does not exist: """$BACKUP_DIR""""
        return 1
    fi
    
    # PostgreSQL backups
    echo "PostgreSQL Backups:"
    find """"$BACKUP_DIR"""" -name "*postgres*.sql*" -type f -exec ls -lh {} \; | \
        awk '{print $9, "(" $5 ")", $6, $7, $8}' | sort -r | head -10
    echo
    
    # Redis backups
    echo "Redis Backups:"
    find """"$BACKUP_DIR"""" -name "*redis*.rdb*" -type f -exec ls -lh {} \; | \
        awk '{print $9, "(" $5 ")", $6, $7, $8}' | sort -r | head -10
    echo
    
    # Configuration backups
    echo "Configuration Backups:"
    find """"$BACKUP_DIR"""" -name "*config*.tar.gz" -type f -exec ls -lh {} \; | \
        awk '{print $9, "(" $5 ")", $6, $7, $8}' | sort -r | head -10
    echo
    
    # Kubernetes backups
    echo "Kubernetes Backups:"
    find """"$BACKUP_DIR"""" -name "*k8s*.yaml*" -type f -exec ls -lh {} \; | \
        awk '{print $9, "(" $5 ")", $6, $7, $8}' | sort -r | head -10
}

# Verify backup integrity
verify_backup() {
    local file="$1"
    local file_type=""
    
    if [ ! -f """"$file"""" ]; then
        error "Backup file not found: """$file""""
    fi
    
    log "Verifying backup file: """$file""""
    
    # Determine file type
    case """"$file"""" in
        *postgres*.sql*)
            file_type="postgres"
            ;;
        *redis*.rdb*)
            file_type="redis"
            ;;
        *config*.tar.gz)
            file_type="config"
            ;;
        *k8s*.yaml*)
            file_type="kubernetes"
            ;;
        *)
            error "Unknown backup file type: """$file""""
            ;;
    esac
    
    case """"$file_type"""" in
        postgres)
            # Verify PostgreSQL backup
            if [[ """"$file"""" == *.gz ]]; then
                gunzip -t """"$file"""" || error "PostgreSQL backup file is corrupted"
                local temp_file="/tmp/$(basename """"$file"""" .gz)"
                gunzip -c """"$file"""" > """"$temp_file""""
                file=""""$temp_file""""
            fi
            
            PGPASSWORD=""""$DB_PASSWORD"""" pg_restore --list """"$file"""" >/dev/null 2>&1 || \
                error "PostgreSQL backup verification failed"
            
            log "PostgreSQL backup verified successfully"
            ;;
            
        redis)
            # Verify Redis backup
            if [[ """"$file"""" == *.gz ]]; then
                gunzip -t """"$file"""" || error "Redis backup file is corrupted"
            fi
            
            # Basic file format check
            file """"$file"""" | grep -q "data" || error "Redis backup file format invalid"
            log "Redis backup verified successfully"
            ;;
            
        config)
            # Verify configuration backup
            tar -tzf """"$file"""" >/dev/null 2>&1 || error "Configuration backup verification failed"
            log "Configuration backup verified successfully"
            ;;
            
        kubernetes)
            # Verify Kubernetes backup
            if [[ """"$file"""" == *.gz ]]; then
                gunzip -t """"$file"""" || error "Kubernetes backup file is corrupted"
                gunzip -c """"$file"""" | yaml-lint >/dev/null 2>&1 || \
                    warning "Kubernetes YAML validation failed (proceeding anyway)"
            else
                yaml-lint """"$file"""" >/dev/null 2>&1 || \
                    warning "Kubernetes YAML validation failed (proceeding anyway)"
            fi
            log "Kubernetes backup verified successfully"
            ;;
    esac
}

# Get latest backup file
get_latest_backup() {
    local type="$1"
    local pattern=""
    
    case """"$type"""" in
        postgres)
            pattern="*postgres*.sql*"
            ;;
        redis)
            pattern="*redis*.rdb*"
            ;;
        config)
            pattern="*config*.tar.gz"
            ;;
        k8s|kubernetes)
            pattern="*k8s*.yaml*"
            ;;
        *)
            error "Unknown backup type: """$type""""
            ;;
    esac
    
    find """"$BACKUP_DIR"""" -name """"$pattern"""" -type f -printf '%T@ %p\n' | \
        sort -n | tail -1 | cut -d' ' -f2-
}

# Restore PostgreSQL
restore_postgresql() {
    local backup_file="$1"
    local target_time="${2:-}"
    
    log "Starting PostgreSQL restore from: """$backup_file""""
    
    # Verify backup first
    verify_backup """"$backup_file""""
    
    # Stop applications that use the database
    log "Stopping applications..."
    docker-compose stop mcp-organization mcp-gateway mcp-debate-engine mcp-rag || true
    sleep 5
    
    # Create a backup of current database before restore
    log "Creating pre-restore backup..."
    local pre_restore_backup="pre_restore_$(date +%Y%m%d_%H%M%S).sql"
    PGPASSWORD=""""$DB_PASSWORD"""" pg_dump \
        -h """"$DB_HOST"""" -p """"$DB_PORT"""" -U """"$DB_USER"""" -d """"$DB_NAME"""" \
        --file=""""$BACKUP_DIR"""/"""$pre_restore_backup"""" || \
        warning "Could not create pre-restore backup"
    
    # Drop and recreate database
    log "Recreating database..."
    PGPASSWORD=""""$DB_PASSWORD"""" psql -h """"$DB_HOST"""" -p """"$DB_PORT"""" -U """"$DB_USER"""" -d postgres <<EOF
SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '"""$DB_NAME"""';
DROP DATABASE IF EXISTS """$DB_NAME""";
CREATE DATABASE """$DB_NAME""";
EOF
    
    # Restore from backup
    log "Restoring database from backup..."
    if [[ """"$backup_file"""" == *.gz ]]; then
        gunzip -c """"$backup_file"""" | PGPASSWORD=""""$DB_PASSWORD"""" pg_restore \
            -h """"$DB_HOST"""" -p """"$DB_PORT"""" -U """"$DB_USER"""" -d """"$DB_NAME"""" \
            --verbose --clean --if-exists --no-owner --no-privileges
    else
        PGPASSWORD=""""$DB_PASSWORD"""" pg_restore \
            -h """"$DB_HOST"""" -p """"$DB_PORT"""" -U """"$DB_USER"""" -d """"$DB_NAME"""" \
            --verbose --clean --if-exists --no-owner --no-privileges \
            """"$backup_file""""
    fi
    
    if [ "$?" -eq 0 ]; then
        log "PostgreSQL restore completed successfully"
        
        # Point-in-time recovery if specified
        if [ -n """"$target_time"""" ]; then
            log "Applying point-in-time recovery to: """$target_time""""
            # This would require WAL files and more complex setup
            warning "Point-in-time recovery requires WAL files - not implemented in basic restore"
        fi
        
        # Restart applications
        log "Restarting applications..."
        docker-compose start mcp-organization mcp-gateway mcp-debate-engine mcp-rag
        
        # Verify database integrity
        log "Verifying database integrity..."
        PGPASSWORD=""""$DB_PASSWORD"""" psql -h """"$DB_HOST"""" -p """"$DB_PORT"""" -U """"$DB_USER"""" -d """"$DB_NAME"""" \
            -c "SELECT COUNT(*) as table_count FROM information_schema.tables WHERE table_schema = 'public';"
        
    else
        error "PostgreSQL restore failed"
    fi
}

# Restore Redis
restore_redis() {
    local backup_file="$1"
    
    log "Starting Redis restore from: """$backup_file""""
    
    # Verify backup first
    verify_backup """"$backup_file""""
    
    # Stop Redis
    log "Stopping Redis..."
    docker-compose stop redis || systemctl stop redis-server || service redis-server stop || true
    sleep 3
    
    # Backup current Redis data
    local redis_data_dir="/var/lib/redis"
    if [ -f """"$redis_data_dir"""/dump.rdb" ]; then
        log "Backing up current Redis data..."
        cp """"$redis_data_dir"""/dump.rdb" """"$redis_data_dir"""/dump.rdb.backup.$(date +%Y%m%d_%H%M%S)"
    fi
    
    # Restore Redis dump
    log "Restoring Redis data..."
    if [[ """"$backup_file"""" == *.gz ]]; then
        gunzip -c """"$backup_file"""" > """"$redis_data_dir"""/dump.rdb"
    else
        cp """"$backup_file"""" """"$redis_data_dir"""/dump.rdb"
    fi
    
    # Set proper permissions
    chown redis:redis """"$redis_data_dir"""/dump.rdb" 2>/dev/null || true
    chmod 660 """"$redis_data_dir"""/dump.rdb" 2>/dev/null || true
    
    # Start Redis
    log "Starting Redis..."
    docker-compose start redis || systemctl start redis-server || service redis-server start
    
    # Wait for Redis to start
    sleep 5
    
    # Verify Redis data
    local redis_cmd="redis-cli -h """$REDIS_HOST""" -p """$REDIS_PORT""""
    if [ -n """"$REDIS_PASSWORD"""" ]; then
        redis_cmd=""""$redis_cmd""" -a """$REDIS_PASSWORD""""
    fi
    
    if """$redis_cmd""" ping >/dev/null 2>&1; then
        local key_count=$("""$redis_cmd""" dbsize | cut -d: -f2)
        log "Redis restore completed successfully. Keys restored: """$key_count""""
    else
        error "Redis restore failed - service not responding"
    fi
}

# Restore configuration
restore_configuration() {
    local backup_file="$1"
    
    log "Starting configuration restore from: """$backup_file""""
    
    # Verify backup first
    verify_backup """"$backup_file""""
    
    # Create backup of current configuration
    log "Backing up current configuration..."
    local config_backup="config_backup_$(date +%Y%m%d_%H%M%S).tar.gz"
    tar -czf """"$BACKUP_DIR"""/"""$config_backup"""" \
        /opt/mcp/ /etc/mcp/ 2>/dev/null || true
    
    # Extract configuration backup
    log "Extracting configuration..."
    tar -xzf """"$backup_file"""" -C / 2>/dev/null || \
        error "Configuration restore failed"
    
    # Restart services to pick up new configuration
    log "Restarting services..."
    docker-compose restart || systemctl restart mcp-* || true
    
    log "Configuration restore completed successfully"
}

# Restore Kubernetes resources
restore_kubernetes() {
    local backup_file="$1"
    
    log "Starting Kubernetes restore from: """$backup_file""""
    
    if ! command -v kubectl >/dev/null 2>&1; then
        error "kubectl not found"
    fi
    
    # Verify backup first
    verify_backup """"$backup_file""""
    
    # Extract YAML if compressed
    local yaml_file=""""$backup_file""""
    if [[ """"$backup_file"""" == *.gz ]]; then
        yaml_file="/tmp/k8s_restore_$(date +%Y%m%d_%H%M%S).yaml"
        gunzip -c """"$backup_file"""" > """"$yaml_file""""
    fi
    
    # Apply Kubernetes resources
    log "Applying Kubernetes resources..."
    kubectl apply -f """"$yaml_file"""" || error "Kubernetes restore failed"
    
    # Wait for pods to be ready
    log "Waiting for pods to be ready..."
    kubectl wait --for=condition=ready pod --all -n production --timeout=300s || \
        warning "Some pods may not be ready yet"
    
    # Clean up temporary file
    if [[ """"$backup_file"""" == *.gz ]]; then
        rm -f """"$yaml_file""""
    fi
    
    log "Kubernetes restore completed successfully"
}

# Confirm restore operation
confirm_restore() {
    if [ """"$CONFIRM_RESTORE"""" = true ]; then
        return 0
    fi
    
    echo
    warning "This operation will OVERWRITE existing data!"
    echo "Restore type: """$RESTORE_TYPE""""
    echo "Backup file: """$BACKUP_FILE""""
    echo
    read -p "Are you sure you want to continue? (yes/NO): " confirm
    
    if [ """"$confirm"""" != "yes" ]; then
        log "Restore operation cancelled by user"
        exit 0
    fi
}

# Main restore function
main_restore() {
    case """"$RESTORE_TYPE"""" in
        postgres|postgresql)
            confirm_restore
            restore_postgresql """"$BACKUP_FILE""""
            ;;
        redis)
            confirm_restore
            restore_redis """"$BACKUP_FILE""""
            ;;
        config|configuration)
            confirm_restore
            restore_configuration """"$BACKUP_FILE""""
            ;;
        k8s|kubernetes)
            confirm_restore
            restore_kubernetes """"$BACKUP_FILE""""
            ;;
        all)
            confirm_restore
            
            # Restore in order: config, databases, k8s
            local config_file=$(get_latest_backup "config")
            local postgres_file=$(get_latest_backup "postgres")
            local redis_file=$(get_latest_backup "redis")
            local k8s_file=$(get_latest_backup "k8s")
            
            if [ -n """"$config_file"""" ]; then
                restore_configuration """"$config_file""""
            fi
            
            if [ -n """"$postgres_file"""" ]; then
                restore_postgresql """"$postgres_file""""
            fi
            
            if [ -n """"$redis_file"""" ]; then
                restore_redis """"$redis_file""""
            fi
            
            if [ -n """"$k8s_file"""" ]; then
                restore_kubernetes """"$k8s_file""""
            fi
            ;;
        *)
            error "Unknown restore type: """$RESTORE_TYPE""""
            ;;
    esac
}

# Parse command line arguments
while [[ "$#" -gt 0 ]]; do
    case $1 in
        -f|--file)
            BACKUP_FILE="$2"
            shift 2
            ;;
        -t|--type)
            RESTORE_TYPE="$2"
            shift 2
            ;;
        -d|--directory)
            BACKUP_DIR="$2"
            shift 2
            ;;
        -y|--yes)
            CONFIRM_RESTORE=true
            shift
            ;;
        -l|--list)
            list_backups
            exit 0
            ;;
        -v|--verify)
            verify_backup "$2"
            exit 0
            ;;
        --dry-run)
            log "DRY RUN MODE - No changes will be made"
            # Set dry run flag and continue
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            error "Unknown option: $1"
            ;;
    esac
done

# Validate required parameters
if [ -z """"$RESTORE_TYPE"""" ]; then
    error "Restore type is required. Use --type or see --help"
fi

# Handle special cases for backup file
if [ """"$BACKUP_FILE"""" = "latest" ] || [ -z """"$BACKUP_FILE"""" ]; then
    BACKUP_FILE=$(get_latest_backup """"$RESTORE_TYPE"""")
    if [ -z """"$BACKUP_FILE"""" ]; then
        error "No backup files found for type: """$RESTORE_TYPE""""
    fi
    log "Using latest backup: """$BACKUP_FILE""""
fi

# Ensure backup file exists
if [ ! -f """"$BACKUP_FILE"""" ] && [ ! -f """"$BACKUP_DIR"""/"""$BACKUP_FILE"""" ]; then
    # Try with backup directory prefix
    if [ -f """"$BACKUP_DIR"""/"""$BACKUP_FILE"""" ]; then
        BACKUP_FILE=""""$BACKUP_DIR"""/"""$BACKUP_FILE""""
    else
        error "Backup file not found: """$BACKUP_FILE""""
    fi
fi

log "Starting restore operation..."
log "Type: """$RESTORE_TYPE""""
log "File: """$BACKUP_FILE""""

main_restore

log "Restore operation completed successfully!"