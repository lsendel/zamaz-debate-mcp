package com.zamaz.mcp.sidecar.controller;

import com.zamaz.mcp.sidecar.service.PerformanceOptimizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * Performance Controller for MCP Sidecar
 * 
 * Provides REST endpoints for performance monitoring and optimization
 */
@RestController
@RequestMapping("/api/v1/performance")
@RequiredArgsConstructor
@Slf4j
public class PerformanceController {

    private final PerformanceOptimizationService performanceService;

    /**
     * Get all performance metrics
     */
    @GetMapping("/metrics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, Object>>> getPerformanceMetrics() {
        return performanceService.getAllMetrics()
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("Performance metrics requested"))
                .onErrorResume(error -> {
                    log.error("Error getting performance metrics: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Reset performance metrics
     */
    @PostMapping("/metrics/reset")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS')")
    public Mono<ResponseEntity<Map<String, String>>> resetPerformanceMetrics() {
        return performanceService.resetMetrics()
                .then(Mono.just(ResponseEntity.ok(Map.of(
                    "status", "Performance metrics reset",
                    "timestamp", java.time.Instant.now().toString()
                ))))
                .doOnSuccess(response -> log.info("Performance metrics reset"))
                .onErrorResume(error -> {
                    log.error("Error resetting performance metrics: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Create bulkhead
     */
    @PostMapping("/bulkheads")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS')")
    public Mono<ResponseEntity<Map<String, String>>> createBulkhead(@RequestBody CreateBulkheadRequest request) {
        return Mono.fromRunnable(() -> {
            performanceService.createBulkhead(
                request.getName(),
                request.getMaxConcurrentCalls(),
                Duration.parse(request.getMaxWaitDuration())
            );
        })
        .then(Mono.just(ResponseEntity.ok(Map.of(
            "status", "Bulkhead created",
            "name", request.getName(),
            "maxConcurrentCalls", String.valueOf(request.getMaxConcurrentCalls())
        ))))
        .doOnSuccess(response -> log.info("Bulkhead created: {}", request.getName()))
        .onErrorResume(error -> {
            log.error("Error creating bulkhead: {}", error.getMessage());
            return Mono.just(ResponseEntity.badRequest().build());
        });
    }

    /**
     * Compress data
     */
    @PostMapping("/compress")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('USER')")
    public Mono<ResponseEntity<CompressResponse>> compressData(@RequestBody CompressRequest request) {
        byte[] data = request.getData().getBytes();
        
        return performanceService.compressData(data)
                .map(compressed -> {
                    CompressResponse response = new CompressResponse(
                        java.util.Base64.getEncoder().encodeToString(compressed),
                        data.length,
                        compressed.length,
                        compressed.length < data.length
                    );
                    return ResponseEntity.ok(response);
                })
                .doOnSuccess(response -> log.debug("Data compression requested"))
                .onErrorResume(error -> {
                    log.error("Error compressing data: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Decompress data
     */
    @PostMapping("/decompress")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('USER')")
    public Mono<ResponseEntity<Map<String, String>>> decompressData(@RequestBody DecompressRequest request) {
        byte[] compressedData = java.util.Base64.getDecoder().decode(request.getCompressedData());
        
        return performanceService.decompressData(compressedData)
                .map(decompressed -> ResponseEntity.ok(Map.of(
                    "data", new String(decompressed),
                    "originalSize", String.valueOf(compressedData.length),
                    "decompressedSize", String.valueOf(decompressed.length)
                )))
                .doOnSuccess(response -> log.debug("Data decompression requested"))
                .onErrorResume(error -> {
                    log.error("Error decompressing data: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Execute optimized operation
     */
    @PostMapping("/optimize")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('USER')")
    public Mono<ResponseEntity<Map<String, Object>>> executeOptimized(@RequestBody OptimizeRequest request) {
        // Build optimization options
        PerformanceOptimizationService.OptimizationOptions options = 
            PerformanceOptimizationService.OptimizationOptions.builder();

        if (request.isRetryEnabled()) {
            options.withRetry(request.getMaxRetries());
        }

        if (request.getTimeout() != null) {
            options.withTimeout(Duration.parse(request.getTimeout()));
        }

        if (request.isCacheEnabled() && request.getCacheKey() != null) {
            options.withCache(request.getCacheKey(), Duration.parse(request.getCacheTtl()));
        }

        if (request.getBulkheadName() != null) {
            options.withBulkhead(request.getBulkheadName());
        }

        if (request.isIoOperation()) {
            options.asIoOperation();
        }

        if (request.getMetricsKey() != null) {
            options.withMetricsKey(request.getMetricsKey());
        }

        // Simulate an operation to optimize
        Mono<String> operation = Mono.just("Optimized result")
                .delayElement(Duration.ofMillis(100));

        return performanceService.optimizePipeline(operation, options)
                .map(result -> Map.of(
                    "result", result,
                    "optimizationApplied", true,
                    "timestamp", java.time.Instant.now().toString()
                ))
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("Optimized operation executed"))
                .onErrorResume(error -> {
                    log.error("Error executing optimized operation: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Test request batching
     */
    @PostMapping("/batch/test")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS')")
    public Mono<ResponseEntity<Map<String, Object>>> testBatching(@RequestBody BatchTestRequest request) {
        return performanceService.executeWithBatching(request.getBatcherName(), request.getKey())
                .map(result -> Map.of(
                    "result", result,
                    "batcherName", request.getBatcherName(),
                    "key", request.getKey()
                ))
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("Batch test executed"))
                .onErrorResume(error -> {
                    log.error("Error testing batching: {}", error.getMessage());
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * Test request deduplication
     */
    @PostMapping("/dedup/test")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS')")
    public Mono<ResponseEntity<Map<String, Object>>> testDeduplication(@RequestBody DedupTestRequest request) {
        // Simulate an expensive operation
        Mono<String> expensiveOperation = Mono.just("Dedup result for " + request.getKey())
                .delayElement(Duration.ofMillis(500));

        return performanceService.executeWithDeduplication(request.getKey(), expensiveOperation)
                .map(result -> Map.of(
                    "result", result,
                    "key", request.getKey(),
                    "deduplicationApplied", true
                ))
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.debug("Deduplication test executed"))
                .onErrorResume(error -> {
                    log.error("Error testing deduplication: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get performance optimization status
     */
    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OPS') or hasRole('MONITOR')")
    public Mono<ResponseEntity<Map<String, Object>>> getOptimizationStatus() {
        return performanceService.getAllMetrics()
                .map(metrics -> {
                    Map<String, Object> status = new java.util.HashMap<>();
                    status.put("optimizationEnabled", true);
                    status.put("metrics", metrics);
                    status.put("timestamp", java.time.Instant.now());
                    
                    // Calculate overall optimization score
                    double score = calculateOptimizationScore(metrics);
                    status.put("optimizationScore", score);
                    status.put("optimizationLevel", getOptimizationLevel(score));
                    
                    return ResponseEntity.ok(status);
                })
                .doOnSuccess(response -> log.debug("Optimization status requested"))
                .onErrorResume(error -> {
                    log.error("Error getting optimization status: {}", error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Calculate optimization score based on metrics
     */
    @SuppressWarnings("unchecked")
    private double calculateOptimizationScore(Map<String, Object> metrics) {
        double score = 0.0;
        int factors = 0;

        // Check performance metrics
        if (metrics.containsKey("performanceMetrics")) {
            Map<String, Map<String, Object>> perfMetrics = 
                (Map<String, Map<String, Object>>) metrics.get("performanceMetrics");
            
            for (Map<String, Object> metric : perfMetrics.values()) {
                // Cache hit rate contributes to score
                Double cacheHitRate = (Double) metric.get("cacheHitRate");
                if (cacheHitRate != null) {
                    score += cacheHitRate * 100;
                    factors++;
                }
                
                // Low average latency contributes to score
                Double avgLatency = (Double) metric.get("averageLatency");
                if (avgLatency != null && avgLatency > 0) {
                    score += Math.min(100, 1000 / avgLatency);
                    factors++;
                }
            }
        }

        // Check connection pool efficiency
        if (metrics.containsKey("connectionPools")) {
            Map<String, Map<String, Object>> poolStats = 
                (Map<String, Map<String, Object>>) metrics.get("connectionPools");
            
            for (Map<String, Object> pool : poolStats.values()) {
                Integer total = (Integer) pool.get("totalConnections");
                Integer inUse = (Integer) pool.get("inUseConnections");
                if (total != null && total > 0 && inUse != null) {
                    double utilization = (double) inUse / total;
                    score += (1 - Math.abs(utilization - 0.7)) * 100; // Optimal at 70% utilization
                    factors++;
                }
            }
        }

        return factors > 0 ? score / factors : 0.0;
    }

    /**
     * Get optimization level based on score
     */
    private String getOptimizationLevel(double score) {
        if (score >= 90) return "EXCELLENT";
        if (score >= 75) return "GOOD";
        if (score >= 50) return "MODERATE";
        if (score >= 25) return "POOR";
        return "CRITICAL";
    }

    /**
     * Request DTOs
     */
    public static class CreateBulkheadRequest {
        private String name;
        private int maxConcurrentCalls;
        private String maxWaitDuration;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getMaxConcurrentCalls() { return maxConcurrentCalls; }
        public void setMaxConcurrentCalls(int maxConcurrentCalls) { this.maxConcurrentCalls = maxConcurrentCalls; }
        public String getMaxWaitDuration() { return maxWaitDuration; }
        public void setMaxWaitDuration(String maxWaitDuration) { this.maxWaitDuration = maxWaitDuration; }
    }

    public static class CompressRequest {
        private String data;

        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
    }

    public static class DecompressRequest {
        private String compressedData;

        public String getCompressedData() { return compressedData; }
        public void setCompressedData(String compressedData) { this.compressedData = compressedData; }
    }

    public static class OptimizeRequest {
        private boolean retryEnabled = true;
        private int maxRetries = 3;
        private String timeout;
        private boolean cacheEnabled = false;
        private String cacheKey;
        private String cacheTtl = "PT5M";
        private String bulkheadName;
        private boolean ioOperation = false;
        private String metricsKey;

        public boolean isRetryEnabled() { return retryEnabled; }
        public void setRetryEnabled(boolean retryEnabled) { this.retryEnabled = retryEnabled; }
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        public String getTimeout() { return timeout; }
        public void setTimeout(String timeout) { this.timeout = timeout; }
        public boolean isCacheEnabled() { return cacheEnabled; }
        public void setCacheEnabled(boolean cacheEnabled) { this.cacheEnabled = cacheEnabled; }
        public String getCacheKey() { return cacheKey; }
        public void setCacheKey(String cacheKey) { this.cacheKey = cacheKey; }
        public String getCacheTtl() { return cacheTtl; }
        public void setCacheTtl(String cacheTtl) { this.cacheTtl = cacheTtl; }
        public String getBulkheadName() { return bulkheadName; }
        public void setBulkheadName(String bulkheadName) { this.bulkheadName = bulkheadName; }
        public boolean isIoOperation() { return ioOperation; }
        public void setIoOperation(boolean ioOperation) { this.ioOperation = ioOperation; }
        public String getMetricsKey() { return metricsKey; }
        public void setMetricsKey(String metricsKey) { this.metricsKey = metricsKey; }
    }

    public static class BatchTestRequest {
        private String batcherName;
        private String key;

        public String getBatcherName() { return batcherName; }
        public void setBatcherName(String batcherName) { this.batcherName = batcherName; }
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
    }

    public static class DedupTestRequest {
        private String key;

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
    }

    /**
     * Response DTOs
     */
    public static class CompressResponse {
        private final String compressedData;
        private final int originalSize;
        private final int compressedSize;
        private final boolean compressed;

        public CompressResponse(String compressedData, int originalSize, int compressedSize, boolean compressed) {
            this.compressedData = compressedData;
            this.originalSize = originalSize;
            this.compressedSize = compressedSize;
            this.compressed = compressed;
        }

        public String getCompressedData() { return compressedData; }
        public int getOriginalSize() { return originalSize; }
        public int getCompressedSize() { return compressedSize; }
        public boolean isCompressed() { return compressed; }
    }
}