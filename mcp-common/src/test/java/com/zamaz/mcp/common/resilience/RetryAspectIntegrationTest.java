package com.zamaz.mcp.common.resilience;

import com.zamaz.mcp.common.resilience.annotation.Retry;
import com.zamaz.mcp.common.resilience.exception.RetryExecutionException;
import com.zamaz.mcp.common.resilience.metrics.RetryMetricsCollector;
import com.zamaz.mcp.common.testing.annotations.TestProfile;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for RetryAspect using real Spring AOP configuration.
 */
@SpringBootTest
@ActiveProfiles("test") 
@TestProfile(category = "integration", priority = "high")
@DisplayName("RetryAspect Integration Tests")
class RetryAspectIntegrationTest {
    
    @Autowired
    private TestService testService;
    
    @Autowired
    private RetryMetricsCollector metricsCollector;
    
    @BeforeEach
    void setUp() {
        testService.reset();
        metricsCollector.clearMetrics();
    }
    
    @TestConfiguration
    @EnableAspectJAutoProxy
    static class TestConfig {
        
        @Bean
        public TestService testService() {
            return new TestService();
        }
    }
    
    @Service
    static class TestService {
        private final AtomicInteger callCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private boolean shouldFail = false;
        private int failuresBeforeSuccess = 0;
        
        @Retry(name = "test-basic-retry", maxAttempts = 3, waitDurationMs = 100)
        public String basicRetryMethod() throws IOException {
            int attempt = callCount.incrementAndGet();
            if (shouldFail) {
                throw new IOException("Simulated failure on attempt " + attempt);
            }
            return "Success on attempt " + attempt;
        }
        
        @Retry(name = "test-exponential-retry", 
               maxAttempts = 4, 
               waitDurationMs = 50,
               exponentialBackoffMultiplier = 2.0,
               maxWaitDurationMs = 1000)
        public String exponentialBackoffMethod() throws IOException {
            int attempt = callCount.incrementAndGet();
            if (failureCount.get() < failuresBeforeSuccess) {
                failureCount.incrementAndGet();
                throw new IOException("Simulated failure on attempt " + attempt);
            }
            return "Success after " + attempt + " attempts";
        }
        
        @Retry(name = "test-specific-exceptions",
               maxAttempts = 3,
               waitDurationMs = 50,
               retryExceptions = {IOException.class})
        public String specificExceptionRetryMethod() throws Exception {
            int attempt = callCount.incrementAndGet();
            if (shouldFail) {
                if (attempt <= 2) {
                    throw new IOException("Retryable IO exception");
                } else {
                    throw new IllegalArgumentException("Non-retryable exception");
                }
            }
            return "Success on attempt " + attempt;
        }
        
        @Retry(name = "test-abort-exceptions",
               maxAttempts = 5,
               waitDurationMs = 50,
               abortExceptions = {IllegalArgumentException.class})
        public String abortExceptionMethod() throws Exception {
            int attempt = callCount.incrementAndGet();
            if (attempt == 1) {
                throw new IllegalArgumentException("Should abort immediately");
            }
            return "Success on attempt " + attempt;
        }
        
        public void reset() {
            callCount.set(0);
            failureCount.set(0);
            shouldFail = false;
            failuresBeforeSuccess = 0;
        }
        
        public void setShouldFail(boolean shouldFail) {
            this.shouldFail = shouldFail;
        }
        
        public void setFailuresBeforeSuccess(int failures) {
            this.failuresBeforeSuccess = failures;
        }
        
        public int getCallCount() {
            return callCount.get();
        }
    }
    
    @Nested
    @DisplayName("Basic Retry Functionality")
    class BasicRetryTests {
        
        @Test
        @DisplayName("Should succeed without retries")
        void shouldSucceedWithoutRetries() throws IOException {
            String result = testService.basicRetryMethod();
            
            assertThat(result).isEqualTo("Success on attempt 1");
            assertThat(testService.getCallCount()).isEqualTo(1);
            
            // Verify metrics
            RetryMetricsCollector.RetryStats stats = metricsCollector.getRetryStats("test-basic-retry");
            assertThat(stats.getSuccessfulExecutions()).isEqualTo(1);
            assertThat(stats.getTotalAttempts()).isEqualTo(1);
        }
        
        @Test
        @DisplayName("Should retry and eventually succeed")
        void shouldRetryAndEventuallySucceed() throws IOException {
            testService.setFailuresBeforeSuccess(2);
            
            String result = testService.exponentialBackoffMethod();
            
            assertThat(result).contains("Success after 3 attempts");
            assertThat(testService.getCallCount()).isEqualTo(3);
            
            // Verify metrics
            RetryMetricsCollector.RetryStats stats = metricsCollector.getRetryStats("test-exponential-retry");
            assertThat(stats.getSuccessfulExecutions()).isEqualTo(1);
            assertThat(stats.getTotalAttempts()).isEqualTo(3);
        }
        
        @Test
        @DisplayName("Should fail after exhausting all attempts")
        void shouldFailAfterExhaustingAttempts() {
            testService.setShouldFail(true);
            
            assertThatThrownBy(() -> testService.basicRetryMethod())
                .isInstanceOf(RetryExecutionException.class)
                .hasMessageContaining("test-basic-retry")
                .hasMessageContaining("failed after 3 attempts");
            
            assertThat(testService.getCallCount()).isEqualTo(3);
            
            // Verify metrics
            RetryMetricsCollector.RetryStats stats = metricsCollector.getRetryStats("test-basic-retry");
            assertThat(stats.getFailedExecutions()).isEqualTo(1);
            assertThat(stats.getTotalAttempts()).isEqualTo(3);
        }
    }
    
    @Nested
    @DisplayName("Exception Handling Tests")
    class ExceptionHandlingTests {
        
        @Test
        @DisplayName("Should only retry specified exceptions")
        void shouldOnlyRetrySpecifiedExceptions() throws Exception {
            testService.setShouldFail(true);
            
            // This should retry IOException but fail on IllegalArgumentException
            assertThatThrownBy(() -> testService.specificExceptionRetryMethod())
                .isInstanceOf(RetryExecutionException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
            
            assertThat(testService.getCallCount()).isEqualTo(3); // 2 retries + 1 final attempt
        }
        
        @Test
        @DisplayName("Should abort immediately on specified exceptions")
        void shouldAbortImmediatelyOnSpecifiedExceptions() {
            assertThatThrownBy(() -> testService.abortExceptionMethod())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Should abort immediately");
            
            assertThat(testService.getCallCount()).isEqualTo(1); // Should not retry
        }
    }
    
    @Nested
    @DisplayName("Exponential Backoff Tests")
    class ExponentialBackoffTests {
        
        @Test
        @DisplayName("Should apply exponential backoff timing")
        void shouldApplyExponentialBackoff() {
            testService.setFailuresBeforeSuccess(2);
            
            long startTime = System.currentTimeMillis();
            
            assertThatCode(() -> testService.exponentialBackoffMethod())
                .doesNotThrowAnyException();
            
            long duration = System.currentTimeMillis() - startTime;
            
            // With exponential backoff (50ms * 2^0 + 50ms * 2^1 = 150ms minimum)
            // Plus method execution time, should be at least 100ms
            assertThat(duration).isGreaterThan(100L);
            assertThat(testService.getCallCount()).isEqualTo(3);
        }
    }
    
    @Nested
    @DisplayName("Metrics Integration Tests")
    class MetricsIntegrationTests {
        
        @Test
        @DisplayName("Should collect comprehensive metrics across multiple operations")
        void shouldCollectComprehensiveMetrics() throws IOException {
            // Successful operation
            testService.basicRetryMethod();
            
            // Failed operation
            testService.setShouldFail(true);
            assertThatThrownBy(() -> testService.basicRetryMethod())
                .isInstanceOf(RetryExecutionException.class);
            
            // Another successful operation with retries
            testService.reset();
            testService.setFailuresBeforeSuccess(1);
            testService.exponentialBackoffMethod();
            
            // Verify basic retry metrics
            RetryMetricsCollector.RetryStats basicStats = metricsCollector.getRetryStats("test-basic-retry");
            assertThat(basicStats.getTotalExecutions()).isEqualTo(2);
            assertThat(basicStats.getSuccessfulExecutions()).isEqualTo(1);
            assertThat(basicStats.getFailedExecutions()).isEqualTo(1);
            assertThat(basicStats.getSuccessRate()).isEqualTo(0.5);
            
            // Verify exponential retry metrics
            RetryMetricsCollector.RetryStats exponentialStats = metricsCollector.getRetryStats("test-exponential-retry");
            assertThat(exponentialStats.getTotalExecutions()).isEqualTo(1);
            assertThat(exponentialStats.getSuccessfulExecutions()).isEqualTo(1);
            assertThat(exponentialStats.getAverageAttemptsPerExecution()).isEqualTo(2.0);
        }
        
        @Test
        @DisplayName("Should track retry statistics over time")
        void shouldTrackRetryStatisticsOverTime() throws IOException {
            // Execute multiple operations to build statistics
            for (int i = 0; i < 5; i++) {
                testService.reset();
                if (i % 2 == 0) {
                    // Success cases
                    testService.basicRetryMethod();
                } else {
                    // Failure cases
                    testService.setShouldFail(true);
                    assertThatThrownBy(() -> testService.basicRetryMethod())
                        .isInstanceOf(RetryExecutionException.class);
                }
            }
            
            RetryMetricsCollector.RetryStats stats = metricsCollector.getRetryStats("test-basic-retry");
            assertThat(stats.getTotalExecutions()).isEqualTo(5);
            assertThat(stats.getSuccessfulExecutions()).isEqualTo(3);
            assertThat(stats.getFailedExecutions()).isEqualTo(2);
            assertThat(stats.getSuccessRate()).isEqualTo(0.6);
            assertThat(stats.getLastExecution()).isNotNull();
        }
    }
    
    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {
        
        @Test
        @DisplayName("Should handle high-frequency retry operations")
        void shouldHandleHighFrequencyOperations() {
            // Execute many retry operations to test performance
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < 100; i++) {
                testService.reset();
                assertThatCode(() -> testService.basicRetryMethod())
                    .doesNotThrowAnyException();
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Should complete 100 operations within reasonable time (5 seconds)
            assertThat(duration).isLessThan(5000L);
            
            RetryMetricsCollector.RetryStats stats = metricsCollector.getRetryStats("test-basic-retry");
            assertThat(stats.getTotalExecutions()).isEqualTo(100);
            assertThat(stats.getSuccessfulExecutions()).isEqualTo(100);
        }
        
        @Test
        @DisplayName("Should maintain performance under retry load")
        void shouldMaintainPerformanceUnderRetryLoad() {
            testService.setFailuresBeforeSuccess(2); // Force 3 attempts each time
            
            long startTime = System.currentTimeMillis();
            
            for (int i = 0; i < 10; i++) {
                testService.reset();
                testService.setFailuresBeforeSuccess(2);
                assertThatCode(() -> testService.exponentialBackoffMethod())
                    .doesNotThrowAnyException();
            }
            
            long duration = System.currentTimeMillis() - startTime;
            
            RetryMetricsCollector.RetryStats stats = metricsCollector.getRetryStats("test-exponential-retry");
            assertThat(stats.getTotalExecutions()).isEqualTo(10);
            assertThat(stats.getSuccessfulExecutions()).isEqualTo(10);
            assertThat(stats.getAverageAttemptsPerExecution()).isEqualTo(3.0);
            
            // Should complete within reasonable time considering retries and backoff
            assertThat(duration).isLessThan(10000L);
        }
    }
}