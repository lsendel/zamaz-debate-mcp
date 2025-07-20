package com.zamaz.mcp.common.domain.agentic;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing an agentic flow execution for analytics.
 */
public class AgenticFlowExecution {
    private final UUID id;
    private final AgenticFlowId flowId;
    private final UUID debateId;
    private final UUID participantId;
    private final String prompt;
    private final Map<String, Object> result;
    private final long processingTimeMs;
    private final boolean responseChanged;
    private final String errorMessage;
    private final Instant createdAt;

    /**
     * Creates a new AgenticFlowExecution.
     *
     * @param id               The unique identifier
     * @param flowId           The flow ID
     * @param debateId         The debate ID (optional)
     * @param participantId    The participant ID (optional)
     * @param prompt           The input prompt
     * @param result           The execution result
     * @param processingTimeMs The processing time in milliseconds
     * @param responseChanged  Whether the flow changed the response
     * @param errorMessage     Error message if execution failed (optional)
     * @param createdAt        The creation timestamp
     */
    public AgenticFlowExecution(
            UUID id,
            AgenticFlowId flowId,
            UUID debateId,
            UUID participantId,
            String prompt,
            Map<String, Object> result,
            long processingTimeMs,
            boolean responseChanged,
            String errorMessage,
            Instant createdAt) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.flowId = Objects.requireNonNull(flowId, "Flow ID cannot be null");
        this.debateId = debateId; // Optional
        this.participantId = participantId; // Optional
        this.prompt = Objects.requireNonNull(prompt, "Prompt cannot be null");
        this.result = Objects.requireNonNull(result, "Result cannot be null");
        this.processingTimeMs = processingTimeMs;
        this.responseChanged = responseChanged;
        this.errorMessage = errorMessage; // Optional
        this.createdAt = Objects.requireNonNull(createdAt, "Created at cannot be null");
    }

    /**
     * Creates a new successful execution.
     */
    public static AgenticFlowExecution successful(
            AgenticFlowId flowId,
            UUID debateId,
            UUID participantId,
            String prompt,
            Map<String, Object> result,
            long processingTimeMs,
            boolean responseChanged) {
        return new AgenticFlowExecution(
                UUID.randomUUID(),
                flowId,
                debateId,
                participantId,
                prompt,
                result,
                processingTimeMs,
                responseChanged,
                null,
                Instant.now());
    }

    /**
     * Creates a new failed execution.
     */
    public static AgenticFlowExecution failed(
            AgenticFlowId flowId,
            UUID debateId,
            UUID participantId,
            String prompt,
            String errorMessage,
            long processingTimeMs) {
        return new AgenticFlowExecution(
                UUID.randomUUID(),
                flowId,
                debateId,
                participantId,
                prompt,
                Map.of("error", true),
                processingTimeMs,
                false,
                errorMessage,
                Instant.now());
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public AgenticFlowId getFlowId() {
        return flowId;
    }

    public UUID getDebateId() {
        return debateId;
    }

    public UUID getParticipantId() {
        return participantId;
    }

    public String getPrompt() {
        return prompt;
    }

    public Map<String, Object> getResult() {
        return result;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public boolean isResponseChanged() {
        return responseChanged;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean hasError() {
        return errorMessage != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AgenticFlowExecution that = (AgenticFlowExecution) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AgenticFlowExecution{" +
                "id=" + id +
                ", flowId=" + flowId +
                ", debateId=" + debateId +
                ", participantId=" + participantId +
                ", processingTimeMs=" + processingTimeMs +
                ", responseChanged=" + responseChanged +
                ", hasError=" + hasError() +
                ", createdAt=" + createdAt +
                '}';
    }
}