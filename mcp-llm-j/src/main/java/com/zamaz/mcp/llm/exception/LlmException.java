package com.zamaz.mcp.llm.exception;

public class LlmException extends RuntimeException {
    public LlmException(String message) {
        super(message);
    }
    
    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}