package com.zamaz.mcp.github.dto;

import lombok.Builder;
import lombok.Data;

/**
 * GitHub User DTO
 */
@Data
@Builder
public class GitHubUser {
    private Long id;
    private String login;
    private String avatarUrl;
    private String htmlUrl;
    private String type;
    private String name;
    private String email;
    private String company;
    private String location;
    private String bio;
    private String blog;
    private Boolean siteAdmin;
    private Integer publicRepos;
    private Integer publicGists;
    private Integer followers;
    private Integer following;
}