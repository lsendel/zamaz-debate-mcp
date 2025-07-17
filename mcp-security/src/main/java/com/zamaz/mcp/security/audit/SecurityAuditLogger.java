package com.zamaz.mcp.security.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.security.model.McpUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Security Audit Logger
 * Provides comprehensive security event logging with structured format
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityAuditLogger {

    private final ObjectMapper objectMapper;
    
    // Security event types
    public enum SecurityEventType {
        AUTHENTICATION_SUCCESS,
        AUTHENTICATION_FAILURE,
        AUTHORIZATION_SUCCESS,
        AUTHORIZATION_FAILURE,
        PERMISSION_DENIED,
        ROLE_DENIED,
        SUSPICIOUS_ACTIVITY,
        SECURITY_VIOLATION,
        SESSION_CREATED,
        SESSION_EXPIRED,
        PASSWORD_CHANGED,
        ACCOUNT_LOCKED,
        ACCOUNT_UNLOCKED,
        PRIVILEGE_ESCALATION_ATTEMPT,
        DATA_ACCESS,
        SENSITIVE_OPERATION,
        SECURITY_CONFIGURATION_CHANGED
    }
    
    // Risk levels
    public enum RiskLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    /**
     * Log a security event with full context
     */
    public void logSecurityEvent(SecurityEventType eventType, RiskLevel riskLevel, 
                                String description, Map<String, Object> additionalData) {
        try {
            SecurityAuditEvent event = SecurityAuditEvent.builder()
                .timestamp(Instant.now())
                .eventType(eventType)
                .riskLevel(riskLevel)
                .description(description)
                .userId(getCurrentUserId())
                .organizationId(getCurrentOrganizationId())
                .sessionId(getCurrentSessionId())
                .userAgent(getCurrentUserAgent())
                .clientIp(getCurrentClientIp())
                .additionalData(additionalData != null ? additionalData : new HashMap<>())
                .build();
            
            String jsonEvent = objectMapper.writeValueAsString(event);
            
            // Log based on risk level
            switch (riskLevel) {
                case CRITICAL:
                    log.error("[SECURITY-CRITICAL] {}", jsonEvent);
                    break;
                case HIGH:
                    log.warn("[SECURITY-HIGH] {}", jsonEvent);
                    break;
                case MEDIUM:
                    log.warn("[SECURITY-MEDIUM] {}", jsonEvent);
                    break;
                case LOW:
                    log.info("[SECURITY-LOW] {}", jsonEvent);
                    break;
            }
            
        } catch (Exception e) {
            log.error("Failed to log security event: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Log authentication success
     */
    public void logAuthenticationSuccess(String userId, String method) {
        Map<String, Object> data = Map.of(
            "authMethod", method,
            "success", true
        );
        logSecurityEvent(SecurityEventType.AUTHENTICATION_SUCCESS, RiskLevel.LOW,
            "User authenticated successfully", data);
    }
    
    /**
     * Log authentication failure
     */
    public void logAuthenticationFailure(String userId, String method, String reason) {
        Map<String, Object> data = Map.of(
            "authMethod", method,
            "reason", reason,
            "success", false
        );
        logSecurityEvent(SecurityEventType.AUTHENTICATION_FAILURE, RiskLevel.MEDIUM,
            "Authentication failed: " + reason, data);
    }
    
    /**
     * Log authorization failure
     */
    public void logAuthorizationFailure(String resource, String permission, String reason) {
        Map<String, Object> data = Map.of(
            "resource", resource,
            "permission", permission,
            "reason", reason
        );
        logSecurityEvent(SecurityEventType.AUTHORIZATION_FAILURE, RiskLevel.MEDIUM,
            "Authorization failed for resource: " + resource, data);
    }
    
    /**
     * Log permission denied
     */
    public void logPermissionDenied(String permission, String resource) {
        Map<String, Object> data = Map.of(
            "permission", permission,
            "resource", resource
        );
        logSecurityEvent(SecurityEventType.PERMISSION_DENIED, RiskLevel.MEDIUM,
            "Permission denied: " + permission, data);
    }
    
    /**
     * Log suspicious activity
     */
    public void logSuspiciousActivity(String activity, String details) {
        Map<String, Object> data = Map.of(
            "activity", activity,
            "details", details
        );
        logSecurityEvent(SecurityEventType.SUSPICIOUS_ACTIVITY, RiskLevel.HIGH,
            "Suspicious activity detected: " + activity, data);
    }
    
    /**
     * Log security violation
     */
    public void logSecurityViolation(String violation, String source) {
        Map<String, Object> data = Map.of(
            "violation", violation,
            "source", source
        );
        logSecurityEvent(SecurityEventType.SECURITY_VIOLATION, RiskLevel.HIGH,
            "Security violation: " + violation, data);
    }
    
    /**
     * Log privilege escalation attempt
     */
    public void logPrivilegeEscalationAttempt(String attemptedAction, String currentRole) {
        Map<String, Object> data = Map.of(
            "attemptedAction", attemptedAction,
            "currentRole", currentRole
        );
        logSecurityEvent(SecurityEventType.PRIVILEGE_ESCALATION_ATTEMPT, RiskLevel.CRITICAL,
            "Privilege escalation attempt detected", data);
    }
    
    /**
     * Log sensitive operation
     */
    public void logSensitiveOperation(String operation, String resource) {
        Map<String, Object> data = Map.of(
            "operation", operation,
            "resource", resource
        );
        logSecurityEvent(SecurityEventType.SENSITIVE_OPERATION, RiskLevel.MEDIUM,
            "Sensitive operation performed: " + operation, data);
    }
    
    /**
     * Log data access
     */
    public void logDataAccess(String dataType, String operation, String resourceId) {
        Map<String, Object> data = Map.of(
            "dataType", dataType,
            "operation", operation,
            "resourceId", resourceId
        );
        logSecurityEvent(SecurityEventType.DATA_ACCESS, RiskLevel.LOW,
            "Data access: " + operation + " on " + dataType, data);
    }
    
    /**
     * Log security configuration change
     */
    public void logSecurityConfigurationChange(String configType, String change) {
        Map<String, Object> data = Map.of(
            "configType", configType,
            "change", change
        );
        logSecurityEvent(SecurityEventType.SECURITY_CONFIGURATION_CHANGED, RiskLevel.HIGH,
            "Security configuration changed: " + configType, data);
    }
    
    // Helper methods to extract context
    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof McpUser) {
            return ((McpUser) auth.getPrincipal()).getId();
        }
        return "anonymous";
    }
    
    private String getCurrentOrganizationId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof McpUser) {
            return ((McpUser) auth.getPrincipal()).getCurrentOrganizationId();
        }
        return null;
    }
    
    private String getCurrentSessionId() {
        // This would be extracted from the security context or request
        // Implementation depends on session management strategy
        return "session-" + System.currentTimeMillis();
    }
    
    private String getCurrentUserAgent() {
        // This would be extracted from the current HTTP request
        // Implementation depends on web context availability
        return "unknown";
    }
    
    private String getCurrentClientIp() {
        // This would be extracted from the current HTTP request
        // Implementation depends on web context availability
        return "unknown";
    }
}
