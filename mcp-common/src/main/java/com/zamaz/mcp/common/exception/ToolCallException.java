package com.zamaz.mcp.common.exception;

import org.springframework.http.ProblemDetail;

import lombok.Getter;

@Getter
public class ToolCallException extends RuntimeException {

    private final ProblemDetail problemDetail;

    public ToolCallException(String message, ProblemDetail problemDetail) {
        super(message);
        this.problemDetail = problemDetail;
    }

    public ToolCallException(String message, Throwable cause, ProblemDetail problemDetail) {
        super(message, cause);
        this.problemDetail = problemDetail;
    }

    public ToolCallException(String message) {
        super(message);
        this.problemDetail = ProblemDetail.forStatus(500);
        this.problemDetail.setDetail(message);
    }

    public ToolCallException(String message, Throwable cause) {
        super(message, cause);
        this.problemDetail = ProblemDetail.forStatus(500);
        this.problemDetail.setDetail(message);
    }
}