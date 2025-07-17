package com.zamaz.mcp.github.service;

import com.zamaz.mcp.github.entity.PRHistoricalMetrics;
import com.zamaz.mcp.github.entity.DeveloperProfile;
import com.zamaz.mcp.github.entity.PullRequestReview;
import com.zamaz.mcp.github.repository.PRHistoricalMetricsRepository;
import com.zamaz.mcp.github.repository.DeveloperProfileRepository;
import com.zamaz.mcp.github.repository.PullRequestReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for analyzing PR history and generating comprehensive metrics and patterns
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PRHistoryAnalysisService {

    private final PRHistoricalMetricsRepository prHistoricalMetricsRepository;
    private final DeveloperProfileRepository developerProfileRepository;
    private final PullRequestReviewRepository pullRequestReviewRepository;
    private final GitHubApiClient gitHubApiClient;

    /**
     * Analyze and store historical metrics for a PR
     */
    @Transactional
    public PRHistoricalMetrics analyzePRMetrics(Long repositoryId, Integer prNumber, String accessToken) {
        log.info("Analyzing PR metrics for repository {} PR #{}", repositoryId, prNumber);

        try {
            // Get PR details from GitHub API
            var pullRequest = gitHubApiClient.getPullRequest(accessToken, getOwnerFromRepoId(repositoryId), 
                                                           getRepoNameFromRepoId(repositoryId), prNumber);
            
            // Get existing review data
            Optional<PullRequestReview> reviewOpt = pullRequestReviewRepository
                .findByRepositoryIdAndPrNumber(repositoryId, prNumber);
            
            // Build metrics
            PRHistoricalMetrics.PRHistoricalMetricsBuilder metricsBuilder = PRHistoricalMetrics.builder()
                .repositoryId(repositoryId)
                .prNumber(prNumber)
                .prAuthorId(pullRequest.getUser().getId())
                .prSize(calculatePRSize(pullRequest))
                .complexityScore(calculateComplexityScore(pullRequest))
                .testCoverageChange(calculateTestCoverageChange(pullRequest))
                .codeQualityScore(calculateCodeQualityScore(pullRequest))
                .reviewTurnaroundHours(calculateReviewTurnaroundHours(pullRequest))
                .mergeTimeHours(calculateMergeTimeHours(pullRequest))
                .commentCount(pullRequest.getComments())
                .approvalCount(getApprovalCount(pullRequest))
                .changeRequestCount(getChangeRequestCount(pullRequest))
                .filesChanged(pullRequest.getChangedFiles())
                .linesAdded(pullRequest.getAdditions())
                .linesDeleted(pullRequest.getDeletions())
                .commitCount(pullRequest.getCommits())
                .isHotfix(isHotfix(pullRequest))
                .isFeature(isFeature(pullRequest))
                .isRefactor(isRefactor(pullRequest))
                .isBugfix(isBugfix(pullRequest))
                .mergeConflicts(hasMergeConflicts(pullRequest))
                .ciFailures(getCIFailures(pullRequest));

            // Add review-specific metrics if available
            if (reviewOpt.isPresent()) {
                PullRequestReview review = reviewOpt.get();
                metricsBuilder
                    .complexityScore(enhanceComplexityScore(metricsBuilder.build().getComplexityScore(), review))
                    .codeQualityScore(enhanceCodeQualityScore(metricsBuilder.build().getCodeQualityScore(), review));
            }

            PRHistoricalMetrics metrics = metricsBuilder.build();
            
            // Ensure developer profile exists
            ensureDeveloperProfileExists(pullRequest.getUser().getId(), pullRequest.getUser().getLogin());
            
            // Save metrics
            metrics = prHistoricalMetricsRepository.save(metrics);
            
            log.info("Successfully analyzed PR metrics for repository {} PR #{}", repositoryId, prNumber);
            return metrics;
            
        } catch (Exception e) {
            log.error("Error analyzing PR metrics for repository {} PR #{}: {}", 
                      repositoryId, prNumber, e.getMessage(), e);
            throw new RuntimeException("Failed to analyze PR metrics", e);
        }
    }

    /**
     * Get comprehensive PR history analysis for a repository
     */
    @Transactional(readOnly = true)
    public PRHistoryAnalysisReport getRepositoryAnalysis(Long repositoryId, int months) {
        log.info("Generating PR history analysis for repository {} over {} months", repositoryId, months);

        LocalDateTime fromDate = LocalDateTime.now().minusMonths(months);
        List<PRHistoricalMetrics> metrics = prHistoricalMetricsRepository
            .findByRepositoryIdAndCreatedAtAfter(repositoryId, fromDate);

        return PRHistoryAnalysisReport.builder()
            .repositoryId(repositoryId)
            .analysisDateRange(fromDate)
            .totalPRs(metrics.size())
            .averageComplexity(calculateAverageComplexity(metrics))
            .averageCodeQuality(calculateAverageCodeQuality(metrics))
            .averageReviewTurnaround(calculateAverageReviewTurnaround(metrics))
            .averageMergeTime(calculateAverageMergeTime(metrics))
            .prSizeDistribution(calculatePRSizeDistribution(metrics))
            .topContributors(getTopContributors(metrics))
            .qualityTrends(calculateQualityTrends(metrics))
            .performanceMetrics(calculatePerformanceMetrics(metrics))
            .riskFactors(identifyRiskFactors(metrics))
            .recommendations(generateRecommendations(metrics))
            .build();
    }

    /**
     * Get developer-specific PR analysis
     */
    @Transactional(readOnly = true)
    public DeveloperPRAnalysis getDeveloperAnalysis(Long developerId, int months) {
        log.info("Generating developer PR analysis for developer {} over {} months", developerId, months);

        LocalDateTime fromDate = LocalDateTime.now().minusMonths(months);
        List<PRHistoricalMetrics> metrics = prHistoricalMetricsRepository
            .findByPrAuthorIdAndCreatedAtAfter(developerId, fromDate);

        Optional<DeveloperProfile> profileOpt = developerProfileRepository.findByGithubUserId(developerId);
        
        return DeveloperPRAnalysis.builder()
            .developerId(developerId)
            .developerProfile(profileOpt.orElse(null))
            .analysisDateRange(fromDate)
            .totalPRs(metrics.size())
            .averageComplexity(calculateAverageComplexity(metrics))
            .averageCodeQuality(calculateAverageCodeQuality(metrics))
            .averagePRSize(calculateAveragePRSize(metrics))
            .strengthAreas(identifyDeveloperStrengths(metrics))
            .improvementAreas(identifyDeveloperImprovements(metrics))
            .productivityTrends(calculateProductivityTrends(metrics))
            .collaborationMetrics(calculateCollaborationMetrics(metrics))
            .learningRecommendations(generateLearningRecommendations(metrics, profileOpt.orElse(null)))
            .build();
    }

    /**
     * Get comparative analysis between developers
     */
    @Transactional(readOnly = true)
    public TeamComparisonAnalysis getTeamComparison(Long repositoryId, int months) {
        log.info("Generating team comparison analysis for repository {} over {} months", repositoryId, months);

        LocalDateTime fromDate = LocalDateTime.now().minusMonths(months);
        List<PRHistoricalMetrics> metrics = prHistoricalMetricsRepository
            .findByRepositoryIdAndCreatedAtAfter(repositoryId, fromDate);

        Map<Long, List<PRHistoricalMetrics>> metricsByDeveloper = metrics.stream()
            .collect(Collectors.groupingBy(PRHistoricalMetrics::getPrAuthorId));

        List<DeveloperComparison> comparisons = metricsByDeveloper.entrySet().stream()
            .map(entry -> createDeveloperComparison(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(DeveloperComparison::getOverallScore).reversed())
            .collect(Collectors.toList());

        return TeamComparisonAnalysis.builder()
            .repositoryId(repositoryId)
            .analysisDateRange(fromDate)
            .developerComparisons(comparisons)
            .teamAverages(calculateTeamAverages(metrics))
            .collaborationInsights(generateCollaborationInsights(metrics))
            .mentorshipOpportunities(identifyMentorshipOpportunities(comparisons))
            .teamRecommendations(generateTeamRecommendations(comparisons))
            .build();
    }

    // Helper methods for calculations

    private PRHistoricalMetrics.PRSize calculatePRSize(var pullRequest) {
        int totalLines = pullRequest.getAdditions() + pullRequest.getDeletions();
        return PRHistoricalMetrics.PRSize.fromLinesChanged(totalLines);
    }

    private BigDecimal calculateComplexityScore(var pullRequest) {
        // Complex algorithm considering multiple factors
        double score = 0.0;
        
        // File count factor (20% weight)
        score += Math.min(pullRequest.getChangedFiles() * 2.0, 20.0);
        
        // Lines changed factor (30% weight)
        int totalLines = pullRequest.getAdditions() + pullRequest.getDeletions();
        score += Math.min(totalLines * 0.1, 30.0);
        
        // Commit count factor (15% weight)
        score += Math.min(pullRequest.getCommits() * 1.5, 15.0);
        
        // File type diversity (15% weight)
        score += calculateFileTypeDiversity(pullRequest) * 15.0;
        
        // Branch complexity (20% weight)
        score += calculateBranchComplexity(pullRequest) * 20.0;
        
        return BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTestCoverageChange(var pullRequest) {
        // This would integrate with coverage tools
        // For now, estimate based on test file changes
        long testFileChanges = pullRequest.getFiles().stream()
            .filter(file -> file.getFilename().contains("test") || file.getFilename().contains("spec"))
            .count();
        
        if (testFileChanges > 0) {
            return BigDecimal.valueOf(testFileChanges * 5.0).setScale(2, RoundingMode.HALF_UP);
        }
        
        return BigDecimal.ZERO;
    }

    private BigDecimal calculateCodeQualityScore(var pullRequest) {
        // Comprehensive quality score calculation
        double score = 70.0; // Base score
        
        // Test presence bonus
        boolean hasTests = pullRequest.getFiles().stream()
            .anyMatch(file -> file.getFilename().contains("test") || file.getFilename().contains("spec"));
        if (hasTests) score += 15.0;
        
        // Documentation bonus
        boolean hasDocumentation = pullRequest.getFiles().stream()
            .anyMatch(file -> file.getFilename().toLowerCase().contains("readme") || 
                             file.getFilename().toLowerCase().contains("doc"));
        if (hasDocumentation) score += 10.0;
        
        // Size penalty for very large PRs
        int totalLines = pullRequest.getAdditions() + pullRequest.getDeletions();
        if (totalLines > 500) score -= 10.0;
        if (totalLines > 1000) score -= 10.0;
        
        // Title and description quality
        if (pullRequest.getTitle().length() > 50) score += 5.0;
        if (pullRequest.getBody() != null && pullRequest.getBody().length() > 100) score += 5.0;
        
        return BigDecimal.valueOf(Math.max(0, Math.min(100, score))).setScale(2, RoundingMode.HALF_UP);
    }

    private Integer calculateReviewTurnaroundHours(var pullRequest) {
        if (pullRequest.getCreatedAt() != null) {
            LocalDateTime firstReviewTime = getFirstReviewTime(pullRequest);
            if (firstReviewTime != null) {
                return (int) ChronoUnit.HOURS.between(pullRequest.getCreatedAt(), firstReviewTime);
            }
        }
        return null;
    }

    private Integer calculateMergeTimeHours(var pullRequest) {
        if (pullRequest.getCreatedAt() != null && pullRequest.getMergedAt() != null) {
            return (int) ChronoUnit.HOURS.between(pullRequest.getCreatedAt(), pullRequest.getMergedAt());
        }
        return null;
    }

    private void ensureDeveloperProfileExists(Long githubUserId, String githubUsername) {
        if (!developerProfileRepository.existsByGithubUserId(githubUserId)) {
            DeveloperProfile profile = DeveloperProfile.builder()
                .githubUserId(githubUserId)
                .githubUsername(githubUsername)
                .experienceLevel(DeveloperProfile.ExperienceLevel.INTERMEDIATE)
                .communicationStyle(DeveloperProfile.CommunicationStyle.STANDARD)
                .build();
            developerProfileRepository.save(profile);
        }
    }

    // Additional helper methods would be implemented here for:
    // - calculateFileTypeDiversity
    // - calculateBranchComplexity
    // - getFirstReviewTime
    // - Various metric calculations
    // - Report generation methods

    // Data classes for analysis results
    public static class PRHistoryAnalysisReport {
        // Implementation details
    }

    public static class DeveloperPRAnalysis {
        // Implementation details
    }

    public static class TeamComparisonAnalysis {
        // Implementation details
    }

    public static class DeveloperComparison {
        // Implementation details
    }
}