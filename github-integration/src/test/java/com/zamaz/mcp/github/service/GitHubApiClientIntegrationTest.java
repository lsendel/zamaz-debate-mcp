package com.zamaz.mcp.github.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.zamaz.mcp.github.dto.*;
import com.zamaz.mcp.github.exception.GitHubApiException;
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
 * Integration tests for GitHubApiClient using WireMock to mock GitHub API responses
 */
@SpringBootTest
@TestPropertySource(properties = {
    "github.api.base-url=http://localhost:8089"
})
class GitHubApiClientIntegrationTest {
    
    @Autowired
    private RestTemplate restTemplate;
    
    private GitHubApiClient gitHubApiClient;
    private WireMockServer wireMockServer;
    private ObjectMapper objectMapper;
    
    private static final String TEST_TOKEN = "test-token";
    private static final String TEST_OWNER = "test-owner";
    private static final String TEST_REPO = "test-repo";
    private static final int TEST_PR_NUMBER = 123;
    
    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(8089));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
        
        gitHubApiClient = new GitHubApiClient(restTemplate);
        objectMapper = new ObjectMapper();
    }
    
    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }
    
    @Test
    void testGetPullRequest_Success() {
        // Given
        Map<String, Object> mockPrResponse = createMockPullRequestResponse();
        
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo/pulls/123"))
            .withHeader("Authorization", equalTo("Bearer test-token"))
            .withHeader("Accept", equalTo("application/vnd.github.v3+json"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(toJson(mockPrResponse))));
        
        // When
        GitHubPullRequest result = gitHubApiClient.getPullRequest(TEST_TOKEN, TEST_OWNER, TEST_REPO, TEST_PR_NUMBER);
        
        // Then
        assertNotNull(result);
        assertEquals(123456L, result.getId());
        assertEquals(123, result.getNumber());
        assertEquals("Test PR", result.getTitle());
        assertEquals("This is a test pull request", result.getBody());
        assertEquals("open", result.getState());
        assertEquals("https://github.com/test-owner/test-repo/pull/123", result.getHtmlUrl());
        assertEquals("feature-branch", result.getHeadRef());
        assertEquals("main", result.getBaseRef());
        
        assertNotNull(result.getUser());
        assertEquals("test-user", result.getUser().getLogin());
        
        verify(getRequestedFor(urlEqualTo("/repos/test-owner/test-repo/pulls/123")));
    }
    
    @Test
    void testGetPullRequest_NotFound() {
        // Given
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo/pulls/999"))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Not Found\"}")));
        
        // When & Then
        GitHubApiException exception = assertThrows(GitHubApiException.class, () ->
            gitHubApiClient.getPullRequest(TEST_TOKEN, TEST_OWNER, TEST_REPO, 999));
        
        assertEquals("Failed to retrieve pull request", exception.getMessage());
        
        verify(getRequestedFor(urlEqualTo("/repos/test-owner/test-repo/pulls/999")));
    }
    
    @Test
    void testListPullRequests_Success() {
        // Given
        List<Map<String, Object>> mockPrsResponse = List.of(
            createMockPullRequestResponse(),
            createMockPullRequestResponse("Test PR 2", 124, "closed")
        );
        
        stubFor(get(urlPathEqualTo("/repos/test-owner/test-repo/pulls"))
            .withQueryParam("state", equalTo("open"))
            .withQueryParam("page", equalTo("1"))
            .withQueryParam("per_page", equalTo("30"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(toJson(mockPrsResponse))));
        
        // When
        List<GitHubPullRequest> result = gitHubApiClient.listPullRequests(
            TEST_TOKEN, TEST_OWNER, TEST_REPO, "open", 1, 30);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Test PR", result.get(0).getTitle());
        assertEquals("Test PR 2", result.get(1).getTitle());
        
        verify(getRequestedFor(urlPathEqualTo("/repos/test-owner/test-repo/pulls")));
    }
    
    @Test
    void testPostComment_Success() {
        // Given
        Map<String, Object> mockCommentResponse = createMockCommentResponse();
        
        stubFor(post(urlEqualTo("/repos/test-owner/test-repo/issues/123/comments"))
            .withHeader("Authorization", equalTo("Bearer test-token"))
            .withRequestBody(containing("\"body\":\"Test comment\""))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(toJson(mockCommentResponse))));
        
        // When
        GitHubComment result = gitHubApiClient.postComment(
            TEST_TOKEN, TEST_OWNER, TEST_REPO, TEST_PR_NUMBER, "Test comment");
        
        // Then
        assertNotNull(result);
        assertEquals(789L, result.getId());
        assertEquals("Test comment", result.getBody());
        assertEquals("test-user", result.getUser().getLogin());
        
        verify(postRequestedFor(urlEqualTo("/repos/test-owner/test-repo/issues/123/comments")));
    }
    
    @Test
    void testPostComment_Forbidden() {
        // Given
        stubFor(post(urlEqualTo("/repos/test-owner/test-repo/issues/123/comments"))
            .willReturn(aResponse()
                .withStatus(403)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Forbidden\"}")));
        
        // When & Then
        GitHubApiException exception = assertThrows(GitHubApiException.class, () ->
            gitHubApiClient.postComment(TEST_TOKEN, TEST_OWNER, TEST_REPO, TEST_PR_NUMBER, "Test comment"));
        
        assertEquals("Failed to post comment", exception.getMessage());
        
        verify(postRequestedFor(urlEqualTo("/repos/test-owner/test-repo/issues/123/comments")));
    }
    
    @Test
    void testUpdateComment_Success() {
        // Given
        Map<String, Object> mockCommentResponse = createMockCommentResponse("Updated comment");
        
        stubFor(patch(urlEqualTo("/repos/test-owner/test-repo/issues/comments/789"))
            .withHeader("Authorization", equalTo("Bearer test-token"))
            .withRequestBody(containing("\"body\":\"Updated comment\""))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(toJson(mockCommentResponse))));
        
        // When
        GitHubComment result = gitHubApiClient.updateComment(
            TEST_TOKEN, TEST_OWNER, TEST_REPO, 789L, "Updated comment");
        
        // Then
        assertNotNull(result);
        assertEquals(789L, result.getId());
        assertEquals("Updated comment", result.getBody());
        
        verify(patchRequestedFor(urlEqualTo("/repos/test-owner/test-repo/issues/comments/789")));
    }
    
    @Test
    void testDeleteComment_Success() {
        // Given
        stubFor(delete(urlEqualTo("/repos/test-owner/test-repo/issues/comments/789"))
            .withHeader("Authorization", equalTo("Bearer test-token"))
            .willReturn(aResponse()
                .withStatus(204)));
        
        // When & Then
        assertDoesNotThrow(() ->
            gitHubApiClient.deleteComment(TEST_TOKEN, TEST_OWNER, TEST_REPO, 789L));
        
        verify(deleteRequestedFor(urlEqualTo("/repos/test-owner/test-repo/issues/comments/789")));
    }
    
    @Test
    void testGetRepository_Success() {
        // Given
        Map<String, Object> mockRepoResponse = createMockRepositoryResponse();
        
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo"))
            .withHeader("Authorization", equalTo("Bearer test-token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(toJson(mockRepoResponse))));
        
        // When
        GitHubRepository result = gitHubApiClient.getRepository(TEST_TOKEN, TEST_OWNER, TEST_REPO);
        
        // Then
        assertNotNull(result);
        assertEquals(456789L, result.getId());
        assertEquals("test-repo", result.getName());
        assertEquals("test-owner/test-repo", result.getFullName());
        assertEquals("A test repository", result.getDescription());
        assertEquals("main", result.getDefaultBranch());
        assertFalse(result.getIsPrivate());
        
        assertNotNull(result.getOwner());
        assertEquals("test-owner", result.getOwner().getLogin());
        
        verify(getRequestedFor(urlEqualTo("/repos/test-owner/test-repo")));
    }
    
    @Test
    void testUpdatePullRequest_Success() {
        // Given
        Map<String, Object> mockPrResponse = createMockPullRequestResponse("Updated PR", 123, "closed");
        
        stubFor(patch(urlEqualTo("/repos/test-owner/test-repo/pulls/123"))
            .withHeader("Authorization", equalTo("Bearer test-token"))
            .withRequestBody(containing("\"state\":\"closed\""))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(toJson(mockPrResponse))));
        
        // When
        GitHubPullRequest result = gitHubApiClient.updatePullRequest(
            TEST_TOKEN, TEST_OWNER, TEST_REPO, TEST_PR_NUMBER, "closed", null);
        
        // Then
        assertNotNull(result);
        assertEquals(123456L, result.getId());
        assertEquals("closed", result.getState());
        
        verify(patchRequestedFor(urlEqualTo("/repos/test-owner/test-repo/pulls/123")));
    }
    
    @Test
    void testGetAuthenticatedUser_Success() {
        // Given
        Map<String, Object> mockUserResponse = createMockUserResponse();
        
        stubFor(get(urlEqualTo("/user"))
            .withHeader("Authorization", equalTo("Bearer test-token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(toJson(mockUserResponse))));
        
        // When
        GitHubUser result = gitHubApiClient.getAuthenticatedUser(TEST_TOKEN);
        
        // Then
        assertNotNull(result);
        assertEquals(12345L, result.getId());
        assertEquals("test-user", result.getLogin());
        assertEquals("https://github.com/test-user", result.getHtmlUrl());
        
        verify(getRequestedFor(urlEqualTo("/user")));
    }
    
    @Test
    void testListComments_Success() {
        // Given
        List<Map<String, Object>> mockCommentsResponse = List.of(
            createMockCommentResponse(),
            createMockCommentResponse("Second comment")
        );
        
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo/issues/123/comments"))
            .withHeader("Authorization", equalTo("Bearer test-token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(toJson(mockCommentsResponse))));
        
        // When
        List<GitHubComment> result = gitHubApiClient.listComments(
            TEST_TOKEN, TEST_OWNER, TEST_REPO, TEST_PR_NUMBER);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Test comment", result.get(0).getBody());
        assertEquals("Second comment", result.get(1).getBody());
        
        verify(getRequestedFor(urlEqualTo("/repos/test-owner/test-repo/issues/123/comments")));
    }
    
    @Test
    void testApiRateLimit_RetryAfterHeader() {
        // Given
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo/pulls/123"))
            .inScenario("Rate Limit")
            .whenScenarioStateIs("Started")
            .willReturn(aResponse()
                .withStatus(429)
                .withHeader("X-RateLimit-Remaining", "0")
                .withHeader("Retry-After", "60")
                .withBody("{\"message\":\"API rate limit exceeded\"}"))
            .willSetStateTo("Rate Limited"));
        
        // When & Then
        GitHubApiException exception = assertThrows(GitHubApiException.class, () ->
            gitHubApiClient.getPullRequest(TEST_TOKEN, TEST_OWNER, TEST_REPO, TEST_PR_NUMBER));
        
        assertEquals("Failed to retrieve pull request", exception.getMessage());
        
        verify(getRequestedFor(urlEqualTo("/repos/test-owner/test-repo/pulls/123")));
    }
    
    @Test
    void testInvalidToken_Unauthorized() {
        // Given
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo/pulls/123"))
            .withHeader("Authorization", equalTo("Bearer invalid-token"))
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Bad credentials\"}")));
        
        // When & Then
        GitHubApiException exception = assertThrows(GitHubApiException.class, () ->
            gitHubApiClient.getPullRequest("invalid-token", TEST_OWNER, TEST_REPO, TEST_PR_NUMBER));
        
        assertEquals("Failed to retrieve pull request", exception.getMessage());
        
        verify(getRequestedFor(urlEqualTo("/repos/test-owner/test-repo/pulls/123")));
    }
    
    // Helper methods for creating mock responses
    
    private Map<String, Object> createMockPullRequestResponse() {
        return createMockPullRequestResponse("Test PR", 123, "open");
    }
    
    private Map<String, Object> createMockPullRequestResponse(String title, int number, String state) {
        return Map.of(
            "id", 123456,
            "number", number,
            "title", title,
            "body", "This is a test pull request",
            "state", state,
            "html_url", "https://github.com/test-owner/test-repo/pull/" + number,
            "user", createMockUserResponse(),
            "head", Map.of("ref", "feature-branch"),
            "base", Map.of("ref", "main"),
            "created_at", "2023-01-01T00:00:00Z",
            "updated_at", "2023-01-01T00:00:00Z"
        );
    }
    
    private Map<String, Object> createMockCommentResponse() {
        return createMockCommentResponse("Test comment");
    }
    
    private Map<String, Object> createMockCommentResponse(String body) {
        return Map.of(
            "id", 789,
            "body", body,
            "user", createMockUserResponse(),
            "html_url", "https://github.com/test-owner/test-repo/pull/123#issuecomment-789",
            "created_at", "2023-01-01T00:00:00Z",
            "updated_at", "2023-01-01T00:00:00Z"
        );
    }
    
    private Map<String, Object> createMockRepositoryResponse() {
        return Map.of(
            "id", 456789,
            "name", "test-repo",
            "full_name", "test-owner/test-repo",
            "owner", createMockUserResponse(),
            "html_url", "https://github.com/test-owner/test-repo",
            "description", "A test repository",
            "private", false,
            "default_branch", "main"
        );
    }
    
    private Map<String, Object> createMockUserResponse() {
        return Map.of(
            "id", 12345,
            "login", "test-user",
            "avatar_url", "https://github.com/images/error/test-user_happy.gif",
            "html_url", "https://github.com/test-user",
            "type", "User"
        );
    }
    
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }
}