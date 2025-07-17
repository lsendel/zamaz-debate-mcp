package com.zamaz.mcp.common.versioning;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify API version for controllers and methods.
 * Supports multiple versioning strategies.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiVersioning {
    
    /**
     * API version(s) supported by this endpoint
     */
    String[] value() default {"1"};
    
    /**
     * Minimum supported version
     */
    String min() default "";
    
    /**
     * Maximum supported version
     */
    String max() default "";
    
    /**
     * Whether this version is deprecated
     */
    boolean deprecated() default false;
    
    /**
     * Deprecation message
     */
    String deprecationMessage() default "";
    
    /**
     * Version when this endpoint will be removed
     */
    String removedInVersion() default "";
    
    /**
     * Whether to include version in response headers
     */
    boolean includeInHeaders() default true;
}