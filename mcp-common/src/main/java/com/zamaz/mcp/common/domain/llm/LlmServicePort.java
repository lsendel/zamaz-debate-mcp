package com.zamaz.mcp.common.domain.llm;

import java.util.List;
import java.util.Map;

/**
 * Port interface for interacting with LLM services.
 */
public interface LlmServicePort {

        /**
         * Generates a response from the LLM using the provided prompt and parameters.
         *
         * @param prompt     The prompt to send to the LLM
         * @param parameters Additional parameters for the LLM request
         * @return The LLM response
         */
        LlmResponse generate(String prompt, Map<String, Object> parameters);

        /**
         * Generates a response from the LLM using internal monologue prompting.
         *
         * @param prompt     The prompt to send to the LLM
         * @param parameters Additional parameters for the LLM request
         * @return The LLM response with internal monologue
         */
        LlmResponse generateWithInternalMonologue(String prompt, Map<String, Object> parameters);

        /**
         * Generates a response from the LLM using self-critique loop prompting.
         *
         * @param prompt     The prompt to send to the LLM
         * @param parameters Additional parameters for the LLM request
         * @param iterations The number of critique iterations to perform
         * @return The LLM response with self-critique
         */
        LlmResponse generateWithSelfCritique(String prompt, Map<String, Object> parameters, int iterations);

        /**
         * Generates a response from the LLM using multi-agent red-team prompting.
         *
         * @param prompt     The prompt to send to the LLM
         * @param parameters Additional parameters for the LLM request
         * @param personas   The personas to use in the red-team
         * @return The LLM response with multi-agent perspectives
         */
        LlmResponse generateWithMultiAgentPerspectives(String prompt, Map<String, Object> parameters,
                        Map<String, String> personas);

        /**
         * Generates a response from the LLM that may include tool calls.
         *
         * @param prompt     The prompt to send to the LLM
         * @param parameters Additional parameters for the LLM request
         * @param tools      The available tools
         * @return The LLM response with potential tool calls
         */
        LlmResponse generateWithToolCalling(String prompt, Map<String, Object> parameters,
                        List<ToolDefinition> tools);
}