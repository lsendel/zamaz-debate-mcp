package com.zamaz.mcp.common.domain.agentic.event;

import com.zamaz.mcp.common.domain.agentic.AgenticFlowId;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowResult;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Event emitted when an agentic flow is executed.
 */
public class AgenticFlowExecutionEvent extends AgenticFlowEvent {
    private final String debateId;
    private final String participantId;
    private final Duration processingTime;
    private final Integer confidenceScore;
    private final boolean flowChangedResponse;
    private final Map<String, Object> metrics;

    private AgenticFlowExecutionEvent(Builder builder) {
        super(builder.eventId, builder.flowId, builder.flowType, builder.timestamp);
        this.debateId = builder.debateId;
        this.participantId = builder.participantId;
        this.processingTime = Objects.requireNonNull(builder.processingTime, "Processing time cannot be null");
        this.confidenceScore = builder.confidenceScore;
        this.flowChangedResponse = builder.flowChangedResponse;
        this.metrics = Collections.unmodifiableMap(new HashMap<>(builder.metrics));
    }

    /**
     * Returns the debate ID.
     *
     * @return The debate ID
     */
    public String getDebateId() {
        return debateId;
    }

    /**
     * Returns the participant ID.
     *
     * @return The participant ID
     */
    public String getParticipantId() {
        return participantId;
    }

    /**
     * Returns the processing time.
     *
     * @return The processing time
     */
    public Duration getProcessingTime() {
        return processingTime;
    }

    /**
     * Returns the confidence score.
     *
     * @return The confidence score, or null if not applicable
     */
    public Integer getConfidenceScore() {
        return confidenceScore;
    }

    /**
     * Returns whether the flow changed the response.
     *
     * @return True if the flow changed the response, false otherwise
     */
    public boolean isFlowChangedResponse() {
        return flowChangedResponse;
    }

    /**
     * Returns the metrics collected during execution.
     *
     * @return An unmodifiable map of metrics
     */
    public Map<String, Object> getMetrics() {
        return metrics;
    }

    /**
     * Returns a new builder for creating AgenticFlowExecutionEvent instances.
     *
     * @return A new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for creating AgenticFlowExecutionEvent instances.
     */
    public static class Builder {
        private String eventId;
        private AgenticFlowId flowId;
        private AgenticFlowType flowType;
        private String debateId;
        private String participantId;
        private Duration processingTime;
        private Integer confidenceScore;
        private boolean flowChangedResponse;
        private Map<String, Object> metrics = new HashMap<>();
        private Instant timestamp;

        private Builder() {
            this.eventId = java.util.UUID.randomUUID().toString();
            this.timestamp = Instant.now();
        }

        public Builder eventId(String eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder flowId(AgenticFlowId flowId) {
            this.flowId = flowId;
            return this;
        }

        public Builder flowType(AgenticFlowType flowType) {
            this.flowType = flowType;
            return this;
        }

        public Builder debateId(String debateId) {
            this.debateId = debateId;
            return this;
        }

        public Builder participantId(String participantId) {
            this.participantId = participantId;
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

        public Builder flowChangedResponse(boolean flowChangedResponse) {
            this.flowChangedResponse = flowChangedResponse;
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

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder fromResult(AgenticFlowResult result, AgenticFlowId flowId, AgenticFlowType flowType,
                String debateId, String participantId) {
            this.flowId = flowId;
            this.flowType = flowType;
            this.debateId = debateId;
            this.participantId = participantId;
            this.processingTime = result.getProcessingTime();
            this.confidenceScore = result.getConfidenceScore();
            this.flowChangedResponse = result.isResponseChanged();
            this.metrics = new HashMap<>(result.getMetrics());
            return this;
        }

        public AgenticFlowExecutionEvent build() {
            return new AgenticFlowExecutionEvent(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        AgenticFlowExecutionEvent that = (AgenticFlowExecutionEvent) o;
        return flowChangedResponse == that.flowChangedResponse &&
                Objects.equals(debateId, that.debateId) &&
                Objects.equals(participantId, that.participantId) &&
                processingTime.equals(that.processingTime) &&
                Objects.equals(confidenceScore, that.confidenceScore) &&
                metrics.equals(that.metrics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), debateId, participantId, processingTime,
                confidenceScore, flowChangedResponse, metrics);
    }

    @Override
    public String toString() {
        return "AgenticFlowExecutionEvent{" +
                "eventId='" + getEventId() + '\'' +
                ", flowId=" + getFlowId() +
                ", flowType=" + getFlowType() +
                ", debateId='" + debateId + '\'' +
                ", participantId='" + participantId + '\'' +
                ", processingTime=" + processingTime +
                ", confidenceScore=" + confidenceScore +
                ", flowChangedResponse=" + flowChangedResponse +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}