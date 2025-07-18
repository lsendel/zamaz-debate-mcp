# Operations Manual

## Overview

This manual provides comprehensive guidance for deploying, monitoring, and maintaining the MCP Debate System in production environments.

## Table of Contents

1. [Deployment Guide](deployment.md)
2. [Monitoring & Alerts](monitoring.md)
3. [Logging & Diagnostics](logging-diagnostics.md)
4. [Performance Tuning](#performance-tuning)
5. [Disaster Recovery](#disaster-recovery)
6. [Maintenance Procedures](#maintenance-procedures)

## Production Readiness Checklist

### Pre-Deployment
- [ ] Security audit completed
- [ ] Load testing passed
- [ ] Backup procedures tested
- [ ] Monitoring configured
- [ ] Documentation updated
- [ ] Runbooks prepared

### Infrastructure
- [ ] High availability configured
- [ ] Auto-scaling enabled
- [ ] SSL certificates installed
- [ ] DNS configured
- [ ] CDN setup
- [ ] Firewall rules configured

### Application
- [ ] Environment variables set
- [ ] Secrets management configured
- [ ] Health checks implemented
- [ ] Rate limiting enabled
- [ ] Error tracking configured

## Deployment

### Kubernetes Deployment

```bash
# Deploy to production
kubectl apply -f k8s/production/

# Verify deployment
kubectl get pods -n production
kubectl get services -n production

# Check ingress
kubectl get ingress -n production
```

### Blue-Green Deployment

```bash
# Deploy green version
kubectl apply -f k8s/production/deployments-green.yaml

# Test green deployment
curl https://green.mcp-debate.com/health

# Switch traffic to green
kubectl patch service mcp-gateway -n production \
  -p '{"spec":{"selector":{"version":"green"}}}'

# Remove blue deployment
kubectl delete deployment mcp-gateway-blue -n production
```

### Rollback Procedures

```bash
# Quick rollback
kubectl rollout undo deployment/mcp-gateway -n production

# Rollback to specific version
kubectl rollout undo deployment/mcp-gateway \
  --to-revision=3 -n production

# Emergency rollback script
./scripts/emergency-rollback.sh production
```

## Monitoring

### Key Metrics

#### System Health
- CPU usage < 70%
- Memory usage < 80%
- Disk usage < 85%
- Network latency < 100ms

#### Application Metrics
- Response time p95 < 2s
- Error rate < 1%
- Throughput > 100 req/s
- Queue depth < 1000

#### Business Metrics
- Active debates
- Messages per minute
- AI provider response time
- User engagement rate

### Dashboards

Access Grafana dashboards:
- **Overview**: https://monitoring.mcp-debate.com/d/overview
- **API Performance**: https://monitoring.mcp-debate.com/d/api
- **Database**: https://monitoring.mcp-debate.com/d/database
- **Business Metrics**: https://monitoring.mcp-debate.com/d/business

### Alerts

Critical alerts configured:
- Service down > 2 minutes
- Error rate > 5%
- Response time > 5s
- Database connections exhausted
- Disk space < 10%

## Performance Tuning

### JVM Optimization

```bash
# Production JVM settings
JAVA_OPTS="-server \
  -Xms2g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+UseStringDeduplication \
  -XX:+OptimizeStringConcat \
  -Xlog:gc*:file=gc.log:time,uptime:filecount=5,filesize=10M"
```

### Database Tuning

```sql
-- PostgreSQL configuration
max_connections = 200
shared_buffers = 2GB
effective_cache_size = 6GB
work_mem = 10MB
maintenance_work_mem = 512MB
wal_level = replica
max_wal_size = 2GB
checkpoint_completion_target = 0.9
```

### Redis Optimization

```conf
# Redis configuration
maxmemory 2gb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000
appendonly yes
appendfsync everysec
```

## Disaster Recovery

### Backup Strategy

#### Database Backups
```bash
# Automated daily backups
0 2 * * * /scripts/backup-database.sh

# Manual backup
pg_dump -h postgres-primary -U postgres debate_db | \
  gzip > backup-$(date +%Y%m%d-%H%M%S).sql.gz

# Upload to S3
aws s3 cp backup-*.sql.gz s3://mcp-backups/postgres/
```

#### Application State
```bash
# Redis backup
redis-cli BGSAVE
cp /var/lib/redis/dump.rdb backup-redis-$(date +%Y%m%d).rdb

# Configuration backup
tar -czf config-backup-$(date +%Y%m%d).tar.gz \
  /etc/mcp/ \
  k8s/ \
  docker/
```

### Recovery Procedures

#### Database Recovery
```bash
# Restore from backup
gunzip < backup-20240117.sql.gz | \
  psql -h postgres-primary -U postgres debate_db

# Verify restoration
psql -h postgres-primary -U postgres debate_db \
  -c "SELECT COUNT(*) FROM debates;"
```

#### Full System Recovery
1. Provision infrastructure
2. Deploy Kubernetes cluster
3. Restore databases
4. Deploy applications
5. Restore configuration
6. Verify functionality

### RTO/RPO Targets
- **RTO** (Recovery Time Objective): 4 hours
- **RPO** (Recovery Point Objective): 1 hour

## Maintenance Procedures

### Regular Maintenance

#### Daily Tasks
- [ ] Check system health dashboards
- [ ] Review error logs
- [ ] Verify backup completion
- [ ] Check disk usage

#### Weekly Tasks
- [ ] Review performance metrics
- [ ] Update dependencies
- [ ] Test backup restoration
- [ ] Security scan

#### Monthly Tasks
- [ ] Performance analysis
- [ ] Capacity planning review
- [ ] Security audit
- [ ] Disaster recovery drill

### Database Maintenance

```bash
# Vacuum and analyze
psql -U postgres -d debate_db -c "VACUUM ANALYZE;"

# Reindex
psql -U postgres -d debate_db -c "REINDEX DATABASE debate_db;"

# Check for bloat
SELECT schemaname, tablename, 
  pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables 
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

### Certificate Renewal

```bash
# Check certificate expiry
openssl x509 -in /etc/ssl/certs/mcp-debate.crt -noout -dates

# Renew with Let's Encrypt
certbot renew --nginx

# Verify renewal
nginx -t && nginx -s reload
```

## Troubleshooting

### Service Health Issues

1. **Check pod status**
   ```bash
   kubectl get pods -n production
   kubectl describe pod <pod-name> -n production
   kubectl logs <pod-name> -n production
   ```

2. **Check service endpoints**
   ```bash
   kubectl get endpoints -n production
   kubectl get svc -n production
   ```

3. **Network connectivity**
   ```bash
   kubectl exec -it <pod-name> -n production -- nc -zv service-name 8080
   ```

### Performance Issues

1. **Identify slow queries**
   ```sql
   SELECT query, mean_exec_time, calls 
   FROM pg_stat_statements 
   ORDER BY mean_exec_time DESC 
   LIMIT 10;
   ```

2. **Check cache hit rates**
   ```bash
   redis-cli INFO stats | grep hit
   ```

3. **Profile JVM**
   ```bash
   jcmd <pid> VM.native_memory summary
   jcmd <pid> Thread.print
   ```

### Emergency Contacts

- **On-Call Engineer**: +1-XXX-XXX-XXXX
- **Platform Team**: platform@mcp-debate.com
- **Security Team**: security@mcp-debate.com
- **Escalation**: cto@mcp-debate.com

## Runbooks

Detailed runbooks available:
- [Service Restart Procedures](runbooks/service-restart.md)
- [Database Failover](runbooks/database-failover.md)
- [Traffic Management](runbooks/traffic-management.md)
- [Incident Response](runbooks/incident-response.md)