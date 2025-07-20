package com.zamaz.mcp.controller.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * JPA entity for agentic flow persistence.
 */
@Entity
@Table(name = "agentic_flows",
        indexes = {
                @Index(name = "idx_agentic_flow_org_id", columnList = "organization_id"),
                @Index(name = "idx_agentic_flow_type", columnList = "flow_type"),
                @Index(name = "idx_agentic_flow_status", columnList = "status"),
                @Index(name = "idx_agentic_flow_org_type", columnList = "organization_id, flow_type"),
                @Index(name = "idx_agentic_flow_org_status", columnList = "organization_id, status")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgenticFlowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "flow_type", nullable = false, length = 50)
    private String flowType;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> configuration;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) {
            status = "CREATED";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}