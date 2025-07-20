package com.zamaz.mcp.common.domain.tool;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Value object representing a call to an external tool.
 */
public class ToolCall {
    private final String tool;
    private final Map<String, Object> parameters;

    /**
     * Creates a new ToolCall with the specified parameters.
     *
     * @param tool       The name of the tool to call
     * @param parameters The parameters for the tool call
     */
    public ToolCall(String tool, Map<String, Object> parameters) {
        this.tool = Objects.requireNonNull(tool, "Tool name cannot be null");
        this.parameters = new HashMap<>(Objects.requireNonNull(parameters, "Parameters cannot be null"));
    }

    /**
     * Returns the name of the tool to call.
     *
     * @return The tool name
     */
    public String getTool() {
        return tool;
    }

    /**
     * Returns the parameters for the tool call.
     *
     * @return An unmodifiable view of the parameters
     */
    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ToolCall toolCall = (ToolCall) o;
        return tool.equals(toolCall.tool) &&
                parameters.equals(toolCall.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tool, parameters);
    }

    @Override
    public String toString() {
        return "ToolCall{" +
                "tool='" + tool + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}