package com.zamaz.mcp.common.resilience.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to apply retry pattern to methods.
 * When applied, the method will be retried on failure with exponential backoff.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Retry {
    
    /**
     * The name of the retry configuration
     */
    String name() default "";
    
    /**
     * Maximum number of retry attempts (including initial call)
     */
    int maxAttempts() default 3;
    
    /**
     * Initial wait duration between retries in milliseconds
     */
    long waitDurationMs() default 1000;
    
    /**
     * Exponential backoff multiplier
     */
    double exponentialBackoffMultiplier() default 2.0;
    
    /**
     * Maximum wait duration between retries in milliseconds
     */
    long maxWaitDurationMs() default 60000;
    
    /**
     * Whether to use jitter (randomization) in wait time
     */
    boolean useJitter() default true;
    
    /**
     * Jitter factor (0.0 to 1.0)
     */
    double jitterFactor() default 0.5;
    
    /**
     * Exception types to retry on
     */
    Class<? extends Throwable>[] retryExceptions() default {Exception.class};
    
    /**
     * Exception types to abort retry on (not retry)
     */
    Class<? extends Throwable>[] abortExceptions() default {};
    
    /**
     * Whether to retry on result predicate
     */
    String retryOnResultPredicate() default "";
}