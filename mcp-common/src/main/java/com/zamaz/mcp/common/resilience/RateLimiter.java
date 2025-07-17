package com.zamaz.mcp.common.resilience;

import java.lang.annotation.*;

/**
 * Annotation to apply rate limiting to methods.
 * This annotation triggers automatic rate limiting via AOP.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter {
    
    /**
     * Name of the rate limiter. If not specified, uses method signature.
     */
    String name() default "";
    
    /**
     * Number of permissions available during refresh period
     */
    int limitForPeriod() default 100;
    
    /**
     * Refresh period in seconds
     */
    int limitRefreshPeriodSeconds() default 1;
    
    /**
     * Thread timeout duration in seconds
     */
    int timeoutDurationSeconds() default 5;
    
    /**
     * Fallback method name when rate limit is exceeded
     */
    String fallbackMethod() default "";
}