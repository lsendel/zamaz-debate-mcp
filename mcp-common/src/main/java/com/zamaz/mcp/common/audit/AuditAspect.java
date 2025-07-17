package com.zamaz.mcp.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Aspect for automatic auditing of annotated methods
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {
    
    private final AuditService auditService;
    
    /**
     * Around advice for methods annotated with @Auditable
     */
    @Around("@annotation(auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();
        
        AuditEventBuilder builder = AuditService.builder()
            .eventType(auditable.eventType())
            .action(auditable.action())
            .resourceType(auditable.resourceType())
            .description(auditable.description().isEmpty() ? 
                className + "." + methodName : auditable.description());
        
        // Add method arguments as metadata if enabled
        if (auditable.includeArguments()) {
            Map<String, String> metadata = new HashMap<>();
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null && !auditable.excludeArgumentTypes().contains(args[i].getClass())) {
                    metadata.put("arg" + i, args[i].toString());
                }
            }
            builder.metadata(metadata);
        }
        
        try {
            Object result = joinPoint.proceed();
            
            // Success case
            long duration = System.currentTimeMillis() - startTime;
            builder.success()
                   .duration(duration)
                   .riskLevel(auditable.riskLevel());
            
            // Extract resource ID from result if needed
            if (auditable.extractResourceIdFromResult() && result != null) {
                String resourceId = extractResourceId(result);
                if (resourceId != null) {
                    builder.resourceId(resourceId);
                }
            }
            
            auditService.audit(builder);
            return result;
            
        } catch (Exception e) {
            // Failure case
            long duration = System.currentTimeMillis() - startTime;
            builder.failure(e.getMessage())
                   .duration(duration)
                   .riskLevel(AuditEvent.RiskLevel.MEDIUM);
            
            auditService.audit(builder);
            throw e;
        }
    }
    
    /**
     * After returning advice for successful operations
     */
    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void auditAfterReturning(JoinPoint joinPoint, Auditable auditable, Object result) {
        if (auditable.auditOnlyFailures()) {
            return; // Skip successful operations if configured
        }
        
        // This is handled by the @Around advice above
    }
    
    /**
     * After throwing advice for failed operations
     */
    @AfterThrowing(pointcut = "@annotation(auditable)", throwing = "ex")
    public void auditAfterThrowing(JoinPoint joinPoint, Auditable auditable, Exception ex) {
        // This is handled by the @Around advice above
    }
    
    /**
     * Audit for all repository save operations
     */
    @AfterReturning(
        pointcut = "execution(* org.springframework.data.repository.CrudRepository.save(..))",
        returning = "result"
    )
    public void auditRepositorySave(JoinPoint joinPoint, Object result) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String entityType = extractEntityType(result);
        String entityId = extractResourceId(result);
        
        auditService.audit(AuditService.builder()
            .eventType(AuditEvent.AuditEventType.DATA_MODIFICATION)
            .action(AuditEvent.AuditAction.CREATE) // Could be CREATE or UPDATE
            .resourceType(entityType)
            .resourceId(entityId)
            .description("Entity saved via " + className)
            .riskLevel(AuditEvent.RiskLevel.LOW)
        );
    }
    
    /**
     * Audit for all repository delete operations
     */
    @AfterReturning(
        pointcut = "execution(* org.springframework.data.repository.CrudRepository.delete(..))"
    )
    public void auditRepositoryDelete(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length > 0) {
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String entityType = extractEntityType(args[0]);
            String entityId = extractResourceId(args[0]);
            
            auditService.audit(AuditService.builder()
                .eventType(AuditEvent.AuditEventType.DATA_MODIFICATION)
                .action(AuditEvent.AuditAction.DELETE)
                .resourceType(entityType)
                .resourceId(entityId)
                .description("Entity deleted via " + className)
                .riskLevel(AuditEvent.RiskLevel.MEDIUM)
            );
        }
    }
    
    /**
     * Extract resource ID from an object
     */
    private String extractResourceId(Object obj) {
        if (obj == null) {
            return null;
        }
        
        try {
            // Try to get ID field using reflection
            var idField = obj.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            Object id = idField.get(obj);
            return id != null ? id.toString() : null;
        } catch (Exception e) {
            // Try getId method
            try {
                var getIdMethod = obj.getClass().getMethod("getId");
                Object id = getIdMethod.invoke(obj);
                return id != null ? id.toString() : null;
            } catch (Exception ex) {
                return obj.toString(); // Fallback
            }
        }
    }
    
    /**
     * Extract entity type from an object
     */
    private String extractEntityType(Object obj) {
        if (obj == null) {
            return "unknown";
        }
        
        String className = obj.getClass().getSimpleName();
        // Remove common suffixes
        return className.replaceAll("(Entity|Dto|Model)$", "").toLowerCase();
    }
}