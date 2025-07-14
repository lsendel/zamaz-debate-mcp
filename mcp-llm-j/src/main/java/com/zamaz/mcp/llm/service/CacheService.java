package com.zamaz.mcp.llm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.llm.config.LlmProperties;
import com.zamaz.mcp.llm.model.CompletionRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {
    
    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final LlmProperties llmProperties;
    
    private static final String CACHE_PREFIX = "llm:completion:";
    
    public String generateCacheKey(CompletionRequest request) {
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(requestJson.getBytes());
            return CACHE_PREFIX + HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("Error generating cache key", e);
            return CACHE_PREFIX + request.hashCode();
        }
    }
    
    public <T> Mono<T> get(String key, Class<T> type) {
        if (!llmProperties.getCache().isEnabled()) {
            return Mono.empty();
        }
        
        return redisTemplate.opsForValue().get(key)
                .map(value -> objectMapper.convertValue(value, type))
                .doOnNext(value -> log.debug("Cache hit for key: {}", key))
                .onErrorResume(error -> {
                    log.error("Error reading from cache", error);
                    return Mono.empty();
                });
    }
    
    public Mono<Void> put(String key, Object value) {
        if (!llmProperties.getCache().isEnabled()) {
            return Mono.empty();
        }
        
        return redisTemplate.opsForValue()
                .set(key, value, llmProperties.getCache().getTtl())
                .then()
                .doOnSuccess(v -> log.debug("Cached value for key: {}", key))
                .onErrorResume(error -> {
                    log.error("Error writing to cache", error);
                    return Mono.empty();
                });
    }
}