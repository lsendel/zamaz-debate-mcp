package com.zamaz.mcp.common.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * High-level client for LLM service MCP tools.
 * Provides type-safe methods for LLM operations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LlmServiceClient {

    private final McpServiceClient mcpServiceClient;
    private final McpServiceRegistry serviceRegistry;

    /**
     * List available LLM providers.
     *
     * @return List of available providers
     */
    public JsonNode listProviders() {
        Map<String, Object> params = new HashMap<>();

        String serviceUrl = serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.LLM);
        return mcpServiceClient.callTool(serviceUrl, "list_providers", params);
    }

    /**
     * List available LLM providers with authentication.
     *
     * @param authentication Authentication context
     * @return List of available providers
     */
    public JsonNode listProviders(Authentication authentication) {
        Map<String, Object> params = new HashMap<>();

        String serviceUrl = serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.LLM);
        return mcpServiceClient.callTool(serviceUrl, "list_providers", params, authentication);
    }

    /**
     * Generate text completion using specified provider.
     *
     * @param provider LLM provider (claude, openai, gemini, ollama)
     * @param prompt Input prompt
     * @param model Model name (optional)
     * @param maxTokens Maximum tokens (optional)
     * @param temperature Temperature for randomness (optional)
     * @param authentication Authentication context
     * @return Completion response
     */
    public JsonNode generateCompletion(String provider, String prompt, String model, 
                                     Integer maxTokens, Double temperature, Authentication authentication) {
        Map<String, Object> params = new HashMap<>();
        params.put("provider", provider);
        params.put("prompt", prompt);
        
        if (model != null) {
            params.put("model", model);
        }
        if (maxTokens != null) {
            params.put("maxTokens", maxTokens);
        }
        if (temperature != null) {
            params.put("temperature", temperature);
        }

        String serviceUrl = serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.LLM);
        return mcpServiceClient.callTool(serviceUrl, "generate_completion", params, authentication);
    }

    /**
     * Generate completion with basic parameters.
     *
     * @param provider LLM provider
     * @param prompt Input prompt
     * @param authentication Authentication context
     * @return Completion response
     */
    public JsonNode generateCompletion(String provider, String prompt, Authentication authentication) {
        return generateCompletion(provider, prompt, null, null, null, authentication);
    }

    /**
     * Generate completion with Claude provider.
     *
     * @param prompt Input prompt
     * @param authentication Authentication context
     * @return Completion response
     */
    public JsonNode generateClaudeCompletion(String prompt, Authentication authentication) {
        return generateCompletion("claude", prompt, authentication);
    }

    /**
     * Generate completion with OpenAI provider.
     *
     * @param prompt Input prompt
     * @param authentication Authentication context
     * @return Completion response
     */
    public JsonNode generateOpenAiCompletion(String prompt, Authentication authentication) {
        return generateCompletion("openai", prompt, authentication);
    }

    /**
     * Generate completion with Gemini provider.
     *
     * @param prompt Input prompt
     * @param authentication Authentication context
     * @return Completion response
     */
    public JsonNode generateGeminiCompletion(String prompt, Authentication authentication) {
        return generateCompletion("gemini", prompt, authentication);
    }

    /**
     * Generate completion with Ollama provider.
     *
     * @param prompt Input prompt
     * @param model Ollama model name
     * @param authentication Authentication context
     * @return Completion response
     */
    public JsonNode generateOllamaCompletion(String prompt, String model, Authentication authentication) {
        return generateCompletion("ollama", prompt, model, null, null, authentication);
    }

    /**
     * Get status of a specific provider.
     *
     * @param provider Provider name
     * @param authentication Authentication context
     * @return Provider status
     */
    public JsonNode getProviderStatus(String provider, Authentication authentication) {
        Map<String, Object> params = new HashMap<>();
        params.put("provider", provider);

        String serviceUrl = serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.LLM);
        return mcpServiceClient.callTool(serviceUrl, "get_provider_status", params, authentication);
    }

    /**
     * Get status of a specific provider without authentication.
     *
     * @param provider Provider name
     * @return Provider status
     */
    public JsonNode getProviderStatus(String provider) {
        Map<String, Object> params = new HashMap<>();
        params.put("provider", provider);

        String serviceUrl = serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.LLM);
        return mcpServiceClient.callTool(serviceUrl, "get_provider_status", params);
    }

    /**
     * Generate completion with conversation context.
     *
     * @param provider LLM provider
     * @param conversationContext Previous conversation messages
     * @param newPrompt New user prompt
     * @param authentication Authentication context
     * @return Completion response
     */
    public JsonNode generateCompletionWithContext(String provider, String conversationContext, 
                                                 String newPrompt, Authentication authentication) {
        String fullPrompt = conversationContext + "\n\nUser: " + newPrompt;
        return generateCompletion(provider, fullPrompt, authentication);
    }

    /**
     * Generate completion with system prompt.
     *
     * @param provider LLM provider
     * @param systemPrompt System instructions
     * @param userPrompt User prompt
     * @param authentication Authentication context
     * @return Completion response
     */
    public JsonNode generateCompletionWithSystemPrompt(String provider, String systemPrompt, 
                                                      String userPrompt, Authentication authentication) {
        String fullPrompt = "System: " + systemPrompt + "\n\nUser: " + userPrompt;
        return generateCompletion(provider, fullPrompt, authentication);
    }

    /**
     * Check if LLM service is available.
     *
     * @return true if service is available
     */
    public boolean isLlmServiceAvailable() {
        return serviceRegistry.isServiceAvailable(McpServiceRegistry.McpService.LLM);
    }

    /**
     * Get available LLM tools.
     *
     * @return List of available tools
     */
    public JsonNode getAvailableTools() {
        String serviceUrl = serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.LLM);
        return mcpServiceClient.listTools(serviceUrl);
    }

    /**
     * Get LLM service information.
     *
     * @return Service information
     */
    public JsonNode getServiceInfo() {
        String serviceUrl = serviceRegistry.getServiceUrl(McpServiceRegistry.McpService.LLM);
        return mcpServiceClient.getServerInfo(serviceUrl);
    }

    /**
     * Check if a specific provider is available and working.
     *
     * @param provider Provider name
     * @return true if provider is available
     */
    public boolean isProviderAvailable(String provider) {
        try {
            JsonNode status = getProviderStatus(provider);
            return status.has("status") && "available".equals(status.get("status").asText());
        } catch (Exception e) {
            log.debug("Provider {} not available: {}", provider, e.getMessage());
            return false;
        }
    }

    /**
     * Get all available providers with their status.
     *
     * @return Map of provider names to availability status
     */
    public Map<String, Boolean> getProviderAvailability() {
        Map<String, Boolean> providerStatus = new HashMap<>();
        
        try {
            JsonNode providers = listProviders();
            if (providers.has("providers") && providers.get("providers").isArray()) {
                providers.get("providers").forEach(provider -> {
                    String providerName = provider.get("name").asText();
                    boolean isEnabled = provider.get("enabled").asBoolean();
                    providerStatus.put(providerName, isEnabled);
                });
            }
        } catch (Exception e) {
            log.warn("Failed to get provider availability: {}", e.getMessage());
        }
        
        return providerStatus;
    }
}