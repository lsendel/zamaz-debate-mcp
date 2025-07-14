package com.zamaz.mcp.controller.entity;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "participants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Participant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debate_id", nullable = false)
    private Debate debate;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false, length = 50)
    private String type; // 'human' or 'ai'
    
    @Column(length = 50)
    private String provider; // for AI participants
    
    @Column(length = 100)
    private String model; // for AI participants
    
    @Column(length = 50)
    private String position; // 'for' or 'against'
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode settings;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "participant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Response> responses = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}