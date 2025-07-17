package com.zamaz.mcp.github.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.zamaz.mcp.github.entity.GitHubInstallation;
import com.zamaz.mcp.github.entity.PullRequestReview;
import com.zamaz.mcp.github.entity.RepositoryConfig;
import com.zamaz.mcp.github.repository.GitHubInstallationRepository;
import com.zamaz.mcp.github.repository.PullRequestReviewRepository;
import com.zamaz.mcp.github.repository.RepositoryConfigRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Performance and Load Testing for GitHub Integration
 * Tests system behavior under high load and stress conditions
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PerformanceE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("github_performance_test")
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

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
        registry.add("github.api.base-url", () -> "http://localhost:" + wireMockServer.port());
        registry.add("github.webhook.secret", () -> "test-secret");
        registry.add("spring.task.execution.pool.core-size", () -> "10");
        registry.add("spring.task.execution.pool.max-size", () -> "20");
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
        setupGitHubApiMocks();
    }

    @Test
    @Order(1)
    @DisplayName("High Volume Webhook Processing")
    @Transactional
    void testHighVolumeWebhookProcessing() throws Exception {
        // 1. Setup test data
        GitHubInstallation installation = createTestInstallation();
        RepositoryConfig config = createTestRepositoryConfig(installation);

        // 2. Configure for high volume
        final int WEBHOOK_COUNT = 100;
        final int CONCURRENT_THREADS = 10;
        
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        // 3. Record start time
        long startTime = System.currentTimeMillis();

        // 4. Send webhooks concurrently
        for (int i = 0; i < WEBHOOK_COUNT; i++) {
            final int prNumber = i + 1;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    Map<String, Object> webhook = createPullRequestWebhook(installation.getId(), "opened");
                    ((Map<String, Object>) webhook.get("pull_request")).put("number", prNumber);
                    
                    mockMvc.perform(post("/api/v1/webhooks/github")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-GitHub-Event", "pull_request")
                            .header("X-GitHub-Delivery", "delivery-" + prNumber)
                            .header("X-GitHub-Signature-256", "sha256=test-signature")
                            .content(objectMapper.writeValueAsString(webhook)))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.processed").value(true));
                    
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.err.println("Error processing webhook " + prNumber + ": " + e.getMessage());
                }
            }, executor);
            
            futures.add(future);
        }

        // 5. Wait for all webhooks to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        // 6. Record end time
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 7. Wait for all async processing to complete
        await().atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<PullRequestReview> reviews = reviewRepository.findByInstallationId(installation.getId());
                    assertThat(reviews).hasSize(WEBHOOK_COUNT);
                    assertThat(reviews.stream().allMatch(r -> 
                        "COMPLETED".equals(r.getStatus()) || "FAILED".equals(r.getStatus()))).isTrue();
                });

        // 8. Verify results
        assertThat(successCount.get()).isEqualTo(WEBHOOK_COUNT);
        assertThat(errorCount.get()).isEqualTo(0);

        // 9. Performance assertions
        double throughput = (double) WEBHOOK_COUNT / (duration / 1000.0);
        System.out.printf("Processed %d webhooks in %d ms (%.2f webhooks/sec)%n", 
            WEBHOOK_COUNT, duration, throughput);
        
        // Should process at least 10 webhooks per second
        assertThat(throughput).isGreaterThan(10.0);
        
        // Average processing time should be reasonable
        assertThat(duration / WEBHOOK_COUNT).isLessThan(500); // Less than 500ms per webhook
    }

    @Test
    @Order(2)
    @DisplayName("Memory Usage Under Load")
    @Transactional
    void testMemoryUsageUnderLoad() throws Exception {
        // 1. Setup test data
        GitHubInstallation installation = createTestInstallation();
        RepositoryConfig config = createTestRepositoryConfig(installation);

        // 2. Record initial memory usage
        System.gc();
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        // 3. Process large number of webhooks
        final int WEBHOOK_COUNT = 200;
        
        for (int i = 0; i < WEBHOOK_COUNT; i++) {
            Map<String, Object> webhook = createPullRequestWebhook(installation.getId(), "opened");
            ((Map<String, Object>) webhook.get("pull_request")).put("number", i + 1);
            
            mockMvc.perform(post("/api/v1/webhooks/github")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-GitHub-Event", "pull_request")
                    .header("X-GitHub-Delivery", "delivery-" + (i + 1))
                    .header("X-GitHub-Signature-256", "sha256=test-signature")
                    .content(objectMapper.writeValueAsString(webhook)))
                    .andExpect(status().isOk());
        }

        // 4. Wait for processing to complete
        await().atMost(60, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<PullRequestReview> reviews = reviewRepository.findByInstallationId(installation.getId());
                    assertThat(reviews).hasSize(WEBHOOK_COUNT);
                });

        // 5. Check memory usage
        System.gc();
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;
        
        System.out.printf("Memory increase: %.2f MB%n", memoryIncrease / 1024.0 / 1024.0);
        
        // Memory increase should be reasonable (less than 100MB)
        assertThat(memoryIncrease).isLessThan(100 * 1024 * 1024); // 100MB
    }

    @Test
    @Order(3)
    @DisplayName("Database Performance Under Load")
    @Transactional
    void testDatabasePerformanceUnderLoad() throws Exception {
        // 1. Setup multiple installations and repositories
        List<GitHubInstallation> installations = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            GitHubInstallation installation = createTestInstallation();
            installation.setId(installation.getId() + i);
            installation.setAccountLogin("test-org-" + i);
            installation = installationRepository.save(installation);
            installations.add(installation);
            
            RepositoryConfig config = createTestRepositoryConfig(installation);
            config.setRepositoryFullName("test-org-" + i + "/test-repo");
            configRepository.save(config);
        }

        // 2. Process webhooks across multiple installations
        final int WEBHOOKS_PER_INSTALLATION = 20;
        long startTime = System.currentTimeMillis();

        for (GitHubInstallation installation : installations) {
            for (int i = 0; i < WEBHOOKS_PER_INSTALLATION; i++) {
                Map<String, Object> webhook = createPullRequestWebhook(installation.getId(), "opened");
                ((Map<String, Object>) webhook.get("pull_request")).put("number", i + 1);
                
                String repoName = "test-org-" + installations.indexOf(installation) + "/test-repo";
                ((Map<String, Object>) webhook.get("repository")).put("full_name", repoName);
                
                mockMvc.perform(post("/api/v1/webhooks/github")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Event", "pull_request")
                        .header("X-GitHub-Delivery", "delivery-" + installation.getId() + "-" + i)
                        .header("X-GitHub-Signature-256", "sha256=test-signature")
                        .content(objectMapper.writeValueAsString(webhook)))
                        .andExpect(status().isOk());
            }
        }

        // 3. Wait for all processing to complete
        await().atMost(60, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    long totalReviews = reviewRepository.count();
                    assertThat(totalReviews).isEqualTo(installations.size() * WEBHOOKS_PER_INSTALLATION);
                });

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 4. Verify database performance
        System.out.printf("Database operations completed in %d ms%n", duration);
        
        // Test database queries performance
        long queryStart = System.currentTimeMillis();
        
        for (GitHubInstallation installation : installations) {
            List<PullRequestReview> reviews = reviewRepository.findByInstallationId(installation.getId());
            assertThat(reviews).hasSize(WEBHOOKS_PER_INSTALLATION);
        }
        
        long queryEnd = System.currentTimeMillis();
        long queryDuration = queryEnd - queryStart;
        
        System.out.printf("Database queries completed in %d ms%n", queryDuration);
        
        // Query performance should be reasonable
        assertThat(queryDuration).isLessThan(1000); // Less than 1 second
    }

    @Test
    @Order(4)
    @DisplayName("GitHub API Rate Limiting Handling")
    @Transactional
    void testGitHubApiRateLimitingHandling() throws Exception {
        // 1. Setup test data
        GitHubInstallation installation = createTestInstallation();
        RepositoryConfig config = createTestRepositoryConfig(installation);

        // 2. Configure API to return rate limit errors occasionally
        stubFor(get(urlMatching("/repos/.*/pulls/.*"))
                .inScenario("Rate Limiting")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withStatus(403)
                        .withHeader("X-RateLimit-Remaining", "0")
                        .withHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() / 1000 + 60))
                        .withBody("Rate limit exceeded"))
                .willSetStateTo("Rate Limited"));

        stubFor(get(urlMatching("/repos/.*/pulls/.*"))
                .inScenario("Rate Limiting")
                .whenScenarioStateIs("Rate Limited")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createPullRequestResponse()))
                .willSetStateTo("Started"));

        // 3. Send webhooks that will trigger rate limiting
        final int WEBHOOK_COUNT = 10;
        
        for (int i = 0; i < WEBHOOK_COUNT; i++) {
            Map<String, Object> webhook = createPullRequestWebhook(installation.getId(), "opened");
            ((Map<String, Object>) webhook.get("pull_request")).put("number", i + 1);
            
            mockMvc.perform(post("/api/v1/webhooks/github")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-GitHub-Event", "pull_request")
                    .header("X-GitHub-Delivery", "delivery-rate-limit-" + i)
                    .header("X-GitHub-Signature-256", "sha256=test-signature")
                    .content(objectMapper.writeValueAsString(webhook)))
                    .andExpect(status().isOk());
        }

        // 4. Wait for processing with rate limiting
        await().atMost(120, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<PullRequestReview> reviews = reviewRepository.findByInstallationId(installation.getId());
                    assertThat(reviews).hasSize(WEBHOOK_COUNT);
                    
                    // Some may succeed, some may fail due to rate limiting
                    long completedCount = reviews.stream().filter(r -> "COMPLETED".equals(r.getStatus())).count();
                    long failedCount = reviews.stream().filter(r -> "FAILED".equals(r.getStatus())).count();
                    
                    assertThat(completedCount + failedCount).isEqualTo(WEBHOOK_COUNT);
                });

        // 5. Verify that the system handled rate limiting gracefully
        List<PullRequestReview> reviews = reviewRepository.findByInstallationId(installation.getId());
        long failedCount = reviews.stream().filter(r -> "FAILED".equals(r.getStatus())).count();
        
        // Some failures are expected due to rate limiting
        System.out.printf("Failed reviews due to rate limiting: %d out of %d%n", failedCount, WEBHOOK_COUNT);
    }

    @Test
    @Order(5)
    @DisplayName("Concurrent Installation Processing")
    @Transactional
    void testConcurrentInstallationProcessing() throws Exception {
        // 1. Create multiple installations concurrently
        final int INSTALLATION_COUNT = 20;
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < INSTALLATION_COUNT; i++) {
            final int installationId = i + 1;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    Map<String, Object> webhook = createInstallationWebhook(installationId);
                    
                    mockMvc.perform(post("/api/v1/webhooks/github")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-GitHub-Event", "installation")
                            .header("X-GitHub-Delivery", "install-" + installationId)
                            .header("X-GitHub-Signature-256", "sha256=test-signature")
                            .content(objectMapper.writeValueAsString(webhook)))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.processed").value(true));
                    
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Error processing installation " + installationId + ": " + e.getMessage());
                }
            }, executor);
            
            futures.add(future);
        }

        // 2. Wait for all installations to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();

        // 3. Verify all installations were processed
        assertThat(successCount.get()).isEqualTo(INSTALLATION_COUNT);
        
        // 4. Verify database state
        List<GitHubInstallation> installations = installationRepository.findAll();
        assertThat(installations).hasSizeGreaterThanOrEqualTo(INSTALLATION_COUNT);
    }

    // Helper methods

    private GitHubInstallation createTestInstallation() {
        GitHubInstallation installation = new GitHubInstallation();
        installation.setId(System.currentTimeMillis()); // Use timestamp for uniqueness
        installation.setAccountLogin("test-perf-user");
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

    private Map<String, Object> createPullRequestWebhook(Long installationId, String action) {
        Map<String, Object> webhook = new HashMap<>();
        webhook.put("action", action);
        webhook.put("installation", Map.of("id", installationId));
        
        webhook.put("pull_request", Map.of(
            "id", 1L,
            "number", 123,
            "title", "Performance test PR",
            "body", "This is a performance test PR.",
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

    private Map<String, Object> createInstallationWebhook(int installationId) {
        Map<String, Object> webhook = new HashMap<>();
        webhook.put("action", "created");
        webhook.put("installation", Map.of(
            "id", installationId,
            "account", Map.of(
                "login", "test-user-" + installationId,
                "type", "User"
            )
        ));
        return webhook;
    }

    private void setupGitHubApiMocks() {
        stubFor(get(urlMatching("/repos/.*/pulls/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createPullRequestResponse())
                        .withFixedDelay(100))); // Simulate network delay

        stubFor(post(urlMatching("/repos/.*/issues/.*/comments"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createCommentResponse())
                        .withFixedDelay(150))); // Simulate network delay
    }

    private String createPullRequestResponse() {
        return """
            {
                "id": 1,
                "number": 123,
                "title": "Performance test PR",
                "body": "This is a performance test PR.",
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
                "body": "Performance test comment",
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