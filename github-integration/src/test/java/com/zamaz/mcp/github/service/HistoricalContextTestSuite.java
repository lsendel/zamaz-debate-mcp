package com.zamaz.mcp.github.service;

import com.zamaz.mcp.github.entity.*;
import com.zamaz.mcp.github.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for the Historical Context Awareness System
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest
@ActiveProfiles("test")
public class HistoricalContextTestSuite {

    @Mock
    private PRHistoricalMetricsRepository prHistoricalMetricsRepository;
    
    @Mock
    private DeveloperProfileRepository developerProfileRepository;
    
    @Mock
    private DeveloperSkillAssessmentRepository skillAssessmentRepository;
    
    @Mock
    private KnowledgeBaseEntryRepository knowledgeBaseRepository;
    
    @Mock
    private PersonalizedSuggestionsRepository personalizedSuggestionsRepository;
    
    @Mock
    private CodeQualityTrendsRepository codeQualityTrendsRepository;
    
    @Mock
    private PullRequestReviewRepository pullRequestReviewRepository;
    
    @Mock
    private ReviewIssueRepository reviewIssueRepository;
    
    @Mock
    private ReviewCommentRepository reviewCommentRepository;
    
    @Mock
    private GitHubApiClient gitHubApiClient;

    private PRHistoryAnalysisService prHistoryAnalysisService;
    private DeveloperLearningProgressService learningProgressService;
    private TeamKnowledgeBaseService knowledgeBaseService;
    private PersonalizedSuggestionEngine suggestionEngine;
    private HistoricalTrendAnalysisService trendAnalysisService;

    @BeforeEach
    void setUp() {
        prHistoryAnalysisService = new PRHistoryAnalysisService(
            prHistoricalMetricsRepository,
            developerProfileRepository,
            pullRequestReviewRepository,
            gitHubApiClient
        );
        
        learningProgressService = new DeveloperLearningProgressService(
            developerProfileRepository,
            skillAssessmentRepository,
            prHistoricalMetricsRepository,
            pullRequestReviewRepository,
            reviewIssueRepository
        );
        
        knowledgeBaseService = new TeamKnowledgeBaseService(
            knowledgeBaseRepository,
            developerProfileRepository,
            pullRequestReviewRepository,
            reviewIssueRepository,
            reviewCommentRepository
        );
        
        suggestionEngine = new PersonalizedSuggestionEngine(
            personalizedSuggestionsRepository,
            developerProfileRepository,
            skillAssessmentRepository,
            prHistoricalMetricsRepository,
            knowledgeBaseRepository,
            null, // MLTrainingDataRepository
            null  // PatternRecognitionService
        );
        
        trendAnalysisService = new HistoricalTrendAnalysisService(
            codeQualityTrendsRepository,
            prHistoricalMetricsRepository,
            pullRequestReviewRepository,
            developerProfileRepository
        );
    }

    // PR History Analysis Tests
    
    @Test
    void testAnalyzePRMetrics_Success() {
        // Given
        Long repositoryId = 1L;
        Integer prNumber = 123;
        String accessToken = "test-token";
        
        // Mock GitHub API response
        when(gitHubApiClient.getPullRequest(anyString(), anyString(), anyString(), anyInt()))
            .thenReturn(createMockPullRequest());
        
        // Mock repository existence check
        when(developerProfileRepository.existsByGithubUserId(anyLong()))
            .thenReturn(true);
        
        when(prHistoricalMetricsRepository.save(any(PRHistoricalMetrics.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PRHistoricalMetrics result = prHistoryAnalysisService.analyzePRMetrics(repositoryId, prNumber, accessToken);

        // Then
        assertNotNull(result);
        assertEquals(repositoryId, result.getRepositoryId());
        assertEquals(prNumber, result.getPrNumber());
        
        verify(prHistoricalMetricsRepository).save(any(PRHistoricalMetrics.class));
    }

    @Test
    void testGetRepositoryAnalysis_Success() {
        // Given
        Long repositoryId = 1L;
        int months = 6;
        LocalDateTime fromDate = LocalDateTime.now().minusMonths(months);
        
        List<PRHistoricalMetrics> mockMetrics = createMockPRMetrics();
        when(prHistoricalMetricsRepository.findByRepositoryIdAndCreatedAtAfter(repositoryId, fromDate))
            .thenReturn(mockMetrics);

        // When
        PRHistoryAnalysisService.PRHistoryAnalysisReport report = 
            prHistoryAnalysisService.getRepositoryAnalysis(repositoryId, months);

        // Then
        assertNotNull(report);
        assertEquals(repositoryId, report.getRepositoryId());
        assertEquals(mockMetrics.size(), report.getTotalPRs());
        
        verify(prHistoricalMetricsRepository).findByRepositoryIdAndCreatedAtAfter(repositoryId, fromDate);
    }

    // Developer Learning Progress Tests
    
    @Test
    void testAssessDeveloperSkills_Success() {
        // Given
        Long developerId = 1L;
        
        DeveloperProfile mockProfile = createMockDeveloperProfile();
        when(developerProfileRepository.findByGithubUserId(developerId))
            .thenReturn(Optional.of(mockProfile));
        
        when(prHistoricalMetricsRepository.findByPrAuthorIdAndCreatedAtAfter(anyLong(), any()))
            .thenReturn(createMockPRMetrics());
        
        when(pullRequestReviewRepository.findByPrAuthorAndCompletedAtAfter(anyString(), any()))
            .thenReturn(createMockPullRequestReviews());

        // When
        assertDoesNotThrow(() -> learningProgressService.assessDeveloperSkills(developerId));

        // Then
        verify(developerProfileRepository).findByGithubUserId(developerId);
        verify(prHistoricalMetricsRepository).findByPrAuthorIdAndCreatedAtAfter(anyLong(), any());
    }

    @Test
    void testGetLearningReport_Success() {
        // Given
        Long developerId = 1L;
        int months = 6;
        
        DeveloperProfile mockProfile = createMockDeveloperProfile();
        when(developerProfileRepository.findByGithubUserId(developerId))
            .thenReturn(Optional.of(mockProfile));
        
        when(skillAssessmentRepository.findByDeveloperId(mockProfile.getId()))
            .thenReturn(createMockSkillAssessments());
        
        when(prHistoricalMetricsRepository.findByPrAuthorIdAndCreatedAtAfter(anyLong(), any()))
            .thenReturn(createMockPRMetrics());

        // When
        DeveloperLearningProgressService.DeveloperLearningReport report = 
            learningProgressService.getLearningReport(developerId, months);

        // Then
        assertNotNull(report);
        assertEquals(developerId, report.getDeveloperId());
        assertEquals(mockProfile, report.getDeveloperProfile());
        assertEquals(months, report.getReportPeriod());
        
        verify(developerProfileRepository).findByGithubUserId(developerId);
        verify(skillAssessmentRepository).findByDeveloperId(mockProfile.getId());
    }

    // Team Knowledge Base Tests
    
    @Test
    void testCreateKnowledgeEntry_Success() {
        // Given
        TeamKnowledgeBaseService.KnowledgeEntryRequest request = 
            TeamKnowledgeBaseService.KnowledgeEntryRequest.builder()
                .repositoryId(1L)
                .category("best_practice")
                .title("Test Knowledge Entry")
                .description("Test Description")
                .content("Test Content")
                .tags(Arrays.asList("java", "testing"))
                .severity(KnowledgeBaseEntry.Severity.MEDIUM)
                .createdByUserId(1L)
                .build();
        
        when(knowledgeBaseRepository.save(any(KnowledgeBaseEntry.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        KnowledgeBaseEntry result = knowledgeBaseService.createKnowledgeEntry(request);

        // Then
        assertNotNull(result);
        assertEquals(request.getRepositoryId(), result.getRepositoryId());
        assertEquals(request.getTitle(), result.getTitle());
        assertEquals(request.getCategory(), result.getCategory());
        
        verify(knowledgeBaseRepository).save(any(KnowledgeBaseEntry.class));
    }

    @Test
    void testGetKnowledgeBaseAnalytics_Success() {
        // Given
        Long repositoryId = 1L;
        
        List<KnowledgeBaseEntry> mockEntries = createMockKnowledgeEntries();
        when(knowledgeBaseRepository.findByRepositoryId(repositoryId))
            .thenReturn(mockEntries);

        // When
        TeamKnowledgeBaseService.KnowledgeBaseAnalytics analytics = 
            knowledgeBaseService.getKnowledgeBaseAnalytics(repositoryId);

        // Then
        assertNotNull(analytics);
        assertEquals(repositoryId, analytics.getRepositoryId());
        assertEquals(mockEntries.size(), analytics.getTotalEntries());
        
        verify(knowledgeBaseRepository).findByRepositoryId(repositoryId);
    }

    // Personalized Suggestion Engine Tests
    
    @Test
    void testGenerateSuggestions_Success() {
        // Given
        Long developerId = 1L;
        PersonalizedSuggestionEngine.SuggestionContext context = 
            PersonalizedSuggestionEngine.SuggestionContext.builder()
                .repositoryName("test-repo")
                .currentTask("code-review")
                .technologies(Arrays.asList("java", "spring"))
                .build();
        
        DeveloperProfile mockProfile = createMockDeveloperProfile();
        when(developerProfileRepository.findByGithubUserId(developerId))
            .thenReturn(Optional.of(mockProfile));
        
        when(skillAssessmentRepository.findByDeveloperId(mockProfile.getId()))
            .thenReturn(createMockSkillAssessments());
        
        when(prHistoricalMetricsRepository.findByPrAuthorIdAndCreatedAtAfter(anyLong(), any()))
            .thenReturn(createMockPRMetrics());

        // When
        List<PersonalizedSuggestionEngine.PersonalizedSuggestion> suggestions = 
            suggestionEngine.generateSuggestions(developerId, context);

        // Then
        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());
        
        verify(developerProfileRepository).findByGithubUserId(developerId);
        verify(skillAssessmentRepository).findByDeveloperId(mockProfile.getId());
    }

    // Historical Trend Analysis Tests
    
    @Test
    void testCalculateCodeQualityTrends_Success() {
        // Given
        Long repositoryId = 1L;
        LocalDate analysisDate = LocalDate.now();
        HistoricalTrendAnalysisService.PeriodType periodType = 
            HistoricalTrendAnalysisService.PeriodType.DAILY;
        
        List<PRHistoricalMetrics> mockMetrics = createMockPRMetrics();
        when(prHistoricalMetricsRepository.findByRepositoryIdAndCreatedAtBetween(
            anyLong(), any(), any()))
            .thenReturn(mockMetrics);

        // When
        assertDoesNotThrow(() -> trendAnalysisService.calculateCodeQualityTrends(
            repositoryId, analysisDate, periodType));

        // Then
        verify(prHistoricalMetricsRepository).findByRepositoryIdAndCreatedAtBetween(
            anyLong(), any(), any());
        verify(codeQualityTrendsRepository, atLeastOnce()).save(any(CodeQualityTrends.class));
    }

    @Test
    void testGetTrendAnalysisReport_Success() {
        // Given
        Long repositoryId = 1L;
        int months = 6;
        
        List<CodeQualityTrends> mockTrends = createMockCodeQualityTrends();
        when(codeQualityTrendsRepository.findByRepositoryIdAndAnalysisDateAfter(
            anyLong(), any()))
            .thenReturn(mockTrends);

        // When
        HistoricalTrendAnalysisService.TrendAnalysisReport report = 
            trendAnalysisService.getTrendAnalysisReport(repositoryId, months);

        // Then
        assertNotNull(report);
        assertEquals(repositoryId, report.getRepositoryId());
        
        verify(codeQualityTrendsRepository).findByRepositoryIdAndAnalysisDateAfter(
            anyLong(), any());
    }

    // Helper methods to create mock objects
    
    private Object createMockPullRequest() {
        // Create mock pull request object
        return new Object() {
            public Long getId() { return 1L; }
            public String getTitle() { return "Test PR"; }
            public String getBody() { return "Test PR body"; }
            public int getAdditions() { return 100; }
            public int getDeletions() { return 50; }
            public int getChangedFiles() { return 5; }
            public int getComments() { return 3; }
            public int getCommits() { return 2; }
            public LocalDateTime getCreatedAt() { return LocalDateTime.now().minusDays(2); }
            public LocalDateTime getMergedAt() { return LocalDateTime.now().minusDays(1); }
            public Object getUser() { 
                return new Object() {
                    public Long getId() { return 1L; }
                    public String getLogin() { return "test-user"; }
                };
            }
            public List<Object> getFiles() { return Arrays.asList(); }
        };
    }

    private List<PRHistoricalMetrics> createMockPRMetrics() {
        PRHistoricalMetrics metrics = PRHistoricalMetrics.builder()
            .id(1L)
            .repositoryId(1L)
            .prNumber(123)
            .prAuthorId(1L)
            .prSize(PRHistoricalMetrics.PRSize.MEDIUM)
            .complexityScore(BigDecimal.valueOf(65.5))
            .codeQualityScore(BigDecimal.valueOf(78.2))
            .testCoverageChange(BigDecimal.valueOf(5.0))
            .reviewTurnaroundHours(24)
            .mergeTimeHours(48)
            .commentCount(3)
            .approvalCount(2)
            .changeRequestCount(1)
            .filesChanged(5)
            .linesAdded(100)
            .linesDeleted(50)
            .commitCount(3)
            .isHotfix(false)
            .isFeature(true)
            .isRefactor(false)
            .isBugfix(false)
            .mergeConflicts(false)
            .ciFailures(0)
            .createdAt(LocalDateTime.now())
            .build();
        
        return Arrays.asList(metrics);
    }

    private DeveloperProfile createMockDeveloperProfile() {
        return DeveloperProfile.builder()
            .id(1L)
            .githubUserId(1L)
            .githubUsername("test-user")
            .displayName("Test User")
            .experienceLevel(DeveloperProfile.ExperienceLevel.INTERMEDIATE)
            .primaryLanguages(Arrays.asList("java", "javascript"))
            .domainExpertise(Arrays.asList("backend", "testing"))
            .communicationStyle(DeveloperProfile.CommunicationStyle.STANDARD)
            .learningPreferences(new HashMap<>())
            .timezone("UTC")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    private List<DeveloperSkillAssessment> createMockSkillAssessments() {
        DeveloperSkillAssessment skill1 = DeveloperSkillAssessment.builder()
            .id(1L)
            .developerId(1L)
            .skillCategory("java")
            .skillLevel(DeveloperSkillAssessment.SkillLevel.PROFICIENT)
            .confidenceScore(BigDecimal.valueOf(85.0))
            .evidenceCount(15)
            .lastDemonstrationDate(LocalDateTime.now().minusDays(5))
            .improvementTrend(DeveloperSkillAssessment.ImprovementTrend.IMPROVING)
            .learningGoals(Arrays.asList("Learn advanced Java features"))
            .recommendedResources(Arrays.asList("Java documentation", "Online course"))
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        
        return Arrays.asList(skill1);
    }

    private List<PullRequestReview> createMockPullRequestReviews() {
        // Create mock pull request reviews
        return Arrays.asList();
    }

    private List<KnowledgeBaseEntry> createMockKnowledgeEntries() {
        KnowledgeBaseEntry entry = KnowledgeBaseEntry.builder()
            .id(1L)
            .repositoryId(1L)
            .category("best_practice")
            .title("Test Knowledge Entry")
            .description("Test Description")
            .content("Test Content")
            .tags(Arrays.asList("java", "testing"))
            .severity(KnowledgeBaseEntry.Severity.MEDIUM)
            .frequencyCount(3)
            .effectivenessScore(BigDecimal.valueOf(75.0))
            .sourceReviewIds(Arrays.asList(1L, 2L))
            .createdByUserId(1L)
            .language("java")
            .framework("spring")
            .isApproved(true)
            .approvalDate(LocalDateTime.now())
            .approvedByUserId(2L)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        
        return Arrays.asList(entry);
    }

    private List<CodeQualityTrends> createMockCodeQualityTrends() {
        CodeQualityTrends trend = CodeQualityTrends.builder()
            .id(1L)
            .repositoryId(1L)
            .analysisDate(LocalDate.now())
            .periodType("daily")
            .metricName("average_complexity")
            .metricValue(BigDecimal.valueOf(65.5))
            .trendDirection("stable")
            .changePercentage(BigDecimal.valueOf(2.3))
            .developerCount(5)
            .prCount(10)
            .issueCount(3)
            .baselineValue(BigDecimal.valueOf(63.2))
            .targetValue(BigDecimal.valueOf(60.0))
            .createdAt(LocalDateTime.now())
            .build();
        
        return Arrays.asList(trend);
    }
}