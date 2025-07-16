package com.zamaz.mcp.common.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event published when a new user registers
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UserRegisteredEvent extends DomainEvent {
    
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private String registrationSource; // WEB, API, INVITATION, SOCIAL
    private Instant registeredAt;
    
    public UserRegisteredEvent(String userId, String email, String organizationId) {
        super("USER_REGISTERED", userId, "USER");
        this.userId = userId;
        this.email = email;
        this.registeredAt = Instant.now();
        this.setOrganizationId(organizationId);
        this.setUserId(userId);
        this.setSourceService("mcp-organization");
    }
}