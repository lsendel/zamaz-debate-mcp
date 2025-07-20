package com.zamaz.mcp.common.exception;

import org.springframework.http.ProblemDetail;

/**
 * Exception thrown when a tool call is made to a tool that is not supported.
 */
public class UnsupportedToolException extends ToolCallException {

    public UnsupportedToolException(String toolName) {
        super("Unsupported tool: " + toolName);
        ProblemDetail problemDetail = ProblemDetail.forStatus(400);
        problemDetail.setTitle("Unsupported Tool");
        problemDetail.setDetail("The tool '" + toolName + "' is not supported");
    }
}