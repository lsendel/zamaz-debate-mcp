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