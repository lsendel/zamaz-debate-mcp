package com.zamaz.mcp.common.resilience;

import com.zamaz.mcp.common.resilience.annotation.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.opentelemetry.api.trace.Span;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.Callable;

/**
 * Aspect that applies circuit breaker pattern to annotated methods.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1) // Execute before other aspects like retry
public class CircuitBreakerAspect {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Around("@annotation(circuitBreakerAnnotation)")
    public Object applyCircuitBreaker(ProceedingJoinPoint joinPoint, CircuitBreaker circuitBreakerAnnotation) throws Throwable {
        String circuitBreakerName = circuitBreakerAnnotation.name();
        
        // Get or create circuit breaker
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker;
        if (circuitBreakerAnnotation.useDefault()) {
            circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
        } else {
            // Create custom configuration
            CircuitBreakerConfig config = buildConfig(circuitBreakerAnnotation);
            circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName, config);
        }

        // Add tracing attributes
        Span currentSpan = Span.current();
        if (currentSpan != null) {
            currentSpan.setAttribute("circuitbreaker.name", circuitBreakerName);
            currentSpan.setAttribute("circuitbreaker.state", circuitBreaker.getState().toString());
        }

        // Create callable from join point
        Callable<Object> callable = () -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        };

        try {
            // Execute with circuit breaker
            Object result = circuitBreaker.executeCallable(callable);
            
            // Update span with success
            if (currentSpan != null) {
                currentSpan.setAttribute("circuitbreaker.call_permitted", true);
                currentSpan.setAttribute("circuitbreaker.success", true);
            }
            
            return result;
            
        } catch (CallNotPermittedException e) {
            // Circuit is open
            log.warn("Circuit breaker {} is OPEN, call not permitted", circuitBreakerName);
            
            // Update span
            if (currentSpan != null) {
                currentSpan.setAttribute("circuitbreaker.call_permitted", false);
                currentSpan.setAttribute("circuitbreaker.fallback_used", true);
            }
            
            // Try fallback method if specified
            String fallbackMethodName = circuitBreakerAnnotation.fallbackMethod();
            if (!fallbackMethodName.isEmpty()) {
                return invokeFallbackMethod(joinPoint, fallbackMethodName, e);
            }
            
            throw e;
            
        } catch (Exception e) {
            // Unwrap if it's a wrapper exception
            Throwable cause = e.getCause();
            if (cause != null && cause instanceof Throwable) {
                
                // Update span with failure
                if (currentSpan != null) {
                    currentSpan.setAttribute("circuitbreaker.call_permitted", true);
                    currentSpan.setAttribute("circuitbreaker.success", false);
                    currentSpan.recordException(cause);
                }
                
                throw cause;
            }
            
            if (currentSpan != null) {
                currentSpan.setAttribute("circuitbreaker.call_permitted", true);
                currentSpan.setAttribute("circuitbreaker.success", false);
                currentSpan.recordException(e);
            }
            
            throw e;
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