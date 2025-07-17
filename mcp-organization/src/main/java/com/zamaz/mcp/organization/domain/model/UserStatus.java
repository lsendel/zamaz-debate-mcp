package com.zamaz.mcp.organization.domain.model;

/**
 * Enum representing the status of a user in the system.
 */
public enum UserStatus {
    /**
     * User is active and can perform all allowed operations.
     */
    ACTIVE,
    
    /**
     * User is temporarily suspended and cannot access the system.
     * Can be reactivated by an administrator.
     */
    SUSPENDED,
    
    /**
     * User is permanently banned from the system.
     * Cannot be reactivated.
     */
    BANNED,
    
    /**
     * User account is pending activation.
     * Typically waiting for email verification.
     */
    PENDING
}