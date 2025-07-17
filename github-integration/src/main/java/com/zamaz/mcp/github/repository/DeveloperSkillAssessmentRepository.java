package com.zamaz.mcp.github.repository;

import com.zamaz.mcp.github.entity.DeveloperSkillAssessment;
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
 * Repository for managing developer skill assessments
 */
@Repository
public interface DeveloperSkillAssessmentRepository extends JpaRepository<DeveloperSkillAssessment, Long> {

    /**
     * Find all skill assessments for a developer
     */
    List<DeveloperSkillAssessment> findByDeveloperId(Long developerId);

    /**
     * Find specific skill assessment for a developer
     */
    Optional<DeveloperSkillAssessment> findByDeveloperIdAndSkillCategory(Long developerId, String skillCategory);

    /**
     * Find assessments by skill category
     */
    List<DeveloperSkillAssessment> findBySkillCategory(String skillCategory);

    /**
     * Find assessments by skill level
     */
    List<DeveloperSkillAssessment> findBySkillLevel(DeveloperSkillAssessment.SkillLevel skillLevel);

    /**
     * Find assessments by improvement trend
     */
    List<DeveloperSkillAssessment> findByImprovementTrend(DeveloperSkillAssessment.ImprovementTrend trend);

    /**
     * Find assessments with confidence score above threshold
     */
    List<DeveloperSkillAssessment> findByConfidenceScoreGreaterThan(BigDecimal threshold);

    /**
     * Find assessments with confidence score below threshold
     */
    List<DeveloperSkillAssessment> findByConfidenceScoreLessThan(BigDecimal threshold);

    /**
     * Find assessments by evidence count range
     */
    List<DeveloperSkillAssessment> findByEvidenceCountBetween(Integer minCount, Integer maxCount);

    /**
     * Find assessments demonstrated after a specific date
     */
    List<DeveloperSkillAssessment> findByLastDemonstrationDateAfter(LocalDateTime date);

    /**
     * Find assessments updated after a specific date
     */
    List<DeveloperSkillAssessment> findByUpdatedAtAfter(LocalDateTime date);

    /**
     * Find assessments for developer and skill level
     */
    List<DeveloperSkillAssessment> findByDeveloperIdAndSkillLevel(Long developerId, DeveloperSkillAssessment.SkillLevel skillLevel);

    /**
     * Find assessments for developer and improvement trend
     */
    List<DeveloperSkillAssessment> findByDeveloperIdAndImprovementTrend(Long developerId, DeveloperSkillAssessment.ImprovementTrend trend);

    /**
     * Get average confidence score for a skill category
     */
    @Query("SELECT AVG(dsa.confidenceScore) FROM DeveloperSkillAssessment dsa WHERE dsa.skillCategory = :skillCategory")
    BigDecimal getAverageConfidenceBySkillCategory(@Param("skillCategory") String skillCategory);

    /**
     * Get skill level distribution for a category
     */
    @Query("SELECT dsa.skillLevel, COUNT(dsa) FROM DeveloperSkillAssessment dsa WHERE dsa.skillCategory = :skillCategory GROUP BY dsa.skillLevel")
    List<Object[]> getSkillLevelDistribution(@Param("skillCategory") String skillCategory);

    /**
     * Get improvement trend distribution for a category
     */
    @Query("SELECT dsa.improvementTrend, COUNT(dsa) FROM DeveloperSkillAssessment dsa WHERE dsa.skillCategory = :skillCategory GROUP BY dsa.improvementTrend")
    List<Object[]> getImprovementTrendDistribution(@Param("skillCategory") String skillCategory);

    /**
     * Find top performers in a skill category
     */
    @Query("SELECT dsa FROM DeveloperSkillAssessment dsa WHERE dsa.skillCategory = :skillCategory ORDER BY dsa.confidenceScore DESC, dsa.evidenceCount DESC")
    List<DeveloperSkillAssessment> getTopPerformers(@Param("skillCategory") String skillCategory, Pageable pageable);

    /**
     * Find developers needing improvement in a skill category
     */
    @Query("SELECT dsa FROM DeveloperSkillAssessment dsa WHERE dsa.skillCategory = :skillCategory AND dsa.improvementTrend = 'DECLINING' ORDER BY dsa.confidenceScore ASC")
    List<DeveloperSkillAssessment> getDevelopersNeedingImprovement(@Param("skillCategory") String skillCategory);

    /**
     * Find rapidly improving developers
     */
    @Query("SELECT dsa FROM DeveloperSkillAssessment dsa WHERE dsa.improvementTrend = 'IMPROVING' AND dsa.updatedAt > :recentDate ORDER BY dsa.confidenceScore DESC")
    List<DeveloperSkillAssessment> getRapidlyImprovingDevelopers(@Param("recentDate") LocalDateTime recentDate);

    /**
     * Find stagnating skills (not demonstrated recently)
     */
    @Query("SELECT dsa FROM DeveloperSkillAssessment dsa WHERE dsa.lastDemonstrationDate < :cutoffDate OR dsa.lastDemonstrationDate IS NULL ORDER BY dsa.lastDemonstrationDate ASC")
    List<DeveloperSkillAssessment> getStagnatingSkills(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Get skill category summary statistics
     */
    @Query("SELECT dsa.skillCategory, COUNT(dsa), AVG(dsa.confidenceScore), MIN(dsa.confidenceScore), MAX(dsa.confidenceScore) " +
           "FROM DeveloperSkillAssessment dsa " +
           "GROUP BY dsa.skillCategory " +
           "ORDER BY AVG(dsa.confidenceScore) DESC")
    List<Object[]> getSkillCategoryStatistics();

    /**
     * Get developer skill summary
     */
    @Query("SELECT dsa.skillCategory, dsa.skillLevel, dsa.confidenceScore, dsa.improvementTrend " +
           "FROM DeveloperSkillAssessment dsa WHERE dsa.developerId = :developerId " +
           "ORDER BY dsa.confidenceScore DESC")
    List<Object[]> getDeveloperSkillSummary(@Param("developerId") Long developerId);

    /**
     * Find assessments with specific learning goals
     */
    @Query("SELECT dsa FROM DeveloperSkillAssessment dsa WHERE :goal = ANY(dsa.learningGoals)")
    List<DeveloperSkillAssessment> findByLearningGoal(@Param("goal") String goal);

    /**
     * Find assessments with specific recommended resources
     */
    @Query("SELECT dsa FROM DeveloperSkillAssessment dsa WHERE :resource = ANY(dsa.recommendedResources)")
    List<DeveloperSkillAssessment> findByRecommendedResource(@Param("resource") String resource);

    /**
     * Get monthly skill progression for a developer
     */
    @Query("SELECT EXTRACT(YEAR FROM dsa.updatedAt), EXTRACT(MONTH FROM dsa.updatedAt), dsa.skillCategory, AVG(dsa.confidenceScore) " +
           "FROM DeveloperSkillAssessment dsa WHERE dsa.developerId = :developerId " +
           "GROUP BY EXTRACT(YEAR FROM dsa.updatedAt), EXTRACT(MONTH FROM dsa.updatedAt), dsa.skillCategory " +
           "ORDER BY EXTRACT(YEAR FROM dsa.updatedAt), EXTRACT(MONTH FROM dsa.updatedAt)")
    List<Object[]> getMonthlySkillProgression(@Param("developerId") Long developerId);

    /**
     * Find complementary skills (skills that tend to improve together)
     */
    @Query("SELECT dsa1.skillCategory, dsa2.skillCategory, COUNT(*) as correlation " +
           "FROM DeveloperSkillAssessment dsa1 JOIN DeveloperSkillAssessment dsa2 ON dsa1.developerId = dsa2.developerId " +
           "WHERE dsa1.skillCategory < dsa2.skillCategory AND dsa1.improvementTrend = 'IMPROVING' AND dsa2.improvementTrend = 'IMPROVING' " +
           "GROUP BY dsa1.skillCategory, dsa2.skillCategory " +
           "HAVING COUNT(*) > 1 " +
           "ORDER BY COUNT(*) DESC")
    List<Object[]> findComplementarySkills();

    /**
     * Get skill assessment trends over time
     */
    @Query("SELECT DATE(dsa.updatedAt), dsa.skillCategory, AVG(dsa.confidenceScore) " +
           "FROM DeveloperSkillAssessment dsa " +
           "WHERE dsa.updatedAt BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(dsa.updatedAt), dsa.skillCategory " +
           "ORDER BY DATE(dsa.updatedAt)")
    List<Object[]> getSkillTrends(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Find developers with similar skill profiles
     */
    @Query("SELECT dsa2.developerId, COUNT(dsa2) as similarSkills " +
           "FROM DeveloperSkillAssessment dsa1 JOIN DeveloperSkillAssessment dsa2 ON dsa1.skillCategory = dsa2.skillCategory " +
           "WHERE dsa1.developerId = :developerId AND dsa2.developerId != :developerId " +
           "AND ABS(dsa1.confidenceScore - dsa2.confidenceScore) < 10 " +
           "GROUP BY dsa2.developerId " +
           "HAVING COUNT(dsa2) >= 3 " +
           "ORDER BY COUNT(dsa2) DESC")
    List<Object[]> findSimilarSkillProfiles(@Param("developerId") Long developerId);

    /**
     * Get paginated assessments for a developer
     */
    Page<DeveloperSkillAssessment> findByDeveloperIdOrderByConfidenceScoreDesc(Long developerId, Pageable pageable);

    /**
     * Get most recently updated assessments
     */
    List<DeveloperSkillAssessment> findTop20ByOrderByUpdatedAtDesc();

    /**
     * Count assessments by skill level
     */
    @Query("SELECT dsa.skillLevel, COUNT(dsa) FROM DeveloperSkillAssessment dsa GROUP BY dsa.skillLevel")
    List<Object[]> countBySkillLevel();

    /**
     * Count assessments by improvement trend
     */
    @Query("SELECT dsa.improvementTrend, COUNT(dsa) FROM DeveloperSkillAssessment dsa GROUP BY dsa.improvementTrend")
    List<Object[]> countByImprovementTrend();
}