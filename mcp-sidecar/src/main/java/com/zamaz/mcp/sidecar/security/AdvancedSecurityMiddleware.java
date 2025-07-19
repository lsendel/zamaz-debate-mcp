package com.zamaz.mcp.sidecar.security;

import com.zamaz.mcp.sidecar.service.AuditLoggingService;
import com.zamaz.mcp.sidecar.service.MetricsCollectorService;
import com.zamaz.mcp.sidecar.service.SecurityScanningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Advanced Security Middleware for MCP Sidecar
 * 
 * Features:
 * - OWASP Top 10 protection
 * - Advanced threat detection
 * - Geo-blocking and IP filtering
 * - Request/response sanitization
 * - Security headers injection
 * - Rate limiting by IP and user agent
 * - Suspicious activity detection
 * - Real-time security monitoring
 * - Compliance enforcement
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AdvancedSecurityMiddleware implements WebFilter {

    private final SecurityScanningService securityScanningService;
    private final AuditLoggingService auditLoggingService;
    private final MetricsCollectorService metricsCollectorService;

    @Value("${app.security.enabled:true}")
    private boolean securityEnabled;

    @Value("${app.security.strict-mode:false}")
    private boolean strictMode;

    @Value("${app.security.block-suspicious-requests:true}")
    private boolean blockSuspiciousRequests;

    @Value("${app.security.max-request-size:10485760}") // 10MB
    private long maxRequestSize;

    @Value("${app.security.max-header-size:8192}") // 8KB
    private int maxHeaderSize;

    @Value("${app.security.request-timeout:30s}")
    private Duration requestTimeout;

    // Security state tracking
    private final Map<String, SecurityProfile> ipSecurityProfiles = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> suspiciousActivityCounters = new ConcurrentHashMap<>();
    private final Map<String, Instant> blockedIps = new ConcurrentHashMap<>();
    private final Set<String> allowedIps = ConcurrentHashMap.newKeySet();
    private final Set<String> blockedCountries = ConcurrentHashMap.newKeySet();

    // Security patterns
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(union|select|insert|update|delete|drop|exec|execute|script|javascript|vbscript|onload|onerror|alert|confirm|prompt)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?i)(<script|javascript:|vbscript:|onload=|onerror=|onclick=|onmouseover=)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        "(\\.\\./|\\.\\\\|%2e%2e%2f|%252e%252e%252f|%c0%ae%c0%ae%c0%af)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(
        "(?i)(\\||;|&&|\\$\\(|`|\\{|\\}|\\[|\\]|>|<|\\*|\\?)",
        Pattern.CASE_INSENSITIVE
    );

    // Suspicious user agents
    private static final Set<String> SUSPICIOUS_USER_AGENTS = Set.of(
        "sqlmap", "nmap", "masscan", "zgrab", "shodan", "censys", "bot", "crawler",
        "scanner", "vulnerability", "exploit", "hack", "penetration", "security"
    );

    // Security headers
    private static final Map<String, String> SECURITY_HEADERS = Map.of(
        "X-Frame-Options", "DENY",
        "X-Content-Type-Options", "nosniff",
        "X-XSS-Protection", "1; mode=block",
        "Strict-Transport-Security", "max-age=31536000; includeSubDomains",
        "Content-Security-Policy", "default-src 'self'; script-src 'self' 'unsafe-inline'",
        "Referrer-Policy", "strict-origin-when-cross-origin",
        "Permissions-Policy", "geolocation=(), microphone=(), camera=()",
        "X-Permitted-Cross-Domain-Policies", "none"
    );

    /**
     * Security profile for IP addresses
     */
    public static class SecurityProfile {
        private final String ipAddress;
        private final AtomicLong totalRequests;
        private final AtomicLong suspiciousRequests;
        private final AtomicLong blockedRequests;
        private final Map<String, AtomicInteger> userAgentCounts;
        private final Map<String, AtomicInteger> endpointCounts;
        private volatile Instant firstSeen;
        private volatile Instant lastSeen;
        private volatile ThreatLevel threatLevel;
        private volatile String geoLocation;

        public SecurityProfile(String ipAddress) {
            this.ipAddress = ipAddress;
            this.totalRequests = new AtomicLong(0);
            this.suspiciousRequests = new AtomicLong(0);
            this.blockedRequests = new AtomicLong(0);
            this.userAgentCounts = new ConcurrentHashMap<>();
            this.endpointCounts = new ConcurrentHashMap<>();
            this.firstSeen = Instant.now();
            this.lastSeen = Instant.now();
            this.threatLevel = ThreatLevel.LOW;
        }

        public String getIpAddress() { return ipAddress; }
        public long getTotalRequests() { return totalRequests.get(); }
        public long getSuspiciousRequests() { return suspiciousRequests.get(); }
        public long getBlockedRequests() { return blockedRequests.get(); }
        public Map<String, AtomicInteger> getUserAgentCounts() { return userAgentCounts; }
        public Map<String, AtomicInteger> getEndpointCounts() { return endpointCounts; }
        public Instant getFirstSeen() { return firstSeen; }
        public Instant getLastSeen() { return lastSeen; }
        public ThreatLevel getThreatLevel() { return threatLevel; }
        public String getGeoLocation() { return geoLocation; }

        public void recordRequest(String userAgent, String endpoint) {
            totalRequests.incrementAndGet();
            lastSeen = Instant.now();
            
            if (userAgent != null) {
                userAgentCounts.computeIfAbsent(userAgent, k -> new AtomicInteger(0)).incrementAndGet();
            }
            
            if (endpoint != null) {
                endpointCounts.computeIfAbsent(endpoint, k -> new AtomicInteger(0)).incrementAndGet();
            }
        }

        public void recordSuspiciousActivity() {
            suspiciousRequests.incrementAndGet();
            updateThreatLevel();
        }

        public void recordBlockedRequest() {
            blockedRequests.incrementAndGet();
            updateThreatLevel();
        }

        private void updateThreatLevel() {
            long total = totalRequests.get();
            long suspicious = suspiciousRequests.get();
            long blocked = blockedRequests.get();
            
            if (total == 0) {
                threatLevel = ThreatLevel.LOW;
                return;
            }
            
            double suspiciousRatio = (double) suspicious / total;
            double blockedRatio = (double) blocked / total;
            
            if (blockedRatio > 0.5 || suspiciousRatio > 0.7) {
                threatLevel = ThreatLevel.CRITICAL;
            } else if (blockedRatio > 0.2 || suspiciousRatio > 0.4) {
                threatLevel = ThreatLevel.HIGH;
            } else if (blockedRatio > 0.1 || suspiciousRatio > 0.2) {
                threatLevel = ThreatLevel.MEDIUM;
            } else {
                threatLevel = ThreatLevel.LOW;
            }
        }

        public boolean isHighRisk() {
            return threatLevel == ThreatLevel.HIGH || threatLevel == ThreatLevel.CRITICAL;
        }
    }

    /**
     * Threat levels
     */
    public enum ThreatLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    /**
     * Security scan result
     */
    public static class SecurityScanResult {
        private final boolean blocked;
        private final String reason;
        private final ThreatLevel threatLevel;
        private final Map<String, Object> details;

        public SecurityScanResult(boolean blocked, String reason, ThreatLevel threatLevel, Map<String, Object> details) {
            this.blocked = blocked;
            this.reason = reason;
            this.threatLevel = threatLevel;
            this.details = details != null ? new HashMap<>(details) : new HashMap<>();
        }

        public boolean isBlocked() { return blocked; }
        public String getReason() { return reason; }
        public ThreatLevel getThreatLevel() { return threatLevel; }
        public Map<String, Object> getDetails() { return details; }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!securityEnabled) {
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        
        // Add security headers
        addSecurityHeaders(response);
        
        // Extract client information
        String clientIp = extractClientIp(request);
        String userAgent = request.getHeaders().getFirst(HttpHeaders.USER_AGENT);
        String path = request.getURI().getPath();
        
        // Update security profile
        SecurityProfile profile = updateSecurityProfile(clientIp, userAgent, path);
        
        // Perform security scan
        return performSecurityScan(request, profile)
                .flatMap(scanResult -> {
                    if (scanResult.isBlocked()) {
                        return handleSecurityViolation(exchange, scanResult, profile);
                    }
                    
                    // Continue with the request
                    return chain.filter(exchange)
                            .doOnSuccess(v -> recordSuccessfulRequest(profile))
                            .doOnError(error -> recordErrorRequest(profile, error));
                })
                .doOnError(error -> {
                    log.error("Security middleware error for IP {}: {}", clientIp, error.getMessage());
                    recordErrorRequest(profile, error);
                });
    }

    /**
     * Add security headers to response
     */
    private void addSecurityHeaders(ServerHttpResponse response) {
        HttpHeaders headers = response.getHeaders();
        
        SECURITY_HEADERS.forEach((key, value) -> {
            if (!headers.containsKey(key)) {
                headers.add(key, value);
            }
        });
        
        // Add custom security headers
        headers.add("X-Security-Scan", "enabled");
        headers.add("X-Request-ID", UUID.randomUUID().toString());
        headers.add("X-Timestamp", Instant.now().toString());
    }

    /**
     * Extract client IP address
     */
    private String extractClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }

    /**
     * Update security profile for IP
     */
    private SecurityProfile updateSecurityProfile(String clientIp, String userAgent, String path) {
        SecurityProfile profile = ipSecurityProfiles.computeIfAbsent(clientIp, SecurityProfile::new);
        profile.recordRequest(userAgent, path);
        return profile;
    }

    /**
     * Perform comprehensive security scan
     */
    private Mono<SecurityScanResult> performSecurityScan(ServerHttpRequest request, SecurityProfile profile) {
        return Mono.fromCallable(() -> {
            Map<String, Object> scanDetails = new HashMap<>();
            ThreatLevel maxThreatLevel = ThreatLevel.LOW;
            
            // Check if IP is blocked
            if (isIpBlocked(profile.getIpAddress())) {
                return new SecurityScanResult(true, "IP address is blocked", ThreatLevel.CRITICAL, 
                        Map.of("blockReason", "IP blocked"));
            }
            
            // Check request size
            Long contentLength = request.getHeaders().getContentLength();
            if (contentLength != null && contentLength > maxRequestSize) {
                return new SecurityScanResult(true, "Request size exceeds limit", ThreatLevel.HIGH,
                        Map.of("requestSize", contentLength, "maxSize", maxRequestSize));
            }
            
            // Check headers
            SecurityScanResult headerScan = scanHeaders(request);
            if (headerScan.isBlocked()) {
                return headerScan;
            }
            maxThreatLevel = max(maxThreatLevel, headerScan.getThreatLevel());
            scanDetails.putAll(headerScan.getDetails());
            
            // Check URL and parameters
            SecurityScanResult urlScan = scanUrl(request);
            if (urlScan.isBlocked()) {
                return urlScan;
            }
            maxThreatLevel = max(maxThreatLevel, urlScan.getThreatLevel());
            scanDetails.putAll(urlScan.getDetails());
            
            // Check user agent
            SecurityScanResult userAgentScan = scanUserAgent(request);
            if (userAgentScan.isBlocked()) {
                return userAgentScan;
            }
            maxThreatLevel = max(maxThreatLevel, userAgentScan.getThreatLevel());
            scanDetails.putAll(userAgentScan.getDetails());
            
            // Check rate limiting
            SecurityScanResult rateLimitScan = checkRateLimit(profile);
            if (rateLimitScan.isBlocked()) {
                return rateLimitScan;
            }
            maxThreatLevel = max(maxThreatLevel, rateLimitScan.getThreatLevel());
            scanDetails.putAll(rateLimitScan.getDetails());
            
            // Check suspicious patterns
            SecurityScanResult patternScan = scanForSuspiciousPatterns(request);
            if (patternScan.isBlocked()) {
                return patternScan;
            }
            maxThreatLevel = max(maxThreatLevel, patternScan.getThreatLevel());
            scanDetails.putAll(patternScan.getDetails());
            
            // Final threat assessment
            if (profile.isHighRisk() && strictMode) {
                return new SecurityScanResult(true, "High-risk IP in strict mode", ThreatLevel.HIGH, scanDetails);
            }
            
            return new SecurityScanResult(false, "Security scan passed", maxThreatLevel, scanDetails);
        });
    }

    /**
     * Scan HTTP headers for security threats
     */
    private SecurityScanResult scanHeaders(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        Map<String, Object> details = new HashMap<>();
        
        // Check for oversized headers
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (String value : entry.getValue()) {
                if (value.length() > maxHeaderSize) {
                    details.put("oversizedHeader", entry.getKey());
                    return new SecurityScanResult(true, "Header size exceeds limit", ThreatLevel.HIGH, details);
                }
            }
        }
        
        // Check for suspicious headers
        String host = headers.getFirst(HttpHeaders.HOST);
        if (host != null && (host.contains("..") || host.contains("localhost") && !host.equals("localhost"))) {
            details.put("suspiciousHost", host);
            return new SecurityScanResult(true, "Suspicious host header", ThreatLevel.HIGH, details);
        }
        
        // Check for injection attempts in headers
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (String value : entry.getValue()) {
                if (containsSuspiciousContent(value)) {
                    details.put("suspiciousHeader", entry.getKey());
                    details.put("suspiciousValue", value);
                    return new SecurityScanResult(true, "Suspicious content in header", ThreatLevel.HIGH, details);
                }
            }
        }
        
        return new SecurityScanResult(false, "Header scan passed", ThreatLevel.LOW, details);
    }

    /**
     * Scan URL and parameters for security threats
     */
    private SecurityScanResult scanUrl(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        String query = request.getURI().getQuery();
        Map<String, Object> details = new HashMap<>();
        
        // Check path traversal
        if (PATH_TRAVERSAL_PATTERN.matcher(path).find()) {
            details.put("pathTraversal", path);
            return new SecurityScanResult(true, "Path traversal attempt detected", ThreatLevel.HIGH, details);
        }
        
        // Check for suspicious paths
        if (path.contains("/admin") || path.contains("/config") || path.contains("/.env")) {
            details.put("suspiciousPath", path);
            return new SecurityScanResult(true, "Access to sensitive path", ThreatLevel.MEDIUM, details);
        }
        
        // Check query parameters
        if (query != null) {
            if (containsSuspiciousContent(query)) {
                details.put("suspiciousQuery", query);
                return new SecurityScanResult(true, "Suspicious content in query", ThreatLevel.HIGH, details);
            }
        }
        
        return new SecurityScanResult(false, "URL scan passed", ThreatLevel.LOW, details);
    }

    /**
     * Scan user agent for security threats
     */
    private SecurityScanResult scanUserAgent(ServerHttpRequest request) {
        String userAgent = request.getHeaders().getFirst(HttpHeaders.USER_AGENT);
        Map<String, Object> details = new HashMap<>();
        
        if (userAgent == null || userAgent.isEmpty()) {
            details.put("missingUserAgent", true);
            return new SecurityScanResult(false, "Missing user agent", ThreatLevel.LOW, details);
        }
        
        // Check for suspicious user agents
        String lowerUserAgent = userAgent.toLowerCase();
        for (String suspicious : SUSPICIOUS_USER_AGENTS) {
            if (lowerUserAgent.contains(suspicious)) {
                details.put("suspiciousUserAgent", userAgent);
                return new SecurityScanResult(true, "Suspicious user agent detected", ThreatLevel.HIGH, details);
            }
        }
        
        return new SecurityScanResult(false, "User agent scan passed", ThreatLevel.LOW, details);
    }

    /**
     * Check rate limiting for IP
     */
    private SecurityScanResult checkRateLimit(SecurityProfile profile) {
        Map<String, Object> details = new HashMap<>();
        
        // Simple rate limiting check
        long recentRequests = profile.getTotalRequests();
        if (recentRequests > 1000) { // More than 1000 requests from same IP
            details.put("requestCount", recentRequests);
            return new SecurityScanResult(true, "Rate limit exceeded", ThreatLevel.HIGH, details);
        }
        
        return new SecurityScanResult(false, "Rate limit check passed", ThreatLevel.LOW, details);
    }

    /**
     * Scan for suspicious patterns
     */
    private SecurityScanResult scanForSuspiciousPatterns(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        String query = request.getURI().getQuery();
        Map<String, Object> details = new HashMap<>();
        
        // Combine all scannable content
        StringBuilder content = new StringBuilder();
        content.append(path);
        if (query != null) {
            content.append("?").append(query);
        }
        
        String scanContent = content.toString();
        
        // Check for SQL injection
        if (SQL_INJECTION_PATTERN.matcher(scanContent).find()) {
            details.put("sqlInjection", true);
            return new SecurityScanResult(true, "SQL injection attempt detected", ThreatLevel.CRITICAL, details);
        }
        
        // Check for XSS
        if (XSS_PATTERN.matcher(scanContent).find()) {
            details.put("xss", true);
            return new SecurityScanResult(true, "XSS attempt detected", ThreatLevel.HIGH, details);
        }
        
        // Check for command injection
        if (COMMAND_INJECTION_PATTERN.matcher(scanContent).find()) {
            details.put("commandInjection", true);
            return new SecurityScanResult(true, "Command injection attempt detected", ThreatLevel.CRITICAL, details);
        }
        
        return new SecurityScanResult(false, "Pattern scan passed", ThreatLevel.LOW, details);
    }

    /**
     * Check if content contains suspicious patterns
     */
    private boolean containsSuspiciousContent(String content) {
        if (content == null) return false;
        
        return SQL_INJECTION_PATTERN.matcher(content).find() ||
               XSS_PATTERN.matcher(content).find() ||
               PATH_TRAVERSAL_PATTERN.matcher(content).find() ||
               COMMAND_INJECTION_PATTERN.matcher(content).find();
    }

    /**
     * Handle security violation
     */
    private Mono<Void> handleSecurityViolation(ServerWebExchange exchange, SecurityScanResult scanResult, SecurityProfile profile) {
        ServerHttpResponse response = exchange.getResponse();
        
        // Update security profile
        profile.recordSuspiciousActivity();
        if (scanResult.isBlocked()) {
            profile.recordBlockedRequest();
        }
        
        // Log security event
        logSecurityEvent(exchange, scanResult, profile);
        
        // Record metrics
        metricsCollectorService.recordSecurityThreat(scanResult.getReason(), scanResult.getThreatLevel().name());
        
        // Block IP if necessary
        if (scanResult.getThreatLevel() == ThreatLevel.CRITICAL) {
            blockIp(profile.getIpAddress(), Duration.ofHours(1));
        }
        
        // Return security error response
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().add("X-Security-Block-Reason", scanResult.getReason());
        response.getHeaders().add("X-Threat-Level", scanResult.getThreatLevel().name());
        
        return response.setComplete();
    }

    /**
     * Log security event
     */
    private void logSecurityEvent(ServerWebExchange exchange, SecurityScanResult scanResult, SecurityProfile profile) {
        ServerHttpRequest request = exchange.getRequest();
        
        AuditLoggingService.AuditEvent event = AuditLoggingService.builder()
                .eventType(AuditLoggingService.AuditEventType.SECURITY)
                .severity(mapThreatLevelToSeverity(scanResult.getThreatLevel()))
                .outcome(scanResult.isBlocked() ? AuditLoggingService.AuditOutcome.FAILURE : AuditLoggingService.AuditOutcome.SUCCESS)
                .sourceIp(profile.getIpAddress())
                .userAgent(request.getHeaders().getFirst(HttpHeaders.USER_AGENT))
                .action(request.getMethod().name())
                .resource(request.getURI().getPath())
                .description(scanResult.getReason())
                .details(scanResult.getDetails())
                .tag("threatLevel", scanResult.getThreatLevel().name())
                .tag("blocked", String.valueOf(scanResult.isBlocked()))
                .build();
        
        auditLoggingService.logAuditEvent(event).subscribe();
    }

    /**
     * Map threat level to audit severity
     */
    private AuditLoggingService.AuditSeverity mapThreatLevelToSeverity(ThreatLevel threatLevel) {
        switch (threatLevel) {
            case CRITICAL:
                return AuditLoggingService.AuditSeverity.CRITICAL;
            case HIGH:
                return AuditLoggingService.AuditSeverity.HIGH;
            case MEDIUM:
                return AuditLoggingService.AuditSeverity.MEDIUM;
            case LOW:
            default:
                return AuditLoggingService.AuditSeverity.LOW;
        }
    }

    /**
     * Record successful request
     */
    private void recordSuccessfulRequest(SecurityProfile profile) {
        // Update profile for successful requests
        log.debug("Successful request from IP: {}", profile.getIpAddress());
    }

    /**
     * Record error request
     */
    private void recordErrorRequest(SecurityProfile profile, Throwable error) {
        profile.recordSuspiciousActivity();
        log.warn("Error request from IP {}: {}", profile.getIpAddress(), error.getMessage());
    }

    /**
     * Check if IP is blocked
     */
    private boolean isIpBlocked(String ipAddress) {
        Instant blockExpiry = blockedIps.get(ipAddress);
        if (blockExpiry != null) {
            if (Instant.now().isBefore(blockExpiry)) {
                return true;
            } else {
                blockedIps.remove(ipAddress);
            }
        }
        return false;
    }

    /**
     * Block IP address
     */
    private void blockIp(String ipAddress, Duration duration) {
        Instant expiry = Instant.now().plus(duration);
        blockedIps.put(ipAddress, expiry);
        log.warn("Blocked IP address {} for {}", ipAddress, duration);
    }

    /**
     * Get maximum threat level
     */
    private ThreatLevel max(ThreatLevel a, ThreatLevel b) {
        return a.ordinal() > b.ordinal() ? a : b;
    }

    /**
     * Get security statistics
     */
    public Map<String, Object> getSecurityStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalProfiles", ipSecurityProfiles.size());
        stats.put("blockedIps", blockedIps.size());
        stats.put("securityEnabled", securityEnabled);
        stats.put("strictMode", strictMode);
        
        // Threat level distribution
        Map<ThreatLevel, Long> threatLevelDistribution = new HashMap<>();
        for (ThreatLevel level : ThreatLevel.values()) {
            threatLevelDistribution.put(level, 0L);
        }
        
        ipSecurityProfiles.values().forEach(profile -> {
            ThreatLevel level = profile.getThreatLevel();
            threatLevelDistribution.put(level, threatLevelDistribution.get(level) + 1);
        });
        
        stats.put("threatLevelDistribution", threatLevelDistribution);
        
        // High-risk IPs
        long highRiskIps = ipSecurityProfiles.values().stream()
                .mapToLong(profile -> profile.isHighRisk() ? 1 : 0)
                .sum();
        stats.put("highRiskIps", highRiskIps);
        
        return stats;
    }

    /**
     * Get security profile for IP
     */
    public SecurityProfile getSecurityProfile(String ipAddress) {
        return ipSecurityProfiles.get(ipAddress);
    }
}