package com.zamaz.mcp.organization.repository;

import com.zamaz.mcp.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    
    Optional<Organization> findByNameIgnoreCase(String name);
    
    List<Organization> findByIsActiveTrue();
    
    @Query("SELECT o FROM Organization o JOIN o.organizationUsers ou WHERE ou.user.id = :userId AND o.isActive = true")
    List<Organization> findActiveOrganizationsByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT o FROM Organization o JOIN o.organizationUsers ou WHERE ou.user.id = :userId")
    List<Organization> findAllOrganizationsByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT COUNT(ou) FROM OrganizationUser ou WHERE ou.organization.id = :organizationId")
    Long countUsersByOrganizationId(@Param("organizationId") UUID organizationId);
    
    boolean existsByNameIgnoreCase(String name);
}