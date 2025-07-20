package com.zamaz.mcp.controller.controller;

import com.zamaz.mcp.common.application.agentic.AgenticFlowApplicationService;
import com.zamaz.mcp.common.domain.agentic.AgenticFlow;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowConfiguration;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowResult;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import com.zamaz.mcp.common.domain.agentic.PromptContext;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for agentic flow operations.
 */
@RestController
@RequestMapping("/api/v1/agentic-flows")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Agentic Flows", description = "API for managing and executing agentic flows")
public class AgenticFlowRestController {

    private final AgenticFlowApplicationService agenticFlowService;

    // GET endpoints

    @GetMapping("/{flowId}")
    @Operation(summary = "Get agentic flow by ID", description = "Retrieves a specific agentic flow configuration")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Flow found"),
            @ApiResponse(responseCode = "404", description = "Flow not found")
    })
    public ResponseEntity<AgenticFlow> getFlow(
            @Parameter(description = "Flow ID", required = true) @PathVariable String flowId) {
        log.debug("Getting agentic flow: {}", flowId);

        return agenticFlowService.getFlow(flowId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/organization/{organizationId}")
    @Operation(summary = "Get flows by organization", description = "Retrieves all agentic flows for an organization")
    public ResponseEntity<List<AgenticFlow>> getFlowsByOrganization(
            @Parameter(description = "Organization ID", required = true) @PathVariable String organizationId) {
        log.debug("Getting flows for organization: {}", organizationId);

        List<AgenticFlow> flows = agenticFlowService.getFlowsByOrganization(organizationId);
        return ResponseEntity.ok(flows);
    }

    @GetMapping("/organization/{organizationId}/type/{flowType}")
    @Operation(summary = "Get flows by organization and type", description = "Retrieves agentic flows filtered by type")
    public ResponseEntity<List<AgenticFlow>> getFlowsByTypeAndOrganization(
            @Parameter(description = "Organization ID", required = true) @PathVariable String organizationId,
            @Parameter(description = "Flow type", required = true) @PathVariable AgenticFlowType flowType) {
        log.debug("Getting flows for organization {} and type {}", organizationId, flowType);

        List<AgenticFlow> flows = agenticFlowService.getFlowsByTypeAndOrganization(organizationId, flowType);
        return ResponseEntity.ok(flows);
    }

    @GetMapping("/organization/{organizationId}/active")
    @Operation(summary = "Get active flows", description = "Retrieves all active agentic flows for an organization")
    public ResponseEntity<List<AgenticFlow>> getActiveFlows(
            @Parameter(description = "Organization ID", required = true) @PathVariable String organizationId) {
        log.debug("Getting active flows for organization: {}", organizationId);

        List<AgenticFlow> flows = agenticFlowService.getActiveFlows(organizationId);
        return ResponseEntity.ok(flows);
    }

    @GetMapping("/types")
    @Operation(summary = "Get available flow types", description = "Returns all supported agentic flow types")
    public ResponseEntity<Set<AgenticFlowType>> getAvailableFlowTypes() {
        log.debug("Getting available flow types");

        Set<AgenticFlowType> types = agenticFlowService.getAvailableFlowTypes();
        return ResponseEntity.ok(types);
    }

    @GetMapping("/templates")
    @Operation(summary = "Get flow templates", description = "Returns all available flow templates")
    public ResponseEntity<Map<String, AgenticFlowApplicationService.AgenticFlowTemplate>> getTemplates() {
        log.debug("Getting flow templates");

        Map<String, AgenticFlowApplicationService.AgenticFlowTemplate> templates = agenticFlowService
                .getAvailableTemplates();
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/recommend")
    @Operation(summary = "Get flow recommendation", description = "Recommends a flow type based on context")
    public ResponseEntity<AgenticFlowType> recommendFlowType(
            @Parameter(description = "Prompt text", required = true) @RequestParam @NotBlank String prompt,
            @Parameter(description = "Debate type") @RequestParam(required = false) String debateType,
            @Parameter(description = "Participant role") @RequestParam(required = false) String participantRole) {
        log.debug("Getting flow recommendation for debate type {} and role {}", debateType, participantRole);

        AgenticFlowType recommendation = agenticFlowService.recommendFlowType(prompt, debateType, participantRole);
        return ResponseEntity.ok(recommendation);
    }

    // POST endpoints

    @PostMapping
    @Operation(summary = "Create agentic flow", description = "Creates a new agentic flow configuration")
    @ApiResponse(responseCode = "201", description = "Flow created successfully")
    public ResponseEntity<AgenticFlow> createFlow(
            @Valid @RequestBody CreateFlowRequest request) {
        log.info("Creating agentic flow: {}", request.getName());

        AgenticFlowConfiguration configuration = new AgenticFlowConfiguration(request.getParameters());
        AgenticFlow flow = agenticFlowService.createFlow(
                request.getFlowType(),
                request.getName(),
                request.getDescription(),
                configuration,
                request.getOrganizationId());

        return ResponseEntity.status(HttpStatus.CREATED).body(flow);
    }

    @PostMapping("/from-template")
    @Operation(summary = "Create flow from template", description = "Creates a new agentic flow from a predefined template")
    @ApiResponse(responseCode = "201", description = "Flow created successfully")
    public ResponseEntity<AgenticFlow> createFlowFromTemplate(
            @Valid @RequestBody CreateFlowFromTemplateRequest request) {
        log.info("Creating flow {} from template {}", request.getName(), request.getTemplateName());

        AgenticFlow flow = agenticFlowService.createFlowFromTemplate(
                request.getTemplateName(),
                request.getName(),
                request.getOrganizationId(),
                request.getParameters());

        return ResponseEntity.status(HttpStatus.CREATED).body(flow);
    }

    // PUT endpoints

    @PutMapping("/{flowId}")
    @Operation(summary = "Update agentic flow", description = "Updates an existing agentic flow configuration")
    public ResponseEntity<AgenticFlow> updateFlow(
            @Parameter(description = "Flow ID", required = true) @PathVariable String flowId,
            @Valid @RequestBody UpdateFlowRequest request) {
        log.info("Updating agentic flow: {}", flowId);

        AgenticFlowConfiguration configuration = new AgenticFlowConfiguration(request.getParameters());
        AgenticFlow flow = agenticFlowService.updateFlow(flowId, configuration);

        return ResponseEntity.ok(flow);
    }

    // DELETE endpoints

    @DeleteMapping("/{flowId}")
    @Operation(summary = "Delete agentic flow", description = "Deletes an agentic flow configuration")
    @ApiResponse(responseCode = "204", description = "Flow deleted successfully")
    public ResponseEntity<Void> deleteFlow(
            @Parameter(description = "Flow ID", required = true) @PathVariable String flowId) {
        log.info("Deleting agentic flow: {}", flowId);

        agenticFlowService.deleteFlow(flowId);
        return ResponseEntity.noContent().build();
    }

    // Execution endpoints

    @PostMapping("/{flowId}/execute")
    @Operation(summary = "Execute agentic flow", description = "Executes an agentic flow with the given prompt")
    public CompletableFuture<ResponseEntity<AgenticFlowResult>> executeFlow(
            @Parameter(description = "Flow ID", required = true) @PathVariable String flowId,
            @Valid @RequestBody ExecuteFlowRequest request) {
        log.info("Executing agentic flow: {}", flowId);

        PromptContext context = new PromptContext(request.getDebateId(), request.getParticipantId());

        return agenticFlowService.executeFlowAsync(flowId, request.getPrompt(), context)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error executing flow: {}", flowId, ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }

    @PostMapping("/execute-by-type")
    @Operation(summary = "Execute flow by type", description = "Executes an agentic flow by type without saved configuration")
    public ResponseEntity<AgenticFlowResult> executeFlowByType(
            @Valid @RequestBody ExecuteFlowByTypeRequest request) {
        log.info("Executing agentic flow by type: {}", request.getFlowType());

        AgenticFlowConfiguration configuration = new AgenticFlowConfiguration(request.getParameters());
        PromptContext context = new PromptContext(request.getDebateId(), request.getParticipantId());

        AgenticFlowResult result = agenticFlowService.executeFlowByType(
                request.getFlowType(),
                request.getPrompt(),
                configuration,
                context);

        return ResponseEntity.ok(result);
    }

    // Request DTOs

    @Schema(description = "Request to create a new agentic flow", example = """
            {
                "flowType": "INTERNAL_MONOLOGUE",
                "name": "My Internal Monologue Flow",
                "description": "A flow for step-by-step reasoning",
                "parameters": {
                    "prefix": "Think step by step:",
                    "temperature": 0.7
                },
                "organizationId": "org-123"
            }
            """)
    public static class CreateFlowRequest {
        @NotNull(message = "Flow type is required")
        private AgenticFlowType flowType;

        @NotBlank(message = "Name is required")
        private String name;

        private String description;

        @NotNull(message = "Parameters are required")
        private Map<String, Object> parameters;

        @NotBlank(message = "Organization ID is required")
        private String organizationId;

        // Getters and setters
        public AgenticFlowType getFlowType() {
            return flowType;
        }

        public void setFlowType(AgenticFlowType flowType) {
            this.flowType = flowType;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }

        public String getOrganizationId() {
            return organizationId;
        }

        public void setOrganizationId(String organizationId) {
            this.organizationId = organizationId;
        }
    }

    @Schema(description = "Request to create a flow from template")
    public static class CreateFlowFromTemplateRequest {
        @NotBlank(message = "Template name is required")
        private String templateName;

        @NotBlank(message = "Name is required")
        private String name;

        @NotBlank(message = "Organization ID is required")
        private String organizationId;

        private Map<String, Object> parameters;

        // Getters and setters
        public String getTemplateName() {
            return templateName;
        }

        public void setTemplateName(String templateName) {
            this.templateName = templateName;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getOrganizationId() {
            return organizationId;
        }

        public void setOrganizationId(String organizationId) {
            this.organizationId = organizationId;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
    }

    @Schema(description = "Request to update a flow configuration")
    public static class UpdateFlowRequest {
        @NotNull(message = "Parameters are required")
        private Map<String, Object> parameters;

        // Getters and setters
        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
    }

    @Schema(description = "Request to execute a flow", example = """
            {
                "prompt": "What are the benefits of renewable energy?",
                "debateId": "debate-456",
                "participantId": "participant-789"
            }
            """)
    public static class ExecuteFlowRequest {
        @NotBlank(message = "Prompt is required")
        private String prompt;

        @NotBlank(message = "Debate ID is required")
        private String debateId;

        @NotBlank(message = "Participant ID is required")
        private String participantId;

        // Getters and setters
        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }

        public String getDebateId() {
            return debateId;
        }

        public void setDebateId(String debateId) {
            this.debateId = debateId;
        }

        public String getParticipantId() {
            return participantId;
        }

        public void setParticipantId(String participantId) {
            this.participantId = participantId;
        }
    }

    @Schema(description = "Request to execute a flow by type")
    public static class ExecuteFlowByTypeRequest {
        @NotNull(message = "Flow type is required")
        private AgenticFlowType flowType;

        @NotBlank(message = "Prompt is required")
        private String prompt;

        @NotNull(message = "Parameters are required")
        private Map<String, Object> parameters;

        @NotBlank(message = "Debate ID is required")
        private String debateId;

        @NotBlank(message = "Participant ID is required")
        private String participantId;

        // Getters and setters
        public AgenticFlowType getFlowType() {
            return flowType;
        }

        public void setFlowType(AgenticFlowType flowType) {
            this.flowType = flowType;
        }

        public String getPrompt() {
            return prompt;
        }

        public void setPrompt(String prompt) {
            this.prompt = prompt;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }

        public String getDebateId() {
            return debateId;
        }

        public void setDebateId(String debateId) {
            this.debateId = debateId;
        }

        public String getParticipantId() {
            return participantId;
        }

        public void setParticipantId(String participantId) {
            this.participantId = participantId;
        }
    }
}