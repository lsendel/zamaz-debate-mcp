package com.zamaz.mcp.authserver.repository;

import com.zamaz.mcp.authserver.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findByNameAndIsActiveTrue(String name);

    boolean existsByName(String name);
}