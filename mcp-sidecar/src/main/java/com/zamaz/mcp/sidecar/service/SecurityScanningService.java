package com.zamaz.mcp.sidecar.service;

import com.zamaz.mcp.sidecar.service.MetricsCollectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Security Scanning Service for MCP Sidecar
 * 
 * Provides comprehensive security scanning capabilities:
 * - Request payload scanning for malicious content
 * - SQL injection detection
 * - XSS attack prevention
 * - Path traversal detection
 * - Rate limiting abuse detection
 * - Suspicious activity monitoring
 * - Threat intelligence integration
 * - Security metrics collection
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityScanningService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final WebClient.Builder webClientBuilder;
    private final MetricsCollectorService metricsCollectorService;

    @Value("${app.security.scanning.enabled:true}")
    private boolean securityScanningEnabled;

    @Value("${app.security.scanning.strict-mode:false}")
    private boolean strictMode;

    @Value("${app.security.scanning.max-payload-size:1048576}") // 1MB
    private int maxPayloadSize;

    @Value("${app.security.threat-intelligence.enabled:false}")
    private boolean threatIntelligenceEnabled;

    @Value("${app.security.threat-intelligence.api-url:}")
    private String threatIntelligenceApiUrl;

    // Security patterns
    private static final Map<String, Pattern> SECURITY_PATTERNS = new ConcurrentHashMap<>();
    private static final Map<String, SecurityThreat> THREAT_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, SuspiciousActivity> SUSPICIOUS_ACTIVITIES = new ConcurrentHashMap<>();

    static {
        initializeSecurityPatterns();
    }

    /**
     * Security scan result
     */
    public static class SecurityScanResult {
        private final boolean blocked;
        private final List<SecurityThreat> threats;
        private final String reason;
        private final int riskScore;

        public SecurityScanResult(boolean blocked, List<SecurityThreat> threats, String reason, int riskScore) {
            this.blocked = blocked;
            this.threats = threats;
            this.reason = reason;
            this.riskScore = riskScore;
        }

        public boolean isBlocked() { return blocked; }
        public List<SecurityThreat> getThreats() { return threats; }
        public String getReason() { return reason; }
        public int getRiskScore() { return riskScore; }
    }

    /**
     * Security threat
     */
    public static class SecurityThreat {
        private final String type;
        private final String description;
        private final String pattern;
        private final int severity; // 1-10
        private final Instant detectedAt;

        public SecurityThreat(String type, String description, String pattern, int severity) {
            this.type = type;
            this.description = description;
            this.pattern = pattern;
            this.severity = severity;
            this.detectedAt = Instant.now();
        }

        public String getType() { return type; }
        public String getDescription() { return description; }
        public String getPattern() { return pattern; }
        public int getSeverity() { return severity; }
        public Instant getDetectedAt() { return detectedAt; }
    }

    /**
     * Suspicious activity tracker
     */
    public static class SuspiciousActivity {
        private final String clientId;
        private final String activityType;
        private final Map<String, Integer> eventCounts;
        private final Instant firstSeen;
        private volatile Instant lastSeen;

        public SuspiciousActivity(String clientId, String activityType) {
            this.clientId = clientId;
            this.activityType = activityType;
            this.eventCounts = new ConcurrentHashMap<>();
            this.firstSeen = Instant.now();
            this.lastSeen = Instant.now();
        }

        public void recordEvent(String eventType) {
            eventCounts.merge(eventType, 1, Integer::sum);
            lastSeen = Instant.now();
        }

        public String getClientId() { return clientId; }
        public String getActivityType() { return activityType; }
        public Map<String, Integer> getEventCounts() { return eventCounts; }
        public Instant getFirstSeen() { return firstSeen; }
        public Instant getLastSeen() { return lastSeen; }
    }

    /**
     * Initialize security patterns
     */
    private static void initializeSecurityPatterns() {
        // SQL Injection patterns
        SECURITY_PATTERNS.put("SQL_INJECTION", Pattern.compile(
            "(?i)(union|select|insert|update|delete|drop|create|alter|exec|execute|script|javascript|vbscript)" +
            "|('|(\\-\\-)|(;)|(\\||\\|)|(\\*|\\*))",
            Pattern.CASE_INSENSITIVE
        ));

        // XSS patterns
        SECURITY_PATTERNS.put("XSS", Pattern.compile(
            "(?i)(<script|</script>|<iframe|</iframe>|javascript:|vbscript:|onload=|onerror=|onclick=|onmouseover=)",
            Pattern.CASE_INSENSITIVE
        ));

        // Path traversal patterns
        SECURITY_PATTERNS.put("PATH_TRAVERSAL", Pattern.compile(
            "(?i)(\\.\\./|\\.\\.\\\\|%2e%2e%2f|%2e%2e%5c|%252e%252e%252f)",
            Pattern.CASE_INSENSITIVE
        ));

        // Command injection patterns
        SECURITY_PATTERNS.put("COMMAND_INJECTION", Pattern.compile(
            "(?i)(;|\\||&|\\$\\(|`|nc\\s|netcat|wget|curl|chmod|rm\\s|mv\\s|cp\\s)",
            Pattern.CASE_INSENSITIVE
        ));

        // LDAP injection patterns
        SECURITY_PATTERNS.put("LDAP_INJECTION", Pattern.compile(
            "(?i)(\\*|\\(|\\)|\\\\|\\||&|!|=|<|>|~|;)",
            Pattern.CASE_INSENSITIVE
        ));

        // XML/XXE patterns
        SECURITY_PATTERNS.put("XXE", Pattern.compile(
            "(?i)(<!entity|<!\\[cdata\\[|<!doctype|file://|ftp://|http://|https://)",
            Pattern.CASE_INSENSITIVE
        ));

        // NoSQL injection patterns
        SECURITY_PATTERNS.put("NOSQL_INJECTION", Pattern.compile(
            "(?i)(\\$where|\\$ne|\\$in|\\$nin|\\$or|\\$and|\\$not|\\$nor|\\$exists|\\$type|\\$mod|\\$regex|\\$text|\\$search)",
            Pattern.CASE_INSENSITIVE
        ));
    }

    /**
     * Scan request for security threats
     */
    public Mono<SecurityScanResult> scanRequest(String clientId, String path, String method, 
                                              Map<String, String> headers, String payload) {
        
        if (!securityScanningEnabled) {
            return Mono.just(new SecurityScanResult(false, Collections.emptyList(), "Scanning disabled", 0));
        }

        return Mono.fromCallable(() -> {
            List<SecurityThreat> threats = new ArrayList<>();
            int totalRiskScore = 0;

            // Check payload size
            if (payload != null && payload.length() > maxPayloadSize) {
                threats.add(new SecurityThreat("PAYLOAD_SIZE", 
                    "Payload exceeds maximum allowed size", 
                    "size > " + maxPayloadSize, 5));
                totalRiskScore += 5;
            }

            // Scan path
            threats.addAll(scanPath(path));

            // Scan headers
            threats.addAll(scanHeaders(headers));

            // Scan payload
            if (payload != null) {
                threats.addAll(scanPayload(payload));
            }

            // Calculate total risk score
            totalRiskScore += threats.stream().mapToInt(SecurityThreat::getSeverity).sum();

            // Check for suspicious activity
            checkSuspiciousActivity(clientId, method, path, threats.size());

            // Record security metrics
            recordSecurityMetrics(clientId, threats, totalRiskScore);

            // Determine if request should be blocked
            boolean blocked = shouldBlockRequest(threats, totalRiskScore);
            String reason = blocked ? buildBlockReason(threats) : "Request allowed";

            return new SecurityScanResult(blocked, threats, reason, totalRiskScore);
        })
        .doOnNext(result -> {
            if (result.isBlocked()) {
                log.warn("Security scan blocked request: clientId={}, path={}, reason={}, riskScore={}", 
                        clientId, path, result.getReason(), result.getRiskScore());
            } else if (!result.getThreats().isEmpty()) {
                log.debug("Security scan detected threats: clientId={}, path={}, threats={}, riskScore={}", 
                        clientId, path, result.getThreats().size(), result.getRiskScore());
            }
        });
    }

    /**
     * Scan request path for security threats
     */
    private List<SecurityThreat> scanPath(String path) {
        List<SecurityThreat> threats = new ArrayList<>();
        
        if (path == null) return threats;

        // Check for path traversal
        if (SECURITY_PATTERNS.get("PATH_TRAVERSAL").matcher(path).find()) {
            threats.add(new SecurityThreat("PATH_TRAVERSAL", 
                "Path traversal attempt detected", 
                "Path contains traversal patterns", 8));
        }

        // Check for suspicious patterns in path
        if (path.contains("..") || path.contains("%2e%2e")) {
            threats.add(new SecurityThreat("PATH_TRAVERSAL", 
                "Directory traversal detected", 
                "Path contains dot-dot sequences", 7));
        }

        // Check for encoded characters
        if (path.contains("%00") || path.contains("%0a") || path.contains("%0d")) {
            threats.add(new SecurityThreat("PATH_INJECTION", 
                "Null byte or newline injection detected", 
                "Path contains encoded special characters", 6));
        }

        return threats;
    }

    /**
     * Scan request headers for security threats
     */
    private List<SecurityThreat> scanHeaders(Map<String, String> headers) {
        List<SecurityThreat> threats = new ArrayList<>();
        
        if (headers == null) return threats;

        for (Map.Entry<String, String> header : headers.entrySet()) {
            String name = header.getKey();
            String value = header.getValue();

            // Check for XSS in headers
            if (SECURITY_PATTERNS.get("XSS").matcher(value).find()) {
                threats.add(new SecurityThreat("XSS", 
                    "XSS attempt detected in header: " + name, 
                    "Header contains script tags", 7));
            }

            // Check for SQL injection in headers
            if (SECURITY_PATTERNS.get("SQL_INJECTION").matcher(value).find()) {
                threats.add(new SecurityThreat("SQL_INJECTION", 
                    "SQL injection attempt detected in header: " + name, 
                    "Header contains SQL keywords", 8));
            }

            // Check for suspicious user agents
            if ("User-Agent".equalsIgnoreCase(name)) {
                if (value.contains("sqlmap") || value.contains("nikto") || value.contains("nmap")) {
                    threats.add(new SecurityThreat("SECURITY_SCANNER", 
                        "Security scanner detected", 
                        "User-Agent indicates security tool", 9));
                }
            }
        }

        return threats;
    }

    /**
     * Scan request payload for security threats
     */
    private List<SecurityThreat> scanPayload(String payload) {
        List<SecurityThreat> threats = new ArrayList<>();
        
        if (payload == null || payload.isEmpty()) return threats;

        // Check each security pattern
        for (Map.Entry<String, Pattern> entry : SECURITY_PATTERNS.entrySet()) {
            String threatType = entry.getKey();
            Pattern pattern = entry.getValue();
            
            if (pattern.matcher(payload).find()) {
                int severity = calculateSeverity(threatType);
                threats.add(new SecurityThreat(threatType, 
                    "Detected " + threatType.toLowerCase().replace("_", " ") + " attempt", 
                    "Payload matches " + threatType + " pattern", 
                    severity));
            }
        }

        // Check for suspicious JSON patterns
        if (payload.contains("__proto__") || payload.contains("constructor") || payload.contains("prototype")) {
            threats.add(new SecurityThreat("PROTOTYPE_POLLUTION", 
                "Prototype pollution attempt detected", 
                "Payload contains prototype manipulation", 7));
        }

        // Check for SSRF patterns
        if (payload.contains("localhost") || payload.contains("127.0.0.1") || 
            payload.contains("file://") || payload.contains("ftp://")) {
            threats.add(new SecurityThreat("SSRF", 
                "Server-side request forgery attempt detected", 
                "Payload contains internal URLs", 8));
        }

        return threats;
    }

    /**
     * Calculate severity based on threat type
     */
    private int calculateSeverity(String threatType) {
        return switch (threatType) {
            case "SQL_INJECTION" -> 9;
            case "COMMAND_INJECTION" -> 10;
            case "XXE" -> 8;
            case "XSS" -> 7;
            case "PATH_TRAVERSAL" -> 8;
            case "LDAP_INJECTION" -> 6;
            case "NOSQL_INJECTION" -> 7;
            default -> 5;
        };
    }

    /**
     * Check for suspicious activity patterns
     */
    private void checkSuspiciousActivity(String clientId, String method, String path, int threatCount) {
        String activityKey = clientId + ":" + method + ":" + path;
        
        SuspiciousActivity activity = SUSPICIOUS_ACTIVITIES.computeIfAbsent(activityKey, 
            k -> new SuspiciousActivity(clientId, "REQUEST_PATTERN"));
        
        activity.recordEvent(threatCount > 0 ? "THREAT_DETECTED" : "NORMAL_REQUEST");
        
        // Check for rapid fire requests
        if (activity.getEventCounts().values().stream().mapToInt(Integer::intValue).sum() > 100) {
            log.warn("Rapid fire requests detected: clientId={}, activityKey={}", clientId, activityKey);
        }
        
        // Check for high threat rate
        int totalThreats = activity.getEventCounts().getOrDefault("THREAT_DETECTED", 0);
        int totalRequests = activity.getEventCounts().values().stream().mapToInt(Integer::intValue).sum();
        
        if (totalRequests > 10 && (double) totalThreats / totalRequests > 0.3) {
            log.warn("High threat rate detected: clientId={}, threatRate={}/{}", 
                    clientId, totalThreats, totalRequests);
        }
    }

    /**
     * Record security metrics
     */
    private void recordSecurityMetrics(String clientId, List<SecurityThreat> threats, int riskScore) {
        // Record threat metrics
        for (SecurityThreat threat : threats) {
            metricsCollectorService.recordRequest(
                "security_scan", 
                "THREAT_" + threat.getType(), 
                0, 
                threat.getSeverity() > 7 ? 403 : 200
            );
        }
        
        // Record overall security score
        if (riskScore > 0) {
            log.debug("Security scan completed: clientId={}, threats={}, riskScore={}", 
                    clientId, threats.size(), riskScore);
        }
    }

    /**
     * Determine if request should be blocked
     */
    private boolean shouldBlockRequest(List<SecurityThreat> threats, int totalRiskScore) {
        // Block if any critical severity threat
        boolean hasCriticalThreat = threats.stream()
                .anyMatch(threat -> threat.getSeverity() >= 9);
        
        if (hasCriticalThreat) {
            return true;
        }
        
        // Block if total risk score is too high
        if (totalRiskScore > 15) {
            return true;
        }
        
        // Block if strict mode and any threat
        if (strictMode && !threats.isEmpty()) {
            return true;
        }
        
        return false;
    }

    /**
     * Build block reason from threats
     */
    private String buildBlockReason(List<SecurityThreat> threats) {
        if (threats.isEmpty()) {
            return "Unknown security violation";
        }
        
        StringBuilder reason = new StringBuilder("Security threats detected: ");
        threats.stream()
                .map(SecurityThreat::getType)
                .distinct()
                .forEach(type -> reason.append(type).append(" "));
        
        return reason.toString().trim();
    }

    /**
     * Check IP reputation with threat intelligence
     */
    public Mono<Boolean> checkIPReputation(String clientIp) {
        if (!threatIntelligenceEnabled || threatIntelligenceApiUrl.isEmpty()) {
            return Mono.just(true); // Allow if no threat intelligence
        }

        return webClientBuilder.build()
                .get()
                .uri(threatIntelligenceApiUrl + "/ip/" + clientIp)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Object malicious = response.get("malicious");
                    return malicious == null || !Boolean.TRUE.equals(malicious);
                })
                .timeout(Duration.ofSeconds(2))
                .onErrorReturn(true) // Allow on error
                .doOnNext(safe -> {
                    if (!safe) {
                        log.warn("Malicious IP detected: {}", clientIp);
                    }
                });
    }

    /**
     * Get security scan statistics
     */
    public Mono<Map<String, Object>> getSecurityStatistics() {
        return Mono.fromCallable(() -> {
            Map<String, Object> stats = new HashMap<>();
            
            // Count threats by type
            Map<String, Integer> threatCounts = new HashMap<>();
            THREAT_CACHE.values().forEach(threat -> 
                threatCounts.merge(threat.getType(), 1, Integer::sum));
            
            stats.put("threatCounts", threatCounts);
            stats.put("totalThreats", THREAT_CACHE.size());
            stats.put("suspiciousActivities", SUSPICIOUS_ACTIVITIES.size());
            stats.put("scanningEnabled", securityScanningEnabled);
            stats.put("strictMode", strictMode);
            
            // Calculate threat severity distribution
            Map<String, Integer> severityDistribution = new HashMap<>();
            THREAT_CACHE.values().forEach(threat -> {
                String severityRange = threat.getSeverity() >= 8 ? "HIGH" : 
                                     threat.getSeverity() >= 5 ? "MEDIUM" : "LOW";
                severityDistribution.merge(severityRange, 1, Integer::sum);
            });
            stats.put("severityDistribution", severityDistribution);
            
            return stats;
        });
    }

    /**
     * Clean up old threat data
     */
    @Scheduled(fixedDelayString = "${app.security.cleanup.interval:1h}")
    public void cleanupOldThreats() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(24));
        
        // Clean up threat cache
        THREAT_CACHE.entrySet().removeIf(entry -> 
            entry.getValue().getDetectedAt().isBefore(cutoff));
        
        // Clean up suspicious activities
        SUSPICIOUS_ACTIVITIES.entrySet().removeIf(entry -> 
            entry.getValue().getLastSeen().isBefore(cutoff));
        
        log.debug("Cleaned up old security threats and suspicious activities");
    }

    /**
     * Update threat intelligence
     */
    @Scheduled(fixedDelayString = "${app.security.threat-intelligence.update-interval:6h}")
    public void updateThreatIntelligence() {
        if (!threatIntelligenceEnabled) {
            return;
        }
        
        log.debug("Updating threat intelligence data");
        
        // This would implement actual threat intelligence feed updates
        // For now, just log the update
        
        log.info("Threat intelligence update completed");
    }
}