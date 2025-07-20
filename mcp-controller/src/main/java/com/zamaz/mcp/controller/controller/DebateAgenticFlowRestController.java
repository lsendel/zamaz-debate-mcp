package com.zamaz.mcp.controller.controller;

import com.zamaz.mcp.common.domain.agentic.AgenticFlow;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import com.zamaz.mcp.controller.dto.ResponseDto;
import com.zamaz.mcp.controller.service.DebateService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for debate-specific agentic flow operations.
 */
@RestController
@RequestMapping("/api/v1/debates")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Debate Agentic Flows", description = "API for managing agentic flows in debates")
public class DebateAgenticFlowRestController {

    private final DebateService debateService;

    // Debate-level flow configuration

    @PostMapping("/{debateId}/agentic-flow")
    @Operation(summary = "Configure debate agentic flow", description = "Configures an agentic flow for an entire debate")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Flow configured successfully"),
            @ApiResponse(responseCode = "404", description = "Debate not found")
    })
    public ResponseEntity<AgenticFlow> configureDebateFlow(
            @Parameter(description = "Debate ID", required = true) @PathVariable UUID debateId,
            @Valid @RequestBody ConfigureFlowRequest request) {
        log.info("Configuring agentic flow {} for debate {}", request.getFlowType(), debateId);

        AgenticFlow flow = debateService.configureDebateAgenticFlow(
                debateId,
                request.getFlowType(),
                request.getParameters());

        return ResponseEntity.status(HttpStatus.CREATED).body(flow);
    }

    @GetMapping("/{debateId}/agentic-flow")
    @Operation(summary = "Get debate agentic flow", description = "Retrieves the configured agentic flow for a debate")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flow found"),
            @ApiResponse(responseCode = "404", description = "Flow not found")
    })
    public ResponseEntity<AgenticFlow> getDebateFlow(
            @Parameter(description = "Debate ID", required = true) @PathVariable UUID debateId) {
        log.debug("Getting agentic flow for debate: {}", debateId);

        return debateService.getDebateAgenticFlow(debateId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{debateId}/agentic-flow/recommend")
    @Operation(summary = "Get flow recommendation for debate", description = "Recommends an agentic flow type for the debate")
    public ResponseEntity<AgenticFlowType> recommendDebateFlow(
            @Parameter(description = "Debate ID", required = true) @PathVariable UUID debateId) {
        log.debug("Getting flow recommendation for debate: {}", debateId);

        AgenticFlowType recommendation = debateService.recommendAgenticFlow(debateId);
        return ResponseEntity.ok(recommendation);
    }

    // Participant-level flow configuration

    @PostMapping("/{debateId}/participants/{participantId}/agentic-flow")
    @Operation(summary = "Configure participant agentic flow", description = "Configures an agentic flow for a specific AI participant")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Flow configured successfully"),
            @ApiResponse(responseCode = "404", description = "Participant not found"),
            @ApiResponse(responseCode = "400", description = "Participant is not AI type")
    })
    public ResponseEntity<AgenticFlow> configureParticipantFlow(
            @Parameter(description = "Debate ID", required = true) @PathVariable UUID debateId,
            @Parameter(description = "Participant ID", required = true) @PathVariable UUID participantId,
            @Valid @RequestBody ConfigureFlowRequest request) {
        log.info("Configuring agentic flow {} for participant {} in debate {}",
                request.getFlowType(), participantId, debateId);

        AgenticFlow flow = debateService.configureParticipantAgenticFlow(
                participantId,
                request.getFlowType(),
                request.getParameters());

        return ResponseEntity.status(HttpStatus.CREATED).body(flow);
    }

    @GetMapping("/{debateId}/participants/{participantId}/agentic-flow")
    @Operation(summary = "Get participant agentic flow", description = "Retrieves the configured agentic flow for a participant")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flow found"),
            @ApiResponse(responseCode = "404", description = "Flow not found")
    })
    public ResponseEntity<AgenticFlow> getParticipantFlow(
            @Parameter(description = "Debate ID", required = true) @PathVariable UUID debateId,
            @Parameter(description = "Participant ID", required = true) @PathVariable UUID participantId) {
        log.debug("Getting agentic flow for participant {} in debate {}", participantId, debateId);

        return debateService.getParticipantAgenticFlow(participantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Response processing with agentic flow

    @PostMapping("/{debateId}/participants/{participantId}/process-response")
    @Operation(summary = "Process response with agentic flow", description = "Processes a response using the participant's configured agentic flow")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Response processed successfully"),
            @ApiResponse(responseCode = "404", description = "Participant not found"),
            @ApiResponse(responseCode = "400", description = "No agentic flow configured")
    })
    public CompletableFuture<ResponseEntity<ResponseDto>> processResponseWithFlow(
            @Parameter(description = "Debate ID", required = true) @PathVariable UUID debateId,
            @Parameter(description = "Participant ID", required = true) @PathVariable UUID participantId,
            @Valid @RequestBody ProcessResponseRequest request) {
        log.info("Processing response with agentic flow for participant {} in debate {}",
                participantId, debateId);

        Map<String, Object> roundContext = Map.of(
                "roundId", request.getRoundId(),
                "roundNumber", request.getRoundNumber(),
                "previousResponseCount", request.getPreviousResponseCount());

        return debateService.processResponseWithAgenticFlow(
                participantId,
                request.getPrompt(),
                roundContext)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error processing response with agentic flow", ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }

    // Request DTOs

    public static class ConfigureFlowRequest {
        @NotNull(message = "Flow type is required")
        private AgenticFlowType flowType;

        @NotNull(message = "Parameters are required")
        private Map<String, Object> parameters;

        // Getters and setters
        public AgenticFlowType getFlowType() {
            return flowType;
        }

        public void setFlowType(AgenticFlowType flowType) {
            this.flowType = flowType;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
    }

    public static class ProcessResponseRequest {
        @NotBlank(message = "Prompt is required")
        private String prompt;

        @NotNull(message = "Round ID is required")
        private UUID roundId;

        @NotNull(message = "Round number is required")
        private Integer roundNumber;

        private int previousResponseCount = 0;

        // Getters and setters
        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }

        public UUID getRoundId() {
            return roundId;
        }

        public void setRoundId(UUID roundId) {
            this.roundId = roundId;
        }

        public Integer getRoundNumber() {
            return roundNumber;
        }

        public void setRoundNumber(Integer roundNumber) {
            this.roundNumber = roundNumber;
        }

        public int getPreviousResponseCount() {
            return previousResponseCount;
        }

        public void setPreviousResponseCount(int previousResponseCount) {
            this.previousResponseCount = previousResponseCount;
        }
    }
}