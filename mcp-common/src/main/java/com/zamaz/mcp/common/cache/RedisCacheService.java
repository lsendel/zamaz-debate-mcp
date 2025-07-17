package com.zamaz.mcp.common.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis implementation of the CacheService interface.
 *
 * @param <K> The type of the cache key
 * @param <V> The type of the cached value
 */
@Slf4j
@RequiredArgsConstructor
public class RedisCacheService<K, V> implements CacheService<K, V> {

    private final RedisTemplate<K, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Class<V> valueType;
    private final Duration defaultTtl;
    private final String cachePrefix;

    /**
     * Get a value from the cache.
     *
     * @param key The cache key
     * @return The cached value, or empty if not found
     */
    @Override
    public Optional<V> get(K key) {
        try {
            String value = redisTemplate.opsForValue().get(getPrefixedKey(key));
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, valueType));
        } catch (JsonProcessingException e) {
            log.error("Error deserializing cached value for key: {}", key, e);
            return Optional.empty();
        }
    }

    /**
     * Put a value in the cache.
     *
     * @param key The cache key
     * @param value The value to cache
     */
    @Override
    public void put(K key, V value) {
        put(key, value, defaultTtl);
    }

    /**
     * Put a value in the cache with a specific time-to-live.
     *
     * @param key The cache key
     * @param value The value to cache
     * @param ttl The time-to-live for the cached value
     */
    @Override
    public void put(K key, V value, Duration ttl) {
        try {
            String serializedValue = objectMapper.writeValueAsString(value);
            K prefixedKey = getPrefixedKey(key);
            if (ttl != null) {
                redisTemplate.opsForValue().set(prefixedKey, serializedValue, ttl);
            } else {
                redisTemplate.opsForValue().set(prefixedKey, serializedValue);
            }
        } catch (JsonProcessingException e) {
            log.error("Error serializing value for key: {}", key, e);
        }
    }

    /**
     * Remove a value from the cache.
     *
     * @param key The cache key
     * @return true if the value was removed, false otherwise
     */
    @Override
    public boolean remove(K key) {
        Boolean result = redisTemplate.delete(getPrefixedKey(key));
        return Boolean.TRUE.equals(result);
    }

    /**
     * Check if a key exists in the cache.
     *
     * @param key The cache key
     * @return true if the key exists, false otherwise
     */
    @Override
    public boolean exists(K key) {
        Boolean result = redisTemplate.hasKey(getPrefixedKey(key));
        return Boolean.TRUE.equals(result);
    }

    /**
     * Clear all entries from the cache with this prefix.
     */
    @Override
    public void clear() {
        // This is a potentially expensive operation, use with caution
        redisTemplate.keys(cachePrefix + "*").forEach(redisTemplate::delete);
    }

    /**
     * Get a value from the cache, or compute it if not present.
     *
     * @param key The cache key
     * @param supplier A function to compute the value if not in cache
     * @return The cached or computed value
     */
    @Override
    public V getOrCompute(K key, CacheValueSupplier<V> supplier) {
        return getOrCompute(key, supplier, defaultTtl);
    }

    /**
     * Get a value from the cache, or compute it if not present, with a specific TTL.
     *
     * @param key The cache key
     * @param supplier A function to compute the value if not in cache
     * @param ttl The time-to-live for the cached value
     * @return The cached or computed value
     */
    @Override
    public V getOrCompute(K key, CacheValueSupplier<V> supplier, Duration ttl) {
        Optional<V> cachedValue = get(key);
        if (cachedValue.isPresent()) {
            log.debug("Cache hit for key: {}", key);
            return cachedValue.get();
        }

        log.debug("Cache miss for key: {}", key);
        V computedValue = supplier.get();
        put(key, computedValue, ttl);
        return computedValue;
    }

    /**
     * Add prefix to cache key to avoid collisions between different services.
     *
     * @param key The original key
     * @return The prefixed key
     */
    @SuppressWarnings("unchecked")
    private K getPrefixedKey(K key) {
        if (key instanceof String) {
            return (K) (cachePrefix + key);
        }
        return key;
    }
}
