package com.zamaz.mcp.organization.adapter.persistence.repository;

import com.zamaz.mcp.organization.adapter.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for UserEntity.
 * This is the low-level persistence interface used by the adapter.
 */
@Repository
public interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID> {
    
    /**
     * Finds a user by email address.
     */
    Optional<UserEntity> findByEmail(String email);
    
    /**
     * Checks if a user with the given email exists.
     */
    boolean existsByEmail(String email);
    
    /**
     * Finds all active users.
     */
    List<UserEntity> findByStatus(String status);
    
    /**
     * Searches users by name pattern.
     */
    @Query("SELECT u FROM UserEntity u " +
           "WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :pattern, '%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :pattern, '%'))")
    List<UserEntity> searchByName(@Param("pattern") String pattern);
    
    /**
     * Finds users by IDs.
     */
    List<UserEntity> findByIdIn(List<UUID> ids);
}