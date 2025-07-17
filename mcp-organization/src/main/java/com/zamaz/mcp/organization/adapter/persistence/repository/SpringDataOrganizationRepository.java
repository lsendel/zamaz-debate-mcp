package com.zamaz.mcp.organization.adapter.persistence.repository;

import com.zamaz.mcp.organization.adapter.persistence.entity.OrganizationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for OrganizationEntity.
 * This is the low-level persistence interface used by the adapter.
 */
@Repository
public interface SpringDataOrganizationRepository extends JpaRepository<OrganizationEntity, UUID> {
    
    /**
     * Finds an organization by name.
     */
    Optional<OrganizationEntity> findByName(String name);
    
    /**
     * Checks if an organization with the given name exists.
     */
    boolean existsByName(String name);
    
    /**
     * Finds all organizations that a user is a member of.
     */
    @Query("SELECT DISTINCT o FROM OrganizationEntity o " +
           "JOIN o.organizationUsers ou " +
           "WHERE ou.user.id = :userId")
    List<OrganizationEntity> findByMemberUserId(@Param("userId") UUID userId);
    
    /**
     * Finds all active organizations.
     */
    List<OrganizationEntity> findByActiveTrue();
    
    /**
     * Finds organizations with members eagerly loaded.
     */
    @Query("SELECT o FROM OrganizationEntity o " +
           "LEFT JOIN FETCH o.organizationUsers ou " +
           "LEFT JOIN FETCH ou.user " +
           "WHERE o.id = :id")
    Optional<OrganizationEntity> findByIdWithMembers(@Param("id") UUID id);
}