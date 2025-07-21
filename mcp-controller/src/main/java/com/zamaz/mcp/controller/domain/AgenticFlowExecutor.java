package com.zamaz.mcp.controller.domain;

import com.zamaz.mcp.common.domain.agentic.AgenticFlow;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowResult;

/**
 * Interface for executing agentic flows.
 */
public interface AgenticFlowExecutor {
    
    /**
     * Executes an agentic flow with the given prompt and context.
     */
    AgenticFlowResult execute(
        AgenticFlow flow,
        String prompt,
        ExecutionContext context
    );
}