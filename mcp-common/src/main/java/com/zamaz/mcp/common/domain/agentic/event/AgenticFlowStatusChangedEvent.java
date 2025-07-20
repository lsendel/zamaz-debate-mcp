package com.zamaz.mcp.common.domain.agentic.event;

import com.zamaz.mcp.common.domain.agentic.AgenticFlow;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowId;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowStatus;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;

import java.time.Instant;
import java.util.Objects;

/**
 * Event emitted when an agentic flow's status changes.
 */
public class AgenticFlowStatusChangedEvent extends AgenticFlowEvent {
    private final AgenticFlowStatus previousStatus;
    private final AgenticFlowStatus newStatus;

    /**
     * Creates a new AgenticFlowStatusChangedEvent with the specified parameters.
     *
     * @param eventId        The event ID
     * @param flowId         The flow ID
     * @param flowType       The flow type
     * @param previousStatus The previous status
     * @param newStatus      The new status
     * @param timestamp      The event timestamp
     */
    public AgenticFlowStatusChangedEvent(String eventId, AgenticFlowId flowId, AgenticFlowType flowType,
            AgenticFlowStatus previousStatus, AgenticFlowStatus newStatus,
            Instant timestamp) {
        super(eventId, flowId, flowType, timestamp);
        this.previousStatus = Objects.requireNonNull(previousStatus, "Previous status cannot be null");
        this.newStatus = Objects.requireNonNull(newStatus, "New status cannot be null");
    }

    /**
     * Creates a new AgenticFlowStatusChangedEvent from the specified flow and
     * previous status.
     *
     * @param flow           The flow whose status changed
     * @param previousStatus The previous status
     */
    public AgenticFlowStatusChangedEvent(AgenticFlow flow, AgenticFlowStatus previousStatus) {
        super(flow.getId(), flow.getType());
        this.previousStatus = Objects.requireNonNull(previousStatus, "Previous status cannot be null");
        this.newStatus = flow.getStatus();
    }

    /**
     * Returns the previous status.
     *
     * @return The previous status
     */
    public AgenticFlowStatus getPreviousStatus() {
        return previousStatus;
    }

    /**
     * Returns the new status.
     *
     * @return The new status
     */
    public AgenticFlowStatus getNewStatus() {
        return newStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        AgenticFlowStatusChangedEvent that = (AgenticFlowStatusChangedEvent) o;
        return previousStatus == that.previousStatus && newStatus == that.newStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), previousStatus, newStatus);
    }

    @Override
    public String toString() {
        return "AgenticFlowStatusChangedEvent{" +
                "eventId='" + getEventId() + '\'' +
                ", flowId=" + getFlowId() +
                ", flowType=" + getFlowType() +
                ", previousStatus=" + previousStatus +
                ", newStatus=" + newStatus +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}