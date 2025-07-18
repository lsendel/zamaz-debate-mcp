package com.zamaz.mcp.rag.adapter.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zamaz.mcp.rag.domain.model.Embedding;
import com.zamaz.mcp.rag.domain.port.EmbeddingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * External adapter for embedding generation using OpenAI or other providers.
 */
@Service
public class EmbeddingServiceAdapter implements EmbeddingService {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Value("${rag.embedding.api-key:}")
    private String apiKey;
    
    @Value("${rag.embedding.api-url:https://api.openai.com/v1/embeddings}")
    private String apiUrl;
    
    @Value("${rag.embedding.model:text-embedding-ada-002}")
    private String model;
    
    @Value("${rag.embedding.batch-size:100}")
    private int batchSize;
    
    public EmbeddingServiceAdapter(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public Embedding generateEmbedding(String text) {
        List<Embedding> embeddings = generateEmbeddings(List.of(text));
        return embeddings.isEmpty() ? null : embeddings.get(0);
    }
    
    @Override
    public List<Embedding> generateEmbeddings(List<String> texts) {
        if (texts.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Process in batches
        List<Embedding> allEmbeddings = new ArrayList<>();
        
        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> batch = texts.subList(i, end);
            
            List<Embedding> batchEmbeddings = generateBatchEmbeddings(batch);
            allEmbeddings.addAll(batchEmbeddings);
        }
        
        return allEmbeddings;
    }
    
    /**
     * Generate embeddings for a batch of texts.
     */
    private List<Embedding> generateBatchEmbeddings(List<String> texts) {
        try {
            // Build request
            Map<String, Object> request = new HashMap<>();
            request.put("model", model);
            request.put("input", texts);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            // Make API call
            ResponseEntity<Map> response = restTemplate.postForEntity(
                apiUrl,
                entity,
                Map.class
            );
            
            // Parse response
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("data")) {
                List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
                
                return data.stream()
                    .sorted(Comparator.comparingInt(d -> (Integer) d.get("index")))
                    .map(d -> {
                        List<Double> vector = (List<Double>) d.get("embedding");
                        return new Embedding(vector);
                    })
                    .collect(Collectors.toList());
            }
            
        } catch (Exception e) {
            // Log error and return empty embeddings
            // In production, would implement proper error handling and retry logic
            System.err.println("Failed to generate embeddings: " + e.getMessage());
        }
        
        // Return null embeddings for each text on failure
        return texts.stream()
            .map(t -> (Embedding) null)
            .collect(Collectors.toList());
    }
}