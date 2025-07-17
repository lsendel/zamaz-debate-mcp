package com.zamaz.mcp.common.resilience;

import java.lang.annotation.*;

/**
 * Annotation to apply circuit breaker pattern to methods.
 * This annotation triggers automatic circuit breaker protection via AOP.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CircuitBreaker {
    
    /**
     * Name of the circuit breaker. If not specified, uses method signature.
     */
    String name() default "";
    
    /**
     * Fallback method name. Must have the same signature as the protected method.
     */
    String fallbackMethod() default "";
    
    /**
     * Whether to use the default circuit breaker configuration or a custom one.
     */
    boolean useDefault() default true;
    
    /**
     * Custom failure rate threshold (percentage). Only used if useDefault is false.
     */
    float failureRateThreshold() default 50.0f;
    
    /**
     * Custom slow call rate threshold (percentage). Only used if useDefault is false.
     */
    float slowCallRateThreshold() default 100.0f;
    
    /**
     * Custom slow call duration threshold in milliseconds. Only used if useDefault is false.
     */
    long slowCallDurationThresholdMs() default 2000;
    
    /**
     * Custom sliding window size. Only used if useDefault is false.
     */
    int slidingWindowSize() default 100;
    
    /**
     * Custom wait duration in open state in seconds. Only used if useDefault is false.
     */
    long waitDurationInOpenStateSeconds() default 60;
    
    /**
     * Exceptions to ignore (not count as failures)
     */
    Class<? extends Throwable>[] ignoreExceptions() default {};
    
    /**
     * Exceptions to record as failures
     */
    Class<? extends Throwable>[] recordExceptions() default {Exception.class};
}