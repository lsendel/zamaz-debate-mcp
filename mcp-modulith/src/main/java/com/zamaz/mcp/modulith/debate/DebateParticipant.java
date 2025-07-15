package com.zamaz.mcp.modulith.debate;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Represents a participant in a debate.
 */
@Entity
@Table(name = "debate_participants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebateParticipant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debate_id", nullable = false)
    private Debate debate;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String llmProvider;
    
    private String llmModel;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantRole role;
    
    private String systemPrompt;
    
    private Integer turnOrder;
    
    public enum ParticipantRole {
        DEBATER,
        MODERATOR,
        JUDGE,
        OBSERVER
    }
}