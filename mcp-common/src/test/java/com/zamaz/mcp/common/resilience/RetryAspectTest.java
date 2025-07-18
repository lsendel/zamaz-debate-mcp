package com.zamaz.mcp.common.resilience;

import com.zamaz.mcp.common.resilience.annotation.Retry;
import com.zamaz.mcp.common.resilience.exception.RetryConfigurationException;
import com.zamaz.mcp.common.resilience.exception.RetryExecutionException;
import com.zamaz.mcp.common.resilience.metrics.RetryMetricsCollector;
import com.zamaz.mcp.common.testing.annotations.TestProfile;
import io.github.resilience4j.retry.RetryRegistry;
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
 * Comprehensive tests for the enhanced RetryAspect using the new testing framework.
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestProfile(category = "unit", priority = "high")
@DisplayName("RetryAspect Enhanced Tests")
class RetryAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;
    
    @Mock
    private Signature signature;
    
    @Mock
    private Retry retryAnnotation;
    
    private RetryRegistry retryRegistry;
    private RetryMetricsCollector metricsCollector;
    private MeterRegistry meterRegistry;
    private RetryAspect retryAspect;
    
    @BeforeEach
    void setUp() {
        retryRegistry = RetryRegistry.ofDefaults();
        metricsCollector = new RetryMetricsCollector();
        meterRegistry = new SimpleMeterRegistry();
        retryAspect = new RetryAspect(retryRegistry, metricsCollector);
        
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("TestMethod");
    }
    
    @Nested
    @DisplayName("Configuration Validation Tests")
    class ConfigurationValidationTests {
        
        @Test
        @DisplayName("Should validate max attempts parameter")
        void shouldValidateMaxAttempts() {
            when(retryAnnotation.maxAttempts()).thenReturn(0);
            when(retryAnnotation.name()).thenReturn("test-retry");
            when(retryAnnotation.waitDurationMs()).thenReturn(1000L);
            when(retryAnnotation.exponentialBackoffMultiplier()).thenReturn(1.0);
            when(retryAnnotation.jitterFactor()).thenReturn(0.5);
            when(retryAnnotation.maxWaitDurationMs()).thenReturn(5000L);
            
            assertThatThrownBy(() -> retryAspect.applyRetry(joinPoint, retryAnnotation))
                .isInstanceOf(RetryConfigurationException.class)
                .hasMessageContaining("Max attempts must be at least 1");
        }
        
        @Test
        @DisplayName("Should validate wait duration parameter")
        void shouldValidateWaitDuration() {
            when(retryAnnotation.maxAttempts()).thenReturn(3);
            when(retryAnnotation.name()).thenReturn("test-retry");
            when(retryAnnotation.waitDurationMs()).thenReturn(-100L);
            when(retryAnnotation.exponentialBackoffMultiplier()).thenReturn(1.0);
            when(retryAnnotation.jitterFactor()).thenReturn(0.5);
            when(retryAnnotation.maxWaitDurationMs()).thenReturn(5000L);
            
            assertThatThrownBy(() -> retryAspect.applyRetry(joinPoint, retryAnnotation))
                .isInstanceOf(RetryConfigurationException.class)
                .hasMessageContaining("Wait duration must be non-negative");
        }
        
        @Test
        @DisplayName("Should validate max wait duration parameter")
        void shouldValidateMaxWaitDuration() {
            when(retryAnnotation.maxAttempts()).thenReturn(3);
            when(retryAnnotation.name()).thenReturn("test-retry");
            when(retryAnnotation.waitDurationMs()).thenReturn(5000L);
            when(retryAnnotation.maxWaitDurationMs()).thenReturn(1000L);
            when(retryAnnotation.exponentialBackoffMultiplier()).thenReturn(1.0);
            when(retryAnnotation.jitterFactor()).thenReturn(0.5);
            
            assertThatThrownBy(() -> retryAspect.applyRetry(joinPoint, retryAnnotation))
                .isInstanceOf(RetryConfigurationException.class)
                .hasMessageContaining("Max wait duration must be greater than or equal to wait duration");
        }
        
        @Test
        @DisplayName("Should validate exponential backoff multiplier")
        void shouldValidateExponentialBackoffMultiplier() {
            when(retryAnnotation.maxAttempts()).thenReturn(3);
            when(retryAnnotation.name()).thenReturn("test-retry");
            when(retryAnnotation.waitDurationMs()).thenReturn(1000L);
            when(retryAnnotation.maxWaitDurationMs()).thenReturn(5000L);
            when(retryAnnotation.exponentialBackoffMultiplier()).thenReturn(0.5);
            when(retryAnnotation.jitterFactor()).thenReturn(0.5);
            
            assertThatThrownBy(() -> retryAspect.applyRetry(joinPoint, retryAnnotation))
                .isInstanceOf(RetryConfigurationException.class)
                .hasMessageContaining("Exponential backoff multiplier must be at least 1.0");
        }
        
        @Test
        @DisplayName("Should validate jitter factor")
        void shouldValidateJitterFactor() {
            when(retryAnnotation.maxAttempts()).thenReturn(3);
            when(retryAnnotation.name()).thenReturn("test-retry");
            when(retryAnnotation.waitDurationMs()).thenReturn(1000L);
            when(retryAnnotation.maxWaitDurationMs()).thenReturn(5000L);
            when(retryAnnotation.exponentialBackoffMultiplier()).thenReturn(1.0);
            when(retryAnnotation.jitterFactor()).thenReturn(1.5);
            
            assertThatThrownBy(() -> retryAspect.applyRetry(joinPoint, retryAnnotation))
                .isInstanceOf(RetryConfigurationException.class)
                .hasMessageContaining("Jitter factor must be between 0.0 and 1.0");
        }
    }
    
    @Nested
    @DisplayName("Retry Execution Tests")
    class RetryExecutionTests {
        
        @Test
        @DisplayName("Should succeed on first attempt")
        void shouldSucceedOnFirstAttempt() throws Throwable {
            setupValidRetryAnnotation();
            when(joinPoint.proceed()).thenReturn("success");
            
            Object result = retryAspect.applyRetry(joinPoint, retryAnnotation);
            
            assertThat(result).isEqualTo("success");
            verify(joinPoint, times(1)).proceed();
            
            // Verify metrics
            RetryMetricsCollector.RetryStats stats = metricsCollector.getRetryStats("test-retry");
            assertThat(stats).isNotNull();
            assertThat(stats.getTotalExecutions()).isEqualTo(1);
            assertThat(stats.getSuccessfulExecutions()).isEqualTo(1);
            assertThat(stats.getFailedExecutions()).isEqualTo(0);
        }
        
        @Test
        @DisplayName("Should retry and eventually succeed")
        void shouldRetryAndEventuallySucceed() throws Throwable {
            setupValidRetryAnnotation();
            when(joinPoint.proceed())
                .thenThrow(new IOException("Connection failed"))
                .thenThrow(new IOException("Connection failed"))
                .thenReturn("success");
            
            Object result = retryAspect.applyRetry(joinPoint, retryAnnotation);
            
            assertThat(result).isEqualTo("success");
            verify(joinPoint, times(3)).proceed();
            
            // Verify metrics
            RetryMetricsCollector.RetryStats stats = metricsCollector.getRetryStats("test-retry");
            assertThat(stats).isNotNull();
            assertThat(stats.getTotalExecutions()).isEqualTo(1);
            assertThat(stats.getSuccessfulExecutions()).isEqualTo(1);
            assertThat(stats.getTotalAttempts()).isEqualTo(3); // 2 retries + 1 original
        }
        
        @Test
        @DisplayName("Should fail after exhausting all retry attempts")
        void shouldFailAfterExhaustingRetries() throws Throwable {
            setupValidRetryAnnotation();
            IOException exception = new IOException("Persistent failure");
            when(joinPoint.proceed()).thenThrow(exception);
            
            assertThatThrownBy(() -> retryAspect.applyRetry(joinPoint, retryAnnotation))
                .isInstanceOf(RetryExecutionException.class)
                .hasMessageContaining("Retry 'test-retry' failed after")
                .hasCause(exception);
            
            verify(joinPoint, times(3)).proceed(); // maxAttempts = 3
            
            // Verify metrics
            RetryMetricsCollector.RetryStats stats = metricsCollector.getRetryStats("test-retry");
            assertThat(stats).isNotNull();
            assertThat(stats.getTotalExecutions()).isEqualTo(1);
            assertThat(stats.getSuccessfulExecutions()).isEqualTo(0);
            assertThat(stats.getFailedExecutions()).isEqualTo(1);
        }
        
        @Test
        @DisplayName("Should preserve original exception details")
        void shouldPreserveOriginalExceptionDetails() throws Throwable {
            setupValidRetryAnnotation();
            IllegalArgumentException originalException = new IllegalArgumentException("Invalid input parameter");
            when(joinPoint.proceed()).thenThrow(originalException);
            
            assertThatThrownBy(() -> retryAspect.applyRetry(joinPoint, retryAnnotation))
                .isInstanceOf(RetryExecutionException.class)
                .satisfies(ex -> {
                    RetryExecutionException retryEx = (RetryExecutionException) ex;
                    assertThat(retryEx.getRetryName()).isEqualTo("test-retry");
                    assertThat(retryEx.getAttemptsMade()).isEqualTo(3);
                    assertThat(retryEx.getOriginalException()).isEqualTo(originalException);
                });
        }
    }
    
    @Nested
    @DisplayName("Metrics Collection Tests")
    class MetricsCollectionTests {
        
        @Test
        @DisplayName("Should collect metrics for successful retry operations")
        void shouldCollectSuccessMetrics() throws Throwable {
            setupValidRetryAnnotation();
            when(joinPoint.proceed())
                .thenThrow(new IOException("Temporary failure"))
                .thenReturn("success");
            
            retryAspect.applyRetry(joinPoint, retryAnnotation);
            
            RetryMetricsCollector.RetryStats stats = metricsCollector.getRetryStats("test-retry");
            assertThat(stats).isNotNull();
            assertThat(stats.getTotalExecutions()).isEqualTo(1);
            assertThat(stats.getSuccessfulExecutions()).isEqualTo(1);
            assertThat(stats.getSuccessRate()).isEqualTo(1.0);
            assertThat(stats.getAverageAttemptsPerExecution()).isEqualTo(2.0);
            assertThat(stats.getAverageDurationMs()).isGreaterThan(0);
        }
        
        @Test
        @DisplayName("Should collect metrics for failed retry operations")
        void shouldCollectFailureMetrics() throws Throwable {
            setupValidRetryAnnotation();
            when(joinPoint.proceed()).thenThrow(new IOException("Persistent failure"));
            
            assertThatThrownBy(() -> retryAspect.applyRetry(joinPoint, retryAnnotation))
                .isInstanceOf(RetryExecutionException.class);
            
            RetryMetricsCollector.RetryStats stats = metricsCollector.getRetryStats("test-retry");
            assertThat(stats).isNotNull();
            assertThat(stats.getTotalExecutions()).isEqualTo(1);
            assertThat(stats.getFailedExecutions()).isEqualTo(1);
            assertThat(stats.getSuccessRate()).isEqualTo(0.0);
            assertThat(stats.getLastError()).contains("Persistent failure");
        }
        
        @Test
        @DisplayName("Should track multiple retry operations")
        void shouldTrackMultipleOperations() throws Throwable {
            setupValidRetryAnnotation();
            
            // First operation - success
            when(joinPoint.proceed()).thenReturn("success1");
            retryAspect.applyRetry(joinPoint, retryAnnotation);
            
            // Second operation - failure
            when(joinPoint.proceed()).thenThrow(new IOException("Failure"));
            assertThatThrownBy(() -> retryAspect.applyRetry(joinPoint, retryAnnotation))
                .isInstanceOf(RetryExecutionException.class);
            
            RetryMetricsCollector.RetryStats stats = metricsCollector.getRetryStats("test-retry");
            assertThat(stats).isNotNull();
            assertThat(stats.getTotalExecutions()).isEqualTo(2);
            assertThat(stats.getSuccessfulExecutions()).isEqualTo(1);
            assertThat(stats.getFailedExecutions()).isEqualTo(1);
            assertThat(stats.getSuccessRate()).isEqualTo(0.5);
        }
    }
    
    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {
        
        @Test
        @DisplayName("Should use default configuration when parameters match defaults")
        void shouldUseDefaultConfiguration() throws Throwable {
            when(retryAnnotation.name()).thenReturn("test-retry");
            when(retryAnnotation.maxAttempts()).thenReturn(3);
            when(retryAnnotation.waitDurationMs()).thenReturn(1000L);
            when(retryAnnotation.exponentialBackoffMultiplier()).thenReturn(1.0);
            when(retryAnnotation.retryExceptions()).thenReturn(new Class[0]);
            when(retryAnnotation.abortExceptions()).thenReturn(new Class[0]);
            when(retryAnnotation.jitterFactor()).thenReturn(0.5);
            when(retryAnnotation.maxWaitDurationMs()).thenReturn(0L);
            when(retryAnnotation.useJitter()).thenReturn(false);
            
            when(joinPoint.proceed()).thenReturn("success");
            
            Object result = retryAspect.applyRetry(joinPoint, retryAnnotation);
            assertThat(result).isEqualTo("success");
        }
        
        @Test
        @DisplayName("Should use custom configuration when parameters differ from defaults")
        void shouldUseCustomConfiguration() throws Throwable {
            when(retryAnnotation.name()).thenReturn("custom-retry");
            when(retryAnnotation.maxAttempts()).thenReturn(5);
            when(retryAnnotation.waitDurationMs()).thenReturn(2000L);
            when(retryAnnotation.exponentialBackoffMultiplier()).thenReturn(2.0);
            when(retryAnnotation.retryExceptions()).thenReturn(new Class[]{IOException.class});
            when(retryAnnotation.abortExceptions()).thenReturn(new Class[0]);
            when(retryAnnotation.jitterFactor()).thenReturn(0.3);
            when(retryAnnotation.maxWaitDurationMs()).thenReturn(10000L);
            when(retryAnnotation.useJitter()).thenReturn(true);
            
            when(joinPoint.proceed()).thenReturn("success");
            
            Object result = retryAspect.applyRetry(joinPoint, retryAnnotation);
            assertThat(result).isEqualTo("success");
        }
        
        @Test
        @DisplayName("Should use method signature as retry name when annotation name is empty")
        void shouldUseMethodSignatureAsRetryName() throws Throwable {
            when(retryAnnotation.name()).thenReturn("");
            setupValidRetryAnnotationDefaults();
            when(joinPoint.proceed()).thenReturn("success");
            
            retryAspect.applyRetry(joinPoint, retryAnnotation);
            
            // Verify metrics were collected with method signature as name
            RetryMetricsCollector.RetryStats stats = metricsCollector.getRetryStats("TestMethod");
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
            setupValidRetryAnnotation();
            IllegalArgumentException originalException = new IllegalArgumentException("Root cause");
            RuntimeException wrappedException = new RuntimeException("Wrapper", originalException);
            when(joinPoint.proceed()).thenThrow(wrappedException);
            
            assertThatThrownBy(() -> retryAspect.applyRetry(joinPoint, retryAnnotation))
                .isInstanceOf(RetryExecutionException.class)
                .satisfies(ex -> {
                    RetryExecutionException retryEx = (RetryExecutionException) ex;
                    assertThat(retryEx.getOriginalException()).isEqualTo(originalException);
                });
        }
        
        @Test
        @DisplayName("Should handle null pointer exceptions gracefully")
        void shouldHandleNullPointerExceptions() throws Throwable {
            setupValidRetryAnnotation();
            when(joinPoint.proceed()).thenThrow(new NullPointerException("Null value encountered"));
            
            assertThatThrownBy(() -> retryAspect.applyRetry(joinPoint, retryAnnotation))
                .isInstanceOf(RetryExecutionException.class)
                .hasMessageContaining("Null value encountered");
        }
    }
    
    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {
        
        @Test
        @DisplayName("Should complete retry operations within reasonable time")
        void shouldCompleteWithinReasonableTime() throws Throwable {
            setupValidRetryAnnotation();
            when(joinPoint.proceed()).thenReturn("success");
            
            long startTime = System.currentTimeMillis();
            retryAspect.applyRetry(joinPoint, retryAnnotation);
            long duration = System.currentTimeMillis() - startTime;
            
            assertThat(duration).isLessThan(1000L); // Should complete within 1 second
        }
        
        @Test
        @DisplayName("Should handle concurrent retry operations")
        void shouldHandleConcurrentOperations() {
            setupValidRetryAnnotation();
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            
            // Simulate concurrent retry operations
            IntStream.range(0, 10).parallel().forEach(i -> {
                try {
                    when(joinPoint.proceed()).thenReturn("success-" + i);
                    retryAspect.applyRetry(joinPoint, retryAnnotation);
                    successCount.incrementAndGet();
                } catch (Throwable e) {
                    errorCount.incrementAndGet();
                }
            });
            
            assertThat(successCount.get()).isEqualTo(10);
            assertThat(errorCount.get()).isEqualTo(0);
        }
    }
    
    private void setupValidRetryAnnotation() {
        when(retryAnnotation.name()).thenReturn("test-retry");
        setupValidRetryAnnotationDefaults();
    }
    
    private void setupValidRetryAnnotationDefaults() {
        when(retryAnnotation.maxAttempts()).thenReturn(3);
        when(retryAnnotation.waitDurationMs()).thenReturn(1000L);
        when(retryAnnotation.exponentialBackoffMultiplier()).thenReturn(1.0);
        when(retryAnnotation.jitterFactor()).thenReturn(0.5);
        when(retryAnnotation.maxWaitDurationMs()).thenReturn(5000L);
        when(retryAnnotation.retryExceptions()).thenReturn(new Class[0]);
        when(retryAnnotation.abortExceptions()).thenReturn(new Class[0]);
        when(retryAnnotation.useJitter()).thenReturn(false);
    }
}