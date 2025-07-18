package com.zamaz.mcp.controller.adapter.web;

import com.zamaz.mcp.controller.adapter.web.dto.CreateDebateRequest;
import com.zamaz.mcp.controller.adapter.web.dto.DebateResponse;
import com.zamaz.mcp.controller.adapter.web.dto.JoinDebateRequest;
import com.zamaz.mcp.controller.adapter.web.dto.StartDebateRequest;
import com.zamaz.mcp.controller.adapter.web.dto.SubmitResponseRequest;
import com.zamaz.mcp.controller.application.command.CreateDebateCommand;
import com.zamaz.mcp.controller.application.command.JoinDebateCommand;
import com.zamaz.mcp.controller.application.command.StartDebateCommand;
import com.zamaz.mcp.controller.application.command.SubmitResponseCommand;
import com.zamaz.mcp.controller.application.query.GetDebateQuery;
import com.zamaz.mcp.controller.application.query.ListDebatesQuery;
import com.zamaz.mcp.controller.application.usecase.CreateDebateUseCase;
import com.zamaz.mcp.controller.application.usecase.GetDebateUseCase;
import com.zamaz.mcp.controller.application.usecase.JoinDebateUseCase;
import com.zamaz.mcp.controller.application.usecase.ListDebatesUseCase;
import com.zamaz.mcp.controller.application.usecase.StartDebateUseCase;
import com.zamaz.mcp.controller.application.usecase.SubmitResponseUseCase;
import com.zamaz.mcp.controller.domain.model.Debate;
import com.zamaz.mcp.controller.domain.model.DebateId;
import com.zamaz.mcp.controller.domain.model.DebateStatus;
import com.zamaz.mcp.controller.domain.model.ParticipantId;
import com.zamaz.mcp.controller.domain.model.ResponseId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * REST controller for debate operations.
 */
@RestController
@RequestMapping("/api/debates")
@Tag(name = "Debates", description = "Debate management operations")
public class DebateController {
    
    private final CreateDebateUseCase createDebateUseCase;
    private final JoinDebateUseCase joinDebateUseCase;
    private final StartDebateUseCase startDebateUseCase;
    private final SubmitResponseUseCase submitResponseUseCase;
    private final GetDebateUseCase getDebateUseCase;
    private final ListDebatesUseCase listDebatesUseCase;
    private final DebateMapper debateMapper;
    
    public DebateController(
            CreateDebateUseCase createDebateUseCase,
            JoinDebateUseCase joinDebateUseCase,
            StartDebateUseCase startDebateUseCase,
            SubmitResponseUseCase submitResponseUseCase,
            GetDebateUseCase getDebateUseCase,
            ListDebatesUseCase listDebatesUseCase,
            DebateMapper debateMapper) {
        this.createDebateUseCase = Objects.requireNonNull(createDebateUseCase);
        this.joinDebateUseCase = Objects.requireNonNull(joinDebateUseCase);
        this.startDebateUseCase = Objects.requireNonNull(startDebateUseCase);
        this.submitResponseUseCase = Objects.requireNonNull(submitResponseUseCase);
        this.getDebateUseCase = Objects.requireNonNull(getDebateUseCase);
        this.listDebatesUseCase = Objects.requireNonNull(listDebatesUseCase);
        this.debateMapper = Objects.requireNonNull(debateMapper);
    }
    
    @PostMapping
    @Operation(summary = "Create a new debate")
    public ResponseEntity<Map<String, String>> createDebate(@Valid @RequestBody CreateDebateRequest request) {
        CreateDebateCommand command = debateMapper.toCommand(request);
        DebateId debateId = createDebateUseCase.execute(command);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of(
                "debateId", debateId.toString(),
                "status", "created"
            ));
    }
    
    @PostMapping("/{debateId}/participants")
    @Operation(summary = "Join a debate as a participant")
    public ResponseEntity<Map<String, String>> joinDebate(
            @Parameter(description = "Debate ID") @PathVariable String debateId,
            @Valid @RequestBody JoinDebateRequest request) {
        
        JoinDebateCommand command = debateMapper.toCommand(DebateId.from(debateId), request);
        ParticipantId participantId = joinDebateUseCase.execute(command);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of(
                "participantId", participantId.toString(),
                "debateId", debateId,
                "status", "joined"
            ));
    }
    
    @PostMapping("/{debateId}/start")
    @Operation(summary = "Start a debate")
    public ResponseEntity<Map<String, String>> startDebate(
            @Parameter(description = "Debate ID") @PathVariable String debateId,
            @Valid @RequestBody(required = false) StartDebateRequest request) {
        
        StartDebateCommand command = StartDebateCommand.of(DebateId.from(debateId));
        startDebateUseCase.execute(command);
        
        return ResponseEntity.ok(Map.of(
            "debateId", debateId,
            "status", "started"
        ));
    }
    
    @PostMapping("/{debateId}/responses")
    @Operation(summary = "Submit a response to the current round")
    public ResponseEntity<Map<String, String>> submitResponse(
            @Parameter(description = "Debate ID") @PathVariable String debateId,
            @Valid @RequestBody SubmitResponseRequest request) {
        
        SubmitResponseCommand command = debateMapper.toCommand(DebateId.from(debateId), request);
        ResponseId responseId = submitResponseUseCase.execute(command);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(Map.of(
                "responseId", responseId.toString(),
                "debateId", debateId,
                "status", "submitted"
            ));
    }
    
    @GetMapping("/{debateId}")
    @Operation(summary = "Get a debate by ID")
    public ResponseEntity<DebateResponse> getDebate(
            @Parameter(description = "Debate ID") @PathVariable String debateId) {
        
        GetDebateQuery query = GetDebateQuery.of(DebateId.from(debateId));
        return getDebateUseCase.execute(query)
            .map(debate -> ResponseEntity.ok(debateMapper.toResponse(debate)))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    @Operation(summary = "List debates with optional filtering")
    public ResponseEntity<List<DebateResponse>> listDebates(
            @Parameter(description = "Filter by status") @RequestParam(required = false) Set<String> status,
            @Parameter(description = "Filter by topic containing text") @RequestParam(required = false) String topic,
            @Parameter(description = "Maximum results") @RequestParam(required = false) Integer limit,
            @Parameter(description = "Offset for pagination") @RequestParam(required = false) Integer offset) {
        
        ListDebatesQuery query = buildListQuery(status, topic, limit, offset);
        List<Debate> debates = listDebatesUseCase.execute(query);
        List<DebateResponse> responses = debates.stream()
            .map(debateMapper::toResponse)
            .toList();
        
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/active")
    @Operation(summary = "List active debates")
    public ResponseEntity<List<DebateResponse>> listActiveDebates() {
        ListDebatesQuery query = ListDebatesQuery.active();
        List<Debate> debates = listDebatesUseCase.execute(query);
        List<DebateResponse> responses = debates.stream()
            .map(debateMapper::toResponse)
            .toList();
        
        return ResponseEntity.ok(responses);
    }
    
    @GetMapping("/completed")
    @Operation(summary = "List completed debates")
    public ResponseEntity<List<DebateResponse>> listCompletedDebates() {
        ListDebatesQuery query = ListDebatesQuery.completed();
        List<Debate> debates = listDebatesUseCase.execute(query);
        List<DebateResponse> responses = debates.stream()
            .map(debateMapper::toResponse)
            .toList();
        
        return ResponseEntity.ok(responses);
    }
    
    private ListDebatesQuery buildListQuery(Set<String> statusStrings, String topic, Integer limit, Integer offset) {
        Set<DebateStatus> statuses;
        if (statusStrings == null || statusStrings.isEmpty()) {
            statuses = Set.of(DebateStatus.values());
        } else {
            statuses = statusStrings.stream()
                .map(DebateStatus::fromValue)
                .collect(java.util.stream.Collectors.toSet());
        }
        
        return new ListDebatesQuery(statuses, topic, limit, offset);
    }
}