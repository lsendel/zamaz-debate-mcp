package com.zamaz.mcp.common.resilience;

import java.lang.annotation.*;

/**
 * Annotation to apply retry pattern to methods.
 * This annotation triggers automatic retry logic via AOP.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Retry {
    
    /**
     * Name of the retry configuration. If not specified, uses method signature.
     */
    String name() default "";
    
    /**
     * Maximum number of retry attempts
     */
    int maxAttempts() default 3;
    
    /**
     * Wait duration between retries in milliseconds
     */
    long waitDurationMs() default 1000;
    
    /**
     * Whether to use exponential backoff
     */
    boolean exponentialBackoff() default true;
    
    /**
     * Exponential backoff multiplier
     */
    double backoffMultiplier() default 2.0;
    
    /**
     * Whether to add jitter to retry intervals
     */
    boolean jitter() default true;
    
    /**
     * Fallback method name. Must have the same signature as the protected method.
     */
    String fallbackMethod() default "";
    
    /**
     * Exceptions to retry on
     */
    Class<? extends Throwable>[] retryOn() default {Exception.class};
    
    /**
     * Exceptions to abort retry on
     */
    Class<? extends Throwable>[] abortOn() default {};
}