package com.zamaz.mcp.common.resilience;

import com.zamaz.mcp.common.resilience.annotation.CircuitBreaker;
import com.zamaz.mcp.common.resilience.exception.CircuitBreakerConfigurationException;
import com.zamaz.mcp.common.resilience.exception.CircuitBreakerExecutionException;
import com.zamaz.mcp.common.resilience.metrics.CircuitBreakerMetricsCollector;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.validation.constraints.NotNull;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced aspect that applies circuit breaker pattern to annotated methods with comprehensive
 * error handling, validation, and metrics collection.
 */
@Aspect
@Component
@Slf4j
@Order(1) // Execute before other aspects like retry
public class CircuitBreakerAspect {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final CircuitBreakerMetricsCollector metricsCollector;
    private final Map<String, Counter> circuitBreakerCounters = new ConcurrentHashMap<>();
    private final Map<String, Timer> circuitBreakerTimers = new ConcurrentHashMap<>();
    
    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    public CircuitBreakerAspect(CircuitBreakerRegistry circuitBreakerRegistry, 
                               CircuitBreakerMetricsCollector metricsCollector) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.metricsCollector = metricsCollector;
    }

    @Around("@annotation(circuitBreakerAnnotation)")
    public Object applyCircuitBreaker(@NotNull ProceedingJoinPoint joinPoint, 
                                    @NotNull CircuitBreaker circuitBreakerAnnotation) throws Throwable {
        // Validate annotation parameters
        validateCircuitBreakerConfiguration(circuitBreakerAnnotation);
        
        String circuitBreakerName = determineCircuitBreakerName(circuitBreakerAnnotation, joinPoint);
        Instant startTime = Instant.now();
        
        log.debug("Starting circuit breaker operation '{}' with configuration: failureRate={}, slowCallRate={}", 
                 circuitBreakerName, circuitBreakerAnnotation.failureRateThreshold(), 
                 circuitBreakerAnnotation.slowCallRateThreshold());

        // Get or create circuit breaker
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker = 
            createOrGetCircuitBreaker(circuitBreakerName, circuitBreakerAnnotation);
        
        // Setup tracing and metrics
        Span currentSpan = setupTracing(circuitBreakerName, circuitBreaker);
        Timer.Sample timerSample = startTimer(circuitBreakerName);
        
        // Register state change listener
        registerStateChangeListener(circuitBreaker, circuitBreakerName);

        try {
            // Execute with circuit breaker and enhanced error handling
            Object result = circuitBreaker.executeCallable(() -> executeWithErrorHandling(joinPoint));
            
            // Record successful execution
            Duration executionDuration = Duration.between(startTime, Instant.now());
            metricsCollector.recordSuccessfulExecution(circuitBreakerName, executionDuration);
            recordSuccessMetrics(circuitBreakerName, executionDuration, currentSpan, timerSample);
            
            log.debug("Circuit breaker operation '{}' completed successfully in {}ms (state: {})", 
                     circuitBreakerName, executionDuration.toMillis(), circuitBreaker.getState());
            
            return result;
            
        } catch (CallNotPermittedException e) {
            // Circuit is open - handle fallback
            Duration executionDuration = Duration.between(startTime, Instant.now());
            metricsCollector.recordCallNotPermitted(circuitBreakerName);
            recordCallNotPermittedMetrics(circuitBreakerName, currentSpan, timerSample);
            
            log.warn("Circuit breaker '{}' is OPEN, call not permitted (state: {})", 
                    circuitBreakerName, circuitBreaker.getState());
            
            // Try fallback method if specified
            String fallbackMethodName = circuitBreakerAnnotation.fallbackMethod();
            if (StringUtils.hasText(fallbackMethodName)) {
                return executeFallbackWithMetrics(joinPoint, fallbackMethodName, e, 
                                                circuitBreakerName, currentSpan, startTime);
            }
            
            throw new CircuitBreakerExecutionException(circuitBreakerName, circuitBreaker.getState(), e);
            
        } catch (Exception e) {
            // Record failed execution
            Duration executionDuration = Duration.between(startTime, Instant.now());
            Throwable originalException = extractOriginalException(e);
            
            metricsCollector.recordFailedExecution(circuitBreakerName, executionDuration, originalException);
            recordFailureMetrics(circuitBreakerName, executionDuration, currentSpan, timerSample, originalException);
            
            log.error("Circuit breaker operation '{}' failed in {}ms (state: {})", 
                     circuitBreakerName, executionDuration.toMillis(), circuitBreaker.getState(), originalException);
            
            throw new CircuitBreakerExecutionException(circuitBreakerName, circuitBreaker.getState(), originalException);
        }
    }

    /**
     * Validates circuit breaker configuration parameters.
     */
    private void validateCircuitBreakerConfiguration(CircuitBreaker annotation) {
        if (annotation.failureRateThreshold() < 0 || annotation.failureRateThreshold() > 100) {
            throw new CircuitBreakerConfigurationException(
                "Failure rate threshold must be between 0 and 100, got: " + annotation.failureRateThreshold());
        }
        
        if (annotation.slowCallRateThreshold() < 0 || annotation.slowCallRateThreshold() > 100) {
            throw new CircuitBreakerConfigurationException(
                "Slow call rate threshold must be between 0 and 100, got: " + annotation.slowCallRateThreshold());
        }
        
        if (annotation.slowCallDurationThresholdMs() < 0) {
            throw new CircuitBreakerConfigurationException(
                "Slow call duration threshold must be non-negative, got: " + annotation.slowCallDurationThresholdMs());
        }
        
        if (annotation.permittedCallsInHalfOpenState() < 1) {
            throw new CircuitBreakerConfigurationException(
                "Permitted calls in half-open state must be at least 1, got: " + annotation.permittedCallsInHalfOpenState());
        }
        
        if (annotation.slidingWindowSize() < 1) {
            throw new CircuitBreakerConfigurationException(
                "Sliding window size must be at least 1, got: " + annotation.slidingWindowSize());
        }
        
        if (annotation.minimumNumberOfCalls() < 1) {
            throw new CircuitBreakerConfigurationException(
                "Minimum number of calls must be at least 1, got: " + annotation.minimumNumberOfCalls());
        }
        
        if (annotation.waitDurationInOpenStateSeconds() < 1) {
            throw new CircuitBreakerConfigurationException(
                "Wait duration in open state must be at least 1 second, got: " + annotation.waitDurationInOpenStateSeconds());
        }
    }
    
    /**
     * Determines the circuit breaker name from annotation or method signature.
     */
    private String determineCircuitBreakerName(CircuitBreaker annotation, ProceedingJoinPoint joinPoint) {
        if (StringUtils.hasText(annotation.name())) {
            return annotation.name();
        }
        return joinPoint.getSignature().toShortString();
    }
    
    /**
     * Creates or retrieves a circuit breaker instance with appropriate configuration.
     */
    private io.github.resilience4j.circuitbreaker.CircuitBreaker createOrGetCircuitBreaker(
            String circuitBreakerName, CircuitBreaker annotation) {
        try {
            if (annotation.useDefault()) {
                return circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
            } else {
                CircuitBreakerConfig config = buildConfig(annotation);
                return circuitBreakerRegistry.circuitBreaker(circuitBreakerName, config);
            }
        } catch (Exception e) {
            throw new CircuitBreakerConfigurationException(
                "Failed to create circuit breaker configuration for: " + circuitBreakerName, e);
        }
    }
    
    /**
     * Sets up OpenTelemetry tracing for the circuit breaker operation.
     */
    private Span setupTracing(String circuitBreakerName, io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker) {
        Span currentSpan = Span.current();
        if (currentSpan != null) {
            currentSpan.setAttribute("circuitbreaker.name", circuitBreakerName);
            currentSpan.setAttribute("circuitbreaker.state", circuitBreaker.getState().toString());
            currentSpan.setAttribute("circuitbreaker.failure_rate", circuitBreaker.getMetrics().getFailureRate());
        }
        return currentSpan;
    }
    
    /**
     * Starts a timer for measuring circuit breaker execution duration.
     */
    private Timer.Sample startTimer(String circuitBreakerName) {
        if (meterRegistry != null) {
            Timer timer = circuitBreakerTimers.computeIfAbsent(circuitBreakerName, 
                name -> Timer.builder("circuitbreaker.execution.duration")
                           .tag("circuitbreaker.name", name)
                           .register(meterRegistry));
            return Timer.start(meterRegistry);
        }
        return null;
    }
    
    /**
     * Registers state change listener for circuit breaker events.
     */
    private void registerStateChangeListener(io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker, 
                                           String circuitBreakerName) {
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> {
                log.info("Circuit breaker '{}' state transition: {} -> {}", 
                    circuitBreakerName, event.getStateTransition().getFromState(), 
                    event.getStateTransition().getToState());
                
                metricsCollector.recordStateChange(circuitBreakerName, 
                    event.getStateTransition().getFromState(), 
                    event.getStateTransition().getToState());
                
                // Update counter metric
                if (meterRegistry != null) {
                    circuitBreakerCounters.computeIfAbsent(circuitBreakerName + ".state.transitions",
                        name -> Counter.builder("circuitbreaker.state.transitions")
                                     .tag("circuitbreaker.name", circuitBreakerName)
                                     .register(meterRegistry))
                        .increment();
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
    private void recordSuccessMetrics(String circuitBreakerName, Duration executionDuration, 
                                    Span currentSpan, Timer.Sample timerSample) {
        if (timerSample != null) {
            timerSample.stop(circuitBreakerTimers.get(circuitBreakerName));
        }
        
        if (currentSpan != null) {
            currentSpan.setAttribute("circuitbreaker.call_permitted", true);
            currentSpan.setAttribute("circuitbreaker.success", true);
            currentSpan.setAttribute("circuitbreaker.execution_duration_ms", executionDuration.toMillis());
        }
    }
    
    /**
     * Records metrics for calls not permitted by circuit breaker.
     */
    private void recordCallNotPermittedMetrics(String circuitBreakerName, Span currentSpan, Timer.Sample timerSample) {
        if (timerSample != null) {
            timerSample.stop(circuitBreakerTimers.get(circuitBreakerName));
        }
        
        if (meterRegistry != null) {
            circuitBreakerCounters.computeIfAbsent(circuitBreakerName + ".calls.not.permitted",
                name -> Counter.builder("circuitbreaker.calls.not.permitted")
                             .tag("circuitbreaker.name", circuitBreakerName)
                             .register(meterRegistry))
                .increment();
        }
        
        if (currentSpan != null) {
            currentSpan.setAttribute("circuitbreaker.call_permitted", false);
            currentSpan.setAttribute("circuitbreaker.fallback_attempted", true);
        }
    }
    
    /**
     * Records failure metrics for monitoring and analysis.
     */
    private void recordFailureMetrics(String circuitBreakerName, Duration executionDuration,
                                    Span currentSpan, Timer.Sample timerSample, Throwable exception) {
        if (timerSample != null) {
            timerSample.stop(circuitBreakerTimers.get(circuitBreakerName));
        }
        
        if (currentSpan != null) {
            currentSpan.setAttribute("circuitbreaker.call_permitted", true);
            currentSpan.setAttribute("circuitbreaker.success", false);
            currentSpan.setAttribute("circuitbreaker.execution_duration_ms", executionDuration.toMillis());
            currentSpan.recordException(exception);
        }
    }
    
    /**
     * Executes fallback method with metrics tracking.
     */
    private Object executeFallbackWithMetrics(ProceedingJoinPoint joinPoint, String fallbackMethodName, 
                                            Exception exception, String circuitBreakerName, 
                                            Span currentSpan, Instant startTime) throws Throwable {
        Instant fallbackStartTime = Instant.now();
        boolean fallbackSuccessful = false;
        
        try {
            Object result = invokeFallbackMethod(joinPoint, fallbackMethodName, exception);
            fallbackSuccessful = true;
            
            Duration fallbackDuration = Duration.between(fallbackStartTime, Instant.now());
            metricsCollector.recordFallbackExecution(circuitBreakerName, true, fallbackDuration);
            
            if (currentSpan != null) {
                currentSpan.setAttribute("circuitbreaker.fallback_used", true);
                currentSpan.setAttribute("circuitbreaker.fallback_successful", true);
                currentSpan.setAttribute("circuitbreaker.fallback_duration_ms", fallbackDuration.toMillis());
            }
            
            log.debug("Fallback method '{}' executed successfully for circuit breaker '{}' in {}ms", 
                     fallbackMethodName, circuitBreakerName, fallbackDuration.toMillis());
            
            return result;
            
        } catch (Throwable fallbackException) {
            Duration fallbackDuration = Duration.between(fallbackStartTime, Instant.now());
            metricsCollector.recordFallbackExecution(circuitBreakerName, false, fallbackDuration);
            
            if (currentSpan != null) {
                currentSpan.setAttribute("circuitbreaker.fallback_used", true);
                currentSpan.setAttribute("circuitbreaker.fallback_successful", false);
                currentSpan.setAttribute("circuitbreaker.fallback_duration_ms", fallbackDuration.toMillis());
                currentSpan.recordException(fallbackException);
            }
            
            log.error("Fallback method '{}' failed for circuit breaker '{}': {}", 
                     fallbackMethodName, circuitBreakerName, fallbackException.getMessage());
            
            throw fallbackException;
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
     * Build circuit breaker configuration from annotation
     */
    private CircuitBreakerConfig buildConfig(CircuitBreaker annotation) {
        CircuitBreakerConfig.SlidingWindowType windowType = 
            "TIME_BASED".equals(annotation.slidingWindowType()) 
                ? CircuitBreakerConfig.SlidingWindowType.TIME_BASED 
                : CircuitBreakerConfig.SlidingWindowType.COUNT_BASED;

        CircuitBreakerConfig.Builder configBuilder = CircuitBreakerConfig.custom()
            .failureRateThreshold(annotation.failureRateThreshold())
            .slowCallRateThreshold(annotation.slowCallRateThreshold())
            .slowCallDurationThreshold(Duration.ofMillis(annotation.slowCallDurationThresholdMs()))
            .permittedNumberOfCallsInHalfOpenState(annotation.permittedCallsInHalfOpenState())
            .slidingWindowType(windowType)
            .slidingWindowSize(annotation.slidingWindowSize())
            .minimumNumberOfCalls(annotation.minimumNumberOfCalls())
            .waitDurationInOpenState(Duration.ofSeconds(annotation.waitDurationInOpenStateSeconds()))
            .automaticTransitionFromOpenToHalfOpenEnabled(annotation.automaticTransitionEnabled());

        // Configure record exceptions
        if (annotation.recordExceptions().length > 0) {
            configBuilder.recordExceptions(annotation.recordExceptions());
        }

        // Configure ignore exceptions
        if (annotation.ignoreExceptions().length > 0) {
            configBuilder.ignoreExceptions(annotation.ignoreExceptions());
        }

        return configBuilder.build();
    }

    /**
     * Invoke fallback method
     */
    private Object invokeFallbackMethod(ProceedingJoinPoint joinPoint, String fallbackMethodName, Exception exception) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> targetClass = joinPoint.getTarget().getClass();
        Method method = signature.getMethod();
        
        // Try to find fallback method with exception parameter
        Method fallbackMethod = null;
        try {
            Class<?>[] paramTypes = method.getParameterTypes();
            Class<?>[] fallbackParamTypes = Arrays.copyOf(paramTypes, paramTypes.length + 1);
            fallbackParamTypes[paramTypes.length] = Exception.class;
            fallbackMethod = targetClass.getMethod(fallbackMethodName, fallbackParamTypes);
        } catch (NoSuchMethodException e) {
            // Try without exception parameter
            try {
                fallbackMethod = targetClass.getMethod(fallbackMethodName, method.getParameterTypes());
            } catch (NoSuchMethodException ex) {
                log.error("Fallback method {} not found in class {}", fallbackMethodName, targetClass.getName());
                throw exception;
            }
        }
        
        // Invoke fallback method
        Object[] args = joinPoint.getArgs();
        if (fallbackMethod.getParameterCount() > method.getParameterCount()) {
            // Include exception as last parameter
            Object[] fallbackArgs = Arrays.copyOf(args, args.length + 1);
            fallbackArgs[args.length] = exception;
            return fallbackMethod.invoke(joinPoint.getTarget(), fallbackArgs);
        } else {
            return fallbackMethod.invoke(joinPoint.getTarget(), args);
        }
    }
}