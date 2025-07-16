package com.zamaz.mcp.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event published when a new organization is created
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class OrganizationCreatedEvent extends DomainEvent {
    
    private String organizationId;
    private String organizationName;
    private String ownerId;
    private String ownerEmail;
    private String plan; // FREE, STARTER, PROFESSIONAL, ENTERPRISE
    private Instant createdAt;
    
    public OrganizationCreatedEvent(String organizationId, String organizationName, String ownerId) {
        super("ORGANIZATION_CREATED", organizationId, "ORGANIZATION");
        this.organizationId = organizationId;
        this.organizationName = organizationName;
        this.ownerId = ownerId;
        this.createdAt = Instant.now();
        this.setOrganizationId(organizationId);
        this.setUserId(ownerId);
        this.setSourceService("mcp-organization");
    }
}