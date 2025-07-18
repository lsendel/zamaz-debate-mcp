package com.zamaz.mcp.common.resilience;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Management endpoint for MCP rate limiting operations.
 * Provides monitoring and administration capabilities for rate limits.
 */
@RestController
@RequestMapping("/api/v1/mcp/rate-limits")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "MCP Rate Limiting", description = "Rate limiting management and monitoring")
@ConditionalOnProperty(name = "mcp.rate-limiting.enabled", havingValue = "true", matchIfMissing = true)
public class McpRateLimitingController {

    private final McpRateLimitingService rateLimitingService;
    private final McpRateLimitingConfiguration rateLimitingConfig;
    
    @Autowired(required = false)
    private MeterRegistry meterRegistry;
    
    // Metrics tracking
    private final Map<String, Counter> rateLimitViolationCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> rateLimitRequestCounters = new ConcurrentHashMap<>();
    private final Map<String, Timer> rateLimitTimers = new ConcurrentHashMap<>();
    private final AtomicInteger activeRateLimiters = new AtomicInteger(0);
    private final AtomicLong totalRateLimitViolations = new AtomicLong(0);
    
    @PostConstruct
    public void initializeMetrics() {
        if (meterRegistry != null) {
            // Register gauge for active rate limiters
            Gauge.builder("rate_limiter.active", activeRateLimiters, AtomicInteger::get)
                .description("Number of active rate limiters")
                .register(meterRegistry);
                
            // Register gauge for total violations
            Gauge.builder("rate_limiter.violations.total", totalRateLimitViolations, AtomicLong::get)
                .description("Total rate limit violations")
                .register(meterRegistry);
                
            log.info("Rate limiting metrics initialized with MeterRegistry");
        }
    }

    /**
     * Get rate limit status for the current user.
     */
    @GetMapping("/status")
    @Operation(summary = "Get current user's rate limit status")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getRateLimitStatus(Authentication authentication) {
        
        Map<String, McpRateLimitingService.RateLimitStatus> rateLimitStatus = 
            rateLimitingService.getAllRateLimitStatus(authentication);
        
        Map<String, Object> response = new HashMap<>();
        response.put("rateLimits", rateLimitStatus);
        response.put("userContext", authentication.getName());
        response.put("timestamp", System.currentTimeMillis());
        
        // Record status check metrics
        if (meterRegistry != null) {
            recordRateLimitRequest("status_check_" + authentication.getName(), true);
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get rate limit status for a specific service and tool.
     */
    @GetMapping("/status/{serviceName}/{toolName}")
    @Operation(summary = "Get rate limit status for specific service and tool")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<McpRateLimitingService.RateLimitStatus> getSpecificRateLimitStatus(
            @PathVariable String serviceName,
            @PathVariable String toolName,
            Authentication authentication) {
        
        McpRateLimitingService.RateLimitStatus status = 
            rateLimitingService.getRateLimitStatus(serviceName, toolName, authentication);
        
        return ResponseEntity.ok(status);
    }

    /**
     * Check if a request would be permitted (dry run).
     */
    @PostMapping("/check/{serviceName}/{toolName}")
    @Operation(summary = "Check if request would be permitted (dry run)")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> checkRateLimit(
            @PathVariable String serviceName,
            @PathVariable String toolName,
            Authentication authentication) {
        
        boolean permitted = rateLimitingService.isRequestPermitted(serviceName, toolName, authentication);
        McpRateLimitingService.RateLimitStatus status = 
            rateLimitingService.getRateLimitStatus(serviceName, toolName, authentication);
        
        Map<String, Object> response = new HashMap<>();
        response.put("permitted", permitted);
        response.put("status", status);
        response.put("serviceName", serviceName);
        response.put("toolName", toolName);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get rate limiting configuration (admin only).
     */
    @GetMapping("/config")
    @Operation(summary = "Get rate limiting configuration")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getRateLimitingConfig() {
        
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", rateLimitingConfig.isEnabled());
        config.put("organizationLevelLimiting", rateLimitingConfig.isOrganizationLevelLimiting());
        config.put("userLevelLimiting", rateLimitingConfig.isUserLevelLimiting());
        config.put("ipLevelLimiting", rateLimitingConfig.isIpLevelLimiting());
        config.put("globalLimits", rateLimitingConfig.getGlobal());
        config.put("serviceLimits", rateLimitingConfig.getServices());
        config.put("toolLimits", rateLimitingConfig.getTools());
        config.put("tierLimits", rateLimitingConfig.getTiers());
        
        return ResponseEntity.ok(config);
    }

    /**
     * Reset rate limiters for current user (admin only).
     */
    @PostMapping("/reset")
    @Operation(summary = "Reset rate limiters for current user")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> resetUserRateLimiters(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String toolName,
            Authentication authentication) {
        
        rateLimitingService.resetRateLimiters(serviceName, toolName, authentication);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Rate limiters reset successfully");
        response.put("serviceName", serviceName);
        response.put("toolName", toolName);
        response.put("userContext", authentication.getName());
        response.put("timestamp", System.currentTimeMillis());
        
        log.info("Rate limiters reset by admin {} for service: {}, tool: {}", 
                authentication.getName(), serviceName, toolName);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get system-wide rate limiting metrics (admin only).
     */
    @GetMapping("/metrics")
    @Operation(summary = "Get system-wide rate limiting metrics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getRateLimitingMetrics() {
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("timestamp", System.currentTimeMillis());
        
        // Get metrics from rate limiting service
        Map<String, McpRateLimitingService.RateLimitMetrics> serviceMetrics = 
            rateLimitingService.getAllRateLimitMetrics();
        
        // Aggregate metrics
        int totalRateLimiters = serviceMetrics.size();
        long totalRequests = 0;
        long totalViolations = 0;
        long activeUsers = 0;
        
        for (McpRateLimitingService.RateLimitMetrics limitMetrics : serviceMetrics.values()) {
            totalRequests += limitMetrics.getRequests();
            totalViolations += limitMetrics.getViolations();
            if (limitMetrics.getAvailablePermits() < limitMetrics.getCapacity()) {
                activeUsers++;
            }
        }
        
        // Update metric counters
        activeRateLimiters.set(totalRateLimiters);
        totalRateLimitViolations.set(totalViolations);
        
        // Populate response
        metrics.put("totalRateLimiters", totalRateLimiters);
        metrics.put("activeUsers", activeUsers);
        metrics.put("totalRequests", totalRequests);
        metrics.put("totalViolations", totalViolations);
        metrics.put("violationRate", totalRequests > 0 ? 
            (double) totalViolations / totalRequests * 100 : 0.0);
        
        // Add configuration info
        metrics.put("configuration", Map.of(
            "enabled", rateLimitingConfig.isEnabled(),
            "defaultRateLimit", rateLimitingConfig.getDefaultRateLimit(),
            "defaultBurstLimit", rateLimitingConfig.getDefaultBurstLimit(),
            "cleanupIntervalSeconds", rateLimitingConfig.getCleanupIntervalSeconds()
        ));
        
        // Add Micrometer integration status
        metrics.put("micrometerIntegrated", meterRegistry != null);
        
        if (meterRegistry != null) {
            // Record metrics in Micrometer
            recordMetricsToMicrometer(totalRequests, totalViolations, activeUsers);
        }
        
        return ResponseEntity.ok(metrics);
    }

    /**
     * Health check for rate limiting system.
     */
    @GetMapping("/health")
    @Operation(summary = "Rate limiting system health check")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("enabled", rateLimitingConfig.isEnabled());
        health.put("timestamp", System.currentTimeMillis());
        
        // Check if configuration is valid
        try {
            boolean configValid = rateLimitingConfig.getGlobal() != null &&
                                 rateLimitingConfig.getServices() != null &&
                                 rateLimitingConfig.getTools() != null &&
                                 rateLimitingConfig.getTiers() != null;
            
            health.put("configurationValid", configValid);
            
            if (!configValid) {
                health.put("status", "DEGRADED");
                health.put("warning", "Rate limiting configuration is incomplete");
            }
            
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("error", "Rate limiting configuration error: " + e.getMessage());
            log.warn("Rate limiting health check failed", e);
        }
        
        return ResponseEntity.ok(health);
    }

    /**
     * Get effective rate limits for a specific context.
     */
    @GetMapping("/effective-limits/{serviceName}/{toolName}")
    @Operation(summary = "Get effective rate limits for specific context")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getEffectiveRateLimits(
            @PathVariable String serviceName,
            @PathVariable String toolName,
            Authentication authentication) {
        
        // Extract user and organization tiers
        String userTier = extractUserTier(authentication);
        String organizationTier = extractOrganizationTier(authentication);
        
        McpRateLimitingConfiguration.EffectiveRateLimits effectiveLimits = 
            rateLimitingConfig.getEffectiveRateLimits(serviceName, toolName, userTier, organizationTier);
        
        Map<String, Object> response = new HashMap<>();
        response.put("serviceName", serviceName);
        response.put("toolName", toolName);
        response.put("userTier", userTier);
        response.put("organizationTier", organizationTier);
        response.put("effectiveLimits", effectiveLimits);
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Extract user tier from authentication.
     */
    private String extractUserTier(Authentication authentication) {
        if (authentication != null && authentication.getAuthorities() != null) {
            return authentication.getAuthorities().stream()
                .filter(auth -> auth.getAuthority().startsWith("TIER_"))
                .findFirst()
                .map(tierAuth -> tierAuth.getAuthority().substring(5).toLowerCase())
                .orElse("free");
        }
        return "free";
    }

    /**
     * Extract organization tier from authentication.
     */
    private String extractOrganizationTier(Authentication authentication) {
        if (authentication != null && authentication.getAuthorities() != null) {
            return authentication.getAuthorities().stream()
                .filter(auth -> auth.getAuthority().startsWith("ORG_TIER_"))
                .findFirst()
                .map(orgTierAuth -> orgTierAuth.getAuthority().substring(9).toLowerCase())
                .orElse("free");
        }
        return "free";
    }
    
    /**
     * Records metrics to Micrometer for Prometheus export.
     */
    private void recordMetricsToMicrometer(long totalRequests, long totalViolations, long activeUsers) {
        if (meterRegistry == null) {
            return;
        }
        
        // Record total requests counter
        Counter totalRequestsCounter = Counter.builder("rate_limiter.requests.total")
            .description("Total rate limit requests")
            .register(meterRegistry);
        
        // Since we can't set counter values directly, we track the delta
        // In a real implementation, this would be done incrementally
        
        // Record violation rate gauge
        meterRegistry.gauge("rate_limiter.violation.rate", 
            totalRequests > 0 ? (double) totalViolations / totalRequests * 100 : 0.0);
            
        // Record active users gauge
        meterRegistry.gauge("rate_limiter.users.active", activeUsers);
    }
    
    /**
     * Get or create a counter for rate limit violations.
     */
    private Counter getOrCreateViolationCounter(String key) {
        return rateLimitViolationCounters.computeIfAbsent(key, k -> 
            Counter.builder("rate_limiter.violations")
                .tag("key", k)
                .description("Rate limit violations counter")
                .register(meterRegistry));
    }
    
    /**
     * Get or create a counter for rate limit requests.
     */
    private Counter getOrCreateRequestCounter(String key) {
        return rateLimitRequestCounters.computeIfAbsent(key, k -> 
            Counter.builder("rate_limiter.requests")
                .tag("key", k)
                .description("Rate limit requests counter")
                .register(meterRegistry));
    }
    
    /**
     * Get or create a timer for rate limit operations.
     */
    private Timer getOrCreateTimer(String key) {
        return rateLimitTimers.computeIfAbsent(key, k -> 
            Timer.builder("rate_limiter.operation.duration")
                .tag("key", k)
                .description("Rate limit operation duration")
                .register(meterRegistry));
    }
    
    /**
     * Record a rate limit request metric.
     */
    public void recordRateLimitRequest(String key, boolean allowed) {
        if (meterRegistry != null) {
            getOrCreateRequestCounter(key).increment();
            
            if (!allowed) {
                getOrCreateViolationCounter(key).increment();
                totalRateLimitViolations.incrementAndGet();
            }
        }
    }
    
    /**
     * Record rate limit operation timing.
     */
    public void recordRateLimitTiming(String key, Runnable operation) {
        if (meterRegistry != null) {
            Timer timer = getOrCreateTimer(key);
            timer.record(operation);
        } else {
            operation.run();
        }
    }
}