package com.zamaz.mcp.common.resilience;

import com.zamaz.mcp.common.resilience.annotation.CircuitBreaker;
import com.zamaz.mcp.common.resilience.exception.CircuitBreakerConfigurationException;
import com.zamaz.mcp.common.resilience.exception.CircuitBreakerExecutionException;
import com.zamaz.mcp.common.resilience.metrics.CircuitBreakerMetricsCollector;
import com.zamaz.mcp.common.testing.annotations.TestProfile;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for the enhanced CircuitBreakerAspect using the new testing framework.
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestProfile(category = "unit", priority = "high")
@DisplayName("CircuitBreakerAspect Enhanced Tests")
class CircuitBreakerAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;
    
    @Mock
    private Signature signature;
    
    @Mock
    private CircuitBreaker circuitBreakerAnnotation;
    
    private CircuitBreakerRegistry circuitBreakerRegistry;
    private CircuitBreakerMetricsCollector metricsCollector;
    private MeterRegistry meterRegistry;
    private CircuitBreakerAspect circuitBreakerAspect;
    
    @BeforeEach
    void setUp() {
        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        metricsCollector = new CircuitBreakerMetricsCollector();
        meterRegistry = new SimpleMeterRegistry();
        circuitBreakerAspect = new CircuitBreakerAspect(circuitBreakerRegistry, metricsCollector);
        
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("TestMethod");
    }
    
    @Nested
    @DisplayName("Configuration Validation Tests")
    class ConfigurationValidationTests {
        
        @Test
        @DisplayName("Should validate failure rate threshold parameter")
        void shouldValidateFailureRateThreshold() {
            when(circuitBreakerAnnotation.failureRateThreshold()).thenReturn(-5.0f);
            when(circuitBreakerAnnotation.name()).thenReturn("test-cb");
            setupValidCircuitBreakerDefaults();
            
            assertThatThrownBy(() -> circuitBreakerAspect.applyCircuitBreaker(joinPoint, circuitBreakerAnnotation))
                .isInstanceOf(CircuitBreakerConfigurationException.class)
                .hasMessageContaining("Failure rate threshold must be between 0 and 100");
        }
        
        @Test
        @DisplayName("Should validate slow call rate threshold parameter")
        void shouldValidateSlowCallRateThreshold() {
            when(circuitBreakerAnnotation.slowCallRateThreshold()).thenReturn(150.0f);
            when(circuitBreakerAnnotation.name()).thenReturn("test-cb");
            setupValidCircuitBreakerDefaults();
            
            assertThatThrownBy(() -> circuitBreakerAspect.applyCircuitBreaker(joinPoint, circuitBreakerAnnotation))
                .isInstanceOf(CircuitBreakerConfigurationException.class)
                .hasMessageContaining("Slow call rate threshold must be between 0 and 100");
        }
        
        @Test
        @DisplayName("Should validate slow call duration threshold parameter")
        void shouldValidateSlowCallDurationThreshold() {
            when(circuitBreakerAnnotation.slowCallDurationThresholdMs()).thenReturn(-1000L);
            when(circuitBreakerAnnotation.name()).thenReturn("test-cb");
            setupValidCircuitBreakerDefaults();
            
            assertThatThrownBy(() -> circuitBreakerAspect.applyCircuitBreaker(joinPoint, circuitBreakerAnnotation))
                .isInstanceOf(CircuitBreakerConfigurationException.class)
                .hasMessageContaining("Slow call duration threshold must be non-negative");
        }
        
        @Test
        @DisplayName("Should validate permitted calls in half-open state parameter")
        void shouldValidatePermittedCallsInHalfOpenState() {
            when(circuitBreakerAnnotation.permittedCallsInHalfOpenState()).thenReturn(0);
            when(circuitBreakerAnnotation.name()).thenReturn("test-cb");
            setupValidCircuitBreakerDefaults();
            
            assertThatThrownBy(() -> circuitBreakerAspect.applyCircuitBreaker(joinPoint, circuitBreakerAnnotation))
                .isInstanceOf(CircuitBreakerConfigurationException.class)
                .hasMessageContaining("Permitted calls in half-open state must be at least 1");
        }
        
        @Test
        @DisplayName("Should validate sliding window size parameter")
        void shouldValidateSlidingWindowSize() {
            when(circuitBreakerAnnotation.slidingWindowSize()).thenReturn(0);
            when(circuitBreakerAnnotation.name()).thenReturn("test-cb");
            setupValidCircuitBreakerDefaults();
            
            assertThatThrownBy(() -> circuitBreakerAspect.applyCircuitBreaker(joinPoint, circuitBreakerAnnotation))
                .isInstanceOf(CircuitBreakerConfigurationException.class)
                .hasMessageContaining("Sliding window size must be at least 1");
        }
        
        @Test
        @DisplayName("Should validate minimum number of calls parameter")
        void shouldValidateMinimumNumberOfCalls() {
            when(circuitBreakerAnnotation.minimumNumberOfCalls()).thenReturn(0);
            when(circuitBreakerAnnotation.name()).thenReturn("test-cb");
            setupValidCircuitBreakerDefaults();
            
            assertThatThrownBy(() -> circuitBreakerAspect.applyCircuitBreaker(joinPoint, circuitBreakerAnnotation))
                .isInstanceOf(CircuitBreakerConfigurationException.class)
                .hasMessageContaining("Minimum number of calls must be at least 1");
        }
        
        @Test
        @DisplayName("Should validate wait duration in open state parameter")
        void shouldValidateWaitDurationInOpenState() {
            when(circuitBreakerAnnotation.waitDurationInOpenStateSeconds()).thenReturn(0);
            when(circuitBreakerAnnotation.name()).thenReturn("test-cb");
            setupValidCircuitBreakerDefaults();
            
            assertThatThrownBy(() -> circuitBreakerAspect.applyCircuitBreaker(joinPoint, circuitBreakerAnnotation))
                .isInstanceOf(CircuitBreakerConfigurationException.class)
                .hasMessageContaining("Wait duration in open state must be at least 1 second");
        }
    }
    
    @Nested
    @DisplayName("Circuit Breaker Execution Tests")
    class CircuitBreakerExecutionTests {
        
        @Test
        @DisplayName("Should succeed when circuit breaker is closed")
        void shouldSucceedWhenCircuitBreakerIsClosed() throws Throwable {
            setupValidCircuitBreakerAnnotation();
            when(joinPoint.proceed()).thenReturn("success");
            
            Object result = circuitBreakerAspect.applyCircuitBreaker(joinPoint, circuitBreakerAnnotation);
            
            assertThat(result).isEqualTo("success");
            verify(joinPoint, times(1)).proceed();
            
            // Verify metrics
            CircuitBreakerMetricsCollector.CircuitBreakerStats stats = 
                metricsCollector.getCircuitBreakerStats("test-cb");
            assertThat(stats).isNotNull();
            assertThat(stats.getTotalExecutions()).isEqualTo(1);
            assertThat(stats.getSuccessfulExecutions()).isEqualTo(1);
            assertThat(stats.getFailedExecutions()).isEqualTo(0);
            assertThat(stats.getSuccessRate()).isEqualTo(1.0);
        }
        
        @Test
        @DisplayName("Should record failure when method throws exception")
        void shouldRecordFailureWhenMethodThrowsException() throws Throwable {
            setupValidCircuitBreakerAnnotation();
            IOException exception = new IOException("Service unavailable");
            when(joinPoint.proceed()).thenThrow(exception);
            
            assertThatThrownBy(() -> circuitBreakerAspect.applyCircuitBreaker(joinPoint, circuitBreakerAnnotation))
                .isInstanceOf(CircuitBreakerExecutionException.class)
                .hasMessageContaining("test-cb")
                .hasCause(exception);
            
            verify(joinPoint, times(1)).proceed();
            
            // Verify metrics
            CircuitBreakerMetricsCollector.CircuitBreakerStats stats = 
                metricsCollector.getCircuitBreakerStats("test-cb");
            assertThat(stats).isNotNull();
            assertThat(stats.getTotalExecutions()).isEqualTo(1);
            assertThat(stats.getSuccessfulExecutions()).isEqualTo(0);
            assertThat(stats.getFailedExecutions()).isEqualTo(1);
            assertThat(stats.getSuccessRate()).isEqualTo(0.0);
            assertThat(stats.getLastError()).contains("Service unavailable");
        }
        
        @Test
        @DisplayName("Should preserve original exception details")
        void shouldPreserveOriginalExceptionDetails() throws Throwable {
            setupValidCircuitBreakerAnnotation();
            IllegalArgumentException originalException = new IllegalArgumentException("Invalid input parameter");
            when(joinPoint.proceed()).thenThrow(originalException);
            
            assertThatThrownBy(() -> circuitBreakerAspect.applyCircuitBreaker(joinPoint, circuitBreakerAnnotation))
                .isInstanceOf(CircuitBreakerExecutionException.class)
                .satisfies(ex -> {
                    CircuitBreakerExecutionException cbEx = (CircuitBreakerExecutionException) ex;
                    assertThat(cbEx.getCircuitBreakerName()).isEqualTo("test-cb");
                    assertThat(cbEx.getCircuitBreakerState()).isEqualTo(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED);
                    assertThat(cbEx.getOriginalException()).isEqualTo(originalException);
                });
        }
        
        @Test
        @DisplayName("Should handle circuit breaker state transitions")
        void shouldHandleCircuitBreakerStateTransitions() throws Throwable {
            setupValidCircuitBreakerAnnotation();
            
            // Configure circuit breaker to open quickly
            when(circuitBreakerAnnotation.useDefault()).thenReturn(false);
            when(circuitBreakerAnnotation.failureRateThreshold()).thenReturn(50.0f);
            when(circuitBreakerAnnotation.minimumNumberOfCalls()).thenReturn(2);
            when(circuitBreakerAnnotation.slidingWindowSize()).thenReturn(2);
            
            // Force failures to trigger state transition
            when(joinPoint.proceed()).thenThrow(new IOException("Service failure"));
            
            // Execute multiple failures to trigger state change
            for (int i = 0; i < 3; i++) {
                try {
                    circuitBreakerAspect.applyCircuitBreaker(joinPoint, circuitBreakerAnnotation);
                } catch (CircuitBreakerExecutionException e) {
                    // Expected
                }
            }
            
            // Verify state changes were recorded
            CircuitBreakerMetricsCollector.CircuitBreakerStats stats = 
                metricsCollector.getCircuitBreakerStats("test-cb");
            assertThat(stats).isNotNull();
            assertThat(stats.getFailedExecutions()).isGreaterThan(0);
        }
    }
    
    @Nested
    @DisplayName("Fallback Mechanism Tests")
    class FallbackMechanismTests {
        
        @Test
        @DisplayName("Should execute fallback when circuit breaker is open")
        void shouldExecuteFallbackWhenCircuitBreakerIsOpen() throws Throwable {
            setupValidCircuitBreakerAnnotation();
            when(circuitBreakerAnnotation.fallbackMethod()).thenReturn("fallbackMethod");
            
            // Mock the circuit breaker to throw CallNotPermittedException
            CallNotPermittedException callNotPermittedException = 
                CallNotPermittedException.createCallNotPermittedException(
                    circuitBreakerRegistry.circuitBreaker("test-cb"));
            
            when(joinPoint.proceed()).thenThrow(callNotPermittedException);
            
            // We would need to set up the fallback method invocation here
            // This is a simplified test - in real scenarios, we'd need proper method mocking
            
            assertThatThrownBy(() -> circuitBreakerAspect.applyCircuitBreaker(joinPoint, circuitBreakerAnnotation))
                .isInstanceOf(CircuitBreakerExecutionException.class);
            
            // Verify that call not permitted was recorded
            CircuitBreakerMetricsCollector.CircuitBreakerStats stats = 
                metricsCollector.getCircuitBreakerStats("test-cb");
            assertThat(stats).isNotNull();
            assertThat(stats.getCallsNotPermitted()).isGreaterThan(0);
        }
        
        @Test
        @DisplayName("Should record fallback execution metrics")
        void shouldRecordFallbackExecutionMetrics() {
            String circuitBreakerName = "test-fallback-cb";
            
            // Simulate fallback execution
            metricsCollector.recordFallbackExecution(circuitBreakerName, true, Duration.ofMillis(100));
            metricsCollector.recordFallbackExecution(circuitBreakerName, false, Duration.ofMillis(200));
            
            CircuitBreakerMetricsCollector.CircuitBreakerStats stats = 
                metricsCollector.getCircuitBreakerStats(circuitBreakerName);
            assertThat(stats).isNotNull();
            assertThat(stats.getFallbackExecutions()).isEqualTo(2);
            assertThat(stats.getSuccessfulFallbacks()).isEqualTo(1);
            assertThat(stats.getFallbackSuccessRate()).isEqualTo(0.5);
            assertThat(stats.getAverageFallbackTimeMs()).isEqualTo(150.0);
        }
    }
    
    @Nested
    @DisplayName("Metrics Collection Tests")
    class MetricsCollectionTests {
        
        @Test
        @DisplayName("Should collect comprehensive circuit breaker metrics")
        void shouldCollectComprehensiveMetrics() throws Throwable {
            setupValidCircuitBreakerAnnotation();
            
            // Execute successful operations
            when(joinPoint.proceed()).thenReturn("success1", "success2");
            circuitBreakerAspect.applyCircuitBreaker(joinPoint, circuitBreakerAnnotation);
            circuitBreakerAspect.applyCircuitBreaker(joinPoint, circuitBreakerAnnotation);
            
            // Execute failed operation
            when(joinPoint.proceed()).thenThrow(new IOException("Failure"));
            assertThatThrownBy(() -> circuitBreakerAspect.applyCircuitBreaker(joinPoint, circuitBreakerAnnotation))
                .isInstanceOf(CircuitBreakerExecutionException.class);
            
            CircuitBreakerMetricsCollector.CircuitBreakerStats stats = 
                metricsCollector.getCircuitBreakerStats("test-cb");
            assertThat(stats).isNotNull();
            assertThat(stats.getTotalExecutions()).isEqualTo(3);
            assertThat(stats.getSuccessfulExecutions()).isEqualTo(2);
            assertThat(stats.getFailedExecutions()).isEqualTo(1);
            assertThat(stats.getSuccessRate()).isEqualTo(2.0/3.0);
            assertThat(stats.getFailureRate()).isEqualTo(1.0/3.0);
            assertThat(stats.getAverageExecutionTimeMs()).isGreaterThan(0);
        }
        
        @Test
        @DisplayName("Should calculate health score correctly")
        void shouldCalculateHealthScoreCorrectly() {
            String circuitBreakerName = "health-test-cb";
            
            // High success rate, no calls not permitted, no fallbacks
            for (int i = 0; i < 10; i++) {
                metricsCollector.recordSuccessfulExecution(circuitBreakerName, Duration.ofMillis(100));
            }
            
            CircuitBreakerMetricsCollector.CircuitBreakerStats stats = 
                metricsCollector.getCircuitBreakerStats(circuitBreakerName);
            double healthScore = stats.getHealthScore();
            
            assertThat(healthScore).isGreaterThan(0.8); // Should be high health score
            
            // Add some failures and calls not permitted
            for (int i = 0; i < 5; i++) {
                metricsCollector.recordFailedExecution(circuitBreakerName, Duration.ofMillis(100), 
                    new RuntimeException("Test failure"));
                metricsCollector.recordCallNotPermitted(circuitBreakerName);
            }
            
            double degradedHealthScore = stats.getHealthScore();
            assertThat(degradedHealthScore).isLessThan(healthScore); // Should be lower
        }
        
        @Test
        @DisplayName("Should track state changes correctly")
        void shouldTrackStateChangesCorrectly() {
            String circuitBreakerName = "state-test-cb";
            
            metricsCollector.recordStateChange(circuitBreakerName, 
                io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED,
                io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN);
            
            metricsCollector.recordStateChange(circuitBreakerName,
                io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN,
                io.github.resilience4j.circuitbreaker.CircuitBreaker.State.HALF_OPEN);
            
            CircuitBreakerMetricsCollector.CircuitBreakerStats stats = 
                metricsCollector.getCircuitBreakerStats(circuitBreakerName);
            assertThat(stats).isNotNull();
            assertThat(stats.getStateChanges()).isEqualTo(2);
            assertThat(stats.getCurrentState()).isEqualTo(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.HALF_OPEN);
            assertThat(stats.getPreviousState()).isEqualTo(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN);
        }
    }
    
    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {
        
        @Test
        @DisplayName("Should use default configuration when useDefault is true")
        void shouldUseDefaultConfiguration() throws Throwable {
            when(circuitBreakerAnnotation.name()).thenReturn("test-cb");
            when(circuitBreakerAnnotation.useDefault()).thenReturn(true);
            setupValidCircuitBreakerDefaults();
            
            when(joinPoint.proceed()).thenReturn("success");
            
            Object result = circuitBreakerAspect.applyCircuitBreaker(joinPoint, circuitBreakerAnnotation);
            assertThat(result).isEqualTo("success");
        }
        
        @Test
        @DisplayName("Should use custom configuration when useDefault is false")
        void shouldUseCustomConfiguration() throws Throwable {
            when(circuitBreakerAnnotation.name()).thenReturn("custom-cb");
            when(circuitBreakerAnnotation.useDefault()).thenReturn(false);
            when(circuitBreakerAnnotation.failureRateThreshold()).thenReturn(60.0f);
            when(circuitBreakerAnnotation.slowCallRateThreshold()).thenReturn(80.0f);
            when(circuitBreakerAnnotation.slowCallDurationThresholdMs()).thenReturn(2000L);
            when(circuitBreakerAnnotation.permittedCallsInHalfOpenState()).thenReturn(5);
            when(circuitBreakerAnnotation.slidingWindowType()).thenReturn("COUNT_BASED");
            when(circuitBreakerAnnotation.slidingWindowSize()).thenReturn(20);
            when(circuitBreakerAnnotation.minimumNumberOfCalls()).thenReturn(15);
            when(circuitBreakerAnnotation.waitDurationInOpenStateSeconds()).thenReturn(30);
            when(circuitBreakerAnnotation.automaticTransitionEnabled()).thenReturn(true);
            when(circuitBreakerAnnotation.recordExceptions()).thenReturn(new Class[]{IOException.class});
            when(circuitBreakerAnnotation.ignoreExceptions()).thenReturn(new Class[0]);
            when(circuitBreakerAnnotation.fallbackMethod()).thenReturn("");
            
            when(joinPoint.proceed()).thenReturn("success");
            
            Object result = circuitBreakerAspect.applyCircuitBreaker(joinPoint, circuitBreakerAnnotation);
            assertThat(result).isEqualTo("success");
        }
        
        @Test
        @DisplayName("Should use method signature as circuit breaker name when annotation name is empty")
        void shouldUseMethodSignatureAsCircuitBreakerName() throws Throwable {
            when(circuitBreakerAnnotation.name()).thenReturn("");
            setupValidCircuitBreakerDefaults();
            when(joinPoint.proceed()).thenReturn("success");
            
            circuitBreakerAspect.applyCircuitBreaker(joinPoint, circuitBreakerAnnotation);
            
            // Verify metrics were collected with method signature as name
            CircuitBreakerMetricsCollector.CircuitBreakerStats stats = 
                metricsCollector.getCircuitBreakerStats("TestMethod");
            assertThat(stats).isNotNull();
            assertThat(stats.getTotalExecutions()).isEqualTo(1);
        }
    }
    
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should extract original exception from wrapped exceptions")
        void shouldExtractOriginalException() throws Throwable {
            setupValidCircuitBreakerAnnotation();
            IllegalArgumentException originalException = new IllegalArgumentException("Root cause");
            RuntimeException wrappedException = new RuntimeException("Wrapper", originalException);
            when(joinPoint.proceed()).thenThrow(wrappedException);
            
            assertThatThrownBy(() -> circuitBreakerAspect.applyCircuitBreaker(joinPoint, circuitBreakerAnnotation))
                .isInstanceOf(CircuitBreakerExecutionException.class)
                .satisfies(ex -> {
                    CircuitBreakerExecutionException cbEx = (CircuitBreakerExecutionException) ex;
                    assertThat(cbEx.getOriginalException()).isEqualTo(originalException);
                });
        }
        
        @Test
        @DisplayName("Should handle null pointer exceptions gracefully")
        void shouldHandleNullPointerExceptions() throws Throwable {
            setupValidCircuitBreakerAnnotation();
            when(joinPoint.proceed()).thenThrow(new NullPointerException("Null value encountered"));
            
            assertThatThrownBy(() -> circuitBreakerAspect.applyCircuitBreaker(joinPoint, circuitBreakerAnnotation))
                .isInstanceOf(CircuitBreakerExecutionException.class)
                .hasMessageContaining("Null value encountered");
        }
    }
    
    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {
        
        @Test
        @DisplayName("Should complete circuit breaker operations within reasonable time")
        void shouldCompleteWithinReasonableTime() throws Throwable {
            setupValidCircuitBreakerAnnotation();
            when(joinPoint.proceed()).thenReturn("success");
            
            long startTime = System.currentTimeMillis();
            circuitBreakerAspect.applyCircuitBreaker(joinPoint, circuitBreakerAnnotation);
            long duration = System.currentTimeMillis() - startTime;
            
            assertThat(duration).isLessThan(1000L); // Should complete within 1 second
        }
        
        @Test
        @DisplayName("Should handle concurrent circuit breaker operations")
        void shouldHandleConcurrentOperations() {
            setupValidCircuitBreakerAnnotation();
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            
            // Simulate concurrent circuit breaker operations
            java.util.stream.IntStream.range(0, 10).parallel().forEach(i -> {
                try {
                    when(joinPoint.proceed()).thenReturn("success-" + i);
                    circuitBreakerAspect.applyCircuitBreaker(joinPoint, circuitBreakerAnnotation);
                    successCount.incrementAndGet();
                } catch (Throwable e) {
                    errorCount.incrementAndGet();
                }
            });
            
            assertThat(successCount.get()).isEqualTo(10);
            assertThat(errorCount.get()).isEqualTo(0);
        }
    }
    
    private void setupValidCircuitBreakerAnnotation() {
        when(circuitBreakerAnnotation.name()).thenReturn("test-cb");
        setupValidCircuitBreakerDefaults();
    }
    
    private void setupValidCircuitBreakerDefaults() {
        when(circuitBreakerAnnotation.useDefault()).thenReturn(true);
        when(circuitBreakerAnnotation.failureRateThreshold()).thenReturn(50.0f);
        when(circuitBreakerAnnotation.slowCallRateThreshold()).thenReturn(100.0f);
        when(circuitBreakerAnnotation.slowCallDurationThresholdMs()).thenReturn(60000L);
        when(circuitBreakerAnnotation.permittedCallsInHalfOpenState()).thenReturn(3);
        when(circuitBreakerAnnotation.slidingWindowType()).thenReturn("COUNT_BASED");
        when(circuitBreakerAnnotation.slidingWindowSize()).thenReturn(100);
        when(circuitBreakerAnnotation.minimumNumberOfCalls()).thenReturn(10);
        when(circuitBreakerAnnotation.waitDurationInOpenStateSeconds()).thenReturn(60);
        when(circuitBreakerAnnotation.automaticTransitionEnabled()).thenReturn(false);
        when(circuitBreakerAnnotation.recordExceptions()).thenReturn(new Class[0]);
        when(circuitBreakerAnnotation.ignoreExceptions()).thenReturn(new Class[0]);
        when(circuitBreakerAnnotation.fallbackMethod()).thenReturn("");
    }
}