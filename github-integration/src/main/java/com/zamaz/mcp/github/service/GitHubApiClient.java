package com.zamaz.mcp.github.service;

import com.zamaz.mcp.github.dto.GitHubPullRequest;
import com.zamaz.mcp.github.dto.GitHubRepository;
import com.zamaz.mcp.github.dto.GitHubComment;
import com.zamaz.mcp.github.dto.GitHubUser;
import com.zamaz.mcp.github.exception.GitHubApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * GitHub API client service for handling all GitHub API interactions.
 * Supports authentication, PR operations, comments, and repository management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubApiClient {
    
    private final RestTemplate restTemplate;
    
    @Value("${github.api.base-url:https://api.github.com}")
    private String baseUrl;
    
    @Value("${github.api.version:application/vnd.github.v3+json}")
    private String apiVersion;
    
    /**
     * Retrieve a pull request by repository and PR number
     */
    public GitHubPullRequest getPullRequest(String token, String owner, String repo, int prNumber) {
        String url = String.format("%s/repos/%s/%s/pulls/%d", baseUrl, owner, repo, prNumber);
        
        try {
            HttpHeaders headers = createHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class
            );
            
            return mapToPullRequest(response.getBody());
            
        } catch (HttpClientErrorException e) {
            log.error("Failed to get pull request {}/{}/pulls/{}: {}", 
                owner, repo, prNumber, e.getMessage());
            throw new GitHubApiException("Failed to retrieve pull request", e);
        } catch (ResourceAccessException e) {
            log.error("Network error accessing GitHub API: {}", e.getMessage());
            throw new GitHubApiException("Network error", e);
        }
    }
    
    /**
     * List pull requests for a repository
     */
    @Cacheable(value = "github-prs", key = "#owner + ':' + #repo + ':' + #state")
    public List<GitHubPullRequest> listPullRequests(String token, String owner, String repo, 
                                                   String state, int page, int perPage) {
        String url = String.format("%s/repos/%s/%s/pulls?state=%s&page=%d&per_page=%d", 
            baseUrl, owner, repo, state, page, perPage);
        
        try {
            HttpHeaders headers = createHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<List> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, List.class
            );
            
            return response.getBody().stream()
                .map(pr -> mapToPullRequest((Map<String, Object>) pr))
                .toList();
                
        } catch (HttpClientErrorException e) {
            log.error("Failed to list pull requests for {}/{}: {}", 
                owner, repo, e.getMessage());
            throw new GitHubApiException("Failed to list pull requests", e);
        }
    }
    
    /**
     * Post a comment on a pull request
     */
    public GitHubComment postComment(String token, String owner, String repo, 
                                   int prNumber, String body) {
        String url = String.format("%s/repos/%s/%s/issues/%d/comments", 
            baseUrl, owner, repo, prNumber);
        
        try {
            HttpHeaders headers = createHeaders(token);
            Map<String, String> requestBody = Map.of("body", body);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, Map.class
            );
            
            return mapToComment(response.getBody());
            
        } catch (HttpClientErrorException e) {
            log.error("Failed to post comment on PR {}/{}/pulls/{}: {}", 
                owner, repo, prNumber, e.getMessage());
            throw new GitHubApiException("Failed to post comment", e);
        }
    }
    
    /**
     * Update a pull request comment
     */
    public GitHubComment updateComment(String token, String owner, String repo, 
                                     long commentId, String body) {
        String url = String.format("%s/repos/%s/%s/issues/comments/%d", 
            baseUrl, owner, repo, commentId);
        
        try {
            HttpHeaders headers = createHeaders(token);
            Map<String, String> requestBody = Map.of("body", body);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.PATCH, entity, Map.class
            );
            
            return mapToComment(response.getBody());
            
        } catch (HttpClientErrorException e) {
            log.error("Failed to update comment {} on {}/{}: {}", 
                commentId, owner, repo, e.getMessage());
            throw new GitHubApiException("Failed to update comment", e);
        }
    }
    
    /**
     * Delete a pull request comment
     */
    public void deleteComment(String token, String owner, String repo, long commentId) {
        String url = String.format("%s/repos/%s/%s/issues/comments/%d", 
            baseUrl, owner, repo, commentId);
        
        try {
            HttpHeaders headers = createHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            restTemplate.exchange(url, HttpMethod.DELETE, entity, Void.class);
            
        } catch (HttpClientErrorException e) {
            log.error("Failed to delete comment {} on {}/{}: {}", 
                commentId, owner, repo, e.getMessage());
            throw new GitHubApiException("Failed to delete comment", e);
        }
    }
    
    /**
     * Get repository information
     */
    @Cacheable(value = "github-repos", key = "#owner + ':' + #repo")
    public GitHubRepository getRepository(String token, String owner, String repo) {
        String url = String.format("%s/repos/%s/%s", baseUrl, owner, repo);
        
        try {
            HttpHeaders headers = createHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class
            );
            
            return mapToRepository(response.getBody());
            
        } catch (HttpClientErrorException e) {
            log.error("Failed to get repository {}/{}: {}", 
                owner, repo, e.getMessage());
            throw new GitHubApiException("Failed to get repository", e);
        }
    }
    
    /**
     * Update pull request status/labels
     */
    public GitHubPullRequest updatePullRequest(String token, String owner, String repo, 
                                             int prNumber, String state, List<String> labels) {
        String url = String.format("%s/repos/%s/%s/pulls/%d", baseUrl, owner, repo, prNumber);
        
        try {
            HttpHeaders headers = createHeaders(token);
            Map<String, Object> requestBody = new HashMap<>();
            if (state != null) {
                requestBody.put("state", state);
            }
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.PATCH, entity, Map.class
            );
            
            // Update labels separately if provided
            if (labels != null && !labels.isEmpty()) {
                updateLabels(token, owner, repo, prNumber, labels);
            }
            
            return mapToPullRequest(response.getBody());
            
        } catch (HttpClientErrorException e) {
            log.error("Failed to update PR {}/{}/pulls/{}: {}", 
                owner, repo, prNumber, e.getMessage());
            throw new GitHubApiException("Failed to update pull request", e);
        }
    }
    
    /**
     * Get authenticated user information
     */
    public GitHubUser getAuthenticatedUser(String token) {
        String url = String.format("%s/user", baseUrl);
        
        try {
            HttpHeaders headers = createHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, Map.class
            );
            
            return mapToUser(response.getBody());
            
        } catch (HttpClientErrorException e) {
            log.error("Failed to get authenticated user: {}", e.getMessage());
            throw new GitHubApiException("Failed to get user", e);
        }
    }
    
    /**
     * List comments on a pull request
     */
    public List<GitHubComment> listComments(String token, String owner, String repo, int prNumber) {
        String url = String.format("%s/repos/%s/%s/issues/%d/comments", 
            baseUrl, owner, repo, prNumber);
        
        try {
            HttpHeaders headers = createHeaders(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            ResponseEntity<List> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, List.class
            );
            
            return response.getBody().stream()
                .map(comment -> mapToComment((Map<String, Object>) comment))
                .toList();
                
        } catch (HttpClientErrorException e) {
            log.error("Failed to list comments on PR {}/{}/pulls/{}: {}", 
                owner, repo, prNumber, e.getMessage());
            throw new GitHubApiException("Failed to list comments", e);
        }
    }
    
    private HttpHeaders createHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", apiVersion);
        headers.set("User-Agent", "Kiro-GitHub-Integration/1.0");
        return headers;
    }
    
    private void updateLabels(String token, String owner, String repo, int prNumber, List<String> labels) {
        String url = String.format("%s/repos/%s/%s/issues/%d/labels", 
            baseUrl, owner, repo, prNumber);
        
        try {
            HttpHeaders headers = createHeaders(token);
            Map<String, Object> requestBody = Map.of("labels", labels);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            restTemplate.exchange(url, HttpMethod.PUT, entity, Map.class);
            
        } catch (HttpClientErrorException e) {
            log.warn("Failed to update labels for PR {}/{}/pulls/{}: {}", 
                owner, repo, prNumber, e.getMessage());
            // Don't throw exception for label updates
        }
    }
    
    private GitHubPullRequest mapToPullRequest(Map<String, Object> data) {
        return GitHubPullRequest.builder()
            .id(((Number) data.get("id")).longValue())
            .number((Integer) data.get("number"))
            .title((String) data.get("title"))
            .body((String) data.get("body"))
            .state((String) data.get("state"))
            .htmlUrl((String) data.get("html_url"))
            .user(mapToUser((Map<String, Object>) data.get("user")))
            .headRef((String) ((Map<String, Object>) data.get("head")).get("ref"))
            .baseRef((String) ((Map<String, Object>) data.get("base")).get("ref"))
            .createdAt(LocalDateTime.now()) // Parse from data.get("created_at")
            .updatedAt(LocalDateTime.now()) // Parse from data.get("updated_at")
            .build();
    }
    
    private GitHubComment mapToComment(Map<String, Object> data) {
        return GitHubComment.builder()
            .id(((Number) data.get("id")).longValue())
            .body((String) data.get("body"))
            .user(mapToUser((Map<String, Object>) data.get("user")))
            .htmlUrl((String) data.get("html_url"))
            .createdAt(LocalDateTime.now()) // Parse from data.get("created_at")
            .updatedAt(LocalDateTime.now()) // Parse from data.get("updated_at")
            .build();
    }
    
    private GitHubRepository mapToRepository(Map<String, Object> data) {
        return GitHubRepository.builder()
            .id(((Number) data.get("id")).longValue())
            .name((String) data.get("name"))
            .fullName((String) data.get("full_name"))
            .owner(mapToUser((Map<String, Object>) data.get("owner")))
            .htmlUrl((String) data.get("html_url"))
            .description((String) data.get("description"))
            .isPrivate((Boolean) data.get("private"))
            .defaultBranch((String) data.get("default_branch"))
            .build();
    }
    
    private GitHubUser mapToUser(Map<String, Object> data) {
        return GitHubUser.builder()
            .id(((Number) data.get("id")).longValue())
            .login((String) data.get("login"))
            .avatarUrl((String) data.get("avatar_url"))
            .htmlUrl((String) data.get("html_url"))
            .type((String) data.get("type"))
            .build();
    }
}