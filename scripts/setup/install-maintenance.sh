#!/bin/bash

# Installation script for MCP maintenance and disaster recovery scripts
# This script sets up all necessary components for automated maintenance

set -euo pipefail

# Configuration
MCP_HOME="/opt/mcp"
SCRIPT_DIR="""$MCP_HOME""/scripts"
LOG_DIR="/var/log/mcp"
BACKUP_DIR="/backups"
USER="mcp"
GROUP="mcp"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

error() {
    log "ERROR: $1"
    exit 1
}

# Check if running as root
if [ """$EUID""" -ne 0 ]; then
    error "This script must be run as root"
fi

log "Starting MCP maintenance system installation..."

# Create system user
create_system_user() {
    log "Creating system user and group..."
    
    if ! getent group """$GROUP""" >/dev/null 2>&1; then
        groupadd """$GROUP"""
        log "Created group: ""$GROUP"""
    fi
    
    if ! getent passwd """$USER""" >/dev/null 2>&1; then
        useradd -r -g """$GROUP""" -d """$MCP_HOME""" -s /bin/bash """$USER"""
        log "Created user: ""$USER"""
    fi
}

# Create directory structure
create_directories() {
    log "Creating directory structure..."
    
    local directories=(
        """$MCP_HOME"""
        """$SCRIPT_DIR"""
        """$SCRIPT_DIR""/backup"
        """$SCRIPT_DIR""/recovery"
        """$SCRIPT_DIR""/maintenance"
        """$SCRIPT_DIR""/monitoring"
        """$SCRIPT_DIR""/security"
        """$SCRIPT_DIR""/reporting"
        """$SCRIPT_DIR""/config"
        """$SCRIPT_DIR""/disaster-recovery"
        """$SCRIPT_DIR""/cron"
        """$LOG_DIR"""
        """$BACKUP_DIR"""
        "/etc/mcp"
        "/var/lib/mcp"
    )
    
    for dir in "${directories[@]}"; do
        mkdir -p """$dir"""
        chown """$USER"":""$GROUP""" """$dir"""
        chmod 755 """$dir"""
        log "Created directory: ""$dir"""
    done
    
    # Set special permissions for backup directory
    chmod 750 """$BACKUP_DIR"""
    
    # Set special permissions for log directory
    chmod 755 """$LOG_DIR"""
}

# Copy scripts
copy_scripts() {
    log "Copying maintenance scripts..."
    
    local source_dir="$(dirname "$(dirname "$(readlink -f "$0")")")"
    
    # Copy all scripts
    if [ -d """$source_dir""/backup" ]; then
        cp -r """$source_dir""/backup"/* """$SCRIPT_DIR""/backup/"
    fi
    
    if [ -d """$source_dir""/recovery" ]; then
        cp -r """$source_dir""/recovery"/* """$SCRIPT_DIR""/recovery/"
    fi
    
    if [ -d """$source_dir""/maintenance" ]; then
        cp -r """$source_dir""/maintenance"/* """$SCRIPT_DIR""/maintenance/"
    fi
    
    if [ -d """$source_dir""/cron" ]; then
        cp -r """$source_dir""/cron"/* """$SCRIPT_DIR""/cron/"
    fi
    
    # Make all scripts executable
    find """$SCRIPT_DIR""" -name "*.sh" -exec chmod +x {} \;
    
    # Set ownership
    chown -R """$USER"":""$GROUP""" """$SCRIPT_DIR"""
    
    log "Scripts copied and permissions set"
}

# Install system dependencies
install_dependencies() {
    log "Installing system dependencies..."
    
    # Update package list
    apt-get update
    
    # Install required packages
    local packages=(
        "postgresql-client"
        "redis-tools"
        "curl"
        "jq"
        "bc"
        "awscli"
        "logrotate"
        "rsync"
        "pigz"
        "pv"
    )
    
    for package in "${packages[@]}"; do
        if ! dpkg -l | grep -q "^ii  ""$package"" "; then
            apt-get install -y """$package"""
            log "Installed: ""$package"""
        else
            log "Already installed: ""$package"""
        fi
    done
}

# Configure logrotate
configure_logrotate() {
    log "Configuring log rotation..."
    
    cat > /etc/logrotate.d/mcp <<EOF
""$LOG_DIR""/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    create 644 ""$USER"" $GROUP
    postrotate
        systemctl reload rsyslog > /dev/null 2>&1 || true
    endscript
}

""$LOG_DIR""/backup/*.log {
    daily
    rotate 90
    compress
    delaycompress
    missingok
    notifempty
    create 644 ""$USER"" $GROUP
}

""$LOG_DIR""/health-check/*.json {
    hourly
    rotate 168
    compress
    delaycompress
    missingok
    notifempty
    create 644 ""$USER"" $GROUP
}
EOF
    
    log "Logrotate configuration created"
}

# Install cron jobs
install_cron_jobs() {
    log "Installing cron jobs..."
    
    # Copy cron file
    cp """$SCRIPT_DIR""/cron/mcp-maintenance.cron" /etc/cron.d/mcp-maintenance
    
    # Set proper permissions
    chown root:root /etc/cron.d/mcp-maintenance
    chmod 644 /etc/cron.d/mcp-maintenance
    
    # Restart cron service
    systemctl restart cron
    
    log "Cron jobs installed and cron service restarted"
}

# Create configuration files
create_config_files() {
    log "Creating configuration files..."
    
    # Main configuration file
    cat > /etc/mcp/maintenance.conf <<EOF
# MCP Maintenance Configuration

# Backup settings
BACKUP_DIR="""$BACKUP_DIR"""
BACKUP_RETENTION_DAYS=30
BACKUP_COMPRESSION_LEVEL=6
S3_BUCKET=""
S3_PREFIX="backups"

# Database settings
DB_HOST="localhost"
DB_PORT=5432
DB_NAME="debate_db"
DB_USER="postgres"
DB_PASSWORD=""

# Redis settings
REDIS_HOST="localhost"
REDIS_PORT=6379
REDIS_PASSWORD=""

# Notification settings
SLACK_WEBHOOK=""
EMAIL_TO=""

# Health check settings
HEALTH_CHECK_TIMEOUT=30
CRITICAL_THRESHOLD=90
WARNING_THRESHOLD=80

# Cleanup settings
LOG_RETENTION_DAYS=30
TEMP_CLEANUP_DAYS=7
DOCKER_CLEANUP=true

# Monitoring settings
PERFORMANCE_CHECK_INTERVAL=300
METRIC_COLLECTION_INTERVAL=60
EOF
    
    # Environment file template
    cat > /etc/mcp/environment.template <<EOF
# MCP Environment Variables Template
# Copy to /etc/mcp/environment and fill in actual values

# Database credentials
export DB_PASSWORD="your_db_password"
export REDIS_PASSWORD="your_redis_password"

# Notification settings
export SLACK_WEBHOOK="https://hooks.slack.com/services/your/webhook/url"
export EMAIL_TO="admin@your-domain.com"

# AWS credentials (for S3 backups)
export AWS_ACCESS_KEY_ID="your_access_key"
export AWS_SECRET_ACCESS_KEY="your_secret_key"
export AWS_DEFAULT_REGION="us-east-1"

# Backup settings
export S3_BUCKET="your-backup-bucket"
export BACKUP_ENCRYPTION_KEY="your_encryption_key"
EOF
    
    # Set permissions
    chmod 640 /etc/mcp/maintenance.conf
    chmod 600 /etc/mcp/environment.template
    chown """$USER"":""$GROUP""" /etc/mcp/maintenance.conf
    chown """$USER"":""$GROUP""" /etc/mcp/environment.template
    
    log "Configuration files created"
}

# Create systemd services
create_systemd_services() {
    log "Creating systemd services..."
    
    # Health monitoring service
    cat > /etc/systemd/system/mcp-health-monitor.service <<EOF
[Unit]
Description=MCP Health Monitor
After=network.target

[Service]
Type=simple
User=$USER
Group=$GROUP
WorkingDirectory=$MCP_HOME
Environment=PATH=/usr/local/bin:/usr/bin:/bin
EnvironmentFile=-/etc/mcp/environment
ExecStart=""$SCRIPT_DIR""/monitoring/health-monitor-daemon.sh
Restart=always
RestartSec=30
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF
    
    # Backup service
    cat > /etc/systemd/system/mcp-backup.service <<EOF
[Unit]
Description=MCP Backup Service
After=network.target

[Service]
Type=oneshot
User=$USER
Group=$GROUP
WorkingDirectory=$MCP_HOME
Environment=PATH=/usr/local/bin:/usr/bin:/bin
EnvironmentFile=-/etc/mcp/environment
ExecStart=""$SCRIPT_DIR""/backup/backup-database.sh
StandardOutput=journal
StandardError=journal
EOF
    
    # Backup timer
    cat > /etc/systemd/system/mcp-backup.timer <<EOF
[Unit]
Description=Run MCP backup daily
Requires=mcp-backup.service

[Timer]
OnCalendar=daily
Persistent=true

[Install]
WantedBy=timers.target
EOF
    
    # Reload systemd and enable services
    systemctl daemon-reload
    systemctl enable mcp-backup.timer
    systemctl start mcp-backup.timer
    
    log "Systemd services created and enabled"
}

# Create monitoring scripts
create_monitoring_scripts() {
    log "Creating additional monitoring scripts..."
    
    # API health monitor
    cat > """$SCRIPT_DIR""/monitoring/api-health.sh" <<'EOF'
#!/bin/bash
# API Health Monitor

ENDPOINTS=(
    "http://localhost:8080/actuator/health"
    "http://localhost:5005/actuator/health"
    "http://localhost:5013/actuator/health"
    "http://localhost:5002/actuator/health"
    "http://localhost:5004/actuator/health"
)

for endpoint in "${ENDPOINTS[@]}"; do
    if ! curl -sf """$endpoint""" >/dev/null; then
        echo "CRITICAL: ""$endpoint"" is down"
        exit 2
    fi
done

echo "OK: All API endpoints are healthy"
EOF
    
    # Container health monitor
    cat > """$SCRIPT_DIR""/monitoring/container-health.sh" <<'EOF'
#!/bin/bash
# Container Health Monitor

unhealthy_containers=$(docker ps --filter health=unhealthy --format "{{.Names}}")

if [ -n """$unhealthy_containers""" ]; then
    echo "CRITICAL: Unhealthy containers found: ""$unhealthy_containers"""
    exit 2
fi

echo "OK: All containers are healthy"
EOF
    
    # Network connectivity check
    cat > """$SCRIPT_DIR""/monitoring/network-check.sh" <<'EOF'
#!/bin/bash
# Network Connectivity Check

hosts=(
    "google.com"
    "api.openai.com"
    "api.anthropic.com"
)

for host in "${hosts[@]}"; do
    if ! ping -c 1 -W 5 """$host""" >/dev/null 2>&1; then
        echo "WARNING: Cannot reach ""$host"""
    fi
done

echo "OK: Network connectivity verified"
EOF
    
    # Make scripts executable
    chmod +x """$SCRIPT_DIR""/monitoring"/*.sh
    
    log "Monitoring scripts created"
}

# Create maintenance tools
create_maintenance_tools() {
    log "Creating maintenance tools..."
    
    # Quick status script
    cat > """$SCRIPT_DIR""/maintenance/quick-status.sh" <<'EOF'
#!/bin/bash
# Quick System Status

echo "=== MCP System Status ==="
echo "Time: $(date)"
echo

echo "=== Services ==="
docker-compose ps || echo "Docker Compose not available"
echo

echo "=== System Resources ==="
echo "CPU: $(top -bn1 | grep "Cpu(s)" | awk '{print $2}')"
echo "Memory: $(free -h | awk 'NR==2{printf "%.1f%%", $3*100/$2}')"
echo "Disk: $(df -h / | awk 'NR==2{print $5}')"
echo

echo "=== Database ==="
if pg_isready -h localhost -p 5432 >/dev/null 2>&1; then
    echo "PostgreSQL: UP"
else
    echo "PostgreSQL: DOWN"
fi

if redis-cli ping >/dev/null 2>&1; then
    echo "Redis: UP"
else
    echo "Redis: DOWN"
fi
echo

echo "=== Recent Logs ==="
tail -5 /var/log/mcp/*.log 2>/dev/null || echo "No recent logs"
EOF
    
    chmod +x """$SCRIPT_DIR""/maintenance/quick-status.sh"
    
    log "Maintenance tools created"
}

# Test installation
test_installation() {
    log "Testing installation..."
    
    # Test script execution
    if ! """$SCRIPT_DIR""/maintenance/quick-status.sh" >/dev/null 2>&1; then
        error "Quick status script test failed"
    fi
    
    # Test cron syntax
    if ! crontab -T /etc/cron.d/mcp-maintenance >/dev/null 2>&1; then
        error "Cron syntax test failed"
    fi
    
    # Test directory permissions
    if ! sudo -u """$USER""" touch """$LOG_DIR""/test.log" 2>/dev/null; then
        error "Log directory permissions test failed"
    fi
    rm -f """$LOG_DIR""/test.log"
    
    # Test backup directory
    if ! sudo -u """$USER""" touch """$BACKUP_DIR""/test.backup" 2>/dev/null; then
        error "Backup directory permissions test failed"
    fi
    rm -f """$BACKUP_DIR""/test.backup"
    
    log "All tests passed"
}

# Create installation summary
create_summary() {
    log "Creating installation summary..."
    
    cat > """$MCP_HOME""/INSTALLATION_SUMMARY.txt" <<EOF
MCP Maintenance System Installation Summary
==========================================

Installation Date: $(date)
MCP Home: $MCP_HOME
User/Group: ""$USER"":$GROUP

Installed Components:
- Backup scripts: ""$SCRIPT_DIR""/backup/
- Recovery scripts: ""$SCRIPT_DIR""/recovery/
- Maintenance scripts: ""$SCRIPT_DIR""/maintenance/
- Monitoring scripts: ""$SCRIPT_DIR""/monitoring/
- Cron jobs: /etc/cron.d/mcp-maintenance
- Configuration: /etc/mcp/
- Logs: ""$LOG_DIR""/
- Backups: ""$BACKUP_DIR""/

Systemd Services:
- mcp-backup.service (scheduled via timer)
- mcp-backup.timer (daily backups)

Next Steps:
1. Review and update /etc/mcp/maintenance.conf
2. Copy and customize /etc/mcp/environment.template to /etc/mcp/environment
3. Test backup: ""$SCRIPT_DIR""/backup/backup-database.sh --dry-run
4. Test recovery: ""$SCRIPT_DIR""/recovery/restore-database.sh --help
5. Run health check: ""$SCRIPT_DIR""/maintenance/health-check.sh
6. Check status: ""$SCRIPT_DIR""/maintenance/quick-status.sh

Important Notes:
- Set proper passwords in /etc/mcp/environment
- Configure Slack webhook for notifications
- Set up S3 bucket for offsite backups
- Test disaster recovery procedures monthly

Documentation:
- See ""$MCP_HOME""/docs/ for detailed documentation
- Disaster recovery plan: docs/operations/disaster-recovery.md
EOF
    
    chown """$USER"":""$GROUP""" """$MCP_HOME""/INSTALLATION_SUMMARY.txt"
    
    log "Installation summary created: ""$MCP_HOME""/INSTALLATION_SUMMARY.txt"
}

# Main installation function
main() {
    log "MCP Maintenance System Installation"
    log "===================================="
    
    create_system_user
    create_directories
    install_dependencies
    copy_scripts
    configure_logrotate
    create_config_files
    install_cron_jobs
    create_systemd_services
    create_monitoring_scripts
    create_maintenance_tools
    test_installation
    create_summary
    
    log ""
    log "Installation completed successfully!"
    log ""
    log "Next steps:"
    log "1. Review configuration: /etc/mcp/maintenance.conf"
    log "2. Set up environment: cp /etc/mcp/environment.template /etc/mcp/environment"
    log "3. Test backup: ""$SCRIPT_DIR""/backup/backup-database.sh --dry-run"
    log "4. Check status: ""$SCRIPT_DIR""/maintenance/quick-status.sh"
    log ""
    log "For detailed information, see: ""$MCP_HOME""/INSTALLATION_SUMMARY.txt"
}

# Run main function
main "$@"