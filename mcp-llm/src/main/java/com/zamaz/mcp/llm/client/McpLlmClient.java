package com.zamaz.mcp.llm.client;

import com.zamaz.mcp.llm.dto.LlmRequest;
import com.zamaz.mcp.llm.dto.LlmResponseDto;

/**
 * Client interface for interacting with the MCP LLM service.
 */
public interface McpLlmClient {

    /**
     * Generates a response from the LLM using the provided request.
     *
     * @param request The LLM request
     * @return The LLM response
     */
    LlmResponseDto generate(LlmRequest request);
}