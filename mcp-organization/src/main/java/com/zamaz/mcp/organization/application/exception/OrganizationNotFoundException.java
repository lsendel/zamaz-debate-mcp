package com.zamaz.mcp.organization.application.exception;

import com.zamaz.mcp.common.application.exception.ResourceNotFoundException;

/**
 * Exception thrown when an organization is not found.
 */
public class OrganizationNotFoundException extends ResourceNotFoundException {
    
    public OrganizationNotFoundException(String organizationId) {
        super("Organization not found: " + organizationId);
    }
    
    public OrganizationNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}