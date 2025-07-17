package com.zamaz.mcp.github.repository;

import com.zamaz.mcp.github.entity.PRHistoricalMetrics;
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
 * Repository for managing PR historical metrics
 */
@Repository
public interface PRHistoricalMetricsRepository extends JpaRepository<PRHistoricalMetrics, Long> {

    /**
     * Find metrics by repository ID
     */
    List<PRHistoricalMetrics> findByRepositoryId(Long repositoryId);

    /**
     * Find metrics by PR author ID
     */
    List<PRHistoricalMetrics> findByPrAuthorId(Long prAuthorId);

    /**
     * Find metrics by repository ID and PR number
     */
    Optional<PRHistoricalMetrics> findByRepositoryIdAndPrNumber(Long repositoryId, Integer prNumber);

    /**
     * Find metrics by repository ID created after a specific date
     */
    List<PRHistoricalMetrics> findByRepositoryIdAndCreatedAtAfter(Long repositoryId, LocalDateTime date);

    /**
     * Find metrics by PR author ID created after a specific date
     */
    List<PRHistoricalMetrics> findByPrAuthorIdAndCreatedAtAfter(Long prAuthorId, LocalDateTime date);

    /**
     * Find metrics by PR size
     */
    List<PRHistoricalMetrics> findByPrSize(PRHistoricalMetrics.PRSize prSize);

    /**
     * Find metrics by repository ID and PR size
     */
    List<PRHistoricalMetrics> findByRepositoryIdAndPrSize(Long repositoryId, PRHistoricalMetrics.PRSize prSize);

    /**
     * Find metrics with complexity score above threshold
     */
    List<PRHistoricalMetrics> findByComplexityScoreGreaterThan(BigDecimal threshold);

    /**
     * Find metrics with code quality score below threshold
     */
    List<PRHistoricalMetrics> findByCodeQualityScoreLessThan(BigDecimal threshold);

    /**
     * Find metrics for hotfixes
     */
    List<PRHistoricalMetrics> findByIsHotfixTrue();

    /**
     * Find metrics for features
     */
    List<PRHistoricalMetrics> findByIsFeatureTrue();

    /**
     * Find metrics for refactors
     */
    List<PRHistoricalMetrics> findByIsRefactorTrue();

    /**
     * Find metrics for bug fixes
     */
    List<PRHistoricalMetrics> findByIsBugfixTrue();

    /**
     * Find metrics with merge conflicts
     */
    List<PRHistoricalMetrics> findByMergeConflictsTrue();

    /**
     * Find metrics with CI failures
     */
    List<PRHistoricalMetrics> findByCiFailuresGreaterThan(Integer threshold);

    /**
     * Get average complexity score for repository
     */
    @Query("SELECT AVG(phm.complexityScore) FROM PRHistoricalMetrics phm WHERE phm.repositoryId = :repositoryId")
    BigDecimal getAverageComplexityScoreByRepository(@Param("repositoryId") Long repositoryId);

    /**
     * Get average code quality score for repository
     */
    @Query("SELECT AVG(phm.codeQualityScore) FROM PRHistoricalMetrics phm WHERE phm.repositoryId = :repositoryId")
    BigDecimal getAverageCodeQualityScoreByRepository(@Param("repositoryId") Long repositoryId);

    /**
     * Get average review turnaround time for repository
     */
    @Query("SELECT AVG(phm.reviewTurnaroundHours) FROM PRHistoricalMetrics phm WHERE phm.repositoryId = :repositoryId AND phm.reviewTurnaroundHours IS NOT NULL")
    Double getAverageReviewTurnaroundByRepository(@Param("repositoryId") Long repositoryId);

    /**
     * Get average merge time for repository
     */
    @Query("SELECT AVG(phm.mergeTimeHours) FROM PRHistoricalMetrics phm WHERE phm.repositoryId = :repositoryId AND phm.mergeTimeHours IS NOT NULL")
    Double getAverageMergeTimeByRepository(@Param("repositoryId") Long repositoryId);

    /**
     * Get PR size distribution for repository
     */
    @Query("SELECT phm.prSize, COUNT(phm) FROM PRHistoricalMetrics phm WHERE phm.repositoryId = :repositoryId GROUP BY phm.prSize")
    List<Object[]> getPRSizeDistributionByRepository(@Param("repositoryId") Long repositoryId);

    /**
     * Get top contributors by PR count
     */
    @Query("SELECT phm.prAuthorId, COUNT(phm) FROM PRHistoricalMetrics phm WHERE phm.repositoryId = :repositoryId GROUP BY phm.prAuthorId ORDER BY COUNT(phm) DESC")
    List<Object[]> getTopContributorsByRepository(@Param("repositoryId") Long repositoryId, Pageable pageable);

    /**
     * Get metrics for date range
     */
    List<PRHistoricalMetrics> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get metrics for repository and date range
     */
    List<PRHistoricalMetrics> findByRepositoryIdAndCreatedAtBetween(Long repositoryId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get metrics for author and date range
     */
    List<PRHistoricalMetrics> findByPrAuthorIdAndCreatedAtBetween(Long prAuthorId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find metrics with high complexity and low quality
     */
    @Query("SELECT phm FROM PRHistoricalMetrics phm WHERE phm.complexityScore > :complexityThreshold AND phm.codeQualityScore < :qualityThreshold")
    List<PRHistoricalMetrics> findHighComplexityLowQuality(@Param("complexityThreshold") BigDecimal complexityThreshold, 
                                                           @Param("qualityThreshold") BigDecimal qualityThreshold);

    /**
     * Get monthly PR metrics summary
     */
    @Query("SELECT EXTRACT(YEAR FROM phm.createdAt), EXTRACT(MONTH FROM phm.createdAt), COUNT(phm), AVG(phm.complexityScore), AVG(phm.codeQualityScore) " +
           "FROM PRHistoricalMetrics phm WHERE phm.repositoryId = :repositoryId " +
           "GROUP BY EXTRACT(YEAR FROM phm.createdAt), EXTRACT(MONTH FROM phm.createdAt) " +
           "ORDER BY EXTRACT(YEAR FROM phm.createdAt), EXTRACT(MONTH FROM phm.createdAt)")
    List<Object[]> getMonthlyMetricsSummary(@Param("repositoryId") Long repositoryId);

    /**
     * Get developer performance metrics
     */
    @Query("SELECT phm.prAuthorId, COUNT(phm), AVG(phm.complexityScore), AVG(phm.codeQualityScore), AVG(phm.reviewTurnaroundHours), AVG(phm.mergeTimeHours) " +
           "FROM PRHistoricalMetrics phm WHERE phm.repositoryId = :repositoryId " +
           "GROUP BY phm.prAuthorId")
    List<Object[]> getDeveloperPerformanceMetrics(@Param("repositoryId") Long repositoryId);

    /**
     * Find most recent metrics
     */
    List<PRHistoricalMetrics> findTop10ByOrderByCreatedAtDesc();

    /**
     * Find metrics for specific time period
     */
    @Query("SELECT phm FROM PRHistoricalMetrics phm WHERE phm.createdAt >= :startDate AND phm.createdAt <= :endDate ORDER BY phm.createdAt DESC")
    List<PRHistoricalMetrics> findByTimePeriod(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Get paginated metrics for repository
     */
    Page<PRHistoricalMetrics> findByRepositoryIdOrderByCreatedAtDesc(Long repositoryId, Pageable pageable);

    /**
     * Get paginated metrics for author
     */
    Page<PRHistoricalMetrics> findByPrAuthorIdOrderByCreatedAtDesc(Long prAuthorId, Pageable pageable);
}