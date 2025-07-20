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
    private String name;
    private String description;
    private final AgenticFlowConfiguration configuration;
    private AgenticFlowStatus status;
    private final OrganizationId organizationId;
    private final Instant createdAt;
    private Instant updatedAt;
    private Long version;

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

    /**
     * Returns the name of this flow.
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this flow.
     *
     * @param name The new name
     */
    public void setName(String name) {
        this.name = name;
        this.updatedAt = Instant.now();
    }

    /**
     * Returns the description of this flow.
     *
     * @return The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of this flow.
     *
     * @param description The new description
     */
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now();
    }

    /**
     * Returns the version of this flow.
     *
     * @return The version
     */
    public Long getVersion() {
        return version;
    }

    /**
     * Sets the version of this flow.
     *
     * @param version The new version
     */
    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "AgenticFlow{" +
                "id=" + id +
                ", type=" + type +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", organizationId=" + organizationId +
                '}';
    }

    /**
     * Builder for creating AgenticFlow instances.
     */
    public static AgenticFlowBuilder builder() {
        return new AgenticFlowBuilder();
    }

    public static class AgenticFlowBuilder {
        private AgenticFlowId id;
        private AgenticFlowType type;
        private String name;
        private String description;
        private AgenticFlowConfiguration configuration;
        private AgenticFlowStatus status;
        private OrganizationId organizationId;
        private Instant createdAt;
        private Instant updatedAt;
        private Long version;

        AgenticFlowBuilder() {
        }

        public AgenticFlowBuilder id(AgenticFlowId id) {
            this.id = id;
            return this;
        }

        public AgenticFlowBuilder type(AgenticFlowType type) {
            this.type = type;
            return this;
        }

        public AgenticFlowBuilder name(String name) {
            this.name = name;
            return this;
        }

        public AgenticFlowBuilder description(String description) {
            this.description = description;
            return this;
        }

        public AgenticFlowBuilder configuration(AgenticFlowConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        public AgenticFlowBuilder status(AgenticFlowStatus status) {
            this.status = status;
            return this;
        }

        public AgenticFlowBuilder organizationId(OrganizationId organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public AgenticFlowBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AgenticFlowBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public AgenticFlowBuilder version(Long version) {
            this.version = version;
            return this;
        }

        public AgenticFlow build() {
            AgenticFlow flow = new AgenticFlow(
                    id != null ? id : new AgenticFlowId(),
                    type,
                    configuration,
                    status != null ? status : AgenticFlowStatus.ACTIVE,
                    organizationId,
                    createdAt != null ? createdAt : Instant.now(),
                    updatedAt != null ? updatedAt : Instant.now()
            );
            flow.setName(name);
            flow.setDescription(description);
            flow.setVersion(version != null ? version : 0L);
            return flow;
        }
    }
}