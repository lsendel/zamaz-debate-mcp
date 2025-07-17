package com.zamaz.mcp.controller.controller;

import com.zamaz.mcp.controller.dto.DebateDto;
import com.zamaz.mcp.controller.dto.ParticipantDto;
import com.zamaz.mcp.controller.dto.ResponseDto;
import com.zamaz.mcp.controller.service.DebateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/debates")
@RequiredArgsConstructor
@Tag(name = "Debates", description = "Debate management endpoints")
public class DebateController {
    
    private final DebateService debateService;
    
    @PostMapping
    @Operation(summary = "Create a new debate")
    public ResponseEntity<DebateDto> createDebate(@Valid @RequestBody DebateDto.CreateDebateRequest request) {
        DebateDto debate = debateService.createDebate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(debate);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get debate by ID")
    public ResponseEntity<DebateDto> getDebate(@PathVariable UUID id) {
        DebateDto debate = debateService.getDebate(id);
        return ResponseEntity.ok(debate);
    }
    
    @GetMapping
    @Operation(summary = "List debates")
    public ResponseEntity<Page<DebateDto>> listDebates(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        Page<DebateDto> debates = debateService.listDebates(organizationId, status, pageable);
        return ResponseEntity.ok(debates);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update debate")
    public ResponseEntity<DebateDto> updateDebate(
            @PathVariable UUID id,
            @Valid @RequestBody DebateDto.UpdateDebateRequest request) {
        DebateDto debate = debateService.updateDebate(id, request);
        return ResponseEntity.ok(debate);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete debate")
    public ResponseEntity<Void> deleteDebate(@PathVariable UUID id) {
        debateService.deleteDebate(id);
        return ResponseEntity.noContent().build();
    }
    
    
    
    @PostMapping("/{id}/participants")
    @Operation(summary = "Add participant to debate")
    public ResponseEntity<ParticipantDto> addParticipant(
            @PathVariable UUID id,
            @Valid @RequestBody ParticipantDto.CreateParticipantRequest request) {
        ParticipantDto participant = debateService.addParticipant(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(participant);
    }
    
    @DeleteMapping("/{id}/participants/{participantId}")
    @Operation(summary = "Remove participant from debate")
    public ResponseEntity<Void> removeParticipant(
            @PathVariable UUID id,
            @PathVariable UUID participantId) {
        debateService.removeParticipant(id, participantId);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/rounds/{roundId}/responses")
    @Operation(summary = "Submit response to round")
    public ResponseEntity<ResponseDto> submitResponse(
            @PathVariable UUID id,
            @PathVariable UUID roundId,
            @Valid @RequestBody ResponseDto.CreateResponseRequest request) {
        ResponseDto response = debateService.submitResponse(id, roundId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}/rounds")
    @Operation(summary = "List debate rounds")
    public ResponseEntity<List<RoundDto>> listRounds(@PathVariable UUID id) {
        return ResponseEntity.ok(debateService.listRounds(id));
    }
    
    @GetMapping("/{id}/results")
    @Operation(summary = "Get debate results")
    public ResponseEntity<DebateResultDto> getResults(@PathVariable UUID id) {
        return ResponseEntity.ok(debateService.getResults(id));
    }
}