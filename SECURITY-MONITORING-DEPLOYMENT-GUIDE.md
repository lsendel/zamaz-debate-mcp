# Security Monitoring Deployment Guide

**Project**: zamaz-debate-mcp  
**Component**: Security Monitoring Stack  
**Version**: 1.0  
**Last Updated**: 2025-07-16

---

## 🎯 Overview

This guide provides step-by-step instructions for deploying the comprehensive security monitoring infrastructure for the zamaz-debate-mcp system. The monitoring stack provides real-time visibility into security events, automated alerting, and incident response capabilities.

## 📊 Monitoring Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Security Dashboards                       │
│                  (Grafana + Kibana)                         │
└─────────────────────┬───────────────┬──────────────────────┘
                      │               │
┌─────────────────────▼───────────────▼──────────────────────┐
│                    Metrics Store                            │
│              (Prometheus + Elasticsearch)                   │
└─────────────────────┬───────────────┬──────────────────────┘
                      │               │
┌─────────────────────▼───────────────▼──────────────────────┐
│                  Data Collectors                            │
│        (Prometheus Exporters + Logstash + Beats)          │
└─────────────────────┬───────────────┬──────────────────────┘
                      │               │
┌─────────────────────▼───────────────▼──────────────────────┐
│                Application Services                         │
│     (API Gateway, Auth Service, Business Services)         │
└────────────────────────────────────────────────────────────┘
```

---

## 🚀 Pre-Deployment Checklist

### Infrastructure Requirements
```bash
□ Kubernetes cluster (or Docker Swarm) available
□ Minimum 16GB RAM for monitoring stack
□ 500GB storage for logs/metrics (30-day retention)
□ Network connectivity between all services
□ DNS configured for monitoring domains
□ SSL certificates for monitoring endpoints
```

### Access Requirements
```bash
□ Kubernetes admin access (kubectl configured)
□ Docker registry access
□ Cloud provider console access
□ Domain management access
□ Certificate management access
```

### Security Requirements
```bash
□ Monitoring namespace created
□ RBAC policies defined
□ Network policies configured
□ Secrets management ready
□ Backup strategy defined
```

---

## 📦 Component Deployment

### Phase 1: Core Infrastructure

#### 1.1 Create Monitoring Namespace
```bash
# Create dedicated namespace
kubectl create namespace security-monitoring

# Apply security policies
kubectl apply -f - <<EOF
apiVersion: v1
kind: ResourceQuota
metadata:
  name: monitoring-quota
  namespace: security-monitoring
spec:
  hard:
    requests.cpu: "16"
    requests.memory: 32Gi
    requests.storage: 500Gi
    persistentvolumeclaims: "10"
EOF

# Create network policy
kubectl apply -f - <<EOF
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: monitoring-network-policy
  namespace: security-monitoring
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: default
    - namespaceSelector:
        matchLabels:
          name: security-monitoring
  egress:
  - to:
    - namespaceSelector: {}
EOF
```

#### 1.2 Deploy Prometheus
```bash
# Add Prometheus Helm repository
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# Create Prometheus values file
cat > prometheus-values.yaml <<EOF
alertmanager:
  enabled: true
  config:
    global:
      resolve_timeout: 5m
      slack_api_url: '$SLACK_WEBHOOK_URL'
    route:
      group_by: ['alertname', 'cluster', 'service']
      group_wait: 10s
      group_interval: 10s
      repeat_interval: 12h
      receiver: 'security-team'
      routes:
      - match:
          severity: critical
        receiver: security-critical
    receivers:
    - name: 'security-team'
      slack_configs:
      - channel: '#security-alerts'
        title: 'Security Alert'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'
    - name: 'security-critical'
      slack_configs:
      - channel: '#security-critical'
        title: '🚨 CRITICAL Security Alert'
      pagerduty_configs:
      - service_key: '$PAGERDUTY_KEY'

prometheus:
  prometheusSpec:
    retention: 30d
    retentionSize: "450GB"
    resources:
      requests:
        cpu: 2
        memory: 8Gi
    securityContext:
      runAsNonRoot: true
      runAsUser: 65534
      fsGroup: 65534
    storageSpec:
      volumeClaimTemplate:
        spec:
          accessModes: ["ReadWriteOnce"]
          resources:
            requests:
              storage: 500Gi

serverFiles:
  prometheus.yml:
    global:
      scrape_interval: 15s
      evaluation_interval: 15s
      external_labels:
        cluster: 'production'
        environment: 'prod'
    
    rule_files:
      - /etc/prometheus/rules/*.yml
    
    scrape_configs:
    - job_name: 'kubernetes-pods'
      kubernetes_sd_configs:
      - role: pod
      relabel_configs:
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
        action: keep
        regex: true
      - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
        action: replace
        target_label: __metrics_path__
        regex: (.+)
      
    - job_name: 'security-metrics'
      static_configs:
      - targets:
        - 'mcp-gateway:9090'
        - 'mcp-organization:9090'
        - 'mcp-security:9090'
EOF

# Deploy Prometheus
helm install prometheus prometheus-community/kube-prometheus-stack \
  --namespace security-monitoring \
  --values prometheus-values.yaml
```

#### 1.3 Deploy Elasticsearch
```bash
# Create Elasticsearch values file
cat > elasticsearch-values.yaml <<EOF
clusterName: "security-logs"
nodeGroup: "master"
replicas: 3
minimumMasterNodes: 2

resources:
  requests:
    cpu: "1"
    memory: "2Gi"
  limits:
    cpu: "2"
    memory: "4Gi"

volumeClaimTemplate:
  accessModes: [ "ReadWriteOnce" ]
  resources:
    requests:
      storage: 100Gi

esConfig:
  elasticsearch.yml: |
    cluster.name: security-logs
    network.host: 0.0.0.0
    xpack.security.enabled: true
    xpack.security.transport.ssl.enabled: true
    xpack.security.transport.ssl.verification_mode: certificate
    xpack.security.transport.ssl.keystore.path: /usr/share/elasticsearch/config/certs/elastic-certificates.p12
    xpack.security.transport.ssl.truststore.path: /usr/share/elasticsearch/config/certs/elastic-certificates.p12

extraEnvs:
  - name: ELASTIC_PASSWORD
    valueFrom:
      secretKeyRef:
        name: elastic-credentials
        key: password
  - name: ELASTIC_USERNAME
    valueFrom:
      secretKeyRef:
        name: elastic-credentials
        key: username
EOF

# Create Elasticsearch credentials
kubectl create secret generic elastic-credentials \
  --namespace security-monitoring \
  --from-literal=username=elastic \
  --from-literal=password=$(openssl rand -base64 32)

# Deploy Elasticsearch
helm install elasticsearch elastic/elasticsearch \
  --namespace security-monitoring \
  --values elasticsearch-values.yaml
```

### Phase 2: Data Collection

#### 2.1 Deploy Logstash
```bash
# Create Logstash configuration
cat > logstash-config.yaml <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: logstash-config
  namespace: security-monitoring
data:
  logstash.yml: |
    http.host: "0.0.0.0"
    xpack.monitoring.elasticsearch.hosts: ["http://elasticsearch:9200"]
    
  pipeline.conf: |
    input {
      beats {
        port => 5044
      }
      tcp {
        port => 5000
        codec => json
      }
    }
    
    filter {
      if [type] == "security" {
        grok {
          match => { 
            "message" => "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} %{DATA:service} %{GREEDYDATA:message}" 
          }
        }
        
        if [security_event] {
          mutate {
            add_field => { "[@metadata][security]" => "true" }
          }
        }
        
        # Extract security-specific fields
        if [message] =~ /authentication|authorization|security/ {
          mutate {
            add_tag => [ "security-relevant" ]
          }
        }
      }
      
      # GeoIP enrichment for IP addresses
      if [client_ip] {
        geoip {
          source => "client_ip"
          target => "geoip"
        }
      }
    }
    
    output {
      elasticsearch {
        hosts => ["elasticsearch:9200"]
        user => "${ELASTIC_USERNAME}"
        password => "${ELASTIC_PASSWORD}"
        index => "security-%{+YYYY.MM.dd}"
        template_name => "security"
        template => "/usr/share/logstash/templates/security-template.json"
        template_overwrite => true
      }
      
      # Send critical events to monitoring
      if [level] == "ERROR" or [level] == "CRITICAL" {
        http {
          url => "http://prometheus-pushgateway:9091/metrics/job/security_events"
          http_method => "post"
          format => "message"
          message => 'security_event_total{level="%{level}",service="%{service}"} 1'
        }
      }
    }
EOF

# Deploy Logstash
kubectl apply -f - <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: logstash
  namespace: security-monitoring
spec:
  replicas: 2
  selector:
    matchLabels:
      app: logstash
  template:
    metadata:
      labels:
        app: logstash
    spec:
      containers:
      - name: logstash
        image: docker.elastic.co/logstash/logstash:8.12.0
        ports:
        - containerPort: 5044
        - containerPort: 5000
        volumeMounts:
        - name: config
          mountPath: /usr/share/logstash/pipeline/
        - name: logstash-yml
          mountPath: /usr/share/logstash/config/logstash.yml
          subPath: logstash.yml
        env:
        - name: ELASTIC_USERNAME
          valueFrom:
            secretKeyRef:
              name: elastic-credentials
              key: username
        - name: ELASTIC_PASSWORD
          valueFrom:
            secretKeyRef:
              name: elastic-credentials
              key: password
        resources:
          requests:
            cpu: "1"
            memory: "2Gi"
          limits:
            cpu: "2"
            memory: "4Gi"
      volumes:
      - name: config
        configMap:
          name: logstash-config
          items:
          - key: pipeline.conf
            path: pipeline.conf
      - name: logstash-yml
        configMap:
          name: logstash-config
          items:
          - key: logstash.yml
            path: logstash.yml
EOF
```

#### 2.2 Deploy Filebeat on Application Nodes
```bash
# Create Filebeat DaemonSet
cat > filebeat-daemonset.yaml <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: filebeat-config
  namespace: security-monitoring
data:
  filebeat.yml: |
    filebeat.inputs:
    - type: container
      paths:
        - /var/log/containers/*mcp*.log
      processors:
        - add_kubernetes_metadata:
            host: \${NODE_NAME}
            matchers:
            - logs_path:
                logs_path: "/var/log/containers/"
      multiline.pattern: '^\d{4}-\d{2}-\d{2}'
      multiline.negate: true
      multiline.match: after
      
    - type: log
      enabled: true
      paths:
        - /var/log/auth.log
        - /var/log/secure
      fields:
        log_type: authentication
      fields_under_root: true
    
    processors:
      - add_host_metadata:
          when.not.contains.tags: forwarded
      - add_docker_metadata: ~
      - add_kubernetes_metadata: ~
      
    output.logstash:
      hosts: ["logstash:5044"]
      
    logging.level: info
    logging.to_files: true
    logging.files:
      path: /var/log/filebeat
      name: filebeat
      keepfiles: 7
      permissions: 0640
---
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: filebeat
  namespace: security-monitoring
spec:
  selector:
    matchLabels:
      app: filebeat
  template:
    metadata:
      labels:
        app: filebeat
    spec:
      serviceAccountName: filebeat
      terminationGracePeriodSeconds: 30
      hostNetwork: true
      dnsPolicy: ClusterFirstWithHostNet
      containers:
      - name: filebeat
        image: docker.elastic.co/beats/filebeat:8.12.0
        args: [
          "-c", "/etc/filebeat.yml",
          "-e",
        ]
        env:
        - name: NODE_NAME
          valueFrom:
            fieldRef:
              fieldPath: spec.nodeName
        securityContext:
          runAsUser: 0
        resources:
          limits:
            memory: 200Mi
          requests:
            cpu: 100m
            memory: 100Mi
        volumeMounts:
        - name: config
          mountPath: /etc/filebeat.yml
          readOnly: true
          subPath: filebeat.yml
        - name: data
          mountPath: /usr/share/filebeat/data
        - name: varlibdockercontainers
          mountPath: /var/lib/docker/containers
          readOnly: true
        - name: varlog
          mountPath: /var/log
          readOnly: true
      volumes:
      - name: config
        configMap:
          defaultMode: 0640
          name: filebeat-config
      - name: varlibdockercontainers
        hostPath:
          path: /var/lib/docker/containers
      - name: varlog
        hostPath:
          path: /var/log
      - name: data
        hostPath:
          path: /var/lib/filebeat-data
          type: DirectoryOrCreate
EOF

# Create RBAC for Filebeat
kubectl apply -f - <<EOF
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: filebeat
rules:
- apiGroups: [""]
  resources:
  - namespaces
  - pods
  - nodes
  verbs: ["get", "list", "watch"]
---
apiVersion: v1
kind: ServiceAccount
metadata:
  name: filebeat
  namespace: security-monitoring
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: filebeat
subjects:
- kind: ServiceAccount
  name: filebeat
  namespace: security-monitoring
roleRef:
  kind: ClusterRole
  name: filebeat
  apiGroup: rbac.authorization.k8s.io
EOF

# Deploy Filebeat
kubectl apply -f filebeat-daemonset.yaml
```

### Phase 3: Visualization & Alerting

#### 3.1 Deploy Grafana
```bash
# Create Grafana values
cat > grafana-values.yaml <<EOF
adminUser: admin
adminPassword: $(openssl rand -base64 32)

datasources:
  datasources.yaml:
    apiVersion: 1
    datasources:
    - name: Prometheus
      type: prometheus
      url: http://prometheus-server:80
      access: proxy
      isDefault: true
    - name: Elasticsearch
      type: elasticsearch
      url: http://elasticsearch:9200
      access: proxy
      database: "security-*"
      basicAuth: true
      basicAuthUser: elastic
      secureJsonData:
        basicAuthPassword: "\${ELASTIC_PASSWORD}"

dashboardProviders:
  dashboardproviders.yaml:
    apiVersion: 1
    providers:
    - name: 'security'
      orgId: 1
      folder: 'Security'
      type: file
      disableDeletion: false
      editable: true
      options:
        path: /var/lib/grafana/dashboards/security

dashboards:
  security:
    security-overview:
      url: https://raw.githubusercontent.com/zamaz/security-dashboards/main/security-overview.json
    authentication-metrics:
      url: https://raw.githubusercontent.com/zamaz/security-dashboards/main/authentication.json
    threat-detection:
      url: https://raw.githubusercontent.com/zamaz/security-dashboards/main/threat-detection.json

persistence:
  enabled: true
  size: 10Gi

service:
  type: LoadBalancer
  annotations:
    service.beta.kubernetes.io/aws-load-balancer-ssl-cert: arn:aws:acm:region:account:certificate/cert-id
    service.beta.kubernetes.io/aws-load-balancer-backend-protocol: http
    service.beta.kubernetes.io/aws-load-balancer-ssl-ports: "443"
EOF

# Deploy Grafana
helm install grafana grafana/grafana \
  --namespace security-monitoring \
  --values grafana-values.yaml

# Get admin password
kubectl get secret --namespace security-monitoring grafana -o jsonpath="{.data.admin-password}" | base64 --decode
```

#### 3.2 Configure Security Dashboards
```bash
# Import custom security dashboards
kubectl apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: security-dashboards
  namespace: security-monitoring
data:
  security-overview.json: |
    {
      "dashboard": {
        "title": "Security Overview",
        "panels": [
          {
            "title": "Authentication Success Rate",
            "targets": [{
              "expr": "sum(rate(security_authentication_success_total[5m])) / sum(rate(security_authentication_total[5m])) * 100"
            }]
          },
          {
            "title": "Active Security Incidents",
            "targets": [{
              "expr": "security_incidents_active"
            }]
          },
          {
            "title": "Threat Detection Events",
            "targets": [{
              "expr": "sum(rate(security_threat_detected_total[5m])) by (threat_type)"
            }]
          },
          {
            "title": "API Security Violations",
            "targets": [{
              "expr": "sum(rate(security_api_violations_total[5m])) by (violation_type)"
            }]
          }
        ]
      }
    }
EOF
```

### Phase 4: Security-Specific Monitoring

#### 4.1 Deploy Security Event Processor
```bash
# Create security event processor
cat > security-processor.yaml <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: security-event-processor
  namespace: security-monitoring
spec:
  replicas: 2
  selector:
    matchLabels:
      app: security-processor
  template:
    metadata:
      labels:
        app: security-processor
    spec:
      containers:
      - name: processor
        image: zamaz/security-processor:latest
        env:
        - name: ELASTICSEARCH_URL
          value: "http://elasticsearch:9200"
        - name: PROMETHEUS_PUSHGATEWAY
          value: "http://prometheus-pushgateway:9091"
        - name: REDIS_URL
          value: "redis://redis:6379"
        - name: CORRELATION_WINDOW
          value: "15m"
        - name: THREAT_INTEL_UPDATE_INTERVAL
          value: "1h"
        ports:
        - containerPort: 8080
        resources:
          requests:
            cpu: "1"
            memory: "2Gi"
          limits:
            cpu: "2"
            memory: "4Gi"
---
apiVersion: v1
kind: Service
metadata:
  name: security-processor
  namespace: security-monitoring
spec:
  selector:
    app: security-processor
  ports:
  - port: 8080
    targetPort: 8080
EOF

kubectl apply -f security-processor.yaml
```

#### 4.2 Configure Security Alerts
```bash
# Create Prometheus alert rules
cat > security-alerts.yaml <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: security-alerts
  namespace: security-monitoring
data:
  security.rules: |
    groups:
    - name: security.rules
      interval: 30s
      rules:
      # Authentication Alerts
      - alert: HighAuthenticationFailureRate
        expr: |
          sum(rate(security_authentication_failure_total[5m])) 
          / sum(rate(security_authentication_total[5m])) > 0.1
        for: 5m
        labels:
          severity: warning
          team: security
        annotations:
          summary: "High authentication failure rate detected"
          description: "Authentication failure rate is {{ \$value | humanizePercentage }} over the last 5 minutes"
          
      - alert: BruteForceAttack
        expr: |
          sum(security_authentication_failures_per_user) by (user_id) > 5
        for: 1m
        labels:
          severity: critical
          team: security
        annotations:
          summary: "Possible brute force attack on user {{ \$labels.user_id }}"
          description: "User {{ \$labels.user_id }} has {{ \$value }} failed authentication attempts"
      
      # Threat Detection Alerts
      - alert: SuspiciousActivity
        expr: |
          sum(rate(security_suspicious_activity_total[5m])) by (activity_type) > 0
        for: 1m
        labels:
          severity: high
          team: security
        annotations:
          summary: "Suspicious activity detected: {{ \$labels.activity_type }}"
          description: "{{ \$value }} suspicious activities of type {{ \$labels.activity_type }} detected"
      
      - alert: PossibleDataBreach
        expr: |
          sum(rate(security_data_access_bytes[5m])) by (user_id) > 1073741824
        for: 5m
        labels:
          severity: critical
          team: security
        annotations:
          summary: "Possible data exfiltration by user {{ \$labels.user_id }}"
          description: "User {{ \$labels.user_id }} accessed {{ \$value | humanize }}B of data in 5 minutes"
      
      # System Security Alerts
      - alert: SecurityServiceDown
        expr: |
          up{job=~"mcp-security|mcp-gateway"} == 0
        for: 1m
        labels:
          severity: critical
          team: security
        annotations:
          summary: "Security service {{ \$labels.job }} is down"
          description: "Critical security service {{ \$labels.job }} has been down for more than 1 minute"
      
      - alert: HighSecurityAPILatency
        expr: |
          histogram_quantile(0.95, 
            sum(rate(http_request_duration_seconds_bucket{job="mcp-security"}[5m])) 
            by (le, endpoint)
          ) > 1
        for: 5m
        labels:
          severity: warning
          team: security
        annotations:
          summary: "High latency on security endpoint {{ \$labels.endpoint }}"
          description: "95th percentile latency is {{ \$value }}s for {{ \$labels.endpoint }}"
      
      # Compliance Alerts
      - alert: AuditLogFailure
        expr: |
          sum(rate(security_audit_log_failures_total[5m])) > 0
        for: 1m
        labels:
          severity: critical
          team: security
          compliance: true
        annotations:
          summary: "Audit log write failures detected"
          description: "{{ \$value }} audit log write failures in the last 5 minutes"
      
      - alert: SessionAnomalies
        expr: |
          sum(security_concurrent_sessions_per_user) by (user_id) > 5
        for: 5m
        labels:
          severity: warning
          team: security
        annotations:
          summary: "Unusual session count for user {{ \$labels.user_id }}"
          description: "User {{ \$labels.user_id }} has {{ \$value }} concurrent sessions"
EOF

# Apply alert rules
kubectl create configmap prometheus-rules \
  --from-file=security.rules=security-alerts.yaml \
  --namespace security-monitoring
```

---

## 🔧 Post-Deployment Configuration

### 1. Configure Data Retention
```bash
# Set Elasticsearch retention policy
curl -X PUT "http://elasticsearch:9200/_ilm/policy/security-logs" \
  -H "Content-Type: application/json" \
  -u elastic:$ELASTIC_PASSWORD \
  -d '{
    "policy": {
      "phases": {
        "hot": {
          "actions": {
            "rollover": {
              "max_size": "50GB",
              "max_age": "7d"
            }
          }
        },
        "warm": {
          "min_age": "7d",
          "actions": {
            "shrink": {
              "number_of_shards": 1
            },
            "forcemerge": {
              "max_num_segments": 1
            }
          }
        },
        "delete": {
          "min_age": "30d",
          "actions": {
            "delete": {}
          }
        }
      }
    }
  }'
```

### 2. Set Up Automated Reporting
```bash
# Create reporting CronJob
kubectl apply -f - <<EOF
apiVersion: batch/v1
kind: CronJob
metadata:
  name: security-reports
  namespace: security-monitoring
spec:
  schedule: "0 8 * * 1"  # Weekly on Monday 8AM
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: reporter
            image: zamaz/security-reporter:latest
            env:
            - name: GRAFANA_URL
              value: "http://grafana:3000"
            - name: ELASTICSEARCH_URL
              value: "http://elasticsearch:9200"
            - name: REPORT_RECIPIENTS
              value: "security-team@yourdomain.com"
            - name: SMTP_SERVER
              value: "smtp.yourdomain.com"
          restartPolicy: OnFailure
EOF
```

### 3. Configure Backup
```bash
# Create backup CronJob for Elasticsearch
kubectl apply -f - <<EOF
apiVersion: batch/v1
kind: CronJob
metadata:
  name: elasticsearch-backup
  namespace: security-monitoring
spec:
  schedule: "0 2 * * *"  # Daily at 2AM
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: backup
            image: elastic/elasticsearch:8.12.0
            command:
            - /bin/bash
            - -c
            - |
              curl -X PUT "http://elasticsearch:9200/_snapshot/security_backup" \
                -H "Content-Type: application/json" \
                -u elastic:\$ELASTIC_PASSWORD \
                -d '{
                  "type": "s3",
                  "settings": {
                    "bucket": "security-backups",
                    "region": "us-east-1"
                  }
                }'
              
              DATE=\$(date +%Y%m%d)
              curl -X PUT "http://elasticsearch:9200/_snapshot/security_backup/snapshot_\${DATE}?wait_for_completion=true" \
                -u elastic:\$ELASTIC_PASSWORD
            env:
            - name: ELASTIC_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: elastic-credentials
                  key: password
          restartPolicy: OnFailure
EOF
```

---

## 🔍 Verification Steps

### 1. Check Component Health
```bash
# Verify all pods are running
kubectl get pods -n security-monitoring

# Check Prometheus targets
kubectl port-forward -n security-monitoring svc/prometheus-server 9090:80
# Visit http://localhost:9090/targets

# Check Elasticsearch health
kubectl port-forward -n security-monitoring svc/elasticsearch 9200:9200
curl -u elastic:$ELASTIC_PASSWORD http://localhost:9200/_cluster/health

# Access Grafana
kubectl port-forward -n security-monitoring svc/grafana 3000:80
# Visit http://localhost:3000
```

### 2. Test Data Flow
```bash
# Generate test security event
curl -X POST http://your-api-gateway/api/v1/test/security-event \
  -H "Content-Type: application/json" \
  -d '{
    "event_type": "test_authentication_failure",
    "user_id": "test-user",
    "ip": "192.168.1.100"
  }'

# Check if event appears in Elasticsearch
curl -u elastic:$ELASTIC_PASSWORD \
  "http://localhost:9200/security-*/_search?q=event_type:test_authentication_failure"

# Verify metrics in Prometheus
curl "http://localhost:9090/api/v1/query?query=security_authentication_failure_total"
```

### 3. Test Alerting
```bash
# Trigger test alert
curl -X POST http://prometheus-alertmanager:9093/api/v1/alerts \
  -H "Content-Type: application/json" \
  -d '[{
    "labels": {
      "alertname": "TestSecurityAlert",
      "severity": "warning",
      "team": "security"
    },
    "annotations": {
      "summary": "Test security alert",
      "description": "This is a test alert to verify alerting pipeline"
    }
  }]'

# Check Slack/PagerDuty for alert
```

---

## 📊 Monitoring Dashboards

### Available Dashboards

1. **Security Overview**
   - Authentication metrics
   - Active threats
   - System health
   - Compliance status

2. **Threat Detection**
   - Real-time threat map
   - Attack patterns
   - Blocked IPs
   - Suspicious activities

3. **Authentication Analytics**
   - Success/failure rates
   - User behavior patterns
   - Geographic distribution
   - Device analytics

4. **Incident Response**
   - Active incidents
   - Response times
   - Resolution metrics
   - Team performance

5. **Compliance Dashboard**
   - Audit coverage
   - Policy violations
   - Data access patterns
   - Regulatory metrics

---

## 🚨 Troubleshooting

### Common Issues

#### Pods Not Starting
```bash
# Check pod events
kubectl describe pod <pod-name> -n security-monitoring

# Check resource availability
kubectl top nodes
kubectl describe node <node-name>

# Check persistent volume claims
kubectl get pvc -n security-monitoring
```

#### No Data in Dashboards
```bash
# Check data flow
kubectl logs -n security-monitoring deployment/logstash
kubectl logs -n security-monitoring daemonset/filebeat

# Verify network connectivity
kubectl exec -n security-monitoring deployment/logstash -- nc -zv elasticsearch 9200
```

#### High Resource Usage
```bash
# Adjust resource limits
kubectl edit deployment <deployment-name> -n security-monitoring

# Scale down replicas if needed
kubectl scale deployment <deployment-name> --replicas=1 -n security-monitoring
```

---

## 🔐 Security Considerations

### Access Control
```bash
# Create read-only user for dashboards
kubectl create serviceaccount dashboard-viewer -n security-monitoring
kubectl create clusterrolebinding dashboard-viewer \
  --clusterrole=view \
  --serviceaccount=security-monitoring:dashboard-viewer
```

### Network Security
```bash
# Restrict external access
kubectl apply -f - <<EOF
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: deny-external-access
  namespace: security-monitoring
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: security-monitoring
    - podSelector: {}
EOF
```

### Secrets Management
```bash
# Rotate credentials regularly
kubectl create secret generic elastic-credentials \
  --from-literal=password=$(openssl rand -base64 32) \
  --dry-run=client -o yaml | kubectl apply -f -

# Use external secret manager
kubectl apply -f external-secrets-operator.yaml
```

---

## 📚 Maintenance Tasks

### Daily
- Check dashboard health metrics
- Review security alerts
- Verify backup completion
- Monitor resource usage

### Weekly
- Review security reports
- Update threat intelligence
- Check data retention
- Test alert channels

### Monthly
- Rotate credentials
- Update security rules
- Review and optimize queries
- Capacity planning

---

**Deployment Status**: Ready to deploy  
**Estimated Time**: 2-3 hours  
**Support**: security-monitoring@yourdomain.com  

For questions or issues, contact the Security Operations team.