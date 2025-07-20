package com.zamaz.mcp.common.domain.agentic;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Value object representing the context for a prompt to be processed by an
 * agentic flow.
 * Contains information about the debate, participant, and other contextual
 * data.
 */
public class PromptContext {
    private final String debateId;
    private final String participantId;
    private final Map<String, Object> contextData;

    /**
     * Creates a new PromptContext with the specified parameters.
     *
     * @param debateId      The debate ID
     * @param participantId The participant ID
     * @param contextData   Additional context data
     */
    public PromptContext(String debateId, String participantId, Map<String, Object> contextData) {
        this.debateId = debateId;
        this.participantId = participantId;
        this.contextData = new HashMap<>(Objects.requireNonNull(contextData, "Context data cannot be null"));
    }

    /**
     * Creates a new PromptContext with the specified debate and participant IDs.
     *
     * @param debateId      The debate ID
     * @param participantId The participant ID
     */
    public PromptContext(String debateId, String participantId) {
        this(debateId, participantId, new HashMap<>());
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
     * Returns the context data.
     *
     * @return An unmodifiable view of the context data
     */
    public Map<String, Object> getContextData() {
        return Collections.unmodifiableMap(contextData);
    }

    /**
     * Returns the value of the specified context data key.
     *
     * @param key The context data key
     * @return An Optional containing the value, or empty if not found
     */
    public Optional<Object> getContextValue(String key) {
        return Optional.ofNullable(contextData.get(key));
    }

    /**
     * Creates a new PromptContext with the specified context data added.
     *
     * @param key   The context data key
     * @param value The context data value
     * @return A new PromptContext with the context data added
     */
    public PromptContext withContextData(String key, Object value) {
        Map<String, Object> newContextData = new HashMap<>(this.contextData);
        newContextData.put(key, value);
        return new PromptContext(this.debateId, this.participantId, newContextData);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PromptContext that = (PromptContext) o;
        return Objects.equals(debateId, that.debateId) &&
                Objects.equals(participantId, that.participantId) &&
                contextData.equals(that.contextData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(debateId, participantId, contextData);
    }

    @Override
    public String toString() {
        return "PromptContext{" +
                "debateId='" + debateId + '\'' +
                ", participantId='" + participantId + '\'' +
                ", contextData=" + contextData +
                '}';
    }
}