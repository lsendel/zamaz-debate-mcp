package com.zamaz.mcp.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that require specific permissions.
 * Used for method-level authorization.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    
    /**
     * The permission required to access this method.
     * Format: "service:action" (e.g., "debate:create", "context:read")
     */
    String value();
    
    /**
     * Whether organization-level access is required.
     * If true, the user must have access to the organization in context.
     */
    boolean organizationAccess() default true;
    
    /**
     * Whether the user must be the owner of the resource.
     * If true, additional ownership checks will be performed.
     */
    boolean requiresOwnership() default false;
}