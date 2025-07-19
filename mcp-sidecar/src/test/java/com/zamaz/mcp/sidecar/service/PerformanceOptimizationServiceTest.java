package com.zamaz.mcp.sidecar.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PerformanceOptimizationService
 */
@ExtendWith(MockitoExtension.class)
class PerformanceOptimizationServiceTest {

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Mock
    private CachingService cachingService;

    @Mock
    private MetricsCollectorService metricsCollectorService;

    @Mock
    private CacheManager cacheManager;

    private PerformanceOptimizationService performanceService;

    @BeforeEach
    void setUp() {
        performanceService = new PerformanceOptimizationService(
            redisTemplate, cachingService, metricsCollectorService, cacheManager
        );
    }

    @Test
    void shouldOptimizePipelineWithRetry() {
        // Given
        Mono<String> source = Mono.just("test-result");
        PerformanceOptimizationService.OptimizationOptions options = 
            PerformanceOptimizationService.OptimizationOptions.builder()
                .withRetry(3)
                .withTimeout(Duration.ofSeconds(5))
                .withMetricsKey("test");

        // When
        Mono<String> optimized = performanceService.optimizePipeline(source, options);

        // Then
        StepVerifier.create(optimized)
            .expectNext("test-result")
            .verifyComplete();
    }

    @Test
    void shouldOptimizePipelineWithCache() {
        // Given
        String cacheKey = "test-key";
        String cachedValue = "cached-result";
        
        when(cachingService.cacheResult(any(), any(), any()))
            .thenReturn(Mono.just(cachedValue));

        Mono<String> source = Mono.just("test-result");
        PerformanceOptimizationService.OptimizationOptions options = 
            PerformanceOptimizationService.OptimizationOptions.builder()
                .withCache(cacheKey, Duration.ofMinutes(5))
                .withMetricsKey("test");

        // When
        Mono<String> optimized = performanceService.optimizePipeline(source, options);

        // Then
        StepVerifier.create(optimized)
            .expectNext(cachedValue)
            .verifyComplete();
    }

    @Test
    void shouldCompressDataWhenBeneficial() {
        // Given
        String largeData = "a".repeat(2000);
        byte[] dataBytes = largeData.getBytes();

        // When
        Mono<byte[]> compressed = performanceService.compressData(dataBytes);

        // Then
        StepVerifier.create(compressed)
            .assertNext(compressedData -> {
                // Compressed data should be smaller for repetitive content
                assertThat(compressedData.length).isLessThan(dataBytes.length);
            })
            .verifyComplete();
    }

    @Test
    void shouldNotCompressSmallData() {
        // Given
        String smallData = "small";
        byte[] dataBytes = smallData.getBytes();

        // When
        Mono<byte[]> compressed = performanceService.compressData(dataBytes);

        // Then
        StepVerifier.create(compressed)
            .assertNext(result -> {
                // Should return original data for small inputs
                assertThat(result).isEqualTo(dataBytes);
            })
            .verifyComplete();
    }

    @Test
    void shouldDecompressData() {
        // Given
        String originalData = "test data for compression";
        byte[] dataBytes = originalData.getBytes();

        // First compress
        byte[] compressedData = performanceService.compressData(dataBytes).block();

        // When
        Mono<byte[]> decompressed = performanceService.decompressData(compressedData);

        // Then
        StepVerifier.create(decompressed)
            .assertNext(result -> {
                assertThat(new String(result)).isEqualTo(originalData);
            })
            .verifyComplete();
    }

    @Test
    void shouldCreateAndAcquireFromResourcePool() {
        // Given
        performanceService.initialize();

        // When
        ByteBuffer buffer = performanceService.acquireResource(ByteBuffer.class);
        
        // Then
        assertThat(buffer).isNotNull();
        assertThat(buffer.capacity()).isGreaterThan(0);
        
        // Clean up
        performanceService.releaseResource(ByteBuffer.class, buffer);
    }

    @Test
    void shouldCreateAndAcquireStringBuilder() {
        // Given
        performanceService.initialize();

        // When
        StringBuilder sb = performanceService.acquireResource(StringBuilder.class);
        
        // Then
        assertThat(sb).isNotNull();
        assertThat(sb.capacity()).isGreaterThan(0);
        
        // Use and release
        sb.append("test");
        performanceService.releaseResource(StringBuilder.class, sb);
        
        // Acquire again - should be reset
        StringBuilder sb2 = performanceService.acquireResource(StringBuilder.class);
        assertThat(sb2.length()).isEqualTo(0);
    }

    @Test
    void shouldGetAllMetrics() {
        // Given
        performanceService.initialize();

        // When
        Mono<Map<String, Object>> metrics = performanceService.getAllMetrics();

        // Then
        StepVerifier.create(metrics)
            .assertNext(allMetrics -> {
                assertThat(allMetrics).containsKeys(
                    "performanceMetrics",
                    "connectionPools",
                    "resourcePools",
                    "bulkheads",
                    "deduplicationCache"
                );
            })
            .verifyComplete();
    }

    @Test
    void shouldResetMetrics() {
        // Given
        performanceService.initialize();

        // When
        Mono<Void> reset = performanceService.resetMetrics();

        // Then
        StepVerifier.create(reset)
            .verifyComplete();
    }

    @Test
    void shouldCreateBulkhead() {
        // Given
        performanceService.initialize();
        String bulkheadName = "test-bulkhead";
        int maxConcurrentCalls = 10;
        Duration maxWaitDuration = Duration.ofSeconds(5);

        // When
        performanceService.createBulkhead(bulkheadName, maxConcurrentCalls, maxWaitDuration);

        // Then - bulkhead should be created and usable
        Mono<String> operation = Mono.just("test");
        Mono<String> withBulkhead = performanceService.executeWithBulkhead(bulkheadName, operation);
        
        StepVerifier.create(withBulkhead)
            .expectNext("test")
            .verifyComplete();
    }

    @Test
    void shouldExecuteWithoutBulkheadIfNotFound() {
        // Given
        performanceService.initialize();
        Mono<String> operation = Mono.just("test");

        // When
        Mono<String> withBulkhead = performanceService.executeWithBulkhead("non-existent", operation);

        // Then
        StepVerifier.create(withBulkhead)
            .expectNext("test")
            .verifyComplete();
    }

    @Test
    void shouldBuildOptimizationOptions() {
        // When
        PerformanceOptimizationService.OptimizationOptions options = 
            PerformanceOptimizationService.OptimizationOptions.builder()
                .withRetry(5)
                .withTimeout(Duration.ofSeconds(10))
                .withCache("cache-key", Duration.ofMinutes(10))
                .withBulkhead("test-bulkhead")
                .asIoOperation()
                .withMetricsKey("test-metrics");

        // Then
        assertThat(options.isRetryEnabled()).isTrue();
        assertThat(options.getMaxRetries()).isEqualTo(5);
        assertThat(options.getTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(options.isCacheEnabled()).isTrue();
        assertThat(options.getCacheKey()).isEqualTo("cache-key");
        assertThat(options.getCacheTtl()).isEqualTo(Duration.ofMinutes(10));
        assertThat(options.getBulkheadName()).isEqualTo("test-bulkhead");
        assertThat(options.isIoOperation()).isTrue();
        assertThat(options.getMetricsKey()).isEqualTo("test-metrics");
    }

    @Test
    void shouldTrackPerformanceMetrics() {
        // Given
        PerformanceOptimizationService.PerformanceMetrics metrics = 
            new PerformanceOptimizationService.PerformanceMetrics();

        // When
        metrics.recordRequest(100);
        metrics.recordRequest(200);
        metrics.recordCacheHit();
        metrics.recordCacheMiss();
        metrics.recordBatchedRequest();
        metrics.recordDedupedRequest();
        metrics.recordCompressionSavings(1000);

        // Then
        assertThat(metrics.getAverageLatency()).isEqualTo(150.0);
        assertThat(metrics.getCacheHitRate()).isEqualTo(0.5);
        
        Map<String, Object> metricsMap = metrics.toMap();
        assertThat(metricsMap.get("totalRequests")).isEqualTo(2L);
        assertThat(metricsMap.get("cacheHits")).isEqualTo(1L);
        assertThat(metricsMap.get("cacheMisses")).isEqualTo(1L);
        assertThat(metricsMap.get("batchedRequests")).isEqualTo(1L);
        assertThat(metricsMap.get("dedupedRequests")).isEqualTo(1L);
        assertThat(metricsMap.get("compressionSavings")).isEqualTo(1000L);
    }

    @Test
    void shouldResetPerformanceMetrics() {
        // Given
        PerformanceOptimizationService.PerformanceMetrics metrics = 
            new PerformanceOptimizationService.PerformanceMetrics();
        metrics.recordRequest(100);
        metrics.recordCacheHit();

        // When
        metrics.reset();

        // Then
        assertThat(metrics.getAverageLatency()).isEqualTo(0.0);
        assertThat(metrics.getCacheHitRate()).isEqualTo(0.0);
        assertThat(metrics.toMap().get("totalRequests")).isEqualTo(0L);
    }

    @Test
    void shouldManageConnectionPool() {
        // Given
        PerformanceOptimizationService.ConnectionPool pool = 
            new PerformanceOptimizationService.ConnectionPool(
                "test-pool",
                5,
                Duration.ofMinutes(1),
                v -> Mono.just("connection")
            );

        // When
        Mono<PerformanceOptimizationService.PooledConnection> connectionMono = pool.acquire();

        // Then
        StepVerifier.create(connectionMono)
            .assertNext(connection -> {
                assertThat(connection).isNotNull();
                assertThat(connection.getConnection()).isEqualTo("connection");
                assertThat(connection.isExpired()).isFalse();
                
                // Release connection
                pool.release(connection);
            })
            .verifyComplete();

        // Verify pool stats
        Map<String, Object> stats = pool.getStats();
        assertThat(stats.get("name")).isEqualTo("test-pool");
        assertThat(stats.get("maxSize")).isEqualTo(5);
    }

    @Test
    void shouldManageResourcePool() {
        // Given
        PerformanceOptimizationService.ResourcePool<StringBuilder> pool = 
            new PerformanceOptimizationService.ResourcePool<>(
                "test-pool",
                3,
                () -> new StringBuilder(),
                sb -> sb.setLength(0)
            );

        // When
        StringBuilder sb1 = pool.acquire();
        sb1.append("test");
        pool.release(sb1);

        StringBuilder sb2 = pool.acquire();

        // Then
        assertThat(sb2).isNotNull();
        assertThat(sb2.length()).isEqualTo(0); // Should be reset

        Map<String, Object> stats = pool.getStats();
        assertThat(stats.get("name")).isEqualTo("test-pool");
        assertThat(stats.get("maxSize")).isEqualTo(3);
    }
}