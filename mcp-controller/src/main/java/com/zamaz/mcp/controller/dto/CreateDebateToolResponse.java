package com.zamaz.mcp.controller.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CreateDebateToolResponse {
    private UUID debateId;
    private String status;
    private String topic;
}
