# Security Runbooks

This directory contains operational runbooks for handling various security incidents and scenarios in the zamaz-debate-mcp system.

## 📚 Available Runbooks

### 1. [Authentication Failures](./01-authentication-failures.md)
**When to use**: High number of authentication failures detected  
**Severity**: Medium to High  
**Key scenarios**:
- Brute force attacks
- Credential stuffing
- Account takeover attempts
- Distributed authentication attacks

### 2. [Suspicious Activity Detection](./02-suspicious-activity.md)
**When to use**: Malicious patterns or suspicious behavior detected  
**Severity**: High to Critical  
**Key scenarios**:
- XSS/Injection attempts
- Security scanner detection
- Path traversal attacks
- Privilege escalation attempts
- API abuse

### 3. [Data Breach Response](./03-data-breach-response.md)
**When to use**: Potential or confirmed data breach  
**Severity**: CRITICAL  
**Key scenarios**:
- Data exfiltration detected
- Unauthorized data access
- Database compromise
- API data leaks
- Insider data theft

### 4. [DDoS Attack Mitigation](./04-ddos-mitigation.md)
**When to use**: Distributed Denial of Service attack detected  
**Severity**: High to Critical  
**Key scenarios**:
- Volume-based attacks
- Application layer attacks
- Amplification attacks
- Resource exhaustion

### 5. [Insider Threat Detection](./05-insider-threat-detection.md)
**When to use**: Suspicious internal user activity  
**Severity**: High to Critical  
**Key scenarios**:
- Abnormal data access
- Privilege abuse
- Policy violations
- Data theft indicators
- Malicious insider activity

## 🚀 Quick Start Guide

### How to Use These Runbooks

1. **Identify the Incident Type**
   - Check monitoring alerts
   - Review security dashboards
   - Analyze initial indicators

2. **Select Appropriate Runbook**
   - Match incident to runbook type
   - Note severity level
   - Check escalation requirements

3. **Follow Step-by-Step Procedures**
   - Execute commands in order
   - Document all actions taken
   - Collect evidence as directed
   - Escalate when indicated

4. **Complete Post-Incident Tasks**
   - Generate incident report
   - Update security measures
   - Document lessons learned
   - Update runbook if needed

## 📋 Common Prerequisites

### Required Access
- Admin API token (`$ADMIN_TOKEN`)
- Access to security dashboards
- SSH access to infrastructure
- Cloud provider console access

### Required Tools
```bash
# Ensure these tools are available
- curl (API interactions)
- jq (JSON parsing)
- kubectl (Kubernetes management)
- aws-cli (AWS operations)
- docker (Container management)
```

### Environment Variables
```bash
export ADMIN_TOKEN="your-admin-token"
export CDN_TOKEN="your-cdn-token"
export WAF_TOKEN="your-waf-token"
export SLACK_WEBHOOK="your-slack-webhook"
```

## 🔧 Useful Commands Reference

### System Status
```bash
# Check all service health
curl -X GET "https://api.yourdomain.com/api/v1/system/health/all" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# View active security incidents
curl -X GET "https://api.yourdomain.com/api/v1/security/incidents/active" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Check security metrics
curl -X GET "https://api.yourdomain.com/api/v1/security/metrics/summary" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Emergency Controls
```bash
# Enable emergency mode (blocks all non-essential access)
curl -X POST "https://api.yourdomain.com/api/v1/security/emergency/enable" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Force logout all users
curl -X POST "https://api.yourdomain.com/api/v1/security/sessions/terminate-all" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Enable read-only mode
curl -X PUT "https://api.yourdomain.com/api/v1/system/read-only" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"enabled":true}'
```

## 📊 Severity Levels

| Level | Response Time | Escalation | Examples |
|-------|---------------|------------|----------|
| **LOW** | <4 hours | L1 Engineer | Failed logins, minor anomalies |
| **MEDIUM** | <1 hour | L2 Engineer | Sustained attacks, policy violations |
| **HIGH** | <15 minutes | Team Lead + L2 | Active attacks, data access |
| **CRITICAL** | Immediate | All hands | Data breach, system compromise |

## 📞 Escalation Contacts

### 24/7 Security Operations Center
- **Phone**: +1-XXX-XXX-XXXX
- **Email**: soc@yourdomain.com
- **Slack**: #security-incidents

### On-Call Rotation
- **Primary**: Check PagerDuty
- **Secondary**: +1-XXX-XXX-XXXX
- **Manager**: +1-XXX-XXX-XXXX

### Executive Escalation
- **CISO**: ciso@yourdomain.com
- **CTO**: cto@yourdomain.com
- **CEO**: (Through CISO only)

## 🔄 Runbook Maintenance

### Review Schedule
- **Monthly**: Review and update procedures
- **Quarterly**: Full runbook testing
- **Annually**: Complete overhaul and training

### Update Process
1. Document incident variations
2. Propose runbook updates
3. Test in staging environment
4. Security team review
5. Deploy to production

### Training Requirements
- All security team members must be familiar with all runbooks
- Quarterly tabletop exercises
- Annual hands-on incident simulation
- New team member onboarding includes runbook walkthrough

## 📚 Additional Resources

### Internal Documentation
- [Security Architecture Overview](../COMPREHENSIVE-SECURITY-SUMMARY.md)
- [Production Deployment Guide](../PRODUCTION-SECURITY-DEPLOYMENT-GUIDE.md)
- [API Security Guide](../API-SECURITY-GUIDE.md)

### External Resources
- [OWASP Incident Response Guide](https://owasp.org/www-project-incident-response/)
- [NIST Incident Handling Guide](https://nvlpubs.nist.gov/nistpubs/SpecialPublications/NIST.SP.800-61r2.pdf)
- [SANS Incident Handler's Handbook](https://www.sans.org/white-papers/)

### Tools and Scripts
- `/scripts/security-scan.sh` - Run security scan
- `/scripts/incident-response.sh` - Incident response automation
- `/scripts/forensics-collection.sh` - Evidence collection
- `/scripts/security-test-suite.sh` - Security testing

## ⚠️ Important Notes

1. **Always Document**: Record all actions taken with timestamps
2. **Preserve Evidence**: Never modify logs or data during investigation
3. **Follow Legal Requirements**: Engage legal team for any law enforcement interaction
4. **Maintain Confidentiality**: Discuss incidents only with authorized personnel
5. **Learn and Improve**: Every incident is a learning opportunity

---

**Last Updated**: 2025-07-16  
**Maintained By**: Security Operations Team  
**Review Status**: ✅ Current