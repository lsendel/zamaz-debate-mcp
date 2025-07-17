package com.zamaz.mcp.github.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.zamaz.mcp.github.entity.*;
import com.zamaz.mcp.github.repository.*;
import com.zamaz.mcp.github.service.PullRequestAnalyzer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Specialized E2E tests for Pull Request Review Flow
 * Tests complex scenarios, async processing, and edge cases
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PullRequestReviewFlowE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("github_pr_flow_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    static WireMockServer wireMockServer;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GitHubInstallationRepository installationRepository;

    @Autowired
    private RepositoryConfigRepository configRepository;

    @Autowired
    private PullRequestReviewRepository reviewRepository;

    @Autowired
    private ReviewCommentRepository commentRepository;

    @Autowired
    private ReviewIssueRepository issueRepository;

    @SpyBean
    private PullRequestAnalyzer pullRequestAnalyzer;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
        registry.add("github.api.base-url", () -> "http://localhost:" + wireMockServer.port());
        registry.add("github.webhook.secret", () -> "test-secret");
    }

    @BeforeAll
    static void setUp() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void tearDown() {
        wireMockServer.stop();
    }

    @BeforeEach
    void beforeEach() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        wireMockServer.resetAll();
    }

    @Test
    @Order(1)
    @DisplayName("Complete PR Review Flow with Multiple Updates")
    @Transactional
    void testCompleteReviewFlowWithMultipleUpdates() throws Exception {
        // 1. Setup test data
        GitHubInstallation installation = createTestInstallation();
        RepositoryConfig config = createTestRepositoryConfig(installation);
        setupGitHubApiMocks();

        // 2. Initial PR opened
        Map<String, Object> openedWebhook = createPullRequestWebhook(installation.getId(), "opened");
        
        mockMvc.perform(post("/api/v1/webhooks/github")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .header("X-GitHub-Event", "pull_request")
                .header("X-GitHub-Delivery", "delivery-1")
                .header("X-GitHub-Signature-256", "sha256=test-signature")
                .content(objectMapper.writeValueAsString(openedWebhook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEW_STARTED"));

        // 3. Wait for initial review completion
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<PullRequestReview> reviews = reviewRepository.findByInstallationId(installation.getId());
                    assertThat(reviews).hasSize(1);
                    assertThat(reviews.get(0).getStatus()).isEqualTo("COMPLETED");
                });

        // 4. PR synchronized (new commits)
        Map<String, Object> syncWebhook = createPullRequestWebhook(installation.getId(), "synchronize");
        
        mockMvc.perform(post("/api/v1/webhooks/github")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .header("X-GitHub-Event", "pull_request")
                .header("X-GitHub-Delivery", "delivery-2")
                .header("X-GitHub-Signature-256", "sha256=test-signature")
                .content(objectMapper.writeValueAsString(syncWebhook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEW_STARTED"));

        // 5. Wait for updated review completion
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<PullRequestReview> reviews = reviewRepository.findByInstallationId(installation.getId());
                    assertThat(reviews).hasSize(1);
                    PullRequestReview review = reviews.get(0);
                    assertThat(review.getStatus()).isEqualTo("COMPLETED");
                });

        // 6. PR closed and merged
        Map<String, Object> closedWebhook = createPullRequestClosedWebhook(installation.getId(), true);
        
        mockMvc.perform(post("/api/v1/webhooks/github")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .header("X-GitHub-Event", "pull_request")
                .header("X-GitHub-Delivery", "delivery-3")
                .header("X-GitHub-Signature-256", "sha256=test-signature")
                .content(objectMapper.writeValueAsString(closedWebhook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("MERGED"));

        // 7. Verify final state
        List<PullRequestReview> reviews = reviewRepository.findByInstallationId(installation.getId());
        assertThat(reviews).hasSize(1);
        PullRequestReview review = reviews.get(0);
        assertThat(review.getStatus()).isEqualTo("MERGED");

        // 8. Verify analyzer was called multiple times
        verify(pullRequestAnalyzer, times(2)).analyzePullRequestAsync(
                anyString(), anyString(), anyString(), anyInt(), anyLong());

        // 9. Verify GitHub API interactions
        verify(2, getRequestedFor(urlEqualTo("/repos/test-owner/test-repo/pulls/123")));
        verify(2, postRequestedFor(urlEqualTo("/repos/test-owner/test-repo/issues/123/comments")));
    }

    @Test
    @Order(2)
    @DisplayName("Concurrent PR Processing")
    @Transactional
    void testConcurrentPullRequestProcessing() throws Exception {
        // 1. Setup test data
        GitHubInstallation installation = createTestInstallation();
        RepositoryConfig config = createTestRepositoryConfig(installation);
        setupGitHubApiMocks();

        // 2. Send multiple PR webhooks concurrently
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> webhook = createPullRequestWebhook(installation.getId(), "opened");
            ((Map<String, Object>) webhook.get("pull_request")).put("number", 100 + i);
            
            mockMvc.perform(post("/api/v1/webhooks/github")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .header("X-GitHub-Event", "pull_request")
                    .header("X-GitHub-Delivery", "delivery-" + i)
                    .header("X-GitHub-Signature-256", "sha256=test-signature")
                    .content(objectMapper.writeValueAsString(webhook)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REVIEW_STARTED"));
        }

        // 3. Wait for all reviews to complete
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<PullRequestReview> reviews = reviewRepository.findByInstallationId(installation.getId());
                    assertThat(reviews).hasSize(5);
                    assertThat(reviews.stream().allMatch(r -> "COMPLETED".equals(r.getStatus()))).isTrue();
                });

        // 4. Verify all reviews have comments and issues
        List<PullRequestReview> reviews = reviewRepository.findByInstallationId(installation.getId());
        for (PullRequestReview review : reviews) {
            List<ReviewComment> comments = commentRepository.findByReviewId(review.getId());
            assertThat(comments).isNotEmpty();
            
            List<ReviewIssue> issues = issueRepository.findByReviewId(review.getId());
            assertThat(issues).isNotEmpty();
        }
    }

    @Test
    @Order(3)
    @DisplayName("Review Analysis with Different Issue Types")
    @Transactional
    void testReviewAnalysisWithDifferentIssueTypes() throws Exception {
        // 1. Setup test data
        GitHubInstallation installation = createTestInstallation();
        RepositoryConfig config = createTestRepositoryConfig(installation);
        setupGitHubApiMocks();

        // 2. Create PR with specific title and body to trigger different analysis results
        Map<String, Object> webhook = createPullRequestWebhook(installation.getId(), "opened");
        Map<String, Object> pr = (Map<String, Object>) webhook.get("pull_request");
        pr.put("title", "fix"); // Short title to trigger title issue
        pr.put("body", ""); // Empty body to trigger description issue
        
        mockMvc.perform(post("/api/v1/webhooks/github")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .header("X-GitHub-Event", "pull_request")
                .header("X-GitHub-Delivery", "delivery-analysis")
                .header("X-GitHub-Signature-256", "sha256=test-signature")
                .content(objectMapper.writeValueAsString(webhook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEW_STARTED"));

        // 3. Wait for analysis completion
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<PullRequestReview> reviews = reviewRepository.findByInstallationId(installation.getId());
                    assertThat(reviews).hasSize(1);
                    assertThat(reviews.get(0).getStatus()).isEqualTo("COMPLETED");
                });

        // 4. Verify specific issues were detected
        List<PullRequestReview> reviews = reviewRepository.findByInstallationId(installation.getId());
        PullRequestReview review = reviews.get(0);
        
        List<ReviewIssue> issues = issueRepository.findByReviewId(review.getId());
        assertThat(issues).isNotEmpty();
        
        // Check for title and description issues
        assertThat(issues.stream().anyMatch(i -> i.getIssueType().equals("TITLE_TOO_SHORT"))).isTrue();
        assertThat(issues.stream().anyMatch(i -> i.getIssueType().equals("DESCRIPTION_EMPTY"))).isTrue();
        
        // Check severity distribution
        long highSeverityCount = issues.stream().filter(i -> "HIGH".equals(i.getSeverity())).count();
        long mediumSeverityCount = issues.stream().filter(i -> "MEDIUM".equals(i.getSeverity())).count();
        assertThat(highSeverityCount + mediumSeverityCount).isGreaterThan(0);
    }

    @Test
    @Order(4)
    @DisplayName("Review Failure and Recovery")
    @Transactional
    void testReviewFailureAndRecovery() throws Exception {
        // 1. Setup test data
        GitHubInstallation installation = createTestInstallation();
        RepositoryConfig config = createTestRepositoryConfig(installation);
        
        // 2. Setup GitHub API to return errors
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo/pulls/123"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // 3. Send PR webhook - should fail
        Map<String, Object> webhook = createPullRequestWebhook(installation.getId(), "opened");
        
        mockMvc.perform(post("/api/v1/webhooks/github")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .header("X-GitHub-Event", "pull_request")
                .header("X-GitHub-Delivery", "delivery-failure")
                .header("X-GitHub-Signature-256", "sha256=test-signature")
                .content(objectMapper.writeValueAsString(webhook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEW_STARTED"));

        // 4. Wait for failure
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<PullRequestReview> reviews = reviewRepository.findByInstallationId(installation.getId());
                    assertThat(reviews).hasSize(1);
                    assertThat(reviews.get(0).getStatus()).isEqualTo("FAILED");
                });

        // 5. Fix API and retry
        wireMockServer.resetAll();
        setupGitHubApiMocks();
        
        // 6. Send synchronize webhook to retry
        Map<String, Object> retryWebhook = createPullRequestWebhook(installation.getId(), "synchronize");
        
        mockMvc.perform(post("/api/v1/webhooks/github")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .header("X-GitHub-Event", "pull_request")
                .header("X-GitHub-Delivery", "delivery-retry")
                .header("X-GitHub-Signature-256", "sha256=test-signature")
                .content(objectMapper.writeValueAsString(retryWebhook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEW_STARTED"));

        // 7. Wait for successful completion
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<PullRequestReview> reviews = reviewRepository.findByInstallationId(installation.getId());
                    assertThat(reviews).hasSize(1);
                    assertThat(reviews.get(0).getStatus()).isEqualTo("COMPLETED");
                });
    }

    @Test
    @Order(5)
    @DisplayName("Branch Pattern Filtering")
    @Transactional
    void testBranchPatternFiltering() throws Exception {
        // 1. Setup test data with specific branch patterns
        GitHubInstallation installation = createTestInstallation();
        RepositoryConfig config = createTestRepositoryConfig(installation);
        config.setBranchPatterns("main,develop,feature/*");
        configRepository.save(config);

        // 2. Test PR against allowed branch (main)
        Map<String, Object> webhook = createPullRequestWebhook(installation.getId(), "opened");
        ((Map<String, Object>) ((Map<String, Object>) webhook.get("pull_request")).get("base")).put("ref", "main");
        
        mockMvc.perform(post("/api/v1/webhooks/github")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .header("X-GitHub-Event", "pull_request")
                .header("X-GitHub-Delivery", "delivery-main")
                .header("X-GitHub-Signature-256", "sha256=test-signature")
                .content(objectMapper.writeValueAsString(webhook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVIEW_STARTED"));

        // 3. Test PR against disallowed branch (hotfix)
        webhook = createPullRequestWebhook(installation.getId(), "opened");
        ((Map<String, Object>) webhook.get("pull_request")).put("number", 124);
        ((Map<String, Object>) ((Map<String, Object>) webhook.get("pull_request")).get("base")).put("ref", "hotfix");
        
        mockMvc.perform(post("/api/v1/webhooks/github")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .header("X-GitHub-Event", "pull_request")
                .header("X-GitHub-Delivery", "delivery-hotfix")
                .header("X-GitHub-Signature-256", "sha256=test-signature")
                .content(objectMapper.writeValueAsString(webhook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Auto-review not enabled"));

        // 4. Verify only one review was created
        List<PullRequestReview> reviews = reviewRepository.findByInstallationId(installation.getId());
        assertThat(reviews).hasSize(1);
        assertThat(reviews.get(0).getPrNumber()).isEqualTo(123);
    }

    @Test
    @Order(6)
    @DisplayName("Review Comment Management")
    @Transactional
    void testReviewCommentManagement() throws Exception {
        // 1. Setup test data
        GitHubInstallation installation = createTestInstallation();
        RepositoryConfig config = createTestRepositoryConfig(installation);
        setupGitHubApiMocks();

        // 2. Create initial review
        Map<String, Object> webhook = createPullRequestWebhook(installation.getId(), "opened");
        
        mockMvc.perform(post("/api/v1/webhooks/github")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .header("X-GitHub-Event", "pull_request")
                .header("X-GitHub-Delivery", "delivery-initial")
                .header("X-GitHub-Signature-256", "sha256=test-signature")
                .content(objectMapper.writeValueAsString(webhook)))
                .andExpect(status().isOk());

        // 3. Wait for completion
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<PullRequestReview> reviews = reviewRepository.findByInstallationId(installation.getId());
                    assertThat(reviews).hasSize(1);
                    assertThat(reviews.get(0).getStatus()).isEqualTo("COMPLETED");
                });

        // 4. Verify comments were created
        List<PullRequestReview> reviews = reviewRepository.findByInstallationId(installation.getId());
        PullRequestReview review = reviews.get(0);
        
        List<ReviewComment> comments = commentRepository.findByReviewId(review.getId());
        assertThat(comments).isNotEmpty();
        
        // 5. Verify comment types
        assertThat(comments.stream().anyMatch(c -> "REVIEW".equals(c.getCommentType()))).isTrue();
        
        // 6. Verify GitHub API calls
        verify(postRequestedFor(urlEqualTo("/repos/test-owner/test-repo/issues/123/comments")));
    }

    // Helper methods

    private GitHubInstallation createTestInstallation() {
        GitHubInstallation installation = new GitHubInstallation();
        installation.setId(67890L);
        installation.setAccountLogin("test-org");
        installation.setAccountType("Organization");
        installation.setStatus("ACTIVE");
        installation.setAccessToken("test-token");
        installation.setCreatedAt(LocalDateTime.now());
        installation.setUpdatedAt(LocalDateTime.now());
        return installationRepository.save(installation);
    }

    private RepositoryConfig createTestRepositoryConfig(GitHubInstallation installation) {
        RepositoryConfig config = new RepositoryConfig();
        config.setInstallationId(installation.getId());
        config.setRepositoryFullName("test-owner/test-repo");
        config.setAutoReviewEnabled(true);
        config.setNotificationsEnabled(true);
        config.setBranchPatterns("main,develop");
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        return configRepository.save(config);
    }

    private Map<String, Object> createPullRequestWebhook(Long installationId, String action) {
        Map<String, Object> webhook = new HashMap<>();
        webhook.put("action", action);
        webhook.put("installation", Map.of("id", installationId));
        
        webhook.put("pull_request", Map.of(
            "id", 1L,
            "number", 123,
            "title", "feat: Add new feature",
            "body", "This PR adds a new feature with comprehensive tests and documentation.",
            "state", "open",
            "html_url", "https://github.com/test-owner/test-repo/pull/123",
            "user", Map.of("login", "test-author"),
            "head", Map.of("ref", "feature-branch"),
            "base", Map.of("ref", "main")
        ));
        
        webhook.put("repository", Map.of(
            "id", 1L,
            "name", "test-repo",
            "full_name", "test-owner/test-repo",
            "html_url", "https://github.com/test-owner/test-repo",
            "owner", Map.of("login", "test-owner")
        ));
        
        return webhook;
    }

    private Map<String, Object> createPullRequestClosedWebhook(Long installationId, boolean merged) {
        Map<String, Object> webhook = createPullRequestWebhook(installationId, "closed");
        ((Map<String, Object>) webhook.get("pull_request")).put("merged", merged);
        ((Map<String, Object>) webhook.get("pull_request")).put("state", "closed");
        return webhook;
    }

    private void setupGitHubApiMocks() {
        stubFor(get(urlMatching("/repos/test-owner/test-repo/pulls/\\d+"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createPullRequestResponse())));

        stubFor(post(urlMatching("/repos/test-owner/test-repo/issues/\\d+/comments"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createCommentResponse())));
    }

    private String createPullRequestResponse() {
        return """
            {
                "id": 1,
                "number": 123,
                "title": "feat: Add new feature",
                "body": "This PR adds a new feature with comprehensive tests and documentation.",
                "state": "open",
                "html_url": "https://github.com/test-owner/test-repo/pull/123",
                "user": {
                    "id": 1,
                    "login": "test-author",
                    "avatar_url": "https://github.com/test-author.png",
                    "html_url": "https://github.com/test-author",
                    "type": "User"
                },
                "head": {
                    "ref": "feature-branch"
                },
                "base": {
                    "ref": "main"
                },
                "created_at": "2024-01-01T00:00:00Z",
                "updated_at": "2024-01-01T00:00:00Z"
            }
            """;
    }

    private String createCommentResponse() {
        return """
            {
                "id": 1,
                "body": "## 🤖 Automated Code Review\\n\\n### Issues Found:\\n- ⚠️ **TITLE_TOO_SHORT**: PR title is too short\\n- 🚨 **DESCRIPTION_EMPTY**: PR description is empty\\n\\n### Additional Comments:\\n- Code structure looks good overall\\n- Consider adding unit tests for the new functionality\\n\\n---\\n*Generated by Kiro GitHub Integration*",
                "html_url": "https://github.com/test-owner/test-repo/pull/123#issuecomment-1",
                "user": {
                    "id": 1,
                    "login": "github-actions[bot]",
                    "type": "Bot"
                },
                "created_at": "2024-01-01T00:00:00Z",
                "updated_at": "2024-01-01T00:00:00Z"
            }
            """;
    }
}