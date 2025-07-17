package com.zamaz.mcp.controller.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ListDebatesToolRequest {
    private UUID organizationId;
    private String status;
}
