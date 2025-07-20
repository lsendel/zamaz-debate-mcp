package com.zamaz.mcp.common.domain.llm;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Value object representing a response from an LLM service.
 */
public class LlmResponse {
    private final String text;
    private final Duration processingTime;
    private final List<CritiqueIteration> iterations;
    private final Map<String, Object> metadata;

    private LlmResponse(Builder builder) {
        this.text = Objects.requireNonNull(builder.text, "Text cannot be null");
        this.processingTime = Objects.requireNonNull(builder.processingTime, "Processing time cannot be null");
        this.iterations = Collections.unmodifiableList(new ArrayList<>(builder.iterations));
        this.metadata = Collections.unmodifiableMap(new HashMap<>(builder.metadata));
    }

    /**
     * Returns the text of the response.
     *
     * @return The response text
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the processing time of the response.
     *
     * @return The processing time
     */
    public Duration getProcessingTime() {
        return processingTime;
    }

    /**
     * Returns the critique iterations if this response was generated using a
     * self-critique loop.
     *
     * @return An unmodifiable list of critique iterations
     */
    public List<CritiqueIteration> getIterations() {
        return iterations;
    }

    /**
     * Returns the metadata associated with this response.
     *
     * @return An unmodifiable map of metadata
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Returns a new builder for creating LlmResponse instances.
     *
     * @return A new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating LlmResponse instances.
     */
    public static class Builder {
        private String text;
        private Duration processingTime;
        private List<CritiqueIteration> iterations = new ArrayList<>();
        private Map<String, Object> metadata = new HashMap<>();

        private Builder() {
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder processingTime(Duration processingTime) {
            this.processingTime = processingTime;
            return this;
        }

        public Builder iterations(List<CritiqueIteration> iterations) {
            this.iterations = new ArrayList<>(iterations);
            return this;
        }

        public Builder addIteration(CritiqueIteration iteration) {
            this.iterations.add(iteration);
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

        public LlmResponse build() {
            return new LlmResponse(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LlmResponse that = (LlmResponse) o;
        return text.equals(that.text) &&
                processingTime.equals(that.processingTime) &&
                iterations.equals(that.iterations) &&
                metadata.equals(that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, processingTime, iterations, metadata);
    }

    @Override
    public String toString() {
        return "LlmResponse{" +
                "text='" + text + '\'' +
                ", processingTime=" + processingTime +
                ", iterations=" + iterations.size() +
                ", metadata=" + metadata +
                '}';
    }
}