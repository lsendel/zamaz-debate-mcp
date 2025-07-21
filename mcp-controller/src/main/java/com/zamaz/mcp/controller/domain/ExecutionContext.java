package com.zamaz.mcp.controller.domain;

import lombok.*;
import java.util.UUID;

/**
 * Context for agentic flow execution.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionContext {
    private UUID debateId;
    private UUID organizationId;
    private UUID participantId;
    private boolean forceRefresh;
    private long executionTime;
    private String requestId;
}