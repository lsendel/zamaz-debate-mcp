#!/bin/bash

# Security Monitoring and Alerting Setup
# Configures continuous security monitoring for the application

set -e

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
MONITORING_DIR="${PROJECT_ROOT}/monitoring"
ALERTS_DIR="${MONITORING_DIR}/alerts"
DASHBOARDS_DIR="${MONITORING_DIR}/dashboards"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Create monitoring directories
setup_monitoring_structure() {
    log_info "Setting up monitoring directory structure..."
    
    mkdir -p "$MONITORING_DIR"
    mkdir -p "$ALERTS_DIR"
    mkdir -p "$DASHBOARDS_DIR"
    mkdir -p "${MONITORING_DIR}/scripts"
    mkdir -p "${MONITORING_DIR}/configs"
    
    log_success "Monitoring directories created"
}

# Create Prometheus configuration for security metrics
create_prometheus_config() {
    log_info "Creating Prometheus security monitoring configuration..."
    
    cat > "${MONITORING_DIR}/configs/prometheus-security.yml" << 'EOF'
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "security-alerts.yml"

scrape_configs:
  # Application metrics
  - job_name: 'zamaz-debate-security'
    static_configs:
      - targets: ['localhost:8080', 'localhost:8081', 'localhost:8082']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    
  # System metrics
  - job_name: 'node-exporter'
    static_configs:
      - targets: ['localhost:9100']
    
  # Docker metrics
  - job_name: 'cadvisor'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/metrics'
    
  # Application logs
  - job_name: 'loki'
    static_configs:
      - targets: ['localhost:3100']

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - localhost:9093
EOF

    log_success "Prometheus configuration created"
}

# Create security alerting rules
create_security_alerts() {
    log_info "Creating security alerting rules..."
    
    cat > "${ALERTS_DIR}/security-alerts.yml" << 'EOF'
groups:
  - name: security.rules
    rules:
      # Authentication failures
      - alert: HighAuthenticationFailures
        expr: increase(spring_security_authentication_failure_total[5m]) > 10
        for: 1m
        labels:
          severity: warning
          category: security
        annotations:
          summary: "High number of authentication failures"
          description: "{{ $value }} authentication failures in the last 5 minutes"
          
      # Suspicious API activity
      - alert: SuspiciousAPIActivity
        expr: increase(http_requests_total{status=~"4.."}[1m]) > 100
        for: 30s
        labels:
          severity: critical
          category: security
        annotations:
          summary: "Suspicious API activity detected"
          description: "High rate of 4xx responses: {{ $value }} requests per minute"
          
      # Database connection anomalies
      - alert: DatabaseConnectionSpike
        expr: increase(hikaricp_connections_active[2m]) > 50
        for: 1m
        labels:
          severity: warning
          category: security
        annotations:
          summary: "Database connection spike detected"
          description: "Active database connections: {{ $value }}"
          
      # Memory usage anomaly (potential DoS)
      - alert: MemoryUsageAnomaly
        expr: (node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes > 0.9
        for: 2m
        labels:
          severity: critical
          category: security
        annotations:
          summary: "High memory usage detected"
          description: "Memory usage is above 90%: {{ $value }}%"
          
      # Disk usage anomaly
      - alert: DiskUsageAnomaly
        expr: (node_filesystem_size_bytes - node_filesystem_free_bytes) / node_filesystem_size_bytes > 0.85
        for: 5m
        labels:
          severity: warning
          category: security
        annotations:
          summary: "High disk usage detected"
          description: "Disk usage is above 85%: {{ $value }}%"
          
      # JWT token anomalies
      - alert: JWTTokenAnomalies
        expr: increase(jwt_validation_failure_total[5m]) > 5
        for: 1m
        labels:
          severity: warning
          category: security
        annotations:
          summary: "JWT token validation failures"
          description: "{{ $value }} JWT validation failures in 5 minutes"
          
      # Rate limiting triggered
      - alert: RateLimitingTriggered
        expr: increase(rate_limit_exceeded_total[1m]) > 0
        for: 0s
        labels:
          severity: info
          category: security
        annotations:
          summary: "Rate limiting activated"
          description: "Rate limiting has been triggered {{ $value }} times"
          
      # Security scanner detection
      - alert: SecurityScannerDetected
        expr: increase(http_requests_total{user_agent=~".*nmap.*|.*nikto.*|.*sqlmap.*|.*burp.*"}[1m]) > 0
        for: 0s
        labels:
          severity: critical
          category: security
        annotations:
          summary: "Security scanner detected"
          description: "Potential security scanner activity detected"
          
      # Application startup failures
      - alert: ApplicationStartupFailure
        expr: up{job="zamaz-debate-security"} == 0
        for: 30s
        labels:
          severity: critical
          category: availability
        annotations:
          summary: "Application is down"
          description: "Application {{ $labels.instance }} is not responding"
EOF

    log_success "Security alerting rules created"
}

# Create Grafana security dashboard
create_security_dashboard() {
    log_info "Creating Grafana security dashboard..."
    
    cat > "${DASHBOARDS_DIR}/security-dashboard.json" << 'EOF'
{
  "dashboard": {
    "id": null,
    "title": "Security Monitoring Dashboard",
    "tags": ["security", "monitoring"],
    "timezone": "browser",
    "panels": [
      {
        "id": 1,
        "title": "Authentication Failures",
        "type": "stat",
        "targets": [
          {
            "expr": "increase(spring_security_authentication_failure_total[1h])",
            "refId": "A"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "color": {
              "mode": "thresholds"
            },
            "thresholds": {
              "steps": [
                {"color": "green", "value": null},
                {"color": "yellow", "value": 10},
                {"color": "red", "value": 50}
              ]
            }
          }
        },
        "gridPos": {"h": 8, "w": 6, "x": 0, "y": 0}
      },
      {
        "id": 2,
        "title": "API Response Codes",
        "type": "timeseries",
        "targets": [
          {
            "expr": "rate(http_requests_total[5m])",
            "refId": "A",
            "legendFormat": "{{status}}"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 6, "y": 0}
      },
      {
        "id": 3,
        "title": "JWT Token Metrics",
        "type": "timeseries",
        "targets": [
          {
            "expr": "rate(jwt_validation_success_total[5m])",
            "refId": "A",
            "legendFormat": "Successful Validations"
          },
          {
            "expr": "rate(jwt_validation_failure_total[5m])",
            "refId": "B",
            "legendFormat": "Failed Validations"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 8}
      },
      {
        "id": 4,
        "title": "System Resource Usage",
        "type": "timeseries",
        "targets": [
          {
            "expr": "(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100",
            "refId": "A",
            "legendFormat": "Memory Usage %"
          },
          {
            "expr": "100 - (avg(rate(node_cpu_seconds_total{mode='idle'}[5m])) * 100)",
            "refId": "B",
            "legendFormat": "CPU Usage %"
          }
        ],
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 8}
      },
      {
        "id": 5,
        "title": "Security Events Log",
        "type": "logs",
        "targets": [
          {
            "expr": "{job=\"loki\"} |= \"SECURITY\" or \"AUTH\" or \"UNAUTHORIZED\"",
            "refId": "A"
          }
        ],
        "gridPos": {"h": 8, "w": 24, "x": 0, "y": 16}
      }
    ],
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "refresh": "5s"
  }
}
EOF

    log_success "Security dashboard created"
}

# Create security monitoring scripts
create_monitoring_scripts() {
    log_info "Creating security monitoring scripts..."
    
    # Real-time security monitor
    cat > "${MONITORING_DIR}/scripts/realtime-security-monitor.sh" << 'EOF'
#!/bin/bash

# Real-time Security Monitor
# Monitors logs and metrics for security events

MONITOR_LOG="/tmp/security-monitor.log"
ALERT_THRESHOLD=5

log_with_timestamp() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$MONITOR_LOG"
}

monitor_auth_failures() {
    local failures=$(grep "authentication failed" /var/log/app.log 2>/dev/null | tail -n 10 | wc -l)
    if [ "$failures" -gt "$ALERT_THRESHOLD" ]; then
        log_with_timestamp "ALERT: High authentication failures detected ($failures)"
        # Send alert (webhook, email, etc.)
    fi
}

monitor_suspicious_ips() {
    local suspicious_ips=$(awk '{print $1}' /var/log/nginx/access.log 2>/dev/null | sort | uniq -c | sort -nr | head -5)
    log_with_timestamp "Top IPs: $suspicious_ips"
}

monitor_error_rates() {
    local error_rate=$(grep "ERROR" /var/log/app.log 2>/dev/null | tail -n 50 | wc -l)
    if [ "$error_rate" -gt 20 ]; then
        log_with_timestamp "ALERT: High error rate detected ($error_rate)"
    fi
}

while true; do
    monitor_auth_failures
    monitor_suspicious_ips
    monitor_error_rates
    sleep 30
done
EOF

    # Security health check
    cat > "${MONITORING_DIR}/scripts/security-health-check.sh" << 'EOF'
#!/bin/bash

# Security Health Check
# Validates that security controls are functioning

HEALTH_REPORT="/tmp/security-health-$(date +%Y%m%d).json"

check_jwt_endpoint() {
    local response=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/api/auth/validate-token")
    if [ "$response" = "401" ] || [ "$response" = "403" ]; then
        echo '"jwt_protection": "healthy"'
    else
        echo '"jwt_protection": "unhealthy"'
    fi
}

check_rate_limiting() {
    local response=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/api/test/rate-limit")
    if [ "$response" = "429" ]; then
        echo '"rate_limiting": "healthy"'
    else
        echo '"rate_limiting": "needs_check"'
    fi
}

check_https_redirect() {
    local response=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/")
    if [ "$response" = "301" ] || [ "$response" = "302" ]; then
        echo '"https_redirect": "healthy"'
    else
        echo '"https_redirect": "disabled"'
    fi
}

# Generate health report
{
    echo "{"
    echo '"timestamp": "'$(date -Iseconds)'",'    
    check_jwt_endpoint | sed 's/$/,/'
    check_rate_limiting | sed 's/$/,/'
    check_https_redirect
    echo "}"
} > "$HEALTH_REPORT"

echo "Security health check completed: $HEALTH_REPORT"
EOF

    chmod +x "${MONITORING_DIR}/scripts/"*.sh
    
    log_success "Monitoring scripts created and made executable"
}

# Create alerting configuration
create_alerting_config() {
    log_info "Creating alerting configuration..."
    
    cat > "${MONITORING_DIR}/configs/alertmanager.yml" << 'EOF'
global:
  smtp_smarthost: 'localhost:587'
  smtp_from: 'alerts@zamaz-debate.com'

route:
  group_by: ['alertname']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'security-team'
  routes:
  - match:
      severity: critical
    receiver: 'security-team-critical'
  - match:
      category: security
    receiver: 'security-team'

receivers:
- name: 'security-team'
  email_configs:
  - to: 'security@zamaz-debate.com'
    subject: 'Security Alert: {{ .GroupLabels.alertname }}'
    body: |
      {{ range .Alerts }}
      Alert: {{ .Annotations.summary }}
      Description: {{ .Annotations.description }}
      {{ end }}
  webhook_configs:
  - url: 'http://localhost:9093/api/v1/alerts'
    send_resolved: true

- name: 'security-team-critical'
  email_configs:
  - to: 'security@zamaz-debate.com'
    subject: 'CRITICAL Security Alert: {{ .GroupLabels.alertname }}'
    body: |
      CRITICAL SECURITY EVENT DETECTED
      
      {{ range .Alerts }}
      Alert: {{ .Annotations.summary }}
      Description: {{ .Annotations.description }}
      Time: {{ .StartsAt }}
      {{ end }}
  slack_configs:
  - api_url: '${SLACK_WEBHOOK_URL}'
    channel: '#security-alerts'
    title: 'CRITICAL: {{ .GroupLabels.alertname }}'
    text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'
EOF

    log_success "Alerting configuration created"
}

# Create docker-compose for monitoring stack
create_monitoring_stack() {
    log_info "Creating monitoring stack docker-compose..."
    
    cat > "${MONITORING_DIR}/docker-compose-monitoring.yml" << 'EOF'
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus-security
    ports:
      - "9090:9090"
    volumes:
      - ./configs/prometheus-security.yml:/etc/prometheus/prometheus.yml
      - ./alerts:/etc/prometheus/rules
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--web.enable-lifecycle'
      - '--web.enable-admin-api'
    restart: unless-stopped

  alertmanager:
    image: prom/alertmanager:latest
    container_name: alertmanager-security
    ports:
      - "9093:9093"
    volumes:
      - ./configs/alertmanager.yml:/etc/alertmanager/alertmanager.yml
    restart: unless-stopped

  grafana:
    image: grafana/grafana:latest
    container_name: grafana-security
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_PASSWORD:-admin123}
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - grafana-storage:/var/lib/grafana
      - ./dashboards:/etc/grafana/provisioning/dashboards
    restart: unless-stopped

  node-exporter:
    image: prom/node-exporter:latest
    container_name: node-exporter
    ports:
      - "9100:9100"
    volumes:
      - /proc:/host/proc:ro
      - /sys:/host/sys:ro
      - /:/rootfs:ro
    command:
      - '--path.procfs=/host/proc'
      - '--path.rootfs=/rootfs'
      - '--path.sysfs=/host/sys'
      - '--collector.filesystem.mount-points-exclude=^/(sys|proc|dev|host|etc)($$|/)'
    restart: unless-stopped

volumes:
  grafana-storage:
EOF

    log_success "Monitoring stack docker-compose created"
}

# Create monitoring documentation
create_monitoring_docs() {
    log_info "Creating monitoring documentation..."
    
    cat > "${MONITORING_DIR}/README.md" << 'EOF'
# Security Monitoring Setup

## Overview
This directory contains security monitoring configuration for the zamaz-debate-mcp project.

## Components

### Prometheus
- **Port**: 9090
- **Config**: `configs/prometheus-security.yml`
- **Purpose**: Metrics collection and alerting rules

### Alertmanager
- **Port**: 9093
- **Config**: `configs/alertmanager.yml`
- **Purpose**: Alert routing and notifications

### Grafana
- **Port**: 3000
- **Login**: admin/admin123 (change on first login)
- **Purpose**: Security dashboards and visualization

### Node Exporter
- **Port**: 9100
- **Purpose**: System metrics collection

## Quick Start

1. **Start monitoring stack**:
   ```bash
   cd monitoring
   docker-compose -f docker-compose-monitoring.yml up -d
   ```

2. **Access interfaces**:
   - Prometheus: http://localhost:9090
   - Grafana: http://localhost:3000
   - Alertmanager: http://localhost:9093

3. **Import security dashboard**:
   - Login to Grafana
   - Go to + → Import
   - Upload `dashboards/security-dashboard.json`

## Security Alerts

### Configured Alerts
- High authentication failures
- Suspicious API activity
- Database connection anomalies
- Memory/disk usage spikes
- JWT token validation failures
- Security scanner detection

### Alert Channels
- Email notifications
- Slack integration (configure webhook)
- Webhook for custom integrations

## Monitoring Scripts

### Real-time Monitor
```bash
./scripts/realtime-security-monitor.sh
```
Continuous monitoring of security events.

### Health Check
```bash
./scripts/security-health-check.sh
```
Validates security controls are functioning.

## Configuration

### Environment Variables
- `GRAFANA_PASSWORD`: Grafana admin password
- `SLACK_WEBHOOK_URL`: Slack alerts webhook
- `SMTP_SERVER`: Email server for alerts

### Customization
1. Edit `configs/prometheus-security.yml` for metrics
2. Modify `alerts/security-alerts.yml` for alert rules
3. Update `configs/alertmanager.yml` for notification routing

## Troubleshooting

### Common Issues
1. **Metrics not appearing**: Check application `/actuator/prometheus` endpoint
2. **Alerts not firing**: Verify Prometheus can scrape targets
3. **Notifications not sent**: Check Alertmanager configuration

### Logs
```bash
docker-compose -f docker-compose-monitoring.yml logs [service-name]
```

## Security Considerations

1. **Access Control**: Restrict monitoring interface access
2. **Data Retention**: Configure appropriate retention policies
3. **Alert Fatigue**: Tune alert thresholds to reduce false positives
4. **Credential Management**: Use secrets management for sensitive configs
EOF

    log_success "Monitoring documentation created"
}

# Main execution
main() {
    echo "================================================"
    echo "Security Monitoring Setup"
    echo "================================================"
    echo
    
    log_info "Setting up comprehensive security monitoring..."
    
    setup_monitoring_structure
    create_prometheus_config
    create_security_alerts
    create_security_dashboard
    create_monitoring_scripts
    create_alerting_config
    create_monitoring_stack
    create_monitoring_docs
    
    echo
    echo "================================================"
    log_success "Security monitoring setup completed!"
    log_info "Monitoring directory: $MONITORING_DIR"
    log_info "To start monitoring: cd monitoring && docker-compose -f docker-compose-monitoring.yml up -d"
    echo "================================================"
}

# Run main function
main "$@"
