package com.zamaz.mcp.common.infrastructure.persistence.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for agentic flow persistence.
 * This is part of the persistence adapter layer.
 */
@Entity
@Table(name = "agentic_flows")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgenticFlowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private AgenticFlowTypeEntity type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration", columnDefinition = "jsonb", nullable = false)
    private JsonNode configuration;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AgenticFlowStatusEntity status;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Enumeration of agentic flow types for JPA persistence.
     */
    public enum AgenticFlowTypeEntity {
        INTERNAL_MONOLOGUE,
        SELF_CRITIQUE_LOOP,
        MULTI_AGENT_RED_TEAM,
        TOOL_CALLING_VERIFICATION,
        RAG_WITH_RERANKING,
        CONFIDENCE_SCORING,
        CONSTITUTIONAL_PROMPTING,
        ENSEMBLE_VOTING,
        POST_PROCESSING_RULES,
        TREE_OF_THOUGHTS,
        STEP_BACK_PROMPTING,
        PROMPT_CHAINING
    }

    /**
     * Enumeration of agentic flow statuses for JPA persistence.
     */
    public enum AgenticFlowStatusEntity {
        ACTIVE,
        INACTIVE,
        DRAFT
    }
}