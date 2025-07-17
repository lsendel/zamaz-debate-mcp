package com.zamaz.mcp.github.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.zamaz.mcp.github.entity.*;
import com.zamaz.mcp.github.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive End-to-End tests for GitHub Integration
 * Tests the complete workflow from webhook to PR review completion
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GitHubIntegrationE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("github_integration_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    static WireMockServer wireMockServer;

    @Autowired
    private MockMvc mockMvc;

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
        wireMockServer.resetAll();
    }

    @Test
    @Order(1)
    @DisplayName("Complete PR Review Workflow - Happy Path")
    @Transactional
    void testCompleteePullRequestReviewWorkflow() throws Exception {
        // 1. Setup GitHub API mocks
        setupGitHubApiMocks();

        // 2. Create installation and repository configuration
        GitHubInstallation installation = createTestInstallation();
        RepositoryConfig config = createTestRepositoryConfig(installation);

        // 3. Send pull request opened webhook
        Map<String, Object> webhook = createPullRequestOpenedWebhook(installation.getId());
        
        mockMvc.perform(post("/api/v1/webhooks/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-GitHub-Event", "pull_request")
                .header("X-GitHub-Delivery", "test-delivery-1")
                .header("X-GitHub-Signature-256", "sha256=test-signature")
                .content(objectMapper.writeValueAsString(webhook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(true))
                .andExpect(jsonPath("$.eventType").value("pull_request"))
                .andExpect(jsonPath("$.status").value("REVIEW_STARTED"));

        // 4. Verify review record was created
        List<PullRequestReview> reviews = reviewRepository.findByInstallationId(installation.getId());
        assertThat(reviews).hasSize(1);
        PullRequestReview review = reviews.get(0);
        assertThat(review.getStatus()).isEqualTo("PENDING");
        assertThat(review.getPrNumber()).isEqualTo(123);
        assertThat(review.getRepositoryFullName()).isEqualTo("test-owner/test-repo");

        // 5. Wait for async processing and verify completion
        Thread.sleep(2000); // Allow time for async processing
        
        review = reviewRepository.findById(review.getId()).orElseThrow();
        assertThat(review.getStatus()).isEqualTo("COMPLETED");

        // 6. Verify review results were saved
        List<ReviewComment> comments = commentRepository.findByReviewId(review.getId());
        assertThat(comments).isNotEmpty();

        List<ReviewIssue> issues = issueRepository.findByReviewId(review.getId());
        assertThat(issues).isNotEmpty();

        // 7. Verify GitHub API was called to post comments
        verify(postRequestedFor(urlEqualTo("/repos/test-owner/test-repo/issues/123/comments")));
    }

    @Test
    @Order(2)
    @DisplayName("Installation Webhook Processing")
    @Transactional
    void testInstallationWebhookProcessing() throws Exception {
        // 1. Send installation created webhook
        Map<String, Object> webhook = createInstallationCreatedWebhook();
        
        mockMvc.perform(post("/api/v1/webhooks/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-GitHub-Event", "installation")
                .header("X-GitHub-Delivery", "test-delivery-2")
                .header("X-GitHub-Signature-256", "sha256=test-signature")
                .content(objectMapper.writeValueAsString(webhook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(true))
                .andExpect(jsonPath("$.message").value("Installation created"));

        // 2. Verify installation was saved
        GitHubInstallation installation = installationRepository.findById(12345L).orElseThrow();
        assertThat(installation.getAccountLogin()).isEqualTo("test-user");
        assertThat(installation.getStatus()).isEqualTo("ACTIVE");
        assertThat(installation.getAccountType()).isEqualTo("User");

        // 3. Send installation deleted webhook
        Map<String, Object> deleteWebhook = createInstallationDeletedWebhook();
        
        mockMvc.perform(post("/api/v1/webhooks/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-GitHub-Event", "installation")
                .header("X-GitHub-Delivery", "test-delivery-3")
                .header("X-GitHub-Signature-256", "sha256=test-signature")
                .content(objectMapper.writeValueAsString(deleteWebhook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(true))
                .andExpect(jsonPath("$.message").value("Installation deleted"));

        // 4. Verify installation was deleted
        assertThat(installationRepository.findById(12345L)).isEmpty();
    }

    @Test
    @Order(3)
    @DisplayName("PR Closed Webhook Processing")
    @Transactional
    void testPullRequestClosedWebhookProcessing() throws Exception {
        // 1. Setup test data
        GitHubInstallation installation = createTestInstallation();
        RepositoryConfig config = createTestRepositoryConfig(installation);
        PullRequestReview review = createTestReview(installation);

        // 2. Send PR closed webhook
        Map<String, Object> webhook = createPullRequestClosedWebhook(installation.getId(), true);
        
        mockMvc.perform(post("/api/v1/webhooks/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-GitHub-Event", "pull_request")
                .header("X-GitHub-Delivery", "test-delivery-4")
                .header("X-GitHub-Signature-256", "sha256=test-signature")
                .content(objectMapper.writeValueAsString(webhook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(true))
                .andExpect(jsonPath("$.status").value("MERGED"));

        // 3. Verify review status was updated
        review = reviewRepository.findById(review.getId()).orElseThrow();
        assertThat(review.getStatus()).isEqualTo("MERGED");
    }

    @Test
    @Order(4)
    @DisplayName("Configuration Changes Impact")
    @Transactional
    void testConfigurationChangesImpact() throws Exception {
        // 1. Setup installation with auto-review disabled
        GitHubInstallation installation = createTestInstallation();
        RepositoryConfig config = createTestRepositoryConfig(installation);
        config.setAutoReviewEnabled(false);
        configRepository.save(config);

        // 2. Send PR opened webhook
        Map<String, Object> webhook = createPullRequestOpenedWebhook(installation.getId());
        
        mockMvc.perform(post("/api/v1/webhooks/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-GitHub-Event", "pull_request")
                .header("X-GitHub-Delivery", "test-delivery-5")
                .header("X-GitHub-Signature-256", "sha256=test-signature")
                .content(objectMapper.writeValueAsString(webhook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(true))
                .andExpect(jsonPath("$.message").value("Auto-review not enabled"));

        // 3. Verify no review was created
        List<PullRequestReview> reviews = reviewRepository.findByInstallationId(installation.getId());
        assertThat(reviews).isEmpty();

        // 4. Enable auto-review and test again
        config.setAutoReviewEnabled(true);
        configRepository.save(config);

        mockMvc.perform(post("/api/v1/webhooks/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-GitHub-Event", "pull_request")
                .header("X-GitHub-Delivery", "test-delivery-6")
                .header("X-GitHub-Signature-256", "sha256=test-signature")
                .content(objectMapper.writeValueAsString(webhook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(true))
                .andExpect(jsonPath("$.status").value("REVIEW_STARTED"));

        // 5. Verify review was created this time
        reviews = reviewRepository.findByInstallationId(installation.getId());
        assertThat(reviews).hasSize(1);
    }

    @Test
    @Order(5)
    @DisplayName("Analytics Data Collection")
    @Transactional
    void testAnalyticsDataCollection() throws Exception {
        // 1. Setup test data
        GitHubInstallation installation = createTestInstallation();
        RepositoryConfig config = createTestRepositoryConfig(installation);
        
        // 2. Create multiple reviews with different outcomes
        setupMultipleReviews(installation);

        // 3. Test webhook stats endpoint
        mockMvc.perform(get("/api/v1/webhooks/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents").exists());

        // 4. Verify analytics data
        List<PullRequestReview> completedReviews = reviewRepository.findByStatus("COMPLETED");
        assertThat(completedReviews).isNotEmpty();

        List<ReviewIssue> highSeverityIssues = issueRepository.findHighSeverityIssues();
        assertThat(highSeverityIssues).isNotEmpty();

        // 5. Test review statistics
        Long pendingCount = reviewRepository.countByStatus("PENDING");
        Long completedCount = reviewRepository.countByStatus("COMPLETED");
        assertThat(pendingCount + completedCount).isGreaterThan(0);
    }

    @Test
    @Order(6)
    @DisplayName("Error Scenarios and Recovery")
    @Transactional
    void testErrorScenariosAndRecovery() throws Exception {
        // 1. Test invalid webhook signature
        Map<String, Object> webhook = createPullRequestOpenedWebhook(12345L);
        
        mockMvc.perform(post("/api/v1/webhooks/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-GitHub-Event", "pull_request")
                .header("X-GitHub-Delivery", "test-delivery-7")
                .header("X-GitHub-Signature-256", "sha256=invalid-signature")
                .content(objectMapper.writeValueAsString(webhook)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid signature"));

        // 2. Test unknown installation
        mockMvc.perform(post("/api/v1/webhooks/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-GitHub-Event", "pull_request")
                .header("X-GitHub-Delivery", "test-delivery-8")
                .header("X-GitHub-Signature-256", "sha256=test-signature")
                .content(objectMapper.writeValueAsString(webhook)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(true))
                .andExpect(jsonPath("$.error").value("Installation not found"));

        // 3. Test GitHub API failure
        setupGitHubApiErrorMocks();
        
        GitHubInstallation installation = createTestInstallation();
        RepositoryConfig config = createTestRepositoryConfig(installation);
        
        webhook = createPullRequestOpenedWebhook(installation.getId());
        
        mockMvc.perform(post("/api/v1/webhooks/github")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-GitHub-Event", "pull_request")
                .header("X-GitHub-Delivery", "test-delivery-9")
                .header("X-GitHub-Signature-256", "sha256=test-signature")
                .content(objectMapper.writeValueAsString(webhook)))
                .andExpect(status().isOk());

        // 4. Verify error handling
        Thread.sleep(2000); // Allow time for async processing
        
        List<PullRequestReview> reviews = reviewRepository.findByInstallationId(installation.getId());
        if (!reviews.isEmpty()) {
            PullRequestReview review = reviews.get(0);
            assertThat(review.getStatus()).isEqualTo("FAILED");
        }
    }

    @Test
    @Order(7)
    @DisplayName("Webhook Health and Monitoring")
    void testWebhookHealthAndMonitoring() throws Exception {
        // 1. Test health endpoint
        mockMvc.perform(get("/api/v1/webhooks/health"))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.status").value("healthy"))
                .andExpected(jsonPath("$.service").value("github-webhook"));

        // 2. Test stats endpoint
        mockMvc.perform(get("/api/v1/webhooks/stats"))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.totalEvents").exists());

        // 3. Test webhook endpoint
        Map<String, Object> testPayload = Map.of("test", "payload");
        
        mockMvc.perform(post("/api/v1/webhooks/test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testPayload)))
                .andExpect(status().isOk())
                .andExpected(jsonPath("$.status").value("received"))
                .andExpected(jsonPath("$.payload.test").value("payload"));
    }

    // Helper methods for creating test data

    private GitHubInstallation createTestInstallation() {
        GitHubInstallation installation = new GitHubInstallation();
        installation.setId(12345L);
        installation.setAccountLogin("test-user");
        installation.setAccountType("User");
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

    private PullRequestReview createTestReview(GitHubInstallation installation) {
        PullRequestReview review = new PullRequestReview();
        review.setInstallationId(installation.getId());
        review.setRepositoryFullName("test-owner/test-repo");
        review.setPrNumber(123);
        review.setPrTitle("Test PR");
        review.setPrAuthor("test-author");
        review.setStatus("PENDING");
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        return reviewRepository.save(review);
    }

    private void setupMultipleReviews(GitHubInstallation installation) {
        for (int i = 1; i <= 5; i++) {
            PullRequestReview review = new PullRequestReview();
            review.setInstallationId(installation.getId());
            review.setRepositoryFullName("test-owner/test-repo");
            review.setPrNumber(100 + i);
            review.setPrTitle("Test PR " + i);
            review.setPrAuthor("test-author");
            review.setStatus(i % 2 == 0 ? "COMPLETED" : "PENDING");
            review.setCreatedAt(LocalDateTime.now());
            review.setUpdatedAt(LocalDateTime.now());
            review = reviewRepository.save(review);

            if ("COMPLETED".equals(review.getStatus())) {
                ReviewIssue issue = new ReviewIssue();
                issue.setReview(review);
                issue.setIssueType("TEST_ISSUE");
                issue.setDescription("Test issue " + i);
                issue.setSeverity(i % 3 == 0 ? "HIGH" : "MEDIUM");
                issue.setStatus("OPEN");
                issue.setCreatedAt(LocalDateTime.now());
                issue.setUpdatedAt(LocalDateTime.now());
                issueRepository.save(issue);
            }
        }
    }

    private void setupGitHubApiMocks() {
        // Mock PR details
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo/pulls/123"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createPullRequestResponse())));

        // Mock post comment
        stubFor(post(urlEqualTo("/repos/test-owner/test-repo/issues/123/comments"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createCommentResponse())));
    }

    private void setupGitHubApiErrorMocks() {
        // Mock API errors
        stubFor(get(urlMatching("/repos/.*"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        stubFor(post(urlMatching("/repos/.*/issues/.*/comments"))
                .willReturn(aResponse()
                        .withStatus(403)
                        .withBody("Forbidden")));
    }

    private Map<String, Object> createPullRequestOpenedWebhook(Long installationId) {
        Map<String, Object> webhook = new HashMap<>();
        webhook.put("action", "opened");
        webhook.put("installation", Map.of("id", installationId));
        
        webhook.put("pull_request", Map.of(
            "id", 1L,
            "number", 123,
            "title", "Test PR",
            "body", "Test PR description",
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
        Map<String, Object> webhook = createPullRequestOpenedWebhook(installationId);
        webhook.put("action", "closed");
        ((Map<String, Object>) webhook.get("pull_request")).put("merged", merged);
        ((Map<String, Object>) webhook.get("pull_request")).put("state", "closed");
        return webhook;
    }

    private Map<String, Object> createInstallationCreatedWebhook() {
        Map<String, Object> webhook = new HashMap<>();
        webhook.put("action", "created");
        webhook.put("installation", Map.of(
            "id", 12345L,
            "account", Map.of(
                "login", "test-user",
                "type", "User"
            )
        ));
        return webhook;
    }

    private Map<String, Object> createInstallationDeletedWebhook() {
        Map<String, Object> webhook = new HashMap<>();
        webhook.put("action", "deleted");
        webhook.put("installation", Map.of(
            "id", 12345L,
            "account", Map.of(
                "login", "test-user",
                "type", "User"
            )
        ));
        return webhook;
    }

    private String createPullRequestResponse() {
        return """
            {
                "id": 1,
                "number": 123,
                "title": "Test PR",
                "body": "Test PR description",
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
                "body": "Automated review comment",
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