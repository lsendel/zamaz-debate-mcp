package com.zamaz.mcp.github.service;

import com.zamaz.mcp.github.entity.*;
import com.zamaz.mcp.github.repository.*;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Personalized suggestion engine with ML pattern recognition
 * Provides intelligent, context-aware recommendations based on individual developer patterns
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalizedSuggestionEngine {

    private final PersonalizedSuggestionsRepository personalizedSuggestionsRepository;
    private final DeveloperProfileRepository developerProfileRepository;
    private final DeveloperSkillAssessmentRepository skillAssessmentRepository;
    private final PRHistoricalMetricsRepository prHistoricalMetricsRepository;
    private final KnowledgeBaseEntryRepository knowledgeBaseRepository;
    private final MLTrainingDataRepository mlTrainingDataRepository;
    private final PatternRecognitionService patternRecognitionService;

    /**
     * Generate personalized suggestions for a developer
     */
    @Transactional
    public List<PersonalizedSuggestion> generateSuggestions(Long developerId, SuggestionContext context) {
        log.info("Generating personalized suggestions for developer {} with context {}", developerId, context);

        Optional<DeveloperProfile> profileOpt = developerProfileRepository.findByGithubUserId(developerId);
        if (profileOpt.isEmpty()) {
            log.warn("Developer profile not found for ID: {}", developerId);
            return Collections.emptyList();
        }

        DeveloperProfile profile = profileOpt.get();
        List<DeveloperSkillAssessment> skills = skillAssessmentRepository.findByDeveloperId(profile.getId());
        List<PRHistoricalMetrics> recentPRs = prHistoricalMetricsRepository
            .findByPrAuthorIdAndCreatedAtAfter(developerId, LocalDateTime.now().minusMonths(3));

        List<PersonalizedSuggestion> suggestions = new ArrayList<>();

        // Generate skill-based suggestions
        suggestions.addAll(generateSkillBasedSuggestions(profile, skills, context));

        // Generate performance-based suggestions
        suggestions.addAll(generatePerformanceBasedSuggestions(profile, recentPRs, context));

        // Generate pattern-based suggestions using ML
        suggestions.addAll(generatePatternBasedSuggestions(profile, skills, recentPRs, context));

        // Generate collaborative suggestions
        suggestions.addAll(generateCollaborativeSuggestions(profile, context));

        // Generate learning path suggestions
        suggestions.addAll(generateLearningPathSuggestions(profile, skills, context));

        // Rank and filter suggestions
        List<PersonalizedSuggestion> rankedSuggestions = rankAndFilterSuggestions(suggestions, profile, context);

        // Store suggestions for tracking
        storeSuggestions(rankedSuggestions, profile.getId());

        log.info("Generated {} personalized suggestions for developer {}", rankedSuggestions.size(), developerId);
        return rankedSuggestions;
    }

    /**
     * Get suggestion effectiveness analytics
     */
    @Transactional(readOnly = true)
    public SuggestionEffectivenessAnalytics getSuggestionAnalytics(Long developerId, int months) {
        log.info("Generating suggestion analytics for developer {} over {} months", developerId, months);

        Optional<DeveloperProfile> profileOpt = developerProfileRepository.findByGithubUserId(developerId);
        if (profileOpt.isEmpty()) {
            throw new RuntimeException("Developer profile not found for ID: " + developerId);
        }

        DeveloperProfile profile = profileOpt.get();
        LocalDateTime fromDate = LocalDateTime.now().minusMonths(months);
        
        List<PersonalizedSuggestions> suggestions = personalizedSuggestionsRepository
            .findByDeveloperIdAndCreatedAtAfter(profile.getId(), fromDate);

        return SuggestionEffectivenessAnalytics.builder()
            .developerId(developerId)
            .analysisDateRange(fromDate)
            .totalSuggestions(suggestions.size())
            .acceptedSuggestions(getAcceptedSuggestions(suggestions))
            .rejectedSuggestions(getRejectedSuggestions(suggestions))
            .pendingSuggestions(getPendingSuggestions(suggestions))
            .averageEffectivenessRating(calculateAverageEffectiveness(suggestions))
            .suggestionTypeDistribution(getSuggestionTypeDistribution(suggestions))
            .acceptanceRateByType(getAcceptanceRateByType(suggestions))
            .improvementCorrelation(calculateImprovementCorrelation(suggestions, profile))
            .recommendationAccuracy(calculateRecommendationAccuracy(suggestions))
            .build();
    }

    /**
     * Learn from suggestion feedback to improve future recommendations
     */
    @Transactional
    public void processSuggestionFeedback(Long suggestionId, SuggestionFeedback feedback) {
        log.info("Processing suggestion feedback for suggestion {}", suggestionId);

        Optional<PersonalizedSuggestions> suggestionOpt = personalizedSuggestionsRepository.findById(suggestionId);
        if (suggestionOpt.isEmpty()) {
            log.warn("Suggestion not found for ID: {}", suggestionId);
            return;
        }

        PersonalizedSuggestions suggestion = suggestionOpt.get();
        
        // Update suggestion with feedback
        suggestion.setIsAccepted(feedback.isAccepted());
        suggestion.setAcceptanceDate(feedback.isAccepted() ? LocalDateTime.now() : null);
        suggestion.setEffectivenessRating(feedback.getEffectivenessRating());
        suggestion.setFeedbackText(feedback.getFeedbackText());
        
        personalizedSuggestionsRepository.save(suggestion);

        // Create ML training data from feedback
        createMLTrainingDataFromFeedback(suggestion, feedback);

        // Update suggestion patterns based on feedback
        updateSuggestionPatterns(suggestion, feedback);

        log.info("Processed suggestion feedback for suggestion {}", suggestionId);
    }

    /**
     * Get real-time contextual suggestions based on current activity
     */
    @Transactional(readOnly = true)
    public List<ContextualSuggestion> getContextualSuggestions(Long developerId, 
                                                              CodeContext codeContext) {
        log.info("Generating contextual suggestions for developer {} in context {}", developerId, codeContext);

        Optional<DeveloperProfile> profileOpt = developerProfileRepository.findByGithubUserId(developerId);
        if (profileOpt.isEmpty()) {
            return Collections.emptyList();
        }

        DeveloperProfile profile = profileOpt.get();
        List<ContextualSuggestion> suggestions = new ArrayList<>();

        // Analyze current code context
        CodeAnalysisResult analysis = analyzeCodeContext(codeContext);

        // Generate suggestions based on detected patterns
        suggestions.addAll(generatePatternBasedContextualSuggestions(profile, analysis));

        // Generate suggestions based on historical issues
        suggestions.addAll(generateHistoricalContextualSuggestions(profile, analysis));

        // Generate suggestions based on best practices
        suggestions.addAll(generateBestPracticeContextualSuggestions(profile, analysis));

        // Generate suggestions based on similar developers
        suggestions.addAll(generateCollaborativeContextualSuggestions(profile, analysis));

        // Rank by relevance and confidence
        return suggestions.stream()
            .sorted(Comparator.comparing(ContextualSuggestion::getRelevanceScore).reversed())
            .limit(5)
            .collect(Collectors.toList());
    }

    /**
     * Update suggestion models based on developer progress
     */
    @Transactional
    public void updateSuggestionModels(Long developerId) {
        log.info("Updating suggestion models for developer {}", developerId);

        Optional<DeveloperProfile> profileOpt = developerProfileRepository.findByGithubUserId(developerId);
        if (profileOpt.isEmpty()) {
            log.warn("Developer profile not found for ID: {}", developerId);
            return;
        }

        DeveloperProfile profile = profileOpt.get();
        
        // Update skill-based models
        updateSkillBasedModels(profile);

        // Update performance-based models
        updatePerformanceBasedModels(profile);

        // Update pattern recognition models
        updatePatternRecognitionModels(profile);

        // Update collaborative filtering models
        updateCollaborativeModels(profile);

        log.info("Updated suggestion models for developer {}", developerId);
    }

    // Private helper methods

    private List<PersonalizedSuggestion> generateSkillBasedSuggestions(DeveloperProfile profile,
                                                                      List<DeveloperSkillAssessment> skills,
                                                                      SuggestionContext context) {
        List<PersonalizedSuggestion> suggestions = new ArrayList<>();

        // Find skills that need improvement
        List<DeveloperSkillAssessment> improvementSkills = skills.stream()
            .filter(skill -> skill.getImprovementTrend() == DeveloperSkillAssessment.ImprovementTrend.DECLINING ||
                           skill.getConfidenceScore().compareTo(BigDecimal.valueOf(60)) < 0)
            .collect(Collectors.toList());

        for (DeveloperSkillAssessment skill : improvementSkills) {
            suggestions.add(PersonalizedSuggestion.builder()
                .suggestionType(SuggestionType.SKILL_IMPROVEMENT)
                .title(String.format("Improve %s skills", skill.getSkillCategory()))
                .content(generateSkillImprovementContent(skill))
                .confidenceScore(calculateSkillSuggestionConfidence(skill))
                .priorityLevel(determinePriorityLevel(skill))
                .triggerConditions(Arrays.asList("skill_decline", "low_confidence"))
                .expectedBenefit(calculateExpectedBenefit(skill))
                .estimatedTimeToComplete("2-4 weeks")
                .build());
        }

        // Find skills ready for advancement
        List<DeveloperSkillAssessment> advancementSkills = skills.stream()
            .filter(skill -> skill.getImprovementTrend() == DeveloperSkillAssessment.ImprovementTrend.IMPROVING &&
                           skill.getConfidenceScore().compareTo(BigDecimal.valueOf(75)) > 0)
            .collect(Collectors.toList());

        for (DeveloperSkillAssessment skill : advancementSkills) {
            suggestions.add(PersonalizedSuggestion.builder()
                .suggestionType(SuggestionType.SKILL_ADVANCEMENT)
                .title(String.format("Advance %s to next level", skill.getSkillCategory()))
                .content(generateSkillAdvancementContent(skill))
                .confidenceScore(calculateSkillSuggestionConfidence(skill))
                .priorityLevel(PriorityLevel.MEDIUM)
                .triggerConditions(Arrays.asList("skill_improvement", "high_confidence"))
                .expectedBenefit(calculateExpectedBenefit(skill))
                .estimatedTimeToComplete("1-2 weeks")
                .build());
        }

        return suggestions;
    }

    private List<PersonalizedSuggestion> generatePerformanceBasedSuggestions(DeveloperProfile profile,
                                                                           List<PRHistoricalMetrics> recentPRs,
                                                                           SuggestionContext context) {
        List<PersonalizedSuggestion> suggestions = new ArrayList<>();

        if (recentPRs.isEmpty()) {
            return suggestions;
        }

        // Analyze performance trends
        PerformanceAnalysis analysis = analyzePerformance(recentPRs);

        // Generate suggestions based on performance issues
        if (analysis.getAverageComplexity().compareTo(BigDecimal.valueOf(70)) > 0) {
            suggestions.add(PersonalizedSuggestion.builder()
                .suggestionType(SuggestionType.CODE_COMPLEXITY)
                .title("Reduce code complexity")
                .content("Your recent PRs show high complexity. Consider breaking down large methods and improving code structure.")
                .confidenceScore(BigDecimal.valueOf(85))
                .priorityLevel(PriorityLevel.HIGH)
                .triggerConditions(Arrays.asList("high_complexity"))
                .expectedBenefit("Improved code maintainability and reduced bugs")
                .estimatedTimeToComplete("1-2 weeks")
                .build());
        }

        if (analysis.getAverageQualityScore().compareTo(BigDecimal.valueOf(60)) < 0) {
            suggestions.add(PersonalizedSuggestion.builder()
                .suggestionType(SuggestionType.CODE_QUALITY)
                .title("Improve code quality")
                .content("Focus on adding tests, improving documentation, and following coding standards.")
                .confidenceScore(BigDecimal.valueOf(80))
                .priorityLevel(PriorityLevel.HIGH)
                .triggerConditions(Arrays.asList("low_quality"))
                .expectedBenefit("Better code reliability and team collaboration")
                .estimatedTimeToComplete("2-3 weeks")
                .build());
        }

        if (analysis.getAverageReviewTurnaround() > 48) {
            suggestions.add(PersonalizedSuggestion.builder()
                .suggestionType(SuggestionType.REVIEW_PROCESS)
                .title("Improve PR review process")
                .content("Your PRs take longer to review. Consider smaller PRs with better descriptions.")
                .confidenceScore(BigDecimal.valueOf(75))
                .priorityLevel(PriorityLevel.MEDIUM)
                .triggerConditions(Arrays.asList("slow_review"))
                .expectedBenefit("Faster development cycle and better team collaboration")
                .estimatedTimeToComplete("1 week")
                .build());
        }

        return suggestions;
    }

    private List<PersonalizedSuggestion> generatePatternBasedSuggestions(DeveloperProfile profile,
                                                                        List<DeveloperSkillAssessment> skills,
                                                                        List<PRHistoricalMetrics> recentPRs,
                                                                        SuggestionContext context) {
        List<PersonalizedSuggestion> suggestions = new ArrayList<>();

        // Use ML patterns to generate suggestions
        List<MLPattern> patterns = patternRecognitionService.analyzePatterns(profile, skills, recentPRs);

        for (MLPattern pattern : patterns) {
            if (pattern.getConfidence().compareTo(BigDecimal.valueOf(70)) > 0) {
                suggestions.add(PersonalizedSuggestion.builder()
                    .suggestionType(SuggestionType.PATTERN_BASED)
                    .title(pattern.getTitle())
                    .content(pattern.getRecommendation())
                    .confidenceScore(pattern.getConfidence())
                    .priorityLevel(determinePriorityFromPattern(pattern))
                    .triggerConditions(Arrays.asList("ml_pattern"))
                    .expectedBenefit(pattern.getExpectedBenefit())
                    .estimatedTimeToComplete(pattern.getEstimatedTime())
                    .build());
            }
        }

        return suggestions;
    }

    private List<PersonalizedSuggestion> generateCollaborativeSuggestions(DeveloperProfile profile,
                                                                         SuggestionContext context) {
        List<PersonalizedSuggestion> suggestions = new ArrayList<>();

        // Find similar developers
        List<DeveloperProfile> similarDevelopers = findSimilarDevelopers(profile);

        for (DeveloperProfile similarDev : similarDevelopers) {
            // Get suggestions that worked well for similar developers
            List<PersonalizedSuggestions> effectiveSuggestions = personalizedSuggestionsRepository
                .findByDeveloperIdAndIsAcceptedAndEffectivenessRatingGreaterThan(
                    similarDev.getId(), true, 4);

            for (PersonalizedSuggestions suggestion : effectiveSuggestions) {
                if (isApplicableToProfile(suggestion, profile)) {
                    suggestions.add(adaptSuggestionForProfile(suggestion, profile));
                }
            }
        }

        return suggestions;
    }

    private List<PersonalizedSuggestion> generateLearningPathSuggestions(DeveloperProfile profile,
                                                                        List<DeveloperSkillAssessment> skills,
                                                                        SuggestionContext context) {
        List<PersonalizedSuggestion> suggestions = new ArrayList<>();

        // Generate learning path based on career goals and current skills
        LearningPath learningPath = generateLearningPath(profile, skills);

        for (LearningPathItem item : learningPath.getItems()) {
            suggestions.add(PersonalizedSuggestion.builder()
                .suggestionType(SuggestionType.LEARNING_PATH)
                .title(item.getTitle())
                .content(item.getDescription())
                .confidenceScore(item.getConfidence())
                .priorityLevel(item.getPriority())
                .triggerConditions(Arrays.asList("learning_path"))
                .expectedBenefit(item.getExpectedBenefit())
                .estimatedTimeToComplete(item.getEstimatedTime())
                .build());
        }

        return suggestions;
    }

    // Additional helper methods would be implemented here...

    // Data classes for the service
    @Data
    @Builder
    public static class SuggestionContext {
        private String repositoryName;
        private String currentTask;
        private List<String> technologies;
        private String urgency;
        private Map<String, Object> contextData;
    }

    @Data
    @Builder
    public static class PersonalizedSuggestion {
        private SuggestionType suggestionType;
        private String title;
        private String content;
        private BigDecimal confidenceScore;
        private PriorityLevel priorityLevel;
        private List<String> triggerConditions;
        private String expectedBenefit;
        private String estimatedTimeToComplete;
    }

    @Data
    @Builder
    public static class SuggestionFeedback {
        private boolean accepted;
        private Integer effectivenessRating;
        private String feedbackText;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    public static class ContextualSuggestion {
        private String title;
        private String description;
        private BigDecimal relevanceScore;
        private SuggestionType type;
        private String actionText;
        private Map<String, Object> contextData;
    }

    @Data
    @Builder
    public static class CodeContext {
        private String fileName;
        private String language;
        private String framework;
        private String codeSnippet;
        private List<String> dependencies;
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    public static class SuggestionEffectivenessAnalytics {
        private Long developerId;
        private LocalDateTime analysisDateRange;
        private Integer totalSuggestions;
        private Integer acceptedSuggestions;
        private Integer rejectedSuggestions;
        private Integer pendingSuggestions;
        private BigDecimal averageEffectivenessRating;
        private Map<String, Integer> suggestionTypeDistribution;
        private Map<String, BigDecimal> acceptanceRateByType;
        private BigDecimal improvementCorrelation;
        private BigDecimal recommendationAccuracy;
    }

    public enum SuggestionType {
        SKILL_IMPROVEMENT,
        SKILL_ADVANCEMENT,
        CODE_COMPLEXITY,
        CODE_QUALITY,
        REVIEW_PROCESS,
        PATTERN_BASED,
        LEARNING_PATH,
        COLLABORATION,
        BEST_PRACTICE
    }

    public enum PriorityLevel {
        HIGH,
        MEDIUM,
        LOW
    }

    // Additional data classes would be implemented here...
}