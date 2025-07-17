# Runbook: Suspicious Activity Detection

**Severity**: High to Critical  
**Alert**: Suspicious activity patterns detected  
**Automated Response**: Enhanced monitoring and threat intelligence updates  

---

## 🚨 Alert Triggers

- **XSS/Injection Attempts**: Malicious payloads in requests
- **Scanner Detection**: Known security scanner user agents
- **Path Traversal**: Directory traversal attempts
- **Privilege Escalation**: Unauthorized access attempts
- **API Abuse**: Abnormal API usage patterns

---

## 🔍 Initial Assessment (3 minutes)

### 1. Identify Attack Type
```bash
# Check recent security violations
curl -X GET "https://api.yourdomain.com/api/v1/security/violations/recent?limit=20" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '{
    total: .count,
    types: [.violations[].type] | group_by(.) | map({type: .[0], count: length})
  }'

# Common attack types:
# - XSS_ATTEMPT
# - SQL_INJECTION
# - PATH_TRAVERSAL
# - SCANNER_DETECTED
# - PRIVILEGE_ESCALATION
```

### 2. Analyze Attack Source
```bash
# Get attacker details
curl -X GET "https://api.yourdomain.com/api/v1/security/attackers/active" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.attackers[] | {
    ip: .clientIp,
    userAgent: .userAgent,
    requestCount: .totalRequests,
    violationCount: .violations,
    firstSeen: .firstActivity,
    lastSeen: .lastActivity
  }'
```

### 3. Check Impact
```bash
# Assess potential impact
curl -X GET "https://api.yourdomain.com/api/v1/security/impact-assessment" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Key metrics:
# - Affected endpoints
# - Data access attempts
# - Authentication bypass attempts
# - System resource impact
```

---

## 🎯 Response Actions by Attack Type

### Case 1: XSS/Injection Attacks

**Indicators**: Script tags, SQL keywords, command injection patterns

#### Immediate Actions:
```bash
# 1. Block attacking IPs
for ip in $(curl -s "https://api.yourdomain.com/api/v1/security/violations/recent?type=XSS_ATTEMPT" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.violations[].clientIp' | sort -u); do
  curl -X POST "https://api.yourdomain.com/api/v1/security/block-ip" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"ip\":\"$ip\",\"reason\":\"XSS attack attempts\",\"duration\":\"48h\"}"
done

# 2. Enable enhanced input validation
curl -X PUT "https://api.yourdomain.com/api/v1/security/validation" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"level":"PARANOID","logRejected":true}'

# 3. Review targeted endpoints
curl -X GET "https://api.yourdomain.com/api/v1/security/targeted-endpoints?attack=XSS" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Case 2: Security Scanner Detection

**Indicators**: Nikto, SQLMap, Nmap, Burp Suite user agents

#### Immediate Actions:
```bash
# 1. Get scanner details
curl -X GET "https://api.yourdomain.com/api/v1/security/scanners/detected" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.scanners[] | {
    tool: .scannerType,
    ip: .sourceIp,
    startTime: .firstSeen,
    endpointsScanned: .targetedEndpoints | length,
    blocked: .isBlocked
  }'

# 2. Block scanner IPs permanently
curl -X POST "https://api.yourdomain.com/api/v1/security/permanent-block" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"ips":$SCANNER_IPS,"reason":"Security scanner activity"}'

# 3. Enable honey pot responses
curl -X PUT "https://api.yourdomain.com/api/v1/security/honeypot" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"enabled":true,"delayMs":5000}'
```

### Case 3: Privilege Escalation Attempts

**Indicators**: Accessing admin endpoints, role manipulation, JWT tampering

#### CRITICAL - Immediate Actions:
```bash
# 1. EMERGENCY: Terminate all sessions for affected user
USER_ID=$(curl -s "https://api.yourdomain.com/api/v1/security/privilege-escalation/attempts" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.attempts[0].userId')

curl -X POST "https://api.yourdomain.com/api/v1/security/emergency/terminate-user" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"userId\":\"$USER_ID\",\"lockAccount\":true}"

# 2. Audit user's recent activities
curl -X GET "https://api.yourdomain.com/api/v1/security/audit/user/$USER_ID?hours=24" \
  -H "Authorization: Bearer $ADMIN_TOKEN" > user_audit_$USER_ID.json

# 3. Check for lateral movement
curl -X GET "https://api.yourdomain.com/api/v1/security/lateral-movement/check?userId=$USER_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Case 4: API Abuse

**Indicators**: Excessive requests, data scraping, enumeration attempts

#### Immediate Actions:
```bash
# 1. Get abuse patterns
curl -X GET "https://api.yourdomain.com/api/v1/security/api-abuse/analysis" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.patterns[] | {
    endpoint: .endpoint,
    method: .method,
    requestsPerMinute: .rpm,
    uniqueIPs: .ipCount,
    dataVolume: .bytesTransferred
  }'

# 2. Apply dynamic rate limits
curl -X POST "https://api.yourdomain.com/api/v1/security/rate-limits/dynamic" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "rules": [
      {"endpoint":"/api/v1/data/*","limit":10,"window":"1m"},
      {"endpoint":"/api/v1/search","limit":5,"window":"1m"}
    ]
  }'

# 3. Enable API key requirement
curl -X PUT "https://api.yourdomain.com/api/v1/security/api-keys/require" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"endpoints":["/api/v1/data/*","/api/v1/export/*"]}'
```

---

## 📡 Network-Level Response

### For Critical Attacks
```bash
# 1. Update WAF rules (if using cloud WAF)
curl -X POST "https://waf.provider.com/api/rules" \
  -H "Authorization: Bearer $WAF_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "rules": [
      {"pattern":"$ATTACK_PATTERN","action":"BLOCK"},
      {"ip":"$ATTACKER_IP","action":"CHALLENGE"}
    ]
  }'

# 2. Enable DDoS protection
curl -X PUT "https://cdn.provider.com/api/ddos-protection" \
  -H "Authorization: Bearer $CDN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"sensitivity":"HIGH","challengeThreshold":5}'
```

---

## 📊 Real-Time Monitoring

### Attack Dashboard
```bash
# Monitor attack metrics
watch -n 2 '
curl -s "https://api.yourdomain.com/api/v1/security/realtime/stats" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq "{
    activeAttackers: .attackers.active,
    blockedIPs: .blocked.total,
    requestsPerSecond: .traffic.rps,
    violationsPerMinute: .violations.rpm,
    topAttackTypes: .attacks.byType | to_entries | sort_by(-.value) | .[0:3]
  }"'
```

### System Health
```bash
# Monitor system impact
watch -n 5 '
curl -s "https://api.yourdomain.com/api/v1/system/health" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq "{
    cpu: .resources.cpu,
    memory: .resources.memory,
    responseTime: .performance.avgResponseMs,
    errorRate: .errors.rate,
    queueDepth: .queues.depth
  }"'
```

---

## 📧 Escalation Matrix

| Attack Type | Auto-Response | L1 Response | L2 Escalation | L3 Escalation |
|-------------|---------------|-------------|----------------|----------------|
| **XSS/Injection** | Block IP | Analyze patterns | Persistent attacks | Data breach risk |
| **Scanner** | Honeypot | Permanent block | Targeted scan | APT indicators |
| **Privilege Escalation** | Session kill | Account lock | Review all access | Insider threat |
| **API Abuse** | Rate limit | API key enforce | Data exfiltration | Business impact |

---

## 📁 Post-Incident Procedures

### 1. Attack Analysis
```bash
# Generate attack report
curl -X POST "https://api.yourdomain.com/api/v1/security/reports/attack-analysis" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "startTime":"$INCIDENT_START",
    "endTime":"$INCIDENT_END",
    "includeLogs":true,
    "includeMetrics":true
  }' > attack_report_$(date +%Y%m%d_%H%M%S).json
```

### 2. Security Improvements
- Update WAF rules based on attack patterns
- Enhance input validation rules
- Update threat intelligence
- Review and patch vulnerable endpoints
- Update security training materials

### 3. Communication
- Notify affected users (if any)
- Update security advisory
- Share threat intel with partners
- Document lessons learned

---

## 🛠️ Quick Reference

### Block Lists Management
```bash
# Add IP to permanent block list
curl -X POST "https://api.yourdomain.com/api/v1/security/blocklist/add" \
  -d '{"ip":"$IP","reason":"$REASON"}'

# Remove IP from block list
curl -X DELETE "https://api.yourdomain.com/api/v1/security/blocklist/$IP"

# View current block list
curl "https://api.yourdomain.com/api/v1/security/blocklist"
```

### Emergency Controls
```bash
# Enable read-only mode
curl -X PUT "https://api.yourdomain.com/api/v1/system/read-only" \
  -d '{"enabled":true}'

# Disable all external integrations
curl -X PUT "https://api.yourdomain.com/api/v1/integrations/disable-all"

# Force re-authentication for all users
curl -X POST "https://api.yourdomain.com/api/v1/security/force-reauth"
```

---

**Last Updated**: 2025-07-16  
**Next Review**: Monthly  
**Owner**: Security Operations Team  
**Escalation**: security-oncall@yourdomain.com
