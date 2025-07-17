package com.zamaz.mcp.github.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.zamaz.mcp.github.dto.GitHubPullRequest;
import com.zamaz.mcp.github.dto.GitHubComment;
import com.zamaz.mcp.github.dto.GitHubRepository;
import com.zamaz.mcp.github.dto.GitHubUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Advanced tests for GitHubApiClient including pagination, caching, and complex scenarios
 */
@SpringBootTest
@TestPropertySource(properties = {
    "github.api.base-url=http://localhost:8089"
})
class GitHubApiClientAdvancedTest {
    
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
    void testListPullRequests_WithPagination_FirstPage() {
        // Given
        List<Map<String, Object>> mockPrsPage1 = List.of(
            createMockPullRequest("PR 1", 1),
            createMockPullRequest("PR 2", 2),
            createMockPullRequest("PR 3", 3)
        );
        
        stubFor(get(urlPathEqualTo("/repos/test-owner/test-repo/pulls"))
            .withQueryParam("state", equalTo("open"))
            .withQueryParam("page", equalTo("1"))
            .withQueryParam("per_page", equalTo("3"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withHeader("Link", "<https://api.github.com/repos/test-owner/test-repo/pulls?state=open&page=2&per_page=3>; rel=\"next\"")
                .withBody(toJson(mockPrsPage1))));
        
        // When
        List<GitHubPullRequest> result = gitHubApiClient.listPullRequests(
            TEST_TOKEN, TEST_OWNER, TEST_REPO, "open", 1, 3);
        
        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("PR 1", result.get(0).getTitle());
        assertEquals("PR 2", result.get(1).getTitle());
        assertEquals("PR 3", result.get(2).getTitle());
        
        verify(getRequestedFor(urlPathEqualTo("/repos/test-owner/test-repo/pulls"))
            .withQueryParam("page", equalTo("1"))
            .withQueryParam("per_page", equalTo("3")));
    }
    
    @Test
    void testListPullRequests_WithPagination_SecondPage() {
        // Given
        List<Map<String, Object>> mockPrsPage2 = List.of(
            createMockPullRequest("PR 4", 4),
            createMockPullRequest("PR 5", 5)
        );
        
        stubFor(get(urlPathEqualTo("/repos/test-owner/test-repo/pulls"))
            .withQueryParam("state", equalTo("open"))
            .withQueryParam("page", equalTo("2"))
            .withQueryParam("per_page", equalTo("3"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withHeader("Link", "<https://api.github.com/repos/test-owner/test-repo/pulls?state=open&page=1&per_page=3>; rel=\"prev\"")
                .withBody(toJson(mockPrsPage2))));
        
        // When
        List<GitHubPullRequest> result = gitHubApiClient.listPullRequests(
            TEST_TOKEN, TEST_OWNER, TEST_REPO, "open", 2, 3);
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("PR 4", result.get(0).getTitle());
        assertEquals("PR 5", result.get(1).getTitle());
        
        verify(getRequestedFor(urlPathEqualTo("/repos/test-owner/test-repo/pulls"))
            .withQueryParam("page", equalTo("2"))
            .withQueryParam("per_page", equalTo("3")));
    }
    
    @Test
    void testListPullRequests_EmptyResults() {
        // Given
        stubFor(get(urlPathEqualTo("/repos/test-owner/test-repo/pulls"))
            .withQueryParam("state", equalTo("closed"))
            .withQueryParam("page", equalTo("1"))
            .withQueryParam("per_page", equalTo("30"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[]")));
        
        // When
        List<GitHubPullRequest> result = gitHubApiClient.listPullRequests(
            TEST_TOKEN, TEST_OWNER, TEST_REPO, "closed", 1, 30);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(getRequestedFor(urlPathEqualTo("/repos/test-owner/test-repo/pulls")));
    }
    
    @Test
    void testListComments_LargeNumberOfComments() {
        // Given
        List<Map<String, Object>> mockComments = List.of(
            createMockComment("Comment 1", 1),
            createMockComment("Comment 2", 2),
            createMockComment("Comment 3", 3),
            createMockComment("Comment 4", 4),
            createMockComment("Comment 5", 5)
        );
        
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo/issues/123/comments"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(toJson(mockComments))));
        
        // When
        List<GitHubComment> result = gitHubApiClient.listComments(
            TEST_TOKEN, TEST_OWNER, TEST_REPO, TEST_PR_NUMBER);
        
        // Then
        assertNotNull(result);
        assertEquals(5, result.size());
        
        for (int i = 0; i < 5; i++) {
            assertEquals("Comment " + (i + 1), result.get(i).getBody());
        }
        
        verify(getRequestedFor(urlEqualTo("/repos/test-owner/test-repo/issues/123/comments")));
    }
    
    @Test
    void testUpdatePullRequest_WithLabels() {
        // Given
        Map<String, Object> mockPrResponse = createMockPullRequest("Updated PR", 123);
        
        stubFor(patch(urlEqualTo("/repos/test-owner/test-repo/pulls/123"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(toJson(mockPrResponse))));
        
        // Mock the labels update endpoint
        stubFor(put(urlEqualTo("/repos/test-owner/test-repo/issues/123/labels"))
            .withRequestBody(containing("bug"))
            .withRequestBody(containing("enhancement"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[{\"name\":\"bug\"},{\"name\":\"enhancement\"}]")));
        
        // When
        GitHubPullRequest result = gitHubApiClient.updatePullRequest(
            TEST_TOKEN, TEST_OWNER, TEST_REPO, TEST_PR_NUMBER, "closed", 
            List.of("bug", "enhancement"));
        
        // Then
        assertNotNull(result);
        assertEquals("Updated PR", result.getTitle());
        
        verify(patchRequestedFor(urlEqualTo("/repos/test-owner/test-repo/pulls/123")));
        verify(putRequestedFor(urlEqualTo("/repos/test-owner/test-repo/issues/123/labels")));
    }
    
    @Test
    void testUpdatePullRequest_LabelsUpdateFails_DoesNotThrow() {
        // Given
        Map<String, Object> mockPrResponse = createMockPullRequest("Updated PR", 123);
        
        stubFor(patch(urlEqualTo("/repos/test-owner/test-repo/pulls/123"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(toJson(mockPrResponse))));
        
        // Mock the labels update endpoint to fail
        stubFor(put(urlEqualTo("/repos/test-owner/test-repo/issues/123/labels"))
            .willReturn(aResponse()
                .withStatus(403)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"message\":\"Forbidden\"}")));
        
        // When & Then - Should not throw exception even if labels update fails
        assertDoesNotThrow(() -> {
            GitHubPullRequest result = gitHubApiClient.updatePullRequest(
                TEST_TOKEN, TEST_OWNER, TEST_REPO, TEST_PR_NUMBER, "closed", 
                List.of("bug", "enhancement"));
            
            assertNotNull(result);
            assertEquals("Updated PR", result.getTitle());
        });
        
        verify(patchRequestedFor(urlEqualTo("/repos/test-owner/test-repo/pulls/123")));
        verify(putRequestedFor(urlEqualTo("/repos/test-owner/test-repo/issues/123/labels")));
    }
    
    @Test
    void testGetRepository_PrivateRepo() {
        // Given
        Map<String, Object> mockRepoResponse = Map.of(
            "id", 456789,
            "name", "private-repo",
            "full_name", "test-owner/private-repo",
            "owner", createMockUser("test-owner", 1),
            "html_url", "https://github.com/test-owner/private-repo",
            "description", "A private repository",
            "private", true,
            "default_branch", "main"
        );
        
        stubFor(get(urlEqualTo("/repos/test-owner/private-repo"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(toJson(mockRepoResponse))));
        
        // When
        GitHubRepository result = gitHubApiClient.getRepository(TEST_TOKEN, TEST_OWNER, "private-repo");
        
        // Then
        assertNotNull(result);
        assertEquals("private-repo", result.getName());
        assertTrue(result.getIsPrivate());
        assertEquals("A private repository", result.getDescription());
        
        verify(getRequestedFor(urlEqualTo("/repos/test-owner/private-repo")));
    }
    
    @Test
    void testGetAuthenticatedUser_OrganizationAccount() {
        // Given
        Map<String, Object> mockOrgResponse = Map.of(
            "id", 54321,
            "login", "test-org",
            "avatar_url", "https://github.com/images/error/test-org_happy.gif",
            "html_url", "https://github.com/test-org",
            "type", "Organization"
        );
        
        stubFor(get(urlEqualTo("/user"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(toJson(mockOrgResponse))));
        
        // When
        GitHubUser result = gitHubApiClient.getAuthenticatedUser(TEST_TOKEN);
        
        // Then
        assertNotNull(result);
        assertEquals("test-org", result.getLogin());
        assertEquals("Organization", result.getType());
        
        verify(getRequestedFor(urlEqualTo("/user")));
    }
    
    @Test
    void testPostComment_WithSpecialCharacters() {
        // Given
        String specialComment = "This is a comment with special characters: `code`, **bold**, *italic*, @mention, #123, [link](https://example.com)";
        
        Map<String, Object> mockCommentResponse = createMockComment(specialComment, 789);
        
        stubFor(post(urlEqualTo("/repos/test-owner/test-repo/issues/123/comments"))
            .withRequestBody(containing(specialComment))
            .willReturn(aResponse()
                .withStatus(201)
                .withHeader("Content-Type", "application/json")
                .withBody(toJson(mockCommentResponse))));
        
        // When
        GitHubComment result = gitHubApiClient.postComment(
            TEST_TOKEN, TEST_OWNER, TEST_REPO, TEST_PR_NUMBER, specialComment);
        
        // Then
        assertNotNull(result);
        assertEquals(specialComment, result.getBody());
        
        verify(postRequestedFor(urlEqualTo("/repos/test-owner/test-repo/issues/123/comments")));
    }
    
    @Test
    void testGetPullRequest_WithMergeInformation() {
        // Given
        Map<String, Object> mockPrResponse = Map.of(
            "id", 123456,
            "number", 123,
            "title", "Test PR",
            "body", "This is a test pull request",
            "state", "closed",
            "html_url", "https://github.com/test-owner/test-repo/pull/123",
            "user", createMockUser("test-user", 1),
            "head", Map.of("ref", "feature-branch"),
            "base", Map.of("ref", "main"),
            "created_at", "2023-01-01T00:00:00Z",
            "updated_at", "2023-01-01T00:00:00Z",
            "merged", true,
            "mergeable", true,
            "mergeable_state", "clean",
            "merge_commit_sha", "abc123def456",
            "additions", 50,
            "deletions", 10,
            "changed_files", 3
        );
        
        stubFor(get(urlEqualTo("/repos/test-owner/test-repo/pulls/123"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(toJson(mockPrResponse))));
        
        // When
        GitHubPullRequest result = gitHubApiClient.getPullRequest(TEST_TOKEN, TEST_OWNER, TEST_REPO, TEST_PR_NUMBER);
        
        // Then
        assertNotNull(result);
        assertEquals("Test PR", result.getTitle());
        assertEquals("closed", result.getState());
        
        verify(getRequestedFor(urlEqualTo("/repos/test-owner/test-repo/pulls/123")));
    }
    
    // Helper methods
    
    private Map<String, Object> createMockPullRequest(String title, int number) {
        return Map.of(
            "id", 123456 + number,
            "number", number,
            "title", title,
            "body", "This is a test pull request",
            "state", "open",
            "html_url", "https://github.com/test-owner/test-repo/pull/" + number,
            "user", createMockUser("test-user", 1),
            "head", Map.of("ref", "feature-branch"),
            "base", Map.of("ref", "main"),
            "created_at", "2023-01-01T00:00:00Z",
            "updated_at", "2023-01-01T00:00:00Z"
        );
    }
    
    private Map<String, Object> createMockComment(String body, int id) {
        return Map.of(
            "id", id,
            "body", body,
            "user", createMockUser("test-user", 1),
            "html_url", "https://github.com/test-owner/test-repo/pull/123#issuecomment-" + id,
            "created_at", "2023-01-01T00:00:00Z",
            "updated_at", "2023-01-01T00:00:00Z"
        );
    }
    
    private Map<String, Object> createMockUser(String login, int id) {
        return Map.of(
            "id", id,
            "login", login,
            "avatar_url", "https://github.com/images/error/" + login + "_happy.gif",
            "html_url", "https://github.com/" + login,
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