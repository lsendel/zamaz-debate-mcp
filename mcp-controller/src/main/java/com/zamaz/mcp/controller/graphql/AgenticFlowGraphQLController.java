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

/**
 * GraphQL controller for agentic flow operations.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class AgenticFlowGraphQLController {

    private final AgenticFlowApplicationService agenticFlowService;
    private final DebateService debateService;

    // Query Mappings

    @QueryMapping
    public Mono<AgenticFlow> agenticFlow(@Argument String id) {
        log.debug("Getting agentic flow with ID: {}", id);
        return Mono.fromCallable(() -> 
            agenticFlowService.getFlow(id)
                .orElseThrow(() -> new RuntimeException("Agentic flow not found: " + id))
        );
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
    public Map<String, AgenticFlowApplicationService.AgenticFlowTemplate> availableFlowTemplates() {
        log.debug("Getting available flow templates");
        return agenticFlowService.getAvailableTemplates();
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
    public AgenticFlow createAgenticFlow(@Argument AgenticFlowType flowType,
                                       @Argument String name,
                                       @Argument String description,
                                       @Argument Map<String, Object> parameters,
                                       @Argument String organizationId) {
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
    public Mono<AgenticFlowResult> executeAgenticFlow(@Argument String flowId,
                                                     @Argument String prompt,
                                                     @Argument String debateId,
                                                     @Argument String participantId) {
        log.info("Executing agentic flow {} for debate {} participant {}", flowId, debateId, participantId);
        
        PromptContext context = new PromptContext(debateId, participantId);
        
        return Mono.fromFuture(() -> 
            CompletableFuture.supplyAsync(() -> 
                agenticFlowService.executeFlow(flowId, prompt, context)
            )
        );
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
        
        return Mono.fromCallable(() -> 
            agenticFlowService.executeFlowByType(flowType, prompt, configuration, context)
        );
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
        
        // TODO: Implement real-time flow execution events using reactive streams
        return Flux.empty();
    }

    // Schema Mappings for nested resolvers

    @SchemaMapping(typeName = "AgenticFlow", field = "executionHistory")
    public List<AgenticFlowExecution> getExecutionHistory(AgenticFlow flow, 
                                                         @Argument Integer limit) {
        log.debug("Getting execution history for flow: {}", flow.getId());
        
        // TODO: Implement execution history retrieval
        return List.of();
    }

    @SchemaMapping(typeName = "AgenticFlow", field = "statistics")
    public AgenticFlowStatistics getStatistics(AgenticFlow flow) {
        log.debug("Getting statistics for flow: {}", flow.getId());
        
        // TODO: Implement statistics calculation
        return new AgenticFlowStatistics();
    }

    // Helper classes for GraphQL types

    public static class AgenticFlowExecutionEvent {
        private String flowId;
        private String flowType;
        private String prompt;
        private AgenticFlowResult result;
        private String timestamp;

        // Getters and setters
    }

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

        // Getters and setters
    }

    public static class AgenticFlowStatistics {
        private Long executionCount;
        private Double averageProcessingTime;
        private Double responseChangeRate;
        private Long errorCount;
        private String lastExecutionTime;

        // Getters and setters
    }
}