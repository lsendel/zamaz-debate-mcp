package com.zamaz.mcp.organization.domain.event;

import com.zamaz.mcp.common.domain.event.AbstractDomainEvent;
import java.util.UUID;

/**
 * Domain event raised when a user's preferences are updated.
 */
public class UserPreferencesUpdatedEvent extends AbstractDomainEvent {
    
    private final String defaultScopeType;
    private final String defaultSharingLevel;
    private final UUID defaultOrganizationId;
    private final UUID defaultApplicationId; // Can be null
    private final UUID defaultTeamId; // Can be null
    
    public UserPreferencesUpdatedEvent(UUID userId, String defaultScopeType,
                                      String defaultSharingLevel, UUID defaultOrganizationId,
                                      UUID defaultApplicationId, UUID defaultTeamId) {
        super(userId.toString());
        this.defaultScopeType = defaultScopeType;
        this.defaultSharingLevel = defaultSharingLevel;
        this.defaultOrganizationId = defaultOrganizationId;
        this.defaultApplicationId = defaultApplicationId;
        this.defaultTeamId = defaultTeamId;
    }
    
    public UUID getUserId() {
        return UUID.fromString(getAggregateId());
    }
    
    public String getDefaultScopeType() {
        return defaultScopeType;
    }
    
    public String getDefaultSharingLevel() {
        return defaultSharingLevel;
    }
    
    public UUID getDefaultOrganizationId() {
        return defaultOrganizationId;
    }
    
    public UUID getDefaultApplicationId() {
        return defaultApplicationId;
    }
    
    public UUID getDefaultTeamId() {
        return defaultTeamId;
    }
    
    public boolean hasDefaultApplication() {
        return defaultApplicationId != null;
    }
    
    public boolean hasDefaultTeam() {
        return defaultTeamId != null;
    }
    
    @Override
    public String getEventType() {
        return "user.preferences.updated";
    }
}