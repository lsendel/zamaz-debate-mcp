# Runbook: DDoS Attack Mitigation

**Severity**: High to Critical  
**Alert**: Distributed Denial of Service attack detected  
**Automated Response**: Rate limiting and traffic filtering activated  

---

## 🚨 Alert Triggers

- **Traffic Spike**: >10x normal request volume
- **Resource Exhaustion**: CPU/Memory >90% sustained
- **Connection Flood**: >1000 connections/second
- **Pattern Detection**: Known DDoS signatures
- **Geographic Anomaly**: Traffic from unusual regions

---

## 🔍 Initial Assessment (2 minutes)

### 1. Identify Attack Type
```bash
# Check current traffic patterns
curl -X GET "https://api.yourdomain.com/api/v1/security/traffic/analysis" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '{
    type: .attackType,
    requestsPerSecond: .metrics.rps,
    uniqueIPs: .metrics.uniqueSources,
    topSources: .sources[0:5],
    targetEndpoints: .targets[0:5]
  }'

# Common DDoS types:
# - Volume-based: UDP flood, ICMP flood, amplification
# - Protocol: SYN flood, ACK flood, fragmentation
# - Application: HTTP flood, Slowloris, DNS query flood
```

### 2. Measure Impact
```bash
# System health check
curl -X GET "https://api.yourdomain.com/api/v1/system/health/detailed" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '{
    availability: .services | map({name: .name, status: .status}),
    performance: {
      responseTime: .metrics.avgResponseMs,
      errorRate: .metrics.errorRate,
      queueDepth: .metrics.queueDepth
    },
    resources: {
      cpu: .resources.cpu,
      memory: .resources.memory,
      connections: .resources.connections
    }
  }'
```

---

## 🛡️ Immediate Mitigation

### Step 1: Enable DDoS Protection (Automated)
```bash
# Verify auto-mitigation is active
curl -X GET "https://api.yourdomain.com/api/v1/security/ddos/status" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# If not active, manually enable
curl -X POST "https://api.yourdomain.com/api/v1/security/ddos/enable" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "mode": "AGGRESSIVE",
    "autoScale": true,
    "preserveLegitimate": true
  }'
```

### Step 2: Cloud Provider Mitigation
```bash
# Enable CDN DDoS protection (CloudFlare example)
curl -X PUT "https://api.cloudflare.com/client/v4/zones/$ZONE_ID/settings/security_level" \
  -H "Authorization: Bearer $CF_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"value":"under_attack"}'

# Enable rate limiting
curl -X POST "https://api.cloudflare.com/client/v4/zones/$ZONE_ID/rate_limits" \
  -H "Authorization: Bearer $CF_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "threshold": 50,
    "period": 60,
    "action": {"mode": "challenge"}
  }'

# AWS Shield (if using AWS)
aws shield associate-drt-role --role-arn $DRT_ROLE_ARN
aws shield associate-drt-log-bucket --log-bucket $LOG_BUCKET
```

### Step 3: Application-Level Protection
```bash
# Tighten rate limits
curl -X PUT "https://api.yourdomain.com/api/v1/gateway/rate-limits/emergency" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "global": {"rate": 100, "burst": 200},
    "perIP": {"rate": 10, "burst": 20},
    "authenticated": {"rate": 50, "burst": 100}
  }'

# Enable CAPTCHA challenges
curl -X PUT "https://api.yourdomain.com/api/v1/security/captcha/emergency" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"enabled": true, "threshold": 3, "difficulty": "hard"}'
```

---

## 🎯 Attack-Specific Responses

### Volume-Based Attack (Layer 3/4)
```bash
# 1. Enable SYN cookies
echo 1 > /proc/sys/net/ipv4/tcp_syncookies

# 2. Increase connection limits
echo 65535 > /proc/sys/net/core/somaxconn
echo 65535 > /proc/sys/net/ipv4/tcp_max_syn_backlog

# 3. Drop invalid packets
iptables -t mangle -A PREROUTING -m conntrack --ctstate INVALID -j DROP
iptables -t mangle -A PREROUTING -p tcp ! --syn -m conntrack --ctstate NEW -j DROP

# 4. Rate limit connections
iptables -A INPUT -p tcp --dport 443 -m connlimit --connlimit-above 20 -j REJECT
```

### Application Layer Attack (Layer 7)
```bash
# 1. Identify attack patterns
curl -X GET "https://api.yourdomain.com/api/v1/security/ddos/patterns" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.patterns[] | {
    pattern: .signature,
    frequency: .count,
    sources: .topSources[0:3]
  }'

# 2. Deploy pattern-based blocking
curl -X POST "https://api.yourdomain.com/api/v1/security/waf/rules" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "rules": [
      {"pattern": "$ATTACK_PATTERN", "action": "BLOCK"},
      {"userAgent": "$BOT_UA", "action": "BLOCK"},
      {"geoip": ["XX", "YY"], "action": "CHALLENGE"}
    ]
  }'

# 3. Cache static content aggressively
curl -X PUT "https://api.yourdomain.com/api/v1/cache/emergency" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"mode": "AGGRESSIVE", "ttl": 3600}'
```

### Amplification Attack
```bash
# 1. Block amplification vectors
iptables -A INPUT -p udp --dport 53 -j DROP  # DNS
iptables -A INPUT -p udp --dport 123 -j DROP # NTP
iptables -A INPUT -p udp --dport 161 -j DROP # SNMP
iptables -A INPUT -p udp --dport 389 -j DROP # LDAP

# 2. Enable anti-spoofing
echo 1 > /proc/sys/net/ipv4/conf/all/rp_filter
echo 1 > /proc/sys/net/ipv4/conf/default/rp_filter
```

---

## 📊 Real-Time Monitoring

### Attack Dashboard
```bash
# Monitor attack metrics
watch -n 1 'curl -s "https://api.yourdomain.com/api/v1/security/ddos/realtime" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq "{
    status: .attack.active,
    duration: .attack.durationMinutes,
    intensity: .metrics.requestsPerSecond,
    blocked: .metrics.blockedRequests,
    legitimate: .metrics.legitimateRequests,
    topAttackers: .attackers[0:5] | map({ip: .ip, requests: .count})
  }"'
```

### Resource Monitoring
```bash
# Watch system resources
watch -n 2 'echo "=== SYSTEM RESOURCES ===" && \
  top -bn1 | head -5 && \
  echo -e "\n=== NETWORK STATS ===" && \
  netstat -an | grep -c ESTABLISHED && \
  echo -e "\n=== BANDWIDTH ===" && \
  iftop -t -s 2 2>/dev/null | grep "Total send and receive rate"'
```

---

## 🔄 Scaling Response

### Auto-Scaling Triggers
```bash
# 1. Increase compute resources
curl -X POST "https://api.yourdomain.com/api/v1/infrastructure/scale" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "service": "api-gateway",
    "action": "SCALE_UP",
    "target": {"min": 10, "max": 50},
    "reason": "DDoS attack mitigation"
  }'

# 2. Add more edge locations
curl -X POST "https://cdn.provider.com/api/pop/activate" \
  -H "Authorization: Bearer $CDN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"regions": ["us-west", "eu-central", "asia-pacific"]}'
```

### Geographic Filtering
```bash
# Block high-risk countries during attack
curl -X POST "https://api.yourdomain.com/api/v1/security/geoblock" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "action": "BLOCK",
    "countries": ["XX", "YY", "ZZ"],
    "duration": "1h",
    "allowlist": ["US", "GB", "CA", "AU"]
  }'
```

---

## 📈 Escalation Matrix

| Duration | Impact | Escalation Level | Actions |
|----------|--------|------------------|---------|
| <5 min | Low | Automated Only | Monitor, rate limit |
| 5-15 min | Medium | L1 Engineer | Manual verification, adjust limits |
| 15-30 min | High | L2 Engineer + Lead | Scale infrastructure, CDN changes |
| >30 min | Critical | All Hands + Vendor | ISP coordination, emergency response |

### Vendor Escalation
```bash
# CDN Provider
Support Hotline: +1-XXX-XXX-XXXX
Emergency Email: ddos-emergency@cdn-provider.com

# ISP Contact
NOC: +1-XXX-XXX-XXXX
BGP Team: bgp-team@isp.com

# DDoS Mitigation Service
24/7 SOC: +1-XXX-XXX-XXXX
Escalation: escalation@ddos-provider.com
```

---

## 🛠️ Mitigation Toolkit

### Quick Commands
```bash
# View top attackers
netstat -an | awk '{print $5}' | cut -d: -f1 | sort | uniq -c | sort -nr | head -20

# Block IP range
iptables -A INPUT -s 192.168.0.0/16 -j DROP

# Clear connection tracking
conntrack -F

# Emergency cache clear (if cache poisoning suspected)
redis-cli FLUSHALL

# Null route attack source
ip route add blackhole 192.168.0.0/24

# Enable TCP BBR congestion control
echo "net.core.default_qdisc=fq" >> /etc/sysctl.conf
echo "net.ipv4.tcp_congestion_control=bbr" >> /etc/sysctl.conf
sysctl -p
```

### Attack Analysis
```bash
# Analyze attack patterns
tcpdump -nn -c 1000 -i eth0 'tcp[tcpflags] & tcp-syn != 0' | \
  awk '{print $3}' | cut -d. -f1-4 | sort | uniq -c | sort -nr

# Check for slowloris
ss -tn state time-wait | wc -l

# Identify attack vectors
tshark -r attack.pcap -T fields -e ip.src -e ip.dst -e tcp.dstport | \
  sort | uniq -c | sort -nr
```

---

## 📋 Post-Attack Procedures

### 1. Attack Analysis Report
```bash
# Generate comprehensive report
curl -X POST "https://api.yourdomain.com/api/v1/security/ddos/report" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "attackId": "$ATTACK_ID",
    "includeMetrics": true,
    "includePatterns": true,
    "includeMitigation": true
  }' > ddos_report_$(date +%Y%m%d_%H%M%S).json
```

### 2. Cost Impact Assessment
- Infrastructure scaling costs
- Bandwidth overage charges
- Service credits owed
- Mitigation service fees
- Staff overtime costs

### 3. Improvement Actions
- Update DDoS playbook
- Enhance monitoring
- Upgrade infrastructure
- Review SLAs
- Conduct team training

---

## 🔄 Recovery Checklist

```bash
□ Attack traffic subsided (<5% of peak)
□ Remove emergency blocks
□ Restore normal rate limits
□ Scale down infrastructure
□ Clear temporary caches
□ Review and unblock legitimate IPs
□ Update security rules
□ Document lessons learned
□ Schedule post-mortem meeting
□ Update runbook based on experience
```

---

## 💡 Prevention Measures

### Infrastructure Hardening
- Implement anycast networking
- Deploy multiple edge locations
- Use hardware-based filtering
- Implement scrubbing centers
- Regular capacity planning

### Application Hardening
- Optimize code efficiency
- Implement caching layers
- Use asynchronous processing
- Minimize resource consumption
- Regular load testing

---

**Last Updated**: 2025-07-16  
**Next Review**: Quarterly  
**Owner**: Network Security Team  
**Escalation**: noc@yourdomain.com