package com.zamaz.mcp.sidecar.controller;

import com.zamaz.mcp.sidecar.security.AdvancedSecurityMiddleware;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Security Controller for MCP Sidecar
 * 
 * Provides REST endpoints for security monitoring and management
 */
@RestController
@RequestMapping("/api/v1/security")
@RequiredArgsConstructor
@Slf4j
public class SecurityController {

    private final AdvancedSecurityMiddleware securityMiddleware;

    /**
     * Get security statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, Object>>> getSecurityStatistics() {
        return Mono.fromCallable(() -> securityMiddleware.getSecurityStatistics())
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("Security statistics requested"))
                .onErrorResume(error -> {
                    log.error("Error getting security statistics: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get security profile for IP
     */
    @GetMapping("/profiles/{ipAddress}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY') or hasRole('MONITOR')")
    public Mono<ResponseEntity<SecurityProfileResponse>> getSecurityProfile(@PathVariable String ipAddress) {
        return Mono.fromCallable(() -> {
            AdvancedSecurityMiddleware.SecurityProfile profile = securityMiddleware.getSecurityProfile(ipAddress);
            if (profile == null) {
                return ResponseEntity.notFound().<SecurityProfileResponse>build();
            }
            
            SecurityProfileResponse response = new SecurityProfileResponse(
                profile.getIpAddress(),
                profile.getTotalRequests(),
                profile.getSuspiciousRequests(),
                profile.getBlockedRequests(),
                profile.getThreatLevel().name(),
                profile.getFirstSeen(),
                profile.getLastSeen(),
                profile.getUserAgentCounts(),
                profile.getEndpointCounts()
            );
            
            return ResponseEntity.ok(response);
        })
        .doOnSuccess(response -> log.debug("Security profile requested for IP: {}", ipAddress))
        .onErrorResume(error -> {
            log.error("Error getting security profile for IP {}: {}", ipAddress, error.getMessage());
            return Mono.just(ResponseEntity.internalServerError().build());
        });
    }

    /**
     * Get security health
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, Object>>> getSecurityHealth() {
        return Mono.fromCallable(() -> {
            Map<String, Object> statistics = securityMiddleware.getSecurityStatistics();
            
            long highRiskIps = (Long) statistics.get("highRiskIps");
            long blockedIps = (Long) statistics.get("blockedIps");
            boolean securityEnabled = (Boolean) statistics.get("securityEnabled");
            
            String status = "UP";
            if (!securityEnabled) {
                status = "DOWN";
            } else if (highRiskIps > 10 || blockedIps > 50) {
                status = "DEGRADED";
            }
            
            Map<String, Object> health = Map.of(
                "status", status,
                "securityEnabled", securityEnabled,
                "highRiskIps", highRiskIps,
                "blockedIps", blockedIps,
                "totalProfiles", statistics.get("totalProfiles")
            );
            
            return ResponseEntity.ok(health);
        })
        .doOnSuccess(response -> log.debug("Security health check requested"))
        .onErrorResume(error -> {
            log.error("Error getting security health: {}", error.getMessage());
            return Mono.just(ResponseEntity.internalServerError().build());
        });
    }

    /**
     * Get threat level distribution
     */
    @GetMapping("/threat-levels")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, Object>>> getThreatLevelDistribution() {
        return Mono.fromCallable(() -> {
            Map<String, Object> statistics = securityMiddleware.getSecurityStatistics();
            Map<String, Object> response = Map.of(
                "threatLevelDistribution", statistics.get("threatLevelDistribution"),
                "totalProfiles", statistics.get("totalProfiles")
            );
            return ResponseEntity.ok(response);
        })
        .doOnSuccess(response -> log.debug("Threat level distribution requested"))
        .onErrorResume(error -> {
            log.error("Error getting threat level distribution: {}", error.getMessage());
            return Mono.just(ResponseEntity.internalServerError().build());
        });
    }

    /**
     * Test security scanning
     */
    @PostMapping("/test-scan")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY')")
    public Mono<ResponseEntity<Map<String, Object>>> testSecurityScan(
            @RequestBody SecurityTestRequest request) {
        
        return Mono.fromCallable(() -> {
            // This would normally perform a security scan on the provided content
            // For now, we'll return a simulated response
            
            boolean containsSuspiciousContent = request.getContent().contains("script") ||
                    request.getContent().contains("union") ||
                    request.getContent().contains("../");
            
            Map<String, Object> result = Map.of(
                "testContent", request.getContent(),
                "suspicious", containsSuspiciousContent,
                "threatLevel", containsSuspiciousContent ? "HIGH" : "LOW",
                "patterns", containsSuspiciousContent ? 
                    java.util.List.of("potential_injection", "suspicious_pattern") : 
                    java.util.List.of(),
                "recommendation", containsSuspiciousContent ? 
                    "Content blocked due to suspicious patterns" : 
                    "Content appears safe"
            );
            
            return ResponseEntity.ok(result);
        })
        .doOnSuccess(response -> log.debug("Security scan test performed"))
        .onErrorResume(error -> {
            log.error("Error performing security scan test: {}", error.getMessage());
            return Mono.just(ResponseEntity.internalServerError().build());
        });
    }

    /**
     * Response DTOs
     */
    public static class SecurityProfileResponse {
        private final String ipAddress;
        private final long totalRequests;
        private final long suspiciousRequests;
        private final long blockedRequests;
        private final String threatLevel;
        private final java.time.Instant firstSeen;
        private final java.time.Instant lastSeen;
        private final Map<String, java.util.concurrent.atomic.AtomicInteger> userAgentCounts;
        private final Map<String, java.util.concurrent.atomic.AtomicInteger> endpointCounts;

        public SecurityProfileResponse(String ipAddress, long totalRequests, long suspiciousRequests,
                                     long blockedRequests, String threatLevel, java.time.Instant firstSeen,
                                     java.time.Instant lastSeen, 
                                     Map<String, java.util.concurrent.atomic.AtomicInteger> userAgentCounts,
                                     Map<String, java.util.concurrent.atomic.AtomicInteger> endpointCounts) {
            this.ipAddress = ipAddress;
            this.totalRequests = totalRequests;
            this.suspiciousRequests = suspiciousRequests;
            this.blockedRequests = blockedRequests;
            this.threatLevel = threatLevel;
            this.firstSeen = firstSeen;
            this.lastSeen = lastSeen;
            this.userAgentCounts = userAgentCounts;
            this.endpointCounts = endpointCounts;
        }

        public String getIpAddress() { return ipAddress; }
        public long getTotalRequests() { return totalRequests; }
        public long getSuspiciousRequests() { return suspiciousRequests; }
        public long getBlockedRequests() { return blockedRequests; }
        public String getThreatLevel() { return threatLevel; }
        public java.time.Instant getFirstSeen() { return firstSeen; }
        public java.time.Instant getLastSeen() { return lastSeen; }
        public Map<String, java.util.concurrent.atomic.AtomicInteger> getUserAgentCounts() { return userAgentCounts; }
        public Map<String, java.util.concurrent.atomic.AtomicInteger> getEndpointCounts() { return endpointCounts; }
    }

    public static class SecurityTestRequest {
        private String content;
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}