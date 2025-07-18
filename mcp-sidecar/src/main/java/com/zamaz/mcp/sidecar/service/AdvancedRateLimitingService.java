package com.zamaz.mcp.sidecar.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced Rate Limiting Service with User Tiers and Quotas
 * 
 * Features:
 * - Multiple user tiers with different limits
 * - Quota-based limiting (daily, monthly)
 * - Burst allowances
 * - Grace periods for premium users
 * - Usage analytics and reporting
 * - Dynamic limit adjustments
 * - Fair usage policies
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdvancedRateLimitingService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final MetricsCollectorService metricsCollectorService;

    @Value("${app.rate-limiting.enabled:true}")
    private boolean rateLimitingEnabled;

    @Value("${app.rate-limiting.default-tier:FREE}")
    private String defaultTier;

    @Value("${app.rate-limiting.grace-period:PT5M}")
    private Duration gracePeriod;

    // User tier storage
    private final Map<String, UserTier> userTiers = new ConcurrentHashMap<>();
    private final Map<String, RateLimitProfile> rateLimitProfiles = new ConcurrentHashMap<>();
    private final Map<String, UsageStats> usageStatistics = new ConcurrentHashMap<>();

    /**
     * User tier definitions
     */
    public enum UserTier {
        FREE("Free", 100, 10, 1000, 10000),
        BASIC("Basic", 500, 50, 5000, 50000),
        PREMIUM("Premium", 2000, 200, 20000, 200000),
        ENTERPRISE("Enterprise", 10000, 1000, 100000, 1000000),
        UNLIMITED("Unlimited", Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

        private final String displayName;
        private final int requestsPerMinute;
        private final int burstCapacity;
        private final int dailyQuota;
        private final int monthlyQuota;

        UserTier(String displayName, int requestsPerMinute, int burstCapacity, int dailyQuota, int monthlyQuota) {
            this.displayName = displayName;
            this.requestsPerMinute = requestsPerMinute;
            this.burstCapacity = burstCapacity;
            this.dailyQuota = dailyQuota;
            this.monthlyQuota = monthlyQuota;
        }

        public String getDisplayName() { return displayName; }
        public int getRequestsPerMinute() { return requestsPerMinute; }
        public int getBurstCapacity() { return burstCapacity; }
        public int getDailyQuota() { return dailyQuota; }
        public int getMonthlyQuota() { return monthlyQuota; }
    }

    /**
     * Rate limiting decision
     */
    public enum RateLimitDecision {
        ALLOWED, RATE_LIMITED, QUOTA_EXCEEDED, GRACE_PERIOD
    }

    /**
     * Rate limit profile for a user
     */
    public static class RateLimitProfile {
        private final String userId;
        private final UserTier tier;
        private final Map<String, Integer> customLimits;
        private final Instant createdAt;
        private volatile Instant lastUpdated;
        private volatile boolean gracePeriodActive;
        private volatile Instant gracePeriodExpiry;

        public RateLimitProfile(String userId, UserTier tier) {
            this.userId = userId;
            this.tier = tier;
            this.customLimits = new HashMap<>();
            this.createdAt = Instant.now();
            this.lastUpdated = Instant.now();
            this.gracePeriodActive = false;
        }

        public String getUserId() { return userId; }
        public UserTier getTier() { return tier; }
        public Map<String, Integer> getCustomLimits() { return customLimits; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
        public boolean isGracePeriodActive() { return gracePeriodActive; }
        public void setGracePeriodActive(boolean gracePeriodActive) { this.gracePeriodActive = gracePeriodActive; }
        public Instant getGracePeriodExpiry() { return gracePeriodExpiry; }
        public void setGracePeriodExpiry(Instant gracePeriodExpiry) { this.gracePeriodExpiry = gracePeriodExpiry; }

        public void addCustomLimit(String endpoint, int limit) {
            customLimits.put(endpoint, limit);
            lastUpdated = Instant.now();
        }

        public boolean isInGracePeriod() {
            return gracePeriodActive && gracePeriodExpiry != null && Instant.now().isBefore(gracePeriodExpiry);
        }

        public int getEffectiveLimit(String endpoint, int defaultLimit) {
            return customLimits.getOrDefault(endpoint, defaultLimit);
        }
    }

    /**
     * Usage statistics for a user
     */
    public static class UsageStats {
        private final String userId;
        private final Map<String, Long> dailyUsage;
        private final Map<String, Long> monthlyUsage;
        private final Map<String, Long> totalUsage;
        private volatile Instant lastResetDaily;
        private volatile Instant lastResetMonthly;

        public UsageStats(String userId) {
            this.userId = userId;
            this.dailyUsage = new ConcurrentHashMap<>();
            this.monthlyUsage = new ConcurrentHashMap<>();
            this.totalUsage = new ConcurrentHashMap<>();
            this.lastResetDaily = Instant.now();
            this.lastResetMonthly = Instant.now();
        }

        public String getUserId() { return userId; }
        public Map<String, Long> getDailyUsage() { return dailyUsage; }
        public Map<String, Long> getMonthlyUsage() { return monthlyUsage; }
        public Map<String, Long> getTotalUsage() { return totalUsage; }
        public Instant getLastResetDaily() { return lastResetDaily; }
        public void setLastResetDaily(Instant lastResetDaily) { this.lastResetDaily = lastResetDaily; }
        public Instant getLastResetMonthly() { return lastResetMonthly; }
        public void setLastResetMonthly(Instant lastResetMonthly) { this.lastResetMonthly = lastResetMonthly; }

        public void recordUsage(String endpoint, long count) {
            dailyUsage.merge(endpoint, count, Long::sum);
            monthlyUsage.merge(endpoint, count, Long::sum);
            totalUsage.merge(endpoint, count, Long::sum);
        }

        public long getDailyUsage(String endpoint) {
            return dailyUsage.getOrDefault(endpoint, 0L);
        }

        public long getMonthlyUsage(String endpoint) {
            return monthlyUsage.getOrDefault(endpoint, 0L);
        }

        public long getTotalUsage(String endpoint) {
            return totalUsage.getOrDefault(endpoint, 0L);
        }

        public void resetDailyUsage() {
            dailyUsage.clear();
            lastResetDaily = Instant.now();
        }

        public void resetMonthlyUsage() {
            monthlyUsage.clear();
            lastResetMonthly = Instant.now();
        }
    }

    /**
     * Rate limit result
     */
    public static class RateLimitResult {
        private final RateLimitDecision decision;
        private final String userId;
        private final String endpoint;
        private final UserTier tier;
        private final long currentUsage;
        private final long limit;
        private final long remainingQuota;
        private final Duration retryAfter;
        private final Map<String, Object> metadata;

        public RateLimitResult(RateLimitDecision decision, String userId, String endpoint, 
                              UserTier tier, long currentUsage, long limit, 
                              long remainingQuota, Duration retryAfter) {
            this.decision = decision;
            this.userId = userId;
            this.endpoint = endpoint;
            this.tier = tier;
            this.currentUsage = currentUsage;
            this.limit = limit;
            this.remainingQuota = remainingQuota;
            this.retryAfter = retryAfter;
            this.metadata = new HashMap<>();
        }

        public RateLimitDecision getDecision() { return decision; }
        public String getUserId() { return userId; }
        public String getEndpoint() { return endpoint; }
        public UserTier getTier() { return tier; }
        public long getCurrentUsage() { return currentUsage; }
        public long getLimit() { return limit; }
        public long getRemainingQuota() { return remainingQuota; }
        public Duration getRetryAfter() { return retryAfter; }
        public Map<String, Object> getMetadata() { return metadata; }

        public boolean isAllowed() {
            return decision == RateLimitDecision.ALLOWED || decision == RateLimitDecision.GRACE_PERIOD;
        }

        public void addMetadata(String key, Object value) {
            metadata.put(key, value);
        }
    }

    /**
     * Initialize default rate limit profiles
     */
    public void initializeDefaultProfiles() {
        // Initialize user tiers
        for (UserTier tier : UserTier.values()) {
            userTiers.put(tier.name(), tier);
        }

        log.info("Initialized {} user tiers", userTiers.size());
    }

    /**
     * Set user tier
     */
    public Mono<Void> setUserTier(String userId, UserTier tier) {
        return Mono.fromRunnable(() -> {
            RateLimitProfile profile = new RateLimitProfile(userId, tier);
            rateLimitProfiles.put(userId, profile);
            
            log.info("Set user tier: userId={}, tier={}", userId, tier.getDisplayName());
        });
    }

    /**
     * Get user tier
     */
    public Mono<UserTier> getUserTier(String userId) {
        return Mono.fromCallable(() -> {
            RateLimitProfile profile = rateLimitProfiles.get(userId);
            if (profile != null) {
                return profile.getTier();
            }
            return UserTier.valueOf(defaultTier);
        });
    }

    /**
     * Check rate limit for user request
     */
    public Mono<RateLimitResult> checkRateLimit(String userId, String endpoint, String method) {
        if (!rateLimitingEnabled) {
            return Mono.just(new RateLimitResult(
                RateLimitDecision.ALLOWED, userId, endpoint, 
                UserTier.UNLIMITED, 0, Integer.MAX_VALUE, 
                Integer.MAX_VALUE, Duration.ZERO
            ));
        }

        return Mono.fromCallable(() -> {
            // Get user profile
            RateLimitProfile profile = rateLimitProfiles.computeIfAbsent(
                userId, 
                id -> new RateLimitProfile(id, UserTier.valueOf(defaultTier))
            );

            UserTier tier = profile.getTier();
            
            // Get usage statistics
            UsageStats stats = usageStatistics.computeIfAbsent(userId, UsageStats::new);
            
            // Reset usage counters if needed
            resetUsageCountersIfNeeded(stats);
            
            // Check rate limits
            String endpointKey = normalizeEndpoint(endpoint);
            return evaluateRateLimit(profile, stats, endpointKey, method);
        });
    }

    /**
     * Evaluate rate limit for request
     */
    private RateLimitResult evaluateRateLimit(RateLimitProfile profile, UsageStats stats, 
                                            String endpoint, String method) {
        UserTier tier = profile.getTier();
        String userId = profile.getUserId();
        
        // Check daily quota
        long dailyUsage = stats.getDailyUsage(endpoint);
        if (dailyUsage >= tier.getDailyQuota()) {
            if (profile.isInGracePeriod()) {
                return createGracePeriodResult(profile, stats, endpoint, tier);
            }
            return createQuotaExceededResult(profile, stats, endpoint, tier, "daily");
        }
        
        // Check monthly quota
        long monthlyUsage = stats.getMonthlyUsage(endpoint);
        if (monthlyUsage >= tier.getMonthlyQuota()) {
            if (profile.isInGracePeriod()) {
                return createGracePeriodResult(profile, stats, endpoint, tier);
            }
            return createQuotaExceededResult(profile, stats, endpoint, tier, "monthly");
        }
        
        // Check rate limit (requests per minute)
        String rateLimitKey = "rate_limit:" + userId + ":" + endpoint;
        int effectiveLimit = profile.getEffectiveLimit(endpoint, tier.getRequestsPerMinute());
        
        return checkRedisRateLimit(rateLimitKey, effectiveLimit, tier.getBurstCapacity())
                .map(allowed -> {
                    if (allowed) {
                        // Record usage
                        stats.recordUsage(endpoint, 1);
                        
                        // Calculate remaining quota
                        long remainingDaily = tier.getDailyQuota() - dailyUsage - 1;
                        long remainingMonthly = tier.getMonthlyQuota() - monthlyUsage - 1;
                        long remainingQuota = Math.min(remainingDaily, remainingMonthly);
                        
                        // Record metrics
                        metricsCollectorService.recordRequest(endpoint, method, 0, 200);
                        
                        return new RateLimitResult(
                            RateLimitDecision.ALLOWED, userId, endpoint, tier,
                            dailyUsage + 1, tier.getDailyQuota(), remainingQuota, Duration.ZERO
                        );
                    } else {
                        // Record rate limit hit
                        metricsCollectorService.recordRateLimitHit(userId, endpoint, tier.name());
                        
                        Duration retryAfter = Duration.ofMinutes(1);
                        return new RateLimitResult(
                            RateLimitDecision.RATE_LIMITED, userId, endpoint, tier,
                            dailyUsage, tier.getDailyQuota(), 0, retryAfter
                        );
                    }
                })
                .block(); // This is acceptable for rate limiting checks
    }

    /**
     * Create grace period result
     */
    private RateLimitResult createGracePeriodResult(RateLimitProfile profile, UsageStats stats, 
                                                   String endpoint, UserTier tier) {
        // Allow request but mark as grace period
        stats.recordUsage(endpoint, 1);
        
        RateLimitResult result = new RateLimitResult(
            RateLimitDecision.GRACE_PERIOD, profile.getUserId(), endpoint, tier,
            stats.getDailyUsage(endpoint), tier.getDailyQuota(), 0, Duration.ZERO
        );
        
        result.addMetadata("gracePeriodExpiry", profile.getGracePeriodExpiry());
        return result;
    }

    /**
     * Create quota exceeded result
     */
    private RateLimitResult createQuotaExceededResult(RateLimitProfile profile, UsageStats stats, 
                                                     String endpoint, UserTier tier, String quotaType) {
        Duration retryAfter = "daily".equals(quotaType) ? 
            Duration.ofHours(24) : Duration.ofDays(30);
        
        RateLimitResult result = new RateLimitResult(
            RateLimitDecision.QUOTA_EXCEEDED, profile.getUserId(), endpoint, tier,
            stats.getDailyUsage(endpoint), tier.getDailyQuota(), 0, retryAfter
        );
        
        result.addMetadata("quotaType", quotaType);
        return result;
    }

    /**
     * Check rate limit using Redis
     */
    private Mono<Boolean> checkRedisRateLimit(String key, int limit, int burstCapacity) {
        List<String> keys = Arrays.asList(key);
        List<String> args = Arrays.asList(
            String.valueOf(limit),
            String.valueOf(burstCapacity),
            String.valueOf(60), // 1 minute window
            String.valueOf(System.currentTimeMillis())
        );

        RedisScript<List<Long>> script = RedisScript.of(
            "local key = KEYS[1]\n" +
            "local rate = tonumber(ARGV[1])\n" +
            "local burst = tonumber(ARGV[2])\n" +
            "local window = tonumber(ARGV[3])\n" +
            "local current_time = tonumber(ARGV[4])\n" +
            "\n" +
            "local current = redis.call('GET', key)\n" +
            "if current == false then\n" +
            "    current = 0\n" +
            "else\n" +
            "    current = tonumber(current)\n" +
            "end\n" +
            "\n" +
            "if current < rate then\n" +
            "    redis.call('INCR', key)\n" +
            "    redis.call('EXPIRE', key, window)\n" +
            "    return {1, current + 1, rate - current - 1}\n" +
            "else\n" +
            "    return {0, current, 0}\n" +
            "end",
            List.class
        );

        return redisTemplate.execute(script, keys, args.toArray(new String[0]))
                .map(result -> result.get(0) == 1L)
                .onErrorReturn(false);
    }

    /**
     * Normalize endpoint for rate limiting
     */
    private String normalizeEndpoint(String endpoint) {
        if (endpoint == null) return "unknown";
        
        // Remove version numbers and IDs
        endpoint = endpoint.replaceAll("/v\\d+", "");
        endpoint = endpoint.replaceAll("/[0-9a-fA-F-]{36}", "/{id}");
        endpoint = endpoint.replaceAll("/\\d+", "/{id}");
        
        // Group similar endpoints
        if (endpoint.startsWith("/api/v1/auth")) return "/auth";
        if (endpoint.startsWith("/api/v1/llm")) return "/llm";
        if (endpoint.startsWith("/api/v1/debates")) return "/debates";
        if (endpoint.startsWith("/api/v1/organizations")) return "/organizations";
        if (endpoint.startsWith("/api/v1/rag")) return "/rag";
        
        return endpoint;
    }

    /**
     * Reset usage counters if needed
     */
    private void resetUsageCountersIfNeeded(UsageStats stats) {
        Instant now = Instant.now();
        
        // Reset daily counters if a day has passed
        if (Duration.between(stats.getLastResetDaily(), now).toDays() >= 1) {
            stats.resetDailyUsage();
        }
        
        // Reset monthly counters if a month has passed
        if (Duration.between(stats.getLastResetMonthly(), now).toDays() >= 30) {
            stats.resetMonthlyUsage();
        }
    }

    /**
     * Activate grace period for user
     */
    public Mono<Void> activateGracePeriod(String userId, Duration duration) {
        return Mono.fromRunnable(() -> {
            RateLimitProfile profile = rateLimitProfiles.get(userId);
            if (profile != null) {
                profile.setGracePeriodActive(true);
                profile.setGracePeriodExpiry(Instant.now().plus(duration));
                
                log.info("Activated grace period for user: userId={}, duration={}", userId, duration);
            }
        });
    }

    /**
     * Deactivate grace period for user
     */
    public Mono<Void> deactivateGracePeriod(String userId) {
        return Mono.fromRunnable(() -> {
            RateLimitProfile profile = rateLimitProfiles.get(userId);
            if (profile != null) {
                profile.setGracePeriodActive(false);
                profile.setGracePeriodExpiry(null);
                
                log.info("Deactivated grace period for user: userId={}", userId);
            }
        });
    }

    /**
     * Get usage statistics for user
     */
    public Mono<UsageStats> getUserUsageStats(String userId) {
        return Mono.fromCallable(() -> {
            UsageStats stats = usageStatistics.get(userId);
            if (stats != null) {
                resetUsageCountersIfNeeded(stats);
            }
            return stats;
        });
    }

    /**
     * Get rate limiting statistics
     */
    public Mono<Map<String, Object>> getRateLimitingStatistics() {
        return Mono.fromCallable(() -> {
            Map<String, Object> stats = new HashMap<>();
            
            // User tier distribution
            Map<UserTier, Long> tierDistribution = rateLimitProfiles.values().stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        RateLimitProfile::getTier,
                        java.util.stream.Collectors.counting()
                    ));
            
            stats.put("tierDistribution", tierDistribution);
            stats.put("totalUsers", rateLimitProfiles.size());
            stats.put("activeGracePeriods", rateLimitProfiles.values().stream()
                    .mapToLong(p -> p.isInGracePeriod() ? 1 : 0)
                    .sum());
            
            // Usage statistics
            long totalRequests = usageStatistics.values().stream()
                    .flatMap(usage -> usage.getTotalUsage().values().stream())
                    .mapToLong(Long::longValue)
                    .sum();
            
            stats.put("totalRequests", totalRequests);
            stats.put("rateLimitingEnabled", rateLimitingEnabled);
            
            return stats;
        });
    }

    /**
     * Set custom rate limit for user endpoint
     */
    public Mono<Void> setCustomRateLimit(String userId, String endpoint, int limit) {
        return Mono.fromRunnable(() -> {
            RateLimitProfile profile = rateLimitProfiles.get(userId);
            if (profile != null) {
                profile.addCustomLimit(endpoint, limit);
                log.info("Set custom rate limit: userId={}, endpoint={}, limit={}", userId, endpoint, limit);
            }
        });
    }

    /**
     * Clear usage statistics for user
     */
    public Mono<Void> clearUserUsage(String userId) {
        return Mono.fromRunnable(() -> {
            UsageStats stats = usageStatistics.get(userId);
            if (stats != null) {
                stats.resetDailyUsage();
                stats.resetMonthlyUsage();
                log.info("Cleared usage statistics for user: userId={}", userId);
            }
        });
    }
}