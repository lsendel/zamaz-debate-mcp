package com.zamaz.mcp.controller.domain;

import lombok.*;
import java.util.UUID;

/**
 * Request for flow execution in batch operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowExecutionRequest {
    private UUID flowId;
    private String prompt;
    private ExecutionContext context;
}