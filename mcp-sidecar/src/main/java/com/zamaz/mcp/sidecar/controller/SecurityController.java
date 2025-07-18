package com.zamaz.mcp.sidecar.controller;

import com.zamaz.mcp.sidecar.service.SecurityScanningService;
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
 * Provides security management and monitoring endpoints
 */
@RestController
@RequestMapping("/api/v1/security")
@RequiredArgsConstructor
@Slf4j
public class SecurityController {

    private final SecurityScanningService securityScanningService;

    /**
     * Get security statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY')")
    public Mono<ResponseEntity<Map<String, Object>>> getSecurityStatistics() {
        return securityScanningService.getSecurityStatistics()
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("Security statistics requested"))
                .onErrorResume(error -> {
                    log.error("Error getting security statistics", error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Check IP reputation
     */
    @GetMapping("/ip-reputation/{ip}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY')")
    public Mono<ResponseEntity<Map<String, Object>>> checkIPReputation(@PathVariable String ip) {
        return securityScanningService.checkIPReputation(ip)
                .map(safe -> {
                    Map<String, Object> result = Map.of(
                        "ip", ip,
                        "safe", safe,
                        "timestamp", System.currentTimeMillis()
                    );
                    return ResponseEntity.ok(result);
                })
                .doOnSuccess(response -> log.debug("IP reputation check requested for: {}", ip))
                .onErrorResume(error -> {
                    log.error("Error checking IP reputation for: {}", ip, error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Manual security scan
     */
    @PostMapping("/scan")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY')")
    public Mono<ResponseEntity<Map<String, Object>>> performSecurityScan(
            @RequestParam String clientId,
            @RequestParam String path,
            @RequestParam String method,
            @RequestBody(required = false) Map<String, Object> request) {
        
        String payload = request != null ? request.toString() : null;
        Map<String, String> headers = request != null && request.containsKey("headers") ? 
            (Map<String, String>) request.get("headers") : Map.of();
        
        return securityScanningService.scanRequest(clientId, path, method, headers, payload)
                .map(scanResult -> {
                    Map<String, Object> result = Map.of(
                        "blocked", scanResult.isBlocked(),
                        "reason", scanResult.getReason(),
                        "riskScore", scanResult.getRiskScore(),
                        "threatsDetected", scanResult.getThreats().size(),
                        "threats", scanResult.getThreats(),
                        "timestamp", System.currentTimeMillis()
                    );
                    return ResponseEntity.ok(result);
                })
                .doOnSuccess(response -> log.debug("Manual security scan performed: clientId={}, path={}", clientId, path))
                .onErrorResume(error -> {
                    log.error("Error performing manual security scan", error);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get security health status
     */
    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SECURITY') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, Object>>> getSecurityHealth() {
        return securityScanningService.getSecurityStatistics()
                .map(stats -> {
                    Map<String, Object> health = Map.of(
                        "scanningEnabled", stats.get("scanningEnabled"),
                        "threatIntelligenceEnabled", stats.containsKey("threatIntelligenceEnabled") ? 
                            stats.get("threatIntelligenceEnabled") : false,
                        "recentThreats", stats.get("totalThreats"),
                        "suspiciousActivities", stats.get("suspiciousActivities"),
                        "status", "UP",
                        "timestamp", System.currentTimeMillis()
                    );
                    return ResponseEntity.ok(health);
                })
                .doOnSuccess(response -> log.debug("Security health check requested"))
                .onErrorResume(error -> {
                    log.error("Error getting security health", error);
                    Map<String, Object> errorHealth = Map.of(
                        "status", "DOWN",
                        "error", error.getMessage(),
                        "timestamp", System.currentTimeMillis()
                    );
                    return Mono.just(ResponseEntity.ok(errorHealth));
                });
    }
}