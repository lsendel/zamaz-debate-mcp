package com.zamaz.mcp.common.domain.tool;

/**
 * Port interface for external tools that can be called by the system.
 */
public interface ExternalToolPort {

    /**
     * Returns the name of this tool.
     *
     * @return The tool name
     */
    String getToolName();

    /**
     * Executes a tool call and returns the response.
     *
     * @param toolCall The tool call to execute
     * @return The tool response
     * @throws com.zamaz.mcp.common.exception.ToolCallException if the tool call
     *                                                          fails
     */
    ToolResponse executeToolCall(ToolCall toolCall);

    /**
     * Returns whether this tool can handle the specified tool call.
     *
     * @param toolCall The tool call to check
     * @return True if this tool can handle the call, false otherwise
     */
    default boolean canHandle(ToolCall toolCall) {
        return getToolName().equals(toolCall.getTool());
    }
}