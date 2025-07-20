package com.zamaz.mcp.llm.dto;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Data Transfer Object for LLM requests.
 */
public class LlmRequest {
    private final String prompt;
    private final Map<String, Object> parameters;
    private final ResponseFormat responseFormat;

    private LlmRequest(Builder builder) {
        this.prompt = Objects.requireNonNull(builder.prompt, "Prompt cannot be null");
        this.parameters = new HashMap<>(builder.parameters);
        this.responseFormat = builder.responseFormat;
    }

    /**
     * Returns the prompt for this request.
     *
     * @return The prompt
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * Returns the parameters for this request.
     *
     * @return The parameters
     */
    public Map<String, Object> getParameters() {
        return new HashMap<>(parameters);
    }

    /**
     * Returns the response format for this request.
     *
     * @return The response format
     */
    public ResponseFormat getResponseFormat() {
        return responseFormat;
    }

    /**
     * Returns a new builder for creating LlmRequest instances.
     *
     * @return A new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating LlmRequest instances.
     */
    public static class Builder {
        private String prompt;
        private Map<String, Object> parameters = new HashMap<>();
        private ResponseFormat responseFormat;

        private Builder() {
        }

        public Builder prompt(String prompt) {
            this.prompt = prompt;
            return this;
        }

        public Builder parameters(Map<String, Object> parameters) {
            this.parameters = new HashMap<>(parameters);
            return this;
        }

        public Builder addParameter(String key, Object value) {
            this.parameters.put(key, value);
            return this;
        }

        public Builder responseFormat(ResponseFormat responseFormat) {
            this.responseFormat = responseFormat;
            return this;
        }

        public LlmRequest build() {
            return new LlmRequest(this);
        }
    }
}