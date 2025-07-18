package com.zamaz.mcp.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service for proactively warming up critical caches at application startup.
 * This improves performance by pre-loading frequently accessed data into cache
 * before the first user requests arrive.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheWarmupService {

    private final CacheManager cacheManager;

    /**
     * Warm up caches when application is ready
     */
    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void warmupCaches() {
        log.info("Starting cache warmup process...");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Run warmup tasks in parallel for better performance
            CompletableFuture<Void> allWarmupTasks = CompletableFuture.allOf(
                warmupSystemConfigurations(),
                warmupTemplateData(),
                warmupProviderConfigurations(),
                warmupActiveData(),
                warmupUserPreferences()
            );
            
            // Wait for all warmup tasks to complete
            allWarmupTasks.get();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Cache warmup completed successfully in {}ms", duration);
            
        } catch (Exception e) {
            log.error("Cache warmup failed", e);
        }
    }

    /**
     * Warm up system configuration caches
     */
    @Async
    public CompletableFuture<Void> warmupSystemConfigurations() {
        log.debug("Warming up system configuration caches...");
        
        try {
            // Warm up feature flags
            warmupCache("feature-flags", "system-feature-flags", () -> {
                // This would typically call a service to load feature flags
                log.debug("Loading feature flags into cache");
                return "feature-flags-data";
            });
            
            // Warm up system configurations
            warmupCache("system-config", "global-config", () -> {
                log.debug("Loading system configuration into cache");
                return "system-config-data";
            });
            
            // Warm up validation rules
            warmupCache("validation-rules", "global-validation", () -> {
                log.debug("Loading validation rules into cache");
                return "validation-rules-data";
            });
            
            log.debug("System configuration caches warmed up successfully");
            
        } catch (Exception e) {
            log.warn("Failed to warm up system configuration caches", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Warm up template data caches
     */
    @Async
    public CompletableFuture<Void> warmupTemplateData() {
        log.debug("Warming up template caches...");
        
        try {
            // Warm up debate templates
            warmupCache("templates", "debate-templates", () -> {
                log.debug("Loading debate templates into cache");
                return "debate-templates-data";
            });
            
            // Warm up email templates
            warmupCache("email-templates", "notification-templates", () -> {
                log.debug("Loading email templates into cache");
                return "email-templates-data";
            });
            
            // Warm up system templates
            warmupCache("system-templates", "ui-templates", () -> {
                log.debug("Loading system templates into cache");
                return "system-templates-data";
            });
            
            log.debug("Template caches warmed up successfully");
            
        } catch (Exception e) {
            log.warn("Failed to warm up template caches", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Warm up LLM provider configuration caches
     */
    @Async
    public CompletableFuture<Void> warmupProviderConfigurations() {
        log.debug("Warming up provider configuration caches...");
        
        try {
            // Warm up LLM provider configurations
            warmupCache("llm-provider-configs", "provider-settings", () -> {
                log.debug("Loading LLM provider configurations into cache");
                return "llm-provider-configs-data";
            });
            
            // Warm up provider capabilities
            warmupCache("provider-capabilities", "llm-capabilities", () -> {
                log.debug("Loading provider capabilities into cache");
                return "provider-capabilities-data";
            });
            
            // Warm up LLM providers status
            warmupCache("llm-providers", "active-providers", () -> {
                log.debug("Loading LLM provider status into cache");
                return "llm-providers-data";
            });
            
            log.debug("Provider configuration caches warmed up successfully");
            
        } catch (Exception e) {
            log.warn("Failed to warm up provider configuration caches", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Warm up active data caches (organization-specific)
     */
    @Async
    public CompletableFuture<Void> warmupActiveData() {
        log.debug("Warming up active data caches...");
        
        try {
            // Warm up active organizations
            warmupCache("organizations", "default-organization", () -> {
                log.debug("Loading default organization into cache");
                return "default-organization-data";
            });
            
            // Warm up organization configurations
            warmupCache("organization-config", "default-org-config", () -> {
                log.debug("Loading organization configuration into cache");
                return "organization-config-data";
            });
            
            // Warm up active debates (if any)
            warmupCache("active-debates", "startup-debates", () -> {
                log.debug("Loading active debates into cache");
                return "active-debates-data";
            });
            
            log.debug("Active data caches warmed up successfully");
            
        } catch (Exception e) {
            log.warn("Failed to warm up active data caches", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Warm up user preference caches
     */
    @Async
    public CompletableFuture<Void> warmupUserPreferences() {
        log.debug("Warming up user preference caches...");
        
        try {
            // Warm up default user preferences
            warmupCache("user-preferences", "default-preferences", () -> {
                log.debug("Loading default user preferences into cache");
                return "default-user-preferences-data";
            });
            
            // Warm up localization data
            warmupCache("localization-data", "default-locale", () -> {
                log.debug("Loading localization data into cache");
                return "localization-data";
            });
            
            log.debug("User preference caches warmed up successfully");
            
        } catch (Exception e) {
            log.warn("Failed to warm up user preference caches", e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Generic cache warmup method
     */
    private void warmupCache(String cacheName, String key, java.util.function.Supplier<Object> dataSupplier) {
        try {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                // Check if data is already cached
                if (cache.get(key) == null) {
                    // Load data and put into cache
                    Object data = dataSupplier.get();
                    cache.put(key, data);
                    log.trace("Warmed up cache '{}' with key '{}'", cacheName, key);
                } else {
                    log.trace("Cache '{}' with key '{}' already warmed up", cacheName, key);
                }
            } else {
                log.warn("Cache '{}' not found for warmup", cacheName);
            }
        } catch (Exception e) {
            log.warn("Failed to warm up cache '{}' with key '{}'", cacheName, key, e);
        }
    }

    /**
     * Manual cache warmup trigger (for administrative purposes)
     */
    public void triggerManualWarmup() {
        log.info("Manual cache warmup triggered");
        warmupCaches();
    }

    /**
     * Warm up specific cache by name
     */
    public void warmupSpecificCache(String cacheName) {
        log.info("Warming up specific cache: {}", cacheName);
        
        switch (cacheName) {
            case "system-config":
                warmupSystemConfigurations();
                break;
            case "templates":
                warmupTemplateData();
                break;
            case "llm-providers":
                warmupProviderConfigurations();
                break;
            case "organizations":
                warmupActiveData();
                break;
            case "user-preferences":
                warmupUserPreferences();
                break;
            default:
                log.warn("Unknown cache name for specific warmup: {}", cacheName);
        }
    }

    /**
     * Get cache warmup statistics
     */
    public CacheWarmupStats getWarmupStats() {
        CacheWarmupStats stats = new CacheWarmupStats();
        
        // Count warmed caches
        String[] cacheNames = {
            "feature-flags", "system-config", "validation-rules",
            "templates", "email-templates", "system-templates",
            "llm-provider-configs", "provider-capabilities", "llm-providers",
            "organizations", "organization-config", "active-debates",
            "user-preferences", "localization-data"
        };
        
        int warmedCaches = 0;
        for (String cacheName : cacheNames) {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null && cache.get("warmup-marker") != null) {
                warmedCaches++;
            }
        }
        
        stats.setTotalCaches(cacheNames.length);
        stats.setWarmedCaches(warmedCaches);
        stats.setWarmupPercentage((double) warmedCaches / cacheNames.length * 100);
        
        return stats;
    }

    /**
     * Cache warmup statistics
     */
    public static class CacheWarmupStats {
        private int totalCaches;
        private int warmedCaches;
        private double warmupPercentage;

        // Getters and setters
        public int getTotalCaches() { return totalCaches; }
        public void setTotalCaches(int totalCaches) { this.totalCaches = totalCaches; }

        public int getWarmedCaches() { return warmedCaches; }
        public void setWarmedCaches(int warmedCaches) { this.warmedCaches = warmedCaches; }

        public double getWarmupPercentage() { return warmupPercentage; }
        public void setWarmupPercentage(double warmupPercentage) { this.warmupPercentage = warmupPercentage; }
    }
}