package com.zamaz.mcp.llm.dto;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Data Transfer Object for LLM responses.
 */
public class LlmResponseDto {
    private final String text;
    private final String model;
    private final int tokens;
    private final Map<String, Object> metadata;

    private LlmResponseDto(Builder builder) {
        this.text = Objects.requireNonNull(builder.text, "Text cannot be null");
        this.model = builder.model;
        this.tokens = builder.tokens;
        this.metadata = new HashMap<>(builder.metadata);
    }

    /**
     * Returns the text of this response.
     *
     * @return The text
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the model that generated this response.
     *
     * @return The model
     */
    public String getModel() {
        return model;
    }

    /**
     * Returns the number of tokens in this response.
     *
     * @return The token count
     */
    public int getTokens() {
        return tokens;
    }

    /**
     * Returns the metadata for this response.
     *
     * @return The metadata
     */
    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    /**
     * Returns a new builder for creating LlmResponseDto instances.
     *
     * @return A new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating LlmResponseDto instances.
     */
    public static class Builder {
        private String text;
        private String model;
        private int tokens;
        private Map<String, Object> metadata = new HashMap<>();

        private Builder() {
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder tokens(int tokens) {
            this.tokens = tokens;
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

        public LlmResponseDto build() {
            return new LlmResponseDto(this);
        }
    }
}