package com.zamaz.mcp.controller.adapter.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for starting a debate.
 */
@Schema(description = "Request to start a debate (currently no parameters needed)")
public record StartDebateRequest() {
    
    public static StartDebateRequest empty() {
        return new StartDebateRequest();
    }
}