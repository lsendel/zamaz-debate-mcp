package com.zamaz.mcp.organization.repository;

import com.zamaz.mcp.organization.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByEmailAndIsActiveTrue(String email);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u JOIN u.organizationUsers ou WHERE ou.organization.id = :organizationId")
    List<User> findUsersByOrganizationId(@Param("organizationId") UUID organizationId);
    
    @Query("SELECT u FROM User u JOIN u.organizationUsers ou WHERE ou.organization.id = :organizationId AND ou.role = :role")
    List<User> findUsersByOrganizationIdAndRole(@Param("organizationId") UUID organizationId, @Param("role") String role);
}