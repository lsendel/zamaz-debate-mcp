package com.zamaz.mcp.context.adapter.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zamaz.mcp.common.architecture.adapter.external.ExternalServiceAdapter;
import com.zamaz.mcp.context.application.port.outbound.ContextCacheService;
import com.zamaz.mcp.context.domain.model.*;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis implementation of context caching service.
 * This is an external service adapter in hexagonal architecture.
 */
@Component
public class RedisContextCacheService implements ContextCacheService, ExternalServiceAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisContextCacheService.class);
    private static final String WINDOW_KEY_PREFIX = "context:window:";
    private static final String METADATA_KEY_PREFIX = "context:metadata:";
    private static final String ORG_KEY_PREFIX = "org:";
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    public RedisContextCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }
    
    @Override
    public void cacheWindow(String key, ContextWindow window, Duration ttl) {
        try {
            String fullKey = WINDOW_KEY_PREFIX + key;
            ContextWindowCache cacheObject = ContextWindowCache.fromDomain(window);
            String jsonValue = objectMapper.writeValueAsString(cacheObject);
            
            redisTemplate.opsForValue().set(fullKey, jsonValue, ttl);
            
            logger.debug("Cached context window with key: {} for {} seconds", 
                fullKey, ttl.getSeconds());
        } catch (JsonProcessingException e) {
            logger.error("Failed to cache context window for key: {}", key, e);
        }
    }
    
    @Override
    public Optional<ContextWindow> getCachedWindow(String key) {
        try {
            String fullKey = WINDOW_KEY_PREFIX + key;
            Object cachedValue = redisTemplate.opsForValue().get(fullKey);
            
            if (cachedValue == null) {
                logger.debug("Cache miss for context window key: {}", fullKey);
                return Optional.empty();
            }
            
            ContextWindowCache cacheObject = objectMapper.readValue(
                cachedValue.toString(), ContextWindowCache.class
            );
            
            ContextWindow window = cacheObject.toDomain();
            
            logger.debug("Cache hit for context window key: {}", fullKey);
            return Optional.of(window);
        } catch (Exception e) {
            logger.error("Failed to retrieve cached context window for key: {}", key, e);
            return Optional.empty();
        }
    }
    
    @Override
    public void cacheMetadata(ContextId contextId, ContextMetadata metadata) {
        try {
            String key = METADATA_KEY_PREFIX + contextId.asString();
            String jsonValue = objectMapper.writeValueAsString(metadata.asMap());
            
            redisTemplate.opsForValue().set(key, jsonValue, Duration.ofHours(1));
            
            logger.debug("Cached metadata for context: {}", contextId);
        } catch (JsonProcessingException e) {
            logger.error("Failed to cache metadata for context: {}", contextId, e);
        }
    }
    
    @Override
    public Optional<ContextMetadata> getCachedMetadata(ContextId contextId) {
        try {
            String key = METADATA_KEY_PREFIX + contextId.asString();
            Object cachedValue = redisTemplate.opsForValue().get(key);
            
            if (cachedValue == null) {
                return Optional.empty();
            }
            
            @SuppressWarnings("unchecked")
            var metadataMap = objectMapper.readValue(
                cachedValue.toString(), 
                objectMapper.getTypeFactory().constructMapType(
                    java.util.Map.class, 
                    String.class, 
                    Object.class
                )
            );
            
            ContextMetadata metadata = ContextMetadata.of(metadataMap);
            
            logger.debug("Retrieved cached metadata for context: {}", contextId);
            return Optional.of(metadata);
        } catch (Exception e) {
            logger.error("Failed to retrieve cached metadata for context: {}", contextId, e);
            return Optional.empty();
        }
    }
    
    @Override
    public void evictContext(ContextId contextId) {
        try {
            String pattern = "*:" + contextId.asString() + ":*";
            var keys = redisTemplate.keys(pattern);
            
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.debug("Evicted {} cache entries for context: {}", keys.size(), contextId);
            }
            
            // Also evict metadata
            String metadataKey = METADATA_KEY_PREFIX + contextId.asString();
            redisTemplate.delete(metadataKey);
            
        } catch (Exception e) {
            logger.error("Failed to evict cache entries for context: {}", contextId, e);
        }
    }
    
    @Override
    public void evictOrganization(String organizationId) {
        try {
            String pattern = "*" + ORG_KEY_PREFIX + organizationId + "*";
            var keys = redisTemplate.keys(pattern);
            
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.debug("Evicted {} cache entries for organization: {}", keys.size(), organizationId);
            }
        } catch (Exception e) {
            logger.error("Failed to evict cache entries for organization: {}", organizationId, e);
        }
    }
    
    @Override
    public void clearAll() {
        try {
            String windowPattern = WINDOW_KEY_PREFIX + "*";
            String metadataPattern = METADATA_KEY_PREFIX + "*";
            
            var windowKeys = redisTemplate.keys(windowPattern);
            var metadataKeys = redisTemplate.keys(metadataPattern);
            
            int deletedCount = 0;
            if (windowKeys != null && !windowKeys.isEmpty()) {
                redisTemplate.delete(windowKeys);
                deletedCount += windowKeys.size();
            }
            
            if (metadataKeys != null && !metadataKeys.isEmpty()) {
                redisTemplate.delete(metadataKeys);
                deletedCount += metadataKeys.size();
            }
            
            logger.info("Cleared {} context cache entries", deletedCount);
        } catch (Exception e) {
            logger.error("Failed to clear context cache", e);
        }
    }
    
    @Override
    public String generateWindowCacheKey(ContextId contextId, int maxTokens, int maxMessages) {
        return String.format("%s:%d:%d", contextId.asString(), maxTokens, maxMessages);
    }
    
    /**
     * Cache-specific DTO for storing ContextWindow in Redis.
     */
    private static class ContextWindowCache {
        public String contextId;
        public java.util.List<MessageSnapshot> messages;
        public int totalTokens;
        public int messageCount;
        
        public static ContextWindowCache fromDomain(ContextWindow window) {
            ContextWindowCache cache = new ContextWindowCache();
            cache.contextId = window.getContextId().asString();
            cache.messages = window.toSnapshots();
            cache.totalTokens = window.getTotalTokens().value();
            cache.messageCount = window.getMessageCount();
            return cache;
        }
        
        public ContextWindow toDomain() {
            var domainMessages = messages.stream()
                .map(MessageSnapshot::toMessage)
                .toList();
            
            return ContextWindow.of(
                ContextId.from(contextId),
                domainMessages,
                TokenCount.of(totalTokens)
            );
        }
        
        // Default constructor for Jackson
        public ContextWindowCache() {}
    }
}