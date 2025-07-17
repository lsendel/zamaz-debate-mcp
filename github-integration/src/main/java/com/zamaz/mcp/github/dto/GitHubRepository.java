package com.zamaz.mcp.github.dto;

import lombok.Builder;
import lombok.Data;

/**
 * GitHub Repository DTO
 */
@Data
@Builder
public class GitHubRepository {
    private Long id;
    private String name;
    private String fullName;
    private GitHubUser owner;
    private String htmlUrl;
    private String description;
    private Boolean isPrivate;
    private String defaultBranch;
    private String language;
    private Integer forksCount;
    private Integer stargazersCount;
    private Integer watchersCount;
    private Integer size;
    private String cloneUrl;
    private String sshUrl;
    private String homepage;
}