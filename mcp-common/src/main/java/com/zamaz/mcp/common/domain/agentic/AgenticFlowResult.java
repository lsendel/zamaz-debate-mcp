package com.zamaz.mcp.common.domain.agentic;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Value object representing the result of processing a prompt through an
 * agentic flow.
 */
public class AgenticFlowResult {
    private final String originalPrompt;
    private final String enhancedPrompt;
    private final String fullResponse;
    private final String finalResponse;
    private final String reasoning;
    private final List<ProcessingStep> processingSteps;
    private final Duration processingTime;
    private final Integer confidenceScore;
    private final boolean responseChanged;
    private final Map<String, Object> metrics;

    private AgenticFlowResult(Builder builder) {
        this.originalPrompt = Objects.requireNonNull(builder.originalPrompt, "Original prompt cannot be null");
        this.enhancedPrompt = builder.enhancedPrompt;
        this.fullResponse = Objects.requireNonNull(builder.fullResponse, "Full response cannot be null");
        this.finalResponse = Objects.requireNonNull(builder.finalResponse, "Final response cannot be null");
        this.reasoning = builder.reasoning;
        this.processingSteps = Collections.unmodifiableList(new ArrayList<>(builder.processingSteps));
        this.processingTime = Objects.requireNonNull(builder.processingTime, "Processing time cannot be null");
        this.confidenceScore = builder.confidenceScore;
        this.responseChanged = builder.responseChanged;
        this.metrics = Collections.unmodifiableMap(new HashMap<>(builder.metrics));
    }

    /**
     * Returns the original prompt that was processed.
     *
     * @return The original prompt
     */
    public String getOriginalPrompt() {
        return originalPrompt;
    }

    /**
     * Returns the enhanced prompt that was sent to the LLM.
     *
     * @return The enhanced prompt, or null if not applicable
     */
    public String getEnhancedPrompt() {
        return enhancedPrompt;
    }

    /**
     * Returns the full response from the LLM.
     *
     * @return The full response
     */
    public String getFullResponse() {
        return fullResponse;
    }

    /**
     * Returns the final response after processing.
     *
     * @return The final response
     */
    public String getFinalResponse() {
        return finalResponse;
    }

    /**
     * Returns the reasoning process extracted from the response.
     *
     * @return The reasoning, or null if not applicable
     */
    public String getReasoning() {
        return reasoning;
    }

    /**
     * Returns the steps taken during processing.
     *
     * @return An unmodifiable list of processing steps
     */
    public List<ProcessingStep> getProcessingSteps() {
        return processingSteps;
    }

    /**
     * Returns the time taken to process the prompt.
     *
     * @return The processing time
     */
    public Duration getProcessingTime() {
        return processingTime;
    }

    /**
     * Returns the confidence score of the response.
     *
     * @return The confidence score, or null if not applicable
     */
    public Integer getConfidenceScore() {
        return confidenceScore;
    }

    /**
     * Returns whether the response was changed during processing.
     *
     * @return True if the response was changed, false otherwise
     */
    public boolean isResponseChanged() {
        return responseChanged;
    }

    /**
     * Returns the metrics collected during processing.
     *
     * @return An unmodifiable map of metrics
     */
    public Map<String, Object> getMetrics() {
        return metrics;
    }

    /**
     * Returns a new builder for creating AgenticFlowResult instances.
     *
     * @return A new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating AgenticFlowResult instances.
     */
    public static class Builder {
        private String originalPrompt;
        private String enhancedPrompt;
        private String fullResponse;
        private String finalResponse;
        private String reasoning;
        private List<ProcessingStep> processingSteps = new ArrayList<>();
        private Duration processingTime;
        private Integer confidenceScore;
        private boolean responseChanged;
        private Map<String, Object> metrics = new HashMap<>();

        private Builder() {
        }

        public Builder originalPrompt(String originalPrompt) {
            this.originalPrompt = originalPrompt;
            return this;
        }

        public Builder enhancedPrompt(String enhancedPrompt) {
            this.enhancedPrompt = enhancedPrompt;
            return this;
        }

        public Builder fullResponse(String fullResponse) {
            this.fullResponse = fullResponse;
            return this;
        }

        public Builder finalResponse(String finalResponse) {
            this.finalResponse = finalResponse;
            return this;
        }

        public Builder reasoning(String reasoning) {
            this.reasoning = reasoning;
            return this;
        }

        public Builder processingSteps(List<ProcessingStep> processingSteps) {
            this.processingSteps = new ArrayList<>(processingSteps);
            return this;
        }

        public Builder addProcessingStep(ProcessingStep processingStep) {
            this.processingSteps.add(processingStep);
            return this;
        }

        public Builder processingTime(Duration processingTime) {
            this.processingTime = processingTime;
            return this;
        }

        public Builder confidenceScore(Integer confidenceScore) {
            this.confidenceScore = confidenceScore;
            return this;
        }

        public Builder responseChanged(boolean responseChanged) {
            this.responseChanged = responseChanged;
            return this;
        }

        public Builder metrics(Map<String, Object> metrics) {
            this.metrics = new HashMap<>(metrics);
            return this;
        }

        public Builder addMetric(String key, Object value) {
            this.metrics.put(key, value);
            return this;
        }

        public AgenticFlowResult build() {
            return new AgenticFlowResult(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AgenticFlowResult that = (AgenticFlowResult) o;
        return responseChanged == that.responseChanged &&
                originalPrompt.equals(that.originalPrompt) &&
                Objects.equals(enhancedPrompt, that.enhancedPrompt) &&
                fullResponse.equals(that.fullResponse) &&
                finalResponse.equals(that.finalResponse) &&
                Objects.equals(reasoning, that.reasoning) &&
                processingSteps.equals(that.processingSteps) &&
                processingTime.equals(that.processingTime) &&
                Objects.equals(confidenceScore, that.confidenceScore) &&
                metrics.equals(that.metrics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalPrompt, enhancedPrompt, fullResponse, finalResponse, reasoning,
                processingSteps, processingTime, confidenceScore, responseChanged, metrics);
    }

    @Override
    public String toString() {
        return "AgenticFlowResult{" +
                "originalPrompt='" + originalPrompt + '\'' +
                ", finalResponse='" + finalResponse + '\'' +
                ", processingSteps=" + processingSteps.size() +
                ", processingTime=" + processingTime +
                ", confidenceScore=" + confidenceScore +
                ", responseChanged=" + responseChanged +
                '}';
    }
}