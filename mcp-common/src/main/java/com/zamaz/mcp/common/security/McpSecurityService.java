package com.zamaz.mcp.common.security;

import com.zamaz.mcp.security.context.SecurityContext;
import com.zamaz.mcp.security.context.SecurityContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder as SpringSecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Security service for MCP tools to validate organization access and extract 
 * organization context from authenticated users.
 */
@Service
@Slf4j
public class McpSecurityService {
    
    /**
     * Validates that the authenticated user has access to the specified organization.
     * 
     * @param organizationId The organization ID to validate access to
     * @param authentication The authentication context
     * @throws McpSecurityException if user doesn't have access to the organization
     */
    public void validateOrganizationAccess(UUID organizationId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Attempted MCP tool access without authentication");
            throw new McpSecurityException("Authentication required for MCP tools");
        }
        
        SecurityContext securityContext = SecurityContextHolder.getCurrentContext();
        if (securityContext == null) {
            log.warn("No security context found for authenticated user: {}", authentication.getName());
            throw new McpSecurityException("Security context not available");
        }
        
        String userOrganizationId = securityContext.getOrganizationId();
        if (userOrganizationId == null) {
            log.warn("User {} has no organization context", authentication.getName());
            throw new McpSecurityException("User has no organization context");
        }
        
        if (!userOrganizationId.equals(organizationId.toString())) {
            log.warn("User {} attempted to access organization {} but belongs to {}", 
                authentication.getName(), organizationId, userOrganizationId);
            throw new McpSecurityException("Access denied to organization: " + organizationId);
        }
        
        log.debug("Validated organization access for user {} to organization {}", 
            authentication.getName(), organizationId);
    }
    
    /**
     * Extracts the organization ID from the authenticated user's security context.
     * This should be used instead of trusting client-provided organization IDs.
     * 
     * @param authentication The authentication context
     * @return The organization ID from the user's security context
     * @throws McpSecurityException if organization context is not available
     */
    public UUID getAuthenticatedOrganizationId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new McpSecurityException("Authentication required");
        }
        
        SecurityContext securityContext = SecurityContextHolder.getCurrentContext();
        if (securityContext == null || securityContext.getOrganizationId() == null) {
            log.warn("User {} has no organization context", authentication.getName());
            throw new McpSecurityException("User has no organization context");
        }
        
        try {
            return UUID.fromString(securityContext.getOrganizationId());
        } catch (IllegalArgumentException e) {
            log.error("Invalid organization ID format in security context: {}", 
                securityContext.getOrganizationId());
            throw new McpSecurityException("Invalid organization context");
        }
    }
    
    /**
     * Extracts the user ID from the authenticated user's security context.
     * 
     * @param authentication The authentication context
     * @return The user ID from the security context
     * @throws McpSecurityException if user context is not available
     */
    public UUID getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new McpSecurityException("Authentication required");
        }
        
        SecurityContext securityContext = SecurityContextHolder.getCurrentContext();
        if (securityContext == null || securityContext.getUserId() == null) {
            log.warn("User {} has no user ID in security context", authentication.getName());
            throw new McpSecurityException("User context not available");
        }
        
        try {
            return UUID.fromString(securityContext.getUserId());
        } catch (IllegalArgumentException e) {
            log.error("Invalid user ID format in security context: {}", 
                securityContext.getUserId());
            throw new McpSecurityException("Invalid user context");
        }
    }
    
    /**
     * Validates that a parameter exists and is not null.
     * 
     * @param paramValue The parameter value to validate
     * @param paramName The parameter name for error messages
     * @throws McpSecurityException if parameter is missing
     */
    public void validateRequiredParameter(Object paramValue, String paramName) {
        if (paramValue == null) {
            throw new McpSecurityException("Required parameter missing: " + paramName);
        }
    }
    
    /**
     * Validates and parses a UUID parameter.
     * 
     * @param paramValue The parameter value to parse
     * @param paramName The parameter name for error messages
     * @return The parsed UUID
     * @throws McpSecurityException if parameter is invalid
     */
    public UUID validateUuidParameter(Object paramValue, String paramName) {
        validateRequiredParameter(paramValue, paramName);
        
        try {
            return UUID.fromString(paramValue.toString());
        } catch (IllegalArgumentException e) {
            throw new McpSecurityException("Invalid UUID format for parameter: " + paramName);
        }
    }
}