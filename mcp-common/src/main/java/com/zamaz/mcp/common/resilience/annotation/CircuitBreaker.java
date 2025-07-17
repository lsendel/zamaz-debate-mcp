package com.zamaz.mcp.common.resilience.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to apply circuit breaker pattern to methods.
 * When applied, the method will be protected by a circuit breaker
 * that can prevent cascading failures.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CircuitBreaker {
    
    /**
     * The name of the circuit breaker
     */
    String name();
    
    /**
     * Whether to use the default configuration
     */
    boolean useDefault() default true;
    
    /**
     * The failure rate threshold percentage (0-100)
     */
    float failureRateThreshold() default 50.0f;
    
    /**
     * The slow call rate threshold percentage (0-100)
     */
    float slowCallRateThreshold() default 100.0f;
    
    /**
     * The slow call duration threshold in milliseconds
     */
    long slowCallDurationThresholdMs() default 2000;
    
    /**
     * The number of permitted calls when the circuit breaker is half open
     */
    int permittedCallsInHalfOpenState() default 10;
    
    /**
     * The sliding window type (TIME_BASED or COUNT_BASED)
     */
    String slidingWindowType() default "TIME_BASED";
    
    /**
     * The sliding window size
     */
    int slidingWindowSize() default 100;
    
    /**
     * The minimum number of calls required before calculating error rate
     */
    int minimumNumberOfCalls() default 10;
    
    /**
     * The wait duration in open state in seconds
     */
    long waitDurationInOpenStateSeconds() default 60;
    
    /**
     * Whether automatic transition from open to half-open is enabled
     */
    boolean automaticTransitionEnabled() default true;
    
    /**
     * The fallback method name to call when circuit is open
     */
    String fallbackMethod() default "";
    
    /**
     * Exception types to record as failures
     */
    Class<? extends Throwable>[] recordExceptions() default {Exception.class};
    
    /**
     * Exception types to ignore (not count as failures)
     */
    Class<? extends Throwable>[] ignoreExceptions() default {};
}