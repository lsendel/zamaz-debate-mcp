package com.zamaz.mcp.common.api;

import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API versioning annotations for consistent versioning across all services.
 */
public final class ApiVersioning {

    /**
     * Marks a controller or method as version 1.
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @RequestMapping("/api/v1")
    public @interface V1 {
    }

    /**
     * Marks a controller or method as version 2.
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @RequestMapping("/api/v2")
    public @interface V2 {
    }

    /**
     * Marks a controller or method as version 3.
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @RequestMapping("/api/v3")
    public @interface V3 {
    }

    /**
     * Marks a controller or method as deprecated.
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Deprecated {
        /**
         * The version when this API will be removed.
         */
        String removedInVersion() default "";
        
        /**
         * Alternative API to use instead.
         */
        String useInstead() default "";
        
        /**
         * Additional deprecation message.
         */
        String message() default "";
    }

    /**
     * Marks a controller or method as experimental.
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Experimental {
        /**
         * Description of the experimental feature.
         */
        String description() default "";
        
        /**
         * Expected stabilization version.
         */
        String stabilizationVersion() default "";
    }

    /**
     * Marks a controller or method as requiring authentication.
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RequiresAuth {
        /**
         * Required roles for access.
         */
        String[] roles() default {};
        
        /**
         * Required permissions for access.
         */
        String[] permissions() default {};
    }

    /**
     * Marks a controller or method as rate limited.
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RateLimit {
        /**
         * Number of requests allowed per time window.
         */
        int value() default 100;
        
        /**
         * Time window in seconds.
         */
        int windowSeconds() default 60;
        
        /**
         * Rate limit scope (user, ip, global).
         */
        String scope() default "user";
    }

    private ApiVersioning() {
        // Utility class - prevent instantiation
    }
}