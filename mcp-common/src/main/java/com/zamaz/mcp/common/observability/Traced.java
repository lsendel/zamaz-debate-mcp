package com.zamaz.mcp.common.observability;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking methods to be traced.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Traced {
    
    /**
     * The name of the span.
     * If not specified, the span name will be the class name and method name.
     *
     * @return the span name
     */
    String value() default "";
    
    /**
     * Whether to include method parameters in the span.
     *
     * @return true if method parameters should be included
     */
    boolean includeParameters() default true;
}
