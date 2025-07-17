package com.zamaz.mcp.github.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * GitHub Pull Request DTO
 */
@Data
@Builder
public class GitHubPullRequest {
    private Long id;
    private Integer number;
    private String title;
    private String body;
    private String state;
    private String htmlUrl;
    private GitHubUser user;
    private String headRef;
    private String baseRef;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String mergeCommitSha;
    private Boolean merged;
    private Boolean mergeable;
    private String mergeableState;
    private Integer additions;
    private Integer deletions;
    private Integer changedFiles;
}