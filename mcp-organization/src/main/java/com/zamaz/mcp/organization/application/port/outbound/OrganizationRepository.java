package com.zamaz.mcp.organization.application.port.outbound;

import com.zamaz.mcp.common.application.port.outbound.Repository;
import com.zamaz.mcp.organization.domain.model.Organization;
import com.zamaz.mcp.organization.domain.model.OrganizationId;
import com.zamaz.mcp.organization.domain.model.OrganizationName;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Organization aggregate.
 * This is an outbound port that defines persistence operations.
 */
public interface OrganizationRepository extends Repository<Organization, OrganizationId> {
    
    /**
     * Finds an organization by name.
     * 
     * @param name the organization name
     * @return the organization if found
     */
    Optional<Organization> findByName(OrganizationName name);
    
    /**
     * Checks if an organization with the given name exists.
     * 
     * @param name the organization name
     * @return true if exists
     */
    boolean existsByName(OrganizationName name);
    
    /**
     * Finds all organizations that a user is a member of.
     * 
     * @param userId the user ID
     * @return list of organizations
     */
    List<Organization> findByMemberUserId(com.zamaz.mcp.organization.domain.model.UserId userId);
    
    /**
     * Finds all active organizations.
     * 
     * @return list of active organizations
     */
    List<Organization> findAllActive();
    
    /**
     * Counts the number of organizations.
     * 
     * @return the total count
     */
    long count();
}