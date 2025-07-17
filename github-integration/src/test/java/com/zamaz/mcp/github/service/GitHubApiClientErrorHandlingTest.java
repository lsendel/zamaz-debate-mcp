package com.zamaz.mcp.github.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.zamaz.mcp.github.exception.GitHubApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for error handling scenarios in GitHubApiClient
 */
@SpringBootTest
@TestPropertySource(properties = {
    "github.api.base-url=http://localhost:8089"
})
class GitHubApiClientErrorHandlingTest {
    
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
    void testNetworkTimeout_ThrowsGitHubApiException() {
        // Given
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo/pulls/123"))
            .willReturn(aResponse()
                .withFixedDelay(35000) // Longer than timeout
                .withStatus(200)));
        
        // When & Then
        GitHubApiException exception = assertThrows(GitHubApiException.class, () ->
            gitHubApiClient.getPullRequest(TEST_TOKEN, TEST_OWNER, TEST_REPO, TEST_PR_NUMBER));
        
        assertEquals("Network error", exception.getMessage());
        assertNotNull(exception.getCause());
    }
    
    @Test
    void testServerError_500_ThrowsGitHubApiException() {
        // Given
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo/pulls/123"))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Internal Server Error\"}")));
        
        // When & Then
        GitHubApiException exception = assertThrows(GitHubApiException.class, () ->
            gitHubApiClient.getPullRequest(TEST_TOKEN, TEST_OWNER, TEST_REPO, TEST_PR_NUMBER));
        
        assertEquals("Failed to retrieve pull request", exception.getMessage());
        
        verify(getRequestedFor(urlEqualTo("/repos/test-owner/test-repo/pulls/123")));
    }
    
    @Test
    void testBadGateway_502_ThrowsGitHubApiException() {
        // Given
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo/pulls/123"))
            .willReturn(aResponse()
                .withStatus(502)
                .withHeader("Content-Type", "text/html")
                .withBody("<html><body>Bad Gateway</body></html>")));
        
        // When & Then
        GitHubApiException exception = assertThrows(GitHubApiException.class, () ->
            gitHubApiClient.getPullRequest(TEST_TOKEN, TEST_OWNER, TEST_REPO, TEST_PR_NUMBER));
        
        assertEquals("Failed to retrieve pull request", exception.getMessage());
        
        verify(getRequestedFor(urlEqualTo("/repos/test-owner/test-repo/pulls/123")));
    }
    
    @Test
    void testServiceUnavailable_503_ThrowsGitHubApiException() {
        // Given
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo/pulls/123"))
            .willReturn(aResponse()
                .withStatus(503)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Service Unavailable\"}")));
        
        // When & Then
        GitHubApiException exception = assertThrows(GitHubApiException.class, () ->
            gitHubApiClient.getPullRequest(TEST_TOKEN, TEST_OWNER, TEST_REPO, TEST_PR_NUMBER));
        
        assertEquals("Failed to retrieve pull request", exception.getMessage());
        
        verify(getRequestedFor(urlEqualTo("/repos/test-owner/test-repo/pulls/123")));
    }
    
    @Test
    void testUnauthorized_401_ThrowsGitHubApiException() {
        // Given
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo/pulls/123"))
            .willReturn(aResponse()
                .withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Bad credentials\"}")));
        
        // When & Then
        GitHubApiException exception = assertThrows(GitHubApiException.class, () ->
            gitHubApiClient.getPullRequest(TEST_TOKEN, TEST_OWNER, TEST_REPO, TEST_PR_NUMBER));
        
        assertEquals("Failed to retrieve pull request", exception.getMessage());
        
        verify(getRequestedFor(urlEqualTo("/repos/test-owner/test-repo/pulls/123")));
    }
    
    @Test
    void testForbidden_403_ThrowsGitHubApiException() {
        // Given
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo/pulls/123"))
            .willReturn(aResponse()
                .withStatus(403)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Forbidden\"}")));
        
        // When & Then
        GitHubApiException exception = assertThrows(GitHubApiException.class, () ->
            gitHubApiClient.getPullRequest(TEST_TOKEN, TEST_OWNER, TEST_REPO, TEST_PR_NUMBER));
        
        assertEquals("Failed to retrieve pull request", exception.getMessage());
        
        verify(getRequestedFor(urlEqualTo("/repos/test-owner/test-repo/pulls/123")));
    }
    
    @Test
    void testRateLimitExceeded_429_ThrowsGitHubApiException() {
        // Given
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo/pulls/123"))
            .willReturn(aResponse()
                .withStatus(429)
                .withHeader("Content-Type", "application/json")
                .withHeader("X-RateLimit-Remaining", "0")
                .withHeader("X-RateLimit-Reset", String.valueOf(System.currentTimeMillis() / 1000 + 3600))
                .withBody("{\"message\":\"API rate limit exceeded\"}")));
        
        // When & Then
        GitHubApiException exception = assertThrows(GitHubApiException.class, () ->
            gitHubApiClient.getPullRequest(TEST_TOKEN, TEST_OWNER, TEST_REPO, TEST_PR_NUMBER));
        
        assertEquals("Failed to retrieve pull request", exception.getMessage());
        
        verify(getRequestedFor(urlEqualTo("/repos/test-owner/test-repo/pulls/123")));
    }
    
    @Test
    void testUnprocessableEntity_422_ThrowsGitHubApiException() {
        // Given
        stubFor(post(urlEqualTo("/repos/test-owner/test-repo/issues/123/comments"))
            .willReturn(aResponse()
                .withStatus(422)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Validation Failed\",\"errors\":[{\"field\":\"body\",\"code\":\"missing\"}]}")));
        
        // When & Then
        GitHubApiException exception = assertThrows(GitHubApiException.class, () ->
            gitHubApiClient.postComment(TEST_TOKEN, TEST_OWNER, TEST_REPO, TEST_PR_NUMBER, ""));
        
        assertEquals("Failed to post comment", exception.getMessage());
        
        verify(postRequestedFor(urlEqualTo("/repos/test-owner/test-repo/issues/123/comments")));
    }
    
    @Test
    void testMalformedJsonResponse_ThrowsGitHubApiException() {
        // Given
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo/pulls/123"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{ invalid json")));
        
        // When & Then
        assertThrows(Exception.class, () ->
            gitHubApiClient.getPullRequest(TEST_TOKEN, TEST_OWNER, TEST_REPO, TEST_PR_NUMBER));
        
        verify(getRequestedFor(urlEqualTo("/repos/test-owner/test-repo/pulls/123")));
    }
    
    @Test
    void testEmptyResponse_HandlesGracefully() {
        // Given
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo/pulls/123"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("")));
        
        // When & Then
        assertThrows(Exception.class, () ->
            gitHubApiClient.getPullRequest(TEST_TOKEN, TEST_OWNER, TEST_REPO, TEST_PR_NUMBER));
        
        verify(getRequestedFor(urlEqualTo("/repos/test-owner/test-repo/pulls/123")));
    }
    
    @Test
    void testConnectionRefused_ThrowsGitHubApiException() {
        // Given - Stop the server to simulate connection refused
        wireMockServer.stop();
        
        // When & Then
        GitHubApiException exception = assertThrows(GitHubApiException.class, () ->
            gitHubApiClient.getPullRequest(TEST_TOKEN, TEST_OWNER, TEST_REPO, TEST_PR_NUMBER));
        
        assertEquals("Network error", exception.getMessage());
        assertNotNull(exception.getCause());
    }
    
    @Test
    void testDeleteComment_NotFound_ThrowsGitHubApiException() {
        // Given
        stubFor(delete(urlEqualTo("/repos/test-owner/test-repo/issues/comments/999"))
            .willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Not Found\"}")));
        
        // When & Then
        GitHubApiException exception = assertThrows(GitHubApiException.class, () ->
            gitHubApiClient.deleteComment(TEST_TOKEN, TEST_OWNER, TEST_REPO, 999L));
        
        assertEquals("Failed to delete comment", exception.getMessage());
        
        verify(deleteRequestedFor(urlEqualTo("/repos/test-owner/test-repo/issues/comments/999")));
    }
    
    @Test
    void testUpdateComment_Conflict_ThrowsGitHubApiException() {
        // Given
        stubFor(patch(urlEqualTo("/repos/test-owner/test-repo/issues/comments/123"))
            .willReturn(aResponse()
                .withStatus(409)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Conflict\"}")));
        
        // When & Then
        GitHubApiException exception = assertThrows(GitHubApiException.class, () ->
            gitHubApiClient.updateComment(TEST_TOKEN, TEST_OWNER, TEST_REPO, 123L, "Updated comment"));
        
        assertEquals("Failed to update comment", exception.getMessage());
        
        verify(patchRequestedFor(urlEqualTo("/repos/test-owner/test-repo/issues/comments/123")));
    }
}