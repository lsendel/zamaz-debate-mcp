package com.zamaz.mcp.controller.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SubmitTurnToolRequest {
    private UUID debateId;
    private UUID participantId;
    private String content;
}
