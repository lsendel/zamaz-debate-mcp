package com.zamaz.mcp.sidecar.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Advanced Rate Limiting Configuration for MCP Sidecar
 * 
 * Implements multiple rate limiting strategies:
 * - User-based rate limiting
 * - IP-based rate limiting
 * - API key-based rate limiting
 * - Path-based rate limiting
 * - Organization-based rate limiting
 * - Dynamic rate limiting based on user roles
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class RateLimitingConfig {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    /**
     * Primary rate limiter for general requests
     */
    @Bean
    @Primary
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(
                10, // replenishRate - tokens per second
                20, // burstCapacity - maximum tokens
                1   // requestedTokens - tokens per request
        );
    }

    /**
     * High-throughput rate limiter for premium users
     */
    @Bean("premiumRateLimiter")
    public RedisRateLimiter premiumRateLimiter() {
        return new RedisRateLimiter(
                50, // replenishRate - 50 tokens per second
                100, // burstCapacity - 100 tokens
                1    // requestedTokens
        );
    }

    /**
     * Strict rate limiter for AI/LLM endpoints
     */
    @Bean("aiRateLimiter")
    public RedisRateLimiter aiRateLimiter() {
        return new RedisRateLimiter(
                5,  // replenishRate - 5 tokens per second
                10, // burstCapacity - 10 tokens
                1   // requestedTokens
        );
    }

    /**
     * Lenient rate limiter for read-only operations
     */
    @Bean("readOnlyRateLimiter")
    public RedisRateLimiter readOnlyRateLimiter() {
        return new RedisRateLimiter(
                30, // replenishRate - 30 tokens per second
                60, // burstCapacity - 60 tokens
                1   // requestedTokens
        );
    }

    /**
     * User-based key resolver
     */
    @Bean("userKeyResolver")
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
            if (userId != null) {
                return Mono.just("user:" + userId);
            }
            
            // Fall back to IP if no user ID
            String clientIp = getClientIp(exchange);
            return Mono.just("ip:" + clientIp);
        };
    }

    /**
     * IP-based key resolver
     */
    @Bean("ipKeyResolver")
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String clientIp = getClientIp(exchange);
            return Mono.just("ip:" + clientIp);
        };
    }

    /**
     * API key-based resolver
     */
    @Bean("apiKeyResolver")
    public KeyResolver apiKeyResolver() {
        return exchange -> {
            String apiKey = exchange.getRequest().getHeaders().getFirst("X-API-Key");
            if (apiKey != null) {
                return Mono.just("apikey:" + apiKey);
            }
            
            // Fall back to user-based if no API key
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
            if (userId != null) {
                return Mono.just("user:" + userId);
            }
            
            // Final fallback to IP
            String clientIp = getClientIp(exchange);
            return Mono.just("ip:" + clientIp);
        };
    }

    /**
     * Path-based key resolver for different endpoints
     */
    @Bean("pathKeyResolver")
    public KeyResolver pathKeyResolver() {
        return exchange -> {
            String path = exchange.getRequest().getPath().value();
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
            
            if (userId != null) {
                return Mono.just("user:" + userId + ":path:" + normalizePathForRateLimit(path));
            }
            
            String clientIp = getClientIp(exchange);
            return Mono.just("ip:" + clientIp + ":path:" + normalizePathForRateLimit(path));
        };
    }

    /**
     * Organization-based key resolver
     */
    @Bean("organizationKeyResolver")
    public KeyResolver organizationKeyResolver() {
        return exchange -> {
            String orgId = exchange.getRequest().getHeaders().getFirst("X-Organization-ID");
            if (orgId != null) {
                return Mono.just("org:" + orgId);
            }
            
            // Fall back to user-based
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
            if (userId != null) {
                return Mono.just("user:" + userId);
            }
            
            String clientIp = getClientIp(exchange);
            return Mono.just("ip:" + clientIp);
        };
    }

    /**
     * Role-based key resolver with dynamic limits
     */
    @Bean("roleBasedKeyResolver")
    public KeyResolver roleBasedKeyResolver() {
        return exchange -> {
            String userRole = exchange.getRequest().getHeaders().getFirst("X-User-Role");
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
            
            if (userId != null && userRole != null) {
                return Mono.just("role:" + userRole + ":user:" + userId);
            }
            
            if (userId != null) {
                return Mono.just("user:" + userId);
            }
            
            String clientIp = getClientIp(exchange);
            return Mono.just("ip:" + clientIp);
        };
    }

    /**
     * Composite key resolver that combines multiple factors
     */
    @Bean("compositeKeyResolver")
    public KeyResolver compositeKeyResolver() {
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");
            String orgId = exchange.getRequest().getHeaders().getFirst("X-Organization-ID");
            String path = exchange.getRequest().getPath().value();
            
            StringBuilder keyBuilder = new StringBuilder();
            
            if (userId != null) {
                keyBuilder.append("user:").append(userId);
            } else {
                String clientIp = getClientIp(exchange);
                keyBuilder.append("ip:").append(clientIp);
            }
            
            if (orgId != null) {
                keyBuilder.append(":org:").append(orgId);
            }
            
            keyBuilder.append(":path:").append(normalizePathForRateLimit(path));
            
            return Mono.just(keyBuilder.toString());
        };
    }

    /**
     * Custom rate limiter for burst protection
     */
    @Bean("burstProtectionRateLimiter")
    public CustomBurstProtectionRateLimiter burstProtectionRateLimiter() {
        return new CustomBurstProtectionRateLimiter(redisTemplate);
    }

    /**
     * Get client IP address from request
     */
    private String getClientIp(org.springframework.web.server.ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return exchange.getRequest().getRemoteAddress() != null 
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() 
                : "unknown";
    }

    /**
     * Normalize path for rate limiting (remove IDs, focus on endpoints)
     */
    private String normalizePathForRateLimit(String path) {
        if (path == null) return "unknown";
        
        // Remove version numbers and IDs
        path = path.replaceAll("/v\\d+/", "/");
        path = path.replaceAll("/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "/{id}");
        path = path.replaceAll("/\\d+", "/{id}");
        
        // Group similar operations
        if (path.contains("/auth/")) return "/auth";
        if (path.contains("/llm/")) return "/llm";
        if (path.contains("/debates/")) return "/debates";
        if (path.contains("/organizations/")) return "/organizations";
        if (path.contains("/rag/")) return "/rag";
        
        return path;
    }

    /**
     * Custom burst protection rate limiter
     */
    public static class CustomBurstProtectionRateLimiter {
        private final ReactiveRedisTemplate<String, String> redisTemplate;
        private final RedisScript<List<Long>> script;

        public CustomBurstProtectionRateLimiter(ReactiveRedisTemplate<String, String> redisTemplate) {
            this.redisTemplate = redisTemplate;
            this.script = RedisScript.of(
                    "local key = KEYS[1]\n" +
                    "local window = tonumber(ARGV[1])\n" +
                    "local limit = tonumber(ARGV[2])\n" +
                    "local burst_limit = tonumber(ARGV[3])\n" +
                    "local current_time = tonumber(ARGV[4])\n" +
                    "\n" +
                    "local current_count = redis.call('GET', key)\n" +
                    "if current_count == false then\n" +
                    "    current_count = 0\n" +
                    "else\n" +
                    "    current_count = tonumber(current_count)\n" +
                    "end\n" +
                    "\n" +
                    "local burst_key = key .. ':burst'\n" +
                    "local burst_count = redis.call('GET', burst_key)\n" +
                    "if burst_count == false then\n" +
                    "    burst_count = 0\n" +
                    "else\n" +
                    "    burst_count = tonumber(burst_count)\n" +
                    "end\n" +
                    "\n" +
                    "if current_count >= limit or burst_count >= burst_limit then\n" +
                    "    return {0, current_count, burst_count}\n" +
                    "end\n" +
                    "\n" +
                    "redis.call('INCR', key)\n" +
                    "redis.call('EXPIRE', key, window)\n" +
                    "redis.call('INCR', burst_key)\n" +
                    "redis.call('EXPIRE', burst_key, 60)\n" +
                    "\n" +
                    "return {1, current_count + 1, burst_count + 1}",
                    List.class
            );
        }

        public Mono<Boolean> isAllowed(String key, int windowSeconds, int limit, int burstLimit) {
            long currentTime = System.currentTimeMillis() / 1000;
            
            return redisTemplate.execute(script, 
                    Arrays.asList(key), 
                    String.valueOf(windowSeconds),
                    String.valueOf(limit),
                    String.valueOf(burstLimit),
                    String.valueOf(currentTime))
                    .map(results -> {
                        if (results != null && !results.isEmpty()) {
                            Long allowed = results.get(0);
                            Long currentCount = results.get(1);
                            Long burstCount = results.get(2);
                            
                            log.debug("Rate limit check: key={}, allowed={}, current={}, burst={}", 
                                    key, allowed, currentCount, burstCount);
                            
                            return allowed == 1;
                        }
                        return false;
                    })
                    .onErrorReturn(false);
        }
    }
}