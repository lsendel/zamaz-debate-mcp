package com.zamaz.mcp.debate.domain.model;

import com.zamaz.mcp.common.domain.model.ValueObject;
import com.zamaz.mcp.common.domain.model.valueobject.ApplicationId;
import com.zamaz.mcp.common.domain.model.valueobject.ScopeType;
import com.zamaz.mcp.common.domain.organization.OrganizationId;

import java.util.Objects;
import java.util.Optional;

/**
 * Value object representing the scope of a debate.
 * Encapsulates the scope type and associated organization/application IDs.
 */
public final class DebateScope extends ValueObject {
    
    private final ScopeType scopeType;
    private final OrganizationId organizationId;
    private final ApplicationId applicationId; // Optional for APPLICATION/BOTH scopes
    
    /**
     * Creates a debate scope for organization-only.
     * 
     * @param organizationId the organization ID
     */
    public DebateScope(OrganizationId organizationId) {
        this.scopeType = ScopeType.ORGANIZATION;
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID is required");
        this.applicationId = null;
    }
    
    /**
     * Creates a debate scope for application-only.
     * 
     * @param organizationId the organization ID
     * @param applicationId the application ID
     */
    public DebateScope(OrganizationId organizationId, ApplicationId applicationId) {
        this.scopeType = ScopeType.APPLICATION;
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID is required");
        this.applicationId = Objects.requireNonNull(applicationId, "Application ID is required for APPLICATION scope");
    }
    
    /**
     * Creates a debate scope for both organization and application.
     * 
     * @param organizationId the organization ID
     * @param applicationId the application ID
     * @param both whether this is a BOTH scope (true) or just APPLICATION (false)
     */
    public DebateScope(OrganizationId organizationId, ApplicationId applicationId, boolean both) {
        this.scopeType = both ? ScopeType.BOTH : ScopeType.APPLICATION;
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID is required");
        this.applicationId = Objects.requireNonNull(applicationId, "Application ID is required");
    }
    
    /**
     * Creates a debate scope with explicit scope type.
     * 
     * @param scopeType the scope type
     * @param organizationId the organization ID
     * @param applicationId the application ID (required for APPLICATION/BOTH)
     */
    public DebateScope(ScopeType scopeType, OrganizationId organizationId, ApplicationId applicationId) {
        this.scopeType = Objects.requireNonNull(scopeType, "Scope type is required");
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID is required");
        
        if (scopeType.requiresApplicationAccess() && applicationId == null) {
            throw new IllegalArgumentException("Application ID is required for scope type " + scopeType);
        }
        
        if (!scopeType.requiresApplicationAccess() && applicationId != null) {
            throw new IllegalArgumentException("Application ID should not be provided for scope type " + scopeType);
        }
        
        this.applicationId = applicationId;
    }
    
    /**
     * Creates an organization-only scope.
     * 
     * @param organizationId the organization ID
     * @return new DebateScope instance
     */
    public static DebateScope organizationOnly(OrganizationId organizationId) {
        return new DebateScope(organizationId);
    }
    
    /**
     * Creates an application scope.
     * 
     * @param organizationId the organization ID
     * @param applicationId the application ID
     * @return new DebateScope instance
     */
    public static DebateScope applicationScope(OrganizationId organizationId, ApplicationId applicationId) {
        return new DebateScope(organizationId, applicationId);
    }
    
    /**
     * Creates a both (organization and application) scope.
     * 
     * @param organizationId the organization ID
     * @param applicationId the application ID
     * @return new DebateScope instance
     */
    public static DebateScope bothScope(OrganizationId organizationId, ApplicationId applicationId) {
        return new DebateScope(organizationId, applicationId, true);
    }
    
    /**
     * Checks if this scope includes organization access.
     * 
     * @return true if organization access is included
     */
    public boolean includesOrganization() {
        return scopeType.requiresOrganizationAccess();
    }
    
    /**
     * Checks if this scope includes application access.
     * 
     * @return true if application access is included
     */
    public boolean includesApplication() {
        return scopeType.requiresApplicationAccess();
    }
    
    /**
     * Checks if this scope matches the given organization.
     * 
     * @param orgId the organization ID to check
     * @return true if the scope includes this organization
     */
    public boolean matchesOrganization(OrganizationId orgId) {
        return includesOrganization() && organizationId.equals(orgId);
    }
    
    /**
     * Checks if this scope matches the given application.
     * 
     * @param appId the application ID to check
     * @return true if the scope includes this application
     */
    public boolean matchesApplication(ApplicationId appId) {
        return includesApplication() && applicationId != null && applicationId.equals(appId);
    }
    
    /**
     * Checks if this scope matches both the organization and application.
     * 
     * @param orgId the organization ID to check
     * @param appId the application ID to check
     * @return true if the scope matches both
     */
    public boolean matches(OrganizationId orgId, ApplicationId appId) {
        boolean orgMatches = matchesOrganization(orgId);
        boolean appMatches = !includesApplication() || matchesApplication(appId);
        return orgMatches && appMatches;
    }
    
    // Getters
    
    public ScopeType getScopeType() {
        return scopeType;
    }
    
    public OrganizationId getOrganizationId() {
        return organizationId;
    }
    
    public Optional<ApplicationId> getApplicationId() {
        return Optional.ofNullable(applicationId);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DebateScope that = (DebateScope) obj;
        return scopeType == that.scopeType &&
               Objects.equals(organizationId, that.organizationId) &&
               Objects.equals(applicationId, that.applicationId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(scopeType, organizationId, applicationId);
    }
    
    @Override
    public String toString() {
        return "DebateScope{" +
               "scopeType=" + scopeType +
               ", organizationId=" + organizationId +
               ", applicationId=" + applicationId +
               '}';
    }
}