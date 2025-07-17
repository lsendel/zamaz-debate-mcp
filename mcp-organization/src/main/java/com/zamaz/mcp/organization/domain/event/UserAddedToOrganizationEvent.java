package com.zamaz.mcp.organization.domain.event;

import com.zamaz.mcp.common.domain.event.AbstractDomainEvent;
import java.util.UUID;

/**
 * Domain event raised when a user is added to an organization.
 */
public class UserAddedToOrganizationEvent extends AbstractDomainEvent {
    
    private final UUID userId;
    private final String role;
    
    public UserAddedToOrganizationEvent(UUID organizationId, UUID userId, String role) {
        super(organizationId.toString());
        this.userId = userId;
        this.role = role;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public String getRole() {
        return role;
    }
    
    @Override
    public String getEventType() {
        return "organization.user.added";
    }
}