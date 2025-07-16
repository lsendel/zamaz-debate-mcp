package com.zamaz.mcp.context.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for context window retrieval.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContextWindowResponse {
    private List<MessageDto> messages;
    private Integer totalTokens;
    private Integer messageCount;
    private Boolean truncated;
    private String truncationStrategy;
}