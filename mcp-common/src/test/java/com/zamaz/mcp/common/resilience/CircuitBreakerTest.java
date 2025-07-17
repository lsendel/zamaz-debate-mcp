package com.zamaz.mcp.common.resilience;

import com.zamaz.mcp.common.resilience.annotation.CircuitBreaker;
import com.zamaz.mcp.common.resilience.annotation.Retry;
import com.zamaz.mcp.common.resilience.annotation.RateLimiter;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Service;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for circuit breaker implementation
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {
    CircuitBreakerConfig.class,
    RetryConfig.class,
    RateLimiterConfig.class,
    CircuitBreakerAspect.class,
    RetryAspect.class,
    CircuitBreakerManager.class,
    CircuitBreakerHealthIndicator.class,
    CircuitBreakerTest.TestConfig.class
})
@TestPropertySource(properties = {
    "mcp.resilience.circuit-breaker.global.failure-rate-threshold=50",
    "mcp.resilience.circuit-breaker.global.minimum-number-of-calls=3",
    "mcp.resilience.circuit-breaker.global.wait-duration-in-open-state=1s",
    "mcp.resilience.retry.global.max-attempts=3",
    "mcp.resilience.rate-limiter.global.limit-for-period=10"
})
@EnableAspectJAutoProxy
class CircuitBreakerTest {
    
    @Autowired
    private TestService testService;
    
    @Autowired
    private CircuitBreakerManager circuitBreakerManager;
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Test
    void testCircuitBreakerOpensAfterFailures() {
        // Given
        testService.resetFailureCount();
        testService.setShouldFail(true);
        
        // When - make calls until circuit opens
        int failureCount = 0;
        for (int i = 0; i < 5; i++) {
            try {
                testService.unreliableMethod();
            } catch (Exception e) {
                failureCount++;
            }
        }
        
        // Then
        assertEquals(5, failureCount);
        
        // Circuit should be open now
        assertThrows(CallNotPermittedException.class, () -> testService.unreliableMethod());
        
        // Check circuit breaker status
        CircuitBreakerManager.CircuitBreakerStatus status = 
            circuitBreakerManager.getStatus("test-service");
        assertNotNull(status);
        assertEquals("OPEN", status.getState());
        assertTrue(status.getFailureRate() >= 50.0f);
    }
    
    @Test
    void testFallbackMethodCalled() {
        // Given
        testService.resetFailureCount();
        testService.setShouldFail(true);
        
        // Force circuit to open
        for (int i = 0; i < 5; i++) {
            try {
                testService.methodWithFallback("test");
            } catch (Exception ignored) {
            }
        }
        
        // When - circuit is open, fallback should be called
        String result = testService.methodWithFallback("test");
        
        // Then
        assertEquals("Fallback response for: test", result);
    }
    
    @Test
    void testRetryMechanism() {
        // Given
        testService.resetFailureCount();
        testService.setFailuresBeforeSuccess(2); // Fail twice, then succeed
        
        // When
        String result = testService.retriableMethod();
        
        // Then
        assertEquals("Success after retries", result);
        assertEquals(3, testService.getCallCount()); // 2 failures + 1 success
    }
    
    @Test
    void testRateLimiting() {
        // Given
        testService.resetFailureCount();
        
        // When - make rapid calls
        int successCount = 0;
        int rateLimitedCount = 0;
        
        for (int i = 0; i < 15; i++) {
            try {
                testService.rateLimitedMethod();
                successCount++;
            } catch (Exception e) {
                if (e.getMessage().contains("RateLimiter")) {
                    rateLimitedCount++;
                }
            }
        }
        
        // Then - some calls should be rate limited
        assertTrue(successCount <= 10); // Limited to 10 per period
        assertTrue(rateLimitedCount > 0);
    }
    
    @Test
    void testCombinedResilience() {
        // Given
        testService.resetFailureCount();
        testService.setFailuresBeforeSuccess(1);
        
        // When - method with circuit breaker and retry
        String result = testService.resilientMethod("test");
        
        // Then
        assertEquals("Resilient response: test", result);
        assertEquals(2, testService.getCallCount()); // 1 failure + 1 success due to retry
    }
    
    @TestConfiguration
    static class TestConfig {
        @Bean
        public TestService testService() {
            return new TestService();
        }
    }
    
    @Service
    static class TestService {
        
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger callCount = new AtomicInteger(0);
        private boolean shouldFail = false;
        private int failuresBeforeSuccess = Integer.MAX_VALUE;
        
        @CircuitBreaker(name = "test-service")
        public String unreliableMethod() {
            callCount.incrementAndGet();
            if (shouldFail) {
                failureCount.incrementAndGet();
                throw new RuntimeException("Service unavailable");
            }
            return "Success";
        }
        
        @CircuitBreaker(name = "test-service", fallbackMethod = "fallbackMethod")
        public String methodWithFallback(String input) {
            callCount.incrementAndGet();
            if (shouldFail) {
                failureCount.incrementAndGet();
                throw new RuntimeException("Service unavailable");
            }
            return "Success: " + input;
        }
        
        public String fallbackMethod(String input, Exception ex) {
            return "Fallback response for: " + input;
        }
        
        @Retry(name = "test-retry", maxAttempts = 3)
        public String retriableMethod() {
            int count = callCount.incrementAndGet();
            if (count <= failuresBeforeSuccess) {
                throw new RuntimeException("Temporary failure");
            }
            return "Success after retries";
        }
        
        @RateLimiter(name = "test-rate-limiter", limitForPeriod = 10)
        public String rateLimitedMethod() {
            return "Rate limited response";
        }
        
        @CircuitBreaker(name = "test-combined")
        @Retry(name = "test-combined", maxAttempts = 3)
        public String resilientMethod(String input) {
            int count = callCount.incrementAndGet();
            if (count <= failuresBeforeSuccess) {
                throw new RuntimeException("Temporary failure");
            }
            return "Resilient response: " + input;
        }
        
        public void setShouldFail(boolean shouldFail) {
            this.shouldFail = shouldFail;
        }
        
        public void setFailuresBeforeSuccess(int count) {
            this.failuresBeforeSuccess = count;
        }
        
        public void resetFailureCount() {
            failureCount.set(0);
            callCount.set(0);
        }
        
        public int getCallCount() {
            return callCount.get();
        }
    }
}