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
 * JPA entity for agentic flow execution history.
 */
@Entity
@Table(name = "agentic_flow_executions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgenticFlowExecutionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flow_id", nullable = false)
    private AgenticFlowEntity flow;

    @Column(name = "debate_id")
    private UUID debateId;

    @Column(name = "participant_id")
    private UUID participantId;

    @Column(name = "prompt", columnDefinition = "TEXT", nullable = false)
    private String prompt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> result;

    @Column(name = "processing_time_ms", nullable = false)
    private Long processingTimeMs;

    @Column(name = "response_changed", nullable = false)
    private Boolean responseChanged;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (responseChanged == null) {
            responseChanged = false;
        }
    }
}