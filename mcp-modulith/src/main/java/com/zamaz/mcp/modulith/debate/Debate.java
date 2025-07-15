package com.zamaz.mcp.modulith.debate;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Debate entity representing a debate between LLM participants.
 */
@Entity
@Table(name = "debates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Debate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID organizationId;
    
    @Column(nullable = false)
    private String topic;
    
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DebateStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DebateFormat format;
    
    @OneToMany(mappedBy = "debate", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<DebateParticipant> participants = new ArrayList<>();
    
    @OneToMany(mappedBy = "debate", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<DebateTurn> turns = new ArrayList<>();
    
    private Integer maxRounds;
    private Integer maxTurnLength;
    private Integer currentRound;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = DebateStatus.DRAFT;
        }
        if (currentRound == null) {
            currentRound = 0;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum DebateStatus {
        DRAFT,
        READY,
        IN_PROGRESS,
        PAUSED,
        COMPLETED,
        CANCELLED
    }
    
    public enum DebateFormat {
        TURN_BASED,
        FREE_FORM,
        MODERATED,
        PANEL_DISCUSSION
    }
}