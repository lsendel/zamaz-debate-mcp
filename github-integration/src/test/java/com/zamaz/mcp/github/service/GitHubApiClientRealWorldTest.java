package com.zamaz.mcp.github.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.zamaz.mcp.github.dto.GitHubPullRequest;
import com.zamaz.mcp.github.dto.GitHubComment;
import com.zamaz.mcp.github.dto.GitHubRepository;
import com.zamaz.mcp.github.util.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Real-world scenario tests for GitHubApiClient
 */
@SpringBootTest
@TestPropertySource(properties = {
    "github.api.base-url=http://localhost:8089"
})
class GitHubApiClientRealWorldTest {
    
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
    void testCompleteCodeReviewWorkflow() {
        // Given - Set up a complete code review workflow
        int prNumber = 123;
        
        // 1. Get repository information
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(TestUtils.toJson(TestUtils.createMockRepository("test-repo", "test-owner", false)))));
        
        // 2. Get pull request details
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo/pulls/123"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(TestUtils.toJson(TestUtils.createMockPullRequest("Fix authentication bug", 123, "open")))));
        
        // 3. List existing comments
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo/issues/123/comments"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(TestUtils.toJson(List.of(
                    TestUtils.createMockComment("Looks good overall", 1),
                    TestUtils.createMockComment("Need to fix this line", 2)
                )))));
        
        // 4. Post a new review comment
        stubFor(post(urlEqualTo("/repos/test-owner/test-repo/issues/123/comments"))
            .withRequestBody(containing("LGTM! Ready to merge"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(TestUtils.toJson(TestUtils.createMockComment("LGTM! Ready to merge", 3)))));
        
        // 5. Update pull request status
        stubFor(patch(urlEqualTo("/repos/test-owner/test-repo/pulls/123"))
            .withRequestBody(containing("\"state\":\"closed\""))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(TestUtils.toJson(TestUtils.createMockPullRequest("Fix authentication bug", 123, "closed")))));
        
        // When - Execute the complete workflow
        
        // 1. Get repository information
        GitHubRepository repo = gitHubApiClient.getRepository(TEST_TOKEN, TEST_OWNER, TEST_REPO);
        
        // 2. Get pull request details
        GitHubPullRequest pr = gitHubApiClient.getPullRequest(TEST_TOKEN, TEST_OWNER, TEST_REPO, prNumber);
        
        // 3. List existing comments
        List<GitHubComment> comments = gitHubApiClient.listComments(TEST_TOKEN, TEST_OWNER, TEST_REPO, prNumber);
        
        // 4. Post a new review comment
        GitHubComment newComment = gitHubApiClient.postComment(
            TEST_TOKEN, TEST_OWNER, TEST_REPO, prNumber, "LGTM! Ready to merge");
        
        // 5. Update pull request status
        GitHubPullRequest updatedPr = gitHubApiClient.updatePullRequest(
            TEST_TOKEN, TEST_OWNER, TEST_REPO, prNumber, "closed", null);
        
        // Then - Verify the complete workflow
        assertNotNull(repo);
        assertEquals("test-repo", repo.getName());
        assertEquals("test-owner", repo.getOwner().getLogin());
        
        assertNotNull(pr);
        assertEquals("Fix authentication bug", pr.getTitle());
        assertEquals("open", pr.getState());
        
        assertNotNull(comments);
        assertEquals(2, comments.size());
        assertEquals("Looks good overall", comments.get(0).getBody());
        assertEquals("Need to fix this line", comments.get(1).getBody());
        
        assertNotNull(newComment);
        assertEquals("LGTM! Ready to merge", newComment.getBody());
        
        assertNotNull(updatedPr);
        assertEquals("closed", updatedPr.getState());
        
        // Verify all API calls were made
        verify(getRequestedFor(urlEqualTo("/repos/test-owner/test-repo")));
        verify(getRequestedFor(urlEqualTo("/repos/test-owner/test-repo/pulls/123")));
        verify(getRequestedFor(urlEqualTo("/repos/test-owner/test-repo/issues/123/comments")));
        verify(postRequestedFor(urlEqualTo("/repos/test-owner/test-repo/issues/123/comments")));
        verify(patchRequestedFor(urlEqualTo("/repos/test-owner/test-repo/pulls/123")));
    }
    
    @Test
    void testPullRequestDiscoveryAndFiltering() {
        // Given - Set up multiple pull requests with different states
        List<Map<String, Object>> openPrs = List.of(
            TestUtils.createMockPullRequest("Feature A", 1, "open"),
            TestUtils.createMockPullRequest("Feature B", 2, "open"),
            TestUtils.createMockPullRequest("Bug fix C", 3, "open")
        );
        
        List<Map<String, Object>> closedPrs = List.of(
            TestUtils.createMockPullRequest("Feature D", 4, "closed"),
            TestUtils.createMockPullRequest("Feature E", 5, "closed")
        );
        
        stubFor(get(urlPathEqualTo("/repos/test-owner/test-repo/pulls"))
            .withQueryParam("state", equalTo("open"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(TestUtils.toJson(openPrs))));
        
        stubFor(get(urlPathEqualTo("/repos/test-owner/test-repo/pulls"))
            .withQueryParam("state", equalTo("closed"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(TestUtils.toJson(closedPrs))));
        
        // When
        List<GitHubPullRequest> openResults = gitHubApiClient.listPullRequests(
            TEST_TOKEN, TEST_OWNER, TEST_REPO, "open", 1, 30);
        
        List<GitHubPullRequest> closedResults = gitHubApiClient.listPullRequests(
            TEST_TOKEN, TEST_OWNER, TEST_REPO, "closed", 1, 30);
        
        // Then
        assertNotNull(openResults);
        assertEquals(3, openResults.size());
        assertTrue(openResults.stream().allMatch(pr -> "open".equals(pr.getState())));
        
        assertNotNull(closedResults);
        assertEquals(2, closedResults.size());
        assertTrue(closedResults.stream().allMatch(pr -> "closed".equals(pr.getState())));
        
        // Verify correct filtering by state
        verify(getRequestedFor(urlPathEqualTo("/repos/test-owner/test-repo/pulls"))
            .withQueryParam("state", equalTo("open")));
        verify(getRequestedFor(urlPathEqualTo("/repos/test-owner/test-repo/pulls"))
            .withQueryParam("state", equalTo("closed")));
    }
    
    @Test
    void testAutomatedResponseToNewPullRequest() {
        // Given - Simulate an automated response to a new pull request
        int prNumber = 456;
        
        // Get the new pull request
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo/pulls/456"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(TestUtils.toJson(TestUtils.createMockPullRequest("New feature implementation", 456, "open")))));
        
        // Post automated welcome comment
        stubFor(post(urlEqualTo("/repos/test-owner/test-repo/issues/456/comments"))
            .withRequestBody(containing("Thank you for your contribution"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(TestUtils.toJson(TestUtils.createMockComment("Thank you for your contribution! Our team will review this soon.", 100)))));
        
        // Add labels to the PR
        stubFor(put(urlEqualTo("/repos/test-owner/test-repo/issues/456/labels"))
            .withRequestBody(containing("needs-review"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[{\"name\":\"needs-review\"}]")));
        
        // When - Simulate automated response workflow
        GitHubPullRequest pr = gitHubApiClient.getPullRequest(TEST_TOKEN, TEST_OWNER, TEST_REPO, prNumber);
        
        // Post welcome comment
        GitHubComment welcomeComment = gitHubApiClient.postComment(
            TEST_TOKEN, TEST_OWNER, TEST_REPO, prNumber, 
            "Thank you for your contribution! Our team will review this soon.");
        
        // Add labels
        GitHubPullRequest updatedPr = gitHubApiClient.updatePullRequest(
            TEST_TOKEN, TEST_OWNER, TEST_REPO, prNumber, null, List.of("needs-review"));
        
        // Then
        assertNotNull(pr);
        assertEquals("New feature implementation", pr.getTitle());
        assertEquals("open", pr.getState());
        
        assertNotNull(welcomeComment);
        assertEquals("Thank you for your contribution! Our team will review this soon.", welcomeComment.getBody());
        
        assertNotNull(updatedPr);
        
        // Verify all automation steps were executed
        verify(getRequestedFor(urlEqualTo("/repos/test-owner/test-repo/pulls/456")));
        verify(postRequestedFor(urlEqualTo("/repos/test-owner/test-repo/issues/456/comments")));
        verify(putRequestedFor(urlEqualTo("/repos/test-owner/test-repo/issues/456/labels")));
    }
    
    @Test
    void testCommentManagementLifecycle() {
        // Given - Set up comment management lifecycle
        long commentId = 789;
        
        // Post initial comment
        stubFor(post(urlEqualTo("/repos/test-owner/test-repo/issues/123/comments"))
            .withRequestBody(containing("Initial comment"))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(TestUtils.toJson(TestUtils.createMockComment("Initial comment", commentId)))));
        
        // Update comment
        stubFor(patch(urlEqualTo("/repos/test-owner/test-repo/issues/comments/789"))
            .withRequestBody(containing("Updated comment"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(TestUtils.toJson(TestUtils.createMockComment("Updated comment", commentId)))));
        
        // Delete comment
        stubFor(delete(urlEqualTo("/repos/test-owner/test-repo/issues/comments/789"))
            .willReturn(aResponse()
                .withStatus(204)));
        
        // When - Execute comment lifecycle
        GitHubComment initialComment = gitHubApiClient.postComment(
            TEST_TOKEN, TEST_OWNER, TEST_REPO, 123, "Initial comment");
        
        GitHubComment updatedComment = gitHubApiClient.updateComment(
            TEST_TOKEN, TEST_OWNER, TEST_REPO, commentId, "Updated comment");
        
        gitHubApiClient.deleteComment(TEST_TOKEN, TEST_OWNER, TEST_REPO, commentId);
        
        // Then
        assertNotNull(initialComment);
        assertEquals("Initial comment", initialComment.getBody());
        assertEquals(commentId, initialComment.getId());
        
        assertNotNull(updatedComment);
        assertEquals("Updated comment", updatedComment.getBody());
        assertEquals(commentId, updatedComment.getId());
        
        // Verify complete lifecycle
        verify(postRequestedFor(urlEqualTo("/repos/test-owner/test-repo/issues/123/comments")));
        verify(patchRequestedFor(urlEqualTo("/repos/test-owner/test-repo/issues/comments/789")));
        verify(deleteRequestedFor(urlEqualTo("/repos/test-owner/test-repo/issues/comments/789")));
    }
    
    @Test
    void testRepositoryAccessAndPermissions() {
        // Given - Set up repository access scenarios
        
        // Public repository
        stubFor(get(urlEqualTo("/repos/test-owner/public-repo"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(TestUtils.toJson(TestUtils.createMockRepository("public-repo", "test-owner", false)))));
        
        // Private repository with access
        stubFor(get(urlEqualTo("/repos/test-owner/private-repo"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(TestUtils.toJson(TestUtils.createMockRepository("private-repo", "test-owner", true)))));
        
        // Repository without access
        stubFor(get(urlEqualTo("/repos/test-owner/forbidden-repo"))
            .willReturn(aResponse()
                .withStatus(403)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Forbidden\"}")));
        
        // When & Then
        
        // Should access public repository
        GitHubRepository publicRepo = gitHubApiClient.getRepository(TEST_TOKEN, TEST_OWNER, "public-repo");
        assertNotNull(publicRepo);
        assertEquals("public-repo", publicRepo.getName());
        assertFalse(publicRepo.getIsPrivate());
        
        // Should access private repository with proper permissions
        GitHubRepository privateRepo = gitHubApiClient.getRepository(TEST_TOKEN, TEST_OWNER, "private-repo");
        assertNotNull(privateRepo);
        assertEquals("private-repo", privateRepo.getName());
        assertTrue(privateRepo.getIsPrivate());
        
        // Should fail to access forbidden repository
        assertThrows(Exception.class, () ->
            gitHubApiClient.getRepository(TEST_TOKEN, TEST_OWNER, "forbidden-repo"));
        
        // Verify all access attempts
        verify(getRequestedFor(urlEqualTo("/repos/test-owner/public-repo")));
        verify(getRequestedFor(urlEqualTo("/repos/test-owner/private-repo")));
        verify(getRequestedFor(urlEqualTo("/repos/test-owner/forbidden-repo")));
    }
}