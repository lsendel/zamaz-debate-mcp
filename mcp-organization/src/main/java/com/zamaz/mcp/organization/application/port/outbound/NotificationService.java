package com.zamaz.mcp.organization.application.port.outbound;

import com.zamaz.mcp.organization.domain.model.Organization;
import com.zamaz.mcp.organization.domain.model.User;
import com.zamaz.mcp.organization.domain.model.Role;

/**
 * Service interface for sending notifications.
 * This is an outbound port for notification operations.
 */
public interface NotificationService {
    
    /**
     * Sends a welcome notification to a new organization owner.
     * 
     * @param organization the created organization
     * @param owner the organization owner
     */
    void sendOrganizationCreatedNotification(Organization organization, User owner);
    
    /**
     * Sends a notification when a user is added to an organization.
     * 
     * @param organization the organization
     * @param user the added user
     * @param role the assigned role
     */
    void sendUserAddedToOrganizationNotification(Organization organization, User user, Role role);
    
    /**
     * Sends a notification when a user is removed from an organization.
     * 
     * @param organization the organization
     * @param user the removed user
     */
    void sendUserRemovedFromOrganizationNotification(Organization organization, User user);
    
    /**
     * Sends a notification when a user's role is changed.
     * 
     * @param organization the organization
     * @param user the user
     * @param oldRole the previous role
     * @param newRole the new role
     */
    void sendRoleChangedNotification(Organization organization, User user, Role oldRole, Role newRole);
    
    /**
     * Sends email verification notification.
     * 
     * @param user the user to verify
     * @param verificationToken the verification token
     */
    void sendEmailVerificationNotification(User user, String verificationToken);
}