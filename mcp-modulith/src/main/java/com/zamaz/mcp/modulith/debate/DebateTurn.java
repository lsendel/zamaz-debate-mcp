package com.zamaz.mcp.modulith.debate;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a single turn/message in a debate.
 */
@Entity
@Table(name = "debate_turns")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebateTurn {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debate_id", nullable = false)
    private Debate debate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private DebateParticipant participant;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
    
    private Integer roundNumber;
    private Integer turnNumber;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private Integer promptTokens;
    private Integer completionTokens;
    private Long responseTimeMs;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}