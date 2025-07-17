package com.zamaz.mcp.common.audit;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Audit event entity for tracking all system activities
 */
@Entity
@Table(name = "audit_events", indexes = {
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_organization", columnList = "organizationId"),
    @Index(name = "idx_audit_user", columnList = "userId"),
    @Index(name = "idx_audit_event_type", columnList = "eventType"),
    @Index(name = "idx_audit_resource", columnList = "resourceType, resourceId"),
    @Index(name = "idx_audit_session", columnList = "sessionId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditEventType eventType;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditAction action;
    
    @Column(nullable = false)
    private String organizationId;
    
    private String userId;
    
    private String sessionId;
    
    private String requestId;
    
    @Column(nullable = false)
    private String resourceType;
    
    private String resourceId;
    
    private String resourceName;
    
    @Column(length = 1000)
    private String description;
    
    private String sourceIp;
    
    private String userAgent;
    
    @Enumerated(EnumType.STRING)
    private AuditResult result;
    
    private String errorMessage;
    
    @ElementCollection
    @CollectionTable(name = "audit_event_metadata", 
                    joinColumns = @JoinColumn(name = "audit_event_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value", length = 2000)
    private Map<String, String> metadata;
    
    // Performance metrics
    private Long duration; // in milliseconds
    
    private Integer recordsAffected;
    
    // Security context
    private String authenticationMethod;
    
    private String permissions;
    
    // Geographic information
    private String country;
    
    private String region;
    
    private String city;
    
    // Risk assessment
    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;
    
    private Double riskScore;
    
    // Compliance tags
    @ElementCollection
    @CollectionTable(name = "audit_compliance_tags",
                    joinColumns = @JoinColumn(name = "audit_event_id"))
    @Column(name = "tag")
    private java.util.Set<String> complianceTags;
    
    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (result == null) {
            result = AuditResult.SUCCESS;
        }
        if (riskLevel == null) {
            riskLevel = RiskLevel.LOW;
        }
    }
    
    /**
     * Audit event types
     */
    public enum AuditEventType {
        AUTHENTICATION,
        AUTHORIZATION,
        DATA_ACCESS,
        DATA_MODIFICATION,
        SYSTEM_EVENT,
        SECURITY_EVENT,
        COMPLIANCE_EVENT,
        PERFORMANCE_EVENT,
        ERROR_EVENT,
        BUSINESS_EVENT
    }
    
    /**
     * Audit actions
     */
    public enum AuditAction {
        // Authentication
        LOGIN,
        LOGOUT,
        LOGIN_FAILED,
        PASSWORD_CHANGE,
        PASSWORD_RESET,
        
        // Authorization
        ACCESS_GRANTED,
        ACCESS_DENIED,
        PERMISSION_CHANGE,
        ROLE_CHANGE,
        
        // Data operations
        CREATE,
        READ,
        UPDATE,
        DELETE,
        EXPORT,
        IMPORT,
        
        // System events
        STARTUP,
        SHUTDOWN,
        CONFIGURATION_CHANGE,
        
        // Security events
        SUSPICIOUS_ACTIVITY,
        SECURITY_BREACH,
        RATE_LIMIT_EXCEEDED,
        
        // Business events
        DEBATE_CREATED,
        DEBATE_STARTED,
        DEBATE_COMPLETED,
        PARTICIPANT_ADDED,
        RESPONSE_SUBMITTED
    }
    
    /**
     * Audit result status
     */
    public enum AuditResult {
        SUCCESS,
        FAILURE,
        PARTIAL_SUCCESS,
        CANCELLED,
        TIMEOUT
    }
    
    /**
     * Risk assessment levels
     */
    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}