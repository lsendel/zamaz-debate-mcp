package com.zamaz.mcp.common.domain.agentic;

/**
 * Interface for processors that implement specific agentic flow types.
 */
public interface AgenticFlowProcessor {

    /**
     * Returns the type of agentic flow that this processor handles.
     *
     * @return The flow type
     */
    AgenticFlowType getFlowType();

    /**
     * Processes a prompt using this agentic flow.
     *
     * @param prompt        The prompt to process
     * @param configuration The flow configuration
     * @param context       The prompt context
     * @return The result of processing
     */
    AgenticFlowResult process(String prompt, AgenticFlowConfiguration configuration, PromptContext context);

    /**
     * Validates the configuration for this flow type.
     *
     * @param configuration The configuration to validate
     * @return True if the configuration is valid, false otherwise
     */
    default boolean validateConfiguration(AgenticFlowConfiguration configuration) {
        return true;
    }
}