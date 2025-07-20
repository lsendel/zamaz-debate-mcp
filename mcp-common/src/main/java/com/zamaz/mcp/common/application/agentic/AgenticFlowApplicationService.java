package com.zamaz.mcp.common.application.agentic;

import com.zamaz.mcp.common.domain.agentic.*;
import com.zamaz.mcp.common.domain.agentic.event.AgenticFlowCreatedEvent;
import com.zamaz.mcp.common.domain.agentic.event.AgenticFlowDeletedEvent;
import com.zamaz.mcp.common.domain.agentic.event.AgenticFlowExecutionEvent;
import com.zamaz.mcp.common.domain.agentic.event.AgenticFlowStatusChangedEvent;
import com.zamaz.mcp.common.domain.event.EventPublisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Application service for managing and executing agentic flows.
 */
@Service
@Transactional
public class AgenticFlowApplicationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AgenticFlowApplicationService.class);
    
    private final AgenticFlowRepository repository;
    private final AgenticFlowDomainService domainService;
    private final EventPublisher eventPublisher;
    private final Map<AgenticFlowType, AgenticFlowProcessor> processorRegistry;
    private final Map<String, AgenticFlowTemplate> templateRegistry;

    /**
     * Creates a new AgenticFlowApplicationService.
     *
     * @param repository      The agentic flow repository
     * @param domainService   The domain service
     * @param eventPublisher  The event publisher
     * @param processors      List of available flow processors
     */
    public AgenticFlowApplicationService(
            AgenticFlowRepository repository,
            AgenticFlowDomainService domainService,
            EventPublisher eventPublisher,
            List<AgenticFlowProcessor> processors) {
        this.repository = repository;
        this.domainService = domainService;
        this.eventPublisher = eventPublisher;
        this.processorRegistry = new ConcurrentHashMap<>();
        this.templateRegistry = new ConcurrentHashMap<>();
        
        // Register all processors
        for (AgenticFlowProcessor processor : processors) {
            registerProcessor(processor);
        }
        
        // Initialize default templates
        initializeDefaultTemplates();
    }

    /**
     * Creates a new agentic flow configuration.
     *
     * @param flowType      The type of flow
     * @param name          The flow name
     * @param description   The flow description
     * @param configuration The flow configuration
     * @param organizationId The organization ID
     * @return The created flow
     */
    public AgenticFlow createFlow(AgenticFlowType flowType, String name, String description,
                                 AgenticFlowConfiguration configuration, String organizationId) {
        logger.info("Creating agentic flow: {} of type {}", name, flowType);
        
        // Validate configuration
        AgenticFlowProcessor processor = processorRegistry.get(flowType);
        if (processor == null) {
            throw new IllegalArgumentException("Unsupported flow type: " + flowType);
        }
        
        if (!processor.validateConfiguration(configuration)) {
            throw new IllegalArgumentException("Invalid configuration for flow type: " + flowType);
        }
        
        // Create flow through domain service
        AgenticFlow flow = domainService.createFlow(flowType, name, description, configuration, organizationId);
        
        // Save to repository
        flow = repository.save(flow);
        
        // Publish event
        eventPublisher.publish(new AgenticFlowCreatedEvent(
            flow.getId(),
            flow.getFlowType(),
            flow.getOrganizationId(),
            Instant.now()
        ));
        
        logger.info("Created agentic flow with ID: {}", flow.getId());
        return flow;
    }

    /**
     * Updates an existing agentic flow configuration.
     *
     * @param flowId        The flow ID
     * @param configuration The new configuration
     * @return The updated flow
     */
    public AgenticFlow updateFlow(String flowId, AgenticFlowConfiguration configuration) {
        logger.info("Updating agentic flow: {}", flowId);
        
        AgenticFlow flow = repository.findById(flowId)
                .orElseThrow(() -> new IllegalArgumentException("Flow not found: " + flowId));
        
        // Validate new configuration
        AgenticFlowProcessor processor = processorRegistry.get(flow.getFlowType());
        if (!processor.validateConfiguration(configuration)) {
            throw new IllegalArgumentException("Invalid configuration for flow type: " + flow.getFlowType());
        }
        
        // Update through domain service
        flow = domainService.updateConfiguration(flow, configuration);
        
        // Save changes
        flow = repository.save(flow);
        
        logger.info("Updated agentic flow: {}", flowId);
        return flow;
    }

    /**
     * Deletes an agentic flow.
     *
     * @param flowId The flow ID
     */
    public void deleteFlow(String flowId) {
        logger.info("Deleting agentic flow: {}", flowId);
        
        AgenticFlow flow = repository.findById(flowId)
                .orElseThrow(() -> new IllegalArgumentException("Flow not found: " + flowId));
        
        repository.delete(flow);
        
        // Publish event
        eventPublisher.publish(new AgenticFlowDeletedEvent(
            flowId,
            flow.getFlowType(),
            flow.getOrganizationId(),
            Instant.now()
        ));
        
        logger.info("Deleted agentic flow: {}", flowId);
    }

    /**
     * Executes an agentic flow.
     *
     * @param flowId  The flow ID
     * @param prompt  The prompt to process
     * @param context The prompt context
     * @return The flow result
     */
    public AgenticFlowResult executeFlow(String flowId, String prompt, PromptContext context) {
        logger.info("Executing agentic flow: {}", flowId);
        
        AgenticFlow flow = repository.findById(flowId)
                .orElseThrow(() -> new IllegalArgumentException("Flow not found: " + flowId));
        
        // Update status
        flow.setStatus(AgenticFlowStatus.ACTIVE);
        repository.save(flow);
        
        // Publish status change event
        eventPublisher.publish(new AgenticFlowStatusChangedEvent(
            flowId,
            AgenticFlowStatus.CREATED,
            AgenticFlowStatus.ACTIVE,
            Instant.now()
        ));
        
        try {
            // Get processor
            AgenticFlowProcessor processor = processorRegistry.get(flow.getFlowType());
            if (processor == null) {
                throw new IllegalStateException("No processor for flow type: " + flow.getFlowType());
            }
            
            // Execute flow
            AgenticFlowResult result = processor.process(prompt, flow.getConfiguration(), context);
            
            // Publish execution event
            eventPublisher.publish(new AgenticFlowExecutionEvent(
                flowId,
                flow.getFlowType(),
                prompt,
                result,
                Instant.now()
            ));
            
            logger.info("Successfully executed agentic flow: {}", flowId);
            return result;
            
        } catch (Exception e) {
            logger.error("Error executing agentic flow: {}", flowId, e);
            
            // Update status to error
            flow.setStatus(AgenticFlowStatus.ERROR);
            repository.save(flow);
            
            // Publish status change event
            eventPublisher.publish(new AgenticFlowStatusChangedEvent(
                flowId,
                AgenticFlowStatus.ACTIVE,
                AgenticFlowStatus.ERROR,
                Instant.now()
            ));
            
            throw new RuntimeException("Flow execution failed", e);
        }
    }

    /**
     * Executes an agentic flow asynchronously.
     *
     * @param flowId  The flow ID
     * @param prompt  The prompt to process
     * @param context The prompt context
     * @return A future containing the flow result
     */
    public CompletableFuture<AgenticFlowResult> executeFlowAsync(String flowId, String prompt, 
                                                                PromptContext context) {
        return CompletableFuture.supplyAsync(() -> executeFlow(flowId, prompt, context));
    }

    /**
     * Executes a flow by type without a saved configuration.
     *
     * @param flowType      The flow type
     * @param prompt        The prompt to process
     * @param configuration The flow configuration
     * @param context       The prompt context
     * @return The flow result
     */
    public AgenticFlowResult executeFlowByType(AgenticFlowType flowType, String prompt,
                                              AgenticFlowConfiguration configuration,
                                              PromptContext context) {
        logger.info("Executing agentic flow by type: {}", flowType);
        
        AgenticFlowProcessor processor = processorRegistry.get(flowType);
        if (processor == null) {
            throw new IllegalArgumentException("Unsupported flow type: " + flowType);
        }
        
        if (!processor.validateConfiguration(configuration)) {
            throw new IllegalArgumentException("Invalid configuration for flow type: " + flowType);
        }
        
        return processor.process(prompt, configuration, context);
    }

    /**
     * Gets a flow by ID.
     *
     * @param flowId The flow ID
     * @return The flow
     */
    public Optional<AgenticFlow> getFlow(String flowId) {
        return repository.findById(flowId);
    }

    /**
     * Gets all flows for an organization.
     *
     * @param organizationId The organization ID
     * @return List of flows
     */
    public List<AgenticFlow> getFlowsByOrganization(String organizationId) {
        return repository.findByOrganizationId(organizationId);
    }

    /**
     * Gets flows by type for an organization.
     *
     * @param organizationId The organization ID
     * @param flowType       The flow type
     * @return List of flows
     */
    public List<AgenticFlow> getFlowsByTypeAndOrganization(String organizationId, 
                                                          AgenticFlowType flowType) {
        return repository.findByOrganizationIdAndFlowType(organizationId, flowType);
    }

    /**
     * Gets active flows for an organization.
     *
     * @param organizationId The organization ID
     * @return List of active flows
     */
    public List<AgenticFlow> getActiveFlows(String organizationId) {
        return repository.findByOrganizationIdAndStatus(organizationId, AgenticFlowStatus.ACTIVE);
    }

    /**
     * Registers a flow processor.
     *
     * @param processor The processor to register
     */
    public void registerProcessor(AgenticFlowProcessor processor) {
        processorRegistry.put(processor.getFlowType(), processor);
        logger.info("Registered processor for flow type: {}", processor.getFlowType());
    }

    /**
     * Gets available flow types.
     *
     * @return Set of available flow types
     */
    public Set<AgenticFlowType> getAvailableFlowTypes() {
        return new HashSet<>(processorRegistry.keySet());
    }

    /**
     * Creates a flow from a template.
     *
     * @param templateName   The template name
     * @param name           The flow name
     * @param organizationId The organization ID
     * @param parameters     Template parameters
     * @return The created flow
     */
    public AgenticFlow createFlowFromTemplate(String templateName, String name, 
                                            String organizationId, Map<String, Object> parameters) {
        AgenticFlowTemplate template = templateRegistry.get(templateName);
        if (template == null) {
            throw new IllegalArgumentException("Template not found: " + templateName);
        }
        
        // Apply template parameters
        AgenticFlowConfiguration configuration = template.buildConfiguration(parameters);
        
        return createFlow(
            template.getFlowType(),
            name,
            template.getDescription(),
            configuration,
            organizationId
        );
    }

    /**
     * Registers a flow template.
     *
     * @param template The template to register
     */
    public void registerTemplate(AgenticFlowTemplate template) {
        templateRegistry.put(template.getName(), template);
        logger.info("Registered template: {}", template.getName());
    }

    /**
     * Gets available templates.
     *
     * @return Map of template names to templates
     */
    public Map<String, AgenticFlowTemplate> getAvailableTemplates() {
        return new HashMap<>(templateRegistry);
    }

    /**
     * Gets recommended flow type for a given context.
     *
     * @param prompt        The prompt
     * @param debateType    The debate type (optional)
     * @param participantRole The participant role (optional)
     * @return Recommended flow type
     */
    public AgenticFlowType recommendFlowType(String prompt, String debateType, String participantRole) {
        // Simple recommendation logic - can be enhanced with ML
        
        // For fact-checking debates, recommend tool calling
        if (debateType != null && debateType.toLowerCase().contains("fact")) {
            return AgenticFlowType.TOOL_CALLING_VERIFICATION;
        }
        
        // For research debates, recommend RAG
        if (debateType != null && debateType.toLowerCase().contains("research")) {
            return AgenticFlowType.RAG_WITH_RERANKING;
        }
        
        // For complex questions, recommend tree of thoughts
        if (prompt.split("\\s+").length > 50) {
            return AgenticFlowType.TREE_OF_THOUGHTS;
        }
        
        // For moderator role, recommend multi-agent
        if ("moderator".equalsIgnoreCase(participantRole)) {
            return AgenticFlowType.MULTI_AGENT_RED_TEAM;
        }
        
        // Default to internal monologue
        return AgenticFlowType.INTERNAL_MONOLOGUE;
    }

    /**
     * Initializes default templates.
     */
    private void initializeDefaultTemplates() {
        // High accuracy template
        registerTemplate(new AgenticFlowTemplate(
            "high_accuracy",
            "High Accuracy Response",
            "Optimized for maximum accuracy with self-critique and confidence scoring",
            AgenticFlowType.SELF_CRITIQUE_LOOP,
            Map.of(
                "iterations", 3,
                "temperature", 0.3f,
                "critique_focus", "accuracy and completeness"
            )
        ));
        
        // Research template
        registerTemplate(new AgenticFlowTemplate(
            "research_assistant",
            "Research Assistant",
            "Comprehensive research with document retrieval and fact verification",
            AgenticFlowType.RAG_WITH_RERANKING,
            Map.of(
                "initial_retrieval_count", 30,
                "final_document_count", 10,
                "reranking_criteria", "relevance, credibility, and recency"
            )
        ));
        
        // Creative thinking template
        registerTemplate(new AgenticFlowTemplate(
            "creative_thinking",
            "Creative Thinking",
            "Explores multiple creative paths for innovative solutions",
            AgenticFlowType.TREE_OF_THOUGHTS,
            Map.of(
                "branching_factor", 4,
                "max_depth", 3,
                "evaluation_method", "combined"
            )
        ));
        
        // Balanced debate template
        registerTemplate(new AgenticFlowTemplate(
            "balanced_debate",
            "Balanced Debate",
            "Considers multiple perspectives for balanced arguments",
            AgenticFlowType.MULTI_AGENT_RED_TEAM,
            Map.of(
                "architect_prompt", "Present the strongest argument",
                "skeptic_prompt", "Challenge assumptions and find weaknesses",
                "judge_prompt", "Provide balanced assessment"
            )
        ));
        
        // Safe response template
        registerTemplate(new AgenticFlowTemplate(
            "safe_response",
            "Safe Response",
            "Ensures responses comply with ethical guidelines",
            AgenticFlowType.CONSTITUTIONAL_PROMPTING,
            Map.of(
                "principles", List.of(
                    "Be helpful and harmless",
                    "Provide accurate information",
                    "Respect privacy",
                    "Avoid bias"
                ),
                "enforce_revision", true
            )
        ));
    }

    /**
     * Inner class representing a flow template.
     */
    public static class AgenticFlowTemplate {
        private final String name;
        private final String displayName;
        private final String description;
        private final AgenticFlowType flowType;
        private final Map<String, Object> defaultParameters;

        public AgenticFlowTemplate(String name, String displayName, String description,
                                 AgenticFlowType flowType, Map<String, Object> defaultParameters) {
            this.name = name;
            this.displayName = displayName;
            this.description = description;
            this.flowType = flowType;
            this.defaultParameters = new HashMap<>(defaultParameters);
        }

        public AgenticFlowConfiguration buildConfiguration(Map<String, Object> overrides) {
            Map<String, Object> parameters = new HashMap<>(defaultParameters);
            if (overrides != null) {
                parameters.putAll(overrides);
            }
            return new AgenticFlowConfiguration(parameters);
        }

        // Getters
        public String getName() { return name; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public AgenticFlowType getFlowType() { return flowType; }
        public Map<String, Object> getDefaultParameters() { return new HashMap<>(defaultParameters); }
    }
}