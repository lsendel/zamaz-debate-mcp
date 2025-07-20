package com.zamaz.mcp.common.domain.agentic;

import com.zamaz.mcp.common.domain.organization.OrganizationId;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity representing an agentic flow configuration.
 * An agentic flow defines a reasoning or decision-making pattern for AI agents.
 */
public class AgenticFlow {
    private final AgenticFlowId id;
    private final AgenticFlowType type;
    private final AgenticFlowConfiguration configuration;
    private AgenticFlowStatus status;
    private final OrganizationId organizationId;
    private final Instant createdAt;
    private Instant updatedAt;

    /**
     * Creates a new AgenticFlow with the specified parameters.
     *
     * @param id             The unique identifier
     * @param type           The flow type
     * @param configuration  The flow configuration
     * @param status         The flow status
     * @param organizationId The organization ID
     * @param createdAt      The creation timestamp
     * @param updatedAt      The last update timestamp
     */
    public AgenticFlow(
            AgenticFlowId id,
            AgenticFlowType type,
            AgenticFlowConfiguration configuration,
            AgenticFlowStatus status,
            OrganizationId organizationId,
            Instant createdAt,
            Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "ID cannot be null");
        this.type = Objects.requireNonNull(type, "Type cannot be null");
        this.configuration = Objects.requireNonNull(configuration, "Configuration cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.organizationId = Objects.requireNonNull(organizationId, "Organization ID cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Created at cannot be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "Updated at cannot be null");
    }

    /**
     * Creates a new active AgenticFlow with the specified parameters and current
     * timestamps.
     *
     * @param type           The flow type
     * @param configuration  The flow configuration
     * @param organizationId The organization ID
     */
    public AgenticFlow(AgenticFlowType type, AgenticFlowConfiguration configuration, OrganizationId organizationId) {
        this(
                new AgenticFlowId(),
                type,
                configuration,
                AgenticFlowStatus.ACTIVE,
                organizationId,
                Instant.now(),
                Instant.now());
    }

    /**
     * Returns the unique identifier of this flow.
     *
     * @return The ID
     */
    public AgenticFlowId getId() {
        return id;
    }

    /**
     * Returns the type of this flow.
     *
     * @return The type
     */
    public AgenticFlowType getType() {
        return type;
    }

    /**
     * Returns the configuration of this flow.
     *
     * @return The configuration
     */
    public AgenticFlowConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Returns the status of this flow.
     *
     * @return The status
     */
    public AgenticFlowStatus getStatus() {
        return status;
    }

    /**
     * Sets the status of this flow.
     *
     * @param status The new status
     */
    public void setStatus(AgenticFlowStatus status) {
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.updatedAt = Instant.now();
    }

    /**
     * Returns the organization ID of this flow.
     *
     * @return The organization ID
     */
    public OrganizationId getOrganizationId() {
        return organizationId;
    }

    /**
     * Returns the creation timestamp of this flow.
     *
     * @return The creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the last update timestamp of this flow.
     *
     * @return The last update timestamp
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Activates this flow.
     */
    public void activate() {
        setStatus(AgenticFlowStatus.ACTIVE);
    }

    /**
     * Deactivates this flow.
     */
    public void deactivate() {
        setStatus(AgenticFlowStatus.INACTIVE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AgenticFlow that = (AgenticFlow) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AgenticFlow{" +
                "id=" + id +
                ", type=" + type +
                ", status=" + status +
                ", organizationId=" + organizationId +
                '}';
    }
}