package com.zamaz.mcp.common.resilience;

import java.lang.annotation.*;

/**
 * Annotation for applying intelligent rate limiting to MCP tool endpoints.
 * Provides smart defaults based on operation type and cost.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface McpRateLimit {
    
    /**
     * Type of MCP operation for intelligent rate limiting.
     */
    enum OperationType {
        /**
         * Read operations - relatively cheap, higher limits
         */
        READ,
        
        /**
         * Write operations - moderate cost, moderate limits
         */
        WRITE,
        
        /**
         * Expensive operations like LLM calls - strict limits
         */
        EXPENSIVE,
        
        /**
         * Administrative operations - very strict limits
         */
        ADMIN,
        
        /**
         * Custom operation - use specified limits
         */
        CUSTOM
    }

    /**
     * Type of operation for intelligent rate limiting defaults
     */
    OperationType operationType() default OperationType.READ;

    /**
     * Custom rate limiter name (optional)
     */
    String name() default "";

    /**
     * Custom limit for period (overrides operation type defaults)
     */
    int limitForPeriod() default -1;

    /**
     * Custom refresh period in seconds (overrides operation type defaults)
     */
    int limitRefreshPeriodSeconds() default -1;

    /**
     * Custom timeout duration in seconds (overrides operation type defaults)
     */
    int timeoutDurationSeconds() default -1;

    /**
     * Whether to apply organization-level rate limiting
     */
    boolean organizationLevel() default true;

    /**
     * Whether to apply user-level rate limiting
     */
    boolean userLevel() default true;

    /**
     * Fallback method to call when rate limit is exceeded
     */
    String fallbackMethod() default "";

    /**
     * Custom error message when rate limit is exceeded
     */
    String rateLimitMessage() default "";

    /**
     * Whether this rate limit applies to all users in the organization
     */
    boolean sharedWithOrganization() default false;

    /**
     * Priority for rate limiting (higher priority gets more resources)
     */
    int priority() default 0;
}