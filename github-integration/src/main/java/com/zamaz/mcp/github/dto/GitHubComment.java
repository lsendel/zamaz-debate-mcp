package com.zamaz.mcp.github.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * GitHub Comment DTO
 */
@Data
@Builder
public class GitHubComment {
    private Long id;
    private String body;
    private GitHubUser user;
    private String htmlUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String authorAssociation;
    private String nodeId;
    private String issueUrl;
}