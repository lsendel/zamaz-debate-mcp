package com.zamaz.mcp.github.service;

import com.zamaz.mcp.github.entity.DeveloperProfile;
import com.zamaz.mcp.github.entity.DeveloperSkillAssessment;
import com.zamaz.mcp.github.entity.PRHistoricalMetrics;
import com.zamaz.mcp.github.entity.PullRequestReview;
import com.zamaz.mcp.github.entity.ReviewIssue;
import com.zamaz.mcp.github.repository.DeveloperProfileRepository;
import com.zamaz.mcp.github.repository.DeveloperSkillAssessmentRepository;
import com.zamaz.mcp.github.repository.PRHistoricalMetricsRepository;
import com.zamaz.mcp.github.repository.PullRequestReviewRepository;
import com.zamaz.mcp.github.repository.ReviewIssueRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for tracking developer learning progress and skill development
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeveloperLearningProgressService {

    private final DeveloperProfileRepository developerProfileRepository;
    private final DeveloperSkillAssessmentRepository skillAssessmentRepository;
    private final PRHistoricalMetricsRepository prHistoricalMetricsRepository;
    private final PullRequestReviewRepository pullRequestReviewRepository;
    private final ReviewIssueRepository reviewIssueRepository;

    /**
     * Assess and update developer skills based on recent PR activity
     */
    @Transactional
    public void assessDeveloperSkills(Long developerId) {
        log.info("Assessing skills for developer {}", developerId);

        Optional<DeveloperProfile> profileOpt = developerProfileRepository.findByGithubUserId(developerId);
        if (profileOpt.isEmpty()) {
            log.warn("Developer profile not found for ID: {}", developerId);
            return;
        }

        DeveloperProfile profile = profileOpt.get();
        
        // Get recent activity (last 3 months)
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        List<PRHistoricalMetrics> recentPRs = prHistoricalMetricsRepository
            .findByPrAuthorIdAndCreatedAtAfter(developerId, threeMonthsAgo);
        
        List<PullRequestReview> recentReviews = pullRequestReviewRepository
            .findByPrAuthorAndCompletedAtAfter(profile.getGithubUsername(), threeMonthsAgo);

        // Assess core technical skills
        assessCoreSkills(profile, recentPRs, recentReviews);
        
        // Assess language-specific skills
        assessLanguageSkills(profile, recentPRs, recentReviews);
        
        // Assess domain-specific skills
        assessDomainSkills(profile, recentPRs, recentReviews);
        
        // Assess collaboration skills
        assessCollaborationSkills(profile, recentPRs, recentReviews);
        
        // Update learning recommendations
        updateLearningRecommendations(profile);
        
        log.info("Completed skill assessment for developer {}", developerId);
    }

    /**
     * Get comprehensive learning progress report for a developer
     */
    @Transactional(readOnly = true)
    public DeveloperLearningReport getLearningReport(Long developerId, int months) {
        log.info("Generating learning report for developer {} over {} months", developerId, months);

        Optional<DeveloperProfile> profileOpt = developerProfileRepository.findByGithubUserId(developerId);
        if (profileOpt.isEmpty()) {
            throw new RuntimeException("Developer profile not found for ID: " + developerId);
        }

        DeveloperProfile profile = profileOpt.get();
        LocalDateTime fromDate = LocalDateTime.now().minusMonths(months);
        
        List<DeveloperSkillAssessment> skills = skillAssessmentRepository.findByDeveloperId(profile.getId());
        List<PRHistoricalMetrics> recentPRs = prHistoricalMetricsRepository
            .findByPrAuthorIdAndCreatedAtAfter(developerId, fromDate);
        
        return DeveloperLearningReport.builder()
            .developerId(developerId)
            .developerProfile(profile)
            .reportPeriod(months)
            .skillAssessments(skills)
            .skillProgression(calculateSkillProgression(skills, fromDate))
            .learningVelocity(calculateLearningVelocity(skills, recentPRs))
            .strengthAreas(identifyStrengthAreas(skills))
            .improvementAreas(identifyImprovementAreas(skills))
            .recommendedLearningPath(generateLearningPath(profile, skills))
            .mentorshipNeeds(identifyMentorshipNeeds(skills))
            .achievementMilestones(identifyAchievements(skills, recentPRs))
            .nextGoals(generateNextGoals(skills))
            .build();
    }

    /**
     * Track skill demonstration events (when a developer shows competency)
     */
    @Transactional
    public void recordSkillDemonstration(Long developerId, String skillCategory, 
                                       SkillDemonstrationEvent event) {
        log.info("Recording skill demonstration for developer {} in category {}", developerId, skillCategory);

        Optional<DeveloperProfile> profileOpt = developerProfileRepository.findByGithubUserId(developerId);
        if (profileOpt.isEmpty()) {
            log.warn("Developer profile not found for ID: {}", developerId);
            return;
        }

        DeveloperProfile profile = profileOpt.get();
        
        // Find or create skill assessment
        DeveloperSkillAssessment assessment = skillAssessmentRepository
            .findByDeveloperIdAndSkillCategory(profile.getId(), skillCategory)
            .orElseGet(() -> createNewSkillAssessment(profile.getId(), skillCategory));

        // Update skill level based on demonstration
        updateSkillLevelFromDemonstration(assessment, event);
        
        // Update confidence score
        updateConfidenceScore(assessment, event);
        
        // Update evidence count
        assessment.setEvidenceCount(assessment.getEvidenceCount() + 1);
        assessment.setLastDemonstrationDate(LocalDateTime.now());
        
        skillAssessmentRepository.save(assessment);
        
        log.info("Recorded skill demonstration for developer {} in category {}", developerId, skillCategory);
    }

    /**
     * Generate personalized learning recommendations
     */
    @Transactional(readOnly = true)
    public List<LearningRecommendation> generateLearningRecommendations(Long developerId) {
        log.info("Generating learning recommendations for developer {}", developerId);

        Optional<DeveloperProfile> profileOpt = developerProfileRepository.findByGithubUserId(developerId);
        if (profileOpt.isEmpty()) {
            return Collections.emptyList();
        }

        DeveloperProfile profile = profileOpt.get();
        List<DeveloperSkillAssessment> skills = skillAssessmentRepository.findByDeveloperId(profile.getId());
        List<PRHistoricalMetrics> recentPRs = prHistoricalMetricsRepository
            .findByPrAuthorIdAndCreatedAtAfter(developerId, LocalDateTime.now().minusMonths(3));

        List<LearningRecommendation> recommendations = new ArrayList<>();
        
        // Add skill-based recommendations
        recommendations.addAll(generateSkillBasedRecommendations(skills, profile));
        
        // Add performance-based recommendations
        recommendations.addAll(generatePerformanceBasedRecommendations(recentPRs, profile));
        
        // Add career-level recommendations
        recommendations.addAll(generateCareerLevelRecommendations(profile, skills));
        
        // Add peer-learning recommendations
        recommendations.addAll(generatePeerLearningRecommendations(profile, skills));
        
        // Sort by priority and relevance
        return recommendations.stream()
            .sorted(Comparator.comparing(LearningRecommendation::getPriority).reversed()
                    .thenComparing(LearningRecommendation::getRelevanceScore).reversed())
            .limit(10)
            .collect(Collectors.toList());
    }

    /**
     * Calculate skill growth rate over time
     */
    @Transactional(readOnly = true)
    public SkillGrowthAnalysis calculateSkillGrowth(Long developerId, int months) {
        log.info("Calculating skill growth for developer {} over {} months", developerId, months);

        Optional<DeveloperProfile> profileOpt = developerProfileRepository.findByGithubUserId(developerId);
        if (profileOpt.isEmpty()) {
            throw new RuntimeException("Developer profile not found for ID: " + developerId);
        }

        DeveloperProfile profile = profileOpt.get();
        LocalDateTime fromDate = LocalDateTime.now().minusMonths(months);
        
        List<DeveloperSkillAssessment> skills = skillAssessmentRepository.findByDeveloperId(profile.getId());
        List<PRHistoricalMetrics> historicalPRs = prHistoricalMetricsRepository
            .findByPrAuthorIdAndCreatedAtAfter(developerId, fromDate);

        // Calculate growth metrics
        Map<String, SkillGrowthMetric> skillGrowthMetrics = calculateGrowthMetrics(skills, historicalPRs, fromDate);
        
        return SkillGrowthAnalysis.builder()
            .developerId(developerId)
            .analysisDateRange(fromDate)
            .overallGrowthRate(calculateOverallGrowthRate(skillGrowthMetrics))
            .skillGrowthMetrics(skillGrowthMetrics)
            .fastestGrowingSkills(identifyFastestGrowingSkills(skillGrowthMetrics))
            .stagnatingSkills(identifyStagnatingSkills(skillGrowthMetrics))
            .growthPredictions(predictFutureGrowth(skillGrowthMetrics))
            .recommendedActions(generateGrowthActions(skillGrowthMetrics))
            .build();
    }

    /**
     * Scheduled task to update all developer skill assessments
     */
    @Scheduled(cron = "0 0 2 * * ?") // Run daily at 2 AM
    @Async
    public void updateAllDeveloperSkills() {
        log.info("Starting scheduled skill assessment update for all developers");

        List<DeveloperProfile> profiles = developerProfileRepository.findAll();
        
        for (DeveloperProfile profile : profiles) {
            try {
                assessDeveloperSkills(profile.getGithubUserId());
            } catch (Exception e) {
                log.error("Error updating skills for developer {}: {}", 
                         profile.getGithubUserId(), e.getMessage(), e);
            }
        }
        
        log.info("Completed scheduled skill assessment update for {} developers", profiles.size());
    }

    // Private helper methods

    private void assessCoreSkills(DeveloperProfile profile, List<PRHistoricalMetrics> recentPRs, 
                                 List<PullRequestReview> recentReviews) {
        // Assess coding quality
        assessSkill(profile, "coding_quality", calculateCodingQualityScore(recentPRs));
        
        // Assess testing skills
        assessSkill(profile, "testing", calculateTestingScore(recentPRs, recentReviews));
        
        // Assess code review skills
        assessSkill(profile, "code_review", calculateCodeReviewScore(recentReviews));
        
        // Assess documentation skills
        assessSkill(profile, "documentation", calculateDocumentationScore(recentPRs));
    }

    private void assessLanguageSkills(DeveloperProfile profile, List<PRHistoricalMetrics> recentPRs, 
                                    List<PullRequestReview> recentReviews) {
        // This would analyze file types in PRs to assess language proficiency
        Map<String, Integer> languageUsage = analyzeLanguageUsage(recentPRs);
        
        for (Map.Entry<String, Integer> entry : languageUsage.entrySet()) {
            String language = entry.getKey();
            Integer usageCount = entry.getValue();
            
            BigDecimal score = calculateLanguageScore(language, usageCount, recentPRs);
            assessSkill(profile, language, score);
        }
    }

    private void assessDomainSkills(DeveloperProfile profile, List<PRHistoricalMetrics> recentPRs, 
                                   List<PullRequestReview> recentReviews) {
        // Assess domain-specific skills based on PR content analysis
        if (profile.getDomainExpertise() != null) {
            for (String domain : profile.getDomainExpertise()) {
                BigDecimal score = calculateDomainScore(domain, recentPRs, recentReviews);
                assessSkill(profile, domain, score);
            }
        }
    }

    private void assessCollaborationSkills(DeveloperProfile profile, List<PRHistoricalMetrics> recentPRs, 
                                         List<PullRequestReview> recentReviews) {
        // Assess collaboration based on PR interaction patterns
        BigDecimal collaborationScore = calculateCollaborationScore(recentPRs, recentReviews);
        assessSkill(profile, "collaboration", collaborationScore);
        
        // Assess communication skills
        BigDecimal communicationScore = calculateCommunicationScore(recentReviews);
        assessSkill(profile, "communication", communicationScore);
    }

    private void assessSkill(DeveloperProfile profile, String skillCategory, BigDecimal score) {
        DeveloperSkillAssessment assessment = skillAssessmentRepository
            .findByDeveloperIdAndSkillCategory(profile.getId(), skillCategory)
            .orElseGet(() -> createNewSkillAssessment(profile.getId(), skillCategory));

        // Update skill level based on score
        DeveloperSkillAssessment.SkillLevel newLevel = determineSkillLevel(score);
        
        // Track improvement trend
        DeveloperSkillAssessment.ImprovementTrend trend = calculateImprovementTrend(assessment, newLevel);
        
        assessment.setSkillLevel(newLevel);
        assessment.setConfidenceScore(score);
        assessment.setImprovementTrend(trend);
        assessment.setLastDemonstrationDate(LocalDateTime.now());
        
        skillAssessmentRepository.save(assessment);
    }

    private DeveloperSkillAssessment createNewSkillAssessment(Long developerId, String skillCategory) {
        return DeveloperSkillAssessment.builder()
            .developerId(developerId)
            .skillCategory(skillCategory)
            .skillLevel(DeveloperSkillAssessment.SkillLevel.COMPETENT)
            .confidenceScore(BigDecimal.valueOf(50))
            .evidenceCount(0)
            .improvementTrend(DeveloperSkillAssessment.ImprovementTrend.STABLE)
            .learningGoals(new ArrayList<>())
            .recommendedResources(new ArrayList<>())
            .build();
    }

    // Additional helper methods would be implemented here for all the complex calculations

    // Data classes for the service
    @Data
    @Builder
    public static class DeveloperLearningReport {
        private Long developerId;
        private DeveloperProfile developerProfile;
        private Integer reportPeriod;
        private List<DeveloperSkillAssessment> skillAssessments;
        private Map<String, SkillProgressionMetric> skillProgression;
        private BigDecimal learningVelocity;
        private List<String> strengthAreas;
        private List<String> improvementAreas;
        private List<LearningPathItem> recommendedLearningPath;
        private List<String> mentorshipNeeds;
        private List<AchievementMilestone> achievementMilestones;
        private List<LearningGoal> nextGoals;
    }

    @Data
    @Builder
    public static class SkillDemonstrationEvent {
        private String context;
        private String evidence;
        private BigDecimal performanceScore;
        private LocalDateTime timestamp;
        private String sourceType; // pr, review, commit, etc.
        private Long sourceId;
    }

    @Data
    @Builder
    public static class LearningRecommendation {
        private String title;
        private String description;
        private String skillCategory;
        private Integer priority;
        private BigDecimal relevanceScore;
        private String resourceType; // course, book, practice, mentorship
        private List<String> resources;
        private String estimatedTimeToComplete;
        private List<String> prerequisites;
    }

    @Data
    @Builder
    public static class SkillGrowthAnalysis {
        private Long developerId;
        private LocalDateTime analysisDateRange;
        private BigDecimal overallGrowthRate;
        private Map<String, SkillGrowthMetric> skillGrowthMetrics;
        private List<String> fastestGrowingSkills;
        private List<String> stagnatingSkills;
        private Map<String, GrowthPrediction> growthPredictions;
        private List<String> recommendedActions;
    }

    @Data
    @Builder
    public static class SkillGrowthMetric {
        private String skillCategory;
        private BigDecimal growthRate;
        private BigDecimal currentLevel;
        private BigDecimal startingLevel;
        private Integer timeToGrowth;
        private List<String> growthDrivers;
    }

    @Data
    @Builder
    public static class SkillProgressionMetric {
        private String skillCategory;
        private DeveloperSkillAssessment.SkillLevel previousLevel;
        private DeveloperSkillAssessment.SkillLevel currentLevel;
        private BigDecimal progressPercentage;
        private LocalDateTime lastImprovement;
    }

    @Data
    @Builder
    public static class LearningPathItem {
        private String title;
        private String description;
        private Integer sequenceOrder;
        private String estimatedDuration;
        private List<String> resources;
        private String completionCriteria;
    }

    @Data
    @Builder
    public static class AchievementMilestone {
        private String title;
        private String description;
        private LocalDateTime achievedDate;
        private String skillCategory;
        private String evidenceType;
    }

    @Data
    @Builder
    public static class LearningGoal {
        private String title;
        private String description;
        private String skillCategory;
        private LocalDateTime targetDate;
        private List<String> actionItems;
        private String successCriteria;
    }

    @Data
    @Builder
    public static class GrowthPrediction {
        private String skillCategory;
        private DeveloperSkillAssessment.SkillLevel predictedLevel;
        private LocalDateTime predictedDate;
        private BigDecimal confidence;
        private List<String> assumptions;
    }
}