package com.zamaz.mcp.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods for automatic auditing
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    
    /**
     * The type of audit event
     */
    AuditEvent.AuditEventType eventType() default AuditEvent.AuditEventType.BUSINESS_EVENT;
    
    /**
     * The audit action
     */
    AuditEvent.AuditAction action();
    
    /**
     * The resource type being audited
     */
    String resourceType();
    
    /**
     * Description of the operation
     */
    String description() default "";
    
    /**
     * Risk level of the operation
     */
    AuditEvent.RiskLevel riskLevel() default AuditEvent.RiskLevel.LOW;
    
    /**
     * Whether to include method arguments in audit metadata
     */
    boolean includeArguments() default false;
    
    /**
     * Whether to audit only failures
     */
    boolean auditOnlyFailures() default false;
    
    /**
     * Whether to extract resource ID from the method result
     */
    boolean extractResourceIdFromResult() default true;
    
    /**
     * Argument types to exclude from auditing (for security)
     */
    Class<?>[] excludeArgumentTypes() default {
        javax.servlet.http.HttpServletRequest.class,
        javax.servlet.http.HttpServletResponse.class,
        org.springframework.security.core.Authentication.class
    };
    
    /**
     * Compliance tags to add to the audit event
     */
    String[] complianceTags() default {};
}