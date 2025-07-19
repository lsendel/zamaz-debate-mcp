package com.zamaz.mcp.authserver.repository;

import com.zamaz.mcp.authserver.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndIsActiveTrue(String email);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u JOIN FETCH u.organizationUsers ou JOIN FETCH ou.organization WHERE u.email = :email AND u.isActive = true")
    Optional<User> findByEmailWithOrganizations(@Param("email") String email);
}