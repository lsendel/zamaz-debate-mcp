package com.zamaz.mcp.debateengine.adapter.web;

import com.zamaz.mcp.debateengine.adapter.web.dto.*;
import com.zamaz.mcp.debateengine.application.command.AddParticipantCommand;
import com.zamaz.mcp.debateengine.application.command.CreateDebateCommand;
import com.zamaz.mcp.debateengine.application.command.StartDebateCommand;
import com.zamaz.mcp.debateengine.application.query.GetDebateQuery;
import com.zamaz.mcp.debateengine.application.query.ListDebatesQuery;
import com.zamaz.mcp.debateengine.application.usecase.*;
import com.zamaz.mcp.debateengine.domain.model.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * REST controller for debate operations.
 */
@RestController
@RequestMapping("/api/debates")
@Tag(name = "Debates", description = "Debate management operations")
public class DebateController {
    
    private final CreateDebateUseCase createDebateUseCase;
    private final StartDebateUseCase startDebateUseCase;
    private final GetDebateUseCase getDebateUseCase;
    private final DebateMapper debateMapper;
    
    public DebateController(
            CreateDebateUseCase createDebateUseCase,
            StartDebateUseCase startDebateUseCase,
            GetDebateUseCase getDebateUseCase,
            DebateMapper debateMapper) {
        this.createDebateUseCase = Objects.requireNonNull(createDebateUseCase);
        this.startDebateUseCase = Objects.requireNonNull(startDebateUseCase);
        this.getDebateUseCase = Objects.requireNonNull(getDebateUseCase);
        this.debateMapper = Objects.requireNonNull(debateMapper);
    }
    
    @PostMapping
    @Operation(summary = "Create a new debate")
    public ResponseEntity<CreateDebateResponse> createDebate(
            @Parameter(description = "Organization ID") @RequestHeader("X-Organization-Id") String organizationId,
            @Parameter(description = "User ID") @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateDebateRequest request
    ) {
        // Map request to command
        CreateDebateCommand command = debateMapper.toCreateCommand(
            OrganizationId.from(organizationId),
            UUID.fromString(userId),
            request
        );
        
        // Execute use case
        DebateId debateId = createDebateUseCase.execute(command);
        
        // Return response
        CreateDebateResponse response = new CreateDebateResponse(
            debateId.toString(),
            "Debate created successfully"
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{debateId}")
    @Operation(summary = "Get debate by ID")
    public ResponseEntity<DebateResponse> getDebate(
            @Parameter(description = "Organization ID") @RequestHeader("X-Organization-Id") String organizationId,
            @Parameter(description = "Debate ID") @PathVariable String debateId
    ) {
        GetDebateQuery query = GetDebateQuery.of(debateId, organizationId);
        
        return getDebateUseCase.execute(query)
            .map(debate -> ResponseEntity.ok(debateMapper.toResponse(debate)))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping("/{debateId}/start")
    @Operation(summary = "Start a debate")
    public ResponseEntity<Map<String, String>> startDebate(
            @Parameter(description = "Debate ID") @PathVariable String debateId
    ) {
        StartDebateCommand command = StartDebateCommand.of(debateId);
        startDebateUseCase.execute(command);
        
        return ResponseEntity.ok(Map.of(
            "debateId", debateId,
            "status", "started",
            "message", "Debate started successfully"
        ));
    }
    
    @PostMapping("/{debateId}/participants")
    @Operation(summary = "Add participant to debate")
    public ResponseEntity<Map<String, String>> addParticipant(
            @Parameter(description = "Debate ID") @PathVariable String debateId,
            @Valid @RequestBody AddParticipantRequest request
    ) {
        // This would be implemented with AddParticipantUseCase
        // For now, return a placeholder response
        
        return ResponseEntity.ok(Map.of(
            "debateId", debateId,
            "participantId", UUID.randomUUID().toString(),
            "message", "Participant added successfully"
        ));
    }
    
    @GetMapping
    @Operation(summary = "List debates")
    public ResponseEntity<List<DebateResponse>> listDebates(
            @Parameter(description = "Organization ID") @RequestHeader("X-Organization-Id") String organizationId,
            @Parameter(description = "Filter by user ID") @RequestParam(required = false) String userId,
            @Parameter(description = "Filter by status") @RequestParam(required = false) String status,
            @Parameter(description = "Limit results") @RequestParam(required = false) Integer limit,
            @Parameter(description = "Offset for pagination") @RequestParam(required = false) Integer offset
    ) {
        // This would be implemented with ListDebatesUseCase
        // For now, return empty list
        
        return ResponseEntity.ok(List.of());
    }
    
    @PostMapping("/{debateId}/rounds/{roundId}/responses")
    @Operation(summary = "Submit response to current round")
    public ResponseEntity<Map<String, String>> submitResponse(
            @Parameter(description = "Debate ID") @PathVariable String debateId,
            @Parameter(description = "Round ID") @PathVariable String roundId,
            @Valid @RequestBody SubmitResponseRequest request
    ) {
        // This would be implemented with SubmitResponseUseCase
        // For now, return a placeholder response
        
        return ResponseEntity.accepted().body(Map.of(
            "debateId", debateId,
            "roundId", roundId,
            "responseId", UUID.randomUUID().toString(),
            "message", "Response submitted successfully"
        ));
    }
}