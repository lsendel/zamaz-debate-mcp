package com.zamaz.mcp.github.repository;

import com.zamaz.mcp.github.entity.RepositoryConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for repository configuration entities
 */
@Repository
public interface RepositoryConfigRepository extends JpaRepository<RepositoryConfig, Long> {

    /**
     * Find configuration by repository full name
     */
    Optional<RepositoryConfig> findByRepositoryFullName(String repositoryFullName);

    /**
     * Find configuration by installation ID
     */
    List<RepositoryConfig> findByInstallationId(Long installationId);

    /**
     * Find configurations with auto-review enabled
     */
    @Query("SELECT c FROM RepositoryConfig c WHERE c.autoReviewEnabled = true")
    List<RepositoryConfig> findByAutoReviewEnabled();

    /**
     * Find configurations with notifications enabled
     */
    @Query("SELECT c FROM RepositoryConfig c WHERE c.notificationsEnabled = true")
    List<RepositoryConfig> findByNotificationsEnabled();

    /**
     * Find configurations by installation and auto-review enabled
     */
    List<RepositoryConfig> findByInstallationIdAndAutoReviewEnabled(Long installationId, Boolean autoReviewEnabled);

    /**
     * Find configurations by branch pattern
     */
    @Query("SELECT c FROM RepositoryConfig c WHERE c.branchPatterns LIKE %:pattern%")
    List<RepositoryConfig> findByBranchPattern(@Param("pattern") String pattern);

    /**
     * Count configurations by installation
     */
    @Query("SELECT COUNT(c) FROM RepositoryConfig c WHERE c.installationId = :installationId")
    Long countByInstallationId(@Param("installationId") Long installationId);

    /**
     * Find configurations by repository owner
     */
    @Query("SELECT c FROM RepositoryConfig c WHERE c.repositoryFullName LIKE :owner%")
    List<RepositoryConfig> findByRepositoryOwner(@Param("owner") String owner);

    /**
     * Check if repository exists in config
     */
    @Query("SELECT COUNT(c) > 0 FROM RepositoryConfig c WHERE c.repositoryFullName = :repoName")
    boolean existsByRepositoryFullName(@Param("repoName") String repositoryFullName);
}