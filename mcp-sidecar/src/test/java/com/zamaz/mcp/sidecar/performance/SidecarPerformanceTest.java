package com.zamaz.mcp.sidecar.performance;

import com.zamaz.mcp.sidecar.service.SecurityScanningService;
import com.zamaz.mcp.sidecar.service.CachingService;
import com.zamaz.mcp.sidecar.service.MetricsCollectorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitExtension;
import org.springframework.util.StopWatch;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance benchmarks for MCP Sidecar
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(SpringJUnitExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SidecarPerformanceTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private SecurityScanningService securityScanningService;

    @Autowired
    private CachingService cachingService;

    @Autowired
    private MetricsCollectorService metricsCollectorService;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    /**
     * Benchmark authentication performance
     */
    @Test
    void benchmarkAuthenticationPerformance() {
        System.out.println("=== Authentication Performance Benchmark ===");
        
        Map<String, String> loginRequest = Map.of(
            "username", "admin",
            "password", "admin123"
        );

        StopWatch stopWatch = new StopWatch();
        int iterations = 100;
        
        stopWatch.start();
        for (int i = 0; i < iterations; i++) {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/auth/login",
                loginRequest,
                Map.class
            );
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
        stopWatch.stop();
        
        long totalTimeMs = stopWatch.getTotalTimeMillis();
        double avgTimeMs = (double) totalTimeMs / iterations;
        double throughput = (iterations * 1000.0) / totalTimeMs;
        
        System.out.println("Authentication Benchmark Results:");
        System.out.println("Total iterations: " + iterations);
        System.out.println("Total time: " + totalTimeMs + "ms");
        System.out.println("Average time per request: " + String.format("%.2f", avgTimeMs) + "ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " req/sec");
        System.out.println();
        
        // Performance assertions
        assertThat(avgTimeMs).isLessThan(100.0); // Should be under 100ms average
        assertThat(throughput).isGreaterThan(10.0); // Should handle at least 10 req/sec
    }

    /**
     * Benchmark concurrent request handling
     */
    @Test
    void benchmarkConcurrentRequestHandling() throws InterruptedException {
        System.out.println("=== Concurrent Request Handling Benchmark ===");
        
        int concurrentUsers = 50;
        int requestsPerUser = 10;
        int totalRequests = concurrentUsers * requestsPerUser;
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        for (int i = 0; i < concurrentUsers; i++) {
            executor.submit(() -> {
                for (int j = 0; j < requestsPerUser; j++) {
                    try {
                        long startTime = System.currentTimeMillis();
                        
                        ResponseEntity<String> response = restTemplate.getForEntity(
                            baseUrl + "/actuator/health",
                            String.class
                        );
                        
                        long responseTime = System.currentTimeMillis() - startTime;
                        totalResponseTime.addAndGet(responseTime);
                        
                        if (response.getStatusCode() == HttpStatus.OK) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
        
        latch.await(60, TimeUnit.SECONDS);
        stopWatch.stop();
        executor.shutdown();
        
        long totalTimeMs = stopWatch.getTotalTimeMillis();
        double avgResponseTime = (double) totalResponseTime.get() / successCount.get();
        double throughput = (successCount.get() * 1000.0) / totalTimeMs;
        
        System.out.println("Concurrent Request Benchmark Results:");
        System.out.println("Concurrent users: " + concurrentUsers);
        System.out.println("Requests per user: " + requestsPerUser);
        System.out.println("Total requests: " + totalRequests);
        System.out.println("Successful requests: " + successCount.get());
        System.out.println("Failed requests: " + failureCount.get());
        System.out.println("Total time: " + totalTimeMs + "ms");
        System.out.println("Average response time: " + String.format("%.2f", avgResponseTime) + "ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " req/sec");
        System.out.println("Success rate: " + String.format("%.2f", (100.0 * successCount.get() / totalRequests)) + "%");
        System.out.println();
        
        // Performance assertions
        assertThat(successCount.get()).isGreaterThan(totalRequests * 0.95); // 95% success rate
        assertThat(avgResponseTime).isLessThan(200.0); // Average under 200ms
        assertThat(throughput).isGreaterThan(50.0); // At least 50 req/sec
    }

    /**
     * Benchmark security scanning performance
     */
    @Test
    void benchmarkSecurityScanningPerformance() {
        System.out.println("=== Security Scanning Performance Benchmark ===");
        
        List<String> testPayloads = List.of(
            "{\"name\": \"John Doe\", \"email\": \"john@example.com\"}",
            "{\"query\": \"SELECT * FROM users WHERE active = true\"}",
            "{\"comment\": \"This is a normal comment\"}",
            "{\"data\": \"" + "x".repeat(1000) + "\"}",
            "{\"config\": {\"setting\": \"value\", \"enabled\": true}}"
        );
        
        StopWatch stopWatch = new StopWatch();
        int iterations = 1000;
        
        stopWatch.start();
        for (int i = 0; i < iterations; i++) {
            String payload = testPayloads.get(i % testPayloads.size());
            
            StepVerifier.create(
                securityScanningService.scanRequest(
                    "test-client",
                    "/api/v1/test",
                    "POST",
                    Map.of("Content-Type", "application/json"),
                    payload
                )
            )
            .expectNextCount(1)
            .verifyComplete();
        }
        stopWatch.stop();
        
        long totalTimeMs = stopWatch.getTotalTimeMillis();
        double avgTimeMs = (double) totalTimeMs / iterations;
        double throughput = (iterations * 1000.0) / totalTimeMs;
        
        System.out.println("Security Scanning Benchmark Results:");
        System.out.println("Total iterations: " + iterations);
        System.out.println("Total time: " + totalTimeMs + "ms");
        System.out.println("Average time per scan: " + String.format("%.2f", avgTimeMs) + "ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " scans/sec");
        System.out.println();
        
        // Performance assertions
        assertThat(avgTimeMs).isLessThan(10.0); // Should be under 10ms average
        assertThat(throughput).isGreaterThan(100.0); // Should handle at least 100 scans/sec
    }

    /**
     * Benchmark caching performance
     */
    @Test
    void benchmarkCachingPerformance() {
        System.out.println("=== Caching Performance Benchmark ===");
        
        // Cache write performance
        StopWatch writeWatch = new StopWatch();
        int writeIterations = 1000;
        
        writeWatch.start();
        for (int i = 0; i < writeIterations; i++) {
            String key = "test-key-" + i;
            String value = "test-value-" + i;
            
            StepVerifier.create(
                cachingService.set(CachingService.CacheCategory.API_RESPONSE, key, value)
            )
            .expectComplete()
            .verify();
        }
        writeWatch.stop();
        
        // Cache read performance
        StopWatch readWatch = new StopWatch();
        int readIterations = 1000;
        
        readWatch.start();
        for (int i = 0; i < readIterations; i++) {
            String key = "test-key-" + (i % writeIterations);
            
            StepVerifier.create(
                cachingService.get(CachingService.CacheCategory.API_RESPONSE, key)
            )
            .expectNextCount(1)
            .verifyComplete();
        }
        readWatch.stop();
        
        long writeTimeMs = writeWatch.getTotalTimeMillis();
        long readTimeMs = readWatch.getTotalTimeMillis();
        double writeAvg = (double) writeTimeMs / writeIterations;
        double readAvg = (double) readTimeMs / readIterations;
        double writeThroughput = (writeIterations * 1000.0) / writeTimeMs;
        double readThroughput = (readIterations * 1000.0) / readTimeMs;
        
        System.out.println("Caching Benchmark Results:");
        System.out.println("Write operations: " + writeIterations);
        System.out.println("Write time: " + writeTimeMs + "ms");
        System.out.println("Average write time: " + String.format("%.2f", writeAvg) + "ms");
        System.out.println("Write throughput: " + String.format("%.2f", writeThroughput) + " ops/sec");
        System.out.println("Read operations: " + readIterations);
        System.out.println("Read time: " + readTimeMs + "ms");
        System.out.println("Average read time: " + String.format("%.2f", readAvg) + "ms");
        System.out.println("Read throughput: " + String.format("%.2f", readThroughput) + " ops/sec");
        System.out.println();
        
        // Performance assertions
        assertThat(writeAvg).isLessThan(5.0); // Writes should be under 5ms average
        assertThat(readAvg).isLessThan(2.0); // Reads should be under 2ms average
        assertThat(writeThroughput).isGreaterThan(200.0); // At least 200 writes/sec
        assertThat(readThroughput).isGreaterThan(500.0); // At least 500 reads/sec
    }

    /**
     * Benchmark metrics collection performance
     */
    @Test
    void benchmarkMetricsCollectionPerformance() {
        System.out.println("=== Metrics Collection Performance Benchmark ===");
        
        StopWatch stopWatch = new StopWatch();
        int iterations = 10000;
        
        stopWatch.start();
        for (int i = 0; i < iterations; i++) {
            metricsCollectorService.recordRequest("/api/v1/test", "GET", 100, 200);
            metricsCollectorService.recordAuthentication("user" + (i % 100), true, "jwt");
            
            if (i % 10 == 0) {
                metricsCollectorService.recordAIResponse("gpt-4", "user" + (i % 100), 1000, true);
            }
        }
        stopWatch.stop();
        
        long totalTimeMs = stopWatch.getTotalTimeMillis();
        double avgTimeMs = (double) totalTimeMs / iterations;
        double throughput = (iterations * 1000.0) / totalTimeMs;
        
        System.out.println("Metrics Collection Benchmark Results:");
        System.out.println("Total iterations: " + iterations);
        System.out.println("Total time: " + totalTimeMs + "ms");
        System.out.println("Average time per metric: " + String.format("%.4f", avgTimeMs) + "ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " metrics/sec");
        System.out.println();
        
        // Performance assertions
        assertThat(avgTimeMs).isLessThan(1.0); // Should be under 1ms average
        assertThat(throughput).isGreaterThan(1000.0); // Should handle at least 1000 metrics/sec
    }

    /**
     * Benchmark end-to-end request flow
     */
    @Test
    void benchmarkEndToEndRequestFlow() {
        System.out.println("=== End-to-End Request Flow Benchmark ===");
        
        // Login first to get token
        Map<String, String> loginRequest = Map.of(
            "username", "admin",
            "password", "admin123"
        );
        
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity(
            baseUrl + "/api/v1/auth/login",
            loginRequest,
            Map.class
        );
        
        String token = (String) loginResponse.getBody().get("token");
        
        // Benchmark authenticated requests
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("X-User-ID", "test-user");
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        
        StopWatch stopWatch = new StopWatch();
        int iterations = 500;
        
        stopWatch.start();
        for (int i = 0; i < iterations; i++) {
            ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/api/v1/organizations",
                HttpMethod.GET,
                requestEntity,
                String.class
            );
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
        stopWatch.stop();
        
        long totalTimeMs = stopWatch.getTotalTimeMillis();
        double avgTimeMs = (double) totalTimeMs / iterations;
        double throughput = (iterations * 1000.0) / totalTimeMs;
        
        System.out.println("End-to-End Request Flow Benchmark Results:");
        System.out.println("Total iterations: " + iterations);
        System.out.println("Total time: " + totalTimeMs + "ms");
        System.out.println("Average time per request: " + String.format("%.2f", avgTimeMs) + "ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " req/sec");
        System.out.println();
        
        // Performance assertions
        assertThat(avgTimeMs).isLessThan(150.0); // Should be under 150ms average
        assertThat(throughput).isGreaterThan(5.0); // Should handle at least 5 req/sec
    }

    /**
     * Benchmark memory usage under load
     */
    @Test
    void benchmarkMemoryUsage() {
        System.out.println("=== Memory Usage Benchmark ===");
        
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Generate load
        List<String> data = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            metricsCollectorService.recordRequest("/api/v1/test", "GET", 100, 200);
            
            // Cache some data
            String key = "memory-test-" + i;
            String value = "test-value-" + i + "-" + "x".repeat(100);
            
            StepVerifier.create(
                cachingService.set(CachingService.CacheCategory.API_RESPONSE, key, value)
            )
            .expectComplete()
            .verify();
            
            data.add(value);
        }
        
        // Force garbage collection
        System.gc();
        Thread.yield();
        
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = finalMemory - initialMemory;
        
        System.out.println("Memory Usage Benchmark Results:");
        System.out.println("Initial memory: " + String.format("%.2f", initialMemory / 1024.0 / 1024.0) + " MB");
        System.out.println("Final memory: " + String.format("%.2f", finalMemory / 1024.0 / 1024.0) + " MB");
        System.out.println("Memory used: " + String.format("%.2f", memoryUsed / 1024.0 / 1024.0) + " MB");
        System.out.println("Memory efficiency: " + String.format("%.2f", memoryUsed / 10000.0) + " bytes per operation");
        System.out.println();
        
        // Memory assertions
        assertThat(memoryUsed).isLessThan(100 * 1024 * 1024); // Should use less than 100MB
    }

    /**
     * Stress test with sustained load
     */
    @Test
    void stressTestSustainedLoad() throws InterruptedException {
        System.out.println("=== Stress Test - Sustained Load ===");
        
        int duration = 30; // seconds
        int concurrentUsers = 20;
        
        ExecutorService executor = Executors.newFixedThreadPool(concurrentUsers);
        AtomicInteger totalRequests = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        
        for (int i = 0; i < concurrentUsers; i++) {
            executor.submit(() -> {
                while (stopWatch.getTotalTimeSeconds() < duration) {
                    try {
                        long startTime = System.currentTimeMillis();
                        
                        ResponseEntity<String> response = restTemplate.getForEntity(
                            baseUrl + "/actuator/health",
                            String.class
                        );
                        
                        long responseTime = System.currentTimeMillis() - startTime;
                        totalResponseTime.addAndGet(responseTime);
                        totalRequests.incrementAndGet();
                        
                        if (response.getStatusCode() == HttpStatus.OK) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                        
                        Thread.sleep(50); // 50ms between requests
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        totalRequests.incrementAndGet();
                    }
                }
            });
        }
        
        Thread.sleep(duration * 1000);
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        stopWatch.stop();
        
        double avgResponseTime = (double) totalResponseTime.get() / successCount.get();
        double throughput = (successCount.get() * 1000.0) / stopWatch.getTotalTimeMillis();
        
        System.out.println("Stress Test Results:");
        System.out.println("Duration: " + duration + " seconds");
        System.out.println("Concurrent users: " + concurrentUsers);
        System.out.println("Total requests: " + totalRequests.get());
        System.out.println("Successful requests: " + successCount.get());
        System.out.println("Failed requests: " + failureCount.get());
        System.out.println("Average response time: " + String.format("%.2f", avgResponseTime) + "ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " req/sec");
        System.out.println("Success rate: " + String.format("%.2f", (100.0 * successCount.get() / totalRequests.get())) + "%");
        System.out.println();
        
        // Stress test assertions
        assertThat(successCount.get()).isGreaterThan(totalRequests.get() * 0.95); // 95% success rate
        assertThat(avgResponseTime).isLessThan(500.0); // Average under 500ms under load
        assertThat(throughput).isGreaterThan(10.0); // At least 10 req/sec under sustained load
    }
}