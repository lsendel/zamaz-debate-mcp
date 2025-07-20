package com.zamaz.mcp.common.domain.agentic.event;

import com.zamaz.mcp.common.domain.agentic.AgenticFlow;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowId;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import com.zamaz.mcp.common.domain.organization.OrganizationId;

import java.time.Instant;
import java.util.Objects;

/**
 * Event emitted when an agentic flow is deleted.
 */
public class AgenticFlowDeletedEvent extends AgenticFlowEvent {
    private final OrganizationId organizationId;

    /**
     * Creates a new AgenticFlowDeletedEvent with the specified parameters.
     *
     * @param eventId        The event ID
     * @param flowId         The flow ID
     * @param flowType       The flow type
     * @param organizationId The organization ID
     * @param timestamp      The event timestamp
     */
    public AgenticFlowDeletedEvent(String eventId, AgenticFlowId flowId, AgenticFlowType flowType,
            OrganizationId organizationId, Instant timestamp) {
        super(eventId, flowId, flowType, timestamp);
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
    }

    /**
     * Creates a new AgenticFlowDeletedEvent from the specified flow.
     *
     * @param flow The flow that was deleted
     */
    public AgenticFlowDeletedEvent(AgenticFlow flow) {
        super(flow.getId(), flow.getType());
        this.organizationId = flow.getOrganizationId();
    }

    /**
     * Returns the organization ID.
     *
     * @return The organization ID
     */
    public OrganizationId getOrganizationId() {
        return organizationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        AgenticFlowDeletedEvent that = (AgenticFlowDeletedEvent) o;
        return organizationId.equals(that.organizationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), organizationId);
    }

    @Override
    public String toString() {
        return "AgenticFlowDeletedEvent{" +
                "eventId='" + getEventId() + '\'' +
                ", flowId=" + getFlowId() +
                ", flowType=" + getFlowType() +
                ", organizationId=" + organizationId +
                ", timestamp=" + getTimestamp() +
                '}';
    }
}