package com.zamaz.mcp.common.resilience.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to apply rate limiting to methods.
 * When applied, the method calls will be limited to a specified rate.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimiter {
    
    /**
     * The name of the rate limiter
     */
    String name() default "";
    
    /**
     * The number of permissions available during one limit refresh period
     */
    int limitForPeriod() default 100;
    
    /**
     * The period of a limit refresh in milliseconds
     */
    long limitRefreshPeriodMs() default 1000;
    
    /**
     * The default wait time in milliseconds for a permission
     */
    long timeoutDurationMs() default 5000;
}