package com.zamaz.mcp.llm.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zamaz.mcp.llm.service.LlmGatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class McpEndpointController {

    private final ObjectMapper objectMapper;
    private final LlmGatewayService llmGatewayService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<JsonNode> getServerInfo() {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("name", "mcp-llm");
        response.put("version", "1.0.0");
        response.put("description", "LLM Gateway service for multiple AI providers");
        
        ObjectNode capabilities = response.putObject("capabilities");
        capabilities.put("tools", true);
        capabilities.put("resources", true);
        
        return Mono.just(response);
    }

    @PostMapping(value = "/list-tools", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<JsonNode> listTools() {
        ObjectNode response = objectMapper.createObjectNode();
        ArrayNode tools = response.putArray("tools");
        
        // List providers tool
        ObjectNode listProviders = tools.addObject();
        listProviders.put("name", "list_providers");
        listProviders.put("description", "List available LLM providers");
        listProviders.putObject("parameters").put("type", "object");
        
        // Generate completion tool
        ObjectNode generateCompletion = tools.addObject();
        generateCompletion.put("name", "generate_completion");
        generateCompletion.put("description", "Generate text completion using specified provider");
        ObjectNode completionParams = generateCompletion.putObject("parameters");
        completionParams.put("type", "object");
        ObjectNode completionProps = completionParams.putObject("properties");
        completionProps.putObject("provider").put("type", "string").put("description", "LLM provider (claude, openai, gemini, ollama)");
        completionProps.putObject("model").put("type", "string").put("description", "Model name (optional)");
        completionProps.putObject("prompt").put("type", "string").put("description", "Input prompt");
        completionProps.putObject("maxTokens").put("type", "integer").put("description", "Maximum tokens (optional)");
        completionProps.putObject("temperature").put("type", "number").put("description", "Temperature (optional)");
        completionParams.putArray("required").add("provider").add("prompt");
        
        // Get provider status tool
        ObjectNode getProviderStatus = tools.addObject();
        getProviderStatus.put("name", "get_provider_status");
        getProviderStatus.put("description", "Get status of a specific provider");
        ObjectNode statusParams = getProviderStatus.putObject("parameters");
        statusParams.put("type", "object");
        ObjectNode statusProps = statusParams.putObject("properties");
        statusProps.putObject("provider").put("type", "string").put("description", "Provider name");
        statusParams.putArray("required").add("provider");
        
        return Mono.just(response);
    }

    @PostMapping(value = "/call-tool", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<JsonNode> callTool(@RequestBody JsonNode request) {
        String toolName = request.get("name").asText();
        JsonNode params = request.get("arguments");
        
        log.info("MCP tool call: {} with params: {}", toolName, params);
        
        switch (toolName) {
            case "list_providers":
                return listProviders();
                
            case "generate_completion":
                return generateCompletion(params);
                
            case "get_provider_status":
                return getProviderStatus(params);
                
            default:
                ObjectNode error = objectMapper.createObjectNode();
                error.put("error", "Unknown tool: " + toolName);
                return Mono.just(error);
        }
    }

    private Mono<JsonNode> listProviders() {
        return llmGatewayService.getAvailableProviders()
            .collectList()
            .map(providers -> {
                ObjectNode response = objectMapper.createObjectNode();
                ArrayNode providerArray = response.putArray("providers");
                providers.forEach(providerArray::add);
                return (JsonNode) response;
            });
    }

    private Mono<JsonNode> generateCompletion(JsonNode params) {
        String provider = params.get("provider").asText();
        String prompt = params.get("prompt").asText();
        String model = params.has("model") ? params.get("model").asText() : null;
        Integer maxTokens = params.has("maxTokens") ? params.get("maxTokens").asInt() : null;
        Double temperature = params.has("temperature") ? params.get("temperature").asDouble() : null;
        
        // Create completion request
        ObjectNode request = objectMapper.createObjectNode();
        request.put("prompt", prompt);
        if (model != null) request.put("model", model);
        if (maxTokens != null) request.put("maxTokens", maxTokens);
        if (temperature != null) request.put("temperature", temperature);
        
        return llmGatewayService.generateCompletion(provider, request)
            .map(completion -> {
                ObjectNode response = objectMapper.createObjectNode();
                response.put("text", completion.getText());
                response.put("provider", completion.getProvider());
                response.put("model", completion.getModel());
                response.put("usage", completion.getUsage().toString());
                return (JsonNode) response;
            });
    }

    private Mono<JsonNode> getProviderStatus(JsonNode params) {
        String provider = params.get("provider").asText();
        ObjectNode response = objectMapper.createObjectNode();
        response.put("provider", provider);
        response.put("status", "available");
        response.put("healthCheck", "OK");
        return Mono.just(response);
    }

    @GetMapping("/health")
    public Mono<String> health() {
        return Mono.just("OK");
    }
}