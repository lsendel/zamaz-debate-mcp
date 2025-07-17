package com.zamaz.mcp.controller.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CreateDebateToolRequest {
    private String topic;
    private String format;
    private UUID organizationId;
    private String title;
    private String description;
    private Integer maxRounds;
    private JsonNode settings;
    private List<String> participants;
}
