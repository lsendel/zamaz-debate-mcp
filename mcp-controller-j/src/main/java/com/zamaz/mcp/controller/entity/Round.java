package com.zamaz.mcp.controller.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rounds", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"debate_id", "round_number"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Round {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debate_id", nullable = false)
    private Debate debate;
    
    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;
    
    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING";
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @OneToMany(mappedBy = "round", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Response> responses = new ArrayList<>();
}