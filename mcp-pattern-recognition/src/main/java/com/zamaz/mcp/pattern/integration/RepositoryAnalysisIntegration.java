package com.zamaz.mcp.pattern.integration;

import com.zamaz.mcp.pattern.core.PatternDetector;
import com.zamaz.mcp.pattern.model.CodeAnalysisContext;
import com.zamaz.mcp.pattern.model.PatternDetectionResult;
import com.zamaz.mcp.pattern.model.ComprehensiveRecommendations;
import com.zamaz.mcp.pattern.model.PatternReport;
import com.zamaz.mcp.pattern.service.PatternAnalysisService;
import com.zamaz.mcp.pattern.recommendation.PatternRecommendationEngine;
import com.zamaz.mcp.pattern.reporting.PatternReportingService;
import com.zamaz.mcp.pattern.ml.TeamPatternLearningService;
import com.zamaz.mcp.pattern.performance.PerformanceOptimizer;
import com.zamaz.mcp.common.audit.AuditService;
import com.zamaz.mcp.common.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Integration service for connecting pattern recognition with existing repository analysis.
 * 
 * This service provides:
 * - Integration with GitHub repository analysis
 * - MCP endpoint compatibility
 * - Event-driven pattern analysis
 * - Audit logging for pattern detection
 * - Performance-optimized analysis workflows
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RepositoryAnalysisIntegration {
    
    private final PatternAnalysisService patternAnalysisService;
    private final PatternRecommendationEngine recommendationEngine;
    private final PatternReportingService reportingService;
    private final TeamPatternLearningService learningService;
    private final PerformanceOptimizer performanceOptimizer;
    private final AuditService auditService;
    private final EventPublisher eventPublisher;
    private final GitHubIntegrationClient gitHubClient;
    private final McpEndpointAdapter mcpAdapter;
    
    /**
     * Analyze a GitHub repository for patterns.
     * 
     * @param organizationId Organization identifier
     * @param repositoryUrl GitHub repository URL
     * @param analysisOptions Analysis configuration options
     * @return Pattern analysis results
     */
    @Transactional
    public RepositoryPatternAnalysisResult analyzeRepository(String organizationId,
                                                            String repositoryUrl,
                                                            RepositoryAnalysisOptions analysisOptions) {
        
        log.info("Starting repository pattern analysis for org: {}, repo: {}", organizationId, repositoryUrl);
        
        String analysisId = UUID.randomUUID().toString();
        
        try {
            // Audit the analysis start
            auditService.recordEvent("REPOSITORY_ANALYSIS_START", Map.of(
                    "organizationId", organizationId,
                    "repositoryUrl", repositoryUrl,
                    "analysisId", analysisId
            ));
            
            // Clone or fetch repository
            RepositorySnapshot snapshot = gitHubClient.fetchRepository(repositoryUrl, analysisOptions);
            
            // Convert repository files to analysis contexts
            List<CodeAnalysisContext> contexts = convertToAnalysisContexts(snapshot, analysisOptions);
            
            // Optimize analysis for large repositories
            List<PatternDetectionResult> detectionResults = performanceOptimizer.optimizePatternDetection(
                    contexts, analysisOptions.getOptimizationStrategy());
            
            // Generate recommendations
            ComprehensiveRecommendations recommendations = recommendationEngine.generateRecommendations(
                    organizationId, detectionResults, contexts);
            
            // Generate report
            PatternReport report = reportingService.generateComprehensiveReport(
                    organizationId, detectionResults, recommendations);
            
            // Learn from the analysis
            learningService.learnFromAnalysis(organizationId, detectionResults, contexts);
            
            // Create result
            RepositoryPatternAnalysisResult result = RepositoryPatternAnalysisResult.builder()
                    .analysisId(analysisId)
                    .organizationId(organizationId)
                    .repositoryUrl(repositoryUrl)
                    .analysisDate(LocalDateTime.now())
                    .snapshot(snapshot)
                    .detectionResults(detectionResults)
                    .recommendations(recommendations)
                    .report(report)
                    .analysisOptions(analysisOptions)
                    .build();
            
            // Publish analysis completed event
            eventPublisher.publishEvent(new RepositoryAnalysisCompletedEvent(result));
            
            // Audit the analysis completion
            auditService.recordEvent("REPOSITORY_ANALYSIS_COMPLETED", Map.of(
                    "organizationId", organizationId,
                    "repositoryUrl", repositoryUrl,
                    "analysisId", analysisId,
                    "patternsDetected", detectionResults.size(),
                    "executionTime", System.currentTimeMillis() - snapshot.getAnalysisStartTime()
            ));
            
            log.info("Repository pattern analysis completed for {}: {} patterns detected", 
                     repositoryUrl, detectionResults.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error analyzing repository {}", repositoryUrl, e);
            
            // Audit the analysis failure
            auditService.recordEvent("REPOSITORY_ANALYSIS_FAILED", Map.of(
                    "organizationId", organizationId,
                    "repositoryUrl", repositoryUrl,
                    "analysisId", analysisId,
                    "error", e.getMessage()
            ));
            
            throw new RuntimeException("Repository analysis failed", e);
        }
    }
    
    /**
     * Analyze a pull request for pattern changes.
     * 
     * @param organizationId Organization identifier
     * @param pullRequestUrl Pull request URL
     * @param analysisOptions Analysis configuration options
     * @return Pull request pattern analysis results
     */
    @Transactional
    public PullRequestPatternAnalysisResult analyzePullRequest(String organizationId,
                                                              String pullRequestUrl,
                                                              RepositoryAnalysisOptions analysisOptions) {
        
        log.info("Starting pull request pattern analysis for org: {}, PR: {}", organizationId, pullRequestUrl);
        
        String analysisId = UUID.randomUUID().toString();
        
        try {
            // Fetch pull request changes
            PullRequestSnapshot snapshot = gitHubClient.fetchPullRequest(pullRequestUrl, analysisOptions);
            
            // Analyze changed files only
            List<CodeAnalysisContext> changedContexts = convertChangedFilesToContexts(snapshot, analysisOptions);
            
            // Detect patterns in changed files
            List<PatternDetectionResult> detectionResults = patternAnalysisService.analyzeCodeContexts(
                    organizationId, changedContexts);
            
            // Generate pull request specific recommendations
            PullRequestRecommendations recommendations = generatePullRequestRecommendations(
                    organizationId, snapshot, detectionResults);
            
            // Create result
            PullRequestPatternAnalysisResult result = PullRequestPatternAnalysisResult.builder()
                    .analysisId(analysisId)
                    .organizationId(organizationId)
                    .pullRequestUrl(pullRequestUrl)
                    .analysisDate(LocalDateTime.now())
                    .snapshot(snapshot)
                    .detectionResults(detectionResults)
                    .recommendations(recommendations)
                    .impactAssessment(assessPullRequestImpact(snapshot, detectionResults))
                    .build();
            
            // Publish pull request analysis event
            eventPublisher.publishEvent(new PullRequestAnalysisCompletedEvent(result));
            
            log.info("Pull request pattern analysis completed for {}: {} patterns detected", 
                     pullRequestUrl, detectionResults.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("Error analyzing pull request {}", pullRequestUrl, e);
            throw new RuntimeException("Pull request analysis failed", e);
        }
    }
    
    /**
     * Provide MCP endpoint for pattern analysis.
     * 
     * @param mcpRequest MCP request for pattern analysis
     * @return MCP response with pattern analysis results
     */
    public McpResponse handleMcpPatternAnalysis(McpRequest mcpRequest) {
        
        log.info("Handling MCP pattern analysis request: {}", mcpRequest.getMethod());
        
        try {
            return switch (mcpRequest.getMethod()) {
                case "pattern_analysis/analyze_repository" -> 
                        handleAnalyzeRepositoryMcp(mcpRequest);
                case "pattern_analysis/analyze_pull_request" -> 
                        handleAnalyzePullRequestMcp(mcpRequest);
                case "pattern_analysis/get_recommendations" -> 
                        handleGetRecommendationsMcp(mcpRequest);
                case "pattern_analysis/get_report" -> 
                        handleGetReportMcp(mcpRequest);
                case "pattern_analysis/get_team_patterns" -> 
                        handleGetTeamPatternsMcp(mcpRequest);
                default -> mcpAdapter.createErrorResponse(mcpRequest, "Unknown method: " + mcpRequest.getMethod());
            };
            
        } catch (Exception e) {
            log.error("Error handling MCP pattern analysis request", e);
            return mcpAdapter.createErrorResponse(mcpRequest, "Internal error: " + e.getMessage());
        }
    }
    
    /**
     * Integrate with GitHub webhook for continuous analysis.
     * 
     * @param webhookPayload GitHub webhook payload
     * @return Webhook processing result
     */
    @Transactional
    public WebhookProcessingResult processGitHubWebhook(GitHubWebhookPayload webhookPayload) {
        
        log.info("Processing GitHub webhook: {}", webhookPayload.getAction());
        
        try {
            return switch (webhookPayload.getAction()) {
                case "opened", "synchronize" -> processPullRequestWebhook(webhookPayload);
                case "push" -> processPushWebhook(webhookPayload);
                case "release" -> processReleaseWebhook(webhookPayload);
                default -> WebhookProcessingResult.ignored("Unsupported action: " + webhookPayload.getAction());
            };
            
        } catch (Exception e) {
            log.error("Error processing GitHub webhook", e);
            return WebhookProcessingResult.error("Webhook processing failed: " + e.getMessage());
        }
    }
    
    /**
     * Get pattern analysis dashboard data.
     * 
     * @param organizationId Organization identifier
     * @param timeRange Time range for analysis
     * @return Dashboard data
     */
    public PatternAnalysisDashboard getDashboardData(String organizationId, String timeRange) {
        
        log.info("Getting dashboard data for organization: {}, timeRange: {}", organizationId, timeRange);
        
        try {
            // Get dashboard data from reporting service
            DashboardData dashboardData = reportingService.generateDashboardData(organizationId);
            
            // Get team learning insights
            ModelPerformanceMetrics modelMetrics = learningService.getModelPerformance(organizationId);
            
            // Get recent analysis results
            List<PatternDetectionResult> recentResults = getRecentAnalysisResults(organizationId, timeRange);
            
            return PatternAnalysisDashboard.builder()
                    .organizationId(organizationId)
                    .timeRange(timeRange)
                    .dashboardData(dashboardData)
                    .modelMetrics(modelMetrics)
                    .recentResults(recentResults)
                    .lastUpdated(LocalDateTime.now())
                    .build();
            
        } catch (Exception e) {
            log.error("Error getting dashboard data for organization: {}", organizationId, e);
            throw new RuntimeException("Dashboard data retrieval failed", e);
        }
    }
    
    /**
     * Configure organization-specific pattern analysis settings.
     * 
     * @param organizationId Organization identifier
     * @param configuration Pattern analysis configuration
     * @return Configuration result
     */
    @Transactional
    public PatternAnalysisConfigurationResult configurePatternAnalysis(String organizationId,
                                                                       PatternAnalysisConfiguration configuration) {
        
        log.info("Configuring pattern analysis for organization: {}", organizationId);
        
        try {
            // Update team learning configuration
            learningService.updateConfiguration(organizationId, configuration.getLearningConfiguration());
            
            // Update detection configuration
            patternAnalysisService.updateConfiguration(organizationId, configuration.getDetectionConfiguration());
            
            // Update reporting configuration
            reportingService.updateConfiguration(organizationId, configuration.getReportingConfiguration());
            
            // Audit configuration change
            auditService.recordEvent("PATTERN_ANALYSIS_CONFIGURED", Map.of(
                    "organizationId", organizationId,
                    "configuration", configuration.toString()
            ));
            
            return PatternAnalysisConfigurationResult.builder()
                    .organizationId(organizationId)
                    .configuration(configuration)
                    .configuredAt(LocalDateTime.now())
                    .success(true)
                    .build();
            
        } catch (Exception e) {
            log.error("Error configuring pattern analysis for organization: {}", organizationId, e);
            throw new RuntimeException("Pattern analysis configuration failed", e);
        }
    }
    
    // Private helper methods
    
    private List<CodeAnalysisContext> convertToAnalysisContexts(RepositorySnapshot snapshot,
                                                               RepositoryAnalysisOptions options) {
        
        return snapshot.getFiles().stream()
                .filter(file -> shouldAnalyzeFile(file, options))
                .map(file -> convertFileToContext(file, snapshot))
                .collect(Collectors.toList());
    }
    
    private List<CodeAnalysisContext> convertChangedFilesToContexts(PullRequestSnapshot snapshot,
                                                                   RepositoryAnalysisOptions options) {
        
        return snapshot.getChangedFiles().stream()
                .filter(file -> shouldAnalyzeFile(file, options))
                .map(file -> convertFileToContext(file, snapshot))
                .collect(Collectors.toList());
    }
    
    private boolean shouldAnalyzeFile(RepositoryFile file, RepositoryAnalysisOptions options) {
        
        // Check file extension
        if (!options.getSupportedExtensions().contains(file.getExtension())) {
            return false;
        }
        
        // Check file size
        if (file.getSize() > options.getMaxFileSize()) {
            return false;
        }
        
        // Check excluded patterns
        return options.getExcludedPatterns().stream()
                .noneMatch(pattern -> file.getPath().matches(pattern));
    }
    
    private CodeAnalysisContext convertFileToContext(RepositoryFile file, RepositorySnapshot snapshot) {
        
        // Parse the file content
        CompilationUnit compilationUnit = parseJavaFile(file.getContent());
        
        // Extract metadata
        FileMetadata metadata = extractFileMetadata(file);
        
        // Create project context
        ProjectContext projectContext = createProjectContext(snapshot);
        
        return CodeAnalysisContext.builder()
                .filePath(file.getPath())
                .sourceCode(file.getContent())
                .compilationUnit(compilationUnit)
                .fileMetadata(metadata)
                .projectContext(projectContext)
                .fileExtension(file.getExtension())
                .lineCount(file.getLineCount())
                .build();
    }
    
    private CompilationUnit parseJavaFile(String content) {
        // Use JavaParser to parse the file
        try {
            return com.github.javaparser.JavaParser.parse(content);
        } catch (Exception e) {
            log.warn("Failed to parse Java file", e);
            return null;
        }
    }
    
    private FileMetadata extractFileMetadata(RepositoryFile file) {
        return FileMetadata.builder()
                .fileName(file.getName())
                .fileSize(file.getSize())
                .extension(file.getExtension())
                .relativePath(file.getPath().toString())
                .lineCount(file.getLineCount())
                .lastModified(file.getLastModified())
                .build();
    }
    
    private ProjectContext createProjectContext(RepositorySnapshot snapshot) {
        return ProjectContext.builder()
                .projectName(snapshot.getRepositoryName())
                .projectRoot(snapshot.getProjectRoot())
                .totalFiles(snapshot.getFiles().size())
                .build();
    }
    
    private PullRequestRecommendations generatePullRequestRecommendations(String organizationId,
                                                                         PullRequestSnapshot snapshot,
                                                                         List<PatternDetectionResult> results) {
        
        // Generate recommendations specific to pull request context
        List<String> recommendations = new ArrayList<>();
        
        // Check for new patterns introduced
        long newIssues = results.stream()
                .filter(result -> result.isNegativePattern())
                .count();
        
        if (newIssues > 0) {
            recommendations.add("This pull request introduces " + newIssues + " new pattern issues");
        }
        
        // Check for pattern improvements
        long improvements = results.stream()
                .filter(result -> result.isPositivePattern())
                .count();
        
        if (improvements > 0) {
            recommendations.add("This pull request improves code patterns in " + improvements + " places");
        }
        
        return PullRequestRecommendations.builder()
                .pullRequestId(snapshot.getPullRequestId())
                .recommendations(recommendations)
                .overallImpact(assessOverallImpact(results))
                .approvalRecommendation(generateApprovalRecommendation(results))
                .build();
    }
    
    private PullRequestImpactAssessment assessPullRequestImpact(PullRequestSnapshot snapshot,
                                                               List<PatternDetectionResult> results) {
        
        long criticalIssues = results.stream()
                .filter(result -> result.getSeverity() == PatternSeverity.CRITICAL)
                .count();
        
        long highIssues = results.stream()
                .filter(result -> result.getSeverity() == PatternSeverity.HIGH)
                .count();
        
        return PullRequestImpactAssessment.builder()
                .criticalIssues(criticalIssues)
                .highPriorityIssues(highIssues)
                .overallImpact(criticalIssues > 0 ? "HIGH" : highIssues > 0 ? "MEDIUM" : "LOW")
                .recommendation(criticalIssues > 0 ? "REJECT" : highIssues > 3 ? "REQUEST_CHANGES" : "APPROVE")
                .build();
    }
    
    private McpResponse handleAnalyzeRepositoryMcp(McpRequest request) {
        
        Map<String, Object> params = request.getParams();
        String organizationId = (String) params.get("organizationId");
        String repositoryUrl = (String) params.get("repositoryUrl");
        RepositoryAnalysisOptions options = mcpAdapter.parseAnalysisOptions(params);
        
        RepositoryPatternAnalysisResult result = analyzeRepository(organizationId, repositoryUrl, options);
        
        return mcpAdapter.createSuccessResponse(request, result);
    }
    
    private McpResponse handleAnalyzePullRequestMcp(McpRequest request) {
        
        Map<String, Object> params = request.getParams();
        String organizationId = (String) params.get("organizationId");
        String pullRequestUrl = (String) params.get("pullRequestUrl");
        RepositoryAnalysisOptions options = mcpAdapter.parseAnalysisOptions(params);
        
        PullRequestPatternAnalysisResult result = analyzePullRequest(organizationId, pullRequestUrl, options);
        
        return mcpAdapter.createSuccessResponse(request, result);
    }
    
    private McpResponse handleGetRecommendationsMcp(McpRequest request) {
        
        Map<String, Object> params = request.getParams();
        String organizationId = (String) params.get("organizationId");
        String analysisId = (String) params.get("analysisId");
        
        // Get recommendations from analysis result
        ComprehensiveRecommendations recommendations = getRecommendationsByAnalysisId(analysisId);
        
        return mcpAdapter.createSuccessResponse(request, recommendations);
    }
    
    private McpResponse handleGetReportMcp(McpRequest request) {
        
        Map<String, Object> params = request.getParams();
        String reportId = (String) params.get("reportId");
        String format = (String) params.getOrDefault("format", "JSON");
        
        // Get and export report
        ExportedReport report = reportingService.exportReport(reportId, ExportFormat.valueOf(format));
        
        return mcpAdapter.createSuccessResponse(request, report);
    }
    
    private McpResponse handleGetTeamPatternsMcp(McpRequest request) {
        
        Map<String, Object> params = request.getParams();
        String organizationId = (String) params.get("organizationId");
        
        // Get team patterns from learning service
        List<TeamCodingPattern> patterns = learningService.getTeamPatterns(organizationId);
        
        return mcpAdapter.createSuccessResponse(request, patterns);
    }
    
    private WebhookProcessingResult processPullRequestWebhook(GitHubWebhookPayload payload) {
        
        String organizationId = payload.getOrganization().getId();
        String pullRequestUrl = payload.getPullRequest().getUrl();
        
        // Analyze pull request asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                RepositoryAnalysisOptions options = RepositoryAnalysisOptions.defaultOptions();
                analyzePullRequest(organizationId, pullRequestUrl, options);
            } catch (Exception e) {
                log.error("Error analyzing pull request from webhook", e);
            }
        });
        
        return WebhookProcessingResult.success("Pull request analysis started");
    }
    
    private WebhookProcessingResult processPushWebhook(GitHubWebhookPayload payload) {
        
        String organizationId = payload.getOrganization().getId();
        String repositoryUrl = payload.getRepository().getUrl();
        
        // Analyze repository for pattern changes
        CompletableFuture.runAsync(() -> {
            try {
                RepositoryAnalysisOptions options = RepositoryAnalysisOptions.defaultOptions();
                analyzeRepository(organizationId, repositoryUrl, options);
            } catch (Exception e) {
                log.error("Error analyzing repository from webhook", e);
            }
        });
        
        return WebhookProcessingResult.success("Repository analysis started");
    }
    
    private WebhookProcessingResult processReleaseWebhook(GitHubWebhookPayload payload) {
        
        String organizationId = payload.getOrganization().getId();
        String repositoryUrl = payload.getRepository().getUrl();
        
        // Generate release quality report
        CompletableFuture.runAsync(() -> {
            try {
                RepositoryAnalysisOptions options = RepositoryAnalysisOptions.defaultOptions();
                RepositoryPatternAnalysisResult result = analyzeRepository(organizationId, repositoryUrl, options);
                
                // Generate release report
                generateReleaseQualityReport(result);
                
            } catch (Exception e) {
                log.error("Error generating release quality report", e);
            }
        });
        
        return WebhookProcessingResult.success("Release quality analysis started");
    }
    
    private List<PatternDetectionResult> getRecentAnalysisResults(String organizationId, String timeRange) {
        
        LocalDateTime fromDate = switch (timeRange) {
            case "24h" -> LocalDateTime.now().minusDays(1);
            case "7d" -> LocalDateTime.now().minusDays(7);
            case "30d" -> LocalDateTime.now().minusDays(30);
            default -> LocalDateTime.now().minusDays(7);
        };
        
        return patternAnalysisService.getResultsByOrganizationAndDateRange(organizationId, fromDate, LocalDateTime.now());
    }
    
    private ComprehensiveRecommendations getRecommendationsByAnalysisId(String analysisId) {
        // Implementation would retrieve recommendations from storage
        return ComprehensiveRecommendations.empty("unknown");
    }
    
    private String assessOverallImpact(List<PatternDetectionResult> results) {
        
        long criticalIssues = results.stream()
                .filter(result -> result.getSeverity() == PatternSeverity.CRITICAL)
                .count();
        
        if (criticalIssues > 0) return "HIGH";
        
        long highIssues = results.stream()
                .filter(result -> result.getSeverity() == PatternSeverity.HIGH)
                .count();
        
        return highIssues > 3 ? "MEDIUM" : "LOW";
    }
    
    private String generateApprovalRecommendation(List<PatternDetectionResult> results) {
        
        long criticalIssues = results.stream()
                .filter(result -> result.getSeverity() == PatternSeverity.CRITICAL)
                .count();
        
        if (criticalIssues > 0) return "REJECT";
        
        long highIssues = results.stream()
                .filter(result -> result.getSeverity() == PatternSeverity.HIGH)
                .count();
        
        return highIssues > 3 ? "REQUEST_CHANGES" : "APPROVE";
    }
    
    private void generateReleaseQualityReport(RepositoryPatternAnalysisResult result) {
        
        // Generate a specialized report for release quality
        log.info("Generating release quality report for {}", result.getRepositoryUrl());
        
        // Implementation would create a release-specific report
        // and potentially send notifications to stakeholders
    }
}