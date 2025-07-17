package com.zamaz.mcp.controller.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SubmitTurnToolResponse {
    private UUID responseId;
    private UUID roundId;
    private String status;
}
