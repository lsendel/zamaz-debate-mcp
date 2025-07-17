# Runbook: Data Breach Response

**Severity**: CRITICAL  
**Alert**: Potential data breach detected  
**Automated Response**: Evidence preservation and access lockdown  

---

## 🚨 Alert Triggers

- **Data Exfiltration**: Large volume data downloads detected
- **Unauthorized Access**: Access to sensitive data from unknown source
- **Database Anomaly**: Unusual database query patterns
- **API Abuse**: Bulk data export via API
- **Insider Threat**: Privileged user accessing excessive data

---

## ⚡ IMMEDIATE ACTIONS (First 15 Minutes)

### 1. Contain the Breach
```bash
# CRITICAL: Stop active data exfiltration
curl -X POST "https://api.yourdomain.com/api/v1/security/emergency/data-lockdown" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"severity":"CRITICAL","preserveEvidence":true}'

# This will:
# - Block all data export endpoints
# - Terminate suspicious sessions
# - Enable read-only mode for databases
# - Preserve audit logs
```

### 2. Identify Breach Scope
```bash
# Get breach analysis
curl -X GET "https://api.yourdomain.com/api/v1/security/breach/analysis" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '{
    dataAccessed: .summary.dataTypes,
    recordsAffected: .summary.totalRecords,
    timeWindow: {
      start: .summary.firstAccess,
      end: .summary.lastAccess
    },
    sources: .summary.accessSources
  }'

# Identify affected users
curl -X GET "https://api.yourdomain.com/api/v1/security/breach/affected-users" \
  -H "Authorization: Bearer $ADMIN_TOKEN" > affected_users_$(date +%Y%m%d_%H%M%S).json
```

### 3. Preserve Evidence
```bash
# Create forensic snapshot
curl -X POST "https://api.yourdomain.com/api/v1/security/forensics/snapshot" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "includeDatabase": true,
    "includeLogs": true,
    "includeNetworkTraffic": true,
    "encryptSnapshot": true
  }'

# Download audit logs
curl -X GET "https://api.yourdomain.com/api/v1/security/audit/export?hours=48" \
  -H "Authorization: Bearer $ADMIN_TOKEN" > breach_audit_$(date +%Y%m%d_%H%M%S).json
```

---

## 🔍 Breach Assessment (Next 30 Minutes)

### 1. Data Classification
```bash
# Determine data sensitivity
curl -X GET "https://api.yourdomain.com/api/v1/security/breach/data-classification" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.classification[] | {
    dataType: .type,
    sensitivity: .level,
    compliance: .regulations,
    recordCount: .count,
    containsPII: .hasPII,
    containsPHI: .hasPHI,
    containsFinancial: .hasFinancial
  }'
```

### 2. Attack Vector Analysis
```bash
# Identify how the breach occurred
curl -X GET "https://api.yourdomain.com/api/v1/security/breach/attack-vector" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Common vectors:
# - Compromised credentials
# - SQL injection
# - API key leak
# - Insider threat
# - Social engineering
```

### 3. Timeline Reconstruction
```bash
# Build breach timeline
curl -X GET "https://api.yourdomain.com/api/v1/security/breach/timeline" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.events[] | {
    time: .timestamp,
    action: .action,
    user: .userId,
    ip: .sourceIp,
    data: .dataAccessed,
    volume: .recordCount
  }'
```

---

## 🛡️ Containment Actions

### Case 1: External Attacker
```bash
# 1. Block all attacker access points
curl -X POST "https://api.yourdomain.com/api/v1/security/block/comprehensive" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "ips": ["$ATTACKER_IP1", "$ATTACKER_IP2"],
    "userIds": ["$COMPROMISED_USER_ID"],
    "apiKeys": ["$LEAKED_API_KEY"],
    "permanent": true
  }'

# 2. Reset all potentially compromised credentials
curl -X POST "https://api.yourdomain.com/api/v1/security/credentials/mass-reset" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"scope":"organization","forceMFA":true}'

# 3. Patch vulnerability
# Deploy emergency patch based on attack vector
```

### Case 2: Insider Threat
```bash
# 1. Immediately disable user access
curl -X POST "https://api.yourdomain.com/api/v1/users/$USER_ID/emergency-disable" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"preserveEvidence":true,"notifyLegal":true}'

# 2. Revoke all access tokens
curl -X DELETE "https://api.yourdomain.com/api/v1/security/tokens/user/$USER_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# 3. Audit all user activities
curl -X POST "https://api.yourdomain.com/api/v1/security/audit/deep-scan" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"userId":"$USER_ID","daysBack":90,"includeDeleted":true}'
```

---

## 📢 Communication Protocol

### Internal Escalation (First Hour)

| Time | Notify | Method | Template |
|------|--------|--------|----------|
| T+0 | Security Team Lead | Phone + Slack | "CRITICAL: Data breach detected" |
| T+5 | CISO | Phone | Use executive brief template |
| T+15 | Legal Team | Email + Phone | Include initial assessment |
| T+30 | CEO/Board | Phone | High-level impact summary |
| T+45 | PR Team | Secure Channel | Prepare public statement |

### Stakeholder Communication Script
```
CRITICAL SECURITY INCIDENT - DATA BREACH

Time Detected: [TIMESTAMP]
Severity: CRITICAL
Status: CONTAINED/ONGOING

Initial Assessment:
- Data Types Affected: [PII/PHI/Financial/IP]
- Estimated Records: [NUMBER]
- Attack Vector: [TYPE]
- Containment: [STATUS]

Immediate Actions Taken:
1. System lockdown implemented
2. Evidence preserved
3. Investigation underway

Next Steps:
- Forensic analysis in progress
- Legal team engaged
- Customer notification being prepared

ETA for next update: [TIME]
```

---

## 🔬 Forensic Investigation

### 1. Deep Dive Analysis
```bash
# Export all relevant data for forensics
./scripts/breach-forensics.sh \
  --incident-id $INCIDENT_ID \
  --output-dir ./forensics/$(date +%Y%m%d) \
  --encrypt true

# Analyze access patterns
curl -X POST "https://api.yourdomain.com/api/v1/security/forensics/analyze" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "incidentId": "$INCIDENT_ID",
    "analysis": ["access_patterns", "data_flow", "anomalies"]
  }'
```

### 2. Evidence Collection
- Database query logs
- Application logs
- Network traffic captures
- System access logs
- File system changes
- Memory dumps (if available)

---

## 📊 Regulatory Compliance

### Notification Requirements

| Regulation | Timeframe | Threshold | Action Required |
|------------|-----------|-----------|-----------------|
| **GDPR** | 72 hours | Any PII breach | Notify DPA + affected users |
| **CCPA** | Without delay | CA resident data | Notify AG + affected users |
| **HIPAA** | 60 days | PHI breach | Notify HHS + affected individuals |
| **PCI DSS** | Immediately | Card data | Notify card brands + acquirer |

### Compliance Checklist
```bash
□ Document breach discovery time
□ Preserve all evidence
□ Assess data types and volume
□ Determine affected jurisdictions
□ Calculate notification deadlines
□ Prepare regulatory filings
□ Draft user notifications
□ Engage legal counsel
```

---

## 🔄 Recovery Actions

### 1. System Restoration
```bash
# After containment confirmed
# 1. Deploy security patches
kubectl apply -f ./security-patches/breach-fix.yaml

# 2. Reset all credentials
./scripts/credential-rotation.sh --scope all --force true

# 3. Re-enable services with monitoring
curl -X POST "https://api.yourdomain.com/api/v1/system/recovery/initiate" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"enhancedMonitoring":true,"gradualRestore":true}'
```

### 2. Enhanced Security Measures
```bash
# Deploy additional security controls
curl -X POST "https://api.yourdomain.com/api/v1/security/enhance" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "measures": [
      "mandatory_mfa",
      "enhanced_logging",
      "anomaly_detection",
      "zero_trust_mode"
    ]
  }'
```

---

## 📝 Post-Incident Requirements

### 1. Incident Report (Within 48 Hours)
- Executive summary
- Timeline of events
- Data affected
- Root cause analysis
- Remediation actions
- Lessons learned
- Prevention measures

### 2. Customer Notification (As Required)
```
Subject: Important Security Update Regarding Your Account

Dear [Customer Name],

We are writing to inform you of a security incident that may have affected your personal information...

What Happened:
[Clear, factual description]

Information Involved:
[Specific data types]

What We Are Doing:
[Security measures implemented]

What You Should Do:
[Specific actions for customers]

For More Information:
[Contact details and resources]
```

### 3. Security Improvements
- Patch vulnerabilities
- Update security policies
- Enhance monitoring
- Conduct security training
- Review access controls
- Implement lessons learned

---

## 🛠️ Quick Reference Commands

### Emergency Controls
```bash
# Full system lockdown
curl -X POST "https://api.yourdomain.com/api/v1/security/lockdown/full"

# Disable all external access
curl -X PUT "https://api.yourdomain.com/api/v1/security/access/external" \
  -d '{"enabled":false}'

# Enable maximum logging
curl -X PUT "https://api.yourdomain.com/api/v1/logging/level" \
  -d '{"level":"TRACE","duration":"7d"}'

# Block all data exports
curl -X PUT "https://api.yourdomain.com/api/v1/data/export/disable"
```

### Investigation Tools
```bash
# Search for data access
./scripts/data-access-search.sh --user $USER --timeframe 30d

# Analyze API usage
./scripts/api-usage-analysis.sh --suspicious true

# Network traffic analysis
./scripts/network-forensics.sh --capture breach_$(date +%Y%m%d)
```

---

## 📞 Emergency Contacts

### Internal
- **Security Hotline**: +1-XXX-XXX-XXXX (24/7)
- **CISO Direct**: +1-XXX-XXX-XXXX
- **Legal Team**: legal-emergency@yourdomain.com
- **PR Crisis Team**: pr-crisis@yourdomain.com

### External
- **Incident Response Firm**: [Contact Info]
- **Cyber Insurance**: Policy #XXXXX, +1-XXX-XXX-XXXX
- **Law Enforcement**: FBI Cyber Division: +1-XXX-XXX-XXXX
- **Regulatory Bodies**: [Relevant contacts]

---

**CRITICAL**: This is a CRITICAL incident requiring immediate action. Follow this runbook exactly and document all actions taken with timestamps.

**Last Updated**: 2025-07-16  
**Next Review**: Quarterly  
**Owner**: Security Operations Team  
**Classification**: CONFIDENTIAL - Security Incident Response