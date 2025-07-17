# Kiro GitHub Integration Deployment Guide

## Overview

This guide covers the deployment process for the Kiro GitHub Integration system, including staging and production environments, monitoring setup, and troubleshooting.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Architecture Overview](#architecture-overview)
3. [Deployment Environments](#deployment-environments)
4. [Deployment Process](#deployment-process)
5. [Monitoring Setup](#monitoring-setup)
6. [Rollback Procedures](#rollback-procedures)
7. [Troubleshooting](#troubleshooting)

## Prerequisites

### Required Tools
- Docker 20.10+
- Kubernetes 1.24+
- kubectl CLI
- Helm 3.0+ (for monitoring stack)
- GitHub CLI (gh)

### Access Requirements
- GitHub Container Registry (ghcr.io) access
- Kubernetes cluster access
- Monitoring endpoints access

### Environment Variables
Create `.env` files for each environment with required secrets:
```bash
# Required environment variables
GITHUB_APP_ID=
GITHUB_PRIVATE_KEY=
WEBHOOK_SECRET=
SLACK_WEBHOOK_URL=
SMTP_HOST=
SMTP_PORT=
SMTP_USER=
SMTP_PASSWORD=
GRAFANA_ADMIN_PASSWORD=
```

## Architecture Overview

The Kiro system consists of three main microservices:

1. **Webhook Handler**: Receives GitHub webhooks and queues processing tasks
2. **PR Processor**: Analyzes pull requests and generates reviews
3. **Notification Service**: Sends notifications via multiple channels

### Supporting Services
- **Redis**: Message queue and caching
- **Prometheus**: Metrics collection
- **Grafana**: Metrics visualization
- **Loki**: Log aggregation
- **Alertmanager**: Alert routing

## Deployment Environments

### Staging Environment
- Namespace: `kiro-system-staging`
- URL: https://kiro-staging.example.com
- Auto-deployment from `develop` branch

### Production Environment
- Namespace: `kiro-system`
- URL: https://kiro.example.com
- Manual deployment from `main` branch
- Requires approval in GitHub Actions

## Deployment Process

### 1. Local Development

```bash
# Build and run locally
docker-compose -f .github/docker/docker-compose.yml up

# Run tests
cd .github/tests
python -m pytest
```

### 2. CI/CD Pipeline

The deployment pipeline is automated via GitHub Actions:

1. **On Pull Request**:
   - Security scanning
   - Code quality checks
   - Unit and integration tests
   - Build validation

2. **On Merge to develop**:
   - Build Docker images
   - Push to GitHub Container Registry
   - Deploy to staging
   - Run smoke tests

3. **On Merge to main**:
   - Deploy to production (requires approval)
   - Run comprehensive health checks
   - Create GitHub deployment record

### 3. Manual Deployment

Use the deployment script for manual deployments:

```bash
# Deploy to staging
./.github/scripts/deploy/deploy.sh -e staging -v v1.2.3

# Deploy to production
./.github/scripts/deploy/deploy.sh -e production -v v1.2.3

# Dry run to see what would be deployed
./.github/scripts/deploy/deploy.sh -e production -v v1.2.3 --dry-run
```

### 4. Kubernetes Deployment

The system uses Kubernetes deployments with:
- Horizontal Pod Autoscaler (HPA)
- Pod Disruption Budgets (PDB)
- Resource limits and requests
- Health checks and readiness probes

Apply Kubernetes manifests:
```bash
kubectl apply -f .github/k8s/deployment.yaml
```

## Monitoring Setup

### 1. Deploy Monitoring Stack

```bash
# Create monitoring namespace
kubectl create namespace monitoring

# Deploy Prometheus
kubectl apply -f .github/monitoring/prometheus.yml

# Deploy Grafana
kubectl apply -f .github/docker/docker-compose.yml
```

### 2. Access Dashboards

- **Grafana**: http://localhost:3000
  - Default user: admin
  - Password: Set via GRAFANA_ADMIN_PASSWORD env var

- **Prometheus**: http://localhost:9091

### 3. Configure Alerts

Alerts are defined in `.github/monitoring/alerts/kiro-alerts.yml`:
- Service health
- Performance metrics
- Security events
- Business metrics

### 4. Log Aggregation

Loki collects logs from all services:
```bash
# View webhook handler logs
logcli query '{job="webhook-handler"}'

# Search for errors
logcli query '{job="webhook-handler"} |~ "ERROR"'
```

## Rollback Procedures

### Automated Rollback

```bash
# Rollback using deployment script
./.github/scripts/deploy/deploy.sh -e production --rollback

# Emergency rollback
./.github/scripts/deploy/rollback.sh production
```

### Manual Rollback

```bash
# Rollback specific deployment
kubectl rollout undo deployment/kiro-webhook-handler -n kiro-system

# Rollback to specific revision
kubectl rollout undo deployment/kiro-webhook-handler --to-revision=2 -n kiro-system
```

### Rollback Verification

```bash
# Check rollout history
kubectl rollout history deployment/kiro-webhook-handler -n kiro-system

# Verify pods are running
kubectl get pods -n kiro-system -l app=kiro
```

## Troubleshooting

### Common Issues

#### 1. Pods Not Starting

```bash
# Check pod status
kubectl describe pod <pod-name> -n kiro-system

# Check logs
kubectl logs <pod-name> -n kiro-system

# Common causes:
# - Missing secrets/configmaps
# - Image pull errors
# - Resource constraints
```

#### 2. Service Unavailable

```bash
# Check service endpoints
kubectl get endpoints -n kiro-system

# Test service connectivity
kubectl port-forward svc/kiro-webhook-service 8080:80 -n kiro-system
curl http://localhost:8080/health
```

#### 3. High Error Rate

```bash
# Check metrics
curl http://localhost:9091/api/v1/query?query=rate(webhook_handler_errors_total[5m])

# Check recent errors in logs
kubectl logs -n kiro-system -l app=kiro --tail=100 | grep ERROR
```

### Debug Commands

```bash
# Get all resources
kubectl get all -n kiro-system -l app=kiro

# Check events
kubectl get events -n kiro-system --sort-by='.lastTimestamp'

# Exec into pod
kubectl exec -it <pod-name> -n kiro-system -- /bin/sh

# Check Redis connectivity
kubectl exec -it <pod-name> -n kiro-system -- redis-cli -h redis ping
```

### Performance Tuning

1. **Adjust HPA settings**:
   ```yaml
   spec:
     minReplicas: 3
     maxReplicas: 20
     targetCPUUtilizationPercentage: 70
   ```

2. **Increase resource limits**:
   ```yaml
   resources:
     requests:
       memory: "512Mi"
       cpu: "500m"
     limits:
       memory: "1Gi"
       cpu: "1000m"
   ```

3. **Configure Redis memory**:
   ```bash
   redis-cli CONFIG SET maxmemory 2gb
   redis-cli CONFIG SET maxmemory-policy allkeys-lru
   ```

## Security Considerations

1. **Secrets Management**:
   - Use Kubernetes secrets for sensitive data
   - Rotate secrets regularly
   - Never commit secrets to git

2. **Network Policies**:
   - Restrict pod-to-pod communication
   - Use service mesh for mTLS

3. **RBAC**:
   - Limit service account permissions
   - Use separate accounts per service

4. **Image Security**:
   - Scan images for vulnerabilities
   - Use minimal base images
   - Run as non-root user

## Maintenance

### Regular Tasks

1. **Weekly**:
   - Review monitoring dashboards
   - Check for security updates
   - Review error logs

2. **Monthly**:
   - Update dependencies
   - Review and optimize alerts
   - Performance analysis

3. **Quarterly**:
   - Disaster recovery drill
   - Security audit
   - Capacity planning

### Backup Procedures

```bash
# Backup Redis data
kubectl exec -n kiro-system redis-0 -- redis-cli BGSAVE

# Backup Prometheus data
kubectl create job prometheus-backup --from=cronjob/prometheus-backup -n monitoring

# Backup Grafana dashboards
kubectl exec -n monitoring grafana-0 -- grafana-cli admin export-dashboard
```

## Support

For issues or questions:
1. Check the [troubleshooting section](#troubleshooting)
2. Review logs in Grafana/Loki
3. Check Prometheus metrics and alerts
4. Contact the platform team via Slack #kiro-support