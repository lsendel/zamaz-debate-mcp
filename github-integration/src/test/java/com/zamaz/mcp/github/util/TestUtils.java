package com.zamaz.mcp.github.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Utility class for test helper methods
 */
public class TestUtils {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Convert object to JSON string
     */
    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize to JSON", e);
        }
    }
    
    /**
     * Create a mock GitHub user response
     */
    public static Map<String, Object> createMockUser(String login, long id) {
        return Map.of(
            "id", id,
            "login", login,
            "avatar_url", "https://github.com/images/error/" + login + "_happy.gif",
            "html_url", "https://github.com/" + login,
            "type", "User"
        );
    }
    
    /**
     * Create a mock GitHub pull request response
     */
    public static Map<String, Object> createMockPullRequest(String title, int number, String state) {
        return Map.of(
            "id", 123456 + number,
            "number", number,
            "title", title,
            "body", "This is a test pull request",
            "state", state,
            "html_url", "https://github.com/test-owner/test-repo/pull/" + number,
            "user", createMockUser("test-user", 1),
            "head", Map.of("ref", "feature-branch"),
            "base", Map.of("ref", "main"),
            "created_at", "2023-01-01T00:00:00Z",
            "updated_at", "2023-01-01T00:00:00Z"
        );
    }
    
    /**
     * Create a mock GitHub comment response
     */
    public static Map<String, Object> createMockComment(String body, long id) {
        return Map.of(
            "id", id,
            "body", body,
            "user", createMockUser("test-user", 1),
            "html_url", "https://github.com/test-owner/test-repo/pull/123#issuecomment-" + id,
            "created_at", "2023-01-01T00:00:00Z",
            "updated_at", "2023-01-01T00:00:00Z"
        );
    }
    
    /**
     * Create a mock GitHub repository response
     */
    public static Map<String, Object> createMockRepository(String name, String owner, boolean isPrivate) {
        return Map.of(
            "id", 456789,
            "name", name,
            "full_name", owner + "/" + name,
            "owner", createMockUser(owner, 1),
            "html_url", "https://github.com/" + owner + "/" + name,
            "description", "A test repository",
            "private", isPrivate,
            "default_branch", "main"
        );
    }
}