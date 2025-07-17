package com.zamaz.mcp.github.controller;

import com.zamaz.mcp.github.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for the Historical Context Awareness System
 */
@RestController
@RequestMapping("/api/v1/historical-context")
@RequiredArgsConstructor
@Slf4j
public class HistoricalContextController {

    private final PRHistoryAnalysisService prHistoryAnalysisService;
    private final DeveloperLearningProgressService learningProgressService;
    private final TeamKnowledgeBaseService knowledgeBaseService;
    private final PersonalizedSuggestionEngine suggestionEngine;
    private final HistoricalTrendAnalysisService trendAnalysisService;

    // PR History Analysis Endpoints

    /**
     * Get comprehensive PR history analysis for a repository
     */
    @GetMapping("/repositories/{repositoryId}/pr-analysis")
    public ResponseEntity<PRHistoryAnalysisService.PRHistoryAnalysisReport> getRepositoryPRAnalysis(
            @PathVariable Long repositoryId,
            @RequestParam(defaultValue = "6") int months) {
        
        log.info("Getting PR history analysis for repository {} over {} months", repositoryId, months);
        
        PRHistoryAnalysisService.PRHistoryAnalysisReport report = 
            prHistoryAnalysisService.getRepositoryAnalysis(repositoryId, months);
        
        return ResponseEntity.ok(report);
    }

    /**
     * Get developer-specific PR analysis
     */
    @GetMapping("/developers/{developerId}/pr-analysis")
    public ResponseEntity<PRHistoryAnalysisService.DeveloperPRAnalysis> getDeveloperPRAnalysis(
            @PathVariable Long developerId,
            @RequestParam(defaultValue = "6") int months) {
        
        log.info("Getting developer PR analysis for developer {} over {} months", developerId, months);
        
        PRHistoryAnalysisService.DeveloperPRAnalysis analysis = 
            prHistoryAnalysisService.getDeveloperAnalysis(developerId, months);
        
        return ResponseEntity.ok(analysis);
    }

    /**
     * Get team comparison analysis
     */
    @GetMapping("/repositories/{repositoryId}/team-comparison")
    public ResponseEntity<PRHistoryAnalysisService.TeamComparisonAnalysis> getTeamComparison(
            @PathVariable Long repositoryId,
            @RequestParam(defaultValue = "6") int months) {
        
        log.info("Getting team comparison analysis for repository {} over {} months", repositoryId, months);
        
        PRHistoryAnalysisService.TeamComparisonAnalysis analysis = 
            prHistoryAnalysisService.getTeamComparison(repositoryId, months);
        
        return ResponseEntity.ok(analysis);
    }

    /**
     * Analyze PR metrics for a specific PR
     */
    @POST("/repositories/{repositoryId}/prs/{prNumber}/analyze")
    public ResponseEntity<Void> analyzePRMetrics(
            @PathVariable Long repositoryId,
            @PathVariable Integer prNumber,
            @RequestHeader("Authorization") String accessToken) {
        
        log.info("Analyzing PR metrics for repository {} PR #{}", repositoryId, prNumber);
        
        prHistoryAnalysisService.analyzePRMetrics(repositoryId, prNumber, accessToken);
        
        return ResponseEntity.accepted().build();
    }

    // Developer Learning Progress Endpoints

    /**
     * Get comprehensive learning progress report for a developer
     */
    @GetMapping("/developers/{developerId}/learning-report")
    public ResponseEntity<DeveloperLearningProgressService.DeveloperLearningReport> getDeveloperLearningReport(
            @PathVariable Long developerId,
            @RequestParam(defaultValue = "6") int months) {
        
        log.info("Getting learning report for developer {} over {} months", developerId, months);
        
        DeveloperLearningProgressService.DeveloperLearningReport report = 
            learningProgressService.getLearningReport(developerId, months);
        
        return ResponseEntity.ok(report);
    }

    /**
     * Get skill growth analysis for a developer
     */
    @GetMapping("/developers/{developerId}/skill-growth")
    public ResponseEntity<DeveloperLearningProgressService.SkillGrowthAnalysis> getSkillGrowthAnalysis(
            @PathVariable Long developerId,
            @RequestParam(defaultValue = "6") int months) {
        
        log.info("Getting skill growth analysis for developer {} over {} months", developerId, months);
        
        DeveloperLearningProgressService.SkillGrowthAnalysis analysis = 
            learningProgressService.calculateSkillGrowth(developerId, months);
        
        return ResponseEntity.ok(analysis);
    }

    /**
     * Generate learning recommendations for a developer
     */
    @GetMapping("/developers/{developerId}/learning-recommendations")
    public ResponseEntity<List<DeveloperLearningProgressService.LearningRecommendation>> getLearningRecommendations(
            @PathVariable Long developerId) {
        
        log.info("Getting learning recommendations for developer {}", developerId);
        
        List<DeveloperLearningProgressService.LearningRecommendation> recommendations = 
            learningProgressService.generateLearningRecommendations(developerId);
        
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Record skill demonstration event
     */
    @POST("/developers/{developerId}/skills/{skillCategory}/demonstration")
    public ResponseEntity<Void> recordSkillDemonstration(
            @PathVariable Long developerId,
            @PathVariable String skillCategory,
            @RequestBody DeveloperLearningProgressService.SkillDemonstrationEvent event) {
        
        log.info("Recording skill demonstration for developer {} in category {}", developerId, skillCategory);
        
        learningProgressService.recordSkillDemonstration(developerId, skillCategory, event);
        
        return ResponseEntity.accepted().build();
    }

    /**
     * Assess developer skills
     */
    @POST("/developers/{developerId}/assess-skills")
    public ResponseEntity<Void> assessDeveloperSkills(@PathVariable Long developerId) {
        
        log.info("Assessing skills for developer {}", developerId);
        
        learningProgressService.assessDeveloperSkills(developerId);
        
        return ResponseEntity.accepted().build();
    }

    // Team Knowledge Base Endpoints

    /**
     * Search knowledge base entries
     */
    @GetMapping("/knowledge-base/search")
    public ResponseEntity<Page<com.zamaz.mcp.github.entity.KnowledgeBaseEntry>> searchKnowledgeBase(
            @RequestParam(required = false) Long repositoryId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "effectivenessScore") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        log.info("Searching knowledge base with repositoryId={}, category={}, searchTerm={}", 
                repositoryId, category, searchTerm);
        
        Pageable pageable = PageRequest.of(page, size, 
            Sort.by(Sort.Direction.fromString(sortDirection), sortBy));
        
        TeamKnowledgeBaseService.KnowledgeSearchCriteria criteria = 
            TeamKnowledgeBaseService.KnowledgeSearchCriteria.builder()
                .repositoryId(repositoryId)
                .category(category)
                .searchTerm(searchTerm)
                .build();
        
        Page<com.zamaz.mcp.github.entity.KnowledgeBaseEntry> results = 
            knowledgeBaseService.searchKnowledgeBase(criteria, pageable);
        
        return ResponseEntity.ok(results);
    }

    /**
     * Get knowledge base recommendations
     */
    @GetMapping("/knowledge-base/recommendations")
    public ResponseEntity<List<TeamKnowledgeBaseService.KnowledgeRecommendation>> getKnowledgeRecommendations(
            @RequestParam Long repositoryId,
            @RequestParam String context,
            @RequestParam(required = false) List<String> technologies,
            @RequestParam(required = false) Long developerId) {
        
        log.info("Getting knowledge recommendations for repository {} with context {}", repositoryId, context);
        
        List<TeamKnowledgeBaseService.KnowledgeRecommendation> recommendations = 
            knowledgeBaseService.getRecommendations(repositoryId, context, technologies, developerId);
        
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Create knowledge base entry
     */
    @PostMapping("/knowledge-base/entries")
    public ResponseEntity<com.zamaz.mcp.github.entity.KnowledgeBaseEntry> createKnowledgeEntry(
            @RequestBody TeamKnowledgeBaseService.KnowledgeEntryRequest request) {
        
        log.info("Creating knowledge base entry: {}", request.getTitle());
        
        com.zamaz.mcp.github.entity.KnowledgeBaseEntry entry = 
            knowledgeBaseService.createKnowledgeEntry(request);
        
        return ResponseEntity.ok(entry);
    }

    /**
     * Approve knowledge base entry
     */
    @POST("/knowledge-base/entries/{entryId}/approve")
    public ResponseEntity<Void> approveKnowledgeEntry(
            @PathVariable Long entryId,
            @RequestParam Long approvedByUserId) {
        
        log.info("Approving knowledge base entry {} by user {}", entryId, approvedByUserId);
        
        knowledgeBaseService.approveKnowledgeEntry(entryId, approvedByUserId);
        
        return ResponseEntity.accepted().build();
    }

    /**
     * Get knowledge base analytics
     */
    @GetMapping("/knowledge-base/analytics")
    public ResponseEntity<TeamKnowledgeBaseService.KnowledgeBaseAnalytics> getKnowledgeBaseAnalytics(
            @RequestParam Long repositoryId) {
        
        log.info("Getting knowledge base analytics for repository {}", repositoryId);
        
        TeamKnowledgeBaseService.KnowledgeBaseAnalytics analytics = 
            knowledgeBaseService.getKnowledgeBaseAnalytics(repositoryId);
        
        return ResponseEntity.ok(analytics);
    }

    /**
     * Extract knowledge from recent reviews
     */
    @POST("/knowledge-base/extract")
    public ResponseEntity<Void> extractKnowledgeFromReviews(
            @RequestParam Long repositoryId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate) {
        
        log.info("Extracting knowledge from reviews for repository {} since {}", repositoryId, fromDate);
        
        knowledgeBaseService.extractKnowledgeFromReviews(repositoryId, fromDate);
        
        return ResponseEntity.accepted().build();
    }

    // Personalized Suggestion Engine Endpoints

    /**
     * Generate personalized suggestions for a developer
     */
    @GetMapping("/developers/{developerId}/suggestions")
    public ResponseEntity<List<PersonalizedSuggestionEngine.PersonalizedSuggestion>> getPersonalizedSuggestions(
            @PathVariable Long developerId,
            @RequestParam(required = false) String repositoryName,
            @RequestParam(required = false) String currentTask,
            @RequestParam(required = false) List<String> technologies,
            @RequestParam(required = false) String urgency) {
        
        log.info("Getting personalized suggestions for developer {}", developerId);
        
        PersonalizedSuggestionEngine.SuggestionContext context = 
            PersonalizedSuggestionEngine.SuggestionContext.builder()
                .repositoryName(repositoryName)
                .currentTask(currentTask)
                .technologies(technologies)
                .urgency(urgency)
                .build();
        
        List<PersonalizedSuggestionEngine.PersonalizedSuggestion> suggestions = 
            suggestionEngine.generateSuggestions(developerId, context);
        
        return ResponseEntity.ok(suggestions);
    }

    /**
     * Get contextual suggestions based on current activity
     */
    @PostMapping("/developers/{developerId}/contextual-suggestions")
    public ResponseEntity<List<PersonalizedSuggestionEngine.ContextualSuggestion>> getContextualSuggestions(
            @PathVariable Long developerId,
            @RequestBody PersonalizedSuggestionEngine.CodeContext codeContext) {
        
        log.info("Getting contextual suggestions for developer {} in code context", developerId);
        
        List<PersonalizedSuggestionEngine.ContextualSuggestion> suggestions = 
            suggestionEngine.getContextualSuggestions(developerId, codeContext);
        
        return ResponseEntity.ok(suggestions);
    }

    /**
     * Process suggestion feedback
     */
    @POST("/suggestions/{suggestionId}/feedback")
    public ResponseEntity<Void> processSuggestionFeedback(
            @PathVariable Long suggestionId,
            @RequestBody PersonalizedSuggestionEngine.SuggestionFeedback feedback) {
        
        log.info("Processing suggestion feedback for suggestion {}", suggestionId);
        
        suggestionEngine.processSuggestionFeedback(suggestionId, feedback);
        
        return ResponseEntity.accepted().build();
    }

    /**
     * Get suggestion effectiveness analytics
     */
    @GetMapping("/developers/{developerId}/suggestion-analytics")
    public ResponseEntity<PersonalizedSuggestionEngine.SuggestionEffectivenessAnalytics> getSuggestionAnalytics(
            @PathVariable Long developerId,
            @RequestParam(defaultValue = "6") int months) {
        
        log.info("Getting suggestion analytics for developer {} over {} months", developerId, months);
        
        PersonalizedSuggestionEngine.SuggestionEffectivenessAnalytics analytics = 
            suggestionEngine.getSuggestionAnalytics(developerId, months);
        
        return ResponseEntity.ok(analytics);
    }

    // Historical Trend Analysis Endpoints

    /**
     * Get trend analysis report for a repository
     */
    @GetMapping("/repositories/{repositoryId}/trend-analysis")
    public ResponseEntity<HistoricalTrendAnalysisService.TrendAnalysisReport> getTrendAnalysisReport(
            @PathVariable Long repositoryId,
            @RequestParam(defaultValue = "6") int months) {
        
        log.info("Getting trend analysis report for repository {} over {} months", repositoryId, months);
        
        HistoricalTrendAnalysisService.TrendAnalysisReport report = 
            trendAnalysisService.getTrendAnalysisReport(repositoryId, months);
        
        return ResponseEntity.ok(report);
    }

    /**
     * Compare trends between different time periods
     */
    @GetMapping("/repositories/{repositoryId}/trend-comparison")
    public ResponseEntity<HistoricalTrendAnalysisService.TrendComparisonReport> compareTrends(
            @PathVariable Long repositoryId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate period1Start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate period1End,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate period2Start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate period2End) {
        
        log.info("Comparing trends for repository {} between periods", repositoryId);
        
        HistoricalTrendAnalysisService.TrendComparisonReport report = 
            trendAnalysisService.compareTrends(repositoryId, period1Start, period1End, period2Start, period2End);
        
        return ResponseEntity.ok(report);
    }

    /**
     * Predict future trends
     */
    @GetMapping("/repositories/{repositoryId}/trend-prediction")
    public ResponseEntity<HistoricalTrendAnalysisService.TrendPredictionReport> predictTrends(
            @PathVariable Long repositoryId,
            @RequestParam(defaultValue = "6") int historicalMonths,
            @RequestParam(defaultValue = "3") int predictionMonths) {
        
        log.info("Predicting trends for repository {} using {} months of history to predict {} months ahead", 
                repositoryId, historicalMonths, predictionMonths);
        
        HistoricalTrendAnalysisService.TrendPredictionReport report = 
            trendAnalysisService.predictTrends(repositoryId, historicalMonths, predictionMonths);
        
        return ResponseEntity.ok(report);
    }

    /**
     * Get team performance trends
     */
    @GetMapping("/repositories/{repositoryId}/team-performance-trends")
    public ResponseEntity<HistoricalTrendAnalysisService.TeamPerformanceTrends> getTeamPerformanceTrends(
            @PathVariable Long repositoryId,
            @RequestParam(defaultValue = "6") int months) {
        
        log.info("Getting team performance trends for repository {} over {} months", repositoryId, months);
        
        HistoricalTrendAnalysisService.TeamPerformanceTrends trends = 
            trendAnalysisService.getTeamPerformanceTrends(repositoryId, months);
        
        return ResponseEntity.ok(trends);
    }

    /**
     * Calculate code quality trends for a repository
     */
    @POST("/repositories/{repositoryId}/calculate-trends")
    public ResponseEntity<Void> calculateCodeQualityTrends(
            @PathVariable Long repositoryId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate analysisDate,
            @RequestParam String periodType) {
        
        log.info("Calculating code quality trends for repository {} on {} ({})", repositoryId, analysisDate, periodType);
        
        HistoricalTrendAnalysisService.PeriodType period = 
            HistoricalTrendAnalysisService.PeriodType.valueOf(periodType.toUpperCase());
        
        trendAnalysisService.calculateCodeQualityTrends(repositoryId, analysisDate, period);
        
        return ResponseEntity.accepted().build();
    }

    // Health Check Endpoint

    /**
     * Health check endpoint for the historical context system
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        log.info("Health check requested for historical context system");
        
        Map<String, Object> health = Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now(),
            "components", Map.of(
                "pr-analysis", "UP",
                "learning-progress", "UP",
                "knowledge-base", "UP",
                "suggestion-engine", "UP",
                "trend-analysis", "UP"
            )
        );
        
        return ResponseEntity.ok(health);
    }
}