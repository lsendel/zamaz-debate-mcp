package com.zamaz.mcp.organization.domain.service;

import com.zamaz.mcp.common.domain.service.DomainService;
import com.zamaz.mcp.common.domain.exception.DomainRuleViolationException;
import com.zamaz.mcp.organization.domain.model.*;

import java.util.List;

/**
 * Domain service containing business logic that spans multiple aggregates
 * or requires external information for organization operations.
 */
public interface OrganizationDomainService extends DomainService {
    
    /**
     * Validates if an organization name is unique across the system.
     * This requires checking against the repository, hence it's a domain service.
     * 
     * @param name the organization name to validate
     * @return true if the name is available
     */
    boolean isOrganizationNameAvailable(OrganizationName name);
    
    /**
     * Validates if a user can be added to an organization.
     * This might check if the user exists, is not banned, etc.
     * 
     * @param userId the user to validate
     * @param organizationId the target organization
     * @throws DomainRuleViolationException if the user cannot be added
     */
    void validateUserCanJoinOrganization(UserId userId, OrganizationId organizationId);
    
    /**
     * Merges two organizations together.
     * Complex domain operation that affects multiple aggregates.
     * 
     * @param sourceOrg the organization to be merged
     * @param targetOrg the organization to merge into
     * @param mergingUserId the user performing the merge (must be owner of both)
     * @return the merged organization
     * @throws DomainRuleViolationException if merge is not allowed
     */
    Organization mergeOrganizations(Organization sourceOrg, Organization targetOrg, UserId mergingUserId);
    
    /**
     * Transfers ownership of an organization to another user.
     * Ensures business rules around ownership transfer are maintained.
     * 
     * @param organization the organization
     * @param currentOwnerId the current owner performing the transfer
     * @param newOwnerId the user to transfer ownership to
     * @throws DomainRuleViolationException if transfer is not allowed
     */
    void transferOwnership(Organization organization, UserId currentOwnerId, UserId newOwnerId);
    
    /**
     * Validates organization settings against business rules.
     * Some settings might have dependencies or limits based on subscription tier.
     * 
     * @param settings the settings to validate
     * @param organizationId the organization context
     * @throws DomainRuleViolationException if settings are invalid
     */
    void validateOrganizationSettings(OrganizationSettings settings, OrganizationId organizationId);
    
    /**
     * Calculates if an organization has reached its limits.
     * This might involve checking subscription tiers, usage quotas, etc.
     * 
     * @param organization the organization to check
     * @return true if any limits are reached
     */
    boolean hasReachedOrganizationLimits(Organization organization);
    
    /**
     * Gets suggested roles for a new user based on organization patterns.
     * This might analyze existing member distributions, organization type, etc.
     * 
     * @param organization the organization
     * @param userId the new user
     * @return suggested role for the user
     */
    Role suggestRoleForNewMember(Organization organization, UserId userId);
    
    /**
     * Validates if organizations can be merged.
     * Checks compatibility, ownership, and business rules.
     * 
     * @param sourceOrg the organization to be merged
     * @param targetOrg the organization to merge into
     * @param requestingUserId the user requesting the merge
     * @return list of validation errors, empty if valid
     */
    List<String> validateOrganizationMerge(Organization sourceOrg, Organization targetOrg, UserId requestingUserId);
}