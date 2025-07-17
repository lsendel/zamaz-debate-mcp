package com.zamaz.mcp.organization.adapter.domain.service;

import com.zamaz.mcp.common.domain.exception.DomainRuleViolationException;
import com.zamaz.mcp.organization.application.port.outbound.OrganizationRepository;
import com.zamaz.mcp.organization.application.port.outbound.UserRepository;
import com.zamaz.mcp.organization.domain.model.*;
import com.zamaz.mcp.organization.domain.service.OrganizationDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of OrganizationDomainService.
 * This contains business logic that requires external information or spans multiple aggregates.
 */
@Service
@RequiredArgsConstructor
public class OrganizationDomainServiceImpl implements OrganizationDomainService {
    
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    
    @Override
    public boolean isOrganizationNameAvailable(OrganizationName name) {
        return !organizationRepository.existsByName(name);
    }
    
    @Override
    public void validateUserCanJoinOrganization(UserId userId, OrganizationId organizationId) {
        // Check if user exists and is active
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new DomainRuleViolationException(
                "user.notFound",
                "User not found: " + userId
            ));
        
        if (!user.canJoinOrganizations()) {
            throw new DomainRuleViolationException(
                "user.cannotJoin",
                "User is not eligible to join organizations"
            );
        }
        
        // Check if user is already in too many organizations (business rule)
        var userOrganizations = organizationRepository.findByMemberUserId(userId);
        int maxOrganizationsPerUser = 10; // This could be configurable
        
        if (userOrganizations.size() >= maxOrganizationsPerUser) {
            throw new DomainRuleViolationException(
                "user.organizationLimit",
                "User has reached maximum number of organizations: " + maxOrganizationsPerUser
            );
        }
    }
    
    @Override
    public Organization mergeOrganizations(Organization sourceOrg, Organization targetOrg, UserId mergingUserId) {
        // Validate merge is allowed
        var validationErrors = validateOrganizationMerge(sourceOrg, targetOrg, mergingUserId);
        if (!validationErrors.isEmpty()) {
            throw new DomainRuleViolationException(
                "organization.merge.invalid",
                "Cannot merge organizations: " + String.join(", ", validationErrors)
            );
        }
        
        // Transfer all members from source to target
        sourceOrg.getMembers().forEach(member -> {
            if (!targetOrg.isMember(member.getUserId())) {
                // Use the higher role if user exists in both
                Role roleToUse = member.getRole();
                targetOrg.getUserRole(member.getUserId()).ifPresent(existingRole -> {
                    if (existingRole.getLevel() > roleToUse.getLevel()) {
                        roleToUse = existingRole;
                    }
                });
                
                targetOrg.addUser(member.getUserId(), roleToUse);
            }
        });
        
        // Deactivate source organization
        sourceOrg.deactivate();
        
        return targetOrg;
    }
    
    @Override
    public void transferOwnership(Organization organization, UserId currentOwnerId, UserId newOwnerId) {
        // Validate current user is owner
        if (!organization.hasRole(currentOwnerId, Role.OWNER)) {
            throw new DomainRuleViolationException(
                "organization.transfer.notOwner",
                "Only owners can transfer ownership"
            );
        }
        
        // Validate new owner exists and can own organizations
        var newOwner = userRepository.findById(newOwnerId)
            .orElseThrow(() -> new DomainRuleViolationException(
                "user.notFound",
                "New owner not found: " + newOwnerId
            ));
        
        if (!newOwner.isActive()) {
            throw new DomainRuleViolationException(
                "user.inactive",
                "Cannot transfer ownership to inactive user"
            );
        }
        
        // Add new owner if not already member
        if (!organization.isMember(newOwnerId)) {
            organization.addUser(newOwnerId, Role.OWNER);
        } else {
            organization.updateUserRole(newOwnerId, Role.OWNER);
        }
        
        // Optionally demote current owner to admin
        organization.updateUserRole(currentOwnerId, Role.ADMIN);
    }
    
    @Override
    public void validateOrganizationSettings(OrganizationSettings settings, OrganizationId organizationId) {
        // Validate max members setting
        Integer maxMembers = settings.getMaxMembers();
        if (maxMembers != null) {
            if (maxMembers < 1) {
                throw new DomainRuleViolationException(
                    "settings.maxMembers.tooLow",
                    "Maximum members must be at least 1"
                );
            }
            if (maxMembers > 1000) {
                throw new DomainRuleViolationException(
                    "settings.maxMembers.tooHigh",
                    "Maximum members cannot exceed 1000"
                );
            }
            
            // Check if current member count exceeds new limit
            var organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new DomainRuleViolationException(
                    "organization.notFound",
                    "Organization not found"
                ));
            
            if (organization.getMemberCount() > maxMembers) {
                throw new DomainRuleViolationException(
                    "settings.maxMembers.exceedsCurrent",
                    "Cannot set max members below current member count: " + organization.getMemberCount()
                );
            }
        }
        
        // Validate default role
        String defaultRole = settings.getDefaultUserRole();
        if (defaultRole != null) {
            try {
                Role.fromString(defaultRole);
            } catch (IllegalArgumentException e) {
                throw new DomainRuleViolationException(
                    "settings.defaultRole.invalid",
                    "Invalid default role: " + defaultRole
                );
            }
        }
    }
    
    @Override
    public boolean hasReachedOrganizationLimits(Organization organization) {
        // Check member limit
        Integer maxMembers = organization.getSettings().getMaxMembers();
        if (maxMembers != null && organization.getMemberCount() >= maxMembers) {
            return true;
        }
        
        // Additional limits could be checked here:
        // - Storage limits
        // - API call limits
        // - Feature limits based on subscription tier
        
        return false;
    }
    
    @Override
    public Role suggestRoleForNewMember(Organization organization, UserId userId) {
        // Default to organization's default role setting
        String defaultRoleStr = organization.getSettings().getDefaultUserRole();
        Role defaultRole = Role.fromString(defaultRoleStr);
        
        // Could implement more sophisticated logic:
        // - Check user's roles in other organizations
        // - Check invitation type
        // - Check organization's member distribution
        
        return defaultRole;
    }
    
    @Override
    public List<String> validateOrganizationMerge(Organization sourceOrg, Organization targetOrg, UserId requestingUserId) {
        List<String> errors = new ArrayList<>();
        
        // Check if requesting user is owner of both
        if (!sourceOrg.hasRole(requestingUserId, Role.OWNER)) {
            errors.add("User must be owner of source organization");
        }
        if (!targetOrg.hasRole(requestingUserId, Role.OWNER)) {
            errors.add("User must be owner of target organization");
        }
        
        // Check if organizations are active
        if (!sourceOrg.isActive()) {
            errors.add("Source organization is not active");
        }
        if (!targetOrg.isActive()) {
            errors.add("Target organization is not active");
        }
        
        // Check if merge would exceed member limits
        Integer targetMaxMembers = targetOrg.getSettings().getMaxMembers();
        if (targetMaxMembers != null) {
            int combinedMembers = targetOrg.getMemberCount() + sourceOrg.getMemberCount();
            if (combinedMembers > targetMaxMembers) {
                errors.add("Merge would exceed target organization's member limit");
            }
        }
        
        return errors;
    }
}