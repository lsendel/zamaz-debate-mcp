#!/bin/bash

# Security Monitoring Script
# This script sets up and configures security monitoring for the project

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
CONFIG_DIR="monitoring"
ACTION=""

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

show_help() {
    echo "Security Monitoring Script"
    echo ""
    echo "Usage: $0 [options] <action>"
    echo ""
    echo "Actions:"
    echo "  setup       Set up security monitoring"
    echo "  start       Start security monitoring"
    echo "  stop        Stop security monitoring"
    echo "  status      Check security monitoring status"
    echo "  report      Generate security monitoring report"
    echo ""
    echo "Options:"
    echo "  -c, --config <dir>    Configuration directory (default: monitoring)"
    echo "  -h, --help            Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 setup              Set up security monitoring"
    echo "  $0 start              Start security monitoring"
    echo "  $0 report             Generate security monitoring report"
}

# Parse arguments
while [[ "$#" -gt 0 ]]; do
    case $1 in
        setup|start|stop|status|report)
            ACTION="$1"
            shift
            ;;
        -c|--config)
            CONFIG_DIR="$2"
            shift 2
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        -*)
            log_error "Unknown option: $1"
            show_help
            exit 1
            ;;
        *)
            log_error "Unknown argument: $1"
            show_help
            exit 1
            ;;
    esac
done

# Validate arguments
if [[ -z """"$ACTION"""" ]]; then
    log_error "No action specified"
    show_help
    exit 1
fi

# Create configuration directory if it doesn't exist
mkdir -p """"$CONFIG_DIR""""

# Function to check if Docker is running
check_docker() {
    if ! docker info &> /dev/null; then
        log_error "Docker is not running"
        exit 1
    fi
}

# Function to check if Docker Compose is installed
check_docker_compose() {
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose is not installed"
        exit 1
    fi
}

# Function to create Prometheus configuration
create_prometheus_config() {
    log_info "Creating Prometheus configuration"
    
    mkdir -p """"$CONFIG_DIR"""/prometheus"
    
    cat > """"$CONFIG_DIR"""/prometheus/prometheus.yml" << EOF
global:
  scrape_interval: 15s
  evaluation_interval: 15s

alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']

rule_files:
  - "alert_rules.yml"

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'spring-actuator'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s
    static_configs:
      - targets: ['mcp-organization:5005', 'mcp-controller:5013', 'mcp-llm:5002', 'mcp-rag:5004', 'mcp-template:5006']

  - job_name: 'node-exporter'
    static_configs:
      - targets: ['node-exporter:9100']

  - job_name: 'cadvisor'
    static_configs:
      - targets: ['cadvisor:8080']
EOF

    cat > """"$CONFIG_DIR"""/prometheus/alert_rules.yml" << EOF
groups:
  - name: security_alerts
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[1m]) > 0.1
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "High HTTP error rate"
          description: "High rate of HTTP 5xx errors detected"

      - alert: HighAuthFailureRate
        expr: rate(security_authentication_failures_total[5m]) > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High authentication failure rate"
          description: "High rate of authentication failures detected"

      - alert: ContainerHighCPU
        expr: (sum by(name) (rate(container_cpu_usage_seconds_total{name!=""}[1m])) / scalar(count(node_cpu_seconds_total{mode="idle"}))) * 100 > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Container high CPU usage"
          description: "Container {{ \"""$labels""".name }} CPU usage is above 80%"

      - alert: ContainerHighMemory
        expr: (container_memory_usage_bytes{name!=""} / container_spec_memory_limit_bytes{name!=""} * 100) > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Container high memory usage"
          description: "Container {{ \"""$labels""".name }} memory usage is above 80%"

      - alert: HighRateLimit
        expr: rate(rate_limit_exceeded_total[5m]) > 0.1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High rate limit exceeded"
          description: "High rate of rate limit exceeded detected"
EOF

    log_success "Prometheus configuration created"
}

# Function to create Alertmanager configuration
create_alertmanager_config() {
    log_info "Creating Alertmanager configuration"
    
    mkdir -p """"$CONFIG_DIR"""/alertmanager"
    
    cat > """"$CONFIG_DIR"""/alertmanager/config.yml" << EOF
global:
  resolve_timeout: 5m

route:
  group_by: ['alertname', 'job']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 12h
  receiver: 'email-notifications'
  routes:
  - match:
      severity: critical
    receiver: 'slack-notifications'
    continue: true

receivers:
- name: 'email-notifications'
  email_configs:
  - to: 'security-team@example.com'
    from: 'alertmanager@example.com'
    smarthost: 'smtp.example.com:587'
    auth_username: 'alertmanager'
    auth_identity: 'alertmanager'
    auth_password: '{{ .SMTP_PASSWORD }}'

- name: 'slack-notifications'
  slack_configs:
  - api_url: 'https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX'
    channel: '#security-alerts'
    send_resolved: true
    title: '{{ template "slack.default.title" . }}'
    text: '{{ template "slack.default.text" . }}'

templates:
- '/etc/alertmanager/template/*.tmpl'
EOF

    log_success "Alertmanager configuration created"
}

# Function to create Grafana configuration
create_grafana_config() {
    log_info "Creating Grafana configuration"
    
    mkdir -p """"$CONFIG_DIR"""/grafana/dashboards"
    mkdir -p """"$CONFIG_DIR"""/grafana/provisioning/dashboards"
    mkdir -p """"$CONFIG_DIR"""/grafana/provisioning/datasources"
    
    cat > """"$CONFIG_DIR"""/grafana/provisioning/datasources/datasource.yml" << EOF
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: false
EOF

    cat > """"$CONFIG_DIR"""/grafana/provisioning/dashboards/dashboard.yml" << EOF
apiVersion: 1

providers:
  - name: 'Default'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    updateIntervalSeconds: 10
    options:
      path: /etc/grafana/dashboards
EOF

    cat > """"$CONFIG_DIR"""/grafana/dashboards/security-dashboard.json" << EOF
{
  "annotations": {
    "list": [
      {
        "builtIn": 1,
        "datasource": "-- Grafana --",
        "enable": true,
        "hide": true,
        "iconColor": "rgba(0, 211, 255, 1)",
        "name": "Annotations & Alerts",
        "type": "dashboard"
      }
    ]
  },
  "editable": true,
  "gnetId": null,
  "graphTooltip": 0,
  "id": 1,
  "links": [],
  "panels": [
    {
      "aliasColors": {},
      "bars": false,
      "dashLength": 10,
      "dashes": false,
      "datasource": "Prometheus",
      "fieldConfig": {
        "defaults": {
          "custom": {}
        },
        "overrides": []
      },
      "fill": 1,
      "fillGradient": 0,
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 0,
        "y": 0
      },
      "hiddenSeries": false,
      "id": 2,
      "legend": {
        "avg": false,
        "current": false,
        "max": false,
        "min": false,
        "show": true,
        "total": false,
        "values": false
      },
      "lines": true,
      "linewidth": 1,
      "nullPointMode": "null",
      "options": {
        "alertThreshold": true
      },
      "percentage": false,
      "pluginVersion": "7.3.7",
      "pointradius": 2,
      "points": false,
      "renderer": "flot",
      "seriesOverrides": [],
      "spaceLength": 10,
      "stack": false,
      "steppedLine": false,
      "targets": [
        {
          "expr": "rate(http_server_requests_seconds_count{status=~\"5..\"}[1m])",
          "interval": "",
          "legendFormat": "{{instance}} - {{status}}",
          "refId": "A"
        }
      ],
      "thresholds": [],
      "timeFrom": null,
      "timeRegions": [],
      "timeShift": null,
      "title": "HTTP 5xx Error Rate",
      "tooltip": {
        "shared": true,
        "sort": 0,
        "value_type": "individual"
      },
      "type": "graph",
      "xaxis": {
        "buckets": null,
        "mode": "time",
        "name": null,
        "show": true,
        "values": []
      },
      "yaxes": [
        {
          "format": "short",
          "label": null,
          "logBase": 1,
          "max": null,
          "min": null,
          "show": true
        },
        {
          "format": "short",
          "label": null,
          "logBase": 1,
          "max": null,
          "min": null,
          "show": true
        }
      ],
      "yaxis": {
        "align": false,
        "alignLevel": null
      }
    },
    {
      "aliasColors": {},
      "bars": false,
      "dashLength": 10,
      "dashes": false,
      "datasource": "Prometheus",
      "fieldConfig": {
        "defaults": {
          "custom": {}
        },
        "overrides": []
      },
      "fill": 1,
      "fillGradient": 0,
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 12,
        "y": 0
      },
      "hiddenSeries": false,
      "id": 4,
      "legend": {
        "avg": false,
        "current": false,
        "max": false,
        "min": false,
        "show": true,
        "total": false,
        "values": false
      },
      "lines": true,
      "linewidth": 1,
      "nullPointMode": "null",
      "options": {
        "alertThreshold": true
      },
      "percentage": false,
      "pluginVersion": "7.3.7",
      "pointradius": 2,
      "points": false,
      "renderer": "flot",
      "seriesOverrides": [],
      "spaceLength": 10,
      "stack": false,
      "steppedLine": false,
      "targets": [
        {
          "expr": "rate(security_authentication_failures_total[5m])",
          "interval": "",
          "legendFormat": "{{instance}}",
          "refId": "A"
        }
      ],
      "thresholds": [],
      "timeFrom": null,
      "timeRegions": [],
      "timeShift": null,
      "title": "Authentication Failures",
      "tooltip": {
        "shared": true,
        "sort": 0,
        "value_type": "individual"
      },
      "type": "graph",
      "xaxis": {
        "buckets": null,
        "mode": "time",
        "name": null,
        "show": true,
        "values": []
      },
      "yaxes": [
        {
          "format": "short",
          "label": null,
          "logBase": 1,
          "max": null,
          "min": null,
          "show": true
        },
        {
          "format": "short",
          "label": null,
          "logBase": 1,
          "max": null,
          "min": null,
          "show": true
        }
      ],
      "yaxis": {
        "align": false,
        "alignLevel": null
      }
    },
    {
      "aliasColors": {},
      "bars": false,
      "dashLength": 10,
      "dashes": false,
      "datasource": "Prometheus",
      "fieldConfig": {
        "defaults": {
          "custom": {}
        },
        "overrides": []
      },
      "fill": 1,
      "fillGradient": 0,
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 0,
        "y": 8
      },
      "hiddenSeries": false,
      "id": 6,
      "legend": {
        "avg": false,
        "current": false,
        "max": false,
        "min": false,
        "show": true,
        "total": false,
        "values": false
      },
      "lines": true,
      "linewidth": 1,
      "nullPointMode": "null",
      "options": {
        "alertThreshold": true
      },
      "percentage": false,
      "pluginVersion": "7.3.7",
      "pointradius": 2,
      "points": false,
      "renderer": "flot",
      "seriesOverrides": [],
      "spaceLength": 10,
      "stack": false,
      "steppedLine": false,
      "targets": [
        {
          "expr": "rate(rate_limit_exceeded_total[5m])",
          "interval": "",
          "legendFormat": "{{instance}}",
          "refId": "A"
        }
      ],
      "thresholds": [],
      "timeFrom": null,
      "timeRegions": [],
      "timeShift": null,
      "title": "Rate Limit Exceeded",
      "tooltip": {
        "shared": true,
        "sort": 0,
        "value_type": "individual"
      },
      "type": "graph",
      "xaxis": {
        "buckets": null,
        "mode": "time",
        "name": null,
        "show": true,
        "values": []
      },
      "yaxes": [
        {
          "format": "short",
          "label": null,
          "logBase": 1,
          "max": null,
          "min": null,
          "show": true
        },
        {
          "format": "short",
          "label": null,
          "logBase": 1,
          "max": null,
          "min": null,
          "show": true
        }
      ],
      "yaxis": {
        "align": false,
        "alignLevel": null
      }
    },
    {
      "aliasColors": {},
      "bars": false,
      "dashLength": 10,
      "dashes": false,
      "datasource": "Prometheus",
      "fieldConfig": {
        "defaults": {
          "custom": {}
        },
        "overrides": []
      },
      "fill": 1,
      "fillGradient": 0,
      "gridPos": {
        "h": 8,
        "w": 12,
        "x": 12,
        "y": 8
      },
      "hiddenSeries": false,
      "id": 8,
      "legend": {
        "avg": false,
        "current": false,
        "max": false,
        "min": false,
        "show": true,
        "total": false,
        "values": false
      },
      "lines": true,
      "linewidth": 1,
      "nullPointMode": "null",
      "options": {
        "alertThreshold": true
      },
      "percentage": false,
      "pluginVersion": "7.3.7",
      "pointradius": 2,
      "points": false,
      "renderer": "flot",
      "seriesOverrides": [],
      "spaceLength": 10,
      "stack": false,
      "steppedLine": false,
      "targets": [
        {
          "expr": "(container_memory_usage_bytes{name!=\"\"} / container_spec_memory_limit_bytes{name!=\"\"} * 100)",
          "interval": "",
          "legendFormat": "{{name}}",
          "refId": "A"
        }
      ],
      "thresholds": [],
      "timeFrom": null,
      "timeRegions": [],
      "timeShift": null,
      "title": "Container Memory Usage (%)",
      "tooltip": {
        "shared": true,
        "sort": 0,
        "value_type": "individual"
      },
      "type": "graph",
      "xaxis": {
        "buckets": null,
        "mode": "time",
        "name": null,
        "show": true,
        "values": []
      },
      "yaxes": [
        {
          "format": "percent",
          "label": null,
          "logBase": 1,
          "max": null,
          "min": null,
          "show": true
        },
        {
          "format": "short",
          "label": null,
          "logBase": 1,
          "max": null,
          "min": null,
          "show": true
        }
      ],
      "yaxis": {
        "align": false,
        "alignLevel": null
      }
    }
  ],
  "refresh": "5s",
  "schemaVersion": 26,
  "style": "dark",
  "tags": [],
  "templating": {
    "list": []
  },
  "time": {
    "from": "now-1h",
    "to": "now"
  },
  "timepicker": {},
  "timezone": "",
  "title": "Security Dashboard",
  "uid": "security",
  "version": 1
}
EOF

    log_success "Grafana configuration created"
}

# Function to create Docker Compose configuration
create_docker_compose_config() {
    log_info "Creating Docker Compose configuration"
    
    cat > """"$CONFIG_DIR"""/security-monitoring-config.yml" << EOF
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:v2.47.0
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./prometheus/alert_rules.yml:/etc/prometheus/alert_rules.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--storage.tsdb.retention.time=15d'
      - '--web.enable-lifecycle'
    ports:
      - "9090:9090"
    restart: unless-stopped
    networks:
      - monitoring-network

  alertmanager:
    image: prom/alertmanager:v0.26.0
    volumes:
      - ./alertmanager/config.yml:/etc/alertmanager/config.yml
      - alertmanager_data:/alertmanager
    command:
      - '--config.file=/etc/alertmanager/config.yml'
      - '--storage.path=/alertmanager'
    ports:
      - "9093:9093"
    restart: unless-stopped
    networks:
      - monitoring-network

  grafana:
    image: grafana/grafana:10.2.0
    volumes:
      - ./grafana/provisioning:/etc/grafana/provisioning
      - ./grafana/dashboards:/etc/grafana/dashboards
      - grafana_data:/var/lib/grafana
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
      - GF_USERS_ALLOW_SIGN_UP=false
      - GF_INSTALL_PLUGINS=grafana-piechart-panel
    ports:
      - "3000:3000"
    restart: unless-stopped
    networks:
      - monitoring-network

  node-exporter:
    image: prom/node-exporter:v1.7.0
    volumes:
      - /proc:/host/proc:ro
      - /sys:/host/sys:ro
      - /:/rootfs:ro
    command:
      - '--path.procfs=/host/proc'
      - '--path.sysfs=/host/sys'
      - '--collector.filesystem.ignored-mount-points=^/(sys|proc|dev|host|etc)($$|/)'
    ports:
      - "9100:9100"
    restart: unless-stopped
    networks:
      - monitoring-network

  cadvisor:
    image: gcr.io/cadvisor/cadvisor:v0.47.2
    volumes:
      - /:/rootfs:ro
      - /var/run:/var/run:ro
      - /sys:/sys:ro
      - /var/lib/docker/:/var/lib/docker:ro
      - /dev/disk/:/dev/disk:ro
    ports:
      - "8080:8080"
    restart: unless-stopped
    networks:
      - monitoring-network

networks:
  monitoring-network:
    driver: bridge

volumes:
  prometheus_data:
  alertmanager_data:
  grafana_data:
EOF

    log_success "Docker Compose configuration created"
}

# Execute action
case """$ACTION""" in
    setup)
        log_info "Setting up security monitoring"
        
        # Check prerequisites
        check_docker
        check_docker_compose
        
        # Create configurations
        create_prometheus_config
        create_alertmanager_config
        create_grafana_config
        create_docker_compose_config
        
        log_success "Security monitoring setup complete"
        log_info "Run '$0 start' to start security monitoring"
        ;;
    
    start)
        log_info "Starting security monitoring"
        
        # Check prerequisites
        check_docker
        check_docker_compose
        
        # Start monitoring stack
        cd """"$CONFIG_DIR"""" && docker-compose -f security-monitoring-config.yml up -d
        
        log_success "Security monitoring started"
        log_info "Prometheus: http://localhost:9090"
        log_info "Alertmanager: http://localhost:9093"
        log_info "Grafana: http://localhost:3000 (admin/admin)"
        ;;
    
    stop)
        log_info "Stopping security monitoring"
        
        # Check prerequisites
        check_docker
        check_docker_compose
        
        # Stop monitoring stack
        cd """"$CONFIG_DIR"""" && docker-compose -f security-monitoring-config.yml down
        
        log_success "Security monitoring stopped"
        ;;
    
    status)
        log_info "Checking security monitoring status"
        
        # Check prerequisites
        check_docker
        check_docker_compose
        
        # Check monitoring stack status
        cd """"$CONFIG_DIR"""" && docker-compose -f security-monitoring-config.yml ps
        
        log_success "Security monitoring status checked"
        ;;
    
    report)
        log_info "Generating security monitoring report"
        
        # Check prerequisites
        check_docker
        check_docker_compose
        
        # Generate report
        REPORT_FILE="security-monitoring-report-$(date +%Y%m%d-%H%M%S).md"
        
        cat > """"$REPORT_FILE"""" << EOF
# Security Monitoring Report

- **Date:** $(date +"%Y-%m-%d %H:%M:%S")
- **Generated By:** $(whoami)

## Alerts

EOF
        
        # Get alerts from Prometheus
        if curl -s http://localhost:9090/api/v1/alerts > /dev/null; then
            curl -s http://localhost:9090/api/v1/alerts | jq -r '.data.alerts[] | "- **" + .labels.alertname + "**: " + .annotations.description' >> """"$REPORT_FILE"""" || echo "- No alerts found" >> """"$REPORT_FILE""""
        else
            echo "- Failed to connect to Prometheus" >> """"$REPORT_FILE""""
        fi
        
        cat >> """"$REPORT_FILE"""" << EOF

## System Health

EOF
        
        # Get system health metrics
        if curl -s http://localhost:9090/api/v1/query?query=up > /dev/null; then
            echo "### Services Status" >> """"$REPORT_FILE""""
            echo "" >> """"$REPORT_FILE""""
            echo "| Service | Status |" >> """"$REPORT_FILE""""
            echo "|---------|--------|" >> """"$REPORT_FILE""""
            curl -s http://localhost:9090/api/v1/query?query=up | jq -r '.data.result[] | "| " + .metric.job + " | " + if .value[1] == "1" then "✅ Up" else "❌ Down" end + " |"' >> """"$REPORT_FILE"""" || echo "| Failed to get service status |" >> """"$REPORT_FILE""""
        else
            echo "Failed to connect to Prometheus" >> """"$REPORT_FILE""""
        fi
        
        cat >> """"$REPORT_FILE"""" << EOF

## Security Metrics

EOF
        
        # Get security metrics
        if curl -s http://localhost:9090/api/v1/query?query=rate\(http_server_requests_seconds_count{status=\"5xx\"}[1h]\) > /dev/null; then
            echo "### HTTP 5xx Error Rate (1h)" >> """"$REPORT_FILE""""
            echo "" >> """"$REPORT_FILE""""
            echo "| Instance | Rate |" >> """"$REPORT_FILE""""
            echo "|----------|------|" >> """"$REPORT_FILE""""
            curl -s http://localhost:9090/api/v1/query?query=rate\(http_server_requests_seconds_count{status=\"5xx\"}[1h]\) | jq -r '.data.result[] | "| " + .metric.instance + " | " + .value[1] + " |"' >> """"$REPORT_FILE"""" || echo "| No data available |" >> """"$REPORT_FILE""""
        fi
        
        log_success "Security monitoring report generated: """$REPORT_FILE""""
        ;;
    
    *)
        log_error "Unknown action: """$ACTION""""
        show_help
        exit 1
        ;;
esac