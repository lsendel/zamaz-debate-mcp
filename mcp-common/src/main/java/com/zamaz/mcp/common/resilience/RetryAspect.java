package com.zamaz.mcp.common.resilience;

import com.zamaz.mcp.common.resilience.annotation.Retry;
import com.zamaz.mcp.common.resilience.exception.RetryConfigurationException;
import com.zamaz.mcp.common.resilience.exception.RetryExecutionException;
import com.zamaz.mcp.common.resilience.metrics.RetryMetricsCollector;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.trace.Span;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enhanced aspect that applies retry pattern to annotated methods with comprehensive
 * error handling, validation, and metrics collection.
 */
@Aspect
@Component
@Slf4j
@Order(2) // Execute after circuit breaker
public class RetryAspect {

    private final RetryRegistry retryRegistry;
    private final RetryMetricsCollector metricsCollector;
    private final Map<String, Counter> retryAttemptCounters = new ConcurrentHashMap<>();
    private final Map<String, Timer> retryExecutionTimers = new ConcurrentHashMap<>();
    
    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    public RetryAspect(RetryRegistry retryRegistry, RetryMetricsCollector metricsCollector) {
        this.retryRegistry = retryRegistry;
        this.metricsCollector = metricsCollector;
    }

    @Around("@annotation(retryAnnotation)")
    public Object applyRetry(@NotNull ProceedingJoinPoint joinPoint, @NotNull Retry retryAnnotation) throws Throwable {
        // Validate annotation parameters
        validateRetryConfiguration(retryAnnotation);
        
        String retryName = determineRetryName(retryAnnotation, joinPoint);
        Instant startTime = Instant.now();
        
        log.debug("Starting retry operation '{}' with configuration: maxAttempts={}, waitDuration={}ms", 
                 retryName, retryAnnotation.maxAttempts(), retryAnnotation.waitDurationMs());

        // Get or create retry configuration
        io.github.resilience4j.retry.Retry retry = createOrGetRetry(retryName, retryAnnotation);

        // Setup tracing and metrics
        Span currentSpan = setupTracing(retryName, retryAnnotation);
        Timer.Sample timerSample = startTimer(retryName);
        
        AtomicInteger attemptCount = new AtomicInteger(0);
        
        // Register enhanced event listeners
        registerRetryEventListeners(retry, retryName, currentSpan, attemptCount);

        try {
            // Execute with retry and enhanced error handling
            Object result = retry.executeCallable(() -> executeWithErrorHandling(joinPoint));
            
            // Record successful execution
            Duration totalDuration = Duration.between(startTime, Instant.now());
            int totalAttempts = attemptCount.get() + 1;
            
            metricsCollector.recordRetrySuccess(retryName, totalAttempts, totalDuration);
            recordSuccessMetrics(retryName, totalAttempts, totalDuration, currentSpan, timerSample);
            
            log.debug("Retry operation '{}' completed successfully after {} attempts in {}ms", 
                     retryName, totalAttempts, totalDuration.toMillis());
            
            return result;
            
        } catch (Exception e) {
            // Record failed execution
            Duration totalDuration = Duration.between(startTime, Instant.now());
            int totalAttempts = attemptCount.get();
            
            Throwable originalException = extractOriginalException(e);
            metricsCollector.recordRetryFailure(retryName, totalAttempts, totalDuration, originalException);
            recordFailureMetrics(retryName, totalAttempts, totalDuration, currentSpan, timerSample, originalException);
            
            log.error("Retry operation '{}' failed after {} attempts in {}ms", 
                     retryName, totalAttempts, totalDuration.toMillis(), originalException);
            
            throw new RetryExecutionException(retryName, totalAttempts, originalException);
        }
    }

    /**
     * Validates retry configuration parameters.
     */
    private void validateRetryConfiguration(Retry annotation) {
        if (annotation.maxAttempts() < 1) {
            throw new RetryConfigurationException("Max attempts must be at least 1, got: " + annotation.maxAttempts());
        }
        
        if (annotation.waitDurationMs() < 0) {
            throw new RetryConfigurationException("Wait duration must be non-negative, got: " + annotation.waitDurationMs());
        }
        
        if (annotation.maxWaitDurationMs() > 0 && annotation.maxWaitDurationMs() < annotation.waitDurationMs()) {
            throw new RetryConfigurationException("Max wait duration must be greater than or equal to wait duration");
        }
        
        if (annotation.exponentialBackoffMultiplier() < 1.0) {
            throw new RetryConfigurationException("Exponential backoff multiplier must be at least 1.0, got: " + annotation.exponentialBackoffMultiplier());
        }
        
        if (annotation.jitterFactor() < 0.0 || annotation.jitterFactor() > 1.0) {
            throw new RetryConfigurationException("Jitter factor must be between 0.0 and 1.0, got: " + annotation.jitterFactor());
        }
    }
    
    /**
     * Determines the retry name from annotation or method signature.
     */
    private String determineRetryName(Retry annotation, ProceedingJoinPoint joinPoint) {
        if (StringUtils.hasText(annotation.name())) {
            return annotation.name();
        }
        return joinPoint.getSignature().toShortString();
    }
    
    /**
     * Creates or retrieves a retry instance with appropriate configuration.
     */
    private io.github.resilience4j.retry.Retry createOrGetRetry(String retryName, Retry annotation) {
        try {
            if (isDefaultConfiguration(annotation)) {
                return retryRegistry.retry(retryName);
            } else {
                io.github.resilience4j.retry.RetryConfig config = buildConfig(annotation);
                return retryRegistry.retry(retryName, config);
            }
        } catch (Exception e) {
            throw new RetryConfigurationException("Failed to create retry configuration for: " + retryName, e);
        }
    }
    
    /**
     * Checks if the annotation uses default configuration values.
     */
    private boolean isDefaultConfiguration(Retry annotation) {
        return annotation.maxAttempts() == 3 && 
               annotation.waitDurationMs() == 1000 &&
               annotation.exponentialBackoffMultiplier() == 1.0 &&
               annotation.retryExceptions().length == 0 &&
               annotation.abortExceptions().length == 0;
    }
    
    /**
     * Sets up OpenTelemetry tracing for the retry operation.
     */
    private Span setupTracing(String retryName, Retry annotation) {
        Span currentSpan = Span.current();
        if (currentSpan != null) {
            currentSpan.setAttribute("retry.name", retryName);
            currentSpan.setAttribute("retry.max_attempts", annotation.maxAttempts());
            currentSpan.setAttribute("retry.wait_duration_ms", annotation.waitDurationMs());
        }
        return currentSpan;
    }
    
    /**
     * Starts a timer for measuring retry execution duration.
     */
    private Timer.Sample startTimer(String retryName) {
        if (meterRegistry != null) {
            Timer timer = retryExecutionTimers.computeIfAbsent(retryName, 
                name -> Timer.builder("retry.execution.duration")
                           .tag("retry.name", name)
                           .register(meterRegistry));
            return Timer.start(meterRegistry);
        }
        return null;
    }
    
    /**
     * Registers event listeners for retry events with enhanced logging and metrics.
     */
    private void registerRetryEventListeners(io.github.resilience4j.retry.Retry retry, 
                                           String retryName, Span currentSpan, AtomicInteger attemptCount) {
        retry.getEventPublisher()
            .onRetry(event -> {
                int attempt = attemptCount.incrementAndGet();
                Throwable exception = event.getLastThrowable();
                
                // Record attempt in metrics
                metricsCollector.recordRetryAttempt(retryName, attempt, exception);
                
                // Increment counter metric
                if (meterRegistry != null) {
                    retryAttemptCounters.computeIfAbsent(retryName,
                        name -> Counter.builder("retry.attempts")
                                     .tag("retry.name", name)
                                     .register(meterRegistry))
                        .increment();
                }
                
                log.warn("Retry attempt {} for '{}': {}", 
                    attempt, retryName, exception.getMessage());
                
                // Add tracing event
                if (currentSpan != null) {
                    currentSpan.setAttribute("retry.attempt", attempt);
                    currentSpan.addEvent("retry_attempt", 
                        io.opentelemetry.api.common.Attributes.builder()
                            .put("attempt", attempt)
                            .put("error", exception.getMessage())
                            .build());
                }
            });
    }
    
    /**
     * Executes the target method with enhanced error handling.
     */
    private Object executeWithErrorHandling(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            // Preserve the original exception without wrapping
            throw throwable;
        }
    }
    
    /**
     * Records success metrics for monitoring and analysis.
     */
    private void recordSuccessMetrics(String retryName, int totalAttempts, Duration totalDuration, 
                                    Span currentSpan, Timer.Sample timerSample) {
        if (timerSample != null) {
            timerSample.stop(retryExecutionTimers.get(retryName));
        }
        
        if (currentSpan != null) {
            currentSpan.setAttribute("retry.succeeded", true);
            currentSpan.setAttribute("retry.total_attempts", totalAttempts);
            currentSpan.setAttribute("retry.total_duration_ms", totalDuration.toMillis());
        }
    }
    
    /**
     * Records failure metrics for monitoring and analysis.
     */
    private void recordFailureMetrics(String retryName, int totalAttempts, Duration totalDuration,
                                    Span currentSpan, Timer.Sample timerSample, Throwable exception) {
        if (timerSample != null) {
            timerSample.stop(retryExecutionTimers.get(retryName));
        }
        
        if (currentSpan != null) {
            currentSpan.setAttribute("retry.succeeded", false);
            currentSpan.setAttribute("retry.total_attempts", totalAttempts);
            currentSpan.setAttribute("retry.total_duration_ms", totalDuration.toMillis());
            currentSpan.recordException(exception);
        }
    }
    
    /**
     * Extracts the original exception from wrapped exceptions.
     */
    private Throwable extractOriginalException(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current instanceof RuntimeException) {
            current = current.getCause();
        }
        return current;
    }
    
    /**
     * Build retry configuration from annotation
     */
    private io.github.resilience4j.retry.RetryConfig buildConfig(Retry annotation) {
        io.github.resilience4j.retry.RetryConfig.Builder<Object> builder = 
            io.github.resilience4j.retry.RetryConfig.custom()
                .maxAttempts(annotation.maxAttempts());

        // Configure wait duration
        if (annotation.exponentialBackoffMultiplier() > 1.0) {
            // Use exponential backoff
            if (annotation.useJitter()) {
                builder.intervalFunction(io.github.resilience4j.retry.IntervalFunction
                    .ofExponentialRandomBackoff(
                        annotation.waitDurationMs(),
                        annotation.exponentialBackoffMultiplier(),
                        annotation.jitterFactor(),
                        annotation.maxWaitDurationMs()
                    ));
            } else {
                builder.intervalFunction(io.github.resilience4j.retry.IntervalFunction
                    .ofExponentialBackoff(
                        annotation.waitDurationMs(),
                        annotation.exponentialBackoffMultiplier(),
                        annotation.maxWaitDurationMs()
                    ));
            }
        } else {
            // Use fixed wait duration
            builder.waitDuration(Duration.ofMillis(annotation.waitDurationMs()));
        }

        // Configure retry exceptions
        if (annotation.retryExceptions().length > 0) {
            builder.retryOnException(throwable -> {
                Class<?> throwableClass = throwable.getClass();
                return Arrays.stream(annotation.retryExceptions())
                    .anyMatch(exceptionClass -> exceptionClass.isAssignableFrom(throwableClass));
            });
        }

        // Configure abort exceptions
        if (annotation.abortExceptions().length > 0) {
            builder.ignoreExceptions(annotation.abortExceptions());
        }

        return builder.build();
    }
}