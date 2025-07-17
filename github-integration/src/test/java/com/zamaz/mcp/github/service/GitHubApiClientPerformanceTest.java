package com.zamaz.mcp.github.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.zamaz.mcp.github.dto.GitHubPullRequest;
import com.zamaz.mcp.github.dto.GitHubComment;
import com.zamaz.mcp.github.util.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for GitHubApiClient
 */
@SpringBootTest
@TestPropertySource(properties = {
    "github.api.base-url=http://localhost:8089"
})
class GitHubApiClientPerformanceTest {
    
    @Autowired
    private RestTemplate restTemplate;
    
    private GitHubApiClient gitHubApiClient;
    private WireMockServer wireMockServer;
    
    private static final String TEST_TOKEN = "test-token";
    private static final String TEST_OWNER = "test-owner";
    private static final String TEST_REPO = "test-repo";
    
    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8089));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
        
        gitHubApiClient = new GitHubApiClient(restTemplate);
    }
    
    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }
    
    @Test
    void testConcurrentPullRequestRequests() throws InterruptedException {
        // Given
        int numberOfRequests = 50;
        int numberOfThreads = 10;
        
        // Setup mock responses for multiple PR requests
        for (int i = 1; i <= numberOfRequests; i++) {
            stubFor(get(urlEqualTo("/repos/test-owner/test-repo/pulls/" + i))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(TestUtils.toJson(TestUtils.createMockPullRequest("PR " + i, i, "open")))
                    .withFixedDelay(50))); // Simulate network delay
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<CompletableFuture<GitHubPullRequest>> futures = new ArrayList<>();
        
        Instant start = Instant.now();
        
        // When
        for (int i = 1; i <= numberOfRequests; i++) {
            final int prNumber = i;
            CompletableFuture<GitHubPullRequest> future = CompletableFuture.supplyAsync(() ->
                gitHubApiClient.getPullRequest(TEST_TOKEN, TEST_OWNER, TEST_REPO, prNumber), executor);
            futures.add(future);
        }
        
        // Wait for all requests to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        Duration duration = Duration.between(start, Instant.now());
        
        // Then
        assertEquals(numberOfRequests, futures.size());
        
        // Verify all requests completed successfully
        for (int i = 0; i < numberOfRequests; i++) {
            GitHubPullRequest result = futures.get(i).join();
            assertNotNull(result);
            assertEquals("PR " + (i + 1), result.getTitle());
        }
        
        // Performance assertion - should complete in reasonable time with concurrent requests
        // With 50ms delay per request and 10 threads, should complete much faster than 2.5 seconds
        assertTrue(duration.toMillis() < 2500, 
            "Concurrent requests took too long: " + duration.toMillis() + "ms");
        
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }
    
    @Test
    void testBulkCommentPosting() throws InterruptedException {
        // Given
        int numberOfComments = 20;
        int numberOfThreads = 5;
        
        // Setup mock responses for comment posting
        for (int i = 1; i <= numberOfComments; i++) {
            stubFor(post(urlEqualTo("/repos/test-owner/test-repo/issues/123/comments"))
                .withRequestBody(containing("Comment " + i))
                .willReturn(aResponse()
                    .withStatus(201)
                    .withHeader("Content-Type", "application/json")
                    .withBody(TestUtils.toJson(TestUtils.createMockComment("Comment " + i, i)))
                    .withFixedDelay(100))); // Simulate network delay
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        List<CompletableFuture<GitHubComment>> futures = new ArrayList<>();
        
        Instant start = Instant.now();
        
        // When
        for (int i = 1; i <= numberOfComments; i++) {
            final int commentNumber = i;
            CompletableFuture<GitHubComment> future = CompletableFuture.supplyAsync(() ->
                gitHubApiClient.postComment(TEST_TOKEN, TEST_OWNER, TEST_REPO, 123, "Comment " + commentNumber), executor);
            futures.add(future);
        }
        
        // Wait for all requests to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        Duration duration = Duration.between(start, Instant.now());
        
        // Then
        assertEquals(numberOfComments, futures.size());
        
        // Verify all comments were posted successfully
        for (int i = 0; i < numberOfComments; i++) {
            GitHubComment result = futures.get(i).join();
            assertNotNull(result);
            assertEquals("Comment " + (i + 1), result.getBody());
        }
        
        // Performance assertion - should complete in reasonable time with concurrent requests
        // With 100ms delay per request and 5 threads, should complete much faster than 4 seconds
        assertTrue(duration.toMillis() < 4000, 
            "Bulk comment posting took too long: " + duration.toMillis() + "ms");
        
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }
    
    @Test
    void testSequentialVsConcurrentPerformance() throws InterruptedException {
        // Given
        int numberOfRequests = 10;
        
        // Setup mock responses
        for (int i = 1; i <= numberOfRequests; i++) {
            stubFor(get(urlEqualTo("/repos/test-owner/test-repo/pulls/" + i))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(TestUtils.toJson(TestUtils.createMockPullRequest("PR " + i, i, "open")))
                    .withFixedDelay(100))); // 100ms delay per request
        }
        
        // Sequential execution
        Instant sequentialStart = Instant.now();
        List<GitHubPullRequest> sequentialResults = new ArrayList<>();
        
        for (int i = 1; i <= numberOfRequests; i++) {
            GitHubPullRequest result = gitHubApiClient.getPullRequest(TEST_TOKEN, TEST_OWNER, TEST_REPO, i);
            sequentialResults.add(result);
        }
        
        Duration sequentialDuration = Duration.between(sequentialStart, Instant.now());
        
        // Reset WireMock for concurrent test
        wireMockServer.resetAll();
        
        // Setup mock responses again
        for (int i = 1; i <= numberOfRequests; i++) {
            stubFor(get(urlEqualTo("/repos/test-owner/test-repo/pulls/" + i))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(TestUtils.toJson(TestUtils.createMockPullRequest("PR " + i, i, "open")))
                    .withFixedDelay(100))); // 100ms delay per request
        }
        
        // Concurrent execution
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<CompletableFuture<GitHubPullRequest>> futures = new ArrayList<>();
        
        Instant concurrentStart = Instant.now();
        
        for (int i = 1; i <= numberOfRequests; i++) {
            final int prNumber = i;
            CompletableFuture<GitHubPullRequest> future = CompletableFuture.supplyAsync(() ->
                gitHubApiClient.getPullRequest(TEST_TOKEN, TEST_OWNER, TEST_REPO, prNumber), executor);
            futures.add(future);
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        Duration concurrentDuration = Duration.between(concurrentStart, Instant.now());
        
        // Then
        assertEquals(numberOfRequests, sequentialResults.size());
        assertEquals(numberOfRequests, futures.size());
        
        // Concurrent should be significantly faster than sequential
        assertTrue(concurrentDuration.toMillis() < sequentialDuration.toMillis() / 2,
            "Concurrent execution should be at least 2x faster than sequential. " +
            "Sequential: " + sequentialDuration.toMillis() + "ms, " +
            "Concurrent: " + concurrentDuration.toMillis() + "ms");
        
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }
    
    @Test
    void testMemoryUsageWithLargeResponses() {
        // Given - Create a large mock response
        StringBuilder largeBody = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeBody.append("This is line ").append(i).append(" of a very long pull request body. ");
        }
        
        var mockPr = TestUtils.createMockPullRequest("Large PR", 1, "open");
        mockPr.put("body", largeBody.toString());
        
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo/pulls/1"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(TestUtils.toJson(mockPr))));
        
        // When
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        GitHubPullRequest result = gitHubApiClient.getPullRequest(TEST_TOKEN, TEST_OWNER, TEST_REPO, 1);
        
        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        // Then
        assertNotNull(result);
        assertEquals("Large PR", result.getTitle());
        assertTrue(result.getBody().length() > 50000); // Should be a large response
        
        // Memory usage should be reasonable (less than 10MB increase)
        long memoryIncrease = memoryAfter - memoryBefore;
        assertTrue(memoryIncrease < 10 * 1024 * 1024, 
            "Memory increase too large: " + memoryIncrease + " bytes");
    }
    
    @Test
    void testRateLimitHandling() {
        // Given
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo/pulls/1"))
            .inScenario("Rate Limit Test")
            .whenScenarioStateIs("Started")
            .willReturn(aResponse()
                .withStatus(429)
                .withHeader("X-RateLimit-Remaining", "0")
                .withHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() / 1000 + 1))
                .withBody("{\"message\":\"API rate limit exceeded\"}"))
            .willSetStateTo("Rate Limited"));
        
        // When & Then
        Instant start = Instant.now();
        
        assertThrows(Exception.class, () ->
            gitHubApiClient.getPullRequest(TEST_TOKEN, TEST_OWNER, TEST_REPO, 1));
        
        Duration duration = Duration.between(start, Instant.now());
        
        // Should fail quickly and not hang
        assertTrue(duration.toMillis() < 5000, 
            "Rate limit handling should fail quickly, took: " + duration.toMillis() + "ms");
    }
}