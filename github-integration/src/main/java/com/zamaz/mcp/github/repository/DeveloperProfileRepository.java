package com.zamaz.mcp.github.repository;

import com.zamaz.mcp.github.entity.DeveloperProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing developer profiles
 */
@Repository
public interface DeveloperProfileRepository extends JpaRepository<DeveloperProfile, Long> {

    /**
     * Find developer profile by GitHub user ID
     */
    Optional<DeveloperProfile> findByGithubUserId(Long githubUserId);

    /**
     * Find developer profile by GitHub username
     */
    Optional<DeveloperProfile> findByGithubUsername(String githubUsername);

    /**
     * Check if developer profile exists by GitHub user ID
     */
    boolean existsByGithubUserId(Long githubUserId);

    /**
     * Check if developer profile exists by GitHub username
     */
    boolean existsByGithubUsername(String githubUsername);

    /**
     * Find developers by experience level
     */
    List<DeveloperProfile> findByExperienceLevel(DeveloperProfile.ExperienceLevel experienceLevel);

    /**
     * Find developers by communication style
     */
    List<DeveloperProfile> findByCommunicationStyle(DeveloperProfile.CommunicationStyle communicationStyle);

    /**
     * Find developers by primary language
     */
    @Query("SELECT dp FROM DeveloperProfile dp WHERE :language = ANY(dp.primaryLanguages)")
    List<DeveloperProfile> findByPrimaryLanguage(@Param("language") String language);

    /**
     * Find developers by domain expertise
     */
    @Query("SELECT dp FROM DeveloperProfile dp WHERE :domain = ANY(dp.domainExpertise)")
    List<DeveloperProfile> findByDomainExpertise(@Param("domain") String domain);

    /**
     * Find developers created after a specific date
     */
    List<DeveloperProfile> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find developers updated after a specific date
     */
    List<DeveloperProfile> findByUpdatedAtAfter(LocalDateTime date);

    /**
     * Find developers with specific experience levels
     */
    List<DeveloperProfile> findByExperienceLevelIn(List<DeveloperProfile.ExperienceLevel> levels);

    /**
     * Get paginated list of all developer profiles
     */
    Page<DeveloperProfile> findAll(Pageable pageable);

    /**
     * Search developers by display name or username
     */
    @Query("SELECT dp FROM DeveloperProfile dp WHERE " +
           "LOWER(dp.displayName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(dp.githubUsername) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<DeveloperProfile> searchByNameOrUsername(@Param("searchTerm") String searchTerm);

    /**
     * Find developers by timezone
     */
    List<DeveloperProfile> findByTimezone(String timezone);

    /**
     * Get count of developers by experience level
     */
    @Query("SELECT dp.experienceLevel, COUNT(dp) FROM DeveloperProfile dp GROUP BY dp.experienceLevel")
    List<Object[]> countByExperienceLevel();

    /**
     * Get count of developers by communication style
     */
    @Query("SELECT dp.communicationStyle, COUNT(dp) FROM DeveloperProfile dp GROUP BY dp.communicationStyle")
    List<Object[]> countByCommunicationStyle();

    /**
     * Find most active developers based on recent updates
     */
    @Query("SELECT dp FROM DeveloperProfile dp ORDER BY dp.updatedAt DESC")
    List<DeveloperProfile> findMostRecentlyActive(Pageable pageable);

    /**
     * Find developers with specific learning preference
     */
    @Query(value = "SELECT * FROM developer_profile dp WHERE dp.learning_preferences ? :key", nativeQuery = true)
    List<DeveloperProfile> findByLearningPreferenceKey(@Param("key") String key);

    /**
     * Find developers with specific learning preference value
     */
    @Query(value = "SELECT * FROM developer_profile dp WHERE dp.learning_preferences ->> :key = :value", nativeQuery = true)
    List<DeveloperProfile> findByLearningPreferenceValue(@Param("key") String key, @Param("value") String value);
}