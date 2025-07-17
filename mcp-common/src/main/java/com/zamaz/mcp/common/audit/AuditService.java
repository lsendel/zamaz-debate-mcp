package com.zamaz.mcp.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Comprehensive audit service for tracking all system activities
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuditService {
    
    private final AuditEventRepository auditEventRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    
    @Value("${audit.enabled:true}")
    private boolean auditEnabled;
    
    @Value("${audit.async:true}")
    private boolean asyncAudit;
    
    @Value("${audit.include-request-body:false}")
    private boolean includeRequestBody;
    
    @Value("${audit.include-response-body:false}")
    private boolean includeResponseBody;
    
    /**
     * Record an audit event
     */
    public void audit(AuditEventBuilder builder) {
        if (!auditEnabled) {
            return;
        }
        
        if (asyncAudit) {
            auditAsync(builder);
        } else {
            auditSync(builder);
        }
    }
    
    /**
     * Asynchronous audit recording
     */
    @Async("auditExecutor")
    public void auditAsync(AuditEventBuilder builder) {
        auditSync(builder);
    }
    
    /**
     * Synchronous audit recording
     */
    public void auditSync(AuditEventBuilder builder) {
        try {
            AuditEvent event = buildAuditEvent(builder);
            auditEventRepository.save(event);
            
            // Publish event for real-time monitoring
            eventPublisher.publishEvent(new AuditEventCreated(event));
            
            log.debug("Audit event recorded: {} - {} - {}", 
                event.getEventType(), event.getAction(), event.getResourceType());
                
        } catch (Exception e) {
            log.error("Failed to record audit event", e);
        }
    }
    
    /**
     * Build audit event from builder
     */
    private AuditEvent buildAuditEvent(AuditEventBuilder builder) {
        AuditEvent.AuditEventBuilder eventBuilder = AuditEvent.builder()
            .id(UUID.randomUUID().toString())
            .timestamp(LocalDateTime.now())
            .eventType(builder.getEventType())
            .action(builder.getAction())
            .resourceType(builder.getResourceType())
            .resourceId(builder.getResourceId())
            .resourceName(builder.getResourceName())
            .description(builder.getDescription())
            .result(builder.getResult())
            .errorMessage(builder.getErrorMessage())
            .duration(builder.getDuration())
            .recordsAffected(builder.getRecordsAffected())
            .riskLevel(builder.getRiskLevel())
            .riskScore(builder.getRiskScore())
            .complianceTags(builder.getComplianceTags());
        
        // Add security context
        addSecurityContext(eventBuilder);
        
        // Add request context
        addRequestContext(eventBuilder);
        
        // Add custom metadata
        Map<String, String> metadata = new HashMap<>();
        if (builder.getMetadata() != null) {
            metadata.putAll(builder.getMetadata());
        }
        
        // Add system metadata
        metadata.put("jvmMemory", getJvmMemoryInfo());
        metadata.put("threadName", Thread.currentThread().getName());
        
        eventBuilder.metadata(metadata);
        
        return eventBuilder.build();
    }
    
    /**
     * Add security context to audit event
     */
    private void addSecurityContext(AuditEvent.AuditEventBuilder builder) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated()) {
            builder.userId(auth.getName());
            builder.authenticationMethod(auth.getClass().getSimpleName());
            
            if (auth.getAuthorities() != null) {
                builder.permissions(auth.getAuthorities().toString());
            }
            
            // Extract organization ID from security context
            if (auth.getDetails() instanceof Map) {
                Map<?, ?> details = (Map<?, ?>) auth.getDetails();
                Object orgId = details.get("organizationId");
                if (orgId != null) {
                    builder.organizationId(orgId.toString());
                }
            }
        }
        
        // Fallback organization ID
        if (builder.build().getOrganizationId() == null) {
            builder.organizationId("system");
        }
    }
    
    /**
     * Add request context to audit event
     */
    private void addRequestContext(AuditEvent.AuditEventBuilder builder) {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            
            builder.sourceIp(getClientIpAddress(request))
                   .userAgent(request.getHeader("User-Agent"))
                   .sessionId(request.getSession().getId());
            
            // Extract request ID from header
            String requestId = request.getHeader("X-Request-ID");
            if (requestId != null) {
                builder.requestId(requestId);
            }
        }
    }
    
    /**
     * Get client IP address considering proxies
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Handle multiple IPs in X-Forwarded-For
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Get JVM memory information
     */
    private String getJvmMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return String.format("used=%dMB,total=%dMB,max=%dMB",
            usedMemory / 1024 / 1024,
            totalMemory / 1024 / 1024,
            maxMemory / 1024 / 1024);
    }
    
    /**
     * Search audit events
     */
    @Transactional(readOnly = true)
    public Page<AuditEvent> searchAuditEvents(AuditSearchCriteria criteria, Pageable pageable) {
        return auditEventRepository.findByCriteria(criteria, pageable);
    }
    
    /**
     * Get audit events for a specific resource
     */
    @Transactional(readOnly = true)
    public Page<AuditEvent> getResourceAuditTrail(String resourceType, String resourceId, Pageable pageable) {
        return auditEventRepository.findByResourceTypeAndResourceIdOrderByTimestampDesc(
            resourceType, resourceId, pageable);
    }
    
    /**
     * Get audit statistics
     */
    @Transactional(readOnly = true)
    public AuditStatistics getAuditStatistics(String organizationId, LocalDateTime from, LocalDateTime to) {
        return auditEventRepository.getStatistics(organizationId, from, to);
    }
    
    /**
     * Delete old audit events (for compliance with data retention policies)
     */
    @Transactional
    public int deleteOldAuditEvents(LocalDateTime before) {
        return auditEventRepository.deleteByTimestampBefore(before);
    }
    
    /**
     * Builder for creating audit events
     */
    public static AuditEventBuilder builder() {
        return new AuditEventBuilder();
    }
    
    /**
     * Convenience methods for common audit scenarios
     */
    
    public void auditAuthentication(String userId, String organizationId, boolean success, String errorMessage) {
        audit(builder()
            .eventType(AuditEvent.AuditEventType.AUTHENTICATION)
            .action(success ? AuditEvent.AuditAction.LOGIN : AuditEvent.AuditAction.LOGIN_FAILED)
            .organizationId(organizationId)
            .userId(userId)
            .resourceType("user")
            .resourceId(userId)
            .result(success ? AuditEvent.AuditResult.SUCCESS : AuditEvent.AuditResult.FAILURE)
            .errorMessage(errorMessage)
            .riskLevel(success ? AuditEvent.RiskLevel.LOW : AuditEvent.RiskLevel.MEDIUM)
            .description(success ? "User logged in successfully" : "User login failed")
        );
    }
    
    public void auditDataAccess(String resourceType, String resourceId, String action, String organizationId) {
        audit(builder()
            .eventType(AuditEvent.AuditEventType.DATA_ACCESS)
            .action(AuditEvent.AuditAction.READ)
            .organizationId(organizationId)
            .resourceType(resourceType)
            .resourceId(resourceId)
            .description("Data access: " + action)
            .riskLevel(AuditEvent.RiskLevel.LOW)
        );
    }
    
    public void auditDataModification(String resourceType, String resourceId, AuditEvent.AuditAction action, 
                                    String organizationId, Object oldValue, Object newValue) {
        Map<String, String> metadata = new HashMap<>();
        
        if (oldValue != null) {
            try {
                metadata.put("oldValue", objectMapper.writeValueAsString(oldValue));
            } catch (Exception e) {
                metadata.put("oldValue", oldValue.toString());
            }
        }
        
        if (newValue != null) {
            try {
                metadata.put("newValue", objectMapper.writeValueAsString(newValue));
            } catch (Exception e) {
                metadata.put("newValue", newValue.toString());
            }
        }
        
        audit(builder()
            .eventType(AuditEvent.AuditEventType.DATA_MODIFICATION)
            .action(action)
            .organizationId(organizationId)
            .resourceType(resourceType)
            .resourceId(resourceId)
            .metadata(metadata)
            .description("Data modification: " + action)
            .riskLevel(AuditEvent.RiskLevel.MEDIUM)
        );
    }
    
    public void auditSecurityEvent(String description, AuditEvent.RiskLevel riskLevel, String organizationId) {
        audit(builder()
            .eventType(AuditEvent.AuditEventType.SECURITY_EVENT)
            .action(AuditEvent.AuditAction.SUSPICIOUS_ACTIVITY)
            .organizationId(organizationId)
            .resourceType("security")
            .description(description)
            .riskLevel(riskLevel)
            .result(AuditEvent.AuditResult.FAILURE)
        );
    }
}