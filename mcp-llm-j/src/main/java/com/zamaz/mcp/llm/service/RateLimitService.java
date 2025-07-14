package com.zamaz.mcp.llm.service;

import com.zamaz.mcp.llm.config.LlmProperties;
import com.zamaz.mcp.llm.exception.RateLimitException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {
    
    private final LlmProperties llmProperties;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    public Mono<Void> checkRateLimit(String provider) {
        if (!llmProperties.getRateLimiting().isEnabled()) {
            return Mono.empty();
        }
        
        Bucket bucket = buckets.computeIfAbsent(provider, this::createBucket);
        
        return Mono.fromCallable(() -> bucket.tryConsume(1))
                .flatMap(allowed -> {
                    if (allowed) {
                        return Mono.empty();
                    } else {
                        return Mono.error(new RateLimitException("Rate limit exceeded for provider: " + provider));
                    }
                });
    }
    
    private Bucket createBucket(String provider) {
        Integer limit = llmProperties.getRateLimiting().getProviderLimits().get(provider);
        if (limit == null) {
            limit = llmProperties.getRateLimiting().getDefaultRequestsPerMinute();
        }
        
        Bandwidth bandwidth = Bandwidth.classic(limit, Refill.intervally(limit, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }
}