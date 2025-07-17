package com.zamaz.mcp.github.repository;

import com.zamaz.mcp.github.entity.GitHubInstallation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for GitHub installation entities
 */
@Repository
public interface GitHubInstallationRepository extends JpaRepository<GitHubInstallation, Long> {

    /**
     * Find installation by account login
     */
    Optional<GitHubInstallation> findByAccountLogin(String accountLogin);

    /**
     * Find installations by account type
     */
    List<GitHubInstallation> findByAccountType(String accountType);

    /**
     * Find installations by status
     */
    List<GitHubInstallation> findByStatus(String status);

    /**
     * Find active installations
     */
    @Query("SELECT i FROM GitHubInstallation i WHERE i.status = 'ACTIVE'")
    List<GitHubInstallation> findActiveInstallations();

    /**
     * Find installations by account login and status
     */
    Optional<GitHubInstallation> findByAccountLoginAndStatus(String accountLogin, String status);

    /**
     * Count installations by status
     */
    @Query("SELECT COUNT(i) FROM GitHubInstallation i WHERE i.status = :status")
    Long countByStatus(@Param("status") String status);
}