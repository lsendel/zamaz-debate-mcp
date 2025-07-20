package com.zamaz.mcp.common.domain.agentic;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing the unique identifier for an agentic flow.
 */
public class AgenticFlowId {
    private final String value;

    /**
     * Creates a new AgenticFlowId with a random UUID.
     */
    public AgenticFlowId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Creates a new AgenticFlowId with the specified value.
     *
     * @param value The string value of the ID
     */
    public AgenticFlowId(String value) {
        this.value = Objects.requireNonNull(value, "AgenticFlowId value cannot be null");
    }

    /**
     * Returns the string value of this ID.
     *
     * @return The string value
     */
    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AgenticFlowId that = (AgenticFlowId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}