package com.zamaz.mcp.common.exception;

import lombok.Getter;
import org.springframework.http.ProblemDetail;

@Getter
public class ToolCallException extends RuntimeException {
    private final ProblemDetail problemDetail;

    public ToolCallException(ProblemDetail problemDetail) {
        super(problemDetail.getDetail());
        this.problemDetail = problemDetail;
    }
}
