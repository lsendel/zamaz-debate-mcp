package com.zamaz.mcp.debateengine.adapter.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.debateengine.domain.model.AIModel;
import com.zamaz.mcp.debateengine.domain.model.Context;
import com.zamaz.mcp.debateengine.domain.model.Message;
import com.zamaz.mcp.debateengine.domain.model.QualityScore;
import com.zamaz.mcp.debateengine.domain.port.AIService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * External adapter for AI service interactions.
 */
@Service
public class AIServiceAdapter implements AIService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${debate-engine.ai.llm-service-url:http://localhost:5002}")
    private String llmServiceUrl;
    
    @Value("${debate-engine.ai.quality-analysis-enabled:true}")
    private boolean qualityAnalysisEnabled;
    
    public AIServiceAdapter(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public CompletableFuture<AIResponse> generateResponse(
            AIModel model,
            Context context,
            String prompt) {
        
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            // Build request
            Map<String, Object> request = buildGenerationRequest(model, context, prompt);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            try {
                // Call LLM service
                ResponseEntity<Map> response = restTemplate.postForEntity(
                    llmServiceUrl + "/api/llm/generate",
                    entity,
                    Map.class
                );
                
                Map<String, Object> body = response.getBody();
                if (body != null) {
                    String content = (String) body.get("content");
                    Integer tokenCount = (Integer) body.getOrDefault("tokenCount", 0);
                    String modelUsed = (String) body.getOrDefault("model", model.name());
                    
                    long responseTime = System.currentTimeMillis() - startTime;
                    
                    return new AIResponse(content, tokenCount, responseTime, modelUsed);
                }
            } catch (Exception e) {
                // Log error and return fallback response
                System.err.println("Failed to generate AI response: " + e.getMessage());
            }
            
            // Fallback response
            return new AIResponse(
                "I apologize, but I'm unable to generate a response at this time.",
                20,
                System.currentTimeMillis() - startTime,
                model.name()
            );
        });
    }
    
    @Override
    public CompletableFuture<QualityScore> analyzeQuality(
            String response,
            String topic,
            String position) {
        
        if (!qualityAnalysisEnabled) {
            return CompletableFuture.completedFuture(QualityScore.empty());
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Build analysis request
                Map<String, Object> request = Map.of(
                    "text", response,
                    "topic", topic,
                    "position", position,
                    "analysisType", "debate_quality"
                );
                
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
                
                // Call quality analysis endpoint
                ResponseEntity<Map> result = restTemplate.postForEntity(
                    llmServiceUrl + "/api/llm/analyze",
                    entity,
                    Map.class
                );
                
                Map<String, Object> body = result.getBody();
                if (body != null) {
                    return QualityScore.of(
                        ((Number) body.getOrDefault("overall", 0.0)).doubleValue(),
                        ((Number) body.getOrDefault("sentiment", 0.0)).doubleValue(),
                        ((Number) body.getOrDefault("coherence", 0.0)).doubleValue(),
                        ((Number) body.getOrDefault("factuality", 0.0)).doubleValue()
                    );
                }
            } catch (Exception e) {
                System.err.println("Failed to analyze quality: " + e.getMessage());
            }
            
            return QualityScore.empty();
        });
    }
    
    @Override
    public int countTokens(String text, String model) {
        // Simple approximation - in production would use proper tokenizer
        return text.split("\\s+").length * 2;
    }
    
    /**
     * Build generation request for LLM service.
     */
    private Map<String, Object> buildGenerationRequest(
            AIModel model,
            Context context,
            String prompt) {
        
        // Convert context messages to format expected by LLM service
        List<Map<String, String>> messages = context.getMessages().stream()
            .map(msg -> Map.of(
                "role", msg.role().name().toLowerCase(),
                "content", msg.content()
            ))
            .collect(Collectors.toList());
        
        // Add the current prompt
        messages.add(Map.of(
            "role", "user",
            "content", prompt
        ));
        
        return Map.of(
            "provider", model.provider(),
            "model", model.name(),
            "messages", messages,
            "options", model.config(),
            "maxTokens", model.getConfigValue("max_tokens", 1000),
            "temperature", model.getConfigValue("temperature", 0.7)
        );
    }
}