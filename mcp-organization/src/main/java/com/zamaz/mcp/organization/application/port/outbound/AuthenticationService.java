package com.zamaz.mcp.organization.application.port.outbound;

import com.zamaz.mcp.organization.domain.model.Organization;
import com.zamaz.mcp.organization.domain.model.User;
import com.zamaz.mcp.organization.domain.model.Role;

/**
 * Service interface for authentication and authorization operations.
 * This is an outbound port for security operations.
 */
public interface AuthenticationService {
    
    /**
     * Validates if a user has permission to perform an action on an organization.
     * 
     * @param userId the user ID
     * @param organizationId the organization ID
     * @param requiredRole the minimum required role
     * @return true if the user has permission
     */
    boolean hasPermission(String userId, String organizationId, Role requiredRole);
    
    /**
     * Gets the current authenticated user ID from the security context.
     * 
     * @return the authenticated user ID, or null if not authenticated
     */
    String getCurrentUserId();
    
    /**
     * Validates a JWT token and extracts the user ID.
     * 
     * @param token the JWT token
     * @return the user ID if valid, null otherwise
     */
    String validateToken(String token);
    
    /**
     * Generates a JWT token for a user.
     * 
     * @param user the user
     * @param organization the organization context (optional)
     * @return the JWT token
     */
    String generateToken(User user, Organization organization);
    
    /**
     * Generates an email verification token.
     * 
     * @param user the user to verify
     * @return the verification token
     */
    String generateEmailVerificationToken(User user);
    
    /**
     * Validates an email verification token.
     * 
     * @param token the verification token
     * @return the user ID if valid, null otherwise
     */
    String validateEmailVerificationToken(String token);
}