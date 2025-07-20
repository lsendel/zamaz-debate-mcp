package com.zamaz.mcp.common.domain.agentic;

import java.util.Map;

/**
 * Domain service interface for agentic flow operations.
 */
public interface AgenticFlowDomainService {

    /**
     * Processes a prompt using the specified agentic flow.
     *
     * @param flow    The agentic flow to use
     * @param prompt  The prompt to process
     * @param context The prompt context
     * @return The result of processing
     * @throws IllegalArgumentException if the flow is invalid or inactive
     */
    AgenticFlowResult processPrompt(AgenticFlow flow, String prompt, PromptContext context);

    /**
     * Creates a flow configuration for the specified flow type.
     *
     * @param type   The flow type
     * @param params The configuration parameters
     * @return The created configuration
     * @throws IllegalArgumentException if the parameters are invalid for the flow
     *                                  type
     */
    AgenticFlowConfiguration createFlowConfiguration(AgenticFlowType type, Map<String, Object> params);

    /**
     * Validates a flow configuration for the specified flow type.
     *
     * @param flow The flow to validate
     * @return True if the configuration is valid, false otherwise
     */
    boolean validateFlowConfiguration(AgenticFlow flow);

    /**
     * Registers a processor for a specific flow type.
     *
     * @param processor The processor to register
     */
    void registerProcessor(AgenticFlowProcessor processor);

    /**
     * Returns the processor for the specified flow type.
     *
     * @param type The flow type
     * @return The processor for the flow type
     * @throws IllegalArgumentException if no processor is registered for the flow
     *                                  type
     */
    AgenticFlowProcessor getProcessor(AgenticFlowType type);
}