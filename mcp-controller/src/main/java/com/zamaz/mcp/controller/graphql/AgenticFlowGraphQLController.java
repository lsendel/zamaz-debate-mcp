package com.zamaz.mcp.controller.graphql;

import com.zamaz.mcp.common.application.agentic.AgenticFlowApplicationService;
import com.zamaz.mcp.common.domain.agentic.AgenticFlow;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowConfiguration;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowResult;
import com.zamaz.mcp.common.domain.agentic.AgenticFlowType;
import com.zamaz.mcp.common.domain.agentic.PromptContext;
import com.zamaz.mcp.controller.service.DebateService;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.graphql.data.method.annotation.SubscriptionMapping;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;

/**
 * GraphQL controller for agentic flow operations.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
@Validated
public class AgenticFlowGraphQLController {

    private final AgenticFlowApplicationService agenticFlowService;
    private final DebateService debateService;

    // Query Mappings

    @QueryMapping
    public Mono<AgenticFlow> agenticFlow(@Argument String id) {
        log.debug("Getting agentic flow with ID: {}", id);
        return Mono.fromCallable(() -> agenticFlowService.getFlow(id)
                .orElseThrow(() -> new RuntimeException("Agentic flow not found: " + id)));
    }

    @QueryMapping
    public List<AgenticFlow> agenticFlowsByOrganization(@Argument String organizationId) {
        log.debug("Getting agentic flows for organization: {}", organizationId);
        return agenticFlowService.getFlowsByOrganization(organizationId);
    }

    @QueryMapping
    public List<AgenticFlow> agenticFlowsByType(@Argument String organizationId,
            @Argument AgenticFlowType flowType) {
        log.debug("Getting agentic flows for organization {} and type {}", organizationId, flowType);
        return agenticFlowService.getFlowsByTypeAndOrganization(organizationId, flowType);
    }

    @QueryMapping
    public List<AgenticFlow> activeAgenticFlows(@Argument String organizationId) {
        log.debug("Getting active agentic flows for organization: {}", organizationId);
        return agenticFlowService.getActiveFlows(organizationId);
    }

    @QueryMapping
    public Set<AgenticFlowType> availableFlowTypes() {
        log.debug("Getting available flow types");
        return agenticFlowService.getAvailableFlowTypes();
    }

    @QueryMapping
    public List<AgenticFlowTemplate> availableFlowTemplates() {
        log.debug("Getting available flow templates");
        return agenticFlowService.getAvailableTemplates().values()
                .stream()
                .map(this::toTemplateDto)
                .toList();
    }

    @QueryMapping
    public AgenticFlowType recommendFlowType(@Argument String prompt,
            @Argument String debateType,
            @Argument String participantRole) {
        log.debug("Getting flow recommendation for debate type {} and role {}", debateType, participantRole);
        return agenticFlowService.recommendFlowType(prompt, debateType, participantRole);
    }

    // Mutation Mappings

    @MutationMapping
    public AgenticFlow createAgenticFlow(@Argument @NotNull AgenticFlowType flowType,
            @Argument @NotBlank String name,
            @Argument String description,
            @Argument @NotNull Map<String, Object> parameters,
            @Argument @NotBlank String organizationId) {
        log.info("Creating agentic flow {} of type {}", name, flowType);

        AgenticFlowConfiguration configuration = new AgenticFlowConfiguration(parameters);
        return agenticFlowService.createFlow(flowType, name, description, configuration, organizationId);
    }

    @MutationMapping
    public AgenticFlow createAgenticFlowFromTemplate(@Argument String templateName,
            @Argument String name,
            @Argument String organizationId,
            @Argument Map<String, Object> parameters) {
        log.info("Creating agentic flow {} from template {}", name, templateName);

        return agenticFlowService.createFlowFromTemplate(templateName, name, organizationId, parameters);
    }

    @MutationMapping
    public AgenticFlow updateAgenticFlow(@Argument String flowId,
            @Argument Map<String, Object> parameters) {
        log.info("Updating agentic flow: {}", flowId);

        AgenticFlowConfiguration configuration = new AgenticFlowConfiguration(parameters);
        return agenticFlowService.updateFlow(flowId, configuration);
    }

    @MutationMapping
    public Boolean deleteAgenticFlow(@Argument String flowId) {
        log.info("Deleting agentic flow: {}", flowId);

        agenticFlowService.deleteFlow(flowId);
        return true;
    }

    @MutationMapping
    public Mono<AgenticFlowResult> executeAgenticFlow(@Argument @NotBlank String flowId,
            @Argument @NotBlank String prompt,
            @Argument @NotBlank String debateId,
            @Argument @NotBlank String participantId) {
        log.info("Executing agentic flow {} for debate {} participant {}", flowId, debateId, participantId);

        PromptContext context = new PromptContext(debateId, participantId);

        return Mono.fromFuture(
                () -> CompletableFuture.supplyAsync(() -> agenticFlowService.executeFlow(flowId, prompt, context)));
    }

    @MutationMapping
    public Mono<AgenticFlowResult> executeAgenticFlowByType(@Argument AgenticFlowType flowType,
            @Argument String prompt,
            @Argument Map<String, Object> parameters,
            @Argument String debateId,
            @Argument String participantId) {
        log.info("Executing agentic flow by type {} for debate {} participant {}",
                flowType, debateId, participantId);

        AgenticFlowConfiguration configuration = new AgenticFlowConfiguration(parameters);
        PromptContext context = new PromptContext(debateId, participantId);

        return Mono.fromCallable(() -> agenticFlowService.executeFlowByType(flowType, prompt, configuration, context));
    }

    @MutationMapping
    public AgenticFlow configureDebateAgenticFlow(@Argument String debateId,
            @Argument AgenticFlowType flowType,
            @Argument Map<String, Object> parameters) {
        log.info("Configuring agentic flow {} for debate {}", flowType, debateId);

        return debateService.configureDebateAgenticFlow(UUID.fromString(debateId), flowType, parameters);
    }

    @MutationMapping
    public AgenticFlow configureParticipantAgenticFlow(@Argument String participantId,
            @Argument AgenticFlowType flowType,
            @Argument Map<String, Object> parameters) {
        log.info("Configuring agentic flow {} for participant {}", flowType, participantId);

        return debateService.configureParticipantAgenticFlow(UUID.fromString(participantId), flowType, parameters);
    }

    // Subscription Mappings

    @SubscriptionMapping
    public Flux<AgenticFlowExecutionEvent> agenticFlowExecutions(@Argument String organizationId) {
        log.info("Subscribing to agentic flow executions for organization: {}", organizationId);

        // For now, return an empty flux - this would be implemented with a proper event
        // stream
        return Flux.empty();
    }

    // Schema Mappings for nested resolvers

    @SchemaMapping(typeName = "AgenticFlow", field = "executionHistory")
    public List<AgenticFlowExecution> getExecutionHistory(AgenticFlow flow,
            @Argument Integer limit) {
        log.debug("Getting execution history for flow: {}", flow.getId());

        // For now, return empty list - this would be implemented with proper analytics
        // service
        return List.of();
    }

    @SchemaMapping(typeName = "AgenticFlow", field = "statistics")
    public AgenticFlowStatistics getStatistics(AgenticFlow flow) {
        log.debug("Getting statistics for flow: {}", flow.getId());

        // For now, return default statistics - this would be implemented with proper
        // analytics service
        return AgenticFlowStatistics.builder()
                .executionCount(0L)
                .averageProcessingTime(0.0)
                .responseChangeRate(0.0)
                .errorCount(0L)
                .lastExecutionTime(null)
                .build();
    }

    // Helper methods for DTO conversion

    private AgenticFlowTemplate toTemplateDto(AgenticFlowApplicationService.AgenticFlowTemplate template) {
        return AgenticFlowTemplate.builder()
                .name(template.getName())
                .displayName(template.getDisplayName())
                .description(template.getDescription())
                .flowType(template.getFlowType())
                .defaultParameters(template.getDefaultParameters())
                .build();
    }

    // Helper classes for GraphQL types

    @lombok.Data
    @lombok.Builder
    public static class AgenticFlowExecutionEvent {
        private String flowId;
        private String flowType;
        private String prompt;
        private AgenticFlowResult result;
        private String timestamp;
    }

    @lombok.Data
    @lombok.Builder
    public static class AgenticFlowExecution {
        private String id;
        private String flowId;
        private String debateId;
        private String participantId;
        private String prompt;
        private AgenticFlowResult result;
        private Long processingTimeMs;
        private Boolean responseChanged;
        private String createdAt;
    }

    @lombok.Data
    @lombok.Builder
    public static class AgenticFlowStatistics {
        private Long executionCount;
        private Double averageProcessingTime;
        private Double responseChangeRate;
        private Long errorCount;
        private String lastExecutionTime;
    }

    @lombok.Data
    @lombok.Builder
    public static class AgenticFlowTemplate {
        private String name;
        private String displayName;
        private String description;
        private AgenticFlowType flowType;
        private Map<String, Object> defaultParameters;
    }
}