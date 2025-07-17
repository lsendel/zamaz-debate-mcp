# Runbook: Authentication Failures

**Severity**: Medium to High  
**Alert**: High number of authentication failures detected  
**Automated Response**: Session termination and IP tracking  

---

## 🚨 Alert Triggers

- **User-based**: >5 failures in 15 minutes from single user
- **IP-based**: >10 failures in 15 minutes from single IP
- **Organization-wide**: >3 users with failures in 15 minutes

---

## 🔍 Initial Assessment (5 minutes)

### 1. Check Security Dashboard
```bash
# Access Grafana dashboard
open https://monitoring.yourdomain.com/d/security/authentication

# Check metrics:
- Authentication failure rate
- Affected users count
- Source IP distribution
- Failure reasons breakdown
```

### 2. Query Recent Failures
```bash
# Get last 50 authentication failures
curl -X GET "https://api.yourdomain.com/api/v1/security/audit/events?type=AUTHENTICATION_FAILURE&limit=50" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.'

# Analyze patterns:
- Same user multiple times?
- Same IP multiple times?
- Distributed attack pattern?
```

### 3. Check Active Incidents
```bash
# View security incidents
curl -X GET "https://api.yourdomain.com/api/v1/security/incidents/active" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.'
```

---

## 🎯 Response Actions

### Case 1: Single User Account Attack

**Indicators**: Multiple failures for one user account

#### Immediate Actions:
```bash
# 1. Check if user sessions already terminated (automated)
curl -X GET "https://api.yourdomain.com/api/v1/security/sessions/user/$USER_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# 2. If not, manually terminate all sessions
curl -X POST "https://api.yourdomain.com/api/v1/security/sessions/terminate" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"userId":"$USER_ID","reason":"Authentication attack detected"}'

# 3. Check user account status
curl -X GET "https://api.yourdomain.com/api/v1/users/$USER_ID/status" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

#### Follow-up Actions:
1. **Contact user** through verified channel
2. **Reset password** if compromise suspected
3. **Enable MFA** if not already enabled
4. **Review user's recent activity** for anomalies

### Case 2: IP-Based Attack

**Indicators**: Multiple failures from single IP

#### Immediate Actions:
```bash
# 1. Check if IP already marked as malicious (automated)
curl -X GET "https://api.yourdomain.com/api/v1/security/threat-intel/ip/$IP_ADDRESS" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# 2. Get all activity from this IP
curl -X GET "https://api.yourdomain.com/api/v1/security/audit/events?clientIp=$IP_ADDRESS&limit=100" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# 3. Block IP if not already blocked
curl -X POST "https://api.yourdomain.com/api/v1/security/block-ip" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"ip":"$IP_ADDRESS","duration":"24h","reason":"Brute force attack"}'
```

#### Follow-up Actions:
1. **Check IP reputation** in external threat databases
2. **Update firewall rules** if persistent
3. **Analyze attack patterns** for intelligence
4. **Share threat intel** with security community

### Case 3: Distributed Attack

**Indicators**: Failures from multiple IPs targeting multiple accounts

#### Immediate Actions:
```bash
# 1. Enable enhanced monitoring mode
curl -X POST "https://api.yourdomain.com/api/v1/security/monitoring/enhance" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"duration":"1h","level":"HIGH"}'

# 2. Temporarily increase rate limits
curl -X PUT "https://api.yourdomain.com/api/v1/gateway/rate-limits" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"authentication":{"rate":10,"burst":20}}'

# 3. Enable CAPTCHA for login (if available)
curl -X PUT "https://api.yourdomain.com/api/v1/security/captcha" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"enabled":true,"threshold":2}'
```

---

## 📊 Monitoring During Incident

### Real-time Monitoring
```bash
# Watch authentication metrics
watch -n 5 'curl -s "https://monitoring.yourdomain.com/api/v1/query?query=rate(security_authentication_failure_total[1m])" | jq .'

# Monitor active sessions
watch -n 10 'curl -s "https://api.yourdomain.com/api/v1/security/sessions/stats" -H "Authorization: Bearer $ADMIN_TOKEN" | jq .'
```

### Alert Thresholds
- Normal: <5 failures/minute
- Elevated: 5-20 failures/minute  
- Critical: >20 failures/minute

---

## 📧 Escalation Criteria

### Escalate to Security Team Lead if:
- ✅ >50 authentication failures in 5 minutes
- ✅ Coordinated attack from >10 IPs
- ✅ Targeting admin or privileged accounts
- ✅ Successful breach after failures

### Escalate to CISO if:
- ✅ Evidence of account compromise
- ✅ Insider threat indicators
- ✅ Nation-state attack patterns
- ✅ Impact to >100 users

---

## 📁 Post-Incident Actions

### 1. Document Incident
```bash
# Create incident report
curl -X POST "https://api.yourdomain.com/api/v1/security/incidents/report" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type":"AUTHENTICATION_ATTACK",
    "severity":"MEDIUM",
    "startTime":"$START_TIME",
    "endTime":"$END_TIME",
    "affectedUsers":$USER_COUNT,
    "sourceIPs":["$IP1","$IP2"],
    "actionsTaken":["sessions_terminated","ips_blocked"],
    "outcome":"MITIGATED"
  }'
```

### 2. Update Security Measures
- Review and adjust rate limits
- Update threat intelligence
- Enhance monitoring rules
- Update security training

### 3. Lessons Learned
- Document attack patterns
- Update runbook if needed
- Share with security team
- Update automation rules

---

## 🔧 Useful Commands Reference

```bash
# Get user's recent login attempts
curl "https://api.yourdomain.com/api/v1/users/$USER_ID/login-history?limit=20"

# Check if IP is in allow/block list
curl "https://api.yourdomain.com/api/v1/security/ip-lists/check?ip=$IP_ADDRESS"

# Get organization security status
curl "https://api.yourdomain.com/api/v1/organizations/$ORG_ID/security-status"

# Force password reset for user
curl -X POST "https://api.yourdomain.com/api/v1/users/$USER_ID/force-password-reset"
```

---

**Last Updated**: 2025-07-16  
**Next Review**: Monthly  
**Owner**: Security Operations Team
