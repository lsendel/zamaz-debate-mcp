package com.zamaz.mcp.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for warming up caches with frequently accessed data
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheWarmingService {
    
    private final CacheManager cacheManager;
    private final List<CacheWarmer> cacheWarmers = new ArrayList<>();
    
    /**
     * Register a cache warmer
     */
    public void registerWarmer(CacheWarmer warmer) {
        cacheWarmers.add(warmer);
        log.info("Registered cache warmer: {}", warmer.getCacheName());
    }
    
    /**
     * Warm up caches on application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void warmUpCachesOnStartup() {
        log.info("Starting cache warm-up process...");
        
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (CacheWarmer warmer : cacheWarmers) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    log.info("Warming up cache: {}", warmer.getCacheName());
                    warmer.warmUp();
                    log.info("Successfully warmed up cache: {}", warmer.getCacheName());
                } catch (Exception e) {
                    log.error("Failed to warm up cache: {}", warmer.getCacheName(), e);
                }
            });
            futures.add(future);
        }
        
        // Wait for all warmers to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> log.info("Cache warm-up completed"))
            .exceptionally(throwable -> {
                log.error("Cache warm-up failed", throwable);
                return null;
            });
    }
    
    /**
     * Periodically refresh caches
     */
    @Scheduled(fixedDelayString = "${cache.refresh.interval:3600000}") // Default: 1 hour
    public void refreshCaches() {
        log.debug("Refreshing caches...");
        
        for (CacheWarmer warmer : cacheWarmers) {
            if (warmer.shouldRefresh()) {
                try {
                    warmer.refresh();
                    log.debug("Refreshed cache: {}", warmer.getCacheName());
                } catch (Exception e) {
                    log.error("Failed to refresh cache: {}", warmer.getCacheName(), e);
                }
            }
        }
    }
    
    /**
     * Interface for cache warmers
     */
    public interface CacheWarmer {
        /**
         * Get the name of the cache this warmer handles
         */
        String getCacheName();
        
        /**
         * Warm up the cache with initial data
         */
        void warmUp();
        
        /**
         * Refresh cache data
         */
        default void refresh() {
            warmUp();
        }
        
        /**
         * Check if cache should be refreshed
         */
        default boolean shouldRefresh() {
            return true;
        }
    }
}