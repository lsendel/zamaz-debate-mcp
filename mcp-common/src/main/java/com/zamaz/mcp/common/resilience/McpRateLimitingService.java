package com.zamaz.mcp.common.resilience;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing MCP rate limiting operations.
 * Provides multi-tenant, context-aware rate limiting for MCP tools.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class McpRateLimitingService {

    private final RateLimiterRegistry rateLimiterRegistry;
    private final McpRateLimitingConfiguration rateLimitingConfig;
    
    // Cache for rate limiter instances to avoid recreation
    private final Map<String, io.github.resilience4j.ratelimiter.RateLimiter> rateLimiterCache = new ConcurrentHashMap<>();

    /**
     * Get or create a rate limiter for a specific MCP context.
     *
     * @param serviceName Name of the MCP service
     * @param toolName Name of the MCP tool
     * @param authentication Authentication context
     * @return Configured rate limiter instance
     */
    public io.github.resilience4j.ratelimiter.RateLimiter getRateLimiter(
            String serviceName, 
            String toolName, 
            Authentication authentication) {
        
        String rateLimiterKey = createRateLimiterKey(serviceName, toolName, authentication);
        
        return rateLimiterCache.computeIfAbsent(rateLimiterKey, key -> {
            McpRateLimitingConfiguration.EffectiveRateLimits effectiveLimits = 
                calculateEffectiveLimits(serviceName, toolName, authentication);
            
            log.debug("Creating rate limiter '{}' with limits: {} req/{} sec", 
                     key, effectiveLimits.getLimitForPeriod(), effectiveLimits.getLimitRefreshPeriodSeconds());
            
            return createRateLimiter(key, effectiveLimits);
        });
    }

    /**
     * Check if a request is permitted by the rate limiter.
     *
     * @param serviceName Name of the MCP service
     * @param toolName Name of the MCP tool
     * @param authentication Authentication context
     * @return true if request is permitted, false if rate limited
     */
    public boolean isRequestPermitted(String serviceName, String toolName, Authentication authentication) {
        if (!rateLimitingConfig.isEnabled()) {
            return true;
        }

        io.github.resilience4j.ratelimiter.RateLimiter rateLimiter = getRateLimiter(serviceName, toolName, authentication);
        return rateLimiter.acquirePermission();
    }

    /**
     * Get current rate limit status for a context.
     *
     * @param serviceName Name of the MCP service
     * @param toolName Name of the MCP tool
     * @param authentication Authentication context
     * @return Rate limit status information
     */
    public RateLimitStatus getRateLimitStatus(String serviceName, String toolName, Authentication authentication) {
        io.github.resilience4j.ratelimiter.RateLimiter rateLimiter = getRateLimiter(serviceName, toolName, authentication);
        
        io.github.resilience4j.ratelimiter.RateLimiter.Metrics metrics = rateLimiter.getMetrics();
        
        return RateLimitStatus.builder()
            .serviceName(serviceName)
            .toolName(toolName)
            .limitForPeriod(rateLimiter.getRateLimiterConfig().getLimitForPeriod())
            .limitRefreshPeriod(rateLimiter.getRateLimiterConfig().getLimitRefreshPeriod())
            .availablePermissions(metrics.getAvailablePermissions())
            .numberOfWaitingThreads(metrics.getNumberOfWaitingThreads())
            .build();
    }

    /**
     * Get rate limit status for all active rate limiters for a user/organization.
     *
     * @param authentication Authentication context
     * @return Map of rate limiter keys to their status
     */
    public Map<String, RateLimitStatus> getAllRateLimitStatus(Authentication authentication) {
        Map<String, RateLimitStatus> statusMap = new ConcurrentHashMap<>();
        
        String userContext = extractUserContext(authentication);
        String orgContext = extractOrganizationContext(authentication);
        
        rateLimiterCache.entrySet().stream()
            .filter(entry -> {
                String key = entry.getKey();
                return key.contains(userContext) || (orgContext != null && key.contains(orgContext));
            })
            .forEach(entry -> {
                String key = entry.getKey();
                io.github.resilience4j.ratelimiter.RateLimiter rateLimiter = entry.getValue();
                
                // Extract service and tool names from key
                String[] parts = key.split(":");
                String serviceName = parts.length > 0 ? parts[0] : "unknown";
                String toolName = parts.length > 1 ? parts[1] : "unknown";
                
                io.github.resilience4j.ratelimiter.RateLimiter.Metrics metrics = rateLimiter.getMetrics();
                
                RateLimitStatus status = RateLimitStatus.builder()
                    .serviceName(serviceName)
                    .toolName(toolName)
                    .limitForPeriod(rateLimiter.getRateLimiterConfig().getLimitForPeriod())
                    .limitRefreshPeriod(rateLimiter.getRateLimiterConfig().getLimitRefreshPeriod())
                    .availablePermissions(metrics.getAvailablePermissions())
                    .numberOfWaitingThreads(metrics.getNumberOfWaitingThreads())
                    .build();
                
                statusMap.put(key, status);
            });
        
        return statusMap;
    }

    /**
     * Reset rate limiters for a specific context (admin operation).
     *
     * @param serviceName Name of the MCP service (optional)
     * @param toolName Name of the MCP tool (optional)
     * @param authentication Authentication context
     */
    public void resetRateLimiters(String serviceName, String toolName, Authentication authentication) {
        String pattern = createRateLimiterKeyPattern(serviceName, toolName, authentication);
        
        rateLimiterCache.entrySet().removeIf(entry -> {
            String key = entry.getKey();
            boolean matches = matchesPattern(key, pattern);
            
            if (matches) {
                log.info("Resetting rate limiter: {}", key);
            }
            
            return matches;
        });
    }

    /**
     * Create a unique rate limiter key for the given context.
     */
    private String createRateLimiterKey(String serviceName, String toolName, Authentication authentication) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(serviceName).append(":").append(toolName);
        
        // Add organization context if available and enabled
        if (rateLimitingConfig.isOrganizationLevelLimiting()) {
            String orgId = extractOrganizationContext(authentication);
            if (orgId != null) {
                keyBuilder.append(":org:").append(orgId);
            }
        }
        
        // Add user context if enabled
        if (rateLimitingConfig.isUserLevelLimiting() && authentication != null) {
            String userId = extractUserContext(authentication);
            keyBuilder.append(":user:").append(userId);
        }
        
        // Add IP context for unauthenticated requests
        if (rateLimitingConfig.isIpLevelLimiting() && authentication == null) {
            keyBuilder.append(":ip:").append(getClientIp());
        }
        
        return keyBuilder.toString();
    }

    /**
     * Calculate effective rate limits for the given context.
     */
    private McpRateLimitingConfiguration.EffectiveRateLimits calculateEffectiveLimits(
            String serviceName, 
            String toolName, 
            Authentication authentication) {
        
        String userTier = extractUserTier(authentication);
        String organizationTier = extractOrganizationTier(authentication);
        
        return rateLimitingConfig.getEffectiveRateLimits(serviceName, toolName, userTier, organizationTier);
    }

    /**
     * Create a rate limiter with the specified configuration.
     */
    private io.github.resilience4j.ratelimiter.RateLimiter createRateLimiter(
            String name, 
            McpRateLimitingConfiguration.EffectiveRateLimits effectiveLimits) {
        
        io.github.resilience4j.ratelimiter.RateLimiterConfig config = 
            io.github.resilience4j.ratelimiter.RateLimiterConfig.custom()
                .limitForPeriod(effectiveLimits.getLimitForPeriod())
                .limitRefreshPeriod(Duration.ofSeconds(effectiveLimits.getLimitRefreshPeriodSeconds()))
                .timeoutDuration(Duration.ofSeconds(effectiveLimits.getTimeoutDurationSeconds()))
                .build();
        
        return rateLimiterRegistry.rateLimiter(name, config);
    }

    /**
     * Extract user context from authentication.
     */
    private String extractUserContext(Authentication authentication) {
        return authentication != null ? authentication.getName() : "anonymous";
    }

    /**
     * Extract organization context from authentication.
     */
    private String extractOrganizationContext(Authentication authentication) {
        if (authentication != null && authentication.getAuthorities() != null) {
            return authentication.getAuthorities().stream()
                .filter(auth -> auth.getAuthority().startsWith("ORG_"))
                .findFirst()
                .map(orgAuth -> orgAuth.getAuthority().substring(4)) // Remove "ORG_" prefix
                .orElse(null);
        }
        return null;
    }

    /**
     * Extract user tier from authentication.
     */
    private String extractUserTier(Authentication authentication) {
        if (authentication != null && authentication.getAuthorities() != null) {
            return authentication.getAuthorities().stream()
                .filter(auth -> auth.getAuthority().startsWith("TIER_"))
                .findFirst()
                .map(tierAuth -> tierAuth.getAuthority().substring(5).toLowerCase()) // Remove "TIER_" prefix
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
                .map(orgTierAuth -> orgTierAuth.getAuthority().substring(9).toLowerCase()) // Remove "ORG_TIER_" prefix
                .orElse("free");
        }
        return "free";
    }

    /**
     * Get client IP address (placeholder - would need HttpServletRequest in real implementation).
     */
    private String getClientIp() {
        // This is a placeholder. In a real implementation, you would extract 
        // the IP from HttpServletRequest or use a ThreadLocal pattern
        return "127.0.0.1";
    }

    /**
     * Create a pattern for matching rate limiter keys.
     */
    private String createRateLimiterKeyPattern(String serviceName, String toolName, Authentication authentication) {
        StringBuilder pattern = new StringBuilder();
        
        if (serviceName != null) {
            pattern.append(serviceName);
        } else {
            pattern.append("*");
        }
        
        pattern.append(":");
        
        if (toolName != null) {
            pattern.append(toolName);
        } else {
            pattern.append("*");
        }
        
        // Add user/org context
        String userContext = extractUserContext(authentication);
        String orgContext = extractOrganizationContext(authentication);
        
        if (orgContext != null) {
            pattern.append(":org:").append(orgContext);
        }
        
        if (userContext != null) {
            pattern.append("*").append(userContext).append("*");
        }
        
        return pattern.toString();
    }

    /**
     * Check if a key matches a pattern (simple wildcard matching).
     */
    private boolean matchesPattern(String key, String pattern) {
        if (pattern.equals("*")) {
            return true;
        }
        
        if (!pattern.contains("*")) {
            return key.equals(pattern);
        }
        
        // Simple wildcard matching
        String[] patternParts = pattern.split("\\*");
        int index = 0;
        
        for (String part : patternParts) {
            if (part.isEmpty()) continue;
            
            int foundIndex = key.indexOf(part, index);
            if (foundIndex == -1) {
                return false;
            }
            index = foundIndex + part.length();
        }
        
        return true;
    }

    /**
     * Rate limit status information.
     */
    @lombok.Data
    @lombok.Builder
    public static class RateLimitStatus {
        private String serviceName;
        private String toolName;
        private int limitForPeriod;
        private Duration limitRefreshPeriod;
        private int availablePermissions;
        private int numberOfWaitingThreads;
        
        public boolean isThrottled() {
            return availablePermissions <= 0 || numberOfWaitingThreads > 0;
        }
        
        public double getUsagePercentage() {
            return ((double) (limitForPeriod - availablePermissions) / limitForPeriod) * 100;
        }
    }
}