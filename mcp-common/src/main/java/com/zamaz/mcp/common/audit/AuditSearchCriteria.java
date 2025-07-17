package com.zamaz.mcp.common.audit;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * Search criteria for audit events
 */
@Data
@Accessors(chain = true)
public class AuditSearchCriteria {
    
    private String organizationId;
    private String userId;
    private AuditEvent.AuditEventType eventType;
    private AuditEvent.AuditAction action;
    private String resourceType;
    private String resourceId;
    private AuditEvent.AuditResult result;
    private AuditEvent.RiskLevel riskLevel;
    private String sourceIp;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private String searchTerm;
    private String sessionId;
    private String requestId;
    private String userAgent;
    
    /**
     * Builder pattern
     */
    public static AuditSearchCriteria builder() {
        return new AuditSearchCriteria();
    }
    
    /**
     * Convenience methods
     */
    public AuditSearchCriteria forOrganization(String organizationId) {
        this.organizationId = organizationId;
        return this;
    }
    
    public AuditSearchCriteria forUser(String userId) {
        this.userId = userId;
        return this;
    }
    
    public AuditSearchCriteria forResource(String resourceType, String resourceId) {
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        return this;
    }
    
    public AuditSearchCriteria inDateRange(LocalDateTime from, LocalDateTime to) {
        this.fromDate = from;
        this.toDate = to;
        return this;
    }
    
    public AuditSearchCriteria withSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
        return this;
    }
    
    public AuditSearchCriteria failuresOnly() {
        this.result = AuditEvent.AuditResult.FAILURE;
        return this;
    }
    
    public AuditSearchCriteria highRiskOnly() {
        this.riskLevel = AuditEvent.RiskLevel.HIGH;
        return this;
    }
    
    public AuditSearchCriteria fromIp(String sourceIp) {
        this.sourceIp = sourceIp;
        return this;
    }
}