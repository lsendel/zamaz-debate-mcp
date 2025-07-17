package com.zamaz.mcp.github.exception;

/**
 * Exception thrown when GitHub API operations fail
 */
public class GitHubApiException extends RuntimeException {
    
    public GitHubApiException(String message) {
        super(message);
    }
    
    public GitHubApiException(String message, Throwable cause) {
        super(message, cause);
    }
}