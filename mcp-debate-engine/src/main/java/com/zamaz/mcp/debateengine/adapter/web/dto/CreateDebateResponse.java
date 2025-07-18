package com.zamaz.mcp.debateengine.adapter.web.dto;

/**
 * Response DTO for debate creation.
 */
public record CreateDebateResponse(
    String debateId,
    String message
) {
}