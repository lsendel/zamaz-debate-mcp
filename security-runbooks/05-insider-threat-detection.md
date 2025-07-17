# Runbook: Insider Threat Detection and Response

**Severity**: High to Critical  
**Alert**: Suspicious internal user activity detected  
**Automated Response**: Activity logging enhanced, access restrictions applied  

---

## 🚨 Alert Triggers

- **Data Access Anomaly**: Accessing unusual amounts/types of data
- **Privilege Abuse**: Using elevated permissions suspiciously
- **Time Anomaly**: Access during unusual hours or from unusual locations
- **Behavior Change**: Significant deviation from baseline behavior
- **Policy Violation**: Repeated security policy violations

---

## 🔍 Initial Assessment (10 minutes)

### 1. User Behavior Analysis
```bash
# Get user risk score and recent activities
curl -X GET "https://api.yourdomain.com/api/v1/security/users/$USER_ID/risk-assessment" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '{
    riskScore: .score,
    riskLevel: .level,
    anomalies: .anomalies[],
    recentActivities: .activities[0:10] | map({
      time: .timestamp,
      action: .action,
      resource: .resource,
      anomalyScore: .score
    })
  }'

# Compare to baseline behavior
curl -X GET "https://api.yourdomain.com/api/v1/security/users/$USER_ID/baseline-deviation" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 2. Access Pattern Review
```bash
# Analyze data access patterns
curl -X GET "https://api.yourdomain.com/api/v1/security/audit/user-access/$USER_ID?days=30" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '{
    dataTypes: .accessPatterns | group_by(.dataType) | map({
      type: .[0].dataType,
      count: length,
      sensitive: .[0].isSensitive
    }),
    unusualAccess: .anomalies[],
    downloadVolume: .metrics.totalDownloadMB,
    afterHoursAccess: .metrics.afterHoursPercentage
  }'
```

### 3. Privilege Usage Analysis
```bash
# Check for privilege escalation or abuse
curl -X GET "https://api.yourdomain.com/api/v1/security/privileges/usage/$USER_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '{
    elevatedActions: .privilegedActions[],
    unusualPermissions: .anomalousUsage[],
    failedAttempts: .deniedActions[],
    roleChanges: .recentRoleChanges[]
  }'
```

---

## 🎯 Threat Classification

### Level 1: Suspicious Activity
**Indicators**: Minor anomalies, first-time violations

#### Response Actions:
```bash
# 1. Enable enhanced monitoring
curl -X POST "https://api.yourdomain.com/api/v1/security/monitoring/user/$USER_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "level": "ENHANCED",
    "duration": "7d",
    "logAllActions": true,
    "alertThreshold": "MEDIUM"
  }'

# 2. Send security awareness reminder
curl -X POST "https://api.yourdomain.com/api/v1/communications/security-reminder" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "$USER_ID",
    "type": "POLICY_REMINDER",
    "trackAcknowledgment": true
  }'
```

### Level 2: High-Risk Behavior
**Indicators**: Multiple anomalies, sensitive data access, policy violations

#### Response Actions:
```bash
# 1. Restrict access to sensitive data
curl -X POST "https://api.yourdomain.com/api/v1/security/access/restrict" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "$USER_ID",
    "restrictions": ["SENSITIVE_DATA", "BULK_EXPORT", "API_ACCESS"],
    "duration": "24h",
    "requireApproval": true
  }'

# 2. Require re-authentication for sensitive actions
curl -X PUT "https://api.yourdomain.com/api/v1/security/users/$USER_ID/security" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "requireMFA": true,
    "sessionTimeout": 900,
    "requireReauth": ["DELETE", "EXPORT", "ADMIN_ACTIONS"]
  }'

# 3. Alert user's manager
curl -X POST "https://api.yourdomain.com/api/v1/notifications/manager-alert" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "$USER_ID",
    "severity": "HIGH",
    "summary": "Unusual activity detected requiring review"
  }'
```

### Level 3: Critical Threat
**Indicators**: Data exfiltration attempts, system compromise, malicious intent

#### IMMEDIATE Response:
```bash
# 1. EMERGENCY: Suspend user access
curl -X POST "https://api.yourdomain.com/api/v1/security/emergency/suspend-user" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "$USER_ID",
    "preserveEvidence": true,
    "terminateSessions": true,
    "revokeTokens": true,
    "disableAccount": true
  }'

# 2. Preserve evidence
curl -X POST "https://api.yourdomain.com/api/v1/security/forensics/preserve" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "$USER_ID",
    "includeLogs": true,
    "includeFiles": true,
    "includeDatabase": true,
    "legalHold": true
  }'

# 3. Initiate incident response
curl -X POST "https://api.yourdomain.com/api/v1/security/incidents/create" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "INSIDER_THREAT",
    "severity": "CRITICAL",
    "userId": "$USER_ID",
    "notifyLegal": true,
    "notifyHR": true
  }'
```

---

## 🔍 Deep Investigation

### 1. Timeline Reconstruction
```bash
# Build comprehensive activity timeline
curl -X POST "https://api.yourdomain.com/api/v1/security/forensics/timeline" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "$USER_ID",
    "startDate": "30 days ago",
    "includeAll": true,
    "correlateEvents": true
  }' | jq '.events[] | {
    timestamp: .time,
    action: .action,
    resource: .resource,
    ip: .sourceIp,
    device: .deviceId,
    suspicious: .anomalyFlags
  }'
```

### 2. Data Movement Analysis
```bash
# Track all data movements
curl -X GET "https://api.yourdomain.com/api/v1/security/data-movement/$USER_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '{
    downloads: .downloads[] | {
      time: .timestamp,
      file: .resource,
      size: .sizeMB,
      destination: .downloadLocation
    },
    uploads: .uploads[],
    emails: .emailAttachments[],
    apiExports: .apiDataExports[],
    totalVolumeGB: .summary.totalGB
  }'
```

### 3. Collaboration Pattern Analysis
```bash
# Check for unusual collaboration patterns
curl -X GET "https://api.yourdomain.com/api/v1/security/collaboration/$USER_ID/analysis" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '{
    newCollaborators: .recentCollaborators[] | select(.isNew),
    externalSharing: .externalShares[],
    unusualRecipients: .anomalousRecipients[],
    afterHoursActivity: .afterHoursCollaboration[]
  }'
```

---

## 🛡️ Containment Strategies

### Covert Monitoring (When gathering evidence)
```bash
# Enable silent monitoring mode
curl -X POST "https://api.yourdomain.com/api/v1/security/monitoring/covert" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "$USER_ID",
    "captureAll": true,
    "alertSuppression": true,
    "honeytokens": true,
    "duration": "48h"
  }'

# Deploy honeypot files
curl -X POST "https://api.yourdomain.com/api/v1/security/honeypots/deploy" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "targetUser": "$USER_ID",
    "honeypotTypes": ["FINANCIAL_DATA", "CUSTOMER_LIST", "SOURCE_CODE"],
    "trackAccess": true
  }'
```

### Progressive Restrictions
```bash
# Gradually restrict access to avoid alerting subject
curl -X POST "https://api.yourdomain.com/api/v1/security/access/progressive-restrict" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "$USER_ID",
    "schedule": [
      {"delay": "0h", "restrict": ["BULK_OPERATIONS"]},
      {"delay": "4h", "restrict": ["SENSITIVE_READ"]},
      {"delay": "8h", "restrict": ["ALL_WRITE"]},
      {"delay": "24h", "restrict": ["COMPLETE_SUSPENSION"]}
    ]
  }'
```

---

## 📊 Risk Indicators

### Behavioral Red Flags
```bash
# Query comprehensive risk indicators
curl -X GET "https://api.yourdomain.com/api/v1/security/risk-indicators/$USER_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Key indicators:
# - Downloading entire databases
# - Accessing data outside job role
# - Using USB devices extensively
# - Forwarding emails to personal accounts
# - Deleting audit logs or history
# - Installing unauthorized software
# - Bypassing security controls
# - Working unusual hours consistently
# - Resignation/termination pending
```

### Technical Indicators
- Unusual SQL queries
- Mass file operations
- VPN from unusual locations
- Multiple failed auth attempts
- Privilege escalation attempts
- Clear browser history frequently
- Use of encryption tools
- Port scanning internal network

---

## 📋 Investigation Checklist

### Immediate Actions
```bash
□ Document initial alert details
□ Preserve current state evidence
□ Check user's access level and permissions
□ Review recent activity logs
□ Identify accessed sensitive data
□ Check for data exfiltration indicators
□ Review email and communication patterns
□ Check device and location anomalies
```

### Investigation Steps
```bash
□ Interview user's manager
□ Review HR records (performance, disciplinary)
□ Check financial stress indicators
□ Review access badge logs
□ Examine workstation (if applicable)
□ Review cloud storage usage
□ Check personal device usage
□ Analyze network traffic patterns
```

### Legal/HR Coordination
```bash
□ Notify legal counsel
□ Engage HR department
□ Document chain of custody
□ Prepare for possible litigation
□ Consider law enforcement involvement
□ Plan communication strategy
□ Prepare termination procedures (if needed)
□ Review employment agreements
```

---

## 📢 Communication Protocol

### Internal Stakeholders
| Stakeholder | When to Notify | Communication Method |
|-------------|----------------|---------------------|
| Direct Manager | Suspicious activity | Secure email |
| HR Department | High-risk behavior | Phone + secure email |
| Legal Counsel | Critical threat | Immediate phone |
| CISO | Any insider threat | Escalation protocol |
| CEO | Critical threat | CISO escalates |

### Communication Templates
```
Subject: [CONFIDENTIAL] Security Investigation Required

This communication is strictly confidential and protected by attorney-client privilege.

An internal security review has identified activity requiring investigation:
- User: [EMPLOYEE_NAME]
- Department: [DEPARTMENT]
- Risk Level: [LOW/MEDIUM/HIGH/CRITICAL]
- Immediate Action: [REQUIRED_ACTION]

Please contact the security team immediately at [SECURE_CONTACT].
Do not discuss this matter with anyone except authorized personnel.
```

---

## 🔧 Technical Controls

### Endpoint Monitoring
```bash
# Deploy endpoint agent for detailed monitoring
curl -X POST "https://api.yourdomain.com/api/v1/endpoint/deploy-agent" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "targetDevice": "$DEVICE_ID",
    "monitoringLevel": "FORENSIC",
    "captureScreenshots": true,
    "keylogger": false,
    "requireLegalApproval": true
  }'
```

### Network Isolation
```bash
# Isolate user to restricted network segment
curl -X POST "https://api.yourdomain.com/api/v1/network/isolate-user" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "$USER_ID",
    "allowedServices": ["EMAIL", "BASIC_WEB"],
    "blockServices": ["FILE_TRANSFER", "CLOUD_STORAGE", "VPN"],
    "monitorAll": true
  }'
```

---

## 📈 Metrics and Reporting

### Key Metrics to Track
- Time to detection
- False positive rate
- Data loss prevented
- Investigation duration
- Successful prosecutions
- Repeat offenders
- Department risk scores
- Training effectiveness

### Monthly Report Template
```bash
# Generate insider threat metrics report
curl -X POST "https://api.yourdomain.com/api/v1/security/reports/insider-threat" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "period": "monthly",
    "includeMetrics": true,
    "includeTrends": true,
    "includeRecommendations": true
  }'
```

---

## 🛡️ Prevention Strategies

### Technical Controls
- User behavior analytics (UBA)
- Data loss prevention (DLP)
- Privileged access management (PAM)
- Zero trust architecture
- Continuous authentication
- Anomaly detection ML models

### Administrative Controls
- Background checks
- Security awareness training
- Clear acceptable use policies
- Regular access reviews
- Separation of duties
- Mandatory vacations
- Exit procedures

### Physical Controls
- Badge access monitoring
- CCTV in sensitive areas
- USB port restrictions
- Clean desk policy
- Visitor management
- Asset tracking

---

**Last Updated**: 2025-07-16  
**Next Review**: Monthly  
**Owner**: Security Operations Team  
**Classification**: CONFIDENTIAL - Internal Security Use Only