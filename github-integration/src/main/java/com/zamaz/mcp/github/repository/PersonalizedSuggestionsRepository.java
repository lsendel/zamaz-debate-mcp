package com.zamaz.mcp.github.repository;

import com.zamaz.mcp.github.entity.PersonalizedSuggestions;
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
 * Repository for managing personalized suggestions
 */
@Repository
public interface PersonalizedSuggestionsRepository extends JpaRepository<PersonalizedSuggestions, Long> {

    /**
     * Find suggestions by developer ID
     */
    List<PersonalizedSuggestions> findByDeveloperId(Long developerId);

    /**
     * Find suggestions by developer ID with pagination
     */
    Page<PersonalizedSuggestions> findByDeveloperId(Long developerId, Pageable pageable);

    /**
     * Find suggestions by developer ID created after a specific date
     */
    List<PersonalizedSuggestions> findByDeveloperIdAndCreatedAtAfter(Long developerId, LocalDateTime date);

    /**
     * Find suggestions by suggestion type
     */
    List<PersonalizedSuggestions> findBySuggestionType(String suggestionType);

    /**
     * Find suggestions by priority level
     */
    List<PersonalizedSuggestions> findByPriorityLevel(PersonalizedSuggestions.PriorityLevel priorityLevel);

    /**
     * Find accepted suggestions
     */
    List<PersonalizedSuggestions> findByIsAcceptedTrue();

    /**
     * Find rejected suggestions
     */
    List<PersonalizedSuggestions> findByIsAcceptedFalse();

    /**
     * Find pending suggestions (not yet accepted or rejected)
     */
    List<PersonalizedSuggestions> findByIsAcceptedIsNull();

    /**
     * Find suggestions by developer ID and acceptance status
     */
    List<PersonalizedSuggestions> findByDeveloperIdAndIsAccepted(Long developerId, Boolean isAccepted);

    /**
     * Find suggestions by developer ID, acceptance status, and effectiveness rating
     */
    List<PersonalizedSuggestions> findByDeveloperIdAndIsAcceptedAndEffectivenessRatingGreaterThan(
        Long developerId, Boolean isAccepted, Integer effectivenessRating);

    /**
     * Find suggestions with confidence score above threshold
     */
    List<PersonalizedSuggestions> findByConfidenceScoreGreaterThan(BigDecimal threshold);

    /**
     * Find suggestions with effectiveness rating above threshold
     */
    List<PersonalizedSuggestions> findByEffectivenessRatingGreaterThan(Integer threshold);

    /**
     * Find suggestions by developer ID and suggestion type
     */
    List<PersonalizedSuggestions> findByDeveloperIdAndSuggestionType(Long developerId, String suggestionType);

    /**
     * Find suggestions by developer ID and priority level
     */
    List<PersonalizedSuggestions> findByDeveloperIdAndPriorityLevel(Long developerId, PersonalizedSuggestions.PriorityLevel priorityLevel);

    /**
     * Find suggestions created in a date range
     */
    List<PersonalizedSuggestions> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find suggestions accepted in a date range
     */
    List<PersonalizedSuggestions> findByAcceptanceDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find suggestions by trigger condition
     */
    @Query("SELECT ps FROM PersonalizedSuggestions ps WHERE :condition = ANY(ps.triggerConditions)")
    List<PersonalizedSuggestions> findByTriggerCondition(@Param("condition") String condition);

    /**
     * Get suggestion type distribution for a developer
     */
    @Query("SELECT ps.suggestionType, COUNT(ps) FROM PersonalizedSuggestions ps WHERE ps.developerId = :developerId GROUP BY ps.suggestionType")
    List<Object[]> getSuggestionTypeDistribution(@Param("developerId") Long developerId);

    /**
     * Get priority level distribution for a developer
     */
    @Query("SELECT ps.priorityLevel, COUNT(ps) FROM PersonalizedSuggestions ps WHERE ps.developerId = :developerId GROUP BY ps.priorityLevel")
    List<Object[]> getPriorityLevelDistribution(@Param("developerId") Long developerId);

    /**
     * Get acceptance rate by suggestion type
     */
    @Query("SELECT ps.suggestionType, " +
           "COUNT(ps) as total, " +
           "SUM(CASE WHEN ps.isAccepted = true THEN 1 ELSE 0 END) as accepted " +
           "FROM PersonalizedSuggestions ps " +
           "WHERE ps.developerId = :developerId " +
           "GROUP BY ps.suggestionType")
    List<Object[]> getAcceptanceRateByType(@Param("developerId") Long developerId);

    /**
     * Get average effectiveness rating by suggestion type
     */
    @Query("SELECT ps.suggestionType, AVG(ps.effectivenessRating) " +
           "FROM PersonalizedSuggestions ps " +
           "WHERE ps.developerId = :developerId AND ps.effectivenessRating IS NOT NULL " +
           "GROUP BY ps.suggestionType")
    List<Object[]> getAverageEffectivenessRating(@Param("developerId") Long developerId);

    /**
     * Get average confidence score by suggestion type
     */
    @Query("SELECT ps.suggestionType, AVG(ps.confidenceScore) " +
           "FROM PersonalizedSuggestions ps " +
           "WHERE ps.developerId = :developerId AND ps.confidenceScore IS NOT NULL " +
           "GROUP BY ps.suggestionType")
    List<Object[]> getAverageConfidenceScore(@Param("developerId") Long developerId);

    /**
     * Find most effective suggestions
     */
    @Query("SELECT ps FROM PersonalizedSuggestions ps " +
           "WHERE ps.isAccepted = true AND ps.effectivenessRating IS NOT NULL " +
           "ORDER BY ps.effectivenessRating DESC, ps.confidenceScore DESC")
    List<PersonalizedSuggestions> findMostEffective(Pageable pageable);

    /**
     * Find least effective suggestions
     */
    @Query("SELECT ps FROM PersonalizedSuggestions ps " +
           "WHERE ps.isAccepted = true AND ps.effectivenessRating IS NOT NULL " +
           "ORDER BY ps.effectivenessRating ASC, ps.confidenceScore ASC")
    List<PersonalizedSuggestions> findLeastEffective(Pageable pageable);

    /**
     * Find suggestions with high confidence but low acceptance
     */
    @Query("SELECT ps FROM PersonalizedSuggestions ps " +
           "WHERE ps.confidenceScore > :confidenceThreshold AND ps.isAccepted = false " +
           "ORDER BY ps.confidenceScore DESC")
    List<PersonalizedSuggestions> findHighConfidenceLowAcceptance(@Param("confidenceThreshold") BigDecimal confidenceThreshold);

    /**
     * Find recent suggestions for a developer
     */
    @Query("SELECT ps FROM PersonalizedSuggestions ps " +
           "WHERE ps.developerId = :developerId AND ps.createdAt > :recentDate " +
           "ORDER BY ps.createdAt DESC")
    List<PersonalizedSuggestions> findRecentSuggestions(@Param("developerId") Long developerId, @Param("recentDate") LocalDateTime recentDate);

    /**
     * Find pending high-priority suggestions
     */
    @Query("SELECT ps FROM PersonalizedSuggestions ps " +
           "WHERE ps.isAccepted IS NULL AND ps.priorityLevel = 'HIGH' " +
           "ORDER BY ps.createdAt ASC")
    List<PersonalizedSuggestions> findPendingHighPriority();

    /**
     * Find suggestions that need follow-up (accepted but no effectiveness rating)
     */
    @Query("SELECT ps FROM PersonalizedSuggestions ps " +
           "WHERE ps.isAccepted = true AND ps.effectivenessRating IS NULL " +
           "AND ps.acceptanceDate < :followUpDate " +
           "ORDER BY ps.acceptanceDate ASC")
    List<PersonalizedSuggestions> findSuggestionsNeedingFollowUp(@Param("followUpDate") LocalDateTime followUpDate);

    /**
     * Get monthly suggestion statistics
     */
    @Query("SELECT EXTRACT(YEAR FROM ps.createdAt), EXTRACT(MONTH FROM ps.createdAt), " +
           "COUNT(ps) as total, " +
           "SUM(CASE WHEN ps.isAccepted = true THEN 1 ELSE 0 END) as accepted, " +
           "AVG(ps.effectivenessRating) as avgEffectiveness " +
           "FROM PersonalizedSuggestions ps " +
           "WHERE ps.developerId = :developerId " +
           "GROUP BY EXTRACT(YEAR FROM ps.createdAt), EXTRACT(MONTH FROM ps.createdAt) " +
           "ORDER BY EXTRACT(YEAR FROM ps.createdAt), EXTRACT(MONTH FROM ps.createdAt)")
    List<Object[]> getMonthlySuggestionStats(@Param("developerId") Long developerId);

    /**
     * Find similar suggestions for pattern analysis
     */
    @Query("SELECT ps FROM PersonalizedSuggestions ps " +
           "WHERE ps.suggestionType = :suggestionType " +
           "AND ps.developerId != :excludeDeveloperId " +
           "AND ps.confidenceScore > :minConfidence " +
           "ORDER BY ps.confidenceScore DESC")
    List<PersonalizedSuggestions> findSimilarSuggestions(@Param("suggestionType") String suggestionType,
                                                         @Param("excludeDeveloperId") Long excludeDeveloperId,
                                                         @Param("minConfidence") BigDecimal minConfidence);

    /**
     * Get suggestion effectiveness trends
     */
    @Query("SELECT DATE(ps.acceptanceDate), AVG(ps.effectivenessRating) " +
           "FROM PersonalizedSuggestions ps " +
           "WHERE ps.developerId = :developerId " +
           "AND ps.isAccepted = true " +
           "AND ps.effectivenessRating IS NOT NULL " +
           "AND ps.acceptanceDate BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(ps.acceptanceDate) " +
           "ORDER BY DATE(ps.acceptanceDate)")
    List<Object[]> getSuggestionEffectivenessTrends(@Param("developerId") Long developerId,
                                                    @Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Find suggestions with specific context data
     */
    @Query(value = "SELECT * FROM personalized_suggestions ps " +
                   "WHERE ps.context_data ? :key", nativeQuery = true)
    List<PersonalizedSuggestions> findByContextDataKey(@Param("key") String key);

    /**
     * Find suggestions with specific context data value
     */
    @Query(value = "SELECT * FROM personalized_suggestions ps " +
                   "WHERE ps.context_data ->> :key = :value", nativeQuery = true)
    List<PersonalizedSuggestions> findByContextDataValue(@Param("key") String key, @Param("value") String value);

    /**
     * Count suggestions by developer
     */
    @Query("SELECT COUNT(ps) FROM PersonalizedSuggestions ps WHERE ps.developerId = :developerId")
    Long countByDeveloperId(@Param("developerId") Long developerId);

    /**
     * Count accepted suggestions by developer
     */
    @Query("SELECT COUNT(ps) FROM PersonalizedSuggestions ps WHERE ps.developerId = :developerId AND ps.isAccepted = true")
    Long countAcceptedByDeveloperId(@Param("developerId") Long developerId);

    /**
     * Get overall acceptance rate for a developer
     */
    @Query("SELECT " +
           "COUNT(ps) as total, " +
           "SUM(CASE WHEN ps.isAccepted = true THEN 1 ELSE 0 END) as accepted, " +
           "SUM(CASE WHEN ps.isAccepted = false THEN 1 ELSE 0 END) as rejected " +
           "FROM PersonalizedSuggestions ps " +
           "WHERE ps.developerId = :developerId")
    Object[] getAcceptanceStats(@Param("developerId") Long developerId);

    /**
     * Find top performing suggestions across all developers
     */
    @Query("SELECT ps FROM PersonalizedSuggestions ps " +
           "WHERE ps.isAccepted = true AND ps.effectivenessRating >= 4 " +
           "ORDER BY ps.effectivenessRating DESC, ps.confidenceScore DESC")
    List<PersonalizedSuggestions> findTopPerformingSuggestions(Pageable pageable);

    /**
     * Find most recent suggestions across all developers
     */
    List<PersonalizedSuggestions> findTop20ByOrderByCreatedAtDesc();
}