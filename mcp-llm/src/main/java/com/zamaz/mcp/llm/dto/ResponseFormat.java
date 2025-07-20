package com.zamaz.mcp.llm.dto;

/**
 * Enumeration of supported response formats for LLM requests.
 */
public enum ResponseFormat {
    /**
     * Standard text response.
     */
    TEXT,

    /**
     * Response with internal monologue reasoning.
     */
    INTERNAL_MONOLOGUE,

    /**
     * Response with self-critique loop.
     */
    SELF_CRITIQUE,

    /**
     * Response with JSON structure.
     */
    JSON,

    /**
     * Response with tool calls.
     */
    TOOL_CALLS,

    /**
     * Response with multiple agent perspectives.
     */
    MULTI_AGENT
}