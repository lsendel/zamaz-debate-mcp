package com.zamaz.mcp.context.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Data Transfer Object for Context entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContextDto {
    private UUID id;
    private UUID organizationId;
    private UUID userId;
    private String name;
    private String description;
    private String status;
    private Map<String, Object> metadata;
    private Integer totalTokens;
    private Integer messageCount;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastAccessedAt;
    private List<MessageDto> recentMessages;
}