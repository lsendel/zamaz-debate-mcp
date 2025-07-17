package com.zamaz.mcp.organization.application.exception;

import com.zamaz.mcp.common.application.exception.UseCaseException;

/**
 * Exception thrown when a user attempts unauthorized access to organization resources.
 */
public class UnauthorizedOrganizationAccessException extends UseCaseException {
    
    public UnauthorizedOrganizationAccessException(String action, String organizationId, String userId) {
        super(
            "organization.access.unauthorized",
            String.format("User %s is not authorized to %s organization %s", userId, action, organizationId)
        );
    }
    
    public UnauthorizedOrganizationAccessException(String message) {
        super("organization.access.unauthorized", message);
    }
}