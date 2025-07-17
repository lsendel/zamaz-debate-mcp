package com.zamaz.mcp.github.repository;

import com.zamaz.mcp.github.entity.KnowledgeBaseEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing knowledge base entries
 */
@Repository
public interface KnowledgeBaseEntryRepository extends JpaRepository<KnowledgeBaseEntry, Long> {

    /**
     * Find entries by repository ID
     */
    List<KnowledgeBaseEntry> findByRepositoryId(Long repositoryId);

    /**
     * Find entries by repository ID with pagination
     */
    Page<KnowledgeBaseEntry> findByRepositoryId(Long repositoryId, Pageable pageable);

    /**
     * Find entries by category
     */
    List<KnowledgeBaseEntry> findByCategory(String category);

    /**
     * Find entries by category with pagination
     */
    Page<KnowledgeBaseEntry> findByCategory(String category, Pageable pageable);

    /**
     * Find entries by repository ID and category
     */
    List<KnowledgeBaseEntry> findByRepositoryIdAndCategory(Long repositoryId, String category);

    /**
     * Find entries by repository ID and category with pagination
     */
    Page<KnowledgeBaseEntry> findByRepositoryIdAndCategory(Long repositoryId, String category, Pageable pageable);

    /**
     * Find entries by approval status
     */
    List<KnowledgeBaseEntry> findByIsApproved(Boolean isApproved);

    /**
     * Find entries by repository ID and approval status
     */
    List<KnowledgeBaseEntry> findByRepositoryIdAndIsApproved(Long repositoryId, Boolean isApproved);

    /**
     * Find entries by severity
     */
    List<KnowledgeBaseEntry> findBySeverity(KnowledgeBaseEntry.Severity severity);

    /**
     * Find entries by language
     */
    List<KnowledgeBaseEntry> findByLanguage(String language);

    /**
     * Find entries by framework
     */
    List<KnowledgeBaseEntry> findByFramework(String framework);

    /**
     * Find entries by created by user ID
     */
    List<KnowledgeBaseEntry> findByCreatedByUserId(Long createdByUserId);

    /**
     * Find entries by approved by user ID
     */
    List<KnowledgeBaseEntry> findByApprovedByUserId(Long approvedByUserId);

    /**
     * Find entries created after a specific date
     */
    List<KnowledgeBaseEntry> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find entries updated after a specific date
     */
    List<KnowledgeBaseEntry> findByUpdatedAtAfter(LocalDateTime date);

    /**
     * Find entries approved after a specific date
     */
    List<KnowledgeBaseEntry> findByApprovalDateAfter(LocalDateTime date);

    /**
     * Find entries with effectiveness score above threshold
     */
    List<KnowledgeBaseEntry> findByEffectivenessScoreGreaterThan(BigDecimal threshold);

    /**
     * Find entries with frequency count above threshold
     */
    List<KnowledgeBaseEntry> findByFrequencyCountGreaterThan(Integer threshold);

    /**
     * Find entries by specific tag
     */
    @Query("SELECT kbe FROM KnowledgeBaseEntry kbe WHERE :tag = ANY(kbe.tags)")
    List<KnowledgeBaseEntry> findByTag(@Param("tag") String tag);

    /**
     * Find entries containing any of the specified tags
     */
    @Query("SELECT kbe FROM KnowledgeBaseEntry kbe WHERE kbe.tags && CAST(:tags AS text[])")
    List<KnowledgeBaseEntry> findByAnyTag(@Param("tags") List<String> tags);

    /**
     * Find entries from specific source review IDs
     */
    @Query("SELECT kbe FROM KnowledgeBaseEntry kbe WHERE :reviewId = ANY(kbe.sourceReviewIds)")
    List<KnowledgeBaseEntry> findBySourceReviewId(@Param("reviewId") Long reviewId);

    /**
     * Search entries by title or content
     */
    @Query("SELECT kbe FROM KnowledgeBaseEntry kbe WHERE " +
           "LOWER(kbe.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(kbe.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(kbe.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<KnowledgeBaseEntry> searchByTitleOrContent(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Advanced search with multiple criteria
     */
    @Query("SELECT kbe FROM KnowledgeBaseEntry kbe WHERE " +
           "(:repositoryId IS NULL OR kbe.repositoryId = :repositoryId) AND " +
           "(:category IS NULL OR kbe.category = :category) AND " +
           "(:language IS NULL OR kbe.language = :language) AND " +
           "(:framework IS NULL OR kbe.framework = :framework) AND " +
           "(:severity IS NULL OR kbe.severity = :severity) AND " +
           "(:isApproved IS NULL OR kbe.isApproved = :isApproved) AND " +
           "(:searchTerm IS NULL OR LOWER(kbe.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(kbe.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<KnowledgeBaseEntry> advancedSearch(@Param("repositoryId") Long repositoryId,
                                           @Param("category") String category,
                                           @Param("language") String language,
                                           @Param("framework") String framework,
                                           @Param("severity") KnowledgeBaseEntry.Severity severity,
                                           @Param("isApproved") Boolean isApproved,
                                           @Param("searchTerm") String searchTerm,
                                           Pageable pageable);

    /**
     * Get most effective entries
     */
    @Query("SELECT kbe FROM KnowledgeBaseEntry kbe WHERE kbe.isApproved = true ORDER BY kbe.effectivenessScore DESC")
    List<KnowledgeBaseEntry> findMostEffective(Pageable pageable);

    /**
     * Get most frequent patterns
     */
    @Query("SELECT kbe FROM KnowledgeBaseEntry kbe WHERE kbe.isApproved = true ORDER BY kbe.frequencyCount DESC")
    List<KnowledgeBaseEntry> findMostFrequent(Pageable pageable);

    /**
     * Get category distribution
     */
    @Query("SELECT kbe.category, COUNT(kbe) FROM KnowledgeBaseEntry kbe WHERE kbe.repositoryId = :repositoryId GROUP BY kbe.category")
    List<Object[]> getCategoryDistribution(@Param("repositoryId") Long repositoryId);

    /**
     * Get severity distribution
     */
    @Query("SELECT kbe.severity, COUNT(kbe) FROM KnowledgeBaseEntry kbe WHERE kbe.repositoryId = :repositoryId GROUP BY kbe.severity")
    List<Object[]> getSeverityDistribution(@Param("repositoryId") Long repositoryId);

    /**
     * Get language distribution
     */
    @Query("SELECT kbe.language, COUNT(kbe) FROM KnowledgeBaseEntry kbe WHERE kbe.repositoryId = :repositoryId AND kbe.language IS NOT NULL GROUP BY kbe.language")
    List<Object[]> getLanguageDistribution(@Param("repositoryId") Long repositoryId);

    /**
     * Get framework distribution
     */
    @Query("SELECT kbe.framework, COUNT(kbe) FROM KnowledgeBaseEntry kbe WHERE kbe.repositoryId = :repositoryId AND kbe.framework IS NOT NULL GROUP BY kbe.framework")
    List<Object[]> getFrameworkDistribution(@Param("repositoryId") Long repositoryId);

    /**
     * Get top contributors
     */
    @Query("SELECT kbe.createdByUserId, COUNT(kbe) FROM KnowledgeBaseEntry kbe WHERE kbe.repositoryId = :repositoryId GROUP BY kbe.createdByUserId ORDER BY COUNT(kbe) DESC")
    List<Object[]> getTopContributors(@Param("repositoryId") Long repositoryId, Pageable pageable);

    /**
     * Get approval statistics
     */
    @Query("SELECT COUNT(kbe) as total, " +
           "SUM(CASE WHEN kbe.isApproved = true THEN 1 ELSE 0 END) as approved, " +
           "SUM(CASE WHEN kbe.isApproved = false THEN 1 ELSE 0 END) as pending " +
           "FROM KnowledgeBaseEntry kbe WHERE kbe.repositoryId = :repositoryId")
    Object[] getApprovalStatistics(@Param("repositoryId") Long repositoryId);

    /**
     * Get average effectiveness score
     */
    @Query("SELECT AVG(kbe.effectivenessScore) FROM KnowledgeBaseEntry kbe WHERE kbe.repositoryId = :repositoryId AND kbe.effectivenessScore IS NOT NULL")
    BigDecimal getAverageEffectivenessScore(@Param("repositoryId") Long repositoryId);

    /**
     * Get entries pending approval
     */
    @Query("SELECT kbe FROM KnowledgeBaseEntry kbe WHERE kbe.isApproved = false ORDER BY kbe.createdAt DESC")
    List<KnowledgeBaseEntry> findPendingApproval(Pageable pageable);

    /**
     * Get entries by repository pending approval
     */
    @Query("SELECT kbe FROM KnowledgeBaseEntry kbe WHERE kbe.repositoryId = :repositoryId AND kbe.isApproved = false ORDER BY kbe.createdAt DESC")
    List<KnowledgeBaseEntry> findPendingApprovalByRepository(@Param("repositoryId") Long repositoryId, Pageable pageable);

    /**
     * Get recently created entries
     */
    @Query("SELECT kbe FROM KnowledgeBaseEntry kbe WHERE kbe.createdAt > :date ORDER BY kbe.createdAt DESC")
    List<KnowledgeBaseEntry> findRecentlyCreated(@Param("date") LocalDateTime date);

    /**
     * Get recently updated entries
     */
    @Query("SELECT kbe FROM KnowledgeBaseEntry kbe WHERE kbe.updatedAt > :date ORDER BY kbe.updatedAt DESC")
    List<KnowledgeBaseEntry> findRecentlyUpdated(@Param("date") LocalDateTime date);

    /**
     * Get entries by effectiveness score range
     */
    @Query("SELECT kbe FROM KnowledgeBaseEntry kbe WHERE kbe.effectivenessScore BETWEEN :minScore AND :maxScore ORDER BY kbe.effectivenessScore DESC")
    List<KnowledgeBaseEntry> findByEffectivenessScoreRange(@Param("minScore") BigDecimal minScore, @Param("maxScore") BigDecimal maxScore);

    /**
     * Get similar entries by tags
     */
    @Query("SELECT kbe FROM KnowledgeBaseEntry kbe WHERE kbe.id != :excludeId AND kbe.tags && CAST(:tags AS text[]) ORDER BY kbe.effectivenessScore DESC")
    List<KnowledgeBaseEntry> findSimilarByTags(@Param("excludeId") Long excludeId, @Param("tags") List<String> tags, Pageable pageable);

    /**
     * Get entries by category and language
     */
    @Query("SELECT kbe FROM KnowledgeBaseEntry kbe WHERE kbe.category = :category AND kbe.language = :language AND kbe.isApproved = true ORDER BY kbe.effectivenessScore DESC")
    List<KnowledgeBaseEntry> findByCategoryAndLanguage(@Param("category") String category, @Param("language") String language);

    /**
     * Get monthly entry creation statistics
     */
    @Query("SELECT EXTRACT(YEAR FROM kbe.createdAt), EXTRACT(MONTH FROM kbe.createdAt), COUNT(kbe) " +
           "FROM KnowledgeBaseEntry kbe WHERE kbe.repositoryId = :repositoryId " +
           "GROUP BY EXTRACT(YEAR FROM kbe.createdAt), EXTRACT(MONTH FROM kbe.createdAt) " +
           "ORDER BY EXTRACT(YEAR FROM kbe.createdAt), EXTRACT(MONTH FROM kbe.createdAt)")
    List<Object[]> getMonthlyCreationStats(@Param("repositoryId") Long repositoryId);

    /**
     * Get tag usage statistics
     */
    @Query(value = "SELECT tag, COUNT(*) as usage_count FROM " +
                   "(SELECT UNNEST(tags) as tag FROM knowledge_base_entry WHERE repository_id = :repositoryId) t " +
                   "GROUP BY tag ORDER BY usage_count DESC", nativeQuery = true)
    List<Object[]> getTagUsageStats(@Param("repositoryId") Long repositoryId);

    /**
     * Find entries that need review (low effectiveness or old)
     */
    @Query("SELECT kbe FROM KnowledgeBaseEntry kbe WHERE " +
           "kbe.isApproved = true AND " +
           "(kbe.effectivenessScore < :minEffectiveness OR kbe.updatedAt < :oldDate) " +
           "ORDER BY kbe.effectivenessScore ASC, kbe.updatedAt ASC")
    List<KnowledgeBaseEntry> findEntriesNeedingReview(@Param("minEffectiveness") BigDecimal minEffectiveness, 
                                                     @Param("oldDate") LocalDateTime oldDate, 
                                                     Pageable pageable);

    /**
     * Count entries by repository
     */
    @Query("SELECT COUNT(kbe) FROM KnowledgeBaseEntry kbe WHERE kbe.repositoryId = :repositoryId")
    Long countByRepositoryId(@Param("repositoryId") Long repositoryId);

    /**
     * Count approved entries by repository
     */
    @Query("SELECT COUNT(kbe) FROM KnowledgeBaseEntry kbe WHERE kbe.repositoryId = :repositoryId AND kbe.isApproved = true")
    Long countApprovedByRepositoryId(@Param("repositoryId") Long repositoryId);

    /**
     * Find entries with high frequency but low effectiveness
     */
    @Query("SELECT kbe FROM KnowledgeBaseEntry kbe WHERE kbe.frequencyCount > :minFrequency AND kbe.effectivenessScore < :maxEffectiveness ORDER BY kbe.frequencyCount DESC")
    List<KnowledgeBaseEntry> findHighFrequencyLowEffectiveness(@Param("minFrequency") Integer minFrequency, 
                                                               @Param("maxEffectiveness") BigDecimal maxEffectiveness);
}