# Disaster Recovery Plan

## Overview

This document outlines the disaster recovery procedures for the MCP Debate System, including backup strategies, recovery processes, and business continuity plans.

## Recovery Objectives

- **RTO (Recovery Time Objective)**: 4 hours maximum
- **RPO (Recovery Point Objective)**: 1 hour maximum data loss
- **Service Level**: 99.9% uptime (8.76 hours downtime per year)

## Disaster Scenarios

### Scenario 1: Single Service Failure
- **Impact**: Degraded functionality
- **Recovery**: Automatic failover or service restart
- **RTO**: 5 minutes
- **RPO**: 0 (no data loss)

### Scenario 2: Database Corruption
- **Impact**: Data unavailable
- **Recovery**: Database restore from backup
- **RTO**: 2 hours
- **RPO**: 1 hour

### Scenario 3: Complete Infrastructure Loss
- **Impact**: Complete service outage
- **Recovery**: Full infrastructure rebuild
- **RTO**: 4 hours
- **RPO**: 1 hour

### Scenario 4: Security Breach
- **Impact**: Potential data compromise
- **Recovery**: Security incident response
- **RTO**: 8 hours
- **RPO**: 1 hour

## Backup Strategy

### Automated Backups

#### Database Backups
```bash
# Daily full backup at 2 AM
0 2 * * * /opt/mcp/scripts/backup/backup-database.sh

# Hourly incremental backups during business hours
0 9-17 * * * /opt/mcp/scripts/backup/backup-database.sh --incremental
```

#### Configuration Backups
- **Frequency**: Daily
- **Retention**: 30 days
- **Storage**: Local + S3
- **Includes**: 
  - Application configurations
  - Kubernetes manifests
  - SSL certificates
  - Environment variables (encrypted)

#### Redis Backups
- **Frequency**: Every 6 hours
- **Method**: BGSAVE with file copy
- **Retention**: 7 days
- **Storage**: Local + S3

### Backup Verification

Daily backup verification process:
1. Integrity check of backup files
2. Test restoration to staging environment
3. Verification of restored data
4. Performance benchmarks on restored system

## Recovery Procedures

### Quick Recovery Checklist

#### Immediate Response (0-15 minutes)
- [ ] Assess the scope of the outage
- [ ] Activate incident response team
- [ ] Communicate outage to stakeholders
- [ ] Begin initial diagnostics

#### Short-term Recovery (15 minutes - 2 hours)
- [ ] Implement temporary workarounds
- [ ] Begin restoration from backups
- [ ] Monitor recovery progress
- [ ] Update stakeholders

#### Full Recovery (2-4 hours)
- [ ] Complete system restoration
- [ ] Verify data integrity
- [ ] Run comprehensive tests
- [ ] Resume normal operations

### Service-Specific Recovery

#### PostgreSQL Recovery
```bash
# Stop all services using the database
docker-compose stop mcp-organization mcp-gateway mcp-debate-engine mcp-rag

# Restore database from backup
./scripts/recovery/restore-database.sh --type postgres --file latest

# Verify data integrity
psql -U postgres -d debate_db -c "SELECT COUNT(*) FROM debates;"

# Restart services
docker-compose start mcp-organization mcp-gateway mcp-debate-engine mcp-rag
```

#### Redis Recovery
```bash
# Stop Redis and dependent services
docker-compose stop redis mcp-gateway mcp-debate-engine

# Restore Redis data
./scripts/recovery/restore-database.sh --type redis --file latest

# Restart services
docker-compose start redis mcp-gateway mcp-debate-engine
```

#### Application Recovery
```bash
# Restore configuration files
./scripts/recovery/restore-database.sh --type config --file latest

# Rebuild and restart containers
docker-compose build
docker-compose up -d

# Verify service health
./scripts/maintenance/health-check.sh
```

#### Kubernetes Recovery
```bash
# Restore Kubernetes resources
./scripts/recovery/restore-database.sh --type k8s --file latest

# Wait for pods to be ready
kubectl wait --for=condition=ready pod --all -n production --timeout=300s

# Verify ingress and services
kubectl get ingress,svc -n production
```

## Infrastructure Recovery

### Cloud Infrastructure (AWS)

#### VPC and Networking
```bash
# Deploy VPC from Terraform
cd infrastructure/terraform
terraform init
terraform plan -var-file="production.tfvars"
terraform apply -auto-approve

# Verify networking
aws ec2 describe-vpcs --filters "Name=tag:Environment,Values=production"
```

#### EKS Cluster
```bash
# Create EKS cluster
eksctl create cluster --config-file infrastructure/eks-cluster.yaml

# Install ingress controller
kubectl apply -f infrastructure/k8s/ingress-nginx/

# Deploy cert-manager
kubectl apply -f infrastructure/k8s/cert-manager/
```

#### RDS Database
```bash
# Restore RDS from snapshot
aws rds restore-db-instance-from-db-snapshot \
    --db-instance-identifier mcp-debate-prod \
    --db-snapshot-identifier mcp-debate-backup-$(date +%Y%m%d)

# Update connection strings in secrets
kubectl patch secret database-credentials -n production \
    --patch '{"data":{"host":"<new-rds-endpoint>"}}'
```

### On-Premises Recovery

#### Hardware Replacement
1. **Provision new hardware** according to specifications
2. **Install base operating system** (Ubuntu 22.04 LTS)
3. **Configure networking** and firewall rules
4. **Install Docker and dependencies**
5. **Restore application from backups**

#### Storage Recovery
```bash
# Mount backup storage
mount /dev/sdb1 /mnt/backup

# Restore application data
rsync -av /mnt/backup/mcp/ /opt/mcp/

# Restore database
./scripts/recovery/restore-database.sh --type all --file /mnt/backup/latest/
```

## Testing and Validation

### Recovery Testing Schedule

#### Monthly Tests
- Database backup/restore test
- Redis backup/restore test
- Configuration restoration test
- Service failover test

#### Quarterly Tests
- Full disaster recovery simulation
- Cross-region failover test
- Security incident response drill
- Business continuity test

#### Annual Tests
- Complete infrastructure rebuild
- Multi-day outage simulation
- Third-party vendor failure test
- Natural disaster scenario

### Validation Procedures

#### Data Integrity Checks
```sql
-- Verify debate data consistency
SELECT 
    COUNT(*) as total_debates,
    COUNT(DISTINCT organization_id) as unique_orgs,
    MAX(created_at) as latest_debate
FROM debates;

-- Check message consistency
SELECT 
    d.id,
    d.title,
    COUNT(dm.id) as message_count
FROM debates d
LEFT JOIN debate_messages dm ON d.id = dm.debate_id
GROUP BY d.id, d.title
HAVING COUNT(dm.id) = 0;
```

#### Performance Validation
```bash
# Run performance tests
cd performance-tests
./run-performance-tests.sh load k6

# Check response times
curl -w "@curl-format.txt" -s -o /dev/null http://api.mcp-debate.com/health

# Verify SLA compliance
./scripts/maintenance/health-check.sh
```

#### Functional Testing
```bash
# Run smoke tests
cd tests/e2e
npm test -- --grep "smoke"

# API integration tests
cd tests/api
./run-api-tests.sh

# User acceptance tests
cd tests/user-acceptance
./run-uat.sh
```

## Communication Plan

### Stakeholder Notification

#### Internal Stakeholders
- **Engineering Team**: Immediate (Slack, PagerDuty)
- **Management**: Within 30 minutes (Email, Phone)
- **Support Team**: Within 15 minutes (Slack, Email)
- **Sales Team**: Within 2 hours (Email)

#### External Stakeholders
- **Customers**: Within 1 hour (Status page, Email)
- **Partners**: Within 2 hours (Email)
- **Regulators**: Within 24 hours (if required)

### Communication Templates

#### Internal Incident Alert
```
🚨 INCIDENT: MCP Production Outage
Severity: P1 - Critical
Start Time: [TIME]
Impact: [DESCRIPTION]
Current Status: [STATUS]
ETA: [TIME]
Response Team: [TEAM MEMBERS]
```

#### Customer Communication
```
Subject: Service Disruption - MCP Debate Platform

We are currently experiencing a service disruption affecting the MCP Debate Platform. 

Impact: [DESCRIPTION]
Start Time: [TIME]
Expected Resolution: [TIME]

We are working to resolve this issue as quickly as possible and will provide updates every 30 minutes.

Status Page: https://status.mcp-debate.com
```

## Recovery Team

### Roles and Responsibilities

#### Incident Commander
- Overall incident coordination
- Decision making authority
- Stakeholder communication
- Resource allocation

#### Technical Lead
- Technical recovery decisions
- System architecture expertise
- Recovery procedure execution
- Post-incident analysis

#### Database Administrator
- Database recovery operations
- Data integrity verification
- Performance optimization
- Backup management

#### Infrastructure Engineer
- Infrastructure provisioning
- Network configuration
- Security implementation
- Monitoring setup

#### Communication Manager
- Stakeholder notifications
- Status page updates
- Customer communications
- Media relations

### Escalation Procedures

1. **Level 1**: On-call engineer (immediate response)
2. **Level 2**: Technical lead + DBA (15 minutes)
3. **Level 3**: Incident commander (30 minutes)
4. **Level 4**: CTO + senior management (1 hour)

## Post-Incident Activities

### Immediate Post-Recovery (0-24 hours)
- [ ] Verify all systems operational
- [ ] Monitor for secondary issues
- [ ] Complete incident timeline
- [ ] Notify stakeholders of resolution

### Short-term Follow-up (1-7 days)
- [ ] Conduct post-incident review
- [ ] Identify root causes
- [ ] Document lessons learned
- [ ] Plan preventive measures

### Long-term Improvements (1-4 weeks)
- [ ] Implement process improvements
- [ ] Update disaster recovery procedures
- [ ] Enhance monitoring and alerting
- [ ] Conduct additional training

### Post-Incident Review Template

```markdown
# Post-Incident Review

## Incident Summary
- **Date/Time**: 
- **Duration**: 
- **Severity**: 
- **Root Cause**: 

## Timeline
| Time | Event | Actions Taken |
|------|-------|---------------|
|      |       |               |

## Impact Assessment
- **Users Affected**: 
- **Revenue Impact**: 
- **SLA Breach**: 
- **Data Loss**: 

## What Went Well
- 
- 

## What Could Be Improved
- 
- 

## Action Items
| Item | Owner | Due Date | Status |
|------|-------|----------|--------|
|      |       |          |        |

## Preventive Measures
- 
- 
```

## Contact Information

### Emergency Contacts
- **On-Call Engineer**: +1-XXX-XXX-XXXX
- **Incident Commander**: +1-XXX-XXX-XXXX
- **CTO**: +1-XXX-XXX-XXXX

### Vendor Contacts
- **AWS Support**: +1-XXX-XXX-XXXX (Premium Support)
- **Database Vendor**: +1-XXX-XXX-XXXX
- **Security Vendor**: +1-XXX-XXX-XXXX

### External Services
- **Status Page**: https://status.mcp-debate.com
- **Monitoring**: https://monitoring.mcp-debate.com
- **Backup Storage**: AWS S3 (us-east-1, us-west-2)

## Legal and Compliance

### Data Protection
- Follow GDPR requirements for data breach notification
- Maintain data residency compliance
- Document all data access during recovery
- Ensure encryption of restored data

### Audit Requirements
- Log all recovery actions
- Maintain chain of custody for backups
- Document access controls during incident
- Prepare regulatory notifications if required

## Appendices

### Appendix A: Emergency Runbooks
- [Service Restart Procedures](runbooks/service-restart.md)
- [Database Failover](runbooks/database-failover.md)
- [Network Recovery](runbooks/network-recovery.md)

### Appendix B: Recovery Scripts
- [Backup Script](../scripts/backup/backup-database.sh)
- [Recovery Script](../scripts/recovery/restore-database.sh)
- [Health Check](../scripts/maintenance/health-check.sh)

### Appendix C: Infrastructure Diagrams
- Network topology
- Service dependencies
- Data flow diagrams
- Recovery architecture