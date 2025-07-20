package com.zamaz.mcp.llm.adapter;

import com.zamaz.mcp.common.domain.llm.CritiqueIteration;
import com.zamaz.mcp.common.domain.llm.LlmResponse;
import com.zamaz.mcp.common.domain.llm.LlmServicePort;
import com.zamaz.mcp.common.domain.llm.ToolDefinition;
import com.zamaz.mcp.llm.client.McpLlmClient;
import com.zamaz.mcp.llm.dto.LlmRequest;
import com.zamaz.mcp.llm.dto.LlmResponseDto;
import com.zamaz.mcp.llm.dto.ResponseFormat;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter implementation of the LlmServicePort that uses the McpLlmClient.
 */
@Service
public class McpLlmServiceAdapter implements LlmServicePort {
        private final McpLlmClient llmClient;

        /**
         * Creates a new McpLlmServiceAdapter with the specified LLM client.
         *
         * @param llmClient The LLM client to use
         */
        public McpLlmServiceAdapter(McpLlmClient llmClient) {
                this.llmClient = llmClient;
        }

        @Override
        public LlmResponse generate(String prompt, Map<String, Object> parameters) {
                Instant startTime = Instant.now();

                LlmRequest request = LlmRequest.builder()
                                .prompt(prompt)
                                .parameters(parameters)
                                .build();

                LlmResponseDto responseDto = llmClient.generate(request);

                Duration processingTime = Duration.between(startTime, Instant.now());

                return LlmResponse.builder()
                                .text(responseDto.getText())
                                .processingTime(processingTime)
                                .build();
        }

        @Override
        public LlmResponse generateWithInternalMonologue(String prompt, Map<String, Object> parameters) {
                Instant startTime = Instant.now();

                // Add internal monologue specific parameters
                Map<String, Object> enhancedParams = new HashMap<>(parameters);
                enhancedParams.put("temperature", parameters.getOrDefault("temperature", 0.7));
                enhancedParams.put("response_format", "internal_monologue");

                LlmRequest request = LlmRequest.builder()
                                .prompt(prompt)
                                .parameters(enhancedParams)
                                .responseFormat(ResponseFormat.INTERNAL_MONOLOGUE)
                                .build();

                LlmResponseDto responseDto = llmClient.generate(request);

                Duration processingTime = Duration.between(startTime, Instant.now());

                return LlmResponse.builder()
                                .text(responseDto.getText())
                                .processingTime(processingTime)
                                .addMetadata("model", responseDto.getModel())
                                .addMetadata("tokens", responseDto.getTokens())
                                .build();
        }

        @Override
        public LlmResponse generateWithSelfCritique(String prompt, Map<String, Object> parameters, int iterations) {
                Instant startTime = Instant.now();

                // Add self-critique specific parameters
                Map<String, Object> enhancedParams = new HashMap<>(parameters);
                enhancedParams.put("temperature", parameters.getOrDefault("temperature", 0.7));
                enhancedParams.put("response_format", "self_critique");
                enhancedParams.put("iterations", iterations);

                // Check if we should use the native LLM self-critique capability
                boolean useNativeSelfCritique = Boolean.TRUE
                                .equals(parameters.getOrDefault("use_native_self_critique", false));

                if (useNativeSelfCritique) {
                        // Use the LLM's native self-critique capability if available
                        LlmRequest request = LlmRequest.builder()
                                        .prompt(prompt)
                                        .parameters(enhancedParams)
                                        .responseFormat(ResponseFormat.SELF_CRITIQUE)
                                        .build();

                        LlmResponseDto responseDto = llmClient.generate(request);

                        Duration processingTime = Duration.between(startTime, Instant.now());

                        // Parse the iterations from the response metadata if available
                        List<CritiqueIteration> iterationsList = new ArrayList<>();
                        if (responseDto.getMetadata().containsKey("iterations")) {
                                // Implementation depends on how the LLM client returns iterations
                                // This is a placeholder for the actual implementation
                        }

                        return LlmResponse.builder()
                                        .text(responseDto.getText())
                                        .processingTime(processingTime)
                                        .iterations(iterationsList)
                                        .addMetadata("model", responseDto.getModel())
                                        .addMetadata("tokens", responseDto.getTokens())
                                        .build();
                } else {
                        // Implement the self-critique loop manually

                        // Initial generation
                        LlmResponse initialResponse = generate(prompt, parameters);

                        // Self-critique iterations
                        LlmResponse currentResponse = initialResponse;
                        List<CritiqueIteration> critiques = new ArrayList<>();

                        for (int i = 0; i < iterations; i++) {
                                // Generate critique
                                String critiquePrompt = "Please critique the following response, identifying any errors, "
                                                +
                                                "unstated assumptions, or logical fallacies:\n\n"
                                                + currentResponse.getText();

                                LlmResponse critiqueResponse = generate(critiquePrompt, parameters);

                                // Generate revised response
                                String revisionPrompt = "Please provide a revised response addressing the following critique:\n\n"
                                                +
                                                "Original response: " + currentResponse.getText() + "\n\n" +
                                                "Critique: " + critiqueResponse.getText() + "\n\n" +
                                                "Revised response:";

                                currentResponse = generate(revisionPrompt, parameters);

                                critiques.add(new CritiqueIteration(critiqueResponse.getText(),
                                                currentResponse.getText()));
                        }

                        // Calculate total processing time
                        Duration totalProcessingTime = Duration.between(startTime, Instant.now());

                        return LlmResponse.builder()
                                        .text(currentResponse.getText())
                                        .processingTime(totalProcessingTime)
                                        .iterations(critiques)
                                        .addMetadata("model",
                                                        initialResponse.getMetadata().getOrDefault("model", "unknown"))
                                        .addMetadata("tokens", initialResponse.getMetadata().getOrDefault("tokens", 0))
                                        .build();
                }
        }

        @Override
        public LlmResponse generateWithMultiAgentPerspectives(String prompt, Map<String, Object> parameters,
                        Map<String, String> personas) {
                Instant startTime = Instant.now();

                // Add multi-agent perspectives specific parameters
                Map<String, Object> enhancedParams = new HashMap<>(parameters);
                enhancedParams.put("temperature", parameters.getOrDefault("temperature", 0.7));
                enhancedParams.put("response_format", "multi_agent");
                enhancedParams.put("personas", personas);

                // Check if we should use the native LLM multi-agent capability
                boolean useNativeMultiAgent = Boolean.TRUE
                                .equals(parameters.getOrDefault("use_native_multi_agent", false));

                if (useNativeMultiAgent) {
                        // Use the LLM's native multi-agent capability if available
                        LlmRequest request = LlmRequest.builder()
                                        .prompt(prompt)
                                        .parameters(enhancedParams)
                                        .responseFormat(ResponseFormat.MULTI_AGENT)
                                        .build();

                        LlmResponseDto responseDto = llmClient.generate(request);

                        Duration processingTime = Duration.between(startTime, Instant.now());

                        return LlmResponse.builder()
                                        .text(responseDto.getText())
                                        .processingTime(processingTime)
                                        .addMetadata("model", responseDto.getModel())
                                        .addMetadata("tokens", responseDto.getTokens())
                                        .addMetadata("personas", personas.keySet())
                                        .build();
                } else {
                        // For now, just use the standard generate method
                        // In a real implementation, we would need to handle the multi-agent simulation
                        // by making multiple calls to the LLM with different persona prompts
                        LlmResponse response = generate(prompt, parameters);

                        // Calculate total processing time
                        Duration totalProcessingTime = Duration.between(startTime, Instant.now());

                        return LlmResponse.builder()
                                        .text(response.getText())
                                        .processingTime(totalProcessingTime)
                                        .addMetadata("model", response.getMetadata().getOrDefault("model", "unknown"))
                                        .addMetadata("tokens", response.getMetadata().getOrDefault("tokens", 0))
                                        .addMetadata("personas", personas.keySet())
                                        .build();
                }
        }

        @Override
        public LlmResponse generateWithToolCalling(String prompt, Map<String, Object> parameters,
                        List<ToolDefinition> tools) {
                // Implementation for tool calling would go here
                throw new UnsupportedOperationException("Tool calling not implemented yet");
        }
}