package com.zamaz.mcp.controller.controller;

import com.zamaz.mcp.controller.dto.DebateDto;
import com.zamaz.mcp.controller.service.DebateLifecycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/debates")
@RequiredArgsConstructor
@Tag(name = "Debate Lifecycle", description = "Endpoints for managing debate lifecycle")
public class DebateLifecycleController {

    private final DebateLifecycleService debateLifecycleService;

    @PostMapping
    @Operation(summary = "Create a new debate")
    public ResponseEntity<DebateDto> createDebate(@Valid @RequestBody DebateDto.CreateDebateRequest request) {
        DebateDto debate = debateLifecycleService.createDebate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(debate);
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start debate")
    public ResponseEntity<DebateDto> startDebate(@PathVariable UUID id) {
        DebateDto debate = debateLifecycleService.startDebate(id);
        return ResponseEntity.ok(debate);
    }
}
