package com.zamaz.mcp.modulith.api;

import com.zamaz.mcp.modulith.debate.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for debate management.
 */
@RestController
@RequestMapping("/api/debates")
@RequiredArgsConstructor
public class DebateController {
    
    private final DebateService debateService;
    
    @PostMapping
    public ResponseEntity<Debate> createDebate(@RequestBody CreateDebateRequest request) {
        Debate debate = debateService.createDebate(
            request.organizationId(),
            request.topic(),
            request.description()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(debate);
    }
    
    @GetMapping
    public List<Debate> listDebates(@RequestParam UUID organizationId) {
        return debateService.findByOrganization(organizationId);
    }
    
    @PostMapping("/{debateId}/participants")
    public ResponseEntity<DebateParticipant> addParticipant(
            @PathVariable UUID debateId,
            @RequestBody AddParticipantRequest request) {
        DebateParticipant participant = debateService.addParticipant(
            debateId,
            request.name(),
            request.llmProvider(),
            request.role()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(participant);
    }
    
    @PostMapping("/{debateId}/start")
    public ResponseEntity<Debate> startDebate(@PathVariable UUID debateId) {
        Debate debate = debateService.startDebate(debateId);
        return ResponseEntity.ok(debate);
    }
    
    @PostMapping("/{debateId}/next-turn")
    public ResponseEntity<DebateTurn> processNextTurn(@PathVariable UUID debateId) {
        DebateTurn turn = debateService.processNextTurn(debateId);
        return ResponseEntity.ok(turn);
    }
    
    public record CreateDebateRequest(
        UUID organizationId,
        String topic,
        String description
    ) {}
    
    public record AddParticipantRequest(
        String name,
        String llmProvider,
        DebateParticipant.ParticipantRole role
    ) {}
}