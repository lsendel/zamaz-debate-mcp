package com.zamaz.mcp.sidecar.controller;

import com.zamaz.mcp.sidecar.service.AdvancedRateLimitingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rate Limiting Controller for MCP Sidecar
 * 
 * Provides REST endpoints for managing rate limits and user tiers
 */
@RestController
@RequestMapping("/api/v1/rate-limits")
@RequiredArgsConstructor
@Slf4j
public class RateLimitingController {

    private final AdvancedRateLimitingService rateLimitingService;

    /**
     * Get user tier
     */
    @GetMapping("/users/{userId}/tier")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or authentication.name == #userId")
    public Mono<ResponseEntity<UserTierResponse>> getUserTier(@PathVariable String userId) {
        return rateLimitingService.getUserTier(userId)
                .map(tier -> ResponseEntity.ok(new UserTierResponse(
                    userId,
                    tier.name(),
                    tier.getDisplayName(),
                    tier.getRequestsPerMinute(),
                    tier.getBurstCapacity(),
                    tier.getDailyQuota(),
                    tier.getMonthlyQuota()
                )))
                .doOnSuccess(response -> log.debug("User tier requested for: {}", userId))
                .onErrorResume(error -> {
                    log.error("Error getting user tier for {}: {}", userId, error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Set user tier
     */
    @PostMapping("/users/{userId}/tier")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS')")
    public Mono<ResponseEntity<Map<String, String>>> setUserTier(
            @PathVariable String userId,
            @RequestBody SetUserTierRequest request) {
        
        try {
            AdvancedRateLimitingService.UserTier tier = 
                AdvancedRateLimitingService.UserTier.valueOf(request.getTier().toUpperCase());
            
            return rateLimitingService.setUserTier(userId, tier)
                    .then(Mono.just(ResponseEntity.ok(Map.of(
                        "status", "User tier updated",
                        "userId", userId,
                        "tier", tier.name()
                    ))))
                    .doOnSuccess(response -> log.info("User tier set: userId={}, tier={}", userId, tier.name()))
                    .onErrorResume(error -> {
                        log.error("Error setting user tier for {}: {}", userId, error.getMessage());
                        return Mono.just(ResponseEntity.internalServerError().build());
                    });
        } catch (IllegalArgumentException e) {
            log.warn("Invalid tier specified: {}", request.getTier());
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }

    /**
     * Get user usage statistics
     */
    @GetMapping("/users/{userId}/usage")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR') or authentication.name == #userId")
    public Mono<ResponseEntity<UsageStatsResponse>> getUserUsage(@PathVariable String userId) {
        return rateLimitingService.getUserUsageStats(userId)
                .map(stats -> {
                    if (stats == null) {
                        return ResponseEntity.notFound().<UsageStatsResponse>build();
                    }
                    return ResponseEntity.ok(new UsageStatsResponse(
                        userId,
                        stats.getDailyUsage(),
                        stats.getMonthlyUsage(),
                        stats.getTotalUsage(),
                        stats.getLastResetDaily(),
                        stats.getLastResetMonthly()
                    ));
                })
                .doOnSuccess(response -> log.debug("Usage statistics requested for: {}", userId))
                .onErrorResume(error -> {
                    log.error("Error getting usage statistics for {}: {}", userId, error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Clear user usage statistics
     */
    @DeleteMapping("/users/{userId}/usage")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS')")
    public Mono<ResponseEntity<Map<String, String>>> clearUserUsage(@PathVariable String userId) {
        return rateLimitingService.clearUserUsage(userId)
                .then(Mono.just(ResponseEntity.ok(Map.of(
                    "status", "Usage statistics cleared",
                    "userId", userId
                ))))
                .doOnSuccess(response -> log.info("Usage statistics cleared for: {}", userId))
                .onErrorResume(error -> {
                    log.error("Error clearing usage statistics for {}: {}", userId, error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Set custom rate limit for user endpoint
     */
    @PostMapping("/users/{userId}/custom-limits")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS')")
    public Mono<ResponseEntity<Map<String, String>>> setCustomRateLimit(
            @PathVariable String userId,
            @RequestBody SetCustomRateLimitRequest request) {
        
        return rateLimitingService.setCustomRateLimit(userId, request.getEndpoint(), request.getLimit())
                .then(Mono.just(ResponseEntity.ok(Map.of(
                    "status", "Custom rate limit set",
                    "userId", userId,
                    "endpoint", request.getEndpoint(),
                    "limit", String.valueOf(request.getLimit())
                ))))
                .doOnSuccess(response -> log.info("Custom rate limit set: userId={}, endpoint={}, limit={}", 
                    userId, request.getEndpoint(), request.getLimit()))
                .onErrorResume(error -> {
                    log.error("Error setting custom rate limit for {}: {}", userId, error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Activate grace period for user
     */
    @PostMapping("/users/{userId}/grace-period")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS')")
    public Mono<ResponseEntity<Map<String, String>>> activateGracePeriod(
            @PathVariable String userId,
            @RequestBody ActivateGracePeriodRequest request) {
        
        Duration duration = Duration.ofMinutes(request.getDurationMinutes());
        
        return rateLimitingService.activateGracePeriod(userId, duration)
                .then(Mono.just(ResponseEntity.ok(Map.of(
                    "status", "Grace period activated",
                    "userId", userId,
                    "duration", duration.toString()
                ))))
                .doOnSuccess(response -> log.info("Grace period activated: userId={}, duration={}", 
                    userId, duration))
                .onErrorResume(error -> {
                    log.error("Error activating grace period for {}: {}", userId, error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Deactivate grace period for user
     */
    @DeleteMapping("/users/{userId}/grace-period")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS')")
    public Mono<ResponseEntity<Map<String, String>>> deactivateGracePeriod(@PathVariable String userId) {
        return rateLimitingService.deactivateGracePeriod(userId)
                .then(Mono.just(ResponseEntity.ok(Map.of(
                    "status", "Grace period deactivated",
                    "userId", userId
                ))))
                .doOnSuccess(response -> log.info("Grace period deactivated for: {}", userId))
                .onErrorResume(error -> {
                    log.error("Error deactivating grace period for {}: {}", userId, error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Check rate limit for user request
     */
    @PostMapping("/check")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<RateLimitCheckResponse>> checkRateLimit(
            @RequestBody RateLimitCheckRequest request) {
        
        return rateLimitingService.checkRateLimit(request.getUserId(), request.getEndpoint(), request.getMethod())
                .map(result -> ResponseEntity.ok(new RateLimitCheckResponse(
                    result.getDecision().name(),
                    result.getUserId(),
                    result.getEndpoint(),
                    result.getTier().name(),
                    result.getCurrentUsage(),
                    result.getLimit(),
                    result.getRemainingQuota(),
                    result.getRetryAfter().toString(),
                    result.isAllowed(),
                    result.getMetadata()
                )))
                .doOnSuccess(response -> log.debug("Rate limit check: userId={}, endpoint={}, method={}", 
                    request.getUserId(), request.getEndpoint(), request.getMethod()))
                .onErrorResume(error -> {
                    log.error("Error checking rate limit: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get rate limiting statistics
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, Object>>> getRateLimitingStatistics() {
        return rateLimitingService.getRateLimitingStatistics()
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("Rate limiting statistics requested"))
                .onErrorResume(error -> {
                    log.error("Error getting rate limiting statistics: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get available user tiers
     */
    @GetMapping("/tiers")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public ResponseEntity<List<UserTierInfo>> getAvailableTiers() {
        List<UserTierInfo> tiers = List.of(
            new UserTierInfo(
                AdvancedRateLimitingService.UserTier.FREE.name(),
                AdvancedRateLimitingService.UserTier.FREE.getDisplayName(),
                AdvancedRateLimitingService.UserTier.FREE.getRequestsPerMinute(),
                AdvancedRateLimitingService.UserTier.FREE.getBurstCapacity(),
                AdvancedRateLimitingService.UserTier.FREE.getDailyQuota(),
                AdvancedRateLimitingService.UserTier.FREE.getMonthlyQuota()
            ),
            new UserTierInfo(
                AdvancedRateLimitingService.UserTier.BASIC.name(),
                AdvancedRateLimitingService.UserTier.BASIC.getDisplayName(),
                AdvancedRateLimitingService.UserTier.BASIC.getRequestsPerMinute(),
                AdvancedRateLimitingService.UserTier.BASIC.getBurstCapacity(),
                AdvancedRateLimitingService.UserTier.BASIC.getDailyQuota(),
                AdvancedRateLimitingService.UserTier.BASIC.getMonthlyQuota()
            ),
            new UserTierInfo(
                AdvancedRateLimitingService.UserTier.PREMIUM.name(),
                AdvancedRateLimitingService.UserTier.PREMIUM.getDisplayName(),
                AdvancedRateLimitingService.UserTier.PREMIUM.getRequestsPerMinute(),
                AdvancedRateLimitingService.UserTier.PREMIUM.getBurstCapacity(),
                AdvancedRateLimitingService.UserTier.PREMIUM.getDailyQuota(),
                AdvancedRateLimitingService.UserTier.PREMIUM.getMonthlyQuota()
            ),
            new UserTierInfo(
                AdvancedRateLimitingService.UserTier.ENTERPRISE.name(),
                AdvancedRateLimitingService.UserTier.ENTERPRISE.getDisplayName(),
                AdvancedRateLimitingService.UserTier.ENTERPRISE.getRequestsPerMinute(),
                AdvancedRateLimitingService.UserTier.ENTERPRISE.getBurstCapacity(),
                AdvancedRateLimitingService.UserTier.ENTERPRISE.getDailyQuota(),
                AdvancedRateLimitingService.UserTier.ENTERPRISE.getMonthlyQuota()
            ),
            new UserTierInfo(
                AdvancedRateLimitingService.UserTier.UNLIMITED.name(),
                AdvancedRateLimitingService.UserTier.UNLIMITED.getDisplayName(),
                AdvancedRateLimitingService.UserTier.UNLIMITED.getRequestsPerMinute(),
                AdvancedRateLimitingService.UserTier.UNLIMITED.getBurstCapacity(),
                AdvancedRateLimitingService.UserTier.UNLIMITED.getDailyQuota(),
                AdvancedRateLimitingService.UserTier.UNLIMITED.getMonthlyQuota()
            )
        );
        
        return ResponseEntity.ok(tiers);
    }

    /**
     * Request/Response DTOs
     */
    public static class SetUserTierRequest {
        private String tier;
        
        public String getTier() { return tier; }
        public void setTier(String tier) { this.tier = tier; }
    }

    public static class SetCustomRateLimitRequest {
        private String endpoint;
        private int limit;
        
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
    }

    public static class ActivateGracePeriodRequest {
        private int durationMinutes;
        
        public int getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    }

    public static class RateLimitCheckRequest {
        private String userId;
        private String endpoint;
        private String method;
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
    }

    public static class UserTierResponse {
        private final String userId;
        private final String tier;
        private final String displayName;
        private final int requestsPerMinute;
        private final int burstCapacity;
        private final int dailyQuota;
        private final int monthlyQuota;
        
        public UserTierResponse(String userId, String tier, String displayName, 
                               int requestsPerMinute, int burstCapacity, 
                               int dailyQuota, int monthlyQuota) {
            this.userId = userId;
            this.tier = tier;
            this.displayName = displayName;
            this.requestsPerMinute = requestsPerMinute;
            this.burstCapacity = burstCapacity;
            this.dailyQuota = dailyQuota;
            this.monthlyQuota = monthlyQuota;
        }
        
        public String getUserId() { return userId; }
        public String getTier() { return tier; }
        public String getDisplayName() { return displayName; }
        public int getRequestsPerMinute() { return requestsPerMinute; }
        public int getBurstCapacity() { return burstCapacity; }
        public int getDailyQuota() { return dailyQuota; }
        public int getMonthlyQuota() { return monthlyQuota; }
    }

    public static class UsageStatsResponse {
        private final String userId;
        private final Map<String, Long> dailyUsage;
        private final Map<String, Long> monthlyUsage;
        private final Map<String, Long> totalUsage;
        private final java.time.Instant lastResetDaily;
        private final java.time.Instant lastResetMonthly;
        
        public UsageStatsResponse(String userId, Map<String, Long> dailyUsage, 
                                 Map<String, Long> monthlyUsage, Map<String, Long> totalUsage,
                                 java.time.Instant lastResetDaily, java.time.Instant lastResetMonthly) {
            this.userId = userId;
            this.dailyUsage = dailyUsage;
            this.monthlyUsage = monthlyUsage;
            this.totalUsage = totalUsage;
            this.lastResetDaily = lastResetDaily;
            this.lastResetMonthly = lastResetMonthly;
        }
        
        public String getUserId() { return userId; }
        public Map<String, Long> getDailyUsage() { return dailyUsage; }
        public Map<String, Long> getMonthlyUsage() { return monthlyUsage; }
        public Map<String, Long> getTotalUsage() { return totalUsage; }
        public java.time.Instant getLastResetDaily() { return lastResetDaily; }
        public java.time.Instant getLastResetMonthly() { return lastResetMonthly; }
    }

    public static class RateLimitCheckResponse {
        private final String decision;
        private final String userId;
        private final String endpoint;
        private final String tier;
        private final long currentUsage;
        private final long limit;
        private final long remainingQuota;
        private final String retryAfter;
        private final boolean allowed;
        private final Map<String, Object> metadata;
        
        public RateLimitCheckResponse(String decision, String userId, String endpoint, 
                                     String tier, long currentUsage, long limit, 
                                     long remainingQuota, String retryAfter, 
                                     boolean allowed, Map<String, Object> metadata) {
            this.decision = decision;
            this.userId = userId;
            this.endpoint = endpoint;
            this.tier = tier;
            this.currentUsage = currentUsage;
            this.limit = limit;
            this.remainingQuota = remainingQuota;
            this.retryAfter = retryAfter;
            this.allowed = allowed;
            this.metadata = metadata;
        }
        
        public String getDecision() { return decision; }
        public String getUserId() { return userId; }
        public String getEndpoint() { return endpoint; }
        public String getTier() { return tier; }
        public long getCurrentUsage() { return currentUsage; }
        public long getLimit() { return limit; }
        public long getRemainingQuota() { return remainingQuota; }
        public String getRetryAfter() { return retryAfter; }
        public boolean isAllowed() { return allowed; }
        public Map<String, Object> getMetadata() { return metadata; }
    }

    public static class UserTierInfo {
        private final String tier;
        private final String displayName;
        private final int requestsPerMinute;
        private final int burstCapacity;
        private final int dailyQuota;
        private final int monthlyQuota;
        
        public UserTierInfo(String tier, String displayName, int requestsPerMinute, 
                           int burstCapacity, int dailyQuota, int monthlyQuota) {
            this.tier = tier;
            this.displayName = displayName;
            this.requestsPerMinute = requestsPerMinute;
            this.burstCapacity = burstCapacity;
            this.dailyQuota = dailyQuota;
            this.monthlyQuota = monthlyQuota;
        }
        
        public String getTier() { return tier; }
        public String getDisplayName() { return displayName; }
        public int getRequestsPerMinute() { return requestsPerMinute; }
        public int getBurstCapacity() { return burstCapacity; }
        public int getDailyQuota() { return dailyQuota; }
        public int getMonthlyQuota() { return monthlyQuota; }
    }
}