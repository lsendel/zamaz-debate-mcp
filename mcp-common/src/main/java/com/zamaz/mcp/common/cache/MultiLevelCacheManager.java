package com.zamaz.mcp.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Multi-level cache manager that combines local and distributed caching
 * L1 Cache: Local Caffeine cache for frequently accessed data
 * L2 Cache: Distributed Redis cache for shared data across instances
 */
@RequiredArgsConstructor
@Slf4j
public class MultiLevelCacheManager implements CacheManager {

    private final CacheManager localCacheManager;
    private final CacheManager distributedCacheManager;
    private final ConcurrentMap<String, MultiLevelCache> cacheMap = new ConcurrentHashMap<>();

    @Override
    public Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, key -> {
            Cache localCache = localCacheManager.getCache(name);
            Cache distributedCache = distributedCacheManager.getCache(name);
            
            if (localCache == null || distributedCache == null) {
                log.warn("Unable to create multi-level cache for: {}", name);
                return null;
            }
            
            log.info("Creating multi-level cache for: {}", name);
            return new MultiLevelCache(name, localCache, distributedCache);
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        // Combine cache names from both managers
        Collection<String> localNames = localCacheManager.getCacheNames();
        Collection<String> distributedNames = distributedCacheManager.getCacheNames();
        
        ConcurrentHashMap<String, Boolean> allNames = new ConcurrentHashMap<>();
        localNames.forEach(name -> allNames.put(name, true));
        distributedNames.forEach(name -> allNames.put(name, true));
        
        return allNames.keySet();
    }

    /**
     * Multi-level cache implementation
     */
    @RequiredArgsConstructor
    private static class MultiLevelCache implements Cache {
        private final String name;
        private final Cache localCache;
        private final Cache distributedCache;

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getNativeCache() {
            return this;
        }

        @Override
        public ValueWrapper get(Object key) {
            // Try L1 cache first
            ValueWrapper value = localCache.get(key);
            if (value != null) {
                log.trace("L1 cache hit for key: {}", key);
                return value;
            }

            // Try L2 cache
            value = distributedCache.get(key);
            if (value != null) {
                log.trace("L2 cache hit for key: {}", key);
                // Populate L1 cache
                localCache.put(key, value.get());
                return value;
            }

            log.trace("Cache miss for key: {}", key);
            return null;
        }

        @Override
        public <T> T get(Object key, Class<T> type) {
            ValueWrapper value = get(key);
            if (value != null) {
                Object obj = value.get();
                if (type != null && !type.isInstance(obj)) {
                    throw new IllegalStateException("Cached value is not of required type [" + type.getName() + "]: " + obj);
                }
                return type.cast(obj);
            }
            return null;
        }

        @Override
        public <T> T get(Object key, java.util.concurrent.Callable<T> valueLoader) {
            ValueWrapper value = get(key);
            if (value != null) {
                return (T) value.get();
            }

            try {
                T result = valueLoader.call();
                put(key, result);
                return result;
            } catch (Exception e) {
                throw new ValueRetrievalException(key, valueLoader, e);
            }
        }

        @Override
        public void put(Object key, Object value) {
            // Put in both caches
            localCache.put(key, value);
            distributedCache.put(key, value);
            log.trace("Cached value for key: {}", key);
        }

        @Override
        public void evict(Object key) {
            // Evict from both caches
            localCache.evict(key);
            distributedCache.evict(key);
            log.trace("Evicted key: {}", key);
        }

        @Override
        public void clear() {
            // Clear both caches
            localCache.clear();
            distributedCache.clear();
            log.info("Cleared multi-level cache: {}", name);
        }
    }
}