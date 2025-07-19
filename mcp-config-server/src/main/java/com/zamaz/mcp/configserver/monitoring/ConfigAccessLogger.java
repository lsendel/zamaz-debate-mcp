package com.zamaz.mcp.configserver.monitoring;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

/**
 * Aspect for logging configuration access and operations.
 * Provides detailed audit trail of configuration requests.
 */
@Aspect
@Component
public class ConfigAccessLogger {

    private static final Logger logger = LoggerFactory.getLogger(ConfigAccessLogger.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("CONFIG_AUDIT");

    @Autowired
    private ConfigServerMetrics metrics;

    /**
     * Logs configuration environment requests.
     */
    @Around("@annotation(org.springframework.web.bind.annotation.RequestMapping) && " +
            "within(org.springframework.cloud.config.server.environment.*)")
    public Object logConfigAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        
        var sample = metrics.startConfigRequest();
        
        try {
            // Extract request details
            String methodName = joinPoint.getSignature().getName();
            Object[] args = joinPoint.getArgs();
            
            String application = extractApplication(args);
            String profile = extractProfile(args);
            String label = extractLabel(args);
            
            // Log request start
            auditLogger.info("Config access request - Application: {}, Profile: {}, Label: {}, Method: {}, RequestId: {}",
                application, profile, label, methodName, requestId);
            
            // Execute the method
            long startTime = System.currentTimeMillis();
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            // Record metrics
            metrics.recordConfigRequestSuccess(sample);
            metrics.recordConfigAccess(application, profile, label);
            
            // Log successful access
            auditLogger.info("Config access success - Application: {}, Profile: {}, Label: {}, Duration: {}ms, RequestId: {}",
                application, profile, label, duration, requestId);
            
            return result;
            
        } catch (Exception e) {
            // Record failure metrics
            metrics.recordConfigRequestFailure(sample, e.getClass().getSimpleName());
            
            // Log failure
            auditLogger.error("Config access failed - Error: {}, RequestId: {}", 
                e.getMessage(), requestId);
            
            throw e;
            
        } finally {
            MDC.remove("requestId");
        }
    }

    /**
     * Logs encryption operations.
     */
    @Around("execution(* com.zamaz.mcp.configserver.controller.EncryptionController.encrypt*(..))")
    public Object logEncryption(ProceedingJoinPoint joinPoint) throws Throwable {
        var sample = metrics.startEncryption();
        
        try {
            Object result = joinPoint.proceed();
            metrics.recordEncryptionComplete(sample);
            
            auditLogger.info("Encryption operation completed - Method: {}", 
                joinPoint.getSignature().getName());
            
            return result;
        } catch (Exception e) {
            auditLogger.error("Encryption operation failed - Method: {}, Error: {}",
                joinPoint.getSignature().getName(), e.getMessage());
            throw e;
        }
    }

    /**
     * Logs decryption operations.
     */
    @Around("execution(* com.zamaz.mcp.configserver.controller.EncryptionController.decrypt*(..))")
    public Object logDecryption(ProceedingJoinPoint joinPoint) throws Throwable {
        metrics.recordDecryption();
        
        try {
            Object result = joinPoint.proceed();
            
            auditLogger.info("Decryption operation completed - Method: {}", 
                joinPoint.getSignature().getName());
            
            return result;
        } catch (Exception e) {
            auditLogger.error("Decryption operation failed - Method: {}, Error: {}",
                joinPoint.getSignature().getName(), e.getMessage());
            throw e;
        }
    }

    /**
     * Logs refresh events.
     */
    @Around("execution(* *..*Controller.refresh*(..))")
    public Object logRefresh(ProceedingJoinPoint joinPoint) throws Throwable {
        String source = joinPoint.getSignature().getDeclaringTypeName();
        
        try {
            Object result = joinPoint.proceed();
            
            metrics.recordRefreshEvent(source);
            auditLogger.info("Configuration refresh triggered - Source: {}, Timestamp: {}",
                source, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return result;
        } catch (Exception e) {
            auditLogger.error("Configuration refresh failed - Source: {}, Error: {}",
                source, e.getMessage());
            throw e;
        }
    }

    /**
     * Logs webhook operations.
     */
    @Around("execution(* com.zamaz.mcp.configserver.controller.WebhookController.*(..))")
    public Object logWebhook(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().getName();
        
        try {
            Object result = joinPoint.proceed();
            
            auditLogger.info("Webhook processed - Method: {}, Success: true", method);
            
            return result;
        } catch (Exception e) {
            auditLogger.error("Webhook processing failed - Method: {}, Error: {}",
                method, e.getMessage());
            throw e;
        }
    }

    /**
     * Extracts application name from method arguments.
     */
    private String extractApplication(Object[] args) {
        if (args.length > 0 && args[0] instanceof String) {
            return (String) args[0];
        }
        return "unknown";
    }

    /**
     * Extracts profile from method arguments.
     */
    private String extractProfile(Object[] args) {
        if (args.length > 1 && args[1] instanceof String) {
            return (String) args[1];
        }
        return "default";
    }

    /**
     * Extracts label from method arguments.
     */
    private String extractLabel(Object[] args) {
        if (args.length > 2 && args[2] instanceof String) {
            return (String) args[2];
        }
        return "main";
    }

    /**
     * Configuration access audit entry.
     */
    public static class ConfigAccessAudit {
        private final String requestId;
        private final String application;
        private final String profile;
        private final String label;
        private final String operation;
        private final LocalDateTime timestamp;
        private final long duration;
        private final boolean success;
        private final String errorMessage;

        public ConfigAccessAudit(String requestId, String application, String profile,
                                String label, String operation, LocalDateTime timestamp,
                                long duration, boolean success, String errorMessage) {
            this.requestId = requestId;
            this.application = application;
            this.profile = profile;
            this.label = label;
            this.operation = operation;
            this.timestamp = timestamp;
            this.duration = duration;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        // Getters
        public String getRequestId() {
            return requestId;
        }

        public String getApplication() {
            return application;
        }

        public String getProfile() {
            return profile;
        }

        public String getLabel() {
            return label;
        }

        public String getOperation() {
            return operation;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public long getDuration() {
            return duration;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}