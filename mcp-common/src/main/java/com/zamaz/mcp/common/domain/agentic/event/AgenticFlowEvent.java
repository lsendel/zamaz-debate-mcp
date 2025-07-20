package com.zamaz.mcp.common.domain.agentic.event;

import com.zamaz.mcp.common.domain.agentic.AgenticFlowId;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Base class for all agentic flow events.
 */
public abstract class AgenticFlowEvent {
    private final String eventId;
    private final AgenticFlowId flowId;
    private final AgenticFlowType flowType;
    private final Instant timestamp;

    /**
     * Creates a new AgenticFlowEvent with the specified parameters.
     *
     * @param eventId   The event ID
     * @param flowId    The flow ID
     * @param flowType  The flow type
     * @param timestamp The event timestamp
     */
    protected AgenticFlowEvent(String eventId, AgenticFlowId flowId, AgenticFlowType flowType, Instant timestamp) {
        this.eventId = Objects.requireNonNull(eventId, "Event ID cannot be null");
        this.flowId = Objects.requireNonNull(flowId, "Flow ID cannot be null");
        this.flowType = Objects.requireNonNull(flowType, "Flow type cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
    }

    /**
     * Creates a new AgenticFlowEvent with the specified parameters and a random
     * event ID and current timestamp.
     *
     * @param flowId   The flow ID
     * @param flowType The flow type
     */
    protected AgenticFlowEvent(AgenticFlowId flowId, AgenticFlowType flowType) {
        this(UUID.randomUUID().toString(), flowId, flowType, Instant.now());
    }

    /**
     * Returns the event ID.
     *
     * @return The event ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Returns the flow ID.
     *
     * @return The flow ID
     */
    public AgenticFlowId getFlowId() {
        return flowId;
    }

    /**
     * Returns the flow type.
     *
     * @return The flow type
     */
    public AgenticFlowType getFlowType() {
        return flowType;
    }

    /**
     * Returns the event timestamp.
     *
     * @return The timestamp
     */
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AgenticFlowEvent that = (AgenticFlowEvent) o;
        return eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "eventId='" + eventId + '\'' +
                ", flowId=" + flowId +
                ", flowType=" + flowType +
                ", timestamp=" + timestamp +
                '}';
    }
}