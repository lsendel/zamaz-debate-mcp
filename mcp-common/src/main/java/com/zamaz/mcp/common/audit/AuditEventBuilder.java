package com.zamaz.mcp.common.audit;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;
import java.util.Set;

/**
 * Builder for creating audit events
 */
@Data
@Accessors(chain = true, fluent = true)
public class AuditEventBuilder {
    
    private AuditEvent.AuditEventType eventType;
    private AuditEvent.AuditAction action;
    private String organizationId;
    private String userId;
    private String resourceType;
    private String resourceId;
    private String resourceName;
    private String description;
    private AuditEvent.AuditResult result;
    private String errorMessage;
    private Long duration;
    private Integer recordsAffected;
    private AuditEvent.RiskLevel riskLevel;
    private Double riskScore;
    private Map<String, String> metadata;
    private Set<String> complianceTags;
    
    /**
     * Add metadata key-value pair
     */
    public AuditEventBuilder addMetadata(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }
    
    /**
     * Add compliance tag
     */
    public AuditEventBuilder addComplianceTag(String tag) {
        if (this.complianceTags == null) {
            this.complianceTags = new java.util.HashSet<>();
        }
        this.complianceTags.add(tag);
        return this;
    }
    
    /**
     * Set success result
     */
    public AuditEventBuilder success() {
        return result(AuditEvent.AuditResult.SUCCESS).riskLevel(AuditEvent.RiskLevel.LOW);
    }
    
    /**
     * Set failure result
     */
    public AuditEventBuilder failure(String errorMessage) {
        return result(AuditEvent.AuditResult.FAILURE)
               .errorMessage(errorMessage)
               .riskLevel(AuditEvent.RiskLevel.MEDIUM);
    }
    
    /**
     * Set high risk
     */
    public AuditEventBuilder highRisk() {
        return riskLevel(AuditEvent.RiskLevel.HIGH);
    }
    
    /**
     * Set critical risk
     */
    public AuditEventBuilder criticalRisk() {
        return riskLevel(AuditEvent.RiskLevel.CRITICAL);
    }
    
    /**
     * Add GDPR compliance tag
     */
    public AuditEventBuilder gdprCompliance() {
        return addComplianceTag("GDPR");
    }
    
    /**
     * Add SOX compliance tag
     */
    public AuditEventBuilder soxCompliance() {
        return addComplianceTag("SOX");
    }
    
    /**
     * Add HIPAA compliance tag
     */
    public AuditEventBuilder hipaaCompliance() {
        return addComplianceTag("HIPAA");
    }
}