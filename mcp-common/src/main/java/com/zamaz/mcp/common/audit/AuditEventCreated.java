package com.zamaz.mcp.common.audit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.ApplicationEvent;

/**
 * Event published when an audit event is created
 */
public class AuditEventCreated extends ApplicationEvent {
    
    private final AuditEvent auditEvent;
    
    public AuditEventCreated(AuditEvent auditEvent) {
        super(auditEvent);
        this.auditEvent = auditEvent;
    }
    
    public AuditEvent getAuditEvent() {
        return auditEvent;
    }
}