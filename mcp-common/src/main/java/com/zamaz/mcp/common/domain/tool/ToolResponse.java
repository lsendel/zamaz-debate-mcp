package com.zamaz.mcp.common.domain.tool;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Value object representing a response from an external tool.
 */
public class ToolResponse {
    private final String toolName;
    private final Object result;
    private final boolean success;
    private final String errorMessage;
    private final Instant timestamp;
    private final Map<String, Object> metadata;

    private ToolResponse(Builder builder) {
        this.toolName = Objects.requireNonNull(builder.toolName, "Tool name cannot be null");
        this.result = builder.result;
        this.success = builder.success;
        this.errorMessage = builder.errorMessage;
        this.timestamp = Objects.requireNonNull(builder.timestamp, "Timestamp cannot be null");
        this.metadata = Collections.unmodifiableMap(new HashMap<>(builder.metadata));
    }

    /**
     * Returns the name of the tool that was called.
     *
     * @return The tool name
     */
    public String getToolName() {
        return toolName;
    }

    /**
     * Returns the result of the tool call.
     *
     * @return The result, or null if the call failed
     */
    public Object getResult() {
        return result;
    }

    /**
     * Returns whether the tool call was successful.
     *
     * @return True if the call was successful, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the error message if the tool call failed.
     *
     * @return The error message, or null if the call was successful
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns the timestamp when the tool call was executed.
     *
     * @return The timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the metadata associated with this tool response.
     *
     * @return An unmodifiable map of metadata
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Returns a new builder for creating ToolResponse instances.
     *
     * @return A new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating ToolResponse instances.
     */
    public static class Builder {
        private String toolName;
        private Object result;
        private boolean success = true;
        private String errorMessage;
        private Instant timestamp = Instant.now();
        private Map<String, Object> metadata = new HashMap<>();

        private Builder() {
        }

        public Builder toolName(String toolName) {
            this.toolName = toolName;
            return this;
        }

        public Builder result(Object result) {
            this.result = result;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            this.success = false;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = new HashMap<>(metadata);
            return this;
        }

        public Builder addMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public ToolResponse build() {
            return new ToolResponse(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ToolResponse that = (ToolResponse) o;
        return success == that.success &&
                toolName.equals(that.toolName) &&
                Objects.equals(result, that.result) &&
                Objects.equals(errorMessage, that.errorMessage) &&
                timestamp.equals(that.timestamp) &&
                metadata.equals(that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(toolName, result, success, errorMessage, timestamp, metadata);
    }

    @Override
    public String toString() {
        return "ToolResponse{" +
                "toolName='" + toolName + '\'' +
                ", result=" + result +
                ", success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}