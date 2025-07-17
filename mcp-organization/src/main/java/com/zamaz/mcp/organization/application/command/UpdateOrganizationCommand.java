package com.zamaz.mcp.organization.application.command;

import com.zamaz.mcp.common.application.command.Command;
import com.zamaz.mcp.organization.domain.model.OrganizationDescription;
import com.zamaz.mcp.organization.domain.model.OrganizationId;
import com.zamaz.mcp.organization.domain.model.OrganizationName;
import com.zamaz.mcp.organization.domain.model.UserId;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Command for updating organization details.
 * Contains the fields that can be updated.
 */
public record UpdateOrganizationCommand(
    OrganizationId organizationId,
    OrganizationName name,
    OrganizationDescription description,
    Map<String, Object> settings,
    UserId updatingUserId
) implements Command {
    
    public UpdateOrganizationCommand {
        Objects.requireNonNull(organizationId, "Organization ID is required");
        Objects.requireNonNull(updatingUserId, "Updating user ID is required");
        // Name, description, and settings are optional (null means no change)
    }
    
    /**
     * Builder for creating update commands with only the fields to update.
     */
    public static class Builder {
        private final OrganizationId organizationId;
        private final UserId updatingUserId;
        private OrganizationName name;
        private OrganizationDescription description;
        private Map<String, Object> settings;
        
        public Builder(String organizationId, String updatingUserId) {
            this.organizationId = OrganizationId.from(organizationId);
            this.updatingUserId = UserId.from(updatingUserId);
        }
        
        public Builder withName(String name) {
            this.name = name != null ? OrganizationName.from(name) : null;
            return this;
        }
        
        public Builder withDescription(String description) {
            this.description = description != null ? OrganizationDescription.from(description) : null;
            return this;
        }
        
        public Builder withSettings(Map<String, Object> settings) {
            this.settings = settings;
            return this;
        }
        
        public UpdateOrganizationCommand build() {
            return new UpdateOrganizationCommand(
                organizationId,
                name,
                description,
                settings,
                updatingUserId
            );
        }
    }
    
    public Optional<OrganizationName> getName() {
        return Optional.ofNullable(name);
    }
    
    public Optional<OrganizationDescription> getDescription() {
        return Optional.ofNullable(description);
    }
    
    public Optional<Map<String, Object>> getSettings() {
        return Optional.ofNullable(settings);
    }
}