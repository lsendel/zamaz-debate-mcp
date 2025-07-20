package com.zamaz.mcp.controller.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configuration for API rate limiting to prevent abuse.
 * Implements a sliding window rate limiter with different limits for different endpoints.
 */
@Configuration
@Slf4j
public class RateLimitingConfig implements WebMvcConfigurer {

    @Value("${agentic-flow.rate-limit.default-requests-per-minute:60}")
    private int defaultRequestsPerMinute;

    @Value("${agentic-flow.rate-limit.execution-requests-per-minute:20}")
    private int executionRequestsPerMinute;

    @Value("${agentic-flow.rate-limit.creation-requests-per-minute:10}")
    private int creationRequestsPerMinute;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RateLimitInterceptor())
                .addPathPatterns("/api/v1/agentic-flows/**")
                .addPathPatterns("/api/v1/debates/*/agentic-flow/**");
    }

    /**
     * Rate limiting interceptor that enforces request limits per user.
     */
    private class RateLimitInterceptor implements HandlerInterceptor {
        
        // Cache for tracking request counts per user
        private final LoadingCache<String, UserRateLimit> rateLimitCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(10000)
                .build(key -> new UserRateLimit());

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return true; // Let security filter handle unauthenticated requests
            }

            String userId = authentication.getName();
            String path = request.getRequestURI();
            String method = request.getMethod();
            
            // Determine rate limit based on endpoint
            int limit = getRateLimit(path, method);
            
            UserRateLimit userLimit = rateLimitCache.get(userId);
            if (!userLimit.allowRequest(limit)) {
                log.warn("Rate limit exceeded for user {} on endpoint {}", userId, path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
                response.setHeader("X-RateLimit-Remaining", "0");
                response.setHeader("X-RateLimit-Reset", String.valueOf(userLimit.getResetTime()));
                response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
                return false;
            }

            // Add rate limit headers
            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(userLimit.getRemainingRequests(limit)));
            response.setHeader("X-RateLimit-Reset", String.valueOf(userLimit.getResetTime()));

            return true;
        }

        private int getRateLimit(String path, String method) {
            // Different limits for different operations
            if (path.contains("/execute") && "POST".equals(method)) {
                return executionRequestsPerMinute;
            } else if ("POST".equals(method) || "PUT".equals(method)) {
                return creationRequestsPerMinute;
            } else {
                return defaultRequestsPerMinute;
            }
        }
    }

    /**
     * Tracks rate limit state for a user using a sliding window approach.
     */
    private static class UserRateLimit {
        private final ConcurrentHashMap<Long, AtomicInteger> windowCounts = new ConcurrentHashMap<>();
        private static final long WINDOW_SIZE_MS = 60000; // 1 minute
        private static final long BUCKET_SIZE_MS = 1000; // 1 second buckets

        public boolean allowRequest(int limit) {
            long now = System.currentTimeMillis();
            long currentBucket = now / BUCKET_SIZE_MS;
            
            // Clean up old buckets
            windowCounts.entrySet().removeIf(entry -> 
                entry.getKey() < (currentBucket - (WINDOW_SIZE_MS / BUCKET_SIZE_MS)));

            // Count requests in the current window
            int totalRequests = windowCounts.values().stream()
                    .mapToInt(AtomicInteger::get)
                    .sum();

            if (totalRequests >= limit) {
                return false;
            }

            // Increment counter for current bucket
            windowCounts.computeIfAbsent(currentBucket, k -> new AtomicInteger(0))
                       .incrementAndGet();
            return true;
        }

        public int getRemainingRequests(int limit) {
            long now = System.currentTimeMillis();
            long currentBucket = now / BUCKET_SIZE_MS;
            
            // Clean up old buckets
            windowCounts.entrySet().removeIf(entry -> 
                entry.getKey() < (currentBucket - (WINDOW_SIZE_MS / BUCKET_SIZE_MS)));

            int totalRequests = windowCounts.values().stream()
                    .mapToInt(AtomicInteger::get)
                    .sum();

            return Math.max(0, limit - totalRequests);
        }

        public long getResetTime() {
            // Next window starts after the oldest bucket expires
            if (windowCounts.isEmpty()) {
                return System.currentTimeMillis() + WINDOW_SIZE_MS;
            }
            
            long oldestBucket = windowCounts.keySet().stream()
                    .min(Long::compare)
                    .orElse(System.currentTimeMillis() / BUCKET_SIZE_MS);
            
            return (oldestBucket * BUCKET_SIZE_MS) + WINDOW_SIZE_MS;
        }
    }
}