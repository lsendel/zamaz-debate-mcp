package com.zamaz.mcp.github.service;

import com.zamaz.mcp.github.dto.GitHubPullRequest;
import com.zamaz.mcp.github.dto.GitHubComment;
import com.zamaz.mcp.github.dto.GitHubRepository;
import com.zamaz.mcp.github.dto.GitHubUser;
import com.zamaz.mcp.github.exception.GitHubApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple test to validate GitHubApiClient can be instantiated and DTOs work correctly
 */
class GitHubApiClientSimpleTest {
    
    private GitHubApiClient gitHubApiClient;
    
    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplateBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(10))
            .build();
        
        gitHubApiClient = new GitHubApiClient(restTemplate);
    }
    
    @Test
    void testServiceInstantiation() {
        assertNotNull(gitHubApiClient);
    }
    
    @Test
    void testGitHubPullRequestDTO() {
        GitHubUser user = GitHubUser.builder()
            .id(123L)
            .login("testuser")
            .avatarUrl("https://example.com/avatar.jpg")
            .htmlUrl("https://github.com/testuser")
            .type("User")
            .build();
        
        GitHubPullRequest pr = GitHubPullRequest.builder()
            .id(456L)
            .number(789)
            .title("Test PR")
            .body("This is a test pull request")
            .state("open")
            .htmlUrl("https://github.com/owner/repo/pull/789")
            .user(user)
            .headRef("feature-branch")
            .baseRef("main")
            .build();
        
        assertNotNull(pr);
        assertEquals(456L, pr.getId());
        assertEquals(789, pr.getNumber());
        assertEquals("Test PR", pr.getTitle());
        assertEquals("This is a test pull request", pr.getBody());
        assertEquals("open", pr.getState());
        assertEquals("https://github.com/owner/repo/pull/789", pr.getHtmlUrl());
        assertEquals("feature-branch", pr.getHeadRef());
        assertEquals("main", pr.getBaseRef());
        
        assertNotNull(pr.getUser());
        assertEquals("testuser", pr.getUser().getLogin());
        assertEquals(123L, pr.getUser().getId());
    }
    
    @Test
    void testGitHubCommentDTO() {
        GitHubUser user = GitHubUser.builder()
            .id(123L)
            .login("testuser")
            .build();
        
        GitHubComment comment = GitHubComment.builder()
            .id(789L)
            .body("This is a test comment")
            .user(user)
            .htmlUrl("https://github.com/owner/repo/pull/123#issuecomment-789")
            .build();
        
        assertNotNull(comment);
        assertEquals(789L, comment.getId());
        assertEquals("This is a test comment", comment.getBody());
        assertEquals("https://github.com/owner/repo/pull/123#issuecomment-789", comment.getHtmlUrl());
        
        assertNotNull(comment.getUser());
        assertEquals("testuser", comment.getUser().getLogin());
    }
    
    @Test
    void testGitHubRepositoryDTO() {
        GitHubUser owner = GitHubUser.builder()
            .id(123L)
            .login("owner")
            .build();
        
        GitHubRepository repo = GitHubRepository.builder()
            .id(456L)
            .name("test-repo")
            .fullName("owner/test-repo")
            .owner(owner)
            .htmlUrl("https://github.com/owner/test-repo")
            .description("A test repository")
            .isPrivate(false)
            .defaultBranch("main")
            .build();
        
        assertNotNull(repo);
        assertEquals(456L, repo.getId());
        assertEquals("test-repo", repo.getName());
        assertEquals("owner/test-repo", repo.getFullName());
        assertEquals("https://github.com/owner/test-repo", repo.getHtmlUrl());
        assertEquals("A test repository", repo.getDescription());
        assertEquals(false, repo.getIsPrivate());
        assertEquals("main", repo.getDefaultBranch());
        
        assertNotNull(repo.getOwner());
        assertEquals("owner", repo.getOwner().getLogin());
    }
    
    @Test
    void testGitHubUserDTO() {
        GitHubUser user = GitHubUser.builder()
            .id(123L)
            .login("testuser")
            .avatarUrl("https://example.com/avatar.jpg")
            .htmlUrl("https://github.com/testuser")
            .type("User")
            .name("Test User")
            .email("test@example.com")
            .company("Test Company")
            .location("Test Location")
            .bio("Test bio")
            .blog("https://testuser.com")
            .siteAdmin(false)
            .publicRepos(10)
            .publicGists(5)
            .followers(100)
            .following(50)
            .build();
        
        assertNotNull(user);
        assertEquals(123L, user.getId());
        assertEquals("testuser", user.getLogin());
        assertEquals("https://example.com/avatar.jpg", user.getAvatarUrl());
        assertEquals("https://github.com/testuser", user.getHtmlUrl());
        assertEquals("User", user.getType());
        assertEquals("Test User", user.getName());
        assertEquals("test@example.com", user.getEmail());
        assertEquals("Test Company", user.getCompany());
        assertEquals("Test Location", user.getLocation());
        assertEquals("Test bio", user.getBio());
        assertEquals("https://testuser.com", user.getBlog());
        assertEquals(false, user.getSiteAdmin());
        assertEquals(10, user.getPublicRepos());
        assertEquals(5, user.getPublicGists());
        assertEquals(100, user.getFollowers());
        assertEquals(50, user.getFollowing());
    }
    
    @Test
    void testGitHubApiException() {
        GitHubApiException exception1 = new GitHubApiException("Test message");
        assertEquals("Test message", exception1.getMessage());
        assertNull(exception1.getCause());
        
        RuntimeException cause = new RuntimeException("Cause message");
        GitHubApiException exception2 = new GitHubApiException("Test message", cause);
        assertEquals("Test message", exception2.getMessage());
        assertEquals(cause, exception2.getCause());
    }
}